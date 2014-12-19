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
package org.eclipse.andmore.android.db.devices.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class DeviceDbUtils {
	/**
     * 
     */
	private static final String DB_EXTENSION = "db"; //$NON-NLS-1$
	private static final String APPNAME_KEY = "#APP_NAME"; //$NON-NLS-1$
	private static final String remoteDbPathMask = "/data/data/" + APPNAME_KEY + "/databases/"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Retrieves the databases path for a given application on the android file
	 * system
	 */
	public static IPath getRemoteDbFolder(String appName) {
		String remoteDbFolder = remoteDbPathMask.replace(APPNAME_KEY, appName);
		return new Path(remoteDbFolder);
	}

	/**
	 * Retrieves the database file path for a given application's db file on the
	 * android file system.
	 * 
	 * @param appName
	 *            the application name
	 * @param dbFileName
	 *            the db file name
	 * @return the full db file path on a android file system
	 */
	public static IPath getRemoteDbPath(String appName, String dbFileName) {
		IPath remoteDbFolder = getRemoteDbFolder(appName);
		IPath remoteDbPath = remoteDbFolder.append(dbFileName);
		String fileExtension = remoteDbPath.getFileExtension();
		if (fileExtension == null || !fileExtension.equals(DB_EXTENSION)) {
			remoteDbPath = remoteDbPath.addFileExtension(DB_EXTENSION);
		}
		return remoteDbPath;
	}

	/**
	 * List the installed packages in the device with the serial number Each
	 * package entry carries their package location
	 * 
	 * @param serialNumber
	 * @return an Object array that contains: Item 0: a map<String, String> with
	 *         the app package as a key and the app path as value. Item 1:
	 *         number of applications not included on map due to
	 *         filterDbApplications being true
	 * @throws IOException
	 */
	public static Object[] listInstalledPackages(String serialNumber, boolean filterDBbApplications) throws IOException {
		Object[] returnArray = new Object[2];

		Map<String, String> allPackages = DDMSUtils.listInstalledPackages(serialNumber);

		if (filterDBbApplications) {
			Map<String, String> filteredPackages = new LinkedHashMap<String, String>();

			Collection<String> appDataDirs = DDMSFacade.execRemoteApp(serialNumber,
					"ls /data/data/", new NullProgressMonitor()); //$NON-NLS-1$

			for (String appPackage : appDataDirs) {
				IPath remoteDbFolder = getRemoteDbFolder(appPackage);
				Collection<String> databases = DDMSFacade.execRemoteApp(serialNumber,
						"ls " + remoteDbFolder.toString(), new NullProgressMonitor()); //$NON-NLS-1$

				for (String commandOutline : databases) {
					String[] strings = commandOutline.split(" "); //$NON-NLS-1$
					for (String string : strings) {
						if (string.trim().endsWith("." + DB_EXTENSION)) //$NON-NLS-1$
						{
							filteredPackages.put(appPackage, allPackages.get(appPackage));
						}
					}
				}
			}
			returnArray[0] = filteredPackages;
			returnArray[1] = allPackages.size() - filteredPackages.size();
		} else {
			returnArray[0] = allPackages;
			returnArray[1] = 0;
		}

		return returnArray;
	}

}
