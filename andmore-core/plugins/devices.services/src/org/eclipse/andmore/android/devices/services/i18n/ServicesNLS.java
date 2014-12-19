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
package org.eclipse.andmore.android.devices.services.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * NLS class for the plugin com.motorola.studio.android.device.service.console
 */
public class ServicesNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.devices.services.i18n.servicesNLS", ServicesNLS.class);
	}

	public static String ADBShellHandler_WaitingDeviceToLoad;

	/*
	 * General Strings area
	 */
	public static String GEN_Warning;

	/*
	 * Error Strings area
	 */
	public static String ERR_ADBShellHandler_CouldNotExecuteTheAdbShell;

	public static String ERR_ADBShellHandler_MissingAdbShell;

	public static String ERR_ADBShellHandler_AndroidSdkIsNotConfigured;

	public static String ERR_EmulatorConsoleHandler_CouldNotOpenTheConsoleShell;

	public static String ERR_EmulatorConsoleHandler_CouldNotRetrieveTheEmulatorPort;

	/*
	 * Warning Strings area
	 */
	public static String WARN_EmulatorConsoleHandler_CouldNotCloseTheConsoleConnection;

	/*
	 * Deploy service area
	 */
	public static String JOB_Name_Install_Application;

	public static String JOB_Name_Uninstall_Application;

	/*
	 * Languade service area
	 */

	public static String UI_Language;

	public static String UI_Country;

	public static String UI_Wizard_Title;

	public static String UI_Wizard_Page_Locale_Title;

	public static String UI_Wizard_Page_Locale_Description;

	/*
	 * Monkey service area
	 */
	public static String JOB_Name_Monkey;
}
