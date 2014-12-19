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
package org.eclipse.andmore.android.wizards.mat;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.wizard.Wizard;

public class DumpHPROFWizard extends Wizard {
	private DumpHPROFWizardPage mainPage = null;

	private String selectedApp = null;

	private String serialNumber = null;

	private final String WIZARD_IMAGE_PATH = "icons/wizban/dump_hprof_wizard.png"; //$NON-NLS-1$

	public DumpHPROFWizard(String serialNumber) {
		setWindowTitle(AndroidNLS.DumpHprofPage_PageTitle);
		super.setDefaultPageImageDescriptor(AndroidPlugin.getImageDescriptor(WIZARD_IMAGE_PATH));
		setHelpAvailable(false);
		// set parameters
		this.serialNumber = serialNumber;
	}

	@Override
	public void addPages() {
		mainPage = new DumpHPROFWizardPage(serialNumber);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		selectedApp = mainPage.getSelectedApp();
		return selectedApp != null;
	}

	public String getSelectedApp() {
		return selectedApp;
	}

}
