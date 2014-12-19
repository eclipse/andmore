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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent.EventType;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.dialogs.RestoreBackupDialog;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.ui.model.SigningAndKeysModelManager;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to execute the restore backup wizard.
 * */
public class RestoreBackupHandler extends AbstractHandler implements IHandler {

	Date lastBackupDate = new Date();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RestoreBackupDialog dialog = new RestoreBackupDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell());

		int diagReturn = dialog.open();
		if (diagReturn == Window.OK) {
			File archiveFile = dialog.getArchiveFile();
			File destinationFile = dialog.getDestinationDir();
			List<String> selectedKeyStores = dialog.getSelectedKeyStores();

			restoreBackup(archiveFile, destinationFile, selectedKeyStores);
		}

		return null;
	}

	private void restoreBackup(File archiveFile, File destinationFile, List<String> selectedKeyStores) {
		boolean extractionSuccess = false;
		File typePropertiesFile = new File(destinationFile, BackupHandler.KS_TYPES_FILENAME);
		try {
			extractionSuccess = FileUtil.extractZipArchive(archiveFile, destinationFile,
					Arrays.asList(new String[] { BackupHandler.KS_TYPES_FILENAME }), new NullProgressMonitor());

			extractionSuccess = FileUtil.extractZipArchive(archiveFile, destinationFile, selectedKeyStores,
					new NullProgressMonitor());
		} catch (IOException e) {
			// roll back: delete files, if they were created
			rollBackDeleteExtractedFiles(destinationFile, selectedKeyStores);
			EclipseUtils.showErrorDialog(CertificateManagerNLS.RestoreBackupHandler_Error_Restoring_Backup_Title, NLS
					.bind(CertificateManagerNLS.RestoreBackupHandler_Error_Restoring_Backup_Message, archiveFile),
					new Status(IStatus.ERROR, CertificateManagerNLS.RestoreBackupHandler_Error_Restoring_Backup_Status,
							CertificateManagerActivator.PLUGIN_ID, e));
		}

		if (extractionSuccess) {
			Properties properties = null;

			if ((typePropertiesFile != null) && !typePropertiesFile.exists()) {
				showWarningAboutNonIdentifiedKeystoreType(selectedKeyStores);
			}
			properties = loadTypeProperties(typePropertiesFile, properties);
			// recover last backup date
			getDateFromProperties(properties);

			List<String> nonIdentifiedKeystoreTypes = new ArrayList<String>();

			for (String keyStoreFileName : selectedKeyStores) {
				File keyStoreFile = new File(destinationFile, keyStoreFileName);
				String ksType = null;
				if (properties != null) {
					ksType = (String) properties.get(keyStoreFileName);
					if (ksType == null) {
						// type not found at metadata
						nonIdentifiedKeystoreTypes.add(keyStoreFileName);
					}
				}

				if (ksType == null) {
					ksType = KeyStoreManager.getInstance().getDefaultType(); // set
																				// the
																				// keystore
																				// type
																				// to
																				// be
																				// the
																				// default
																				// type
				}
				KeyStoreNode keyStoreNode = new KeyStoreNode(keyStoreFile, ksType);
				keyStoreNode.setLastBackupDate(lastBackupDate);

				boolean map = true;
				try {
					List<IKeyStore> keyStores = KeyStoreManager.getInstance().getKeyStores();
					for (IKeyStore keyStore : keyStores) {
						if (keyStore.getFile().equals(keyStoreFile)) {
							// updates keystore type if necessary
							if ((ksType != null) && (ksType.compareToIgnoreCase(keyStore.getType()) != 0)) {
								keyStore.setType(ksType);
							}
							KeyStoreModelEventManager.getInstance().fireEvent((ITreeNode) keyStore, EventType.COLLAPSE);
							map = false;
							keyStore.setLastBackupDate(lastBackupDate);
						}
					}

					if (map) {
						SigningAndKeysModelManager.getInstance().mapKeyStore(keyStoreNode);
					}
				} catch (KeyStoreManagerException e) {
					EclipseUtils.showErrorDialog(CertificateManagerNLS.RestoreBackupHandler_Error_Mapping_Title,
							CertificateManagerNLS.RestoreBackupHandler_Error_Mapping_Message, new Status(IStatus.ERROR,
									CertificateManagerNLS.RestoreBackupHandler_Error_Mapping_Status,
									CertificateManagerActivator.PLUGIN_ID, e));
					if (map) {
						// roll back operation - undo mapping if keystore was
						// mapped during backup
						SigningAndKeysModelManager.getInstance().unmapKeyStore(keyStoreNode);
					}
				}
			}

			if ((nonIdentifiedKeystoreTypes != null) && !nonIdentifiedKeystoreTypes.isEmpty()) {
				showWarningAboutNonIdentifiedKeystoreType(nonIdentifiedKeystoreTypes);
			}

		} else {
			// roll back: delete files, if they were created
			rollBackDeleteExtractedFiles(destinationFile, selectedKeyStores);
			EclipseUtils.showErrorDialog(CertificateManagerNLS.RestoreBackupHandler_Error_Restoring_Backup_Title,
					NLS.bind(CertificateManagerNLS.RestoreBackupHandler_Error_Restoring_Backup_Message, archiveFile));
		}
	}

	private void rollBackDeleteExtractedFiles(File destinationFile, List<String> selectedKeyStores) {
		// roll back: delete files, if they were created
		File ksTypeDest = new File(destinationFile, BackupHandler.KS_TYPES_FILENAME);
		if (ksTypeDest.exists()) {
			// roll back: delete Kstypes
			ksTypeDest.delete();
		}
		for (String keyStoreFileName : selectedKeyStores) {
			// roll back: delete extract files
			File keyStoreFile = new File(destinationFile, keyStoreFileName);
			if (keyStoreFile.exists()) {
				keyStoreFile.delete();
			}
		}
	}

	private void getDateFromProperties(Properties properties) {
		if (properties != null) {
			// KsTypes.csv available
			String lastString = properties.getProperty("lastBackupDate");
			Long time = new Long(lastString);
			lastBackupDate.setTime(time);
		} else {
			// KsTypes.csv not available
			StudioLogger.debug("KsTypes.csv not available to get lastBackupDate properties");
		}
	}

	/**
	 * Shows warning stating that some keystores will use default keystore type.
	 * 
	 * @param selectedKeyStores
	 */
	private void showWarningAboutNonIdentifiedKeystoreType(List<String> selectedKeyStores) {
		EclipseUtils.showWarningDialog(CertificateManagerNLS.RestoreBackupHandler_RestoreIssue_WarningTitle, NLS.bind(
				CertificateManagerNLS.RestoreBackupHandler_RestoreIssue_MissingMetadataFile_WarningDescription,
				selectedKeyStores, KeyStore.getDefaultType()));
	}

	private Properties loadTypeProperties(File typePropertiesFile, Properties properties) {
		if ((typePropertiesFile != null) && typePropertiesFile.exists()) {
			FileInputStream propInStream = null;
			properties = new Properties();
			try {
				propInStream = new FileInputStream(typePropertiesFile);
				properties.load(propInStream);

				typePropertiesFile.delete();
			} catch (FileNotFoundException e) {
				properties = null;
			} catch (IOException e) {
				properties = null;
			} finally {
				if (propInStream != null) {
					try {
						propInStream.close();
					} catch (IOException e) {
						StudioLogger.error("Could not close steam while loading type properties. " + e.getMessage());
					}
					typePropertiesFile.delete();
				}
			}

		}
		return properties;
	}
}
