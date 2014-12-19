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

/**
 * Level defines standard logging levels.</p> The standard levels are <b> DEBUG
 * < INFO < WARN < ERROR < FATAL </b>.
 * <p>
 * <b>Note:</b><br>
 * A log operation of level x in a logger with level y, is enabled if and only
 * if x >= y.
 * <p>
 * <p>
 * <b>Example:</b><br>
 * If the level is set to <b>ERROR</b> only messages with level of <b>ERROR</b>
 * and <b>FATAL</b> will be logged.
 */
public final class Level {

	// Constants ---------------------------------------
	/**
	 * Disables all logging levels from being logged. After setting Level to
	 * OFF, no messages will be recorded in log file.
	 */
	public static final int OFF = Integer.MAX_VALUE;

	/**
	 * The FATAL level is used for severe error events. In case of FATAL, the
	 * application could be aborted.
	 */
	public static final int FATAL = org.apache.log4j.Level.FATAL_INT;

	/**
	 * The ERROR level is used by errors events. Less severe than FATAL, used
	 * for situations of error that will not crash the application.
	 */
	public static final int ERROR = org.apache.log4j.Level.ERROR_INT;

	/**
	 * The WARN level is used for potentially harmful situations. Used for
	 * situations that can generate an error.
	 */
	public static final int WARN = org.apache.log4j.Level.WARN_INT;

	/**
	 * The INFO level is used for informational messages. Informational messages
	 * are used to notify the progress of the application or relevant messages
	 * to be analyzed, like the tracing of the application execution.
	 */
	public static final int INFO = org.apache.log4j.Level.INFO_INT;

	/**
	 * The DEBUG level is used for relevant informations on an application, like
	 * variable values.
	 */
	public static final int DEBUG = org.apache.log4j.Level.DEBUG_INT;

	/**
	 * Enables all logging levels. After setting Level to ALL, all the messages
	 * will be recorded in log file.
	 */
	public static final int ALL = Integer.MIN_VALUE;
}
