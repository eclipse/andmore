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
import java.util.List;

import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.ui.model.SigningAndKeysModelManager;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

/**
 * Manages old MOTODEV keystore available at
 * <user_home>\motodevstudio\tools\motodev.keystore (to keep backward
 * compatibility)
 */
public class BackwardKeystoreManager {
	/**
	 * MOTODEV Studio directory.
	 */
	private static final String MOTODEV_TOOLS_PATH = File.separator + "motodevstudio" + File.separator + "tools"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Path to user home directory.
	 */
	private static final String USER_HOME_PATH = System.getProperty("user.home"); //$NON-NLS-1$

	/**
	 * Full path where the files will be.
	 */
	private static final String TOOLS_FULL_PATH = USER_HOME_PATH + MOTODEV_TOOLS_PATH;

	private static final String KS_FILENAME_NEW = TOOLS_FULL_PATH + File.separatorChar + "motodev.keystore"; //$NON-NLS-1$

	/**
	 * The constant contains the keysotre type.
	 */
	private static final String KS_TYPE = "JCEKS"; //$NON-NLS-1$

	/**
	 * The constant contains the keystore password.
	 */
	private static final String KS_DEFAULT_PASSWD = "passwd"; //$NON-NLS-1$

	/**
	 * The constant contains the keystore password node on secure storage.
	 */
	private static final String KS_PASSWD_NODE = "org.eclipse.andmore.platform.tools.sign.core"; //$NON-NLS-1$

	/**
	 * The constant contains the keystore password attribute on secure storage.
	 */
	private static final String KS_PASSWD_ATTRIBUTE = "kspasswd"; //$NON-NLS-1$

	/*
	 * Removes old keystore password from Eclipse secure preferences (old
	 * format)
	 * 
	 * @return the stored password (before removal), {@link
	 * SigningCoreConstants#KS_DEFAULT_PASSWD} if found node {@link
	 * SigningCoreConstants#KS_PASSWD_NODE} but {@link
	 * SigningCoreConstants#KS_PASSWD_ATTRIBUTE} does not exist, null if node
	 * {@link SigningCoreConstants#KS_PASSWD_NODE} not found
	 */
	private String removeOldKeystorePassword() {
		String oldPassword = KS_DEFAULT_PASSWD;
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = null;
		try {
			if (preferences.nodeExists(KS_PASSWD_NODE)) {
				// old format node exists = try to get from attribute
				node = preferences.node(KS_PASSWD_NODE);
				try {
					oldPassword = node.get(KS_PASSWD_ATTRIBUTE, KS_DEFAULT_PASSWD);
				} catch (StorageException e) {
					oldPassword = KS_DEFAULT_PASSWD;
				}
				node.remove(KS_PASSWD_ATTRIBUTE);
				preferences.remove(KS_PASSWD_NODE);
				node.flush();
			} else {
				// node already removed => password is in the new format
				oldPassword = null;
			}
		} catch (Exception e) {
			StudioLogger.error(BackwardKeystoreManager.class, e.getMessage(), e);
		}
		return oldPassword;
	}

	/**
	 * Maps old keystore if the <user_home>\motodevstudio\tools\motodev.keystore
	 * file exists.
	 */
	public void mapOldKeystore() {
		File motodevKeystoreFile = new File(KS_FILENAME_NEW);
		if (motodevKeystoreFile.exists()) {
			// we found backward (old default MOTODEV keystore) => import it
			KeyStoreNode keyStoreNode = new KeyStoreNode(motodevKeystoreFile, KS_TYPE);

			try {
				SigningAndKeysModelManager.getInstance().mapKeyStore(keyStoreNode);

				// remove old password from keystore
				String oldPassword = removeOldKeystorePassword();

				if (oldPassword != null) {
					// oldPassword was in old format => add password for
					// keystore in the new format
					PasswordProvider passwordProvider = new PasswordProvider(keyStoreNode.getFile());
					passwordProvider.saveKeyStorePassword(oldPassword);

					// for each child key (save the same password as the
					// keystore to maintain backward compatibility)
					List<IKeyStoreEntry> iKeyStoreEntries = keyStoreNode.getEntries(oldPassword);
					if (iKeyStoreEntries != null) {
						for (IKeyStoreEntry keyStoreEntry : iKeyStoreEntries) {
							passwordProvider.savePassword(keyStoreEntry.getAlias(), oldPassword);
						}
					}
				}
			} catch (Exception e) {
				// error
				EclipseUtils.showErrorDialog(NLS.bind(
						CertificateManagerNLS.KeystoreManagerView_ErrorImportingBackwardKeystore, motodevKeystoreFile),
						e.getMessage());
			}
		}
	}
}
