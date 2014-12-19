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
package org.eclipse.andmore.android.wizards.installapp;

import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.wizard.Wizard;

public class UninstallAppWizard extends Wizard {
	private UninstallAppWizardPage mainPage = null;

	private List<String> packagesToUninstall = null;

	private Map<String, String> availablePackages = null;

	private final String WIZARD_IMAGE_PATH = "icons/wizban/undeploy_wizard.png"; //$NON-NLS-1$

	public UninstallAppWizard() {
		setWindowTitle(AndroidNLS.UninstallAppWizardPage_PageTitle);
		super.setDefaultPageImageDescriptor(AndroidPlugin.getImageDescriptor(WIZARD_IMAGE_PATH));
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		mainPage = new UninstallAppWizardPage(availablePackages);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		packagesToUninstall = mainPage.getPackageList();
		return packagesToUninstall.size() > 0;
	}

	public void init(Map<String, String> applicationList) {
		availablePackages = applicationList;
	}

	public void setAvailablePackages(Map<String, String> applicationList) {
		init(applicationList);
		mainPage.setAvailablePackages(applicationList);
	}

	public List<String> getSelectedPackages() {
		return packagesToUninstall;
	}

}
