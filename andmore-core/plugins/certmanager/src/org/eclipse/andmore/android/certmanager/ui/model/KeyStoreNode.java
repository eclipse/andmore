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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.core.KeyStoreUtils;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent.EventType;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Represents a keystore visual item for the {@link KeystoreManagerView}.
 * 
 * @author gdpr78
 * 
 */
public class KeyStoreNode extends AbstractTreeNode implements IKeyStore {
	public static final String WARN_ABOUT_UNSUPPORTED_ENTRIES_PREFERENCE = CertificateManagerActivator.PLUGIN_ID
			+ ".warnAboutUnsupportedEntries"; //$NON-NLS-1$

	private static final String DUMMY_NODE = "DUMMY_NODE"; //$NON-NLS-1$

	private final File keyStoreFile;

	private KeyStore keyStore;

	private Date lastBackupDate;

	private String type;

	/**
	 * Alias to {@link EntryNode}
	 */
	private final Map<String, ITreeNode> entries = new LinkedHashMap<String, ITreeNode>();

	private final String KEYSTORE_NONSAVED_PASSWORD_ICON_PATH = "icons/keystore.png"; //$NON-NLS-1$

	private final String KEYSTORE_SAVED_PASSWORD_ICON_PATH = "icons/keystore_saved_password.png"; //$NON-NLS-1$

	private static final String WRONG_KEYSTORE_TYPE_ICON_PATH = "icons/keystore_incorrect_type.png";

	private final PasswordProvider passwordProvider;

	private boolean ignoreRefresh;

	private boolean quiet;

	private boolean skipNextReload = false;

	private boolean typeVerified;

	public KeyStoreNode(File path) {
		this.keyStoreFile = path;
		passwordProvider = new PasswordProvider(keyStoreFile);
		updateStatus();
	}

	public KeyStoreNode(File path, String type) {
		this.keyStoreFile = path;
		this.type = type;
		passwordProvider = new PasswordProvider(keyStoreFile);
		updateStatus();
	}

	public KeyStoreNode(File keyStoreFile, KeyStore keyStore) {
		this(keyStoreFile);
		this.keyStore = keyStore;
		this.type = keyStore.getType();
	}

	@Override
	public PasswordProvider getPasswordProvider() {
		return passwordProvider;
	}

	@Override
	public String getKeyStorePassword(boolean promptPassword) {
		String password = null;
		boolean keepTrying = true;

		// keep asking password until user either enter the correct password or
		// cancel the operation
		while (keepTrying) {
			try {
				try {
					keepTrying = false;
					password = getPasswordProvider().getKeyStorePassword(promptPassword);
					if (password != null) {
						isPasswordValid(password);
					}
				} catch (InvalidPasswordException e) {
					getPasswordProvider().deleteKeyStoreSavedPasswordNode();
					password = null;
					keepTrying = true;
				}
			} catch (KeyStoreManagerException e) {
				password = null;
				keepTrying = false;

				StudioLogger.info(this.getClass(),
						CertificateManagerNLS.KeyStoreNode_CouldNotGetKeyStorePassword + e.getLocalizedMessage());
			}
		}

		return password;
	}

	/**
	 * @return the path
	 */
	@Override
	public File getFile() {
		return keyStoreFile;
	}

