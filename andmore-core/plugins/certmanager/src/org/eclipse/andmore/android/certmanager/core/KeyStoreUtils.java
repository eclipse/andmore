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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStrictStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.CertificateDetailsInfo;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.osgi.util.NLS;

public class KeyStoreUtils {
	private static final String ERROR_DELETING_ALIAS = CertificateManagerNLS.KeyStoreUtils_ErrorDeletingAlias;

	/**
	 * Creates a new empty KeyStore, from the default type, located at
	 * keyStoreFile with the password, password
	 * 
	 * @param keyStoreFile
	 *            The file pointing o where the new KeyStore will be located
	 * @param password
	 *            the password for the new KeyStore
	 * @return the {@link KeyStore} representing the new KeyStore
	 * @throws InvalidPasswordException
	 * @throws KeyStoreException
	 *             if KeyStore can't be created
	 */
	public static KeyStore createKeystore(File keyStoreFile, char[] password) throws KeyStoreManagerException,
			InvalidPasswordException {
		return createKeystore(keyStoreFile, KeyStore.getDefaultType(), password);
	}

	/**
	 * Creates a new empty KeyStore, located at keyStoreFile with the password,
	 * password
	 * 
	 * @param keyStoreFile
	 *            The file pointing o where the new KeyStore will be located
	 * @param keyStoreType
	 *            The type of the new KeyStore
	 * @param password
	 *            the password for the new KeyStore
	 * @return the {@link KeyStore} representing the new KeyStore
	 * @throws InvalidPasswordException
	 * @throws KeyStoreException
	 *             if KeyStore can't be created
	 */
	public static KeyStore createKeystore(File keyStoreFile, String keyStoreType, char[] password)
			throws KeyStoreManagerException, InvalidPasswordException {
		KeyStore keyStore = null;
		if ((keyStoreFile != null) && !keyStoreFile.exists()) {
			keyStore = loadKeystore(keyStoreFile, password, keyStoreType);
			try {
				writeKeyStore(keyStore, password, keyStoreFile);
			} catch (Exception e) {
				throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_WriteKeyStore,
						keyStoreFile), e);
			}
		} else {
			throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_FileAlreadyExists,
					keyStoreFile));
		}

		return keyStore;
	}

	public static void writeKeyStore(KeyStore keyStore, char[] password, File keyStoreFile)
			throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, KeyStoreManagerException, InvalidPasswordException {

		writeKeyStore(keyStore, null, password, keyStoreFile);
	}

	private static void writeKeyStore(KeyStore keyStore, char[] oldPassword, char[] newPassword, File keyStoreFile)
			throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, KeyStoreManagerException, InvalidPasswordException {
		FileOutputStream fos = null;
		try {
			if (oldPassword != null) {
				if (loadKeystore(keyStoreFile, oldPassword, keyStore.getType()) != null) {
					fos = new FileOutputStream(keyStoreFile);
					keyStore.store(fos, newPassword);
				}
			} else {
				fos = new FileOutputStream(keyStoreFile);
				keyStore.store(fos, newPassword);
			}
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close steam while writing keystore file. " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Loads a KeyStore from a given file from the default type, usually JKS. If
	 * keyStoreFile path don't exist then a new empty KeyStore will be created
	 * on the given location. <b>Note:</b> Calling this method is the same as
	 * calling loadKeystore(keyStoreFile, password, KeyStore.getDefaultType())
	 * 
	 * @param keyStoreFile
	 *            The keyStore location.
	 * @param password
	 *            The KeyStore password
	 * @return the {@link KeyStore} representing the file.
	 * @throws KeyStoreManagerException
	 * @throws InvalidPasswordException
	 */
	public static KeyStore loadKeystore(File keyStoreFile, char[] password) throws KeyStoreManagerException,
			InvalidPasswordException {
		return loadKeystore(keyStoreFile, password, KeyStore.getDefaultType());
	}

	/**
	 * Loads a KeyStore from a given file. If keyStoreFile path don't exist then
	 * a new empty KeyStore will be created on memory. If you want o create a
	 * new KeyStore file, calling createStore is recommended.
	 * 
	 * @param keyStoreFile
	 *            The keyStore location.
	 * @param password
	 *            The KeyStore password
	 * @param storeType
	 *            The Type of the keystore o be loaded.
	 * @return the {@link KeyStore} representing the file.
	 * @throws KeyStoreManagerException
	 * @throws InvalidPasswordException
	 */
	public static KeyStore loadKeystore(File keyStoreFile, char[] password, String storeType)
			throws KeyStoreManagerException, InvalidPasswordException {
		KeyStore keyStore = null;
		FileInputStream fis = null;
		try {
			keyStore = KeyStore.getInstance(storeType);

			if ((keyStoreFile != null) && keyStoreFile.exists() && (keyStoreFile.length() > 0)) {
				fis = new FileInputStream(keyStoreFile);
			}

			// fis = null means a new keyStore will be created
			keyStore.load(fis, password);
		} catch (IOException e) {
			if (e.getMessage().contains("password was incorrect")
					|| (e.getCause() instanceof UnrecoverableKeyException)) {
				throw new InvalidPasswordException(e.getMessage());
			} else {
				throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_LoadKeyStore,
						keyStoreFile), e);
			}
		} catch (Exception e) {
			throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_LoadKeyStore,
					keyStoreFile), e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close steam while loading keystore. " + e.getMessage());
				}
			}
		}

		return keyStore;
	}

	/**
	 * Simply deletes the KeyStore File
	 * 
	 * @param keyStoreFile
	 *            teh KeyStore file to be deleted.
	 * @throws KeyStoreException
	 *             If any error occur.
	 */
	public static void deleteKeystore(File keyStoreFile) throws KeyStoreManagerException {
		try {
			FileUtil.deleteFile(keyStoreFile);
		} catch (IOException e) {
			throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_DeleteKeyStore,
					keyStoreFile), e);
		}
	}

	/**
	 * Write the keyStore in to the given file, protecting it with password.
	 * Warn: Since there's actually no way to change the password this method
	 * will overwrite the existing file with the keyStore contents, without
	 * further warning.
	 * 
	 * @param keyStore
	 *            the {@link KeyStore} to be written.
	 * @param keyStoreFile
	 *            The KeyStore location
	 * @param oldPassword
	 * @param sourcePassword
	 *            the new Password
	 * @throws KeyStoreException
	 *             If file could no be write.
	 */
	public static void changeKeystorePasswd(KeyStore keyStore, File keyStoreFile, char[] oldPassword, char[] newPassword)
			throws KeyStoreManagerException {
		try {
			keyStore = loadKeystore(keyStoreFile, oldPassword, keyStore.getType());
			writeKeyStore(keyStore, oldPassword, newPassword, keyStoreFile);
		} catch (Exception e) {
			throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_WriteKeyStore,
					keyStoreFile), e);
		}
	}

	/**
	 * Adds a new enty to a given keyStore.
	 * 
	 * @param keyStore
	 *            The Keystore that will receive the entry
	 * @param keyStorePassword
	 *            The KeyStore password
	 * @param keyStoreFile
	 *            The KeyStore file path
	 * @param alias
	 *            The new entry alias
	 * @param entry
	 *            The Entry to be added
	 * @param entryPassword
	 *            The password to protect the entry
	 * @throws KeyStoreManagerException
	 *             if any error occurs.
	 */
	public static void addEntry(KeyStore keyStore, char[] keyStorePassword, File keyStoreFile, String alias,
			Entry entry, char[] entryPassword) throws KeyStoreManagerException {
		try {
			PasswordProtection passwordProtection = new KeyStore.PasswordProtection(entryPassword);
			keyStore = loadKeystore(keyStoreFile, keyStorePassword, keyStore.getType());

			if (!keyStore.containsAlias(alias)) {
				keyStore.setEntry(alias, entry, passwordProtection);
				writeKeyStore(keyStore, keyStorePassword, keyStoreFile);
			} else {
				throw new KeyStoreManagerException(NLS.bind("Alias \"{0}\" already exists.", alias));
			}

		} catch (KeyStoreManagerException e) {
			throw e;
		} catch (Exception e) {
			throw new KeyStoreManagerException(NLS.bind(CertificateManagerNLS.KeyStoreUtils_Error_AddEntryToKeyStore,
					alias), e);
		}
	}

	/**
	 * Adds a new enty to a given keyStore.
	 * 
	 * @param keyStore
	 *            The Keystore that will receive the entry
	 * @param keyStorePassword
	 *            The KeyStore password
	 * @param keyStoreFile
	 *            The KeyStore file path
	 * @param alias
	 *            The new entry alias
	 * @param entry
	 *            The Entry to be added
	 * @param entryPassword
	 *            The password to protect the entry
	 * @throws KeyStoreManagerException
	 *             if any error occurs.
	 */
	public static void changeEntryPassword(KeyStore keyStore, char[] keyStorePassword, File keyStoreFile, String alias,
			Entry entry, char[] entryPassword) throws KeyStoreManagerException {
		try {
			PasswordProtection passwordProtection = new KeyStore.PasswordProtection(entryPassword);
			keyStore.setEntry(alias, entry, passwordProtection);
			writeKeyStore(keyStore, keyStorePassword, keyStoreFile);
		} catch (Exception e) {
			throw new KeyStoreManagerException(NLS.bind("Error attempting to change password for {0}", alias), e);
		}
	}

	/**
	 * Create a new X509 certificate for a given KeyPair
	 * 
	 * @param keyPair
	 *            the {@link KeyPair} used to create the certificate,
	 *            RSAPublicKey and RSAPrivateKey are mandatory on keyPair,
	 *            IllegalArgumentExeption will be thrown otherwise.
	 * @param issuerName
	 *            The issuer name to be used on the certificate
	 * @param ownerName
	 *            The owner name to be used on the certificate
	 * @param expireDate
	 *            The expire date
	 * @return The {@link X509Certificate}
	 * @throws IOException
	 * @throws OperatorCreationException
	 * @throws CertificateException
	 */
	public static X509Certificate createX509Certificate(KeyPair keyPair, CertificateDetailsInfo certDetails)
			throws IOException, OperatorCreationException, CertificateException {

		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		if (!(publicKey instanceof RSAPublicKey) || !(privateKey instanceof RSAPrivateKey)) {
			throw new IllegalArgumentException(CertificateManagerNLS.KeyStoreUtils_RSA_Keys_Expected);
		}

		RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
		RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;

		// Transform the PublicKey into the BouncyCastle expected format
		ASN1InputStream asn1InputStream = null;
		X509Certificate x509Certificate = null;

		try {
			asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(rsaPublicKey.getEncoded()));
			SubjectPublicKeyInfo pubKey = new SubjectPublicKeyInfo((ASN1Sequence) asn1InputStream.readObject());

			X500NameBuilder nameBuilder = new X500NameBuilder(new BCStrictStyle());
			addField(BCStyle.C, certDetails.getCountry(), nameBuilder);
			addField(BCStyle.ST, certDetails.getState(), nameBuilder);
			addField(BCStyle.L, certDetails.getLocality(), nameBuilder);
			addField(BCStyle.O, certDetails.getOrganization(), nameBuilder);
			addField(BCStyle.OU, certDetails.getOrganizationUnit(), nameBuilder);
			addField(BCStyle.CN, certDetails.getCommonName(), nameBuilder);

			X500Name subjectName = nameBuilder.build();
			X500Name issuerName = subjectName;
			X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerName,
					BigInteger.valueOf(new SecureRandom().nextInt()), Calendar.getInstance().getTime(),
					certDetails.getExpirationDate(), subjectName, pubKey);

			AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA"); //$NON-NLS-1$
			AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
			BcContentSignerBuilder sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);

			// Create RSAKeyParameters, the private key format expected by
			// Bouncy Castle
			RSAKeyParameters keyParams = new RSAKeyParameters(true, rsaPrivateKey.getPrivateExponent(),
					rsaPrivateKey.getModulus());

			ContentSigner contentSigner = sigGen.build(keyParams);
			X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);

			// Convert the X509Certificate from BouncyCastle format to the
			// java.security format
			JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
			x509Certificate = certConverter.getCertificate(certificateHolder);
		} finally {
			if (asn1InputStream != null) {
				try {
					asn1InputStream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while creating X509 certificate. " + e.getMessage());
				}
			}
		}

		return x509Certificate;
	}

	private static void addField(ASN1ObjectIdentifier objectId, String value, X500NameBuilder nameBuilder) {
		if (value.length() > 0) {
			nameBuilder.addRDN(objectId, value);
		}
	}

	/**
	 * Creates a new RSA KeyPair
	 * 
	 * @return the new {@link KeyPair}
	 */
	public static KeyPair genKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA"); //$NON-NLS-1$
		keyPairGen.initialize(2048); // As recommended by Android guys, key is
										// created with 2048 bits.
		KeyPair keyPair = keyPairGen.genKeyPair();
		return keyPair;
	}

	/**
	 * Create a new private key entry inside the key pair
	 * 
	 * @param keyPair
	 * @param x509Certificate
	 * @return
	 */
	public static PrivateKeyEntry createPrivateKeyEntry(KeyPair keyPair, X509Certificate x509Certificate) {
		Certificate[] certChain = new Certificate[] { x509Certificate };
		PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), certChain);
		return privateKeyEntry;
	}

	public static void deleteEntry(KeyStore keyStore, char[] password, File keyStoreFile, String alias)
			throws KeyStoreManagerException {
		try {
			keyStore = loadKeystore(keyStoreFile, password, keyStore.getType());

			keyStore.deleteEntry(alias);
			writeKeyStore(keyStore, password, keyStoreFile);
		} catch (Exception e) {
			StudioLogger.error(KeyStoreUtils.class, ERROR_DELETING_ALIAS + alias, e);
			throw new KeyStoreManagerException(ERROR_DELETING_ALIAS + alias, e);
		}
	}

	/**
	 * Change a keyStore type.
	 * 
	 * @param keyStoreFile
	 *            The KeyStoreFile
	 * @param password
	 *            The KeyStore Password
	 * @param originalType
	 *            the original Type
	 * @param destinationType
	 *            the new KeyStore Type
	 * @throws KeyStoreManagerException
	 *             If any error occurs, the operation will be canceled and
	 *             reverted automatically.
	 * @throws InvalidPasswordException
	 */
	public static void changeKeyStoreType(File keyStoreFile, char[] password, String originalType,
			String destinationType, Map<String, String> aliases) throws KeyStoreManagerException,
			InvalidPasswordException {
		boolean rollBack = false;
		String timeStamp = Long.toString(Calendar.getInstance().getTimeInMillis());
		File oldKsFile = new File(keyStoreFile.getAbsolutePath() + "_" + timeStamp);
		oldKsFile.delete();
		boolean renamed = false;
		renamed = keyStoreFile.renameTo(oldKsFile);
		if (renamed) {
			try {
				Builder oldKsBuilder = KeyStore.Builder.newInstance(originalType, null, oldKsFile,
						new PasswordProtection(password));
				KeyStore oldKeyStore = oldKsBuilder.getKeyStore();

				KeyStore newKeyStore = createKeystore(keyStoreFile, destinationType, password);
				for (String alias : aliases.keySet()) {
					ProtectionParameter protectionParameter = new PasswordProtection(aliases.get(alias).toCharArray());
					Entry entry = oldKeyStore.getEntry(alias, protectionParameter);
					newKeyStore.setEntry(alias, entry, protectionParameter);
				}
				writeKeyStore(newKeyStore, password, keyStoreFile);
			} catch (InvalidPasswordException e) {
				rollBack = true;
				StudioLogger.error(KeyStoreUtils.class,
						"Invalid password while trying to create a new keystore, changing a keyStore type.", e);

			} catch (Exception e) {
				if (e.getMessage().contains("password was incorrect")
						|| e.getCause().getMessage().contains("password was incorrect")) {
					keyStoreFile.delete();
					oldKsFile.renameTo(keyStoreFile);
					throw new InvalidPasswordException(e.getMessage());
				} else {
					StudioLogger.error(KeyStoreUtils.class,
							"Exception occurred while attempting to change a keyStore type.", e);
					rollBack = true;
				}
			}

			if (rollBack) {
				keyStoreFile.delete();
				oldKsFile.renameTo(keyStoreFile);

				throw new KeyStoreManagerException(NLS.bind("Could not convert the KeyStore {0} to type {1}",
						keyStoreFile, destinationType));
			}
		} else {
			throw new KeyStoreManagerException(
					NLS.bind(
							"Could not convert the KeyStore {0} to type {1}, could not backup the current keyStore file, maybe it's in use by another program.",
							keyStoreFile, destinationType));
		}
		oldKsFile.delete();
	}

	/**
	 * Import a set of entries from sourcekeystore into the targetkeystore. If
	 * alias already exists on the target keystore then the alias is
	 * concatenated with the source keystore file name.
	 * 
	 * @param targetKeyStore
	 * @param targetFile
	 * @param targetType
	 * @param targetPasswd
	 * @param sourceKeyStore
	 * @param sourceKeyStoreFile
	 * @param sourcePasswd
	 * @param aliases
	 *            a map<String, String> containing alias as key and its password
	 *            as value. this method assume that the password is correct
	 * @throws InvalidPasswordException
	 * @throws KeyStoreManagerException
	 */
	public static void importKeys(KeyStore targetKeyStore, File targetFile, String targetType, char[] targetPasswd,
			KeyStore sourceKeyStore, File sourceKeyStoreFile, char[] sourcePasswd, Map<String, String> aliases)
			throws InvalidPasswordException, KeyStoreManagerException {
		if (!isValidKeyStorePasswd(targetFile, targetType, targetPasswd)) {
			throw new InvalidPasswordException(CertificateManagerNLS.PasswordChanged_InvalidKeystorePassword);
		}

		try {
			for (String alias : aliases.keySet()) {
				if (sourceKeyStore.containsAlias(alias)) {
					ProtectionParameter protectionParameter = new PasswordProtection(aliases.get(alias).toCharArray());
					Entry entry = sourceKeyStore.getEntry(alias, protectionParameter);
					if (targetKeyStore.containsAlias(alias)) {
						alias += "_" + sourceKeyStoreFile.getName();
					}
					int i = 1;
					while (targetKeyStore.containsAlias(alias)) {
						alias += "_" + i;
						i++;
					}
					targetKeyStore.setEntry(alias, entry, protectionParameter);
				} else {
					StudioLogger.error(KeyStoreUtils.class, NLS.bind(
							"Alias {0} could not be imported because it doesn't exists on originKeyStore", alias));
				}
			}
			writeKeyStore(targetKeyStore, targetPasswd, targetFile);
		} catch (Exception e) {
			throw new KeyStoreManagerException("Could not import the selected aliases into " + targetFile.getName(), e);
		}
	}

	/**
	 * Verifies if the password if valid
	 * 
	 * @param keyStoreFile
	 * @param keyStoreType
	 * @param passwd
	 * @return true if password is valid, false otherwise.
	 * @throws KeyStoreManagerException
	 */
	public static boolean isValidKeyStorePasswd(File keyStoreFile, String keyStoreType, char[] passwd)
			throws KeyStoreManagerException {
		KeyStore keystore = null;
		try {
			keystore = loadKeystore(keyStoreFile, passwd, keyStoreType);
		} catch (InvalidPasswordException e) {
			// Do nothing, password is invalid
		}
		return keystore != null;
	}
}
