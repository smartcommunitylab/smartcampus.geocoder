package de.komoot.photon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;

/**
 * helper functions to create convert a photon document to XContentBuilder
 * object / JSON
 *
 * @author christoph
 */
public class Utils {
	private static final Joiner commaJoiner = Joiner.on(", ").skipNulls();
	private static int layerThresholdInMeters = 60;

	public static XContentBuilder convert(PhotonDoc doc, String[] languages) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field(Constants.OSM_ID, doc.getOsmId())
				.field(Constants.OSM_TYPE, doc.getOsmType()).field(Constants.OSM_KEY, doc.getTagKey())
				.field(Constants.OSM_VALUE, doc.getTagValue()).field(Constants.IMPORTANCE, doc.getImportance())
				.field(Constants.EXTRA_TAGS, doc.getExtratags());

		if (doc.getCentroid() != null) {
			builder.startObject("coordinate").field("lat", doc.getCentroid().getY())
					.field("lon", doc.getCentroid().getX()).endObject();
		}

		if (doc.getHouseNumber() != null) {
			builder.field("housenumber", doc.getHouseNumber());
		}

		if (doc.getPostcode() != null) {
			builder.field("postcode", doc.getPostcode());
		}

		writeName(builder, doc.getName(), languages);
		writeIntlNames(builder, doc.getCity(), "city", languages);
		writeIntlNames(builder, doc.getCountry(), "country", languages);
		writeIntlNames(builder, doc.getState(), "state", languages);
		writeIntlNames(builder, doc.getStreet(), "street", languages);
		writeContext(builder, doc.getContext(), languages);
		writeExtent(builder, doc.getBbox());

