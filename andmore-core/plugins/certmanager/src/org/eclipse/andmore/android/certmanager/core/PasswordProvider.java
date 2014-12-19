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
package org.eclipse.andmore.android.certmanager.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * This class is responsible to retrieve passwords for a keystore and its
 * entries. Usage: Instantiate with a keyStoreFile, call the methods
 * getPassword. If needed a dialog will be shown, asking user to type the
 * password.
 */
public class PasswordProvider {

	private static final String PREF_ROOT_NODE = CertificateManagerActivator.PLUGIN_ID + "_passwords"; //$NON-NLS-1$

	private static final String KS_PASSWORD_KEY = "KS_PASSWORD"; //$NON-NLS-1$

	private final class KeyStorePasswdDialog extends Dialog {
		private final File keyStoreFile;

		private String passwd;

		private boolean savePasswd;

		private Text paswordText;

		private Button saveCheckBox;

		private final String alias;

		private KeyStorePasswdDialog(Shell parentShell, File keyStoreFile, String alias) {
			super(parentShell);
			this.keyStoreFile = keyStoreFile;
			this.alias = alias;
		}

		@Override
		protected Control createDialogArea(Composite parent) {

			Composite mainComposite = new Composite(parent, SWT.NONE);
			GridLayout gridLayout = new GridLayout(2, false);
			mainComposite.setLayout(gridLayout);

			// Creates the message
			Label messageLabel = new Label(mainComposite, SWT.NONE);
			GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 2);
			messageLabel.setLayoutData(gridData);

			if (this.alias.equals(KS_PASSWORD_KEY)) {
				getShell().setText(CertificateManagerNLS.PasswordProvider_DialogTitle);
				messageLabel.setText(NLS.bind(CertificateManagerNLS.PasswordProvider_MessageLabel,
						keyStoreFile.getName()));
			} else {
				getShell().setText(CertificateManagerNLS.CertificateBlock_KeyPassword_Label);
				messageLabel.setText(NLS.bind(CertificateManagerNLS.PasswordProvider_Key_MessageLabel, alias));
			}

			// Creates the text field label
			Label passwdLabel = new Label(mainComposite, SWT.NONE);
			gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			passwdLabel.setLayoutData(gridData);
			passwdLabel.setText(CertificateManagerNLS.PasswordProvider_PasswordLabel);

			// Creates the password text
			paswordText = new Text(mainComposite, SWT.BORDER | SWT.PASSWORD);
			gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			paswordText.setLayoutData(gridData);

			// Creates the save password checkbox
			saveCheckBox = new Button(mainComposite, SWT.CHECK);
			saveCheckBox.setText(CertificateManagerNLS.PasswordProvider_SaveThisPassword);
			saveCheckBox.setSelection(false);
			gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
			saveCheckBox.setLayoutData(gridData);
			saveCheckBox.setVisible(KeyStoreManager.getInstance().isKeystoreMapped(keyStoreFile));

			return super.createDialogArea(parent);
		}

		@Override
		protected void okPressed() {
			passwd = paswordText.getText();
			savePasswd = saveCheckBox.getSelection();
			super.okPressed();
		}

		public String getPasswd() {
			return passwd;
		}

