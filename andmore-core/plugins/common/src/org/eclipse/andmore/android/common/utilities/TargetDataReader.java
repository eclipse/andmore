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
package org.eclipse.andmore.android.common.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Knows how to get activity, receiver and/or service actions information from a
 * target
 */
public class TargetDataReader {
	// path inside <Android_SDK>\platforms\<Target>\data\activity_actions.txt
	private final File activityActionsFile;

	// path inside <Android_SDK>\platforms\<Target>\data\broadcast_actions.txt
	private final File broadCastActionsFile;

	// path inside <Android_SDK>\platforms\<Target>\data\service_actions.txt
	private final File serviceActionsFile;

	// path inside <Android_SDK>\platforms\<Target>\data\categories.txt
	private final File categoriesFile;

	/**
	 * @param androidTarget
	 *            path to <android_sdk_root>/platforms/<target_name>
	 */
	public TargetDataReader(File androidTarget) {
		File dataFolder = new File(androidTarget, "data"); //$NON-NLS-1$
		this.activityActionsFile = new File(dataFolder, "activity_actions.txt"); //$NON-NLS-1$
		this.broadCastActionsFile = new File(dataFolder, "broadcast_actions.txt"); //$NON-NLS-1$
		this.serviceActionsFile = new File(dataFolder, "service_actions.txt"); //$NON-NLS-1$
		this.categoriesFile = new File(dataFolder, "categories.txt"); //$NON-NLS-1$
	}

	private List<String> readItems(File file) throws IOException {
		List<String> items = new ArrayList<String>();
		BufferedReader bufferedReader = null;
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			bufferedReader = new BufferedReader(reader);

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				items.add(line.trim());
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}
		return items;
	}

	/**
	 * Reads activity actions file and creates the list of activity actions
	 * 
	 * @return list of activityActions available
	 * @throws IOException
	 *             if file not found, or if there is any problem reading the
	 *             activity_actions file
	 */
	public List<String> getActivityActions() throws IOException {
		return readItems(activityActionsFile);
	}

	/**
	 * Reads service actions file and creates the list of service actions
	 * 
	 * @return list of serviceActions available
	 * @throws IOException
	 *             if file not found, or if there is any problem reading the
	 *             service_actions file
	 */
	public List<String> getServiceActions() throws IOException {
		return readItems(serviceActionsFile);
	}

	/**
	 * Reads broadcast receiver actions file and creates the list of broadcast
	 * receiver actions
	 * 
	 * @return list of broadcastReceiverActions available
	 * @throws IOException
	 *             if file not found, or if there is any problem reading the
	 *             broadcast_actions file
	 */
	public List<String> getReceiverActions() throws IOException {
		return readItems(broadCastActionsFile);
	}

	/**
	 * Reads categories file and creates the list of categories available in the
	 * target
	 * 
	 * @return list of Intent Filters Categories available
	 * @throws IOException
	 *             if file not found, or if there is any problem reading the
	 *             broadcast_actions file
	 */
	public List<String> getIntentFilterCategories() throws IOException {
		return readItems(categoriesFile);
	}

}
