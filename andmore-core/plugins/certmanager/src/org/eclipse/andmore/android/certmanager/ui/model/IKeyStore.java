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
import java.util.Date;
import java.util.List;

import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;

public interface IKeyStore {

	public int KEYSTORE_PASSWORD_MIN_SIZE = 6;

	public int WRONG_KEYSTORE_TYPE_ERROR_CODE = 1;

	PasswordProvider getPasswordProvider();

	List<String> getAliases(String password) throws KeyStoreManagerException, InvalidPasswordException;

	/**
	 * Returns the {@link KeyStore}
	 * 
	 * @throws KeyStoreManagerException
	 */
	KeyStore getKeyStore() throws KeyStoreManagerException;

	/**
	 * Load if needed and returns all entries for this KeyStore
	 * 
	 * @param password
	 * @return
	 * @throws KeyStoreManagerException
	 * @throws InvalidPasswordException
	 */
	List<IKeyStoreEntry> getEntries(String password) throws KeyStoreManagerException, InvalidPasswordException;

	/**
	 * Load if needed and returns the entry with the given alias.
	 * 
	 * @param alias
	 *            The alias of the desired entry.
	 * @param keystorePassword
	 *            the password of the keystore
	 * @return The desired entry.
	 * @throws KeyStoreManagerException
	 * @throws InvalidPasswordException
	 */
	IKeyStoreEntry getEntry(String alias, String keystorePassword) throws KeyStoreManagerException,
			InvalidPasswordException;

	/**
	 * @param password
	 *            from keystore Forces keystore reload
	 * @throws KeyStoreManagerException
	 * @throws InvalidPasswordException
	 */
	void forceReload(char[] charArray, boolean updateUi) throws KeyStoreManagerException, InvalidPasswordException;

	/**
	 * Returns this key store file
	 * 
	 * @return
	 */
	File getFile();

	/**
	 * @return this keystore type
	 */
	String getType();

	/**
	 * Set this keystore type. This is intended to be used only during creation.
	 * This method won't change the keystore type or convert it to another type.
	 * 
	 * @param type
	 * @throws KeyStoreManagerException
	 */
	void setType(String type) throws KeyStoreManagerException;

	/**
	 * Set the backup date for this keystore
	 * 
	 * @param lastBackupDate
	 */
	void setLastBackupDate(Date lastBackupDate);

	/**
	 * Gets the last backup date for this keystore
	 * 
	 * @return null if not backed up yet, a date otherwise
	 */
	Date getLastBackupDate();

	/**
	 * Deletes a key entry with the given alias
	 * 
	 * @param alias
	 *            The alias representing the key to be removed.
	 * @throws KeyStoreManagerException
	 */
	void removeKey(String alias) throws KeyStoreManagerException;

	/**
	 * Deletes a list of key entries from the keystore.
	 * 
	 * @param aliases
	 *            The list of aliases representing the keys to be removed.
	 * @throws KeyStoreManagerException
	 */
	void removeKeys(List<String> aliases) throws KeyStoreManagerException;

	/**
	 * @param password
	 */
	public boolean isPasswordValid(String password) throws KeyStoreManagerException, InvalidPasswordException;

	/**
	 * Return the password of the keystore. If the password is not saved and
	 * {@code promptPassword} is set to {@code true}, then a dialog will be
	 * opened so the user can enter the password.
	 * 
	 * @return Return the password of the keystore.
	 */
	String getKeyStorePassword(boolean promptPassword);
}