		public boolean mustSavePasswd() {
			return savePasswd;
		}
	}

	private final File keyStoreFile;

	private final ISecurePreferences securePreferences;

	private boolean canSavePassword = true;

	public PasswordProvider(File keyStoreFile) {
		this(keyStoreFile, KeyStoreManager.getInstance().isKeystoreMapped(keyStoreFile));
	}

	public PasswordProvider(File keyStoreFile, boolean canSavePassword) {
		this.keyStoreFile = keyStoreFile;
		this.securePreferences = SecurePreferencesFactory.getDefault();
		this.canSavePassword = canSavePassword;
	}

	/**
	 * Retrieves the KeyStore password.
	 * 
	 * @param promptPassword
	 *            whether the password entry dialog will be shown or not
	 * @param useSavedPassword
	 *            whether to use the keyStore saved password
	 * @return the password string or null if user canceled the dialog
	 * @throws KeyStoreManagerException
	 */
	public String getKeyStorePassword(boolean promptPassword, boolean useSavedPassword) throws KeyStoreManagerException {
		return getPassword(KS_PASSWORD_KEY, promptPassword, useSavedPassword);
	}

	/**
	 * Retrieves the KeyStore password. This method will always attempt to
	 * retrieve the saved password. It's behavior is the same as of calling the
	 * method getPassword(promptPassword, true)
	 * 
	 * @param promptPassword
	 *            whether the password entry dialog will be shown or not
	 * @return the password string or null if user canceled the dialog
	 * @throws KeyStoreManagerException
	 */
	public String getKeyStorePassword(boolean promptPassword) throws KeyStoreManagerException {
		return getPassword(KS_PASSWORD_KEY, promptPassword, true);
	}

	/**
	 * Retrieves the password for a given alias within a keyStore. This method
	 * will always attempt to retrieve the saved password. It's behavior is the
	 * same as of calling the method getPassword(promptPassword, true)
	 * 
	 * @param promptPassword
	 *            whether the password entry dialog will be shown or not
	 * @return the password string or null if user canceled the dialog
	 * @throws KeyStoreManagerException
	 */
	public String getPassword(String alias, boolean promptPassword) throws KeyStoreManagerException {
		return getPassword(alias, promptPassword, true);
	}

	/**
	 * Retrieves the password for a given alias within a keyStore. This method
	 * will always attempt to retrieve the saved password. It's behavior is the
	 * same as of calling the method getPassword(promptPassword, true)
	 * 
	 * @param promptPassword
	 *            whether the password entry dialog will be shown or not
	 * @param useSavedPassword
	 *            whether to use the keyStore saved password
	 * @return the password string or null if user canceled the dialog
	 * @throws KeyStoreManagerException
	 */
	public String getPassword(String alias, boolean promptPassword, boolean useSavedPassword)
			throws KeyStoreManagerException {
		String password = null;

		if (useSavedPassword) {
			if (securePreferences != null) {
				String prefKey = alias;
				password = getSavedPasswd(prefKey);
			} else {
				throw new KeyStoreManagerException(CertificateManagerNLS.PasswordProvider_Error_WhileSaving);
			}
		}

		if ((password == null) && promptPassword) {
			password = promptPassword(alias);
		}

		return password;
	}

	private String getSavedPasswd(String prefKey) {
		String password = null;
		// Try to get the password from secure storage
		if (securePreferences.nodeExists(PREF_ROOT_NODE)) {
			ISecurePreferences node = securePreferences.node(PREF_ROOT_NODE);
			try {
				if (node.nodeExists(keyStoreFile.getAbsolutePath())) {
					ISecurePreferences ksNode = node.node(keyStoreFile.getAbsolutePath());
					password = ksNode.get(prefKey, null);
				}
			} catch (StorageException e) {
				// Do nothing, password will be null.
			}
		}
		return password;
	}

	private String promptPassword(final String alias) throws KeyStoreManagerException {
		final String[] result = new String[1];
		final Boolean[] canProceed = new Boolean[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				KeyStorePasswdDialog dialog = new KeyStorePasswdDialog(PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell(), keyStoreFile, alias);

				int diagStatus = dialog.open();

				if (diagStatus == Window.OK) {
					// Read the values from the dialog and do the actions,
					// return passwd and save if required
					result[0] = dialog.getPasswd();

					canSavePassword = KeyStoreManager.getInstance().isKeystoreMapped(keyStoreFile);
					canProceed[0] = dialog.mustSavePasswd();
				} else {
					// dialog cancelled
					canProceed[0] = false;
					result[0] = null;
				}
			}
		});

		if (canProceed[0] && canSavePassword) {
			if (securePreferences != null) {
				savePassword(alias, result[0]);
			} else {
				EclipseUtils.showWarningDialog(CertificateManagerNLS.PasswordProvider_DialogTitle,
						CertificateManagerNLS.PasswordProvider_Error_WhileSaving);
			}
		}

		return result[0];
	}

	public void saveKeyStorePassword(String password) throws KeyStoreManagerException {
		savePassword(KS_PASSWORD_KEY, password);
	}

	public void savePassword(final String alias, String password) throws KeyStoreManagerException {
		String prefKey;
		canSavePassword = KeyStoreManager.getInstance().isKeystoreMapped(keyStoreFile);
		if (canSavePassword) // protect from saving
		{
			if (alias != null) {
				prefKey = alias;
			} else {
				prefKey = KS_PASSWORD_KEY;
			}

			ISecurePreferences rootNode = securePreferences.node(PREF_ROOT_NODE);
			try {
				ISecurePreferences ksNode = rootNode.node(keyStoreFile.getAbsolutePath());
				ksNode.put(prefKey, password, true);
				ksNode.flush();
			} catch (Exception e) {
				throw new KeyStoreManagerException(CertificateManagerNLS.PasswordProvider_Error_WhileSaving);
			}
		}
	}

	/**
	 * Deletes the entire node (including KS_PASSWORD_KEY and children aliases)
	 * 
	 * @throws KeyStoreManagerException
	 */
	public void deleteKeyStoreSavedPasswordNode() throws KeyStoreManagerException {
		deleteSavedPassword(null);
	}

	/**
	 * Deletes only KS_PASSWORD_KEY (not children aliases)
	 */
	public void deleteKeyStoreSavedPassword() throws KeyStoreManagerException {
		deleteSavedPassword(KS_PASSWORD_KEY);
	}

	public void deleteSavedPassword(String alias) throws KeyStoreManagerException {

		ISecurePreferences ksNode = getKeyStoreNode();
		if (ksNode != null) {
			if (alias == null) {
				ksNode.removeNode();
			} else {
				ksNode.remove(alias);
				// if no item has no child, then we can remove the node
				if (ksNode.keys().length == 0) {
					ksNode.removeNode();
				}
			}

			try {
				ksNode.flush();
			} catch (IllegalStateException e) {
				// Do nothing, node has already been removed
			} catch (IOException e) {
				throw new KeyStoreManagerException(NLS.bind(
						CertificateManagerNLS.PasswordProvider_Error_WhileRemovingPassword, keyStoreFile.getName()));
			}
		}
	}

	/**
	 * This method will remove all saved entries for this keystore file that is
	 * not listed on the aliasList. The idea is to remove all saved passwords
	 * that makes reference to non-existant entries.
	 * 
	 * @param aliasList
	 *            the list of alias to be kept if available on the security
	 *            keystore
	 * @throws KeyStoreManagerException
	 *             if writing the security keystore fails for some reason
	 */
	public void cleanModel(List<String> aliasList) throws KeyStoreManagerException {
		ISecurePreferences keyStoreNode = getKeyStoreNode();
		if (keyStoreNode != null) {
			String[] savedKeys = keyStoreNode.keys();
			for (String savedAlias : savedKeys) {
				if (!savedAlias.equals(KS_PASSWORD_KEY) && !aliasList.contains(savedAlias)) {
					keyStoreNode.remove(savedAlias);
				}
			}
			try {
				keyStoreNode.flush();
			} catch (IOException e) {
				throw new KeyStoreManagerException(NLS.bind(
						CertificateManagerNLS.PasswordProvider_Error_WhileRemovingPassword, keyStoreFile.getName()));
			}
		}
	}

	/*
	 * @return the keystore node if it exists
	 */
	private ISecurePreferences getKeyStoreNode() {
		ISecurePreferences ksNode = null;
		if (securePreferences.nodeExists(PREF_ROOT_NODE)) {
			ISecurePreferences rootNode = securePreferences.node(PREF_ROOT_NODE);

			if (rootNode.nodeExists(keyStoreFile.getAbsolutePath())) {
				ksNode = rootNode.node(keyStoreFile.getAbsolutePath());
			}
		}
		return ksNode;
	}

	/**
	 * If keystore password is saved.
	 */
	public boolean isPasswordSaved() {
		return isPasswordSaved(KS_PASSWORD_KEY);
	}

	/**
	 * If alias password is saved.
	 */
	public boolean isPasswordSaved(String prefKey) {
		ISecurePreferences ksNode = null;
		boolean isSaved = false;
		if (securePreferences.nodeExists(PREF_ROOT_NODE)) {
			ISecurePreferences rootNode = securePreferences.node(PREF_ROOT_NODE);
			ksNode = rootNode.node(keyStoreFile.getAbsolutePath());
			try {
				String value = ksNode.get(prefKey, null);
				isSaved = value != null; // password is saved if it is not the
											// default value (because password
											// length should be at least 6
			} catch (StorageException e) {
				StudioLogger.debug("It was not possible to get if the " + prefKey + " is saved or not");
				isSaved = false;
			}
		}
		return isSaved;
	}
}
