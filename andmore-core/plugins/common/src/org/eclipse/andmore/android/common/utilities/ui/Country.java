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

/**
 * This class represents a country.
 */
public class Country implements Comparable<Country> {

	/*
	 * The two letters country code.
	 */
	private final String countryCode;

	/*
	 * The country name.
	 */
	private final String countryName;

	/**
	 * Creates a country instance with the given country code and country name.
	 * 
	 * @param countryCode
	 *            The two letters country code.
	 * @param countryName
	 *            Th two letters country name.
	 */
	public Country(String countryCode, String countryName) {
		this.countryCode = countryCode;
		this.countryName = countryName;
	}

	/**
	 * Returns the two letters country code.
	 * 
	 * @return The two letters country code.
	 */
	public String getCountryCode() {
		return countryCode;
	}

	/**
	 * Returns the country name.
	 * 
	 * @return The country name.
	 */
	public String getCountryName() {
		return countryName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Country otherCountry) {
		return this.countryName.compareToIgnoreCase(otherCountry.countryName);
	}

}
