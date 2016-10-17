package de.komoot.photon;

import java.util.Comparator;

import org.json.JSONObject;

import de.komoot.photon.Constants;

public class PlaceMarkComparator implements Comparator<JSONObject> {

	@Override
	public final int compare(final JSONObject o1, final JSONObject o2) {

		if (o1.get(Constants.OSM_VALUE) != null
				&& String.valueOf(Constants.OSM_VALUE).equalsIgnoreCase(Constants.CIVIC_TAG)
				&& o2.get(Constants.OSM_VALUE) != null
				&& !o2.get(Constants.OSM_VALUE).toString().equalsIgnoreCase(Constants.CIVIC_TAG)) {
			return 1;
		} else if (o2.get(Constants.OSM_VALUE) != null
				&& String.valueOf(o1.get(Constants.OSM_VALUE)).equalsIgnoreCase(Constants.CIVIC_TAG)) {
			return -1;
		} else {
			return 0;
		}

	}

}
