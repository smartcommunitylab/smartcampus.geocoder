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
			String query = request.queryParams("address");
			if (query.length() < Constants.MINIMUM_LETTER_THRESHOLD) {
				return geoJsonConverter.convert(new ArrayList<JSONObject>()).toString();
			} else {
				photonRequest = photonRequestFactory.createSCRequest(request);
			}
		} catch (BadRequestException e) {
			JSONObject json = new JSONObject();
			json.put("message", e.getMessage());
			halt(e.getHttpStatus(), json.toString());
		}
		PhotonRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
//		long startTime = System.currentTimeMillis();
		List<JSONObject> results = handler.handle(photonRequest);
//		long stopTime = System.currentTimeMillis();
//		System.out.println(((stopTime - startTime) / 1000) + " secs");

		List<JSONObject> mappedResult = Utils.mapResult(results);

		/** if search by ref position. - start **/
		
		/*double lat = -1, lon = -1;
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

		/** remove name from all type civic. start.**/
		List<JSONObject> filteredList = new ArrayList<JSONObject>();
		for (JSONObject obj: mappedResult) {
			if (obj.has(Constants.STRADARIO)) {
				obj.remove("name");
			}
			filteredList.add(obj);
		}
		/** remove name from all type civic. end.**/
		
		
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

		if (!filteredList.isEmpty() && (page * count) <= filteredList.size()) {
			if (((page + 1) * count) <= filteredList.size()) {
				paginatedList = filteredList.subList(page * count, (page + 1) * count);
			} else {
				paginatedList = filteredList.subList(page * count, filteredList.size());
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
