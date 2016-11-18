package de.komoot.photon.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.text.WordUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class CivicSQLScriptsGenerator {

	/** file, folder paths definition start **/
	// civic kml file (source: comune).
	private final static String comuneXMLFile = "civic_web/civici_web.kml";
	// mapping between kml and osm.
	private final static String mappingFile = "mapping/kml-osm-mapping.json";
	private final static String deleteStreetHouseFile = "mapping/delete_street_housenumber_placeid.csv";
		// script output folder.
	private static String scriptOutputFolder = "output-sql-scripts";
	// district script.
	private static String districtSQLScript = "/create_district.sql";
	// create trento houses.
	private static File createTrentoHousesSQLScriptFile = new File(
			System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder,
			"create_houses.sql");
	// house update.
	private static File updateHouseNoSQLScriptFile = new File(
			System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder,
			"update_house_numbers.sql");
	// street update.
	private static File updateStreetNameSQLScriptFile = new File(
			System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder,
			"update_street_names.sql");
	// delete duplicates.
	private static File deleteDuplicateStreets = new File(
			System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder,
			"delete_duplicate_placex.sql");
	/** file, folder paths definition end **/

	/** maps, configuration - start. **/
	private static Map<String, List<Node>> districtPlaceMarksMap = new HashMap<String, List<Node>>();
	private static Map<String, Integer> districtPlaceIdMap = new HashMap<String, Integer>();
	private static Map<String, List<String>> deleteStreetHouseMap = new HashMap<String, List<String>>();
	private static Map<String, List<String>> deleteStreetHouseToNameMap = new HashMap<String, List<String>>();
	private static Map<String, List<Integer>> deleteStreetHouseToPlaceIdMap = new HashMap<String, List<Integer>>();
	private static Map<String, List<String>> mapping = new HashMap<String, List<String>>();
	// new houses unique osm_id range start index.
	private static int placeId = 11111200;
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
	/** maps, configuration - end. **/

	private static ObjectMapper mapper = new ObjectMapper();
	private static List<String> missingStreet = new ArrayList<String>();
	private static int deleteCounter = 0;
	

	public void init() {
		try {

			File jsonFile = new File(
					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + mappingFile);

			mapping = mapper.readValue(jsonFile, Map.class);

			// read comune xml file.
			File file = new File(
					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + comuneXMLFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("Placemark");

			// read delete-street-housenumber file and create map.
			List<String> linesDSH = Files.asCharSource(new File(
					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + deleteStreetHouseFile),
					Charsets.UTF_8).readLines();
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
				
				// check for key=street_housenumber
				String streetHouseKey = street + "_" + str[1];
				if (deleteStreetHouseToPlaceIdMap.get(streetHouseKey) == null) {
					ArrayList<Integer> placeIds = new ArrayList<Integer>();
					deleteStreetHouseToPlaceIdMap.put(streetHouseKey, placeIds);
				}
				if (isInteger(str[2]))
				deleteStreetHouseToPlaceIdMap.get(streetHouseKey).add(Integer.valueOf(str[2].replace("\"", "")));
			}
			
			/** Generate sql per kml placemark. **/
			List<String> linesSql = new ArrayList<String>();
			List<String> linesUpdateHouseNumberSql = new ArrayList<String>();
			List<String> linesUpdateStreetNameSql = new ArrayList<String>();
			List<String> linesDeleteDuplicateSql = new ArrayList<String>();

			Set<String> distinceDistricts = getDistinctDistricts(nList);
			
			// update house number sql.
			linesUpdateHouseNumberSql.add("update public.placex as px set housenumber=c.housenumber from (values");
			linesUpdateStreetNameSql.add("update public.placex as px set street=c.street from (values");
			linesDeleteDuplicateSql.add("delete from public.placex where place_id in (");
			linesSql.add("DO $$");
			linesSql.add("BEGIN");
			for (String district : distinceDistricts) {
				gnerateSqlPerKmlPlace(district, nList, linesUpdateHouseNumberSql, linesUpdateStreetNameSql, linesSql, linesDeleteDuplicateSql);
			}
			// replace the last comma.
			String lastLineUpdateHouse = linesUpdateHouseNumberSql.get(linesUpdateHouseNumberSql.size() - 1);
			int indHouse = linesUpdateHouseNumberSql.get(linesUpdateHouseNumberSql.size()-1).lastIndexOf(",");
			if (indHouse >= 0)
				linesUpdateHouseNumberSql.set(linesUpdateHouseNumberSql.size() - 1, new StringBuilder(lastLineUpdateHouse).replace(indHouse, indHouse + 1, ")").toString());
			
			String lastLineUpdateStreet = linesUpdateStreetNameSql.get(linesUpdateStreetNameSql.size() - 1);
			int indStreet = linesUpdateStreetNameSql.get(linesUpdateStreetNameSql.size()-1).lastIndexOf(",");
			if (indStreet >= 0)
				linesUpdateStreetNameSql.set(linesUpdateStreetNameSql.size() - 1, new StringBuilder(lastLineUpdateStreet).replace(indStreet, indStreet + 1, ")").toString());

			String lastLineDeleteStreet = linesDeleteDuplicateSql.get(linesDeleteDuplicateSql.size() - 1);
			int indDeleteDuplicate = linesDeleteDuplicateSql.get(linesDeleteDuplicateSql.size()-1).lastIndexOf(",");
			if (indStreet >= 0)
				linesDeleteDuplicateSql.set(linesDeleteDuplicateSql.size() - 1, new StringBuilder(lastLineDeleteStreet).replace(indDeleteDuplicate, indDeleteDuplicate + 1, ")").toString());
			
			
			linesUpdateHouseNumberSql.add(" as c(housenumber, osm_id) where px.osm_id=c.osm_id");
			linesUpdateStreetNameSql.add(" as c(street, osm_id) where px.osm_id=c.osm_id");
			linesSql.add("END $$");
			
			// write output scripts.
			Files.asCharSink(updateHouseNoSQLScriptFile, Charsets.UTF_8).writeLines(linesUpdateHouseNumberSql);
			Files.asCharSink(updateStreetNameSQLScriptFile, Charsets.UTF_8).writeLines(linesUpdateStreetNameSql);
			Files.asCharSink(createTrentoHousesSQLScriptFile, Charsets.UTF_8).writeLines(linesSql);
			Files.asCharSink(deleteDuplicateStreets, Charsets.UTF_8).writeLines(linesDeleteDuplicateSql);
			ExportResource(districtSQLScript,
					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder);

			System.out.println("Done... (to be deleted duplicated houses:" + deleteCounter + ")");

			if (!missingStreet.isEmpty()) {
				System.err.println("###################### ATTENTION #########################");
				for (String missing : missingStreet) {
					System.out.println("missing street in mapping " + missing);
				}
				System.err.println("###################### --------- #########################");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private boolean isInteger(String string) {
		try {
			Integer.parseInt(string.replace("\"", ""));
			return true;
		} catch (Exception e) {
			return false;
		}
	}


//	public static void main(String args[]) {
//
//		try {
//
//			File jsonFile = new File(
//					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + mappingFile);
//
//			mapping = mapper.readValue(jsonFile, Map.class);
//
//			// read comune xml file.
//			File file = new File(System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + comuneXMLFile);
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			Document doc = dBuilder.parse(file);
//			doc.getDocumentElement().normalize();
//
//			NodeList nList = doc.getElementsByTagName("Placemark");
//
//			// read delete-street-housenumber file and create map.
//			List<String> linesDSH = Files.asCharSource(new File(
//					System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + deleteStreetHouseFile),
//					Charsets.UTF_8).readLines();
//			for (String line : linesDSH) {
//				String str[] = line.split(",");
//
//				// process street.
//				String street = str[0];
//				street = street.replace("'", "");
//
//				if (deleteStreetHouseMap.get(street) == null) {
//					ArrayList<String> houses = new ArrayList<String>();
//					deleteStreetHouseMap.put(street, houses);
//				}
//				deleteStreetHouseMap.get(street).add(str[1]);
//			}
//
//			/** Generate sql per kml placemark. **/
//			List<String> linesSql = new ArrayList<String>();
//			List<String> linesUpdateHouseNumberSql = new ArrayList<String>();
//			List<String> linesUpdateStreetNameSql = new ArrayList<String>();
//			List<String> linesDeleteDuplicate = new ArrayList<String>();
//
//			Set<String> distinceDistricts = getDistinctDistricts(nList);
//
//			// update house number sql.
//			linesUpdateHouseNumberSql.add("update public.placex as px set housenumber= c.housenumber from (values");
//			for (String district : distinceDistricts) {
//				gnerateSqlPerKmlPlace(district, nList, linesUpdateHouseNumberSql, linesUpdateStreetNameSql, linesSql, linesDeleteDuplicate);
//			}
//			linesUpdateHouseNumberSql.add(") as c(housenumber, osm_id) where px.osm_id=c.osm_id");
//			linesUpdateStreetNameSql.add(") as c(street, osm_id) where px.osm_id=c.osm_id");
//			linesSql.add("END $$");
//			
////			// update street name sql.
////			CivicSQLScriptsGenerator.resetPlaceIdCounter();
////			linesUpdateStreetNameSql.add("update public.placex as px set street= c.street from (values");
////			for (String district : distinceDistricts) {
////				gnerateSqlPerKmlPlace(district, nList, linesUpdateStreetNameSql, false, true, false);
////			}
////			
////
////			// create house sql.
////			resetPlaceIdCounter();
////			linesSql.add("DO $$");
////			linesSql.add("BEGIN");
////			for (String district : distinceDistricts) {
////				gnerateSqlPerKmlPlace(district, nList, linesSql, false, false, true);
////			}
//			
//
//			// write output scripts.
//			resetPlaceIdCounter();
//			Files.asCharSink(updateHouseNoSQLScriptFile, Charsets.UTF_8).writeLines(linesUpdateHouseNumberSql);
//			Files.asCharSink(updateStreetNameSQLScriptFile, Charsets.UTF_8).writeLines(linesUpdateStreetNameSql);
//			Files.asCharSink(createTrentoHousesSQLScriptFile, Charsets.UTF_8).writeLines(linesSql);
//			ExportResource(districtSQLScript, System.getenv("GEOCODER_DATA_HOME") + System.getProperty("file.separator") + scriptOutputFolder);
//
//			System.out.println("Done... (skipped duplicated houses:" + skippingCounter + ")");
//
//			if (!missingStreet.isEmpty()) {
//				System.err.println("###################### ATTENTION #########################");
//				for (String missing : missingStreet) {
//					System.out.println("missing street in mapping " + missing);
//				}
//				System.err.println("###################### --------- #########################");
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private static void gnerateSqlPerKmlPlace(String district, NodeList nList, List<String> linesUpdateHouseSQL,
			List<String> linesUpdateStreetNameSQL, List<String> linesSQL, List<String> deleteSQL) throws IOException {

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
		String isIn = "";

//		System.err.println("###############--" + district + "--##########");

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
						houseNumber = simpleDataNodes.item(2).getTextContent();
					String street = "";
					if (simpleDataNodes.item(3) != null) {
						street = simpleDataNodes.item(3).getTextContent().replaceAll("^\"|\"$", "");
						street = street.replace("'", "");
						street = street.toLowerCase();
						// if (street.equalsIgnoreCase("Via sommarive")) {
						// System.out.println("stop");
						// }
						// skip existing street+housenumber matches in osm.
						boolean delete = false;
						if (mapping.containsKey(street)) {

//							if (street.contains("piave")) {
//								System.err.println("debug");
//							}
							for (String possibleStreet : mapping.get(street)) {
								if (deleteStreetHouseMap.containsKey("\"" + possibleStreet + "\"")) {
									if (deleteStreetHouseMap.get("\"" + possibleStreet + "\"")
											.contains("\"" + houseNumber + "\"")) {
										// check further if this street+housenumber map has empty name
										String streetHouseKey = "\"" + possibleStreet + "\"" + "_" + "\"" + houseNumber + "\"";
										if (deleteStreetHouseToPlaceIdMap.containsKey(streetHouseKey)) {
											delete = true;
											for (Integer deletePlaceId: deleteStreetHouseToPlaceIdMap.get(streetHouseKey)){
												deleteSQL.add(deletePlaceId + ",");
											}
											break;
										} else {
											if (!missingStreet.contains(street)) {
												missingStreet.add(street);
											}
										}
									}
								}
							}
						} else {
							if (!missingStreet.contains(street)) {
								missingStreet.add(street);
							}

						}

						if (delete) {
							deleteCounter++;
//							continue;
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

							linesUpdateHouseSQL.add("('" + houseNumber + "'," + placeId + "),");
							linesUpdateStreetNameSQL.add("('" + WordUtils.capitalize(street) + "'," + placeId + "),");
							linesSQL.add("insert into public.placex "
										+ "(place_id,osm_id,osm_type,class,type,name,extratags,parent_place_id,isin,postcode,country_code,geometry,centroid,geometry_sector,rank_search,rank_address) "
										+ "VALUES(" + placeId + "," + placeId + ",'N'," + "'place','house',"
										+ "'\"name\"=>\""
										+ WordUtils.capitalize(street) + " "
										+ houseNumber + "\"',"
										+ "'\"state\"=>\"Trentino-Alto Adige/Sudtirol\",\"city\"=>\"Trento\",\"street\"=>\""
										+ WordUtils.capitalize(street)
										+ "\",\"housenumber\"=>\"" + houseNumber + "\"'," + parentPlaceId + ",'" + isIn
										+ "'," + cap + ",'it',ST_GeomFromText('POINT(" + coordinatesString
										+ ")', 4326)," + "ST_Centroid(ST_GeomFromText('POINT(" + coordinatesString
										+ ")', 4326)),28491461,28, 28);");
							placeId++;

						}
					}
				}
		}
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
	
	/**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     * @throws Exception
     */
    static public void ExportResource(String resourceName, String outputFolder) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        try {
            stream = Thread.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            File jarFolder = new File(outputFolder + System.getProperty("file.separator"));
            resStreamOut = new FileOutputStream(jarFolder + resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }

    }
    
    public static void resetPlaceIdCounter() {
    	placeId = 11111200;
    };

}