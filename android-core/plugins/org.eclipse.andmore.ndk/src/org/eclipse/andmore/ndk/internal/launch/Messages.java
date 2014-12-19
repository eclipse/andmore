/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.ndk.internal.launch;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.andmore.ndk.internal.launch.messages"; //$NON-NLS-1$
	public static String NdkGdbLaunchDelegate_LaunchError_gdbserverOutput;
	public static String NdkGdbLaunchDelegate_Action_ActivityLaunch;
	public static String NdkGdbLaunchDelegate_Action_CheckAndroidDeviceVersion;
	public static String NdkGdbLaunchDelegate_Action_KillExistingGdbServer;
	public static String NdkGdbLaunchDelegate_Action_LaunchHostGdb;
	public static String NdkGdbLaunchDelegate_Action_LaunchingGdbServer;
	public static String NdkGdbLaunchDelegate_Action_ObtainAppAbis;
	public static String NdkGdbLaunchDelegate_Action_ObtainDevice;
	public static String NdkGdbLaunchDelegate_Action_ObtainDeviceABI;
	public static String NdkGdbLaunchDelegate_Action_PerformIncrementalBuild;
	public static String NdkGdbLaunchDelegate_Action_SettingUpPortForward;
	public static String NdkGdbLaunchDelegate_Action_SyncAppToDevice;
	public static String NdkGdbLaunchDelegate_Action_WaitGdbServerAttach;
	public static String NdkGdbLaunchDelegate_Action_WaitingForActivity;
	public static String NdkGdbLaunchDelegate_LaunchError_ActivityLaunchError;
	public static String NdkGdbLaunchDelegate_LaunchError_Api8Needed;
	public static String NdkGdbLaunchDelegate_LaunchError_CouldNotGetProject;
	public static String NdkGdbLaunchDelegate_LaunchError_gdbserverLaunchException;
	public static String NdkGdbLaunchDelegate_LaunchError_InstallError;
	public static String NdkGdbLaunchDelegate_LaunchError_InterruptedWaitingForGdbserver;
	public static String NdkGdbLaunchDelegate_LaunchError_NoActivityInManifest;
	public static String NdkGdbLaunchDelegate_LaunchError_NoCompatibleAbi;
	public static String NdkGdbLaunchDelegate_LaunchError_NoLauncherActivity;
	public static String NdkGdbLaunchDelegate_LaunchError_NoSuchActivity;
	public static String NdkGdbLaunchDelegate_LaunchError_NullApk;
	public static String NdkGdbLaunchDelegate_LaunchError_ObtainingAppFolder;
	public static String NdkGdbLaunchDelegate_LaunchError_PortForwarding;
	public static String NdkGdbLaunchDelegate_LaunchError_ProjectHasErrors;
	public static String NdkGdbLaunchDelegate_LaunchError_PullFileError;
	public static String NdkGdbLaunchDelegate_LaunchError_UnableToDetectAppAbi;
	public static String NdkGdbLaunchDelegate_LaunchError_UnknownAndroidDeviceVersion;
	public static String NdkGdbLaunchDelegate_LaunchError_VerifyIfDebugBuild;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
