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

package org.eclipse.andmore.android.db.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

/**
 * Handles database deployment in devices and plug-in projects.
 */
public class DatabaseDeployer {

	/**
	 * Plug-in Identifier
	 */
	private static final String PLUGIN_IDENTIFIER = "org.eclipse.andmore.android.codeutils";

	/**
	 * Constant representing the location of the DatabaseHelper class.
	 */
	private static final String DATABASE_HELPER_CLASS_LOCATION = "resources/databaseDeploy/DatabaseHelperjava.txt"; //$NON-NLS-1$

	/**
	 * This constant represents the value of the to-be-copied Database Name
	 */
	public static final String DATABASE_NAME = "#dbName#";

	/**
	 * This constant represents the value of newly-created Android Application
	 * Package Name for the Database
	 */
	public static final String APPLICATION_DATABASE_NAMESPACE = "#applicationPackageNamespace#";

	/**
	 * Directory of the Database Helper in the android project
	 */
	public static final String ANDROID_PROJECT_PACKAGE_NAME = "#packageName#"; //$NON-NLS-1$

	/**
	 * Open Helper Package Name
	 */
	public static final String OPEN_HELPER_PACKAGE_NAME = "#databaseOpenHelperPackageName#"; //$NON-NLS-1$

	/**
	 * File name of the Database Helper in the android project
	 */
	public static final String DATABASE_HELPER_CLASS_NAME = "#className#"; //$NON-NLS-1$

	/**
	 * Copy the DatabaseHelper deploy file to the newly-created-android-plu-in.
	 * 
	 * @param project
	 *            Newly-created-android Project
	 * @param parameters
	 *            Parameters to replace in the DatabaseHelper class file
	 * @param monitor
	 *            Monitor
	 * 
	 * @throws IOException
	 *             Exception thrown in case there are problems accessing data of
	 *             a file.
	 * @throws CoreException
	 *             Exception thrown in case there are problems accessing a file
	 *             itself.
	 */
	public static void copyDataBaseDeployerClassToProject(IProject project, Map<String, String> parameters,
			IProgressMonitor monitor) throws IOException, CoreException {
		// get the destination folder
		IFolder destinationFolder = project.getFolder(IAndroidConstants.WS_ROOT + IAndroidConstants.FD_SOURCES);
		// split the new sequence of folders - they will be created, one by one
		String[] folders = parameters.get(ANDROID_PROJECT_PACKAGE_NAME).split(IAndroidConstants.RE_DOT);
		// iterate through the folders and create them
		if ((folders != null) && (folders.length > 0)) {
			// iterate
			for (String folder : folders) {
				// get destination folder with the next one to be created
				destinationFolder = destinationFolder.getFolder(folder);

				// proceed in case the destination folder does not exists and
				// can be created
				if (!destinationFolder.exists()) {
					// create the folder
					destinationFolder.create(true, true, monitor);
				}
			}
		}

		// get the destination file
		IFile destinationFile = destinationFolder.getFile(parameters.get(DATABASE_HELPER_CLASS_NAME)
				+ IAndroidConstants.DOT_JAVA);
		// proceed in case it does not exist and can be created
		if (!destinationFile.exists() && FileUtil.canWrite(destinationFile)) {
			// get the Database Helper class as a text
			String databaseHelperText = readDatabaseHelperClassFile(project, parameters);

			// transform it in a stream
			InputStream stream = null;

			try {
				stream = new ByteArrayInputStream(databaseHelperText.getBytes("UTF-8")); //$NON-NLS-1$
				// create the destination folder
				destinationFile.create(stream, true, monitor);

				DatabaseUtils.formatCode(destinationFile, databaseHelperText, monitor);
			} finally {
				if (stream != null) {
					stream.close();
				}
			}
		}
	}

	/**
	 * Read the DatabaseHelper class file, replace the parameter maps and return
	 * it as a String.
	 * 
	 * @param project
	 *            Project where the DatabaseHelper is retrieved.
	 * @param parameters
	 *            Parameters for replacement in the DatabaseHelper class file.
	 * 
	 * @return a String representing the DatabaseHelper class file.
	 * 
	 * @throws IOException
	 *             Exception thrown when there are problems reading elements
	 *             from the DatabaseHelper class file.
	 */
	private static String readDatabaseHelperClassFile(IProject project, Map<String, String> parameters)
			throws IOException {
		URL url = Platform.getBundle(PLUGIN_IDENTIFIER).getEntry(DATABASE_HELPER_CLASS_LOCATION);
		url = FileLocator.toFileURL(url);

		// string buffer which holds the file as a text
		StringBuffer databaseHelperBuffer = new StringBuffer("");
		// reader
		BufferedReader databaseHelperReader = null;

		// get the file to be read
		File f = null;
		try {
			f = new File(url.getFile());
			// create the reader to manipulate the file
			databaseHelperReader = new BufferedReader(new FileReader(f));

			// read the Database Helper class file in steps (using buffer)
			String buffer = null;

			// iterate while there is stuff to read
			while ((buffer = databaseHelperReader.readLine()) != null) {
				// read and put the content in the string buffer element
				databaseHelperBuffer.append(buffer);
				databaseHelperBuffer.append(System.getProperty("line.separator"));
			}

		} finally {
			// close reader
			if (databaseHelperReader != null) {
				databaseHelperReader.close();
			}
		}

		// string holding the "text" of the DatabaseHelper class
		String databaseHelperText = "";
		// proceed in case there is stuff in the buffer to read
		if (databaseHelperBuffer != null) {
			databaseHelperText = databaseHelperBuffer.toString();

			// iterate through the parameters and replace the parameters in the
			// map
			if (parameters != null) {
				for (String key : parameters.keySet()) {
					databaseHelperText = databaseHelperText.replaceAll(key, parameters.get(key));
				}
			}
		}
		// return the file as a text
		return databaseHelperText;
	}
}
