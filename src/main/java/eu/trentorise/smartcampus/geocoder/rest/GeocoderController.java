package eu.trentorise.smartcampus.geocoder.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.geocoder.manager.OSMGeocoder;
import eu.trentorise.smartcampus.network.RemoteException;

@Controller
public class GeocoderController {

	@Autowired
	OSMGeocoder geocoder;

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/location")
	public @ResponseBody
	String getFromLocation(HttpServletResponse response,
			@RequestParam String latlng,
			@RequestParam(required = false) Double distance,
			@RequestParam(required = false) boolean prettyOutput,
			@RequestParam(required = false) Integer rows,
			@RequestParam(required = false) Integer start)
			throws RemoteException {
		try {
			double[] referenceLocation = extractCoords(latlng);
			return geocoder.getFromLocation(referenceLocation[0],
					referenceLocation[1], distance, start, rows, prettyOutput,
					null);
		} catch (IllegalArgumentException e) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Parameter error syntax");
			} catch (IOException ioe) {
			}
		}

		return null;
	}

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/address")
	public @ResponseBody
	String getFromAddress(HttpServletResponse response,
			@RequestParam String address,
			@RequestParam(required = false) String latlng,
			@RequestParam(required = false) Double distance,
			@RequestParam(required = false) boolean prettyOutput,
			@RequestParam(required = false) Integer rows,
			@RequestParam(required = false) Integer start)
			throws RemoteException {

		try {
			double[] referenceLocation = extractCoords(latlng);
			return geocoder.getFromLocationName(address, referenceLocation,
					distance, start, rows, prettyOutput, null);
		} catch (IllegalArgumentException e) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Parameter error syntax");
			} catch (IOException ioe) {
			}
		}
		return null;
	}

	private double[] extractCoords(String latlng)
			throws IllegalArgumentException {
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
}
