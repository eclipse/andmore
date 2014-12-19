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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * This class is the Implementation of ILogFile that is responsible to collect
 * device properties and write them to files. These files will be used by the
 * Collect Logs functionality
 */
public class DevicePropertyLogger implements ILogFile {

	private final Collection<String> serialNumbers;

	private final Map<String, Map<String, String>> properties;

	public DevicePropertyLogger() {
		serialNumbers = DDMSFacade.getConnectedSerialNumbers();
		properties = new HashMap<String, Map<String, String>>(serialNumbers.size());
		for (String serialNumber : serialNumbers) {
			Map<String, String> propertiesMap = new HashMap<String, String>(140);
			String deviceName = DDMSFacade.getNameBySerialNumber(serialNumber);
			try {
				Collection<String> lines = DDMSFacade.execRemoteApp(serialNumber, "getprop", new NullProgressMonitor());
				for (String line : lines) {
					String[] split = line.split(":");
					StringBuffer buffer = new StringBuffer();
					for (int i = 1; i < split.length; i++) {
						buffer.append(split[i]);
					}

					if (!"".equals(split[0])) {
						propertiesMap.put(split[0], buffer.toString());
					}
				}
			} catch (IOException e) {
				StudioLogger.error(getClass(), "Unable to execute getprop command on device " + deviceName, e);
			}
			properties.put((deviceName), propertiesMap);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.logger.collector.core.ILogFile#getLogFilePath
	 * ()
	 */
	@Override
	public List<IPath> getLogFilePath() {
		ArrayList<IPath> logs = new ArrayList<IPath>(properties.keySet().size());
		IPath LOG_PATH = AndroidPlugin.getDefault().getStateLocation();
		for (String devicename : properties.keySet()) {
			IPath log = LOG_PATH.append(devicename + "_devProperties.log");
			writeLogFile(log, properties.get(devicename));
			logs.add(log);
		}

		return logs;
	}

	private void writeLogFile(IPath log, Map<String, String> devProperties) {
		File logFile = log.toFile();
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(logFile));

			for (String propKey : devProperties.keySet()) {
				bw.append(propKey + "=" + devProperties.get(propKey) + System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			StudioLogger.error(getClass(), "An error occurred while trying to write device Properties log file, "
					+ logFile.getAbsolutePath(), e);
		} finally {
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				StudioLogger.error("Could not close stream while writing device property log. " + e.getMessage());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.logger.collector.core.ILogFile#getLogName()
	 */
	@Override
	public String getLogName() {
		return "Device properties";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.logger.collector.core.ILogFile#
	 * getOutputSubfolderName()
	 */
	@Override
	public String getOutputSubfolderName() {
		return "Devices";
	}
}
