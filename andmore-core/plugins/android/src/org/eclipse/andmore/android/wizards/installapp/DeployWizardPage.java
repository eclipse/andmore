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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.elements.FileChooser;
import org.eclipse.andmore.android.wizards.installapp.DeployWizard.INSTALL_TYPE;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard Page used by Deploy Wizard
 */
public class DeployWizardPage extends WizardPage {
	private FileChooser fileChooser = null;

	private String initialPackagePath = null;

	private String packageSelectionMessage = null;

	private String packagetext = null;

	private final String packageExtension = "apk";

	private Button overwiteRadio = null;

	private Button uninstallRadio = null;

	private Button doNothingRadio = null;

	private static final String contextId = AndroidPlugin.PLUGIN_ID + ".install_app";

	private static INSTALL_TYPE installType;

	private final String DSA_FILE_EXTENSION = ".DSA";

	private final String RSA_FILE_EXTENSION = ".RSA";

	private final String SF_FILE_EXTENSION = ".SF";

	private static String lastUsedPackage = null;

	/**
	 * Constructor
	 * 
	 * @param initialPackagePath
	 * @param selectPCKMessage
	 *            Message asking for package selection
	 */
	public DeployWizardPage(String initialPackagePath, String selectPCKMessage, String browseButtonText,
			String packagetext) {
		super("");

		if ((browseButtonText == null) || (packagetext == null) || (selectPCKMessage == null)) {
			throw new IllegalArgumentException("Could not create Deploy Wizard: null argument");
		}

		if (initialPackagePath == null) {
			this.initialPackagePath = lastUsedPackage != null ? lastUsedPackage : "";
		} else {
			this.initialPackagePath = initialPackagePath;
		}

		this.packagetext = packagetext;
		packageSelectionMessage = selectPCKMessage;

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
		// Main composite for the UI
		Composite mainComposite = new Composite(parent, SWT.FILL);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, contextId);

		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.FILL_VERTICAL, true, true));

		// Package group
		Group packageGroup = new Group(mainComposite, SWT.NONE);
		packageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		packageGroup.setLayout(new GridLayout(3, false));
		packageGroup.setText(packagetext);

		fileChooser = new FileChooser(packageGroup, SWT.NONE, null);
		fileChooser.setFilterExtensions(new String[] { "*." + packageExtension });
		fileChooser.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fileChooser.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateSelection();
			}
		});
		createOptionButtons(mainComposite);

		mainComposite.pack();

		setPageComplete(false);
		loadInitialValues();

		setControl(mainComposite);
	}

	private void createOptionButtons(Composite mainComposite) {

		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		overwiteRadio = new Button(mainComposite, SWT.RADIO);
		overwiteRadio.setText(AndroidNLS.UI_DeployWizardPage_ReplaceApp);
		overwiteRadio.setSelection(true);
		overwiteRadio.setLayoutData(data);
		overwiteRadio.setData(INSTALL_TYPE.OVERWRITE);
		overwiteRadio.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				installType = (INSTALL_TYPE) overwiteRadio.getData();
			}

		});

		uninstallRadio = new Button(mainComposite, SWT.RADIO);
		uninstallRadio.setText(AndroidNLS.UI_DeployWizardPage_UninstallApp);
		uninstallRadio.setSelection(true);
		uninstallRadio.setLayoutData(data);
		uninstallRadio.setData(INSTALL_TYPE.UNINSTALL);
		uninstallRadio.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				installType = (INSTALL_TYPE) uninstallRadio.getData();
			}

		});

		doNothingRadio = new Button(mainComposite, SWT.RADIO);
		doNothingRadio.setText(AndroidNLS.UI_DeployWizardPage_DoNothingApp);
		doNothingRadio.setSelection(true);
		doNothingRadio.setLayoutData(data);
		doNothingRadio.setData(INSTALL_TYPE.DO_NOTHING);
		doNothingRadio.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				installType = (INSTALL_TYPE) doNothingRadio.getData();
			}

		});
		overwiteRadio.setSelection(true);
		uninstallRadio.setSelection(false);
		doNothingRadio.setSelection(false);
		installType = (INSTALL_TYPE.OVERWRITE);

	}

	/**
	 * Load the initial values to be filled in the wizard
	 */
	private void loadInitialValues() {
		if ((initialPackagePath != null) && (initialPackagePath.length() != 0)) {
			fileChooser.setText(initialPackagePath);
			validateSelection();
		} else {
			setMessage(packageSelectionMessage, IMessageProvider.NONE);
		}
	}

	/**
	 * Validates the selected package and device instance setting the
	 * appropriated messages and errors
	 */
	private synchronized void validateSelection() {
		String packagePath = fileChooser.getText();

		if (isValidPackage(packagePath)) {
			setErrorMessage(null);
			setPageComplete(true);
		} else {
			setPageComplete(false);
		}
	}

	/**
	 * Verify if a package is valid MPKG
	 * 
	 * @param packagePath
	 * @return TRUE if the package is valid or FALSE otherwise
	 */
	private boolean isValidPackage(String packagePath) {
		boolean result = false;
		Path path = new Path(packagePath);
		String extension = path.getFileExtension();

		// testing if the entered path is a folder
		result = path.toFile().isFile();
		if (!result) {
			setErrorMessage(AndroidNLS.UI_DeployWizardPage_PackageIsAFolder);
		} else {
			result = ((extension != null) && extension.equals(packageExtension) && path.isValidPath(path.toString()));

			if (!result) {
				setErrorMessage(AndroidNLS.UI_DeployWizardPage_InvalidPath);
			} else {
				// Test if file exists
				result = path.toFile().exists();
				if (!result) {
					setErrorMessage(AndroidNLS.UI_DeployWizardPage_FileDoesNotExist);
				}
			}
		}

		if (result) {
			setMessage("", IMessageProvider.NONE);

			// Test if the package is valid
			result = isPackageSigned(packagePath);
			if (!result) {
				setErrorMessage(AndroidNLS.UI_DeployWizardPage_NotSignedMessage);
			}
		}

		return result;
	}

	/**
	 * Verify if the package is signed based on the existence of an .SF file and
	 * a corresponding DSA or RSA file.
	 * 
	 * @param packagePath
	 * @return TRUE if the package is signed
	 */
	private synchronized boolean isPackageSigned(String packagePath) {
		// Temporary placeholders for the package entries
		List<String> SFFiles = new ArrayList<String>();
		List<String> RSAFiles = new ArrayList<String>();
		List<String> DSAFiles = new ArrayList<String>();

		// Temporary result
		boolean result = false;
		JarFile jar = null;
		try {
			jar = new JarFile(packagePath, false);
			Enumeration<JarEntry> enu = jar.entries();

			// interact over the elements of the package
			while (enu.hasMoreElements()) {
				JarEntry entry = enu.nextElement();
				if (entry.getName().toUpperCase().endsWith(SF_FILE_EXTENSION)) {
					// Mounts the list of SF files
					SFFiles.add(entry.getName().toUpperCase());
				} else if (entry.getName().toUpperCase().endsWith(RSA_FILE_EXTENSION)) {
					// Mounts the list of RSA files
					RSAFiles.add(entry.getName().toUpperCase());
				} else if (entry.getName().toUpperCase().endsWith(DSA_FILE_EXTENSION)) {
					// Mounts the list of DSA files
					DSAFiles.add(entry.getName().toUpperCase());
				}
			}

			if (!SFFiles.isEmpty()) {
				for (String sfFile : SFFiles) {
					// Interacts over the list of SF files until it ends or
					// until a correspondent DSA or RSA is found
					Path p = new Path(sfFile);
					sfFile = p.removeFileExtension().toString();
					result = (DSAFiles.contains(sfFile + DSA_FILE_EXTENSION) || RSAFiles.contains(sfFile
							+ RSA_FILE_EXTENSION));
				}
			}

		} catch (Exception e) {
			// Could not read the jar file
			StudioLogger.error(DeployWizardPage.class, "Deploy: Could not verify file " + packagePath, e);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					StudioLogger.error(DeployWizardPage.class, "Error closing package after verification", e);
				}
			}
		}

		return result;

	}

	/**
	 * Gets the selected package path
	 * 
	 * @return the package Path
	 */
	public String getPackagePath() {
		String packagePath = fileChooser.getText();
		if (isValidPackage(packagePath)) {
			lastUsedPackage = packagePath;
		}
		return packagePath;
	}

	/**
	 * Return true if the application should be replaced in the case it is
	 * already installed on the device
	 * 
	 */
	public INSTALL_TYPE canOverwrite() {
		return installType;
	}
}