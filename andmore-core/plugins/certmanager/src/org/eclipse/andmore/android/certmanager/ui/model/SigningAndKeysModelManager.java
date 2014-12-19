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
package org.eclipse.andmore.android.certmanager.ui.model;

import java.io.File;
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.BackwardKeystoreManager;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * Provides services to map and unmap keystores. Also get access to the root
 * node and populates on first access (based on {@link KeyStoreManager}).
 *
 */
public class SigningAndKeysModelManager {
	private KeyStoreRootNode keyStoresRootNode = new KeyStoreRootNode();

	private static SigningAndKeysModelManager _instance = null;

	private SigningAndKeysModelManager() {
	}

	public static synchronized SigningAndKeysModelManager getInstance() {
		if (_instance == null) {
			_instance = new SigningAndKeysModelManager();
			_instance.populateKeyStoreRootNode();
		}
		return _instance;
	}

	public File[] getKeystoreFiles() {
		List<ITreeNode> nodes = keyStoresRootNode.getChildren();

		File[] files = new File[nodes.size()];
		int i = 0;
		for (ITreeNode node : nodes) {

			File file = ((KeyStoreNode) node).getFile();
			files[i++] = file;
		}

		return files;
	}

	public KeyStoreRootNode populateKeyStoreRootNode() {
		try {
			List<IKeyStore> keyStores = KeyStoreManager.getInstance().getKeyStores();
			if (keyStores != null) {
				if (keyStores.size() > 0) {
					// there are items mapped on persistence
					for (IKeyStore keyStore : keyStores) {
						if (keyStore instanceof KeyStoreNode) {
							keyStoresRootNode.addKeyStoreNode((KeyStoreNode) keyStore);
						}
					}
				} else {
					// we do not have any item mapped in persistence
					// try to import from old Motodev keystore
					// (probably it is the first time user is trying to use the
					// view)
					BackwardKeystoreManager backwardKeystoreManager = new BackwardKeystoreManager();
					backwardKeystoreManager.mapOldKeystore();
				}
			}
		} catch (Exception e) {
			StudioLogger.error(KeystoreManagerView.class, e.getMessage(), e);
			EclipseUtils.showErrorDialog(
					CertificateManagerNLS.KeystoreManagerView_ErrorLoadingMappedKeystoresFromPersistence,
					e.getMessage());
		}

		return keyStoresRootNode;
	}

	public void unmapKeyStore(KeyStoreNode keyStoreNode) {
		keyStoresRootNode.removeKeyStore(keyStoreNode);
		try {
			File file = keyStoreNode.getFile();
			PasswordProvider passwordProvider = new PasswordProvider(file);
			passwordProvider.deleteKeyStoreSavedPasswordNode();
			KeyStoreManager.getInstance().removeKeyStore(keyStoreNode);
		} catch (KeyStoreManagerException e) {
			EclipseUtils.showErrorDialog("Error unmapping KeyStore", NLS.bind("Could not unmap the keystore file {0}",
					keyStoreNode.getFile()), new Status(IStatus.ERROR, "Error unmapping KeyStore",
					CertificateManagerActivator.PLUGIN_ID, e));
		}
	}

	public void mapKeyStore(KeyStoreNode keyStoreNode) throws KeyStoreManagerException {
		keyStoresRootNode.addKeyStoreNode(keyStoreNode);
		KeyStoreManager.getInstance().addKeyStore(keyStoreNode);
	}

	/**
	 * @return the keyStoresRootNode (populated through
	 *         {@link SigningAndKeysModelManager#getInstance(), in the first
	 *         access}
	 */
	public KeyStoreRootNode getKeyStoresRootNode() {
		return keyStoresRootNode;
	}
}
