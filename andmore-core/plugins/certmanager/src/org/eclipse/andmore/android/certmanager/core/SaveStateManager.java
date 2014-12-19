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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * Saves the state of the {@link KeystoreManagerView} with: - the keystores
 * mapped - the last backup date from a keystore
 */
class SaveStateManager {
	private static final String FILE_COMMENT = "-- Signing and keys view persistence --";

	private static final String STATE_FILENAME = "state.properties";

	private final File persistenceFile = new File(System.getProperty("user.home") + File.separator + "."
			+ CertificateManagerActivator.PLUGIN_ID, STATE_FILENAME);

	private static final String KEYSTORE_TYPE = "keystore_type=";

	private static final String BACKUP_DATE = "backup_date=";

	protected class ViewStateEntry {
		private final File keystoreFile;

		private String keystoreType;

		private Date backupDate;

		public ViewStateEntry(File keystoreFile) {

			this.keystoreFile = keystoreFile;
		}

		public ViewStateEntry(File keystoreFile, String keystoreType) {

			this.keystoreFile = keystoreFile;
			this.keystoreType = keystoreType;
		}

		/**
		 * @return the backupDate
		 */
		public Date getBackupDate() {
			return backupDate;
		}

		/**
		 * @param backupDate
		 *            the backupDate to set
		 */
		public void setBackupDate(Date backupDate) {
			this.backupDate = backupDate;
		}

		/**
		 * @return the keystoreType
		 */
		public String getKeystoreType() {
			return keystoreType;
		}

		/**
		 * @param keystoreType
		 *            the keystoreType to set
		 */
		public void setKeystoreType(String keystoreType) {
			this.keystoreType = keystoreType;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ViewStateEntry [keystoreFile=" + keystoreFile + ", keystoreType=" + keystoreType + ", backupDate="
					+ backupDate + "]";
		}

		/**
		 * @return the keystoreFile
		 */
		protected File getKeystoreFile() {
			return keystoreFile;
		}
	}

	private static SaveStateManager _instance;

	/**
	 * This class is a singleton.
	 * 
	 * @return The unique instance of this class.
	 * */
	public synchronized static SaveStateManager getInstance() throws IOException {
		if (_instance == null) {
			_instance = new SaveStateManager();
		}
		return _instance;
	}

	private SaveStateManager() throws IOException {
		// create folder if it does not exist
		if (!persistenceFile.getParentFile().exists()) {
			persistenceFile.getParentFile().mkdirs();
		}
		if (!persistenceFile.exists()) {
			// init file
			persistenceFile.createNewFile();
			store(new Properties());
		}
	}

	private String write(ViewStateEntry entry) {
		StringBuffer buffer = new StringBuffer();
		if (entry.getKeystoreType() != null) {
			buffer.append(KEYSTORE_TYPE + entry.getKeystoreType() + File.pathSeparator);
		}
		if (entry.getBackupDate() != null) {
			buffer.append(BACKUP_DATE + entry.getBackupDate().getTime());
		}
		return buffer.toString();
	}

	/**
	 * List all mapped keystores.
	 * 
	 * @throws IOException
	 *             If there are problems loading the persistence file.
	 */
	public Set<File> getMappedKeystores() throws IOException {
		Set<File> mappedKeystores = new HashSet<File>();
		Properties properties = load();
		Enumeration<Object> enumeration = properties.keys();
		while (enumeration.hasMoreElements()) {
			Object k = enumeration.nextElement();
			if (k instanceof String) {
				String key = (String) k;
				File keystoreFile = new File(key);
				mappedKeystores.add(keystoreFile);
			}
		}
		return mappedKeystores;
	}

	/**
	 * Get a representation of the entry saved in the persistence.
	 * 
	 * @param keystoreFile
	 *            The file that will have its state retrieved.
	 * @return
	 * @throws IOException
	 *             If there are problems loading the persistence file.
	 */
	public ViewStateEntry getEntry(File keystoreFile) throws IOException {
		Properties properties = load();
		Object v = properties.get(keystoreFile.getAbsolutePath());
		ViewStateEntry entry = null;
		if (v instanceof String) {
			String value = (String) v;
			StringTokenizer stringTokenizer = new StringTokenizer(value, File.pathSeparator);
			entry = new ViewStateEntry(keystoreFile);
			while (stringTokenizer.hasMoreTokens()) {
				String token = stringTokenizer.nextToken();
				if (token.contains(KEYSTORE_TYPE)) {
					token = token.substring(KEYSTORE_TYPE.length());
					entry.setKeystoreType(token);
				} else if (token.contains(BACKUP_DATE)) {
					token = token.substring(BACKUP_DATE.length());
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(Long.parseLong(token));
					Date date = calendar.getTime();
					entry.setBackupDate(date);
				}
			}
		}
		return entry;
	}

	/**
	 * Check if a keystore is mapped on the persistence mechanism.
	 * 
	 * @param keystoreFile
	 *            The keystore to be checked.
	 * @return true if keystore mapped, false otherwise
	 * @throws IOException
	 *             If there are problems loading the persistence file.
	 */
	public boolean isKeystoreMapped(File keystoreFile) throws IOException {
		Properties properties = load();
		return properties.containsKey(keystoreFile.getAbsolutePath());
	}

	/**
	 * Adds (maps) a keystore.
	 * 
	 * @param keystoreFile
	 *            The keystore to be added to the persistence mechanism.
	 * @param keystoreType
	 *            The type of the keystore.
	 * @throws IOException
	 *             If there are problems loading the persistence file.
	 */
	public void addEntry(File keystoreFile, String keystoreType) throws IOException {
		ViewStateEntry stateEntry = new ViewStateEntry(keystoreFile, keystoreType);
		addEntry(keystoreFile, stateEntry);
	}

	private void addEntry(File keystoreFile, ViewStateEntry stateEntry) throws IOException {
		Properties prop = load();
		prop.setProperty(keystoreFile.getAbsolutePath(), write(stateEntry));
		store(prop);
	}

	/**
	 * Removes a keystore from the persistence mechanism.
	 * 
	 * @param keystoreFile
	 * @throws IOException
	 */
	public void removeEntry(File keystoreFile) throws IOException {
		Properties properties = load();
		properties.remove(keystoreFile.getAbsolutePath());
		store(properties);
	}

	/**
	 * Sets backup date from a keystore
	 * 
	 * @param keystoreFile
	 *            The keystore file.
	 * @param backupDate
	 *            The date of the backup.
	 * @throws IOException
	 *             If there are problems loading the persistence file.
	 */
	public void setBackupDate(File keystoreFile, Date backupDate) throws IOException {
		ViewStateEntry entry = getEntry(keystoreFile);
		entry.setBackupDate(backupDate);
		addEntry(keystoreFile, entry);
	}

	private Properties load() throws IOException {
		FileInputStream in = null;
		Properties props = new Properties();

		try {
			in = new FileInputStream(persistenceFile);
			props.loadFromXML(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return props;
	}

	private void store(Properties prop) throws IOException {
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(persistenceFile);
			prop.storeToXML(out, FILE_COMMENT);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while saving properties. " + e.getMessage());
				}
			}
		}
	}
}
