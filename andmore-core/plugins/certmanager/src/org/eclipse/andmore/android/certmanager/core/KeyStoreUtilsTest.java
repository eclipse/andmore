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
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.bouncycastle.operator.OperatorCreationException;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.model.CertificateDetailsInfo;
import org.junit.Test;

public class KeyStoreUtilsTest extends TestCase {

	File keyStoreFile = null;

	private String passwd;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		keyStoreFile = File.createTempFile("testKeystore", ".tmp");
		passwd = "passwd";
		super.setUp();
	}

	@Test
	public void testCreateKeystore() {

		KeyStore keyStore = null;
		try {
			keyStore = KeyStoreUtils.createKeystore(keyStoreFile, passwd.toCharArray());
		} catch (KeyStoreManagerException e) {
			assert (false);
		} catch (InvalidPasswordException e) {
			assert (false);
		}

		assert (keyStore != null);
		assert (keyStoreFile.length() > 0);

	}

	@Test
	public void testLoadKeyStore() {
		try {
			KeyStore keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, passwd.toCharArray(), "JKS");
			keyStore.aliases();
		} catch (KeyStoreException e) {
			assert false;
		} catch (KeyStoreManagerException e) {
			assert false;
		} catch (InvalidPasswordException e) {
			assert false;
		}
	}

	@Test
	public void testCreateCertificate() {
		try {
			KeyStore keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, passwd.toCharArray(), "JKS");

			KeyPair keyPair = KeyStoreUtils.genKeyPair();
			X509Certificate x509Certificate = KeyStoreUtils.createX509Certificate(keyPair, new CertificateDetailsInfo(
					"test", "nome", "org", "orgUn", "testUni", "country", "Estate", "30", ""));

			PrivateKeyEntry privateKeyEntry = KeyStoreUtils.createPrivateKeyEntry(keyPair, x509Certificate);

			KeyStoreUtils.addEntry(keyStore, passwd.toCharArray(), keyStoreFile, "aliasTest", privateKeyEntry,
					"aliaspass".toCharArray());
		} catch (Exception e) {
			assert (false);
		}
	}

	@Test
	public void testChangePasswd() {

		KeyStore keyStore = null;
		try {
			keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, passwd.toCharArray());
		} catch (KeyStoreManagerException e) {
			assert false;
		} catch (InvalidPasswordException e) {
			assert false;
		}

		File keyStoreFile2 = new File(keyStoreFile + "_2");
		try {
			KeyStoreUtils.changeKeystorePasswd(keyStore, keyStoreFile2, passwd.toCharArray(),
					"newPasswd2".toCharArray());
		} catch (KeyStoreManagerException e) {
			assert (false);
		}

		assert (keyStore != null);
		assert (keyStoreFile2.length() > 0);
	}

	@Test
	public void testChangeKsType() {
		try {
			KeyStoreUtils.createKeystore(keyStoreFile, "JKS", passwd.toCharArray());
			KeyStoreUtils.changeKeyStoreType(keyStoreFile, passwd.toCharArray(), "JKS", "JCEKS",
					new HashMap<String, String>(0));
		} catch (KeyStoreManagerException e) {
			e.printStackTrace();
		} catch (InvalidPasswordException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testImportKeys() {
		File keyStoreFile1 = new File(keyStoreFile.getAbsolutePath() + "_import");
		try {
			KeyStoreUtils.createKeystore(keyStoreFile1, "pass1".toCharArray());
		} catch (KeyStoreManagerException e) {
			e.printStackTrace();
		} catch (InvalidPasswordException e) {
			e.printStackTrace();
		}
		try {
			KeyStore keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, passwd.toCharArray(), "JKS");
			KeyPair keyPair = KeyStoreUtils.genKeyPair();
			X509Certificate x509Certificate = KeyStoreUtils.createX509Certificate(keyPair, new CertificateDetailsInfo(
					"test", "nome", "org", "orgUn", "testUni", "country", "Estate", "30", ""));

			PrivateKeyEntry privateKeyEntry = KeyStoreUtils.createPrivateKeyEntry(keyPair, x509Certificate);

			KeyStoreUtils.addEntry(keyStore, passwd.toCharArray(), keyStoreFile, "aliasTest", privateKeyEntry,
					passwd.toCharArray());

			keyStore = KeyStoreUtils.loadKeystore(keyStoreFile, passwd.toCharArray(), "JKS");

			Map<String, String> aliases = new HashMap<String, String>(1);
			aliases.put("aliasTest", passwd);
			// KeyStoreUtils.importKeys(keyStore1, keyStoreFile1,
			// "pass1".toCharArray(), keyStore, keyStoreFile1
			// passwd.toCharArray(), aliases);
		} catch (KeyStoreManagerException e) {
			e.printStackTrace();
		} catch (InvalidPasswordException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (OperatorCreationException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
