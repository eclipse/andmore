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
package org.eclipse.andmore.android.certmanager.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.dialogs.BackupDialog;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to execute the backup wizard.
 * */
public class BackupHandler extends AbstractHandler2 implements IHandler {

	public static final String KS_TYPES_FILENAME = "KsTypes.csv"; //$NON-NLS-1$

	private static final String KEYSTORE_EXT = ".keystore"; //$NON-NLS-1$

	private static Date lastBackupDate = new Date();

	private final Calendar cal = GregorianCalendar.getInstance();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		BackupDialog dialog = new BackupDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			List<IKeyStore> keyStores = KeyStoreManager.getInstance().getKeyStores();
			List<String> initialInputList = new ArrayList<String>(keyStores.size());
			for (int i = 0; i < keyStores.size(); i++) {
				boolean insert = true;
				IKeyStore keyStore = keyStores.get(i);
				if (keyStore instanceof ITreeNode) {
					ITreeNode ksNode = (ITreeNode) keyStore;
					IStatus nodeStatus = ksNode.getNodeStatus();
					if (!nodeStatus.isOK() && (nodeStatus.getCode() == IKeyStore.WRONG_KEYSTORE_TYPE_ERROR_CODE)) {
						insert = false;
					}
				}
				if (insert) {
					initialInputList.add(keyStore.getFile().getAbsolutePath());
				}
			}

			dialog.setInput(initialInputList.toArray(new String[initialInputList.size()]));
			dialog.selectKeyStores(getSelection());
		} catch (KeyStoreManagerException e) {
			throw new ExecutionException(e.getLocalizedMessage());
		}

		int diagReturn = dialog.open();
		if (diagReturn == Window.OK) {
			File archiveFile = dialog.getArchiveFile();
			List<String> selectedKeyStores = dialog.getSelectedKeyStores();
			if (FileUtil.canWrite(archiveFile)) {
				try {
					updateBackupDate(selectedKeyStores);
					createZipArchive(archiveFile, selectedKeyStores);

				} catch (KeyStoreManagerException e) {
					EclipseUtils.showErrorDialog(CertificateManagerNLS.BackupHandler_Error_BackUp_Title,
							CertificateManagerNLS.BackupHandler_Error_Setting_Date);
				} catch (IOException e) {
					EclipseUtils.showErrorDialog(CertificateManagerNLS.BackupHandler_Error_BackUp_Title,
							NLS.bind(CertificateManagerNLS.BackupHandler_Error_Writing_Archive, archiveFile));
				}
			} else {
				EclipseUtils.showErrorDialog(CertificateManagerNLS.BackupHandler_Error_BackUp_Title,
						NLS.bind(CertificateManagerNLS.BackupHandler_Error_Writing_Archive, archiveFile));
			}

		}
		return null;
	}

	private void updateBackupDate(List<String> selectedKeyStores) throws KeyStoreManagerException {
		KeyStoreManager keyStoreManager = KeyStoreManager.getInstance();
		List<IKeyStore> keyStores = keyStoreManager.getKeyStores();
		Long lastDateInMillis = cal.getTimeInMillis();
		lastBackupDate.setTime(lastDateInMillis);

		for (IKeyStore keyStore : keyStores) {
			if (selectedKeyStores.contains(keyStore.getFile().getAbsolutePath())) {
				keyStore.setLastBackupDate(lastBackupDate);
			}
		}

	}

	private void createZipArchive(File zipFile, List<String> filePaths) throws IOException, KeyStoreManagerException {
		ZipOutputStream zos = null;
		FileInputStream in = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zipFile));
			Iterator<String> it = filePaths.iterator();
			List<String> entriesNames = new ArrayList<String>(filePaths.size());
			Properties typeProperties = new Properties();

			int nameSuffix = 1;
			while (it.hasNext()) {
				String keyStore = it.next();
				File keyStoreFile = new File(keyStore);
				if (keyStoreFile.exists()) {
					String entryName = keyStoreFile.getName();
					while (entriesNames.contains(entryName)) {
						if (entryName.toLowerCase().endsWith(KEYSTORE_EXT)) {
							entryName = entryName.replace(KEYSTORE_EXT, "_" + nameSuffix + KEYSTORE_EXT); //$NON-NLS-1$
						} else {
							entryName = entryName.concat(Integer.toString(nameSuffix));
						}
						nameSuffix++;
					}
					putKsType(typeProperties, entryName, keyStore);
					ZipEntry zipEntry = new ZipEntry(entryName);
					zos.putNextEntry(zipEntry);
					entriesNames.add(entryName);

					byte[] buf = new byte[1024 * 4];
					int len;
					in = new FileInputStream(keyStoreFile);
					try {
						while ((len = in.read(buf)) > 0) {
							zos.write(buf, 0, len);
						}
					} finally {
						if (in != null) {
							in.close();
						}
					}
					zos.flush();
				}
			}
			zos.putNextEntry(new ZipEntry(KS_TYPES_FILENAME));
			putLastBackupDate(typeProperties);
			typeProperties.store(zos, "KeyStore types"); //$NON-NLS-1$
		} finally {
			if (zos != null) {
				try {
					zos.flush();
					zos.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close steam while creating zip archive. " + e.getMessage());
				}
			}
		}
	}

	private void putKsType(Properties properties, String entryName, String filePaths) throws KeyStoreManagerException {
		List<IKeyStore> keyStores = KeyStoreManager.getInstance().getKeyStores();
		for (IKeyStore keyStore : keyStores) {
			String keyStorePath = keyStore.getFile().getAbsolutePath();
			if (filePaths.contains(keyStorePath)) {
				String type = keyStore.getType();
				if (type != null) {
					properties.put(entryName, type);
				}
				break;
			}
		}

	}

	private void putLastBackupDate(Properties properties) {
		Long time = lastBackupDate.getTime();
		String timeString = time.toString();
		properties.put("lastBackupDate", timeString);
	}

}
