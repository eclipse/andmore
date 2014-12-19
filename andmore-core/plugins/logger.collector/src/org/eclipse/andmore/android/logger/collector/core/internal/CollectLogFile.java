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
package org.eclipse.andmore.android.logger.collector.core.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.TableItem;

import com.motorola.studio.android.logger.collector.util.LogCollectorExtensionLoader;
import com.motorola.studio.android.logger.collector.util.LoggerCollectorConstants;
import com.motorola.studio.android.logger.collector.util.LoggerCollectorMessages;
import com.motorola.studio.android.logger.collector.util.PlatformException;
import com.motorola.studio.android.logger.collector.util.ZipUtil;

/**
 * This class is responsible to manage all collecting log files requirements.
 */
public class CollectLogFile {

	private final ArrayList<ILogFile> logs = LogCollectorExtensionLoader.getLogFiles();

	/**
	 * This method is responsible to retrieve files from informed path.
	 * 
	 * @param path
	 *            The path to be retrieved.
	 * @return The log files
	 */
	public ArrayList<ILogFile> getLogFileList() {
		return logs;
	}

	/**
	 * This method is responsible to compact all log files selected by end-user.
	 * 
	 * @param fileName
	 *            The output compacted file name
	 * @param checkedItems
	 *            The selected items
	 * @return if the selected files are compacted successfully
	 * @throws PlatformException
	 */
	public boolean zipLogFiles(String fileName, List<TableItem> checkedItems) throws PlatformException {
		boolean toReturn = true;
		String nomalizedDirectory = FileUtil.normalizePath(new Path(fileName).removeLastSegments(1).toOSString());
		// Temporary folder path to store the log files while they are not
		// compressed.
		final String tempFolderPath = FileUtil.normalizePath(nomalizedDirectory) + "temp" //$NON-NLS-1$ 
				+ Double.toString((Math.random())).replaceAll("\\.", "0") + //$NON-NLS-1$ //$NON-NLS-2$
				File.separator;
		final IPath tempFolder = new Path(tempFolderPath);

		try {
			// Creating the temporary folder.
			FileUtil.mkdir(tempFolderPath);
			// Copying the log files.
			for (TableItem item : checkedItems) {
				ILogFile logFile = (ILogFile) item.getData();
				IPath outputFolder = tempFolder.append(logFile.getOutputSubfolderName());
				FileUtil.mkdir(outputFolder.toOSString());
				for (IPath path : logFile.getLogFilePath()) {
					FileUtil.copy(new File(path.toOSString()), new File(outputFolder.append(path.lastSegment())
							.toOSString()));
				}

			}
			// Compact all log files from temporary folder
			new ZipUtil(fileName, tempFolderPath).zip();
		} catch (IOException e) {
			toReturn = false;
			throw new PlatformException(new Status(IStatus.CANCEL, LoggerCollectorConstants.PLUGIN_ID,
					LoggerCollectorMessages.getInstance().getString("error.logger.collector.zip"))); //$NON-NLS-1$
		} finally {
			// Deleting temporary folder an all files within it.
			FileUtil.delete(tempFolderPath);
		}
		return toReturn;
	}

}
