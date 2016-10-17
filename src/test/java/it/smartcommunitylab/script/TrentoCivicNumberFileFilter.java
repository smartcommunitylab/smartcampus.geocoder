package it.smartcommunitylab.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class TrentoCivicNumberFileFilter {

	private final static String nominatimCSVFile = "C:/daily-work/nominatim/csv/trento-houses.csv";
	/** civic kml file (source: comune). **/
	private final static String comuneXMLFile = "civici_web/civici_web.kml";
	/** mapping between kml and osm. **/
	private final static String mappingFile = "mapping/kml-osm-mapping.json";
	private final static String deleteStreetHouseFile = "mapping/delete_street_housenumber.csv";
	private static ObjectMapper mapper = new ObjectMapper();

	// sets.
	private static Set<String> kmlSet = new HashSet<String>();
	private static Set<String> osmNominatimSet = new HashSet<String>();
	private static Set<String> kmlUnMatchedSet = new HashSet<String>();
	private static Set<String> osmNominatimMatchedSet = new HashSet<String>();
	private static Set<Map<String, String>> kmlUnMatchedNodeSet = new HashSet<Map<String, String>>();
	// maps.
	private static Map<String, List<Node>> districtPlaceMarksMap = new HashMap<String, List<Node>>();
	private static Map<String, Integer> districtPlaceIdMap = new HashMap<String, Integer>();
	private static Map<String, List<String>> deleteStreetHouseMap = new HashMap<String, List<String>>();
	private static Map<String, List<String>> mapping = new HashMap<String, List<String>>();

	/** place_id. **/
	static {
		districtPlaceIdMap.put("montevaccino", 1111100);
		districtPlaceIdMap.put("villamontagna", 1111101);
		districtPlaceIdMap.put("baselga del bondone", 1111102);
		districtPlaceIdMap.put("vigolo baselga", 1111103);
		districtPlaceIdMap.put("ravina", 1111104);
		districtPlaceIdMap.put("sopramonte", 1111105);
		districtPlaceIdMap.put("romagnano", 1111106);
		districtPlaceIdMap.put("meano", 1111107);
		districtPlaceIdMap.put("villazzano", 1111108);
		districtPlaceIdMap.put("sardagna", 1111109);
		districtPlaceIdMap.put("cognola", 1111110);
		districtPlaceIdMap.put("cadine", 11111111);
		districtPlaceIdMap.put("trento", 11111112);
		districtPlaceIdMap.put("gardolo", 11111113);
		districtPlaceIdMap.put("mattarello", 11111114);
		districtPlaceIdMap.put("povo", 11111115);
	}

	private static List<String> missingStreet = new ArrayList<String>();

	private static int placeId = 11111200;
	private static int skippingCounter = 0;

	/** sql file. **/
	// house update.
	// private static File sqlFile = new File("C:/daily-work/nominatim/scripts", "update_trento_houses.sql");
	private static boolean updateHouse = false;
	// street update.
	private static File sqlFile = new File("C:/daily-work/nominatim/scripts", "update_trento_streetNames.sql");
	private static boolean updateStreets = true;

	private static boolean identifyMatchingStreets = false;
	private static boolean identifyDistinctParams = false;
	private static boolean generateSqlPerKmlPlace = true;

	public static void main(String args[]) {

		try {

			File jsonFile = new File(mappingFile);

			mapping = mapper.readValue(jsonFile, Map.class);

			// read comune xml file.
			InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(comuneXMLFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fis);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("Placemark");

			// read nominatim csv file.
			List<String> lines = Files.asCharSource(new File(nominatimCSVFile), Charsets.UTF_8).readLines();
			osmNominatimSet = initializeStreetList(lines);

			// read delete-street-housenumber file and create map.
			List<String> linesDSH = Files.asCharSource(new File(deleteStreetHouseFile), Charsets.UTF_8).readLines();
			for (String line : linesDSH) {
				String str[] = line.split(",");

				// process street.
				String street = str[0];
				street = street.replace("'", "");

				if (deleteStreetHouseMap.get(street) == null) {
					ArrayList<String> houses = new ArrayList<String>();
					deleteStreetHouseMap.put(street, houses);
				}
				deleteStreetHouseMap.get(street).add(str[1]);
			}

			/** Generate sql per kml placemark. **/
			if (generateSqlPerKmlPlace) {

				List<String> linesSql = new ArrayList<String>();
				Set<String> distinceDistricts = getDistinctDistricts(nList);

				if (updateHouse) {
					linesSql.add("update public.placex as px set housenumber= c.housenumber from (values");
					for (String district : distinceDistricts) {
						gnerateSqlPerKmlPlace(district, nList, linesSql);
					}
					linesSql.add(") as c(housenumber, osm_id) where px.osm_id=c.osm_id");
				} else if (updateStreets) {
					linesSql.add("update public.placex as px set street= c.street from (values");
					for (String district : distinceDistricts) {
						gnerateSqlPerKmlPlace(district, nList, linesSql);
					}
					linesSql.add(") as c(street, osm_id) where px.osm_id=c.osm_id");
				} else {
					linesSql.add("DO $$");
					linesSql.add("BEGIN");
					for (String district : distinceDistricts) {
						gnerateSqlPerKmlPlace(district, nList, linesSql);
					}
					linesSql.add("END $$");

				}

				Files.asCharSink(sqlFile, Charsets.UTF_8).writeLines(linesSql);
				// gnerateSqlPerKmlPlace("povo", nList, linesSql);
				System.out.println("Duplicates Houses:" + skippingCounter);

				if (!missingStreet.isEmpty()) {
					System.err.println("###################### ATTENTION #########################");
					for (String missing : missingStreet) {
						System.out.println("missing street in mapping " + missing);
					}
					System.err.println("###################### --------- #########################");
				}

			} else if (identifyDistinctParams) {
				/**
				 * Get distinct information
				 * (district, city, street etc) from kml file.
				**/

				Set<String> distinceDistricts = getDistinctDistricts(nList);
				Set<String> distinceStreets = getDistinctStreets(nList, mapping);

				// for (String district : distinceDistricts) {
				// System.err.println(district);
				// }

				for (String street : distinceStreets) {
					System.out.println(street);
				}

			} else if (identifyMatchingStreets) { 
				/**
				* identify matching streets between kml and osm/nominatim.
			    **/

				/**
				 * loop through xml file Skip secondary entrance. Skip where
				 * street name matches one of the street name in CSV file. print
				 * rest. print total in the end.
				 **/

				System.out.println("----------------------------");

				int validNodes = 0;
				int matched = 0;
				int unmatched = 0;

				for (int temp = 0; temp < nList.getLength(); temp++) { // 39083

					Node nNode = nList.item(temp);

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {

						Element eElement = (Element) nNode;

						NodeList extendedElements = (NodeList) eElement.getElementsByTagName("ExtendedData").item(0);

						Element schemaDataElements = (Element) extendedElements.item(0);

						NodeList simpleDataNodes = (NodeList) schemaDataElements.getElementsByTagName("SimpleData");

						if (simpleDataNodes.item(6).getTextContent().equalsIgnoreCase("principale")) { // 25598
							String streetName = simpleDataNodes.item(3).getTextContent().toLowerCase()
									.replaceAll("^\"|\"$", "");
							// if (streetName.equalsIgnoreCase("piazza del
							// \"tridente")) {
							// System.out.print("");
							// }
							kmlSet.add(streetName);

							if (!osmNominatimSet.contains(streetName) && !mapping.containsKey(streetName)) {
								kmlUnMatchedSet.add(streetName);
								unmatched++;
							} else {

								if (mapping.get(streetName) != null && mapping.get(streetName).isEmpty()) {

									Map<String, String> tmp = new HashMap<String, String>();

									if (eElement.getElementsByTagName("name").item(0) != null)
										tmp.put("Name", eElement.getElementsByTagName("name").item(0).getTextContent());
									if (eElement.getElementsByTagName("description").item(0) != null)
										tmp.put("Description",
												eElement.getElementsByTagName("description").item(0).getTextContent());
									if (simpleDataNodes.item(0) != null)
										tmp.put("civico_num", simpleDataNodes.item(0).getTextContent().toLowerCase());
									if (simpleDataNodes.item(1) != null)
										tmp.put("civico_let", simpleDataNodes.item(1).getTextContent().toLowerCase());
									if (simpleDataNodes.item(2) != null)
										tmp.put("civico_alf", simpleDataNodes.item(2).getTextContent().toLowerCase());
									if (simpleDataNodes.item(3) != null)
										tmp.put("desvia", simpleDataNodes.item(3).getTextContent().toLowerCase());
									if (simpleDataNodes.item(4) != null)
										tmp.put("strada", simpleDataNodes.item(4).getTextContent().toLowerCase());
									if (simpleDataNodes.item(5) != null)
										tmp.put("cap", simpleDataNodes.item(5).getTextContent().toLowerCase());
									if (simpleDataNodes.item(6) != null)
										tmp.put("tipo_num", simpleDataNodes.item(6).getTextContent().toLowerCase());
									if (simpleDataNodes.item(7) != null)
										tmp.put("tipo_en", simpleDataNodes.item(7).getTextContent().toLowerCase());
									if (simpleDataNodes.item(8) != null)
										tmp.put("ingresso", simpleDataNodes.item(8).getTextContent().toLowerCase());
									if (simpleDataNodes.item(9) != null)
										tmp.put("ingr_en", simpleDataNodes.item(9).getTextContent().toLowerCase());
									if (simpleDataNodes.item(10) != null)
										tmp.put("url", simpleDataNodes.item(10).getTextContent().toLowerCase());
									if (simpleDataNodes.item(11) != null)
										tmp.put("sobborgo", simpleDataNodes.item(11).getTextContent().toLowerCase());

									if (eElement.getElementsByTagName("Point").item(0).getFirstChild() != null) {
										String[] coordinates = eElement.getElementsByTagName("Point").item(0)
												.getFirstChild().getTextContent().split(",");
										if (coordinates.length == 2) {
											tmp.put("lat", coordinates[1]);
											tmp.put("lon", coordinates[0]);
											// add only when there is
											// coordinates.
											kmlUnMatchedNodeSet.add(tmp);
										}
									}
								}
								matched++;
								osmNominatimMatchedSet.add(streetName);
							}
						}
					}
				}

				System.out.println("matched->" + matched);
				System.out.println("unmatched->" + unmatched);

				System.err.println("Placemarks of kml file missing in nominatim/osm.");
				int placeId = 5555555;
				for (Map<String, String> map : kmlUnMatchedNodeSet) {

					String houseNumber = "";
					if (map.containsKey("civico_num"))
						houseNumber = map.get("civico_num");

					String street = "";
					if (map.containsKey("desvia"))
						street = map.get("desvia");

					String addrPlace = street + " " + houseNumber;

					String postCode = "";
					if (map.containsKey("cap"))
						postCode = map.get("cap");

					String isIn = "Trento";
					if (map.containsKey("sobborgo"))
						isIn = map.get("sobborgo");

					String lon = map.get("lon");
					String lat = map.get("lat");

					// "place_id","partition","osm_type","osm_id","class","type","name","admin_level","housenumber","street","addr_place","isin","postcode","country_code","extratags","geometry","parent_place_id","linked_place_id","rank_address","rank_search","importance","indexed_status","indexed_date","wikipedia","geometry_sector","calculated_country_code","centroid"
					System.err.println(
							"insert into public.placex ('place_id', 'osm_type', 'class', 'type', 'housenumber', 'street', 'addr_place', 'isin', 'postcode', 'country_code', 'geometry')"
									+ " values(" + placeId++ + ", 'N', 'place', 'house', '" + houseNumber + "', '"
									+ street + "', '" + addrPlace + " ', '" + isIn + "', '" + postCode + "', 'it', '"
									+ "ST_GeomFromText('POINT(" + lon + " " + lat + ")', 4326))");
				}

				// System.err.println("Unmatched streets in kml file.");
				// for (String unmatchStreet : kmlUnMatchedSet) {
				// System.err.println("\"" + unmatchStreet + "\": " + "[]");
				// }
				//
				// System.out.println("Matched streets in kml file.");
				// Set<String> tmp = kmlSet;
				// tmp.removeAll(kmlUnMatchedSet);
				// for (String matchStreet : tmp) {
				// System.out.println("\"" + matchStreet + "\": " + "[" + "\"" +
				// matchStreet + "\"],");
				// }
				//
				// System.out.println("Unmatched streets in osm/nominatim");
				// Set<String> tmp2 = osmNominatimSet;
				// tmp2.removeAll(osmNominatimMatchedSet);
				// for (String unMatchedNominatimStreet : tmp2) {
				// System.err.println(unMatchedNominatimStreet);
				// }

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void gnerateSqlPerKmlPlace(String district, NodeList nList, List<String> linesSQL)
			throws IOException {

		List<Node> placeMarks = new ArrayList<Node>();

		for (int temp = 0; temp < nList.getLength(); temp++) { // 39083

			Node nNode = nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				NodeList extendedElements = (NodeList) eElement.getElementsByTagName("ExtendedData").item(0);

				Element schemaDataElements = (Element) extendedElements.item(0);

				NodeList simpleDataNodes = (NodeList) schemaDataElements.getElementsByTagName("SimpleData");

				if (simpleDataNodes.item(6).getTextContent().equalsIgnoreCase("principale")) { // 25598
					if (simpleDataNodes.item(11) != null
							&& simpleDataNodes.item(11).getTextContent().toLowerCase().equalsIgnoreCase(district))

						placeMarks.add(nNode);

				}
			}
		}

		districtPlaceMarksMap.clear();
		districtPlaceMarksMap.put(district, placeMarks);

		int parentPlaceId = districtPlaceIdMap.get(district);
		int rankAddress = 31;
		int rankSearch = 31;
		String isIn = "";

		System.err.println("###############--" + district + "--##########");
	
		// update query.
		String mapQuery = "";

		for (String dist : districtPlaceMarksMap.keySet()) {

			List<Node> placesMarkLists = districtPlaceMarksMap.get(dist);

			for (Node nNode : placesMarkLists)

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					NodeList extendedElements = (NodeList) eElement.getElementsByTagName("ExtendedData").item(0);

					Element schemaDataElements = (Element) extendedElements.item(0);

					NodeList simpleDataNodes = (NodeList) schemaDataElements.getElementsByTagName("SimpleData");

					String name = "";
					if (eElement.getElementsByTagName("name").item(0) != null)
						name = eElement.getElementsByTagName("name").item(0).getTextContent();
					String houseNumber = "";
					if (simpleDataNodes.item(0) != null)
						houseNumber = simpleDataNodes.item(0).getTextContent();
					String street = "";
					if (simpleDataNodes.item(3) != null) {
						street = simpleDataNodes.item(3).getTextContent().replaceAll("^\"|\"$", "");
						street = street.replace("'", "");
						street = street.toLowerCase();
						// if (street.equalsIgnoreCase("Via sommarive")) {
						// System.out.println("stop");
						// }
						// skip existing street+housenumber matches in osm.
						boolean duplicate = false;
						if (mapping.containsKey(street)) {

							for (String possibleStreet : mapping.get(street)) {
								if (deleteStreetHouseMap.containsKey("\"" + possibleStreet + "\"")) {
									if (deleteStreetHouseMap.get("\"" + possibleStreet + "\"")
											.contains("\"" + houseNumber + "\"")) {
										// System.out.println("SKIPPING - " +
										// street + " " + houseNumber);
										duplicate = true;
										break;
									}
								}
							}
						} else {
							if (!missingStreet.contains(street)) {
								missingStreet.add(street);
							}

						}

						if (duplicate) {
							skippingCounter++;
							continue;
						}

					}
					String cap = "";
					if (simpleDataNodes.item(5) != null)
						cap = simpleDataNodes.item(5).getTextContent();
					String frazione = "";
					if (simpleDataNodes.item(11) != null)
						frazione = simpleDataNodes.item(11).getTextContent().toLowerCase();
					String url = "";
					if (simpleDataNodes.item(10) != null) {
						url = simpleDataNodes.item(10).getTextContent();
					}

					String coordinatesString = "";
					if (eElement.getElementsByTagName("Point").item(0).getFirstChild() != null) {
						String[] coordinates = eElement.getElementsByTagName("Point").item(0).getFirstChild()
								.getTextContent().split(",");
						if (coordinates.length == 2) {
							coordinatesString = coordinates[0] + " " + coordinates[1];
							street = street.replaceAll("\"", "");

							if (updateHouse)
								mapQuery = mapQuery + "('" + houseNumber + "'," + placeId + "),";
							else if (updateStreets)
								mapQuery = mapQuery + "('"
										+ CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, street) + "',"
										+ placeId + "),";
							else {
								String temp = ("insert into public.placex "
										+ "(place_id,osm_id,osm_type,class,type,name,extratags,parent_place_id,isin,postcode,country_code,geometry,centroid,geometry_sector) "
										+ "VALUES(" + placeId + "," + placeId + ",'N'," + "'building','civic',"
										+ "'\"name\"=>\""
										+ CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, street) + " "
										+ houseNumber + "\"',"
										+ "'\"state\"=>\"Trentino-Alto Adige/Sudtirol\",\"city\"=>\"Trento\",\"street\"=>\""
										+ CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, street)
										+ "\",\"housenumber\"=>\"" + houseNumber + "\"'," + parentPlaceId + ",'" + isIn
										+ "'," + cap + ",'it',ST_GeomFromText('POINT(" + coordinatesString
										+ ")', 4326)," + "ST_Centroid(ST_GeomFromText('POINT(" + coordinatesString
										+ ")', 4326)),28491461);");
								linesSQL.add(temp);
							}
							placeId++;

						}
					}
				}
		}

		linesSQL.add(mapQuery);

	}

	private static Set<String> getDistinctStreets(NodeList nList, Map<String, List<String>> mapping) {
		Set<String> distinct = new HashSet<String>();

		for (int temp = 0; temp < nList.getLength(); temp++) { // 39083

			Node nNode = nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				NodeList extendedElements = (NodeList) eElement.getElementsByTagName("ExtendedData").item(0);

				Element schemaDataElements = (Element) extendedElements.item(0);

				NodeList simpleDataNodes = (NodeList) schemaDataElements.getElementsByTagName("SimpleData");

				if (simpleDataNodes.item(6).getTextContent().equalsIgnoreCase("principale")) { // 25598

					if (simpleDataNodes.item(3) != null) {
						String streetName = simpleDataNodes.item(3).getTextContent().toLowerCase();
						if (!osmNominatimSet.contains(streetName) | !mapping.containsKey(streetName)
								| (mapping.get(streetName) != null && mapping.get(streetName).isEmpty())) {
							distinct.add(streetName);
						}
					}

				}
			}
		}

		return distinct;
	}

	private static Set<String> getDistinctDistricts(NodeList nList) {
		Set<String> distinct = new HashSet<String>();

		for (int temp = 0; temp < nList.getLength(); temp++) { // 39083

			Node nNode = nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				NodeList extendedElements = (NodeList) eElement.getElementsByTagName("ExtendedData").item(0);

				Element schemaDataElements = (Element) extendedElements.item(0);

				NodeList simpleDataNodes = (NodeList) schemaDataElements.getElementsByTagName("SimpleData");

				if (simpleDataNodes.item(6).getTextContent().equalsIgnoreCase("principale")) { // 25598
					if (simpleDataNodes.item(11) != null)
						distinct.add(simpleDataNodes.item(11).getTextContent().toLowerCase());

				}
			}
		}

		return distinct;
	}

	private static Set<String> initializeStreetList(List<String> lines) {
		Set<String> converted = new HashSet<String>();
		String[][] table = new String[lines.size()][];
		for (int i = 0; i < lines.size(); i++) {
			table[i] = StringUtils.commaDelimitedListToStringArray(lines.get(i));
		}
		String[] headings = table[0];
		for (int i = 1; i < headings.length; i++) {
			for (int j = 1; j < table.length; j++) {
				String streetName = table[j][11].toLowerCase().replaceAll("^\"|\"$", "");
				if (!streetName.isEmpty())
					converted.add(streetName);
			}
		}

		return converted;

	}

}

class Mapping {

	private String streetName;
	private List<String> mappedList;

	public Mapping(String streetName, List<String> mappedList) {
		super();
		this.streetName = streetName;
		this.mappedList = mappedList;
	}

	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public List<String> getMappedList() {
		return mappedList;
	}

	public void setMappedList(List<String> mappedList) {
		this.mappedList = mappedList;
	}

}
