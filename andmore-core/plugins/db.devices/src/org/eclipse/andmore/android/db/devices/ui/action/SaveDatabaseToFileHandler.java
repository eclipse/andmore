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
package org.eclipse.andmore.android.db.devices.ui.action;

import java.io.File;
import java.io.IOException;

import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.andmore.android.db.devices.model.DeviceDbNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class SaveDatabaseToFileHandler extends AbstractHandler implements IHandler {
	private DeviceDbNode deviceDbNode;

	public SaveDatabaseToFileHandler(ITreeNode node) {
		if (node instanceof DeviceDbNode) {
			this.deviceDbNode = (DeviceDbNode) node;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (deviceDbNode != null) {
			Shell shell = Display.getCurrent().getActiveShell();
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			dialog.setFileName(deviceDbNode.getRemoteDbPath().lastSegment());
			dialog.setFilterNames(new String[] { DbDevicesNLS.SaveDatabaseToFile_DbFiles,
					DbDevicesNLS.SaveDatabaseToFile_AllFiles });
			dialog.setFilterExtensions(new String[] { "*.db", "*.*" //$NON-NLS-1$ //$NON-NLS-2$
			});

			String selectedFilePath = dialog.open(); // returns null if dialog
														// is cancelled

			if (selectedFilePath != null) {
				// dialog confirmed
				File targetFile = new File(selectedFilePath);

				IPath temporaryFilePath = deviceDbNode.getPath();
				File sourceFile = temporaryFilePath.toFile();

				try {
					FileUtil.copyFile(sourceFile, targetFile);
				} catch (IOException e) {
					throw new ExecutionException(NLS.bind(DbDevicesNLS.SaveDatabaseToFile_CopyDatabase_Error,
							deviceDbNode.getName(), targetFile), e);
				}
			}

		}
		return null;
	}

}
