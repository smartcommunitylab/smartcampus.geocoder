package de.komoot.photon.query;

import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

import de.komoot.photon.Utils;
import spark.Request;

/**
 *
 * @author svantulden
 */
public class ReverseRequestFactory {
	private final LanguageChecker languageChecker;
	private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	public ReverseRequestFactory(Set<String> supportedLanguages) {
		this.languageChecker = new LanguageChecker(supportedLanguages);
	}

	public <R extends ReverseRequest> R create(Request webRequest) throws BadRequestException {
		String language = webRequest.queryParams("lang");
		language = language == null ? "en" : language;
		languageChecker.apply(language);

		Point location = null;
		try {
			Double lon = Double.valueOf(webRequest.queryParams("lon"));
			Double lat = Double.valueOf(webRequest.queryParams("lat"));
			location = geometryFactory.createPoint(new Coordinate(lon, lat));
		} catch (Exception nfe) {
			throw new BadRequestException(400, "missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
		}

		ReverseRequest reverseRequest = new ReverseRequest(location, language);

		return (R) reverseRequest;
	}

	public <R extends ReverseRequest> R createSCRequest(Request webRequest) throws BadRequestException {
		String language = webRequest.queryParams("lang");
		language = language == null ? "en" : language;
		languageChecker.apply(language);

		Point location = null;
		Double lat, lon;
		try {
			if (webRequest.queryParams("latlng") != null) {
				String latlng = webRequest.queryParams("latlng");
				double[] referenceLocation = Utils.extractCoords(latlng);
				lat = referenceLocation[0];
				lon = referenceLocation[1];
				location = geometryFactory.createPoint(new Coordinate(lon, lat));
			}

		} catch (Exception nfe) {
			throw new BadRequestException(400, "missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
		}

		ReverseRequest reverseRequest = new ReverseRequest(location, language);

		return (R) reverseRequest;
	}

}
