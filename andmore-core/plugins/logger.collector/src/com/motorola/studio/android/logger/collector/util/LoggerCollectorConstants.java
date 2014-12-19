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

import java.io.File;

import org.eclipse.core.runtime.Platform;

/**
 * Constant definitions for logger collector plug-in preferences
 */
public class LoggerCollectorConstants {

	/**
	 * The constant contains a string representation of the regular expression
	 * to validate the file name format. The file name must follow the formats
	 * above: - Only alphanumeric characters and point must be supported
	 * ([A-Za-z0-9_.]). - The max length must be 60 characters;
	 */
	public static final String FILE_NAME_REGEX = "[\\w.]{1,60}"; //$NON-NLS-1$

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.logger.collector"; //$NON-NLS-1$

	// The plugin path location
	private static final String PLUGIN_LOCATION = File.separator + ".metadata" //$NON-NLS-1$
			+ File.separator + ".plugins" + File.separator //$NON-NLS-1$
			+ "org.eclipse.andmore.android.logger"; //$NON-NLS-1$

	// The absolute logger files path
	public static final String LOG_PATH = Platform.getLocation() + PLUGIN_LOCATION;

	public static final String PLATFORM_LOG_OUTPUT_FOLDER = "platform";

	public static final String ZIP_FILE_EXTENSION = "zip";
}
