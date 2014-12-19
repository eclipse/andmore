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

package org.eclipse.andmore.android;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.ddms.preferences.PreferenceInitializer;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData.IHprofDumpHandler;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncService;
import com.android.ddmuilib.SyncProgressMonitor;
import com.android.ddmuilib.handler.BaseFileHandler;

/**
 * 
 * Class to handle post HPROF dumping things. Based on the existing HprofHandler
 * from ADT.
 */
public class MotodevHProfDumpHandler extends BaseFileHandler implements IHprofDumpHandler {
	public final static String SAVE_ACTION = "hprof.save"; //$NON-NLS-1$

	public final static String OPEN_ACTION = "hprof.open"; //$NON-NLS-1$

	public final static String HPROF_FILE_EXTENSION = ".hprof"; //$NON-NLS-1$

	private final static String FORMATTED_ERROR_STRING = " '%1$s'.\n\n%2$s"; //$NON-NLS-1$

	private final static String FORMATTED_ERROR_STRING_2 = " '%1$s'."; //$NON-NLS-1$

	private final static String DATE_FORMAT = "yyyy-MM-dd HH-mm-ss"; //$NON-NLS-1$

	private String selectedApp;

	private final IProgressMonitor progressMonitor;

	/**
	 * @return the selectedApp
	 */
	public String getSelectedApp() {
		return selectedApp;
	}

	/**
	 * @param selectedApp
	 *            the selectedApp to set
	 */
	public void setSelectedApp(String selectedApp) {
		this.selectedApp = selectedApp;
	}

	public MotodevHProfDumpHandler(Shell parentShell, IProgressMonitor monitor) {
		super(parentShell);
		this.progressMonitor = monitor;
	}

	@Override
	protected String getDialogTitle() {
		return AndroidNLS.UI_Hprof_Handler_Dialog_Error_Title;
	}

