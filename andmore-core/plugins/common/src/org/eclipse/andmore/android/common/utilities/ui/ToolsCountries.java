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
package org.eclipse.andmore.android.common.utilities.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class to handle the countries.
 */
public class ToolsCountries {

	/**
	 * The shared instance.
	 */
	private static ToolsCountries instance = null;

	/**
	 * Bundle to get countries from module (properties file).
	 */
	private final ResourceBundle bundle;

	/**
	 * Default constructor.
	 */
	private ToolsCountries() {
		this.bundle = ResourceBundle.getBundle("countries"); //$NON-NLS-1$
	}

	/**
	 * Returns the single instance.
	 * 
	 * @return The singleton instance.
	 */
	public static synchronized ToolsCountries getInstance() {
		if (instance == null) {
			instance = new ToolsCountries();
		}
		return instance;
	}

	/**
	 * Returns all countries.
	 * 
	 * @return All countries.
	 */
	public List<Country> getCountries() {
		List<Country> toReturn = new ArrayList<Country>();

		try {
			Enumeration<String> keys = bundle.getKeys();
			while (keys.hasMoreElements()) {
				String countryCode = keys.nextElement();
				String countryName = bundle.getString(countryCode);
				toReturn.add(new Country(countryCode, countryName));
			}

			Collections.sort(toReturn);
		} catch (MissingResourceException e) {
			toReturn = new ArrayList<Country>();
		}

		return toReturn;
	}

}
