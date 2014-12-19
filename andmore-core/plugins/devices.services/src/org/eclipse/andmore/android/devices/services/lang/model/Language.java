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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.eclipse.core.runtime.FileLocator;

/**
 * Language representation
 * 
 * The list of languages has been downloaded from:
 * http://www.loc.gov/standards/iso639-2/ascii_8bits.html
 */
public class Language {

	private static final String FILE_PATH = "resources/ISO-639-2_utf-8.txt";

	private static List<Language> languageList = null;

	private static Map<String, Language> languageMap = null;

	private String name;

	private String id;

	/**
	 * Get the languages list
	 * 
	 * @return the languages list
	 */
	public static List<Language> getLanguageList() {
		if (languageList == null) {
			loadLanguages();
		}
		return languageList;
	}

	/**
	 * Load languages from TXT file
	 */
	private static void loadLanguages() {
		languageList = new ArrayList<Language>();
		languageMap = new HashMap<String, Language>();

		URL languagesURL = DeviceServicesPlugin.getDefault().getBundle().getResource(FILE_PATH);

		BufferedReader input = null;
		try {

			File file = new File(FileLocator.toFileURL(languagesURL).getPath());

			input = new BufferedReader(new FileReader(file));

			String line = null;
			String[] lineParts = null;
			String name = null;
			String ID = null;
			while ((line = input.readLine()) != null) {
				lineParts = line.split("\\|");
				ID = lineParts[2];
				name = lineParts[3];
				if (((ID != null) && (!ID.equals(""))) && ((name != null) && (!name.equals("")))) {
					Language language = new Language(name, ID);
					languageList.add(language);
					languageMap.put(name, language);
				}
			}
		} catch (Exception e) {
			StudioLogger.error("Change Language TmL Service: could not load languages list");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}

	}

	/**
	 * Get language ID from language name
	 * 
	 * @param langName
	 *            language name
	 * @return language ID
	 */
	public static String getIdFromName(String langName) {
		String id = null;
		Language language = languageMap.get(langName);
		if (language != null) {
			id = language.getId();
		}
		return id;
	}

	/**
	 * Constructs a new Language instance, based on the given name and id.
	 * 
	 * @param name
	 *            language name
	 * @param id
	 *            language id
	 */
	public Language(String name, String id) {
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
