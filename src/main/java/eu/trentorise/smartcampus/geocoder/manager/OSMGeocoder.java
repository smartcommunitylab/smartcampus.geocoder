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
import java.util.HashMap;
import java.util.Map;

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

	@Autowired
	@Value("${geocoder.distance}")
	private double geocodeDistance;

	@Autowired
	@Value("${geocoder.host}")
	private String geocoderHost;

	@Autowired
	@Value("${geocoder.path}")
	private String geocoderPath;

	@Autowired
	@Value("${geocoder.sfield}")
	private String spatialField;

	public String getFromLocationName(String address,
			double[] referenceLocation, Double distance, boolean prettyOutput,
			String token) throws RemoteException {
		String components = null;
		try {
			components = parseAddress(address);
		} catch (UnsupportedEncodingException e) {
		}

		return queryLocations(components, referenceLocation, distance,
				prettyOutput, token);
	}

	private String queryLocations(String q, double[] referenceLocation,
			Double distance, boolean prettyOutput, String token)
			throws RemoteException {

		StringBuilder sb = new StringBuilder();
		sb.append(geocoderPath);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("wt", "json");

		params.put("omitHeader", true);
		if (prettyOutput) {
			params.put("indent", true);
		}
		params.put("q", q);

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

		return execute(geocoderHost, sb.toString(), params, token);

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

	public String getFromLocation(double lat, double lng, Double distance,
			boolean prettyOutput, String token) throws RemoteException {
		String q = "*:*";
		return queryLocations(q, new double[] { lat, lng }, distance,
				prettyOutput, token);
	}

	private String execute(String server, String query,
			Map<String, Object> params, String token) throws RemoteException {
		return RemoteConnector.getJSON(server, query, token, params);
	}

}
