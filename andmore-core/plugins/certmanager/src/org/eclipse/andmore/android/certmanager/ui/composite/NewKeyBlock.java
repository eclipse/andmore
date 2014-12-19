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
package org.eclipse.andmore.android.certmanager.ui.composite;

import java.util.Date;

import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeyWizardPage;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsFactory;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * This class shows the field to create a new Android certificates / keys.
 */
public class NewKeyBlock extends CertificateBlock {
	private static final int VALIDITY_SIZE = 3;

	private static final int SMALL_TEXT_SIZE = 64;

	private CreateKeyWizardPage baseWizardPage;

	private Label keyPasswordLabel;

	private Label keyConfirmPasswordLabel;

	private Text keyPassword;

	private Text keyConfirmPassword;

	private Button savePasswordCheckBox;

	private Text textValidity = null;

	private Label labelValidity = null;

	private boolean canSavePassword = false;

	private String keyPasswordText = new String();

	private static final String DEFAULT_VALIDITY_IN_YEARS = "30"; //default  //$NON-NLS-1$

	@Override
	public Composite createContent(Composite parent) {
		Composite composite = super.createContent(parent);

		decorateRequiredFields();

		return composite;
	};

	@Override
	protected void createCustomDetailedInfoArea(Composite parent) {
		labelValidity = WidgetsFactory.createLabel(parent, CertificateManagerNLS.CertificateBlock_Validity + ":"); //$NON-NLS-1$
		textValidity = WidgetsFactory.createText(parent);
		textValidity.addListener(SWT.Modify, this);
		textValidity.addFocusListener(focusListener);
		textValidity.setTextLimit(VALIDITY_SIZE);
		textValidity.setText(DEFAULT_VALIDITY_IN_YEARS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.ui.composite.CertificateBlock
	 * #createCustomArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createCustomArea(Composite parent) {
		Group passwordGroup = WidgetsFactory.createTitledGroup(parent,
				CertificateManagerNLS.NewKeyBlock_PasswordGroupTitle, 2);

		// KEYSTORE PASSWORD SECTION
		keyPasswordLabel = new Label(passwordGroup, SWT.NONE);
		keyPasswordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keyPasswordLabel.setText(CertificateManagerNLS.CertificateBlock_KeyPassword_Label + ":"); //$NON-NLS-1$

		keyPassword = new Text(passwordGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		keyPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keyPassword.setTextLimit(SMALL_TEXT_SIZE);
		keyPassword.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				keyPasswordText = keyPassword.getText();
				NewKeyBlock.super.handleEvent(event);
			}
		});

		keyPassword.addFocusListener(focusListener);

		// CONFIRM PASSWORD SECTION
		keyConfirmPasswordLabel = new Label(passwordGroup, SWT.NONE);
		keyConfirmPasswordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keyConfirmPasswordLabel.setText(CertificateManagerNLS.CertificateBlock_ConfirmKeyPassword_Label + ":"); //$NON-NLS-1$

		keyConfirmPassword = new Text(passwordGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		keyConfirmPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keyConfirmPassword.addListener(SWT.Modify, this);
		keyConfirmPassword.addFocusListener(focusListener);
		keyConfirmPassword.setTextLimit(SMALL_TEXT_SIZE);

		// Creates the save password checkbox
		savePasswordCheckBox = new Button(passwordGroup, SWT.CHECK);
		savePasswordCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		savePasswordCheckBox.setText(CertificateManagerNLS.PasswordProvider_SaveThisPassword);
		savePasswordCheckBox.setSelection(false);
		savePasswordCheckBox.addFocusListener(focusListener);

		if ((baseWizardPage != null) && (baseWizardPage.getKeystore() != null)) {
			// we can not save password if keystore is not mapped in the view
			savePasswordCheckBox.setVisible(KeyStoreManager.getInstance().isKeystoreMapped(
					baseWizardPage.getKeystore().getFile()));
		}
		savePasswordCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// update according to check status
				canSavePassword = savePasswordCheckBox.getSelection();
			}

		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.ui.composite.CertificateBlock
	 * #decorateRequiredFields()
	 */
	@Override
	protected void decorateRequiredFields() {
		super.decorateRequiredFields();

		labelValidity.setText(decorateText(labelValidity.getText()));
		keyPasswordLabel.setText(decorateText(keyPasswordLabel.getText()));
		keyConfirmPasswordLabel.setText(decorateText(keyConfirmPasswordLabel.getText()));
	}

	@Override
	public boolean isPageComplete() {
		return (super.isPageComplete()) && !WidgetsUtil.isNullOrEmpty(this.textValidity)
				&& !WidgetsUtil.isNullOrEmpty(this.keyPassword) && !WidgetsUtil.isNullOrEmpty(this.keyConfirmPassword);
	}

	@Override
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage() && !WidgetsUtil.isNullOrEmpty(this.textValidity)
				&& !WidgetsUtil.isNullOrEmpty(this.keyPassword) && !WidgetsUtil.isNullOrEmpty(this.keyConfirmPassword);
	}

	public Composite createInfoBlock(Composite parent, String alias, String name, String organization,
			String organizationUnit, String country, String state, String locality, Date validity, Date creationDate) {
		Composite toReturn = super.createInfoBlock(parent, alias, name, organization, organizationUnit, country, state,
				locality);
		labelValidity.setText(CertificateManagerNLS.CertificateBlock_ExpirationDate + ":"); //$NON-NLS-1$
		textValidity.setTextLimit(Text.LIMIT);
		textValidity.setText(validity.toString());
		textValidity.setEditable(false);

		keyPasswordLabel.setVisible(false);
		keyPassword.setVisible(false);
		keyConfirmPasswordLabel.setVisible(false);
		keyConfirmPassword.setVisible(false);
		savePasswordCheckBox.setVisible(false);

		return toReturn;
	}

	public String getKeyPassword() {
		return keyPasswordText;
	}

	public String getKeyConfirmPassword() {
		return keyConfirmPassword.getText();
	}

	public boolean needToSaveKeyPassword() {
		return canSavePassword;
	}

	@Override
	public String getErrorMessage() {
		String message = super.getErrorMessage();

		// if there is no error message on other items => check text validity
		// field
		if (message == null) {
			try {
				int validity = Integer.parseInt(textValidity.getText());
				if (validity <= 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException nfe) {
				message = CertificateManagerNLS.CertificateBlock_Validity_Error;
			}
		}

		// if there is no error message on other items => check password fields
		if (message == null) {
			// password text and confirmation password text must match
			if (!keyPassword.getText().equals(keyConfirmPassword.getText())) {
				message = CertificateManagerNLS.CreateKeystorePage_PasswordDoesNotMatch;
			}
			// check password size according to keytool specification
			if (keyPassword.getText().length() < EntryNode.KEY_PASSWORD_MIN_SIZE) {
				message = NLS.bind(CertificateManagerNLS.CreateKeystorePage_PasswordMinSizeMessage,
						IKeyStore.KEYSTORE_PASSWORD_MIN_SIZE);
			}

		}

		return message;
	}

	/**
	 * Set messages (used to set information messages)
	 * 
	 * @param message
	 * @param messageType
	 *            IMessageProvider.INFORMATION
	 */
	protected void setMessage(String message, int messageType) {
		if (baseWizardPage != null) {
			baseWizardPage.setMessage(message, messageType);
		}
	}

	public String getValidity() {
		return textValidity.getText();
	}

	/**
	 * @param baseWizardPage
	 *            the baseWizardPage to set
	 */
	public void setBaseWizardPage(CreateKeyWizardPage baseWizardPage) {
		this.baseWizardPage = baseWizardPage;
	}

}
