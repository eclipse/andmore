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

import java.util.List;

import org.eclipse.andmore.android.devices.services.i18n.ServicesNLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Composite containing the current language in use and two combo boxes where
 * the user can choose a language and country
 */
public class LocationComposite extends Composite {
	// id for listeners
	static final int LOCATION_CHANGE = 1234;

	// selected language
	private String language = "";

	// selected country
	private String country = "";

	// language combobox
	private Combo comboLanguage = null;

	// country combobox
	private Combo comboCountry = null;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent composite
	 */
	public LocationComposite(Composite parent) {
		this(parent, null, null);
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent composite
	 * @param currentLanguageId
	 *            the id of the current language in use by given emulator
	 *            instance.
	 * @param currentCountryId
	 *            the id of the current country in use by given emulator
	 *            instance.
	 */
	public LocationComposite(Composite parent, String currentLanguageId, String currentCountryId) {
		super(parent, SWT.NONE);

		Label label;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);

		/*
		 * Language label and combobox
		 */
		label = new Label(this, SWT.LEFT);
		label.setText(ServicesNLS.UI_Language);

		comboLanguage = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
		comboLanguage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				language = comboLanguage.getText();
				notifyListeners(LOCATION_CHANGE, null);
			}
		});
		if ((currentLanguageId != null) && (!currentLanguageId.equals(""))) {
			fillComboBoxAndSelectCurrentLanguage(currentLanguageId);
		} else {
			comboLanguage.setItems(getLanguagesList());
		}
		language = comboLanguage.getText();
		comboLanguage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comboLanguage.setVisibleItemCount(20);

		/*
		 * Country label and combobox
		 */
		label = new Label(this, SWT.LEFT);
		label.setText(ServicesNLS.UI_Country);

		comboCountry = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
		comboCountry.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				country = comboCountry.getText();
				notifyListeners(LOCATION_CHANGE, null);
			}
		});
		if ((currentCountryId != null) && (!currentCountryId.equals(""))) {
			fillComboBoxAndSelectCurrentCountry(currentCountryId);
		} else {
			comboCountry.setItems(getCountriesList());
		}
		country = comboCountry.getText();
		comboCountry.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comboCountry.setVisibleItemCount(20);
	}

	/*
	 * Sets an array of languages in the language combo box and select that item
	 * which is the current in use by emulator instance. <p>
	 * 
	 * There are some needed explanations here: <p>
	 * 
	 * 1. The currentLanguageId parameter was obtained through an ADB shell
	 * command sent to emulator. The ID returned is a String similar to "pt",
	 * "en", "fr" for Portuguese, English and French respectively. For more
	 * details, please see: <br> com.motorola.studio.android.adt.DDMSUtils.
	 * getCurrentEmulatorLanguageAndCountry method. <p>
	 * 
	 * 2. The currentLanguageId parameter is passed to getLanguagesList method
	 * in order to be returned an array of Strings holding all languages
	 * available and, at the length-plus-one position of this array, the number
	 * representing the index of array regarding the current language name in
	 * use by emulator. <p>
	 * 
	 * 3. The language combo box is filled with all languages contained in
	 * langNamesAndIndex array. Then the number, held at last position of array,
	 * is passed as parameter to select() method of the combo box. Thus, the
	 * current language appears as selected when combo box is rendered. <p>
	 * 
	 * @param currentLanguageId The ID of the current language in use by
	 * emulator instance.
	 */
	private void fillComboBoxAndSelectCurrentLanguage(String currentLanguageId) {
		String[] langNamesAndIndex = getLanguagesList(currentLanguageId);
		int index = langNamesAndIndex.length - 1;
		int currentLanguageIndex = Integer.parseInt(langNamesAndIndex[index]);
		comboLanguage.setItems(langNamesAndIndex);
		comboLanguage.remove(index);
		comboLanguage.select(currentLanguageIndex);
	}

	/*
	 * Sets an array of countries in the country combo box and select that item
	 * which is the current in use by emulator instance. <p>
	 * 
	 * There are some needed explanations here: <p>
	 * 
	 * 1. The currentCountryId parameter was obtained through an ADB shell
	 * command sent to emulator. The ID returned is a String similar to "it",
	 * "us", "ru" for Italy, USA and Russia respectively. For more details,
	 * please see: <br> com.motorola.studio.android.adt.DDMSUtils.
	 * getCurrentEmulatorLanguageAndCountry method. <p>
	 * 
	 * 2. The currentCountryId parameter is passed to getCountriesList method in
	 * order to be returned an array of Strings holding all country available
	 * and, at the length-plus-one position of this array, the number
	 * representing the index of array regarding the current country name in use
	 * by emulator. <p>
	 * 
	 * 3. The country combo box is filled with all countries contained in
	 * countryNamesAndIndex array. Then the number, held at last position of
	 * array, is passed as parameter to select() method of the combo box. Thus,
	 * the current country appears as selected when combo box is rendered. <p>
	 * 
	 * @param currentCountryId The ID of the current country in use by emulator
	 * instance.
	 */
	private void fillComboBoxAndSelectCurrentCountry(String currentCountryId) {
		String[] countryNamesAndIndex = getCountriesList(currentCountryId);
		int index = countryNamesAndIndex.length - 1;
		int currentCountryIndex = Integer.parseInt(countryNamesAndIndex[index]);
		comboCountry.setItems(countryNamesAndIndex);
		comboCountry.remove(index);
		comboCountry.select(currentCountryIndex);
	}

	/*
	 * Get the list of languages to be used to populate the combobox
	 * 
	 * @return the list of languages to be used to populate the combobox
	 */
	private String[] getLanguagesList() {
		List<Language> languageObjs = Language.getLanguageList();
		String[] languages = new String[languageObjs.size()];

		int i = 0;
		for (Language language : languageObjs) {
			languages[i] = language.getName();
			i++;
		}
		return languages;
	}

	/*
	 * Get the list of languages to be used to populate the combo box. Also,
	 * returns the index of the current language at last position of the array.
	 * 
	 * @param currentlanguageId
	 * 
	 * @return An array of Strings containing the list of languages and the
	 * current language index.
	 */
	private String[] getLanguagesList(String currentlanguageId) {
		List<Language> languageObjs = Language.getLanguageList();
		int size = languageObjs.size();
		String[] langNamesAndIndex = new String[size + 1];
		String languageIndex = null;
		int i = 0;
		for (Language language : languageObjs) {
			String name = language.getName();
			String id = language.getId();
			if (id.equalsIgnoreCase(currentlanguageId)) {
				languageIndex = String.valueOf(i);
				langNamesAndIndex[size] = languageIndex;
			}
			langNamesAndIndex[i] = name;
			i++;
		}
		return langNamesAndIndex;
	}

	/*
	 * Get the list of countries to be used to populate the combobox
	 * 
	 * @return the list of countries to be used to populate the combobox
	 */
	private String[] getCountriesList() {
		List<Country> countryObjs = Country.getCountryList();
		String[] countries = new String[countryObjs.size()];
		int i = 0;
		for (Country country : countryObjs) {
			countries[i] = country.getName();
			i++;
		}
		return countries;
	}

	/*
	 * Get the list of countries to be used to populate the combo box. Also,
	 * returns the index of the current country at last position of the array.
	 * 
	 * @param currentCountryId
	 * 
	 * @return An array of Strings containing the list of countries and the
	 * current country index.
	 */
	private String[] getCountriesList(String currentCountryId) {
		List<Country> countryObjs = Country.getCountryList();
		int size = countryObjs.size();
		String[] countryNamesAndIndex = new String[size + 1];
		String countryIndex = null;
		int i = 0;
		for (Country country : countryObjs) {
			String name = country.getName();
			String id = country.getId();
			if (id.equalsIgnoreCase(currentCountryId)) {
				countryIndex = String.valueOf(i);
				countryNamesAndIndex[size] = countryIndex;
			}
			countryNamesAndIndex[i] = name;
			i++;
		}
		return countryNamesAndIndex;
	}

	/**
	 * Get the selected language
	 * 
	 * @return selected language name
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Get the selected country
	 * 
	 * @return selected country name
	 */
	public String getCountry() {
		return country;
	}
}
