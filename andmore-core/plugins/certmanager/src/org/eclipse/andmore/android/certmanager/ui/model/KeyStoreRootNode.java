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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;

/**
 * Node that is the parent of keyStores (root node of
 * {@link KeystoreManagerView})
 */
public class KeyStoreRootNode extends AbstractTreeNode {
	// Map from absolute file path to KeyStoreModel
	private final List<ITreeNode> keyStores = new ArrayList<ITreeNode>();

	/**
	 * Adds keystore to root node of the tree
	 * 
	 * @param keyStoreModel
	 * @throws KeyStoreManagerException
	 *             if the keystore is already listed in the tree
	 */
	public void addKeyStoreNode(KeyStoreNode keyStoreModel) throws KeyStoreManagerException {
		if (!keyStores.contains(keyStoreModel)) {
			keyStores.add(keyStoreModel);
			keyStoreModel.setParent(this);

			KeyStoreModelEventManager.getInstance().fireEvent(keyStoreModel, KeyStoreModelEvent.EventType.ADD);
		} else {
			// error - notify
			throw new KeyStoreManagerException(NLS.bind(
					CertificateManagerNLS.KeyStoreRootNode_Error_AlreadyMappedKeystorePath, keyStoreModel.getFile()
							.getAbsolutePath()));
		}
	}

	public void removeKeyStore(KeyStoreNode keyStoreModel) {
		keyStores.remove(keyStoreModel);
		KeyStoreModelEventManager.getInstance().fireEvent(keyStoreModel, KeyStoreModelEvent.EventType.REMOVE);
		File keysToreFile = keyStoreModel.getFile();

		PasswordProvider password = new PasswordProvider(keysToreFile);

		try {
			password.deleteKeyStoreSavedPasswordNode();
		} catch (KeyStoreManagerException e) {
			StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
		}

	}

	@Override
	public void refresh() {
		// Not necessary, root nod can't be refreshed
	}

	@Override
	public String getId() {
		return ""; //not necessary - root node //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return ""; //not necessary - root node //$NON-NLS-1$
	}

	@Override
	public ImageDescriptor getIcon() {
		return null; // not necessary - root node
	}

	@Override
	public boolean isLeaf() {
		return false; // root node
	}

	@Override
	public ITreeNode getParent() {
		return null; // invisible node
	}

	@Override
	public List<ITreeNode> getChildren() {
		return new ArrayList<ITreeNode>(keyStores);
	}

}
