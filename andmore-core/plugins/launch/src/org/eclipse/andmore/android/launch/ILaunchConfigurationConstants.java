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

package org.eclipse.andmore.android.launch;

import org.eclipse.andmore.internal.launch.LaunchConfigDelegate;
import org.eclipse.andmore.internal.launch.AndroidLaunchConfiguration.TargetMode;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * This interface holds the constants for Launch Configuration
 */
@SuppressWarnings("restriction")
public interface ILaunchConfigurationConstants {

	/**
	 * Launch configuration id
	 */
	public final static String LAUNCH_CONFIGURATION_TYPE_EXTENSION_ID = "androidLaunchConfigurationType";

	public final static String MOTODEV_APP_ICO = "icons/motodevapp.gif";

	public final static String DEFAULT_VALUE = "";

	public final static boolean DEFAULT_BOOL_VALUE = false;

	/**
	 * Launch Configuration attribute ID: Project Name
	 */
	public final static String ATTR_PROJECT_NAME = IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME;

	/**
	 * Launch Configuration attribute ID : Terminate Boolean If true, the VM
	 * supports terminate action.
	 */
	public final static String ATTR_ALLOW_TERMINATE = IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE;

	public final static boolean ATTR_ALLOW_TERMINATE_DEFAULT = true;

	/**
	 * Launch Configuration attribute ID : Launch Action. The type of launch to
	 * be performed. 0: launch default activity. 1: launch specified activity.
	 * 2: Do Nothing
	 * 
	 * Should always be 'activity' for now.
	 */
	public final static String ATTR_LAUNCH_ACTION = LaunchConfigDelegate.ATTR_LAUNCH_ACTION;

	public final static int ATTR_LAUNCH_ACTION_DEFAULT = LaunchConfigDelegate.ACTION_DEFAULT;

	public final static int ATTR_LAUNCH_ACTION_DO_NOTHING = LaunchConfigDelegate.ACTION_DO_NOTHING;

	public final static int ATTR_LAUNCH_ACTION_ACTIVITY = LaunchConfigDelegate.ACTION_ACTIVITY;

	/**
	 * Launch Configuration attribute ID: Activity Name
	 */
	public final static String ATTR_ACTIVITY = LaunchConfigDelegate.ATTR_ACTIVITY;

	/**
	 * Launch Configuration attribute ID: Target Mode True: Automatic False:
	 * Manual
	 */
	public final static String ATTR_TARGET_MODE = LaunchConfigDelegate.ATTR_TARGET_MODE;

	public final static TargetMode ATTR_TARGET_MODE_DEFAULT = LaunchConfigDelegate.DEFAULT_TARGET_MODE;

	/**
	 * This is the attribute we use to store the name of the device. We could
	 * use ADT's directly if we were not forced to remove ADT's entry for it to
	 * work with handsets. If we don't store in our own key, the device name not
	 * to be restored the next time the user opens the Run As window, which is
	 * against Eclipse standards.
	 */
	public final static String ATTR_DEVICE_INSTANCE_NAME = "org.eclipse.andmore.android.launch.instanceName";

	/**
	 * Launch Configuration attribute ID: Instance Name (VM Name for ADT)
	 */
	public final static String ATTR_ADT_DEVICE_INSTANCE_NAME = LaunchConfigDelegate.ATTR_AVD_NAME;

	/**
	 * Launch Configuration attribute ID: Emulator Network Speed
	 * 
	 * Default value is 0.
	 */
	public final static String ATTR_SPEED = LaunchConfigDelegate.ATTR_SPEED;

	public final static int ATTR_SPEED_DEFAULT = LaunchConfigDelegate.DEFAULT_SPEED;

	/**
	 * Launch Configuration attribute ID: Emulator Network Latency
	 */
	public final static String ATTR_DELAY = LaunchConfigDelegate.ATTR_DELAY;

	public final static int ATTR_DELAY_DEFAULT = LaunchConfigDelegate.DEFAULT_DELAY;

	/**
	 * Launch Configuration attribute ID: Wipe Data
	 * 
	 * Default value is FALSE.
	 * 
	 */
	public final static String ATTR_WIPE_DATA = LaunchConfigDelegate.ATTR_WIPE_DATA;

	public final static boolean ATTR_WIPE_DATA_DEFAULT = LaunchConfigDelegate.DEFAULT_WIPE_DATA;

	/**
	 * Launch Configuration attribute ID: Boot Animation
	 * 
	 * Default value is FALSE.
	 */
	public final static String ATTR_NO_BOOT_ANIM = LaunchConfigDelegate.ATTR_NO_BOOT_ANIM;

	public final static boolean ATTR_NO_BOOT_ANIM_DEFAULT = LaunchConfigDelegate.DEFAULT_NO_BOOT_ANIM;

	/**
	 * Launch Configuration attribute ID: Command Line
	 * 
	 * Additional command line options. Default value is empty.
	 */
	public final static String ATTR_COMMANDLINE = LaunchConfigDelegate.ATTR_COMMANDLINE;

	/*
	 * Console View ID
	 */
	public final static String ANDROID_CONSOLE_ID = "Android";
}