		return builder;
	}

	private static void writeExtent(XContentBuilder builder, Envelope bbox) throws IOException {
		if (bbox == null)
			return;

		if (bbox.getArea() == 0.)
			return;

		// http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope
		builder.startObject("extent");
		builder.field("type", "envelope");

		builder.startArray("coordinates");
		builder.startArray().value(bbox.getMinX()).value(bbox.getMaxY()).endArray();
		builder.startArray().value(bbox.getMaxX()).value(bbox.getMinY()).endArray();

		builder.endArray();
		builder.endObject();
	}

	private static void writeName(XContentBuilder builder, Map<String, String> name, String[] languages)
			throws IOException {
		Map<String, String> fNames = filterNames(name, languages);

		if (name.get("alt_name") != null)
			fNames.put("alt", name.get("alt_name"));

		if (name.get("int_name") != null)
			fNames.put("int", name.get("int_name"));

		if (name.get("loc_name") != null)
			fNames.put("loc", name.get("loc_name"));

		if (name.get("old_name") != null)
			fNames.put("old", name.get("old_name"));

		if (name.get("reg_name") != null)
			fNames.put("reg", name.get("reg_name"));

		// SmartCommunity Lab fix.
		if (fNames.get("default") == null && name.get("addr:housename") != null) {
			fNames.put("default", name.get("addr:housename"));
		}

		write(builder, fNames, "name");
	}

	private static void write(XContentBuilder builder, Map<String, String> fNames, String name) throws IOException {
		if (fNames.isEmpty())
			return;

		builder.startObject(name);
		for (Map.Entry<String, String> entry : fNames.entrySet()) {
			builder.field(entry.getKey(), entry.getValue());
		}
		builder.endObject();
	}

	protected static void writeContext(XContentBuilder builder, Set<Map<String, String>> contexts, String[] languages)
			throws IOException {
		final SetMultimap<String, String> multimap = HashMultimap.create();

		for (Map<String, String> context : contexts) {
			if (context.get("name") != null) {
				multimap.put("default", context.get("name"));
			}
		}

		for (String language : languages) {
			for (Map<String, String> context : contexts) {
				if (context.get("name:" + language) != null) {
					multimap.put(language, context.get("name:" + language));
				}
			}
		}

		final Map<String, Collection<String>> map = multimap.asMap();
		if (!multimap.isEmpty()) {
			builder.startObject("context");
			for (Map.Entry<String, Collection<String>> entry : map.entrySet()) {
				builder.field(entry.getKey(), commaJoiner.join(entry.getValue()));
			}
			builder.endObject();
		}
	}

	private static void writeIntlNames(XContentBuilder builder, Map<String, String> names, String name,
			String[] languages) throws IOException {
		Map<String, String> fNames = filterNames(names, languages);
		write(builder, fNames, name);
	}

	private static Map<String, String> filterNames(Map<String, String> names, String[] languages) {
		return filterNames(names, new HashMap<String, String>(), languages);
	}

	private static Map<String, String> filterNames(Map<String, String> names, HashMap<String, String> filteredNames,
			String[] languages) {
		if (names == null)
			return filteredNames;

		if (names.get("name") != null) {
			filteredNames.put("default", names.get("name"));
		}

		for (String language : languages) {
			if (names.get("name:" + language) != null) {
				filteredNames.put(language, names.get("name:" + language));
			}
		}

		return filteredNames;
	}

	// http://stackoverflow.com/a/4031040/1437096
	public static String stripNonDigits(
			final CharSequence input /* inspired by seh's comment */) {
		final StringBuilder sb = new StringBuilder(
				input.length() /* also inspired by seh's comment */);
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (c > 47 && c < 58) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static double[] extractCoords(String latlng) throws IllegalArgumentException {
		if (latlng == null) {
			return null;
		}
		String[] coords = latlng.split(",");
		double lat = -1, lng = -1;
		boolean invalidParam = false;
		if (coords.length == 2) {
			try {
				lat = Double.parseDouble(coords[0]);
				lng = Double.parseDouble(coords[1]);
			} catch (NumberFormatException e) {
				invalidParam = true;
			}
		} else {
			invalidParam = true;
		}

		if (invalidParam) {
			throw new IllegalArgumentException();
		}

		double[] result = { lat, lng };
		return result;
	}

	public static List<JSONObject> mapResult(List<JSONObject> results) {

		List<JSONObject> mapped = new ArrayList<JSONObject>();

		for (JSONObject obj : results) {
			JSONObject tmp = new JSONObject();
			JSONObject geometry = (JSONObject) obj.get("geometry");
			JSONArray coord = geometry.getJSONArray("coordinates");

			if (coord != null && coord.length() == 2) {
				tmp.put("coordinate", coord.get(1) + "," + coord.get(0));
				JSONObject properties = (JSONObject) obj.get("properties");
				Iterator<?> keys = properties.keys();

				while (keys.hasNext()) {
					String key = (String) keys.next();
					if (key.equalsIgnoreCase("extratags")) {
						Map<String, String> exTags = (Map<String, String>) properties.get(key);
						for (String mapKey : exTags.keySet()) {
							tmp.put(mapKey, exTags.get(mapKey));
						}
					} else {
						tmp.put(key, properties.get(key));
					}

				}
				mapped.add(tmp);
			}
		}

		return mapped;
	}

	/**
	 * RANGE SORTER.
	 * 
	 * @param mappedResult
	 * @param latR
	 * @param lonR
	 * @return
	 */
	public static void sortAlgoByDistance(List<JSONObject> mappedResult, double latR, double lonR) {
		int i, j;
		int clusterStart = 0;
		int clusterCurrent = 1;
		int clusterEnd = 0;
		JSONObject clusterStartkey;
		JSONObject clusterCurrentKey;

		for (i = 0; i < mappedResult.size() - 1; i++) {
			clusterStartkey = mappedResult.get(i);
			double[] currentClusterStartCoords = Utils.getCoordinates(clusterStartkey);
			double d1 = distance(latR, currentClusterStartCoords[0], lonR, currentClusterStartCoords[1], 0.0, 0.0);

			for (j = i + 1; j < mappedResult.size(); j++) {
				clusterCurrentKey = mappedResult.get(j);
				double[] currentClusterEndCoords = Utils.getCoordinates(clusterCurrentKey);
				double d2 = distance(latR, currentClusterEndCoords[0], lonR, currentClusterEndCoords[1], 0.0, 0.0);
				clusterStart = i; // i is the new startindex.
				if (Math.abs(d1 - d2) > layerThresholdInMeters) {
					// clusterEnd = j -1;
					break;
				}
			}
			clusterEnd = j - 1;
			if (clusterEnd > clusterStart) {
				Collections.sort(mappedResult.subList(clusterStart, clusterEnd + 1), new PlaceMarkComparator());
				clusterStart = clusterCurrent;
				i = clusterEnd;
			}
		}

	}

	private static double[] getCoordinates(JSONObject key) {
		String[] tmpCoords = key.get("coordinate").toString().split(",");
		// place mark.
		double tmpLat = Double.valueOf(tmpCoords[0]);
		double tmpLon = Double.valueOf(tmpCoords[1]);

		return new double[] { tmpLat, tmpLon };
	}

	
	/*
	 * Calculate distance between two points in latitude and longitude taking
	 * into account height difference. If you are not interested in height
	 * difference pass 0.0. Uses Haversine method as its base.
	 * 
	 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
	 * el2 End altitude in meters
	 * @returns Distance in Meters
	 */
	public static double distance(double lat1, double lat2, double lon1,
	        double lon2, double el1, double el2) {

	    final int R = 6371; // Radius of the earth

	    Double latDistance = Math.toRadians(lat2 - lat1);
	    Double lonDistance = Math.toRadians(lon2 - lon1);
	    Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c * 1000; // convert to meters

	    double height = el1 - el2;

	    distance = Math.pow(distance, 2) + Math.pow(height, 2);

	    return Math.sqrt(distance);
	}

}
