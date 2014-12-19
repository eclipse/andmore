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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;

public interface IKeyStoreEntry {
	/**
	 * @return {@link Key} if the alias represents the private key associated to
	 *         this entry or null if the alias was not found (or if the type is
	 *         not Key for the alias)
	 * @throws NoSuchAlgorithmException
	 *             if no algorithm to recover key not found
	 * @throws KeyStoreException
	 *             if keystore not loaded yet
	 * @throws UnrecoverableKeyException
	 *             if wrong password for the key
	 * @throws KeyStoreManagerException
	 */
	Key getKey(String password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
			KeyStoreManagerException;

	/**
	 * @return The key represented by this node as a PrivateKey.
	 * @throws NoSuchAlgorithmException
	 *             if no algorithm to recover key not found
	 * @throws KeyStoreException
	 *             if keystore not loaded yet
	 * @throws UnrecoverableKeyException
	 *             if wrong password for the key
	 * @throws KeyStoreManagerException
	 * @throws InvalidKeyException
	 *             If this key is not a {@link PrivateKey}.
	 * */
	PrivateKey getPrivateKey(String password) throws UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException, KeyStoreManagerException, InvalidKeyException;

	/**
	 * @return true if this entry contains a private key
	 * @throws KeyStoreException
	 *             if the keystore has not been initialized (loaded).
	 * @throws KeyStoreManagerException
	 */
	boolean isKeyEntry() throws KeyStoreException, KeyStoreManagerException;

	/**
	 * @return true if this entry contains a certificate
	 * @throws KeyStoreException
	 *             if the keystore has not been initialized (loaded).
	 * @throws KeyStoreManagerException
	 */
	boolean isCertificateEntry() throws KeyStoreException, KeyStoreManagerException;

	/**
	 * @return true if this entry is a key pair
	 */
	boolean isKeyPairEntry();

	/**
	 * @return the alias
	 */
	String getAlias();

	/**
	 * Get the first X509Certificate available in the entry
	 * 
	 * @return
	 */
	X509Certificate getX509Certificate();

	/**
	 * @return The keystore node that holds this entry.
	 * */
	public IKeyStore getKeyStoreNode();

}
