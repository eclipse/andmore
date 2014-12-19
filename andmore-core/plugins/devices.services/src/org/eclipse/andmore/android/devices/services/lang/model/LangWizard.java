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
import org.eclipse.jface.wizard.Wizard;

/**
 * Change Language Wizard used to change the device language configuration
 */
public class LangWizard extends Wizard {

	private final String WIZARD_IMAGE_PATH = "resources/flag.png";

	private LangWizardPage page;

	private final String[] currentLangAndCountry = null;

	private String languageID;

	private String countryID;

	public LangWizard() {
		this.setWindowTitle(ServicesNLS.UI_Wizard_Title);
		super.setDefaultPageImageDescriptor(DeviceServicesPlugin.getImageDescriptor(WIZARD_IMAGE_PATH));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		page = new LangWizardPage(currentLangAndCountry);
		super.addPage(page);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return page.isPageComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		languageID = Language.getIdFromName(page.getLanguage());
		countryID = Country.getIdFromName(page.getCountry());

		return true;
	}

	public String getlanguageId() {
		return languageID;
	}

	public String getcountryId() {
		return countryID;
	}
}
