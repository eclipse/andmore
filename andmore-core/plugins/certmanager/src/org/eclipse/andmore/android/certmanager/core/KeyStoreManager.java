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
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreRootNode;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * Provides a common interface to manipulate keystores. Other plugins need to
 * use it (to avoid knowing {@link KeyStoreRootNode} model and
 * {@link SaveStateManager}).
 * 
 * The {@link KeystoreManagerView} also need to call its methods to guarantee
 * persistence of its operations.
 */
public class KeyStoreManager {
	public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";

	public static final String KEYSTORE_TYPE_JCEKS = "JCEKS";

	public static final String KEYSTORE_TYPE_JKS = "JKS";

	private static final String ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE = "Error to access keystore mapping persistence";

	private static KeyStoreManager _instance;

	private List<IKeyStore> keyStores = null;

	/**
	 * This class is a singleton.
	 * 
	 * @return The unique instance of this class.
	 * */
	public synchronized static KeyStoreManager getInstance() {
		if (_instance == null) {
			_instance = new KeyStoreManager();
		}
		return _instance;
	}

	private KeyStoreManager() {
	}

	/**
	 * Add a new keystore to the manager.
	 * 
	 * @param keystore
	 *            The keystore to be added.
	 * @throws KeyStoreManagerException
	 *             if an error occurs while accessing persistence file where
	 *             keystores are mapped.
	 */
	public void addKeyStore(IKeyStore keystore) throws KeyStoreManagerException {
		SaveStateManager manager = null;
		try {
			manager = SaveStateManager.getInstance();
			manager.addEntry(keystore.getFile(), keystore.getType());
			getKeyStores().add(keystore);
		} catch (Exception e) {
			throw new KeyStoreManagerException(ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE, e);
		}
	}

	/**
	 * Remove a keystore from the manager.
	 * 
	 * @param keystore
	 *            The keystore to be removed.
	 * @throws KeyStoreManagerException
	 *             if an error occurs while accessing persistence file where
	 *             keystores are mapped.
	 */
	public void removeKeyStore(IKeyStore keystore) throws KeyStoreManagerException {
		SaveStateManager manager = null;
		try {
			manager = SaveStateManager.getInstance();
			manager.removeEntry(keystore.getFile());
			getKeyStores().remove(keystore);
		} catch (Exception e) {
			throw new KeyStoreManagerException(ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE, e);
		}
	}

	/**
	 * Set the date which the keystore was added to a backup file.
	 * 
	 * @param keyStore
	 *            The keystore to be set.
	 * @param backupDate
	 *            The date of the backup.
	 * @throws KeyStoreManagerException
	 *             If there were problems while persisting the information.
	 * */
	public void setBackupDate(IKeyStore keyStore, Date backupDate) throws KeyStoreManagerException {
		if ((keyStore != null) && (backupDate != null)) {
			try {
				SaveStateManager manager = SaveStateManager.getInstance();
				manager.setBackupDate(keyStore.getFile(), backupDate);
			} catch (Exception e) {
				throw new KeyStoreManagerException(ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE, e);
			}
		}
	}

	/**
	 * Update the type of a managed keystore, using solely the information
	 * provided by the keystore.
	 * 
	 * @param keystore
	 *            The keystore which type needs to be updated.
	 * @throws KeyStoreManagerException
	 *             If there were problems while persisting the information.
	 * */
	public void updateKeyStoreType(IKeyStore keyStore) throws KeyStoreManagerException {
		SaveStateManager manager;
		try {
			manager = SaveStateManager.getInstance();
			if (manager.isKeystoreMapped(keyStore.getFile())) {
				manager.addEntry(keyStore.getFile(), keyStore.getType());
			}
		} catch (IOException e) {
			throw new KeyStoreManagerException(ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE, e);
		}

	}

