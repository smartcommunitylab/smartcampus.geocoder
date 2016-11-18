package de.komoot.photon;

import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.elasticsearch.client.Client;
import org.json.JSONObject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.ElasticsearchReverseSearcher;
import de.komoot.photon.searcher.ReverseElasticsearchSearcher;
import de.komoot.photon.searcher.ReverseRequestHandler;
import de.komoot.photon.searcher.ReverseRequestHandlerFactory;
import de.komoot.photon.utils.ConvertToGeoJson;
import spark.Request;
import spark.Response;
import spark.Route;

public class SCLocationHandler <R extends ReverseRequest> extends Route {

	private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseRequestHandlerFactory requestHandlerFactory;
    private final ConvertToGeoJson geoJsonConverter;

    SCLocationHandler(String path, Client esNodeClient, String languages) {
        super(path);
        Set<String> supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages);
        this.geoJsonConverter = new ConvertToGeoJson();
        this.requestHandlerFactory = new ReverseRequestHandlerFactory(new ReverseElasticsearchSearcher(esNodeClient));
    }

    @Override
    public String handle(Request request, Response response) {
        R photonRequest = null;
        try {
            photonRequest = reverseRequestFactory.createSCRequest(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            halt(e.getHttpStatus(), json.toString());
        }
        ReverseRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
//        Long startTime = System.currentTimeMillis();
        List<JSONObject> results = handler.handle(photonRequest);
//        long stopTime = System.currentTimeMillis();
//		System.out.println(((stopTime - startTime)/1000) + " secs");
        List<JSONObject> mappedResult = Utils.mapResult(results);
        
        Point point = photonRequest.getLocation();
        double lat = point.getCoordinate().getOrdinate(1);
        double lon = point.getCoordinate().getOrdinate(0);
        if (lat > 0 && lon > 0) {
        	// sort result.
            Utils.sortAlgoByDistance(mappedResult, lat, lon);
        }
        
        /** remove name from all type civic. start.**/
		List<JSONObject> filteredList = new ArrayList<JSONObject>();
		for (JSONObject obj: mappedResult) {
			if (obj.has(Constants.STRADARIO)) {
				obj.remove("name");
			}
			filteredList.add(obj);
		}
		/** remove name from all type civic. end.**/
		
        // pagination.
        int page = 0;
        int count = 10;
        List<JSONObject> paginatedList = new ArrayList<JSONObject>();
        
        if (request.queryParams("rows") != null && request.queryParams("start") !=null) {
        	page = Integer.valueOf(request.queryParams("start"));
        	count = Integer.valueOf(request.queryParams("rows"));
        } 
        
		if (!filteredList.isEmpty() && (page * count) <= filteredList.size()) {
			if (((page + 1) * count) <= filteredList.size()) {
				paginatedList = filteredList.subList(page * count, (page + 1) * count);
			} else {
				paginatedList = filteredList.subList(page * count, filteredList.size());
			}

		}

		JSONObject geoJsonResults = geoJsonConverter.convert(paginatedList);
		
		JSONObject expRes = new JSONObject();
		expRes.put("numFound", paginatedList.size()); 
		expRes.put("start", page);
		expRes.put("docs", geoJsonResults.get("features"));
		
		JSONObject result = new JSONObject();
		result.put("responseHeader", new JSONObject());
		result.put("response", expRes);
		
        response.type("application/json; charset=utf-8");
        response.header("Access-Control-Allow-Origin", "*");
        
        if (request.queryParams("debug") != null)
            return geoJsonResults.toString(4);

        
        response.type("application/json; charset=utf-8");
        response.header("Access-Control-Allow-Origin", "*");
        if (request.queryParams("debug") != null)
            return geoJsonResults.toString(4);

        return result.toString();
    }

}
