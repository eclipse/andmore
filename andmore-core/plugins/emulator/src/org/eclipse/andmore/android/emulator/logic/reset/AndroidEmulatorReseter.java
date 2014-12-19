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
package org.eclipse.andmore.android.emulator.logic.reset;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * DESCRIPTION: This class contains the business layer of the Android Emulator
 * reset procedure
 *
 * RESPONSIBILITY: Reset any Android Emulator
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the public method to reset a Android Emulator
 */
public class AndroidEmulatorReseter {
	/**
	 * Resets a Android Emulator based at the provided folder
	 * 
	 * @param userDataFolder
	 *            The folder where a working copy of the emulator is located
	 * @param force
	 *            Perform the reset without questioning the user
	 * 
	 * @return IStatus the status of the operation
	 */

	static String SNAPSHOT_FILE_NAME = "snapshots.img";

	public static IStatus resetInstance(IAndroidLogicInstance instance) {
		IStatus resetStatus = Status.OK_STATUS;

		File userData = instance.getUserdata();
		List<File> stateData = instance.getStateData();

		if ((userData != null) || (stateData != null)) {
			List<File> allFiles = new ArrayList<File>();
			if (stateData != null) {
				allFiles.addAll(stateData);
			}
			if (userData != null) {
				allFiles.add(userData);
			}

			for (File file : allFiles) {
				if (file.exists()) {
					if (!file.delete()) {
						error("There was an error when trying to remove the emulator instance user data files");
						resetStatus = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, NLS.bind(
								EmulatorNLS.EXC_AndroidEmulatorReseter_ErrorWhilePerformingDeleteOperation, new Path(
										file.getPath()).removeLastSegments(1).toString()));
						break;
					}
				}
			}

			// When the snapshots file is missing or corrupted after the reset,
			// the snapshot operation will not work properly
			// (when start and then closing the AVD after a reset operation,
			// / the snapshots file will not be saved), that is why the error
			// message should be shown.

			if ((allFiles != null) && (allFiles.size() > 0)) {
				File snapshot = instance.getSnapshotOriginalFilePath();

				String snapshotToPath = allFiles.get(0).getParentFile() + File.separator + SNAPSHOT_FILE_NAME;

				File snapshotToFile = new File(snapshotToPath);

				if ((snapshot != null) && (snapshotToFile.exists())) {

					BufferedInputStream bis = null;
					BufferedOutputStream bos = null;

					try {
						bis = new BufferedInputStream((new FileInputStream(snapshot)));
						bos = new BufferedOutputStream(new FileOutputStream(snapshotToFile));

						int c;
						while ((c = bis.read()) >= 0) {
							bos.write(c);
						}
					} catch (Exception e) {
						error("Error while copying the original snapshot file to the avd that is being reseted:"
								+ e.getMessage());
						if (resetStatus.equals(Status.OK_STATUS)) {
							resetStatus = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, NLS.bind(
									EmulatorNLS.EXC_AndroidEmulatorReseter_ErrorWhilePerformingSnapshotCopyOperation,
									snapshot.getPath(), allFiles.get(0).getParentFile()));
						} else {

							resetStatus = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, NLS.bind(
									EmulatorNLS.EXC_AndroidEmulatorReseter_ErrorWhilePerformingDeleteSnapshotOperation,
									allFiles.get(0).getParentFile(), snapshot.getPath()));
						}
					} finally {
						try {
							if (bis != null) {
								bis.close();
							}

							if (bos != null) {
								bos.close();
							}
						} catch (Exception e) {
							error("Error while closing the snapshots file of the avd that is being reseted:"
									+ e.getMessage());
						}
					}

				}
			}

		}

		return resetStatus;
	}
}
