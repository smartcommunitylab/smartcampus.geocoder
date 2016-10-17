package de.komoot.photon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.client.Client;
import org.json.JSONObject;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.BaseElasticsearchSearcher;
import de.komoot.photon.searcher.PhotonRequestHandler;
import de.komoot.photon.searcher.PhotonRequestHandlerFactory;
import de.komoot.photon.utils.ConvertToGeoJson;
import spark.Request;
import spark.Response;
import spark.Route;

public class SCAddressHandler<R extends PhotonRequest> extends Route {

	private final PhotonRequestFactory photonRequestFactory;
	private final PhotonRequestHandlerFactory requestHandlerFactory;
	private final ConvertToGeoJson geoJsonConverter;

	SCAddressHandler(String path, Client esNodeClient, String languages) {
		super(path);
		Set<String> supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
		this.photonRequestFactory = new PhotonRequestFactory(supportedLanguages);
		this.geoJsonConverter = new ConvertToGeoJson();
		this.requestHandlerFactory = new PhotonRequestHandlerFactory(new BaseElasticsearchSearcher(esNodeClient));
	}

	@Override
	public Object handle(Request request, Response response) {
		R photonRequest = null;
		try {
			photonRequest = photonRequestFactory.createSCRequest(request);
		} catch (BadRequestException e) {
			JSONObject json = new JSONObject();
			json.put("message", e.getMessage());
			halt(e.getHttpStatus(), json.toString());
		}
		PhotonRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
		List<JSONObject> results = handler.handle(photonRequest);

		List<JSONObject> mappedResult = Utils.mapResult(results);

		/** if search by ref position. - start **/
		double lat = -1, lon = -1;
		if (request.queryParams("latlng") != null) {
			String latlng = request.queryParams("latlng");
			double[] referenceLocation = Utils.extractCoords(latlng);
			lat = referenceLocation[0];
			lon = referenceLocation[1];
		}

		if (lat > 0 && lon > 0) {
			// sort result.
			Utils.sortAlgoByDistance(mappedResult, lat, lon);
		}
		/** if search by ref position. - end **/

		/** pagination - start. **/
		int page = 0;
		int count = 20;
		List<JSONObject> paginatedList = new ArrayList<JSONObject>();

		if (request.queryParams("rows") != null) {
			count = Integer.valueOf(request.queryParams("rows"));
		}
		if (request.queryParams("start") != null) {
			page = Integer.valueOf(request.queryParams("start"));
		}

		if (!mappedResult.isEmpty() && (page * count) <= mappedResult.size()) {
			if (((page + 1) * count) <= mappedResult.size()) {
				paginatedList = mappedResult.subList(page * count, (page + 1) * count);
			} else {
				paginatedList = mappedResult.subList(page * count, mappedResult.size());
			}

		}
		/** pagination - end. **/

		JSONObject geoJsonResults = geoJsonConverter.convert(paginatedList);

		/** response format - start. **/
		JSONObject expRes = new JSONObject();
		expRes.put("numFound", paginatedList.size());
		expRes.put("start", page);
		expRes.put("docs", geoJsonResults.get("features"));

		JSONObject result = new JSONObject();
		result.put("responseHeader", new JSONObject());
		result.put("response", expRes);
		/** response format - end. **/

		response.type("application/json; charset=utf-8");
		response.header("Access-Control-Allow-Origin", "*");

		if (request.queryParams("debug") != null)
			return geoJsonResults.toString(4);

		return result.toString();
	}

}
