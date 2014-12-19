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
package org.eclipse.andmore.android.installer.i18n;

import org.eclipse.osgi.util.NLS;

public class InstallerNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.installer.i18n.installerNLS", //$NON-NLS-1$
				InstallerNLS.class);
	}

	public static String AbstractConfigurationPage_LoadingRepositoriesTask;

	public static String AcceptLicensesDialog_AcceptLicenseButton;

	public static String AcceptLicensesDialog_Description;

	public static String AcceptLicensesDialog_IUDescriptionLabel;

	public static String AcceptLicensesDialog_RejectLicenseButton;

	public static String AcceptLicensesDialog_Title;

	public static String ConfigurationDialog_DialogTitle;

	public static String P2Utilities_LoadingUnits;

	public static String P2Utilities_Preparing;

	public static String P2Utilities_PreparingEnvironment;

	public static String P2Utilities_ErrorDuringUpdate;

	public static String P2Utilities_ErrorWhileLaunchingP2Job;

	public static String P2Utilities_AuthenticationFailed;

	public static String UpdateStudio_AlreadyUpdatedInformationDialogText;

	public static String UpdateStudio_AlreadyUpdatedInformationDialogTitle;

	public static String UpdateStudio_CheckingForUpdatesJobDescription;

	public static String UpdateStudio_UpdateErrorText;

	public static String UpdateStudio_UpdateErrorTitle;

	public static String UpdateStudio_UpdatingStudioJobDescription;

	public static String UpdateStudio_MSG_RESTART_TITLE;

	public static String UpdateStudio_MSG_RESTART_MESSAGE;

	public static String UpdateStudio_UpdateAlreadyRunningTitle;

	public static String UpdateStudio_UpdateAlreadyRunningMsg;

	public static String UpdateStudio_LoadingRepositories;

	public static String UpdateStudioJob_UpdateErrorMessage;

	public static String InstallManager_Could_Not_Find_Proper_Backend;

	public static String P2Installer_Could_Not_Find_Proper_Backend;

	public static String P2Installer_Could_Not_Install_Selected_Items;

	public static String P2Installer_Loading_Repositories;

	public static String MotodevPolicy_Insufficient_Permissions_Message;

	public static String MotodevPolicy_Insufficient_Permissions_Title;
}
