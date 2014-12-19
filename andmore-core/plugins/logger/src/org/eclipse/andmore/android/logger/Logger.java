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
package org.eclipse.andmore.android.logger;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.eclipse.andmore.android.logger.internal.EclipseEnvironmentManager;
import org.eclipse.andmore.android.logger.internal.EnvironmentManager;

/**
 * Logger provides a logging facility to the <b>MOTODEV Studio</b> based
 * applications such as Studios and RCP applications. </p> <b>Note:</b> The logs
 * will be saved on the plug-in state area for this plug-in.
 */
public class Logger {

	// Class Constants --------------------------------------------------------

	/**
	 * This static block loads the log4j configuration file and resolves the
	 * path to save the logger log file.
	 */
	static {
		/* Logging Environment Information */
		EnvironmentManager envManager = new EclipseEnvironmentManager();
		envManager.logEnvironment();
	}

	// Private Variables
	// --------------------------------------------------------

	/*
	 * This Map pools the Logger instances.
	 */
	private static HashMap<String, Logger> pool;

	/*
	 * Log4j logger instance being wrapped.
	 */
	private final org.apache.log4j.Logger logger;

	/**
	 * Builds a logger associated to the specified name.
	 * 
	 * @param name
	 *            The name of the logger.
	 */
	private Logger(String name) {
		logger = org.apache.log4j.Logger.getLogger(name);
	}

	/**
	 * Gets a logger named according to the value of the name parameter. If the
	 * named logger already exists, then the existing instance will be returned.
	 * Otherwise, a new instance is created.
	 * 
	 * @param name
	 *            The name of the logger.
	 * @return the logger associated to the specified name.
	 */
	public static synchronized Logger getLogger(String name) {
		Logger logger = null;
		if (pool == null) {
			pool = new HashMap<String, Logger>();
		}

		if (pool.containsKey(name)) {
			logger = pool.get(name);
		} else {
			logger = new Logger(name);
			pool.put(name, logger);
		}
		return logger;
	}

	/**
	 * Logs the specified message with the specified level.
	 * 
	 * @param level
	 *            The log level.
	 * @param message
	 *            The message to log.
	 */
	public void log(int level, String message) {
		boolean key = true;
		if ((level == org.eclipse.andmore.android.logger.Level.OFF)
				|| (level == org.eclipse.andmore.android.logger.Level.ALL)) {
			key = false;
		}

		if (key) {
			logger.log(Level.toLevel(level), message);
		}
	}

	/**
	 * Logs the specified message and an exception stack trace.
	 * 
	 * @param level
	 *            The log level.
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void log(int level, String message, Throwable exception) {
		boolean key = true;
		if ((level == org.eclipse.andmore.android.logger.Level.OFF)
				|| (level == org.eclipse.andmore.android.logger.Level.ALL)) {
			key = false;
		}
		if (key) {
			logger.log(Level.toLevel(level), message, exception);
		}
	}

	/**
	 * Log a message object with the DEBUG level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void debug(String message) {
		debug(message, null);
	}

	/**
	 * Log a message object with the DEBUG level.
	 * 
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void debug(String message, Throwable exception) {
		if (null == exception) {
			logger.debug(message);
		} else {
			logger.debug(message, exception);
		}
	}

	/**
	 * Log a message object with the INFO level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void info(String message) {
		info(message, null);
	}

	/**
	 * Log a message object with the INFO level.
	 * 
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void info(String message, Throwable exception) {
		if (null == exception) {
			logger.info(message);
		} else {
			logger.info(message, exception);
		}
	}

	/**
	 * Log a message object with the WARN level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void warn(String message) {
		warn(message, null);
	}

	/**
	 * Log a message object with the WARN level.
	 * 
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void warn(String message, Throwable exception) {
		if (null == exception) {
			logger.warn(message);
		} else {
			logger.warn(message, exception);
		}
	}

	/**
	 * Log a message object with the ERROR level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void error(String message) {
		error(message, null);
	}

	/**
	 * Log a message object with the ERROR level.
	 * 
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void error(String message, Throwable exception) {
		if (null == exception) {
			logger.error(message);
		} else {
			logger.error(message, exception);
		}
	}

	/**
	 * Log a message object with the FATAL level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void fatal(String message) {
		fatal(message, null);
	}

	/**
	 * Log a message object with the FATAL level.
	 * 
	 * @param message
	 *            The message to log.
	 * @param exception
	 *            Exception whose stack will be logged.
	 */
	public void fatal(String message, Throwable exception) {
		if (null == exception) {
			logger.fatal(message);
		} else {
			logger.fatal(message, exception);
		}
	}

	/**
	 * Sets the Logger level to the specified value.
	 * 
	 * @param level
	 *            One of the com.motorola.studio.platform.logger.Level
	 *            constants.
	 */
	public void setLevel(int level) {
		logger.setLevel(Level.toLevel(level));
	}
}
