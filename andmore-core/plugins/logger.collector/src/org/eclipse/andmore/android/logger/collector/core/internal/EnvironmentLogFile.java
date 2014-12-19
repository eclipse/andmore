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
package org.eclipse.andmore.android.logger.collector.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.motorola.studio.android.logger.collector.util.LoggerCollectorConstants;

/**
 * This class provides the environment log file to the log files collector
 * feature
 */
public class EnvironmentLogFile implements ILogFile {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.platform.logger.collector.core.ILogFile#getLogFilePath
	 * ()
	 */
	@Override
	public List<IPath> getLogFilePath() {
		ArrayList<IPath> logs = new ArrayList<IPath>();
		logs.add(new Path(LoggerCollectorConstants.LOG_PATH).append("environment.log"));
		return logs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.platform.logger.collector.core.ILogFile#getLogName()
	 */
	@Override
	public String getLogName() {
		return "Environment Log File";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.platform.logger.collector.core.ILogFile#
	 * getOutputSubfolderName()
	 */
	@Override
	public String getOutputSubfolderName() {
		return LoggerCollectorConstants.PLATFORM_LOG_OUTPUT_FOLDER;
	}

}
