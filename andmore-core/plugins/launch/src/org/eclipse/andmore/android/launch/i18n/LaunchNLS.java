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

package org.eclipse.andmore.android.launch.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * DESCRIPTION: This class is the NLS component for Launch plugin. It is the
 * main class for internationalization
 * 
 * RESPONSIBILITY: Provide local strings for using throughout the tool
 * 
 * COLABORATORS: messages.properties: file that contains the strings that will
 * be provided to the plugin
 * 
 * USAGE: Use any of the public static variables for accessing the local strings
 */
public class LaunchNLS extends NLS {
	/**
	 * The bundle location. It refers to messages.properties file inside this
	 * package
	 */

	static {
		NLS.initializeMessages("org.eclipse.andmore.android.launch.i18n.launchNLS", LaunchNLS.class);
	}

	/*
	 * UI string area
	 */

	public static String LaunchComposite_UI_LaunchComposite_DestinationGroupText;

	public static String LaunchConfigurationTab_CreateNewAVDLink;

	public static String LaunchConfigurationTab_DoNothingButton;

	public static String LaunchConfigurationTab_LaunchButton;

	public static String UI_LaunchComposite_ProjectNameLabel;

	public static String UI_LaunchComposite_ActivityDefaultButton;

	public static String UI_LaunchComposite_ActivityGroupLabel;

	public static String UI_LaunchComposite_DeviceNameLabel;

	public static String UI_LaunchComposite_BrowseButton;

	public static String UI_LaunchComposite_ProjectRequiredMessage;

	public static String UI_LaunchComposite_ProjectRequiredTitle;

	public static String UI_LaunchComposite_SelectProjectScreenTitle;

	public static String UI_LaunchComposite_SelectProjectScreenMessage;

	public static String UI_LaunchComposite_SelectActivityScreenTitle;

	public static String UI_LaunchComposite_SelectActivityScreenMessage;

	public static String UI_LaunchComposite_SelectDeviceScreenTitle;

	public static String UI_LaunchComposite_SelectDeviceScreenMessage;

	public static String UI_LaunchConfigurationTab_ERR_DEVICE_INEXISTENT;

	public static String UI_LaunchConfigurationTab_ERR_DEVICE_INCOMPATIBLE;

	public static String UI_LaunchConfigurationTab_ERR_INVALID_ACTIVITY;

	public static String UI_LaunchConfigurationTab_ERR_ACTIVITY_NOT_EXIST;

	public static String UI_LaunchConfigurationTab_ERR_PROJECT_NOT_EXIST;

	public static String UI_LaunchConfigurationTab_WARN_DEVICE_INCOMPATIBLE;

	public static String UI_LaunchConfigurationTab_Tab_Name;

	public static String UI_LaunchConfigurationTab_InfoSelectInstance;

	public static String UI_LaunchConfigurationTab_InfoSelectActivity;

	public static String UI_LaunchConfigurationTab_InfoSelectProject;

	public static String ERR_LaunchConfigurationShortcut_MsgTitle;

	public static String ERR_LaunchConfigurationShortcut_CannotLaunchSelectedResourceMsg;

	public static String ERR_LaunchDelegate_InvalidDeviceInstance;

	public static String ERR_LaunchDelegate_No_Compatible_Device;

	public static String UI_LaunchConfigurationTab_ERR_EMULATOR_INCOMPATIBLE;

	public static String UI_LaunchConfigurationTab_WARN_DEVICE_TARGET_MISSING;

	public static String UI_LaunchConfigurationTab_ERR_PROJECT_IS_LIBRARY;

	public static String UI_StartedInstancesDialog_CompatibleAvdsColumnName;

	public static String UI_StartedInstancesDialog_Message;

	public static String UI_StartedInstancesDialog_Title;

	public static String UI_StartedInstancesDialog_WindowTitle;

	public static String UI_StartedInstancesDialog_ApiLevel;

	public static String UI_StartedInstancesDialog_Tooltip;

	public static String UI_StartedInstancesDialog_UpdateRunConfigurarion;
}
