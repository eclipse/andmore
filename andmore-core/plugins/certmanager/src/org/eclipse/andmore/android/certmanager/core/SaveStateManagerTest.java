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
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * See {@link SaveStateManager}
 */
public class SaveStateManagerTest extends TestCase {
	private static final String JKS = "JKS";

	SaveStateManager manager;

	File adtKeystoreFile = null;

	File motodevKeystoreFile = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		manager = SaveStateManager.getInstance();
		adtKeystoreFile = new File("C:\\Users\\gdpr78\\motodevstudio\\tools\\adt.keystore");
		motodevKeystoreFile = new File("C:\\Users\\gdpr78\\motodevstudio\\tools\\motodev.keystore");

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		manager.removeEntry(adtKeystoreFile);
		manager.removeEntry(motodevKeystoreFile);
	}

	@Test
	public void testAddEntryWithoutBackupDate() {
		try {
			manager.addEntry(adtKeystoreFile, JKS);

			SaveStateManager.ViewStateEntry entry = manager.getEntry(adtKeystoreFile);

			assert ((entry != null) && (entry.getKeystoreFile() != null) && entry.getKeystoreFile().equals(
					adtKeystoreFile));
			assert ((entry != null) && (entry.getKeystoreType() != null) && entry.getKeystoreType().equals(JKS));
		} catch (Exception e) {
			// error
			assert (false);
		}
	}

	@Test
	public void testListKeystoresMapped() {
		try {
			manager.addEntry(adtKeystoreFile, JKS);
			manager.addEntry(motodevKeystoreFile, "JKS");

			assert ((manager.getMappedKeystores() != null) && (manager.getMappedKeystores().size() == 2)
					&& manager.getMappedKeystores().contains(adtKeystoreFile) && manager.getMappedKeystores().contains(
					motodevKeystoreFile));
		} catch (Exception e) {
			// error
			assert (false);
		}
	}

	@Test
	public void testSetBackupDate() {
		try {
			Date date = Calendar.getInstance().getTime();
			manager.addEntry(adtKeystoreFile, JKS);
			manager.setBackupDate(adtKeystoreFile, date);

			SaveStateManager.ViewStateEntry entry = manager.getEntry(adtKeystoreFile);

			assert ((entry != null) && (entry.getBackupDate() != null) && entry.getBackupDate().equals(date));
		} catch (Exception e) {
			// error
			assert (false);
		}
	}

	@Test
	public void testIsMappedKeystore() {
		try {
			manager.addEntry(adtKeystoreFile, JKS);
			boolean result = manager.isKeystoreMapped(adtKeystoreFile);
			assert (result == true);
		} catch (Exception e) {
			// error
			assert (false);
		}
	}

	@Test
	public void testRemoveEntry() {
		try {
			manager.addEntry(motodevKeystoreFile, "JKS");
			assert (manager.isKeystoreMapped(motodevKeystoreFile) == true);

			manager.removeEntry(motodevKeystoreFile);
			assert (manager.isKeystoreMapped(motodevKeystoreFile) == false);
		} catch (Exception e) {
			// error
			assert (false);
		}
	}

}
