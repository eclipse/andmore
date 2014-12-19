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

package org.eclipse.andmore.android.certmanager.ui.wizards;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Enables selection of a keystore (similar to {@link ImportKeystorePage}
 * functionality. It adds a checkbox button that enables user to add the
 * imported keystore into Signing and Keys view.
 */
public class SelectExistentKeystorePage extends ImportKeystorePage {
	private static final int SMALL_TEXT_SIZE = 64;

	public static final String SELECT_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".select_keystore"; //$NON-NLS-1$

	private Button alsoImportIntoView = null;

	private boolean importIntoView = true;

	private Label keystorePasswordLabel;

	private Text keystorePassword;

	private Button savePasswordCheckBox;

	private boolean canSavePassword = false;

	private String password = null;

	private SelectionListener selectionListener = new SelectionAdapter() {

		@Override
		public void widgetSelected(SelectionEvent e) {
			validatePage();
		}
	};

	protected SelectExistentKeystorePage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		setTitle(CertificateManagerNLS.SelectExistentKeystorePage_WizardPageTitle);
		setMessage(CertificateManagerNLS.SelectExistentKeystorePage_WizardPageMessage);

		// KEYSTORE PASSWORD SECTION
		keystorePasswordLabel = new Label(getMainComposite(), SWT.NONE);
		keystorePasswordLabel.setText(CertificateManagerNLS.SelectExistentKeystorePage_KeystorePasswordLabel);

		keystorePassword = new Text(getMainComposite(), SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		keystorePassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		keystorePassword.setTextLimit(SMALL_TEXT_SIZE);
		keystorePassword.addSelectionListener(selectionListener);
		keystorePassword.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				password = keystorePassword.getText();
				validatePage();
			}
		});

		// Creates the save password checkbox
		savePasswordCheckBox = new Button(getMainComposite(), SWT.CHECK);
		savePasswordCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		savePasswordCheckBox.setText(CertificateManagerNLS.PasswordProvider_SaveThisPassword);
		savePasswordCheckBox.setSelection(false);
		savePasswordCheckBox.setVisible(importIntoView);
		savePasswordCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// update according to check status
				canSavePassword = savePasswordCheckBox.getSelection();
			}

		});

		alsoImportIntoView = new Button(getMainComposite(), SWT.CHECK);
		alsoImportIntoView.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		alsoImportIntoView
				.setText(CertificateManagerNLS.SelectExistentKeystorePage_CheckboxText_AlsoImportIntoSigningView);
		alsoImportIntoView.setSelection(importIntoView);
		alsoImportIntoView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// update according to check status
				importIntoView = alsoImportIntoView.getSelection();
				savePasswordCheckBox.setEnabled(importIntoView);
				canSavePassword = importIntoView && savePasswordCheckBox.getSelection();
			}

		});

		// set help id for this page
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getMainComposite(), SELECT_KEYSTORE_HELP_ID);
	}

	/**
	 * @return the importIntoView
	 */
	public boolean needToImportIntoView() {
		return importIntoView;
	}

	/**
	 * @return the password
	 */
	protected String getPassword() {
		return password;
	}

	@Override
	protected boolean validatePage() {
		boolean pageComplete = super.validatePage();

		String infoMessage = CertificateManagerNLS.SelectExistentKeystorePage_WizardPageMessage;
		String errorMessage = null;

		if (pageComplete) {
			if (!keystoreAlreadyMapped) {
				if (keystorePassword.getText().isEmpty()) {
					pageComplete = false;
					errorMessage = CertificateManagerNLS.CertificateBlock_EnterPassword_InfoMessage;
				}
				setMessage(infoMessage);
				setErrorMessage(errorMessage);
				setPageComplete(pageComplete);
			}
		}

		return pageComplete;
	}

	/**
	 * @return the canSavePassword
	 */
	protected boolean canSavePassword() {
		return canSavePassword;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.ui.wizards.ImportKeystorePage
	 * #importKeystore()
	 */
	@Override
	public boolean importKeystore() {
		return importKeystore(getPassword(), canSavePassword());
	}
}
