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

package org.eclipse.andmore.android.common.log;

import org.eclipse.andmore.android.logger.Logger;

public abstract class StudioLogger implements UsageDataConstants {
	private static final Logger logger = Logger.getLogger("Studio for Android");

	public static void debug(String message) {
		logger.debug(message);
	}

	public static void info(String message) {
		logger.info(message);
	}

	public static void warn(String message) {
		logger.warn(message);
	}

	public static void error(String message) {
		logger.error(message);
	}

	public static void fatal(String message) {
		logger.fatal(message);
	}

	public static void info(String logger, String message) {
		Logger.getLogger(logger).info(message);
	}

	public static void debug(Object obj, String message) {
		Logger.getLogger(obj.getClass().toString()).debug(message);
	}

	public static void info(Class<?> aClass, String message) {
		Logger.getLogger(aClass.toString()).info(message);
	}

	public static void warn(String logger, String message) {
		Logger.getLogger(logger).warn(message);
	}

	public static void warn(String logger, String message, Throwable throwable) {
		Logger.getLogger(logger).warn(message, throwable);
	}

	public static void warn(Class<?> aClass, String message) {
		Logger.getLogger(aClass.toString()).warn(message);
	}

	public static void warn(Class<?> aClass, String message, Throwable throwable) {
		Logger.getLogger(aClass.toString()).warn(message, throwable);
	}

	public static void error(String logger, String message) {
		Logger.getLogger(logger).error(message);
	}

	public static void error(Class<?> aClass, String message) {
		Logger.getLogger(aClass.toString()).error(message);
	}

	public static void error(String logger, String message, Throwable error) {
		Logger.getLogger(logger).error(message, error);
	}

	public static void error(Class<?> aClass, String message, Throwable error) {
		Logger.getLogger(aClass.toString()).error(message, error);
	}

	public static void fatal(String logger, String message) {
		Logger.getLogger(logger).fatal(message);
	}

	/**
	 * Meanwhile, this method does nothing because eclipse UDC dependency was
	 * removed. A new approach to collect usage data need to be implemented.
	 * 
	 * @param what
	 *            ID of what happened.
	 * @param kind
	 *            ID of the type of the event that happened.
	 * @param description
	 *            Short description of what happened.
	 * @param pluginID
	 *            Plugin identifier (that generated the event).
	 * @param pluginVersion
	 *            Plugin version.
	 */
	public static void collectUsageData(String what, String kind, String description, String pluginID,
			String pluginVersion) {
		// Do nothing, see the javadoc.
	}
}
