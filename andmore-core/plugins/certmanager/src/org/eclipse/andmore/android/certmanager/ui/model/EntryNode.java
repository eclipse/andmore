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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreUtils;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Represents one keystore element in {@link KeystoreManagerView}. It can be
 * {@link Certificate} or {@link Key}.
 */
public class EntryNode extends AbstractTreeNode implements IKeyStoreEntry {
	/**
	 * The constant contains the key pair DER object identifier.
	 */
	public static final String KEY_PAIR_DER_OBJ_ID = "2.16.840.1.113793.23"; //$NON-NLS-1$    

	public static final int KEY_PASSWORD_MIN_SIZE = 6;

	protected String alias;

	private final String KEY_NONSAVED_PASSWORD_ICON_PATH = "icons/key.png"; //$NON-NLS-1$

	private final String KEY_SAVED_PASSWORD_ICON_PATH = "icons/key_saved_password.png"; //$NON-NLS-1$

	protected EntryNode() {

	}

	/**
	 * 
	 * @param keyStoreModel
	 * @param alias
	 * @throws KeyStoreManagerException
	 *             if the alias is already listed in the tree
	 */
	public EntryNode(ITreeNode keyStoreModel, String alias) throws KeyStoreManagerException {
		this.alias = alias.toLowerCase();
		setParent(keyStoreModel);
		if (!isKeyPairEntry()) {
			keyStoreModel.addChild(this);
		}

		// notify key entry addition
		// KeyStoreModelEventManager.getInstance().fireEvent(this,
		// KeyStoreModelEvent.EventType.ADD);

		// Obtaining certificate to get tooltip information
		X509Certificate cert = getX509Certificate();
		if (cert != null) {
			X500Name x500name;
			try {
				x500name = new JcaX509CertificateHolder(cert).getSubject();

				RDN commonName = x500name.getRDNs(BCStyle.CN).length >= 1 ? x500name.getRDNs(BCStyle.CN)[0] : null;
				RDN organization = x500name.getRDNs(BCStyle.O).length >= 1 ? x500name.getRDNs(BCStyle.O)[0] : null;

				// Adding tooltip information
				String org = organization != null ? organization.getFirst().getValue().toString()
						: CertificateManagerNLS.CertificateInfoDialog_NotAvailableProperty;
				String name = commonName != null ? commonName.getFirst().getValue().toString()
						: CertificateManagerNLS.CertificateInfoDialog_NotAvailableProperty;
				this.setTooltip(NLS.bind(CertificateManagerNLS.CertificateBlock_KeyTooltip, org, name));
			} catch (CertificateEncodingException e) {
				String errorMsg = "Error getting data from certificate";
				StudioLogger.error(EntryNode.class, errorMsg, e);
				throw new KeyStoreManagerException(errorMsg, e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #getKeyStoreNode()
	 */
	@Override
	public IKeyStore getKeyStoreNode() {
		return (KeyStoreNode) getParent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #getAlias()
	 */
	@Override
	public String getAlias() {
		return alias;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #isCertificateEntry()
	 */
	@Override
	public boolean isCertificateEntry() throws KeyStoreException, KeyStoreManagerException {
		return getKeyStoreNode().getKeyStore().isCertificateEntry(alias);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #isKeyEntry()
	 */
	@Override
	public boolean isKeyEntry() throws KeyStoreException, KeyStoreManagerException {
		return getKeyStoreNode().getKeyStore().isKeyEntry(alias);
	}

	@Override
	public boolean isKeyPairEntry() {
		X509Certificate certificate = getX509Certificate();
		Set<String> criticalOIDs = certificate.getCriticalExtensionOIDs();
		return (criticalOIDs != null) && criticalOIDs.contains(KEY_PAIR_DER_OBJ_ID);
	}

	/**
	 * @return {@link Certificate} if alias represents a certificate or null if
	 *         the alias was not found (or if the type is not Certificate for
	 *         the alias)
	 * @throws KeyStoreException
	 *             if keystore not loaded yet
	 * @throws KeyStoreManagerException
	 */
	private Certificate getCertificate() throws KeyStoreException, KeyStoreManagerException {
		Certificate certificate = null;
		KeyStore keyStore = getKeyStoreNode().getKeyStore();
		if (keyStore.isCertificateEntry(alias)) {
			certificate = keyStore.getCertificate(alias);
		} else {
			// unknown type
			StudioLogger.error(NLS.bind(CertificateManagerNLS.EntryNode_NotFoundOrTypeWrong, alias));
		}
		return certificate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #getKey(java.lang.String)
	 */
	@Override
	public Key getKey(String password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
			KeyStoreManagerException {
		Key key = null;
		KeyStore keyStore = getKeyStoreNode().getKeyStore();
		if (keyStore.isKeyEntry(alias)) {
			key = keyStore.getKey(alias, password.toCharArray());
		}

		return key;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry
	 * #getPrivateKey(java.lang.String)
	 */
	@Override
	public PrivateKey getPrivateKey(String password) throws UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException, KeyStoreManagerException, InvalidKeyException {
		Key key = this.getKey(password);

		if (!(key instanceof PrivateKey)) {
			throw new InvalidKeyException("This is not a private key");
		}

		return (PrivateKey) key;
	}

	public Entry getKeyEntry(String password) throws KeyStoreException, NoSuchAlgorithmException,
			KeyStoreManagerException, UnrecoverableEntryException {
		Entry key = null;
		KeyStore keyStore = getKeyStoreNode().getKeyStore();
		if (keyStore.isKeyEntry(alias)) {
			key = keyStore.getEntry(alias, new KeyStore.PasswordProtection(password.toCharArray()));
		}
		return key;
	}

	/**
	 * Get all the certificates associated to this entry
	 * 
	 * @return an Array of {@link Certificate}
	 * @throws KeyStoreException
	 * @throws KeyStoreManagerException
	 */
	private Certificate[] getCertificateChain() throws KeyStoreException, KeyStoreManagerException {
		KeyStore keyStore = getKeyStoreNode().getKeyStore();
		return keyStore.getCertificateChain(alias);
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
		result = (prime * result) + ((alias == null) ? 0 : alias.hashCode());
		result = (prime * result) + ((getKeyStoreNode() == null) ? 0 : getKeyStoreNode().hashCode());
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
		if (!(obj instanceof EntryNode)) {
			return false;
		}
		EntryNode other = (EntryNode) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}
		if (getKeyStoreNode() == null) {
			if (other.getKeyStoreNode() != null) {
				return false;
			}
		} else if (!getKeyStoreNode().equals(other.getKeyStoreNode())) {
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
		return "KeyStoreEntry [alias=" + alias + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void refresh() {
		// keys does not need to be refreshed
	}

	@Override
	public String getId() {
		return alias;
	}

	@Override
	public String getName() {
		return alias;
	}

	@Override
	public ImageDescriptor getIcon() {
		// decision: we will not support key-pair, so we will have just key
		// items below keystore node.
		ImageDescriptor descr = null;
		if (isPasswordSaved()) {
			// saved password
			descr = AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
					KEY_SAVED_PASSWORD_ICON_PATH);
		} else {
			// non saved password
			descr = AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
					KEY_NONSAVED_PASSWORD_ICON_PATH);
		}
		return descr;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public List<ITreeNode> getChildren() {
		return new ArrayList<ITreeNode>(0); // it is the leaf of the tree
	}

	public static IKeyStoreEntry createSelfSignedNode(IKeyStore keystore, String keyStorePass, String alias,
			CertificateDetailsInfo certificateDetailsInfo) throws KeyStoreManagerException {
		KeyPair keyPair = null;

		try {
			keyPair = KeyStoreUtils.genKeyPair();
			X509Certificate x509Certificate = KeyStoreUtils.createX509Certificate(keyPair, certificateDetailsInfo);

			if (keyStorePass == null) {
				PasswordProvider provider = new PasswordProvider(keystore.getFile());
				keyStorePass = provider.getKeyStorePassword(true);
			}

			PrivateKeyEntry privateKeyEntry = KeyStoreUtils.createPrivateKeyEntry(keyPair, x509Certificate);
			KeyStoreUtils.addEntry(keystore.getKeyStore(), keyStorePass.toCharArray(), keystore.getFile(), alias,
					privateKeyEntry, certificateDetailsInfo.getEntryPassword().toCharArray());

			// force reload - because keystore cache can be old due to key
			// entries additions/removals
			keystore.forceReload(keyStorePass.toCharArray(), false);
		} catch (Exception e) {
			throw new KeyStoreManagerException(e.getMessage(), e);
		}

		return new EntryNode((ITreeNode) keystore, alias);
	}

	public static IKeyStoreEntry createSelfSignedNode(IKeyStore keystore, String alias,
			CertificateDetailsInfo certificateDetailsInfo) throws KeyStoreManagerException {
		return createSelfSignedNode(keystore, null, alias, certificateDetailsInfo);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean result = super.testAttribute(target, name, value);
		if (name.equals(PROP_NAME_NODE_STATUS)) {
			if (value.equals(PROP_VALUE_NODE_STATUS_WARNING)) {
				X509Certificate x509Certificate = getX509Certificate();
				try {
					// check validity concerning the current date
					x509Certificate.checkValidity();

					// now check validity related to magic date provided by
					// Google
					Calendar date = Calendar.getInstance();
					date.clear();
					date.set(2033, Calendar.OCTOBER, 22);
					x509Certificate.checkValidity(date.getTime());
				} catch (CertificateExpiredException e) {
					// certificate has expired in the current date; or
					// certificate has expired before 22 Oct 2033
					setTooltip(NLS.bind(CertificateManagerNLS.CertificatePeriodExpired_Issue,
							x509Certificate.getNotAfter()));
					result = true; // decorate node
				} catch (CertificateNotYetValidException e) {
					// certificate is not yet valid in the current date; or
					// certificate is not yet valid in 2033 => it must not
					// happen but we need to deal with this case
					setTooltip(NLS.bind(CertificateManagerNLS.CertificatePeriodNotYeatValid_Issue,
							x509Certificate.getNotBefore()));
					result = true; // decorate node
				}
			}
		}
		return result;

	}

	/**
	 * Get the first X509Certificate available in the entry
	 * 
	 * @return
	 */
	@Override
	public X509Certificate getX509Certificate() {
		X509Certificate x509Certificate = null;
		try {
			if (isCertificateEntry()) {
				Certificate cert = getCertificate();
				if (cert instanceof X509Certificate) {
					// Android certificate
					x509Certificate = (X509Certificate) cert;
				}
			} else if (isKeyEntry()) {
				Certificate[] chain = getCertificateChain();
				for (int i = 0; i < chain.length; i++) {
					Certificate cert = chain[i];
					if (cert instanceof X509Certificate) {
						// Android certificate
						x509Certificate = (X509Certificate) cert;
					}
				}
			}
		} catch (Exception e) {
			StudioLogger.error(EntryNode.class,
					NLS.bind(CertificateManagerNLS.EntryNode_ErrorGettingCertificateFromEntry, getAlias()), e);
		}
		return x509Certificate;
	}

	@Override
	protected boolean isPasswordSaved() {
		PasswordProvider pp = new PasswordProvider(getKeyStoreNode().getFile());
		return pp.isPasswordSaved(alias);
	}
}
