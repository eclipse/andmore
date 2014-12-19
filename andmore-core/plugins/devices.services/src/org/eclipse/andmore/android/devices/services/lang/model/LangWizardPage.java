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

import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.eclipse.andmore.android.devices.services.i18n.ServicesNLS;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Page where the user can choose the language the user wants to apply to the
 * emulator
 */
public class LangWizardPage extends WizardPage {

	// main composite
	LocationComposite locationComposite = null;

	private String[] currentLangAndCountry = null;

	/**
	 * Constructor
	 */
	public LangWizardPage() {
		this(null);
	}

	/**
	 * Constructor
	 */
	public LangWizardPage(String[] currentLangAndCountry) {
		super("langWizardPage");
		this.currentLangAndCountry = currentLangAndCountry;
		setTitle(ServicesNLS.UI_Wizard_Page_Locale_Title);
		setDescription(ServicesNLS.UI_Wizard_Page_Locale_Description);
		setPageComplete(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {

		// Set Help ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DeviceServicesPlugin.LANG_PAGE_CONTEXT_HELP_ID);

		// Define layout
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginTop = 0;
		mainLayout.marginWidth = 0;
		mainLayout.marginHeight = 0;

		if (currentLangAndCountry != null) {
			locationComposite = new LocationComposite(parent, currentLangAndCountry[0], currentLangAndCountry[1]);
		} else {
			locationComposite = new LocationComposite(parent);
		}

		locationComposite.addListener(LocationComposite.LOCATION_CHANGE, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				updatePageComplete();
			}
		});

		locationComposite.setLayoutData(mainLayout);
		setControl(locationComposite);
	}

	/**
	 * Check if the "Next" button can be enabled by checking if the user has
	 * filled all the fields
	 */
	private void updatePageComplete() {
		setPageComplete(false);
		String language = Language.getIdFromName(locationComposite.getLanguage());
		String country = Country.getIdFromName(locationComposite.getCountry());
		if ((currentLangAndCountry == null)
				|| (!currentLangAndCountry[0].equals(language) || !currentLangAndCountry[1].equals(country))) {
			setPageComplete(true);
		}
	}

	/**
	 * Get the selected language
	 * 
	 * @return selected language name
	 */
	public String getLanguage() {
		return locationComposite.getLanguage();
	}

	/**
	 * Get the selected country
	 * 
	 * @return selected country name
	 */
	public String getCountry() {
		return locationComposite.getCountry();
	}

}