	@Override
	public KeyStore getKeyStore() throws KeyStoreManagerException {
		return getKeyStore(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStore#
	 * getKeyStore()
	 */
	public KeyStore getKeyStore(boolean load) throws KeyStoreManagerException {
		if (keyStore == null) {
			boolean tryAgain = false;
			boolean useSavedPass = true;
			String password = null;
			do {
				if (tryAgain) {
					useSavedPass = false;
				}
				password = passwordProvider.getKeyStorePassword(true, useSavedPass);
				tryAgain = false;
				if (password != null) {
					try {
						keyStore = loadKeystore(password.toCharArray());
						setTooltip(null);
						if (load) {
							loadEntries();
						}
					} catch (InvalidPasswordException e) {
						tryAgain = true;
					}
				} else {
					setTooltip(CertificateManagerNLS.KeyStoreNode_CouldNotLoadKeystore_Tooltip);
				}
			} while (tryAgain);
		}

		return keyStore;
	}

	public KeyStore getKeyStore(String password) throws KeyStoreManagerException, InvalidPasswordException {
		if ((keyStore == null) && (password != null)) {
			keyStore = loadKeystore(password.toCharArray());
			loadEntries();
		} else {
			// just check if given password is valid for this keystore
			isPasswordValid(password);
		}

		return keyStore;
	}

	@Override
	public boolean isPasswordValid(String password) throws KeyStoreManagerException, InvalidPasswordException {
		KeyStore myKeyStore = null;
		if (password != null) {
			myKeyStore = loadKeystore(password.toCharArray());
		} else {
			throw new InvalidPasswordException(CertificateManagerNLS.KeyStoreNode_Password_NotNull);
		}

		return myKeyStore != null;
	}

	protected KeyStore loadKeystore(char[] password) throws KeyStoreManagerException, InvalidPasswordException {
		KeyStore keyStore = null;
		setNodeStatus(Status.OK_STATUS);
		setTooltip(null);
		try {
			if (!typeVerified && type.equalsIgnoreCase("jceks")) //$NON-NLS-1$
			{
				// Try to load this as JKS.
				keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, password, "JKS"); //$NON-NLS-1$
				if (keyStore != null) {
					// Keystore type is actually wrong, it's a jks keystore.
					EclipseUtils.showWarningDialog(CertificateManagerNLS.KeyStoreNode_Wrong_KeystoreType_Title,
							NLS.bind(CertificateManagerNLS.KeyStoreNode_Wrong_KeystoreType_Message, getName()));
					setType("JKS"); //$NON-NLS-1$
					typeVerified = true;
				}
			}
		} catch (KeyStoreManagerException keyStoreManagerException) {
			// Do nothing, let's try with the correct type.
		} catch (InvalidPasswordException invalidPasswordException) {
			setNodeStatus(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID,
					CertificateManagerNLS.KeyStoreNode_InvalidPassword));
			throw invalidPasswordException;
		}

		try {
			keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, password, type);
			setNodeStatus(Status.OK_STATUS);
		} catch (KeyStoreManagerException keyStoreManagerException) {
			setNodeStatus(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID,
					IKeyStore.WRONG_KEYSTORE_TYPE_ERROR_CODE,
					CertificateManagerNLS.KeyStoreNode_KeystoreTypeWrong_NodeStatus, null));
			throw keyStoreManagerException;
		} catch (InvalidPasswordException invalidPasswordException) {
			setNodeStatus(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID,
					CertificateManagerNLS.KeyStoreNode_InvalidPassword));
			throw invalidPasswordException;
		}
		return keyStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((keyStoreFile == null) ? 0 : keyStoreFile.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KeyStoreNode)) {
			return false;
		}
		KeyStoreNode other = (KeyStoreNode) obj;
		if (keyStoreFile == null) {
			if (other.keyStoreFile != null) {
				return false;
			}
		} else if (!keyStoreFile.equals(other.keyStoreFile)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName() + " - ( " + getId() + " )"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void refresh() throws KeyStoreManagerException {
		if (!ignoreRefresh) {
			if (!skipNextReload) {
				keyStore = null;
				skipNextReload = false;
			}
			entries.clear();
			updateStatus();
			if (getNodeStatus().isOK()) {
				quiet = true;
				loadEntries();
				quiet = false;
				passwordProvider.cleanModel(new ArrayList<String>(entries.keySet()));
			}
		} else {
			setIgnoreRefresh(false);
		}
	}

	private void setIgnoreRefresh(boolean ignoreRefresh) {
		this.ignoreRefresh = ignoreRefresh;
	}

	@Override
	public String getId() {
		return keyStoreFile.getAbsolutePath();
	}

	@Override
	public String getName() {
		return keyStoreFile.getName();
	}

	@Override
	public ImageDescriptor getIcon() {
		ImageDescriptor descr = null;
		if (!isStoreTypeCorrect()) {
			// wrong keystore type
			descr = AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
					WRONG_KEYSTORE_TYPE_ICON_PATH);
		} else if (isPasswordSaved()) {
			// saved password
			descr = AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
					KEYSTORE_SAVED_PASSWORD_ICON_PATH);
		} else {
			// non saved password
			descr = AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
					KEYSTORE_NONSAVED_PASSWORD_ICON_PATH);
		}
		return descr;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public List<ITreeNode> getChildren() throws KeyStoreManagerException {
		ArrayList<ITreeNode> children = new ArrayList<ITreeNode>(entries.values());
		return children;
	}

	private void loadEntries() throws KeyStoreManagerException {
		if (entries.size() == 1) {
			ITreeNode entryNode = entries.get(DUMMY_NODE);
			if (entryNode != null) {
				entries.remove(DUMMY_NODE);
				KeyStoreModelEventManager.getInstance().fireEvent(entryNode, EventType.REMOVE);
			}
		}
		entries.clear();
		KeyStore keyStore = getKeyStore(false);
		if (keyStore != null) {
			Enumeration<String> aliases;
			try {
				aliases = keyStore.aliases();
			} catch (KeyStoreException e) {
				throw new KeyStoreManagerException(NLS.bind(
						CertificateManagerNLS.KeyStoreModel_Error_GettingAliasesFromKeystore, getName()), e);
			}

			List<String> keyPairEntries = new ArrayList<String>();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				EntryNode keyStoreEntry = new EntryNode(this, alias);
				if (!keyStoreEntry.isKeyPairEntry()) {
					// we will not support key pairs
					entries.put(alias, keyStoreEntry);
				} else {
					// is key pair
					keyPairEntries.add(alias);
					String msg = NLS.bind(CertificateManagerNLS.KeyStoreNode_KeyPairNotMapped_LogMessage, alias);
					StudioLogger.debug(msg);
				}
			}
			if ((keyPairEntries != null) && !keyPairEntries.isEmpty()) {
				// found key pairs
				DialogWithToggleUtils.showInformation(WARN_ABOUT_UNSUPPORTED_ENTRIES_PREFERENCE,
						CertificateManagerNLS.KeyStoreNode_KeyPairNotMapped_Title,
						CertificateManagerNLS.KeyStoreNode_KeyPairNotMapped_Message);
			}

			if (entries.isEmpty()) {
				entries.put(DUMMY_NODE, new EntryDummyNode(this));
			}
		} else {
			setNodeStatus(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID,
					CertificateManagerNLS.KeyStoreNode_UseRefresh_StatusNode));
		}
	}

	private void updateStatus() {
		setNodeStatus(Status.OK_STATUS);
		if (!keyStoreFile.exists()) {
			setNodeStatus(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID,
					CertificateManagerNLS.KeyStoreNode_KeystoreFileNotFound));
		}
	}

	@Override
	public void addChild(ITreeNode newChild) {
		if (entries.size() == 1) {
			ITreeNode entryNode = entries.get(DUMMY_NODE);
			if (entryNode != null) {
				entries.remove(DUMMY_NODE);
				KeyStoreModelEventManager.getInstance().fireEvent(entryNode, EventType.REMOVE);
			}
		}
		if ((newChild instanceof IKeyStoreEntry) || (newChild instanceof EntryDummyNode)) {
			EntryNode entryNode = (EntryNode) newChild;
			String alias = entryNode.getAlias();
			entries.put(alias, entryNode);
			if (!quiet && !(newChild instanceof EntryDummyNode)) {
				KeyStoreModelEventManager.getInstance().fireEvent(newChild, EventType.ADD);
			}
		}
	}

	/**
	 * @return the lastBackupDate
	 */
	@Override
	public Date getLastBackupDate() {
		return lastBackupDate;
	}

	/**
	 * @param lastBackupDate
	 *            the lastBackupDate to set
	 */
	@Override
	public void setLastBackupDate(Date lastBackupDate) {
		this.lastBackupDate = lastBackupDate;
		try {
			KeyStoreManager.getInstance().setBackupDate(this, lastBackupDate);
		} catch (KeyStoreManagerException e) {
			StudioLogger.error("Could not set backup date for keystore");
		}
		KeyStoreModelEventManager.getInstance().fireEvent(this, EventType.UPDATE);
	}

	/**
	 * @return the type
	 */
	@Override
	public String getType() {
		return type != null ? type : KeyStore.getDefaultType().toUpperCase();
	}

	/**
	 * @param type
	 *            the type to set
	 * @throws KeyStoreManagerException
	 */
	@Override
	public void setType(String type) throws KeyStoreManagerException {
		this.type = type;
		KeyStoreManager.getInstance().updateKeyStoreType(this);
	}

	@Override
	public List<IKeyStoreEntry> getEntries(String password) throws KeyStoreManagerException, InvalidPasswordException {
		getKeyStore(password);
		ArrayList<IKeyStoreEntry> children = new ArrayList<IKeyStoreEntry>(entries.size());
		for (ITreeNode treeNode : entries.values()) {
			if (treeNode instanceof IKeyStoreEntry) {
				children.add((IKeyStoreEntry) treeNode);
			}
		}
		return children;
	}

	@Override
	public IKeyStoreEntry getEntry(String alias, String keystorePassword) throws KeyStoreManagerException,
			InvalidPasswordException {
		IKeyStoreEntry result = null;
		for (IKeyStoreEntry entry : getEntries(keystorePassword)) {
			if (entry.getAlias().equalsIgnoreCase(alias)) {
				result = entry;
			}
		}

		return result;
	}

	@Override
	public List<String> getAliases(String password) throws KeyStoreManagerException, InvalidPasswordException {
		getKeyStore(password);

		ArrayList<String> children = new ArrayList<String>(entries.size());
		for (ITreeNode treeNode : entries.values()) {
			if (treeNode instanceof IKeyStoreEntry) {
				children.add(((IKeyStoreEntry) treeNode).getAlias());
			}
		}
		return children;
	}

	@Override
	public void removeKey(String alias) throws KeyStoreManagerException {
		String password = passwordProvider.getKeyStorePassword(true, true);
		if (password != null) {
			KeyStoreUtils.deleteEntry(keyStore, password.toCharArray(), keyStoreFile, alias);
			try {
				forceReload(password.toCharArray(), false);
			} catch (InvalidPasswordException e) {
				// Should never happen.
				StudioLogger.debug("Could reload ks after removing entry, invalid password"); //$NON-NLS-1$
			}

			ITreeNode entryNode = entries.remove(alias);
			KeyStoreModelEventManager.getInstance().fireEvent(entryNode, EventType.REMOVE);
			if (entries.isEmpty()) {
				EntryDummyNode entryDummyNode = new EntryDummyNode(this);
				entries.put(DUMMY_NODE, entryDummyNode);
				KeyStoreModelEventManager.getInstance().fireEvent(entryDummyNode, EventType.ADD);
			}
		} else {
			// password not found
			throw new KeyStoreManagerException(
					CertificateManagerNLS.KeyStoreNode_NotFoundOrIncorrectPasswordToDeleteEntry + alias);
		}

	}

	@Override
	public void removeKeys(List<String> aliases) throws KeyStoreManagerException {
		String password = passwordProvider.getKeyStorePassword(true, true);
		if (password != null) {
			for (String alias : aliases) {
				KeyStoreUtils.deleteEntry(keyStore, password.toCharArray(), keyStoreFile, alias);

				ITreeNode entryNode = entries.remove(alias);
				KeyStoreModelEventManager.getInstance().fireEvent(entryNode, EventType.REMOVE);
			}
			try {
				forceReload(password.toCharArray(), false);
			} catch (InvalidPasswordException e) {
				// Should never happen.
				StudioLogger.debug("Could reload ks after removing entry, invalid password"); //$NON-NLS-1$
			}
			if (entries.isEmpty()) {
				EntryDummyNode entryDummyNode = new EntryDummyNode(this);
				entries.put(DUMMY_NODE, entryDummyNode);
				KeyStoreModelEventManager.getInstance().fireEvent(entryDummyNode, EventType.ADD);
			}
		} else {
			// password not found
			throw new KeyStoreManagerException(
					CertificateManagerNLS.KeyStoreNode_IncorrectPasswordToDeleteEntries_Error);
		}

	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean result = super.testAttribute(target, name, value);
		if (name.equals(PROP_NAME_NODE_STATUS)) {
			if (value.equals(PROP_VALUE_NODE_STATUS_ERROR)) {
				if (!isStoreTypeCorrect()) {
					// when store type is incorrect the icon is changed, not
					// decorated.
					result = false;
				} else if (!keyStoreFile.exists()) {
					// keystore not found
					result = true;
					setTooltip(CertificateManagerNLS.KeyStoreNode_ErrorKeystoreNotFound);
				}
			} else if (value.equals(PROP_VALUE_NODE_STATUS_KEYSTORE_TYPE_OK)) {
				result = isStoreTypeCorrect();
			}

		}
		return result;
	}

	@Override
	public void forceReload(char[] password, boolean updateUi) throws KeyStoreManagerException,
			InvalidPasswordException {
		keyStore = loadKeystore(password);

		if (updateUi) {
			skipNextReload = true;
			KeyStoreModelEventManager.getInstance().fireEvent(this, EventType.REFRESH);
		}
	}

	@Override
	protected boolean isPasswordSaved() {
		PasswordProvider pp = new PasswordProvider(getFile());
		return pp.isPasswordSaved();
	}

	protected boolean isStoreTypeCorrect() {
		return getNodeStatus().getCode() != WRONG_KEYSTORE_TYPE_ERROR_CODE;
	}

}