	@Override
	public void onEndFailure(final Client client, final String message) {
		mParentShell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				displayErrorFromUiThread(AndroidNLS.UI_Hprof_Handler_Dialog_Unable_to_create_Hprof
						+ FORMATTED_ERROR_STRING + AndroidNLS.UI_Hprof_Handler_Dialog_Error_Check_Log_Cat, client
						.getClientData().getClientDescription(), message != null ? message + "\n\n" : ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		synchronized (DDMSFacade.class) {
			DDMSFacade.class.notify();
		}
	}

	@Override
	public void onSuccess(final String remoteFilePath, final Client client) {
		mParentShell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				extractRemoteHprof(remoteFilePath, client);
			}
		});
		synchronized (DDMSFacade.class) {
			DDMSFacade.class.notify();
		}
	}

	private void extractRemoteHprof(final String remoteFilePath, final Client client) {
		progressMonitor.beginTask(AndroidNLS.DumpHprofFile_GeneratingMemoryAnalysisOutput, 100);
		final IDevice targetDevice = client.getDevice();
		try {
			// get the sync service to pull the HPROF file
			final SyncService syncService = client.getDevice().getSyncService();
			if (syncService != null) {
				// get from the preference what action to take
				IPreferenceStore preferenceStore = DdmsPlugin.getDefault().getPreferenceStore();
				String actionValue = preferenceStore.getString(PreferenceInitializer.ATTR_HPROF_ACTION);

				if (SAVE_ACTION.equals(actionValue)) {
					warnAboutSaveHprofPreference();
				}

				actionValue = preferenceStore.getString(PreferenceInitializer.ATTR_HPROF_ACTION);

				if (OPEN_ACTION.equals(actionValue)) {
					progressMonitor.setTaskName(AndroidNLS.DumpHprofFile_CreatingTempFile);
					File hprofTempFile = File.createTempFile(selectedApp, HPROF_FILE_EXTENSION);
					progressMonitor.worked(25);

					String tempHprofFilePath = hprofTempFile.getAbsolutePath();

					progressMonitor.setTaskName(AndroidNLS.DumpHprofFile_GettingFileFromRemoteDevice);
					progressMonitor.worked(50);

					syncService.pullFile(remoteFilePath, tempHprofFilePath,
							new SyncProgressMonitor(progressMonitor, "")); //$NON-NLS-1$

					openHprofFileInEditor(tempHprofFilePath);

				} else {
					progressMonitor.setTaskName(AndroidNLS.DumpHprofFile_SavingFile);
					try {
						promptAndPull(syncService,
								client.getClientData().getClientDescription() + HPROF_FILE_EXTENSION, remoteFilePath,
								AndroidNLS.MotodevHProfDumpHandler_saveHProfFile);
					} catch (Exception e) {
						displayErrorFromUiThread(AndroidNLS.UI_Hprof_Handler_Dialog_Unable_to_download_Hprof
								+ FORMATTED_ERROR_STRING, targetDevice.getSerialNumber(), e.getLocalizedMessage());
					}
					progressMonitor.worked(100);
				}

			} else {
				displayErrorFromUiThread(AndroidNLS.UI_Hprof_Handler_Dialog_Unable_to_download_Hprof
						+ FORMATTED_ERROR_STRING_2, targetDevice.getSerialNumber());
			}
		} catch (Exception e) {
			displayErrorFromUiThread(AndroidNLS.UI_Hprof_Handler_Dialog_Unable_to_download_Hprof
					+ FORMATTED_ERROR_STRING_2, targetDevice.getSerialNumber());

		} finally {
			progressMonitor.done();
		}
	}

	@Override
	public void onSuccess(final byte[] data, final Client client) {
		mParentShell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				extractLocalHprof(data, client, progressMonitor);
			}
		});
		synchronized (DDMSFacade.class) {
			DDMSFacade.class.notify();
		}
	}

	private void extractLocalHprof(final byte[] data, final Client client, IProgressMonitor monitor) {
		monitor.beginTask(AndroidNLS.DumpHprofFile_GeneratingMemoryAnalysisOutput, 100);
		IPreferenceStore preferenceStore = DdmsPlugin.getDefault().getPreferenceStore();
		String value = preferenceStore.getString(PreferenceInitializer.ATTR_HPROF_ACTION);

		if (SAVE_ACTION.equals(value)) {
			warnAboutSaveHprofPreference();
		}

		value = preferenceStore.getString(PreferenceInitializer.ATTR_HPROF_ACTION);

		if (OPEN_ACTION.equals(value)) {
			try {
				monitor.setTaskName(AndroidNLS.DumpHprofFile_SavingTempFile);
				File tempHprofFile = saveTempFile(data, HPROF_FILE_EXTENSION);
				monitor.worked(50);
				monitor.setTaskName(AndroidNLS.DumpHprofFile_OpeningMemoryAnalysisFile);
				openHprofFileInEditor(tempHprofFile.getAbsolutePath());
				monitor.worked(50);
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				displayErrorFromUiThread(AndroidNLS.UI_Hprof_Handler_Dialog_Unable_to_Save_Hprof_Data
						+ FORMATTED_ERROR_STRING_2, errorMsg != null ? ":\n" + errorMsg //$NON-NLS-1$
				: "."); //$NON-NLS-1$
			}
		} else {
			monitor.setTaskName(AndroidNLS.DumpHprofFile_SavingFile);
			promptAndSave(client.getClientData().getClientDescription() + HPROF_FILE_EXTENSION, data,
					AndroidNLS.UI_Hprof_Handler_Save_Prompt);
			monitor.worked(100);
		}
		monitor.done();
	}

	private void warnAboutSaveHprofPreference() {
		Display.getCurrent().syncExec(new Runnable() {

			@Override
			public void run() {
				boolean openPrefPage = DialogWithToggleUtils.showQuestion(AndroidPlugin.WARN_ABOUT_HPROF_PREFERENCE,
						AndroidNLS.MotodevHProfDumpHandler_warnAboutHprofSavePrefTitle,
						AndroidNLS.MotodevHProfDumpHandler_warnAboutHprofSavePrefMsg);
				if (openPrefPage) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					EclipseUtils.openPreference(ww.getShell(), "org.eclipe.andmore.ddms.preferences.PreferencePage"); //$NON-NLS-1$
				}
			}
		});
	}

	/**
	 * Opens the HProf file into MAT editor
	 * 
	 * @param path
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws PartInitException
	 */
	private void openHprofFileInEditor(String path) throws IOException, InterruptedException, PartInitException {
		// make a file to convert the hprof into something
		// readable by normal tools
		String hprofPath = getHProfLocalFileName(path);

		String[] commands = new String[3];
		commands[0] = DdmsPlugin.getHprofConverter();
		commands[1] = path;
		commands[2] = hprofPath;

		Process p = Runtime.getRuntime().exec(commands);
		p.waitFor();

		IFileStore fileSystemStore = EFS.getLocalFileSystem().getStore(new Path(hprofPath));
		if (!fileSystemStore.fetchInfo().isDirectory() && fileSystemStore.fetchInfo().exists()) {
			IWorkbenchPage workbenchPage = AndroidPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
			IEditorPart editorPart = IDE.openEditorOnFileStore(workbenchPage, fileSystemStore);
			// Store information about the opened file and the selected app
			AndroidPlugin.getDefault().getPreferenceStore()
					.setValue(editorPart.getEditorInput().getName(), selectedApp);

		}
	}

	/**
	 * Gets local (desktop) file name based on selected app and the current date
	 * 
	 * @param path
	 * @return
	 */
	private String getHProfLocalFileName(String path) {
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		File ddmsPath = new File(path);

		File hprofFileHandler = new File(ddmsPath.getParent(), selectedApp + " " + dateFormat.format(date) //$NON-NLS-1$
				+ HPROF_FILE_EXTENSION);
		String hprofPath = hprofFileHandler.getAbsolutePath();
		return hprofPath;
	}
}
