/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either   express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.geocoder.manager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.network.RemoteConnector;
import eu.trentorise.smartcampus.network.RemoteException;

/**
 * Geocoder based on the SC geocoder Web service. Performs the operations of
 * direct/reverse geocoding.
 * 
 * @author raman
 * 
 */

@Component
public class OSMGeocoder {

	private static final Logger LOGGER = Logger.getLogger(OSMGeocoder.class);
	@Autowired
	@Value("${geocoder.distance}")
	private double geocodeDistance;

	@Autowired
	@Value("${geocoder.path}")
	private String geocoderPath;

	@Autowired
	@Value("${geocoder.sfield}")
	private String spatialField;

	@Autowired
	@Value("${geocoder.rows}")
	private Integer defaultRows;

	@Autowired
	@Value("${geocoder.start}")
	private Integer defaultStart;

	public String getFromLocationNameUrl(String address,
			double[] referenceLocation, Double distance, Integer start,
			Integer rows, boolean prettyOutput, String token)
			throws RemoteException {
		String components = null;
		try {
			components = parseAddress(address);
		} catch (UnsupportedEncodingException e) {
		}

		return queryLocations(components, referenceLocation, distance, start,
				rows, prettyOutput, token, true);
	}

	private String queryLocations(String q, double[] referenceLocation,
			Double distance, Integer start, Integer rows, boolean prettyOutput,
			String token, boolean ordered) throws RemoteException {

		StringBuilder sb = new StringBuilder();
		sb.append(geocoderPath);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("wt", "json");

		params.put("omitHeader", false);
		if (prettyOutput) {
			params.put("indent", true);
		}
		params.put("q", q);

		if (start == null || start < 0) {
			start = defaultStart;
		}
		if (rows == null || rows < 0) {
			rows = defaultRows;
		}

		params.put("start", start);
		params.put("rows", rows);

		if (referenceLocation != null) {
			params.put("spatial", true);
			params.put("sfield", spatialField);
			params.put("sort", "geodist() asc");
			params.put("fq", "{!geofilt}");
			params.put("pt", referenceLocation[0] + "," + referenceLocation[1]);

			// default distance value
			if (distance == null || distance < 0) {
				distance = geocodeDistance;
			}
			params.put("d", distance);
		}

		if (ordered) {
			params.put("defType", "edismax");
			params.put("bq", "osm_key:highway^100 osm_value:bus_stop^-10");
		}

		return geocoderPath + generateQueryString(params);

	}

	private String parseAddress(String address)
			throws UnsupportedEncodingException {
		String q = "";
		String[] tokens = address.split(",");
		String[] subtokens = tokens[0].split(" ");
		String nq = "name:(", sq = "street:(";
		for (String subtoken : subtokens) {
			nq += " +" + subtoken.trim();
			sq += " +" + subtoken.trim();
		}
		nq += ")";
		sq += ")";
		q += "+(" + nq + " OR " + sq + ") ";
		if (tokens.length >= 3 && tokens[1] != null
				&& tokens[1].trim().length() > 0) {
			q += "+housenumber:" + tokens[1].trim() + " ";
		}
		if (tokens.length == 2 && tokens[1] != null
				&& tokens[1].trim().length() > 0) {
			if (Character.isDigit(tokens[1].trim().toCharArray()[0])) {
				q += "+housenumber:" + tokens[1].trim() + " ";
			} else {
				q += "+city:" + tokens[1].trim() + " ";
			}
		}
		if (tokens.length >= 3 && tokens[2] != null
				&& tokens[2].trim().length() > 0) {
			q += "+city:" + tokens[2].trim() + " ";
		}
		return q;
	}

	public String getFromLocationUrl(double lat, double lng, Double distance,
			Integer start, Integer rows, boolean prettyOutput, String token)
			throws RemoteException {
		String q = "*:*";
		return queryLocations(q, new double[] { lat, lng }, distance, start,
				rows, prettyOutput, token, false);
	}

	private String execute(String server, String query,
			Map<String, Object> params, String token) throws RemoteException {
		try {
			// fix because RemoteConnector encode input/output only in UTF-8
			return new String(RemoteConnector.getJSON(server, query, token,
					params).getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Error encoding result in ISO-8859-1");
			return "{}";
		}
	}

	private static String generateQueryString(Map<String, Object> parameters) {
		String queryString = "?";
		if (parameters != null) {
			for (String param : parameters.keySet()) {
				Object value = parameters.get(param);
				if (value == null) {
					if (queryString.length() > 1) {
						queryString += "&";
					}
					queryString += param + "=";
				} else if (value instanceof List) {
					for (Object v : ((List<?>) value)) {
						if (queryString.length() > 1) {
							queryString += "&";
						}
						queryString += param + "=" + encodeValue(v.toString());
					}
				} else {
					if (queryString.length() > 1) {
						queryString += "&";
					}
					queryString += param + "=" + encodeValue(value.toString());
				}

			}
		}
		return queryString.length() > 1 ? queryString : "";
	}

	private static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, "utf8");
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}
}
