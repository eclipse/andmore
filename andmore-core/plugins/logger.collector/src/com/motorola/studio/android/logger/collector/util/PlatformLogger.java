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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.andmore.android.logger.Logger;

/**
 * Class with useful methods to log errors.
 */
public class PlatformLogger {

	/**
	 * Logger for this class.
	 */
	private final Logger logger;

	/**
	 * Shared instances by Class
	 */
	private static Map<Class<? extends Object>, PlatformLogger> instances = new HashMap<Class<? extends Object>, PlatformLogger>();

	/**
	 * Default constructor
	 * 
	 * @param clazz
	 *            Class to be logged.
	 */
	private PlatformLogger(Class<? extends Object> clazz) {
		this.logger = Logger.getLogger(clazz.getName());
	}

	/**
	 * Returns an instance by a given class object.
	 * 
	 * @param clazz
	 *            The given class.
	 * @return an instance by a given class object.
	 */
	public static synchronized PlatformLogger getInstance(Class<? extends Object> clazz) {
		PlatformLogger instance = instances.get(clazz);
		if (instance == null) {
			instance = new PlatformLogger(clazz);
			instances.put(clazz, instance);
		}
		return instance;
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
	 * Log a message object with the ERROR level.
	 * 
	 * @param message
	 *            The message to log.
	 */
	public void error(String message) {
		this.logger.error(message);
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
		this.logger.error(message, exception);
	}

}
