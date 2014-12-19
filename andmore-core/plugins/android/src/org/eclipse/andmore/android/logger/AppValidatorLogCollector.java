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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Class to collect log from application validator
 */
public class AppValidatorLogCollector implements ILogFile {

	@Override
	public String getLogName() {
		return AndroidNLS.UI_Logger_ApplicationValidatorFolder;
	}

	@Override
	public List<IPath> getLogFilePath() {
		ArrayList<IPath> logs = new ArrayList<IPath>();
		String userHomeProp = System.getProperty("user.home");
		File userHomeFile = new File(userHomeProp, "appValidator.log");
		if (userHomeFile.exists()) {
			IPath path = new Path(userHomeFile.getAbsolutePath());
			logs.add(path);
		}
		return logs;
	}

	@Override
	public String getOutputSubfolderName() {
		return "ApplicationValidator";
	}

}
