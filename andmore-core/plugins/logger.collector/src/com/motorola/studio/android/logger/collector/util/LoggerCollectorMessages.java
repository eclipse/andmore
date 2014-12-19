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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class to handle the messages. By default, all keys has the class name in its
 * prefix, plus a sequential number.
 */
public class LoggerCollectorMessages {

	/**
	 * The shared instance.
	 */
	private static LoggerCollectorMessages instance = null;

	/**
	 * Bundle to get messages from module (properties file).
	 */
	private final ResourceBundle bundle;

	/**
	 * Class used to get the message.
	 */
	private Class<? extends Object> clazz;

	/**
	 * Default constructor.
	 */
	private LoggerCollectorMessages() {
		this.bundle = ResourceBundle.getBundle("loggerCollector"); //$NON-NLS-1$
	}

	/**
	 * Returns the single instance.
	 * 
	 * @param clazz
	 *            Class used to get the messages.
	 * @return The singleton instance.
	 */
	public static synchronized LoggerCollectorMessages getInstance(Class<? extends Object> clazz) {
		if (instance == null) {
			instance = new LoggerCollectorMessages();
		}
		instance.clazz = clazz;
		return instance;
	}

	/**
	 * Returns the single instance.
	 * 
	 * @return The singleton instance.
	 */
	public static synchronized LoggerCollectorMessages getInstance() {
		if (instance == null) {
			instance = new LoggerCollectorMessages();
		}
		instance.clazz = null;
		return instance;
	}

	/**
	 * Returns the message of the given key.
	 * 
	 * @param key
	 *            The message key
	 * @return The message of the given key.
	 */
	public String getString(String key) {
		try {
			return bundle.getString(((this.clazz == null) ? "" : (this.clazz.getName() + ".")) + key); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MissingResourceException e) {
			return "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Returns the message of the given key.
	 * 
	 * @param key
	 *            The message key
	 * @param arguments
	 *            Arguments to replace the pattern in the corresponding message
	 *            of the key.
	 * @return The message of the given key.
	 */
	public String getString(String key, Object... arguments) {
		try {
			String message = bundle.getString(((this.clazz == null) ? "" : (this.clazz.getName() + ".")) + key); //$NON-NLS-1$ //$NON-NLS-2$
			return MessageFormat.format(message, arguments);
		} catch (MissingResourceException e) {
			return "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
