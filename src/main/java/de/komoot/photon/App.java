package de.komoot.photon;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.setIpAddress;
import static spark.Spark.setPort;
import static spark.Spark.staticFileLocation;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.elasticsearch.client.Client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.komoot.photon.elasticsearch.Server;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimUpdater;
import de.komoot.photon.utils.CivicSQLScriptsGenerator;
import lombok.extern.slf4j.Slf4j;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

@Slf4j
public class App {

	public static void main(String[] rawArgs) {
		// parse command line arguments
		CommandLineArgs args = new CommandLineArgs();
		final JCommander jCommander = new JCommander(args);
		try {
			jCommander.parse(rawArgs);
		} catch (ParameterException e) {
			log.warn("could not start photon: " + e.getMessage());
			jCommander.usage();
			return;
		}

		// show help
		if (args.isUsage()) {
			jCommander.usage();
			return;
		}

		if (args.isGenerateDBScript()) {
			CivicSQLScriptsGenerator civicSQLScriptsGenerator = new CivicSQLScriptsGenerator();
			civicSQLScriptsGenerator.init();
			return;
		}

		if (args.getJsonDump() != null) {
			startJsonDump(args);
			return;
		}

		boolean shutdownES = false;
		final Server esServer = new Server(args).start();
		try {
			Client esClient = esServer.getClient();

			if (args.isRecreateIndex()) {
				shutdownES = true;
				startRecreatingIndex(esServer);
				return;
			}

			if (args.isNominatimImport()) {
				shutdownES = true;
				startNominatimImport(args, esServer, esClient);
				return;
			}

			// no special action specified -> normal mode: start search API
			startApi(args, esClient);
		} finally {
			if (shutdownES)
				esServer.shutdown();
		}
	}

	/**
	 * dump elastic search index and create a new and empty one
	 *
	 * @param esServer
	 */
	private static void startRecreatingIndex(Server esServer) {
		try {
			esServer.recreateIndex();
		} catch (IOException e) {
			log.error("cannot setup index, elastic search config files not readable", e);
			return;
		}

		log.info("deleted photon index and created an empty new one.");
	}

	/**
	 * take nominatim data and dump it to json
	 *
	 * @param args
	 */
	private static void startJsonDump(CommandLineArgs args) {
		try {
			final String filename = args.getJsonDump();
			final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages());
			NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(),
					args.getDatabase(), args.getUser(), args.getPassword());
			nominatimConnector.setImporter(jsonDumper);
			nominatimConnector.readEntireDatabase();
			log.info("json dump was created: " + filename);
		} catch (FileNotFoundException e) {
			log.error("cannot create dump", e);
		}
	}

	/**
	 * take nominatim data to fill elastic search index
	 *
	 * @param args
	 * @param esServer
	 * @param esNodeClient
	 */
	private static void startNominatimImport(CommandLineArgs args, Server esServer, Client esNodeClient) {
		try {
			esServer.recreateIndex(); // dump previous data
		} catch (IOException e) {
			log.error("cannot setup index, elastic search config files not readable", e);
			return;
		}

		log.info("starting import from nominatim to photon with languages: " + args.getLanguages());
		de.komoot.photon.elasticsearch.Importer importer = new de.komoot.photon.elasticsearch.Importer(esNodeClient,
				args.getLanguages());
		NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(),
				args.getDatabase(), args.getUser(), args.getPassword());
		nominatimConnector.setImporter(importer);
		try {
			nominatimConnector.readEntireDatabase();
		} catch (Exception e) {
			log.info("error importing from nominatim: " + e.getMessage());
		}

		log.info("imported data from nominatim to photon with languages: " + args.getLanguages());
	}

	/**
	 * start api to accept search requests via http
	 *
	 * @param args
	 * @param esNodeClient
	 */
	private static void startApi(CommandLineArgs args, Client esNodeClient) {
		setPort(args.getListenPort());
		setIpAddress(args.getListenIp());

		// context-path.
		String contextPath = args.getContextPath();

		// demo.html.
		staticFileLocation("/public");

		// setup search API
		get(new SearchRequestHandler("api", esNodeClient, args.getLanguages()));
		get(new SearchRequestHandler("api/", esNodeClient, args.getLanguages()));
		get(new ReverseSearchRequestHandler("reverse", esNodeClient, args.getLanguages()));
		get(new ReverseSearchRequestHandler("reverse/", esNodeClient, args.getLanguages()));
		// sco apis.
		get(new SCAddressHandler("/" + contextPath + "/address", esNodeClient, args.getLanguages()));
		get(new SCAddressHandler(contextPath + "/address/", esNodeClient, args.getLanguages()));
		get(new SCLocationHandler(contextPath + "/location", esNodeClient, args.getLanguages()));
		get(new SCLocationHandler("/" + contextPath + "/location", esNodeClient, args.getLanguages()));

		options(new Route("/*") {
			@Override
			public Object handle(Request request, Response response) {
				String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
				if (accessControlRequestHeaders != null) {
					response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
				}

				String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
				if (accessControlRequestMethod != null) {
					response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
				}

				return "OK";
			}
		});
		before(new Filter() {

			@Override
			public void handle(Request request, Response response) {
				response.header("Access-Control-Allow-Origin", "*");
				response.header("Access-Control-Request-Method", "*");
				response.header("Access-Control-Allow-Headers", "*");
				// Note: this may or may not be necessary in your particular
				// application
				response.type("application/json");

			}
		});

		// setup update API
		final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(),
				args.getDatabase(), args.getUser(), args.getPassword());
		Updater updater = new de.komoot.photon.elasticsearch.Updater(esNodeClient, args.getLanguages());
		nominatimUpdater.setUpdater(updater);

		get(new Route("/nominatim-update") {
			@Override
			public Object handle(Request request, Response response) {
				Thread nominatimUpdaterThread = new Thread() {
					@Override
					public void run() {
						nominatimUpdater.update();
					}
				};
				nominatimUpdaterThread.start();
				return "nominatim update started (more information in console output) ...";
			}
		});
	}

	// Enables CORS on requests. This method is an initialization method and
	// should be called once.
	private static void enableCORS(final String origin, final String methods, final String headers) {

	}
}
