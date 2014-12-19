/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.andmore.ndk.internal.messages"; //$NON-NLS-1$

	public static String AddNativeWizardPage_Description;

	public static String AddNativeWizardPage_LibraryName;

	public static String AddNativeWizardPage_Location_not_valid;

	public static String AddNativeWizardPage_Title;

	public static String NDKPreferencePage_Location;

	public static String NDKPreferencePage_not_a_valid_directory;

	public static String NDKPreferencePage_not_a_valid_NDK_directory;

	public static String NDKPreferencePage_Preferences;

	public static String SetFolders_Missing_project_name;

	public static String SetFolders_No_folders;

	public static String SetFolders_Project_does_not_exist;

	public static String SimpleFile_Bad_file_operation;

	public static String SimpleFile_Bundle_not_found;

	public static String SimpleFile_Could_not_fine_source;

	public static String SimpleFile_No_project_name;

	public static String SimpleFile_Project_does_not_exist;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
