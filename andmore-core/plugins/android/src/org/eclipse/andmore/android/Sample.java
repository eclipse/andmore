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
package org.eclipse.andmore.android;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.andmore.android.common.IAndroidConstants;

import com.android.sdklib.IAndroidTarget;

/**
 * Bean that represents a sample application
 */
public class Sample {
	private final File folder;

	private final String name;

	private final IAndroidTarget target;

	public Sample(File sampleFolder, IAndroidTarget parentTarget) {
		folder = sampleFolder;
		name = sampleFolder.getName();
		target = parentTarget;
	}

	/**
	 * Retrieves the sample application folder
	 * 
	 * @return the sample application folder
	 */
	public File getFolder() {
		return folder;
	}

	/**
	 * Retrieves the sample application name
	 * 
	 * @return the sample application name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the sample application target SDK
	 * 
	 * @return the sample application target SDK
	 */
	public IAndroidTarget getTarget() {
		return target;
	}

	/**
	 * Checks if a folder is a sample folder
	 * 
	 * @param sampleFolder
	 *            The folder to be tested
	 * @return true if a folder is a sample folder or false otherwise
	 */
	public static boolean isSample(File sampleFolder) {
		boolean result = false;

		// check if the folder contains a manifest file
		FilenameFilter androidManifest = new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.equals(IAndroidConstants.FN_ANDROID_MANIFEST);
			}
		};

		if ((sampleFolder != null) && (sampleFolder.isDirectory())) {
			if (sampleFolder.list(androidManifest).length > 0) {
				result = true;
			}
		}
		return result;
	}
}
