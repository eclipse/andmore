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
package org.eclipse.andmore.android.emulator.device;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.sequoyah.device.common.utilities.logger.LoggerConstants;

/**
 * DESCRIPTION: This class implements the TmL logger interface to redirect all
 * logs from TmL to the log system used by the emulator
 *
 * RESPONSIBILITY: Delegate the logging requests from TmL to the same logger
 * used by the emulator
 *
 * COLABORATORS: None.
 *
 * USAGE: An instance of this class is constructed during the emulator log
 * startup. This class is not supposed to be constructed by clients
 */
public class SequoyahLogRedirector implements org.eclipse.sequoyah.vnc.utilities.logger.ILogger,
		org.eclipse.sequoyah.device.common.utilities.logger.ILogger {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#debug(java.lang.Object)
	 */
	@Override
	public void debug(Object message) {
		if (message instanceof String) {
			StudioLogger.debug((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#error(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(Object message, Object throwable) {
		if (message instanceof String) {
			StudioLogger.error((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#error(java.lang.Object)
	 */
	@Override
	public void error(Object message) {
		if (message instanceof String) {
			StudioLogger.error((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#fatal(java.lang.Object)
	 */
	@Override
	public void fatal(Object message) {
		if (message instanceof String) {
			StudioLogger.fatal((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#info(java.lang.Object)
	 */
	@Override
	public void info(Object message) {
		if (message instanceof String) {
			StudioLogger.info((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#log(java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Object priority, Object message, Object throwable) {
		log(priority, message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#log(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Object priority, Object message) {
		String priorityStr = (String) priority;
		if (message instanceof String) {
			if (priorityStr.equals(LoggerConstants.FATAL)) {
				StudioLogger.fatal((String) message);
			} else if (priorityStr.equals(LoggerConstants.ERROR)) {
				StudioLogger.error((String) message);
			} else if (priorityStr.equals(LoggerConstants.WARNING)) {
				StudioLogger.warn((String) message);
			} else if (priorityStr.equals(LoggerConstants.INFO)) {
				StudioLogger.info((String) message);
			} else if (priorityStr.equals(LoggerConstants.DEBUG)) {
				StudioLogger.debug((String) message);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#warn(java.lang.Object)
	 */
	@Override
	public void warn(Object message) {
		if (message instanceof String) {
			StudioLogger.warn((String) message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.vnc.utilities.logger.ILogger#getCurrentLevel()
	 */
	@Override
	public Object getCurrentLevel() {
		return LoggerConstants.TXT_ALL;
	}

	// ************************************************
	// FROM THIS POINT, NO METHODS WILL BE IMPLEMENTED
	// ************************************************

	/*
     * 
     */
	@Override
	public void configureLogger(Object arg0) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#log(java.lang.Object)
	 */
	@Override
	public void log(Object arg0) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#setLevel(java.lang.
	 * Object)
	 */
	@Override
	public void setLevel(Object arg0) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.vnc.utilities.logger.ILogger#setLogToConsole()
	 */
	@Override
	public void setLogToConsole() {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#setLogToFile(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public void setLogToFile(String arg0, String arg1) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#setLogToFile(java.lang
	 * .String)
	 */
	@Override
	public void setLogToFile(String arg0) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.vnc.utilities.logger.ILogger#setLogToHTMLFile(java
	 * .lang.String)
	 */
	@Override
	public void setLogToHTMLFile(String arg0) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.common.utilities.logger.ILogger#setLogToDefault
	 * ()
	 */
	@Override
	public void setLogToDefault() {
		// nothing to do here
	}

}