	/**
	 * @return The list of mapped keystores in the persistence, or empty list if
	 *         there is no keystore mapped.
	 * @throws KeyStoreManagerException
	 *             if an error occurs to access persistence file where keystores
	 *             are mapped.
	 */
	public List<IKeyStore> getKeyStores() throws KeyStoreManagerException {
		if (keyStores == null) {
			keyStores = new ArrayList<IKeyStore>();
			SaveStateManager manager = null;
			try {
				manager = SaveStateManager.getInstance();
				if (manager.getMappedKeystores() != null) {
					for (File keystoreFile : manager.getMappedKeystores()) {
						SaveStateManager.ViewStateEntry stateEntry = manager.getEntry(keystoreFile);
						if (stateEntry != null) {
							IKeyStore keyStoreNode = new KeyStoreNode(keystoreFile);
							keyStoreNode.setType(stateEntry.getKeystoreType());
							keyStoreNode.setLastBackupDate(stateEntry.getBackupDate());
							keyStores.add(keyStoreNode);
						}
					}
				}
			} catch (IOException e) {
				throw new KeyStoreManagerException(ERROR_TO_ACCESS_KEYSTORE_MAPPING_PERSISTENCE, e);
			}
		}
		return keyStores;
	}

	/**
	 * Check if a keystore file is already mapped. The input parameter is a
	 * File, instead of a String, to ensure that File.getCanonicalPath() will be
	 * used in the filenames comparison.
	 * 
	 * @param keystoreFile
	 *            A file representing the keystore.
	 * @return True if the file is already mapped, false otherwise.
	 * */
	public boolean isKeystoreMapped(File keystoreFile) {
		boolean result = false;
		SaveStateManager manager = null;

		try {
			manager = SaveStateManager.getInstance();
			if (manager.getMappedKeystores() != null) {
				for (File mappedKeystoreFile : manager.getMappedKeystores()) {
					if (mappedKeystoreFile.getCanonicalPath().equals(keystoreFile.getCanonicalPath())) {
						result = true;
						break;
					}
				}
			}
		} catch (IOException e) {
			result = false;
			StudioLogger.error(getClass(),
					"IOException while trying to check if a file is mapped on Signing and Keys view.");
		}

		return result;
	}

	/**
	 * The current available keystore types are:
	 * <ul>
	 * <li>JKS</li>
	 * <li>JCEKS</li>
	 * <li>PKCS12</li>
	 * </ul>
	 * 
	 * @return The list of available keystore types.
	 * */
	public List<String> getAvailableTypes() {
		List<String> availableKeystoreTypes = new ArrayList<String>();

		availableKeystoreTypes.add(KEYSTORE_TYPE_JKS);
		availableKeystoreTypes.add(KEYSTORE_TYPE_JCEKS);
		availableKeystoreTypes.add(KEYSTORE_TYPE_PKCS12);

		if (!availableKeystoreTypes.contains(getDefaultType())) {
			availableKeystoreTypes.add(getDefaultType());
		}

		return availableKeystoreTypes;
	}

	/**
	 * When no store type is specified, the manager define a type that should be
	 * used as the default one.
	 * 
	 * @return The default keystore type used in the Signing and Keys view.
	 * */
	public String getDefaultType() {
		return KeyStore.getDefaultType().toUpperCase();
	}

	/**
	 * Create a new keystore given a file, a store type and a password.
	 */
	public static IKeyStore createKeyStore(File keyStoreFile, String keyStoreType, char[] password)
			throws KeyStoreManagerException {
		IKeyStore keyStoreNode = null;
		try {
			KeyStore keyStore = KeyStoreUtils.createKeystore(keyStoreFile, keyStoreType, password);
			keyStoreNode = new KeyStoreNode(keyStoreFile, keyStore);
		} catch (InvalidPasswordException e) {
			StudioLogger.error("Invalid password when creating a keystore: " + e.getMessage());
		}
		return keyStoreNode;
	}

	/**
	 * Create a new keystore given a file and a password. The store type is set
	 * to be the default.
	 */
	public static IKeyStore createKeyStore(File keyStoreFile, char[] password) throws KeyStoreManagerException {
		IKeyStore keyStoreNode = null;
		try {
			KeyStore keyStore = KeyStoreUtils.createKeystore(keyStoreFile, password);
			keyStoreNode = new KeyStoreNode(keyStoreFile, keyStore);
		} catch (InvalidPasswordException e) {
			StudioLogger.error("Invalid password when creating a keystore: " + e.getMessage());
		}

		return keyStoreNode;
	}
}
