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

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

/**
 * DESCRIPTION: This class implements the Deploy Wizard
 *
 * RESPONSIBILITY: This class is responsible for provide a Wizard that allows
 * the user to select a package and a device instance
 * 
 */
public class DeployWizard extends Wizard {
	private String packagePath = null;

	private String selectMessage = null;

	private String wizardDescription = null;

	private String wizardTitle = null;

	private String browseButtonText = null;

	private String packagetext = null;

	private DeployWizardPage page;

	private final String WIZARD_IMAGE_PATH = "icons/wizban/deploy_wizard.png"; //$NON-NLS-1$

	public static enum INSTALL_TYPE {
		DO_NOTHING, OVERWRITE, UNINSTALL
	};

	/**
	 * The constructor
	 * 
	 * @param packagePath
	 *            Location of the package containing the application
	 */
	public DeployWizard(String packagePath) {
		this.packagePath = packagePath;
		super.setDefaultPageImageDescriptor(AndroidPlugin.getImageDescriptor(WIZARD_IMAGE_PATH));
		initializeMessages();
	}

	/**
	 * Initializes all the texts that will be used within the wizard
	 */
	private void initializeMessages() {
		selectMessage = AndroidNLS.UI_DeployWizard_SelectMessage;
		wizardDescription = AndroidNLS.UI_DeployWizard_WizardDescription;
		wizardTitle = AndroidNLS.UI_DeployWizard_WizardTitle;
		browseButtonText = AndroidNLS.UI_DeployWizard_BrowseButtonText;
		packagetext = AndroidNLS.UI_DeployWizard_PackageText;
		this.setWindowTitle(wizardTitle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		page = new DeployWizardPage(packagePath, selectMessage, browseButtonText, packagetext);
		page.setDescription(wizardDescription);
		page.setTitle(wizardTitle);
		super.addPage(page);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Do nothing
	}

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
		packagePath = page.getPackagePath();
		return true;
	}

	public String getPackagePath() {
		return packagePath;
	}

	/**
	 * Return true if the application should be replaced in the case it is
	 * already installed on the device
	 * 
	 */
	public INSTALL_TYPE canOverwrite() {
		return page.canOverwrite();
	}
}
