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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the page of signature of external packages wizard It
 * extends the page that removes the signature and implements the needed fields
 */
public class SignExternalPackagePage extends RemoveExternalPackageSignaturePage {

	private Label keystoreLabel = null;

	private Label keysLabel = null;

	private Combo keystoreCombo = null;

	private Text keystorePassword = null;

	private Button loadKeystore = null;

	private Button savePassword = null;

	private Combo keysCombo = null;

	private String keyEntryPassword;

	private String keyStoreType;

	PasswordProvider pP = null;

	private IKeyStore initialSelectedKeyStore = null;

	private IKeyStoreEntry initialSelectedEntry = null;

	private IKeyStore selectedKeystore = null;

	SelectionAdapter loadKeysSelectionAdapter = new SelectionAdapter() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		};

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
		 * .swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			setKeyEntries();
		}
	};

	/**
	 * Create a new wizard page based on selection
	 * 
	 * @param pageName
	 *            the page name
	 * @param selection
	 *            the selection
	 */
	public SignExternalPackagePage(String pageName, IStructuredSelection selection, IKeyStore selectedIKeyStore,
			IKeyStoreEntry selectedEntry) {
		super(pageName, selection);
		this.initialSelectedKeyStore = selectedIKeyStore;
		this.initialSelectedEntry = selectedEntry;
		setDescription(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_DESCRIPTION);
		setTitle(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE);
	}

	private HashMap<String, IKeyStore> getAvailableKeystores() {
		HashMap<String, IKeyStore> keystores = new HashMap<String, IKeyStore>();
		Iterator<IKeyStore> iterator = null;
		try {
			if ((KeyStoreManager.getInstance() != null) && (KeyStoreManager.getInstance().getKeyStores() != null)) {
				iterator = KeyStoreManager.getInstance().getKeyStores().iterator();
			}
		} catch (KeyStoreManagerException e) {
			StudioLogger.error(this.getClass(), "Error retrieving keystore list", e); //$NON-NLS-1$
		}

		while ((iterator != null) && (iterator.hasNext())) {
			KeyStoreNode keystore = (KeyStoreNode) iterator.next();

			keystores.put(keystore.toString(), keystore);
		}
		return keystores;

	}

	/**
	 * @param keystorePath
	 * @return key strings for the selected keystore
	 */
	private final String[] getAvailableEntriesForKeystore(IKeyStore keystore) {
		ArrayList<String> entries = new ArrayList<String>();

		if (keystore != null) {

			pP = new PasswordProvider(keystore.getFile());
			String password = null;
			try {
				// retrieve the saved password
				password = pP.getKeyStorePassword(false);
			} catch (KeyStoreManagerException e1) {
				StudioLogger.error(this.getClass(), "Error retrieving keys from keystore", e1); //$NON-NLS-1$
			}

			if (password == null) {
				// password is not saved
				if (!this.keystorePassword.getText().isEmpty()) {
					// get the password from the wizard
					password = this.keystorePassword.getText();
				}
			} else {
				// the password was saved
				try {
					keystore.isPasswordValid(password);
				} catch (InvalidPasswordException e) {
					if (!this.keystorePassword.getText().isEmpty()) {
						// the saved password is invalid, get the password from
						// the wizard
						password = this.keystorePassword.getText();
					}
				} catch (KeyStoreManagerException e) {
					// this exception should never happen here as it was handled
					// at some point before
					StudioLogger.error("This keystore was imported with wrong store type"); //$NON-NLS-1$
				}
			}
			try {
				try {
					if (password != null) {
						// validate the password that was saved or from the
						// wizard
						keystore.isPasswordValid(password);
						if (this.keystorePassword.getText().isEmpty()) {
							// block the password fields if the password saved
							// is valid
							this.keystorePassword.setText(password);
							this.keystorePassword.setEnabled(false);
							this.savePassword.setSelection(true);
							this.savePassword.setEnabled(false);
							this.loadKeystore.setEnabled(false);
						}
					}
					List<IKeyStoreEntry> keys = keystore.getEntries(password);

					if ((keys.size() > 0) && (password != null)) {
						this.keyStoreType = keystore.getType();
						Iterator<IKeyStoreEntry> iterator2 = keys.iterator();
						while ((iterator2 != null) && (iterator2.hasNext())) {
							EntryNode keyEntry = (EntryNode) iterator2.next();
							entries.add(keyEntry.getId());
						}

					}

				} catch (KeyStoreManagerException e) {
					StudioLogger.error(this.getClass(), "Error retrieving keys from keystore", e); //$NON-NLS-1$
				}

			} catch (InvalidPasswordException e) {
				setErrorMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Invalid_Keystore_Pass);
				this.keystorePassword.setText(""); //$NON-NLS-1$
				this.keystorePassword.setFocus(); // select the password text
													// box so the user can
													// retype the password
			}
		}

		return entries.toArray(new String[0]);
	}

	/**
	 * Fill the key entries combo
	 * 
	 */
	private void setKeyEntries() {
		setErrorMessage(null);

		String keystoreSelected = keystoreCombo.getItem(keystoreCombo.getSelectionIndex());

		this.selectedKeystore = (IKeyStore) keystoreCombo.getData(keystoreSelected);
		String[] availableKeys = getAvailableEntriesForKeystore(this.selectedKeystore);

		if (availableKeys.length > 0) {
			keysCombo.setItems(availableKeys);
			int selectedEntryIndex = 0;

			if (initialSelectedEntry != null) {
				selectedEntryIndex = keysCombo.indexOf(initialSelectedEntry.getAlias());
				initialSelectedEntry = null; // the selectedEntry only serves as
												// first selection
			}

			keysCombo.select(selectedEntryIndex > 0 ? selectedEntryIndex : 0);

			updatePageComplete();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.packaging.ui.wizards.
	 * RemoveExternalPackageSignaturePage
	 * #createExtendedArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createExtendedArea(Composite parent) {
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);

		// Keystore label
		this.keystoreLabel = new Label(parent, SWT.NONE);
		this.keystoreLabel.setText(CertificateManagerNLS.SIGN_WIZARD_AREA_SIGN_KEYSTORE_LABEL);
		this.keystoreLabel.setLayoutData(layoutData);

		// Keystore combo
		this.keystoreCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.SINGLE);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		this.keystoreCombo.setLayoutData(layoutData);
		keystoreCombo.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				keystorePassword.setText(""); //$NON-NLS-1$
				keystorePassword.setEnabled(true);
				savePassword.setSelection(false);
				savePassword.setEnabled(false);
				loadKeystore.setEnabled(false);
				keysCombo.removeAll();
				setKeyEntries();
				if (keystorePassword.getEnabled()) {
					keystorePassword.setFocus();
				}
				// it only serves as
				initialSelectedKeyStore = null;
			}

		});

		// Keystore password label
		Label keystorePasswordLabel = new Label(parent, SWT.NONE);
		keystorePasswordLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystorePasswordLabel);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		keystorePasswordLabel.setLayoutData(layoutData);

		// Keystore password combo
		this.keystorePassword = new Text(parent, SWT.BORDER | SWT.PASSWORD);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		this.keystorePassword.setLayoutData(layoutData);
		this.keystorePassword.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (keystorePassword.getText().isEmpty()) {
					loadKeystore.setEnabled(false);
					savePassword.setEnabled(false);
				} else {
					loadKeystore.setEnabled(true);
					savePassword.setEnabled(true);
				}
				keysCombo.removeAll();
				updatePageComplete();
			}
		});
		this.keystorePassword.addSelectionListener(loadKeysSelectionAdapter);

		// Load key entries Button
		this.loadKeystore = new Button(parent, SWT.PUSH);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		this.loadKeystore.setLayoutData(layoutData);
		this.loadKeystore.setText(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_LOAD);
		this.loadKeystore.setEnabled(false);
		this.loadKeystore.addSelectionListener(loadKeysSelectionAdapter);

		// Save Keystore Password checkbox
		this.savePassword = new Button(parent, SWT.CHECK);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		this.savePassword.setLayoutData(layoutData);
		this.savePassword.setText(CertificateManagerNLS.PasswordProvider_SaveThisPassword);
		this.savePassword.setEnabled(false);
		this.savePassword.setSelection(false);

		// key entry label
		this.keysLabel = new Label(parent, SWT.NONE);
		this.keysLabel.setText(CertificateManagerNLS.SIGN_WIZARD_AREA_SIGN_KEYS_LABEL);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		this.keysLabel.setLayoutData(layoutData);

		// key entry combo
		this.keysCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.SINGLE);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		this.keysCombo.setLayoutData(layoutData);

		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, CertificateManagerActivator.SIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID);

		populateKeyStoreCombo();
	}

	@Override
	protected void createPackageTreeLabel() {
		GridData layoutData;
		Label packagesLabel = new Label(this.mainComposite, SWT.NONE);
		packagesLabel.setText(CertificateManagerNLS.SignExternalPackagePage_Package_Tree_Label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		packagesLabel.setLayoutData(layoutData);
	}

	protected void populateKeyStoreCombo() {
		HashMap<String, IKeyStore> availableKeystores = getAvailableKeystores();
		if (availableKeystores.size() > 0) {

			for (String keystoreKey : availableKeystores.keySet()) {
				IKeyStore newKeystore = availableKeystores.get(keystoreKey);
				this.keystoreCombo.setData(keystoreKey, availableKeystores.get(keystoreKey));
				this.keystoreCombo.add(newKeystore.toString());

				if (initialSelectedKeyStore != null) {
					if (initialSelectedKeyStore.equals(newKeystore)) {
						// select combo with the item selected in Signing and
						// keys view
						keystoreCombo.select(keystoreCombo.indexOf(newKeystore.toString()));
						setKeyEntries();
					}
				}
			}

			if (initialSelectedKeyStore == null) {
				this.keystoreCombo.select(0);
			}
		}
	}

	/**
	 * 
	 * @return the key entry selected by user
	 */
	public IKeyStoreEntry getSelectedKeyEntry() {
		IKeyStoreEntry result = null;
		try {
			result = this.selectedKeystore.getEntry(this.keysCombo.getItem(this.keysCombo.getSelectionIndex()),
					getKeystorePassword());
		} catch (KeyStoreManagerException e) {
			// should never happen
			StudioLogger.error("Could not retrieve entry while signing package"); //$NON-NLS-1$
		} catch (InvalidPasswordException e) {
			// should never happen
			StudioLogger.error("Invalid password while retrieving entry to sign package"); //$NON-NLS-1$
		}

		return result;
	}

	/**
	 * 
	 * @return the keystore selected by user
	 */
	public IKeyStore getSelectedKeyStore() {
		return this.selectedKeystore;
	}

	/**
	 * 
	 * @return the keystore password entered by user
	 */
	public String getKeystorePassword() {
		return this.keystorePassword.getText();
	}

	/**
	 * 
	 * @return key entry password
	 */
	public String getKeyEntryPassword() {

		try {
			this.keyEntryPassword = pP.getPassword(this.keysCombo.getItem(this.keysCombo.getSelectionIndex()), true);
		} catch (KeyStoreManagerException e) {
			StudioLogger.error(this.getClass(), "Error retrieving keys entry password", e); //$NON-NLS-1$
		}
		return this.keyEntryPassword;
	}

	public PasswordProvider getPasswordProvider() {
		return pP;
	}

	/**
	 * 
	 * @return the keystore type
	 */
	public String getKeyStoreType() {
		return this.keyStoreType;
	}

	/**
	 * Update the page status, validating each field of this page The basic
	 * validation is made by superclass
	 */
	@Override
	public void updatePageComplete() {
		super.updatePageComplete();
		int severity = getMessageType();
		String messageAux = severity == IMessageProvider.NONE ? null : getMessage();

		if (messageAux == null) {
			if (!(((this.keystoreCombo != null) && (this.keystoreCombo.getItemCount() > 0)
					&& (this.keystoreCombo.getItem(this.keystoreCombo.getSelectionIndex()) != null) && !this.keystoreCombo
					.getItem(this.keystoreCombo.getSelectionIndex()).equalsIgnoreCase("")) && ((this.keysCombo != null) //$NON-NLS-1$
					&& (this.keysCombo.getItemCount() > 0)
					&& (this.keysCombo.getItem(this.keysCombo.getSelectionIndex()) != null) && !this.keysCombo.getItem(
					this.keysCombo.getSelectionIndex()).equalsIgnoreCase("")))) //$NON-NLS-1$
			{
				messageAux =

				CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_NO_CERTIFICATE_ERROR;
				severity = IMessageProvider.ERROR;
			}

			if (messageAux == null) {
				messageAux =

				CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_DESCRIPTION;
				severity = IMessageProvider.NONE;
			}

			setMessage(messageAux, severity);
			setPageComplete(severity == IMessageProvider.NONE);
		}

	}

	/**
	 * @return checkbox save password
	 */
	public boolean getSavePasswordSelection() {
		return this.savePassword.getSelection();
	}
}
