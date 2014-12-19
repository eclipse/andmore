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

package org.eclipse.andmore.android.logger.collector.core;

import java.util.List;

import org.eclipse.core.runtime.IPath;

/**
 * This interface is used by any features that wants to have a log file
 * contribution within collect log files feature
 */
public interface ILogFile {
	/**
	 * Get the name of
	 * 
	 * @return the name that will be displayed by the user
	 */
	public String getLogName();

	/**
	 * Get the full path of the log file
	 * 
	 * @return
	 */
	public List<IPath> getLogFilePath();

	/**
	 * Tell to the log collector where the log should be placed within log
	 * package
	 * 
	 * @return the sub dir to put all declared log files
	 */
	public String getOutputSubfolderName();

}
