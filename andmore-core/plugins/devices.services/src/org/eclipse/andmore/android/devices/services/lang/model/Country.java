/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.devices.services.lang.model;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Country representation
 * 
 * The list of countries has been downloaded from:
 * http://www.iso.org/iso/country_codes/iso_3166_code_lists.htm
 */
public class Country {

	private static final String FILE_PATH = "resources/iso_3166-1_list_en.xml";

	private static final String COUNTRY_NODE = "ISO_3166-1_Entry";

	private static final String COUNTRY_NAME = "ISO_3166-1_Country_name";

	private static final String COUNTRY_ID = "ISO_3166-1_Alpha-2_Code_element";

	private static List<Country> countryList = null;

	private static Map<String, Country> countryMap = null;

	private String name;

	private String id;

	/**
	 * Get the countries list
	 * 
	 * @return the countries list
	 */
	public static List<Country> getCountryList() {
		if (countryList == null) {
			loadCountries();
		}
		return countryList;
	}

	/*
	 * Load countries from XML file
	 */
	private static void loadCountries() {
		countryList = new ArrayList<Country>();
		countryMap = new HashMap<String, Country>();

		URL countriesURL = DeviceServicesPlugin.getDefault().getBundle().getResource(FILE_PATH);

		try {

			InputStream countriesIS = countriesURL.openStream();

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(countriesIS);

			NodeList list = document.getDocumentElement().getElementsByTagName(COUNTRY_NODE);

			for (int i = 0; i < list.getLength(); i++) {

				Element countryNode = (Element) list.item(i);

				String name = countryNode.getElementsByTagName(COUNTRY_NAME).item(0).getChildNodes().item(0)
						.getNodeValue();
				String ID = countryNode.getElementsByTagName(COUNTRY_ID).item(0).getChildNodes().item(0).getNodeValue();

				if (((ID != null) && (!ID.equals(""))) && ((name != null) && (!name.equals("")))) {
					Country country = new Country(name, ID);
					countryList.add(country);
					countryMap.put(name, country);
				}
			}

		} catch (Exception e) {
			StudioLogger.error("Change Language TmL Service: could not load countries list");
		}

	}

	/**
	 * Get country ID from country name
	 * 
	 * @param countryName
	 *            country name
	 * @return country ID
	 */
	public static String getIdFromName(String countryName) {
		String id = null;
		Country country = countryMap.get(countryName);
		if (country != null) {
			id = country.getId();
		}
		return id;
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            country name
	 * @param id
	 *            country name
	 */
	public Country(String name, String id) {
		super();
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
