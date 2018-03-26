package tr.edu.ege.ssd.measurer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Paths {

	static final String RESS_PATH = "short_index.txt";
	static String HOST = "http://localhost:8080/img/";
	// static String HOST = "http://192.168.1.26:8080/img/";

	static List<String> WELLCOME_URIs;
	static HashMap<String, List<String>> CATEGORY_URIs;
	static List<String> ADVERS_URIs;
	static HashMap<String, List<String>> RECC_URIs;
	static List<String> ALL_URIs;

	static int IMAGE_COUNT_ON_EACH_PAGE = 10;

	static int CATEGORY_COUNT = 10;
	static int IMAGE_COUNT_IN_CATEGORY = 100;
	static int RECOMMENDATION_COUNT = 10;
	static int ADVERS_COUNT = 10;

	static public List<String> defineAdversURIs(List<String> allUris) {
		List<String> wellcomePageURIs = new ArrayList<String>();
		for (int i = 0; i < Paths.ADVERS_COUNT; i++) {
			wellcomePageURIs.add(allUris.get((int) randomWithRange(0, 5000)));
		}
		return wellcomePageURIs;
	}

	static public HashMap<String, List<String>> defineCategoryURIs(List<String> allUris) {
		HashMap<String, List<String>> categoryURIs = new HashMap<String, List<String>>();
		for (int category = 0; category < Paths.CATEGORY_COUNT; category++) {
			List<String> uris = new ArrayList<String>();
			for (int i = 0; i < Paths.IMAGE_COUNT_IN_CATEGORY; i++) {
				uris.add(allUris.get((int) randomWithRange(100, 5000)));
			}
			categoryURIs.put(Integer.toString(category), uris);
		}
		return categoryURIs;
	}

	static HashMap<String, List<String>> defineReccURIs(HashMap<String, List<String>> categoryURIs,
			List<String> allURIs) {
		HashMap<String, List<String>> reccURIs = new HashMap<String, List<String>>();
		Collection<List<String>> values = categoryURIs.values();
		for (List<String> list : values) {
			for (String image : list) {
				List<String> reccsPerURIs = new ArrayList<String>();
				for (int i = 0; i < Paths.RECOMMENDATION_COUNT; i++) {
					reccsPerURIs.add(allURIs.get((int) randomWithRange(100, 1000)));
				}
				reccURIs.put(image, reccsPerURIs);
			}
		}
		return reccURIs;
	}

	static public List<String> defineWellcomePageURIs(List<String> allUris) {
		List<String> wellcomePageURIs = new ArrayList<String>();
		for (int i = 0; i < Paths.IMAGE_COUNT_ON_EACH_PAGE; i++) {
			wellcomePageURIs.add(allUris.get((int) randomWithRange(0, 200)));
		}
		return wellcomePageURIs;
	}

	static List<String> getAllURIs() throws Exception {
		if (ALL_URIs == null)
			loadURIs();
		return ALL_URIs;
	}

	static List<String> loadURIs() throws Exception {
		List<String> uris = new ArrayList<>();
		InputStream resourceStream = Paths.class.getResourceAsStream("/" + RESS_PATH);
		InputStreamReader sreader;
		if (resourceStream == null)
			sreader = new FileReader(new File("./files/index.txt"));
		else
			sreader = new InputStreamReader(resourceStream);
		BufferedReader reader = new BufferedReader(sreader);
		String line = null;
		int i = 0;
		while ((line = reader.readLine()) != null && i++ < 5000) {
			// String[] urisinline = line.split("##");
			// if (urisinline.length > 0)
			// uris.add(urisinline[0]);
			uris.add(HOST + line);
		}
		reader.close();
		System.out.println(String.format("%d uri loaded.", uris.size()));
		ALL_URIs = uris;
		return uris;
	}

	static void prepareURIs() throws Exception {
		List<String> allURIs = Paths.loadURIs();
		System.out.println("Preparing sample uris");
		WELLCOME_URIs = Paths.defineWellcomePageURIs(allURIs);
		CATEGORY_URIs = Paths.defineCategoryURIs(allURIs);
		ADVERS_URIs = Paths.defineAdversURIs(allURIs);
		RECC_URIs = Paths.defineReccURIs(CATEGORY_URIs, allURIs);
	}

	static String randomCategory() {
		return Integer.toString((int) randomWithRange(0, CATEGORY_COUNT));
	}

	static double randomWithRange(double min, double max) {
		double range = (max - min);
		return (Math.random() * range) + min;
	}

	static List<String> getRandomCategory() {
		return CATEGORY_URIs.get(Integer.toString((int) randomWithRange(0, 10)));
	}
}