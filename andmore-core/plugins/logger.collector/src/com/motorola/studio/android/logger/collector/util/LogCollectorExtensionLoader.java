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
package com.motorola.studio.android.logger.collector.util;

import java.util.ArrayList;

import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * This class is responsible to load the log collector contributor extension
 * point and read all needed information
 */
public class LogCollectorExtensionLoader {
	private static final String LOGGER_EXTENSION_POINT_ID = "org.eclipse.andmore.android.logger.collector.log";

	private static final String LOG_FILE_ELEMENT = "logContribution";

	private static final String LOG_FILE_ATTRIBUTE = "logFileImpl";

	public static ArrayList<ILogFile> getLogFiles() {
		ArrayList<ILogFile> logs = new ArrayList<ILogFile>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(LOGGER_EXTENSION_POINT_ID);
		if (point != null) {
			IExtension[] extensions = point.getExtensions();

			for (IExtension ext : extensions) {
				for (IConfigurationElement element : ext.getConfigurationElements()) {
					if (element.getName().equals(LOG_FILE_ELEMENT)) {
						try {
							Object o = element.createExecutableExtension(LOG_FILE_ATTRIBUTE);
							if (o instanceof ILogFile) {
								logs.add((ILogFile) o);
							}
						} catch (CoreException e) {
							// do nothing
						}
					}
				}
			}

		}

		return logs;
	}
}
