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
package org.eclipse.andmore.android.db.devices.i18n;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class DbDevicesNLS extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.andmore.android.db.devices.i18n.messages"; //$NON-NLS-1$

	public static String ApplicationNode_ConnectedDbs_Refresh_Message;

	public static String ApplicationNode_Could_Not_Create_Database;

	public static String ExternalStorageNode_ConnectedDbs_Refresh_Message;

	public static String DeviceDbNode_Application_Running_Msg_Text;

	public static String DeviceDbNode_Application_Running_Msg_Title;

	public static String DeviceDbNode_Calculate_Local_Md5_Failed;

	public static String DeviceDbNode_Could_Not_Create_DeviceDbNode;

	public static String DeviceDbNode_Create_Device_Db_Failed;

	public static String DeviceDbNode_Create_Temp_Local_Db_Failed;

	public static String DeviceDbNode_DBOutOfSync_Refresh_Message;

	public static String DeviceDbNode_Delete_Remote_File_Failed;

	public static String DeviceDbNode_Md5Sum_Differs;

	public static String DeviceDbNode_Push_Local_File_To_Device_Failed;

	public static String DeviceDbNode_RefreshQuestion;

	public static String DeviceDbNode_Remote_File_Modified_Msg;

	public static String DeviceDbNode_Remote_File_Modified_Title;

	public static String DeviceDbNode_Tootip_Prefix;

	public static String DeviceDbNode_User_Canceled_Overwrite;

	public static String DeviceNode_Cant_Refresh_Node;

	public static String DeviceNode_CouldNotLoadInstalledApps;

	public static String DeviceNode_CouldNotVerifySdCard;

	public static String DeviceNode_X_Apps_Filtered;

	public static String DevicesRootNode_Cant_Refresh_Node;

	public static String UI_MapDatabaseAction_QueryDbPath_DialogTitle;

	public static String UI_MapDatabaseAction_QueryDbPath_DialogMessage;

	public static String MapDatabaseAction_Error_WrongDatabasePlace;

	public static String UI_PreferencePage_PathLabel;

	public static String ERR_DbPrefPage_InvalidDir;

	public static String ERR_DbUtils_Local_Db_Title;

	public static String ERR_DbUtils_Local_Db_Msg;

	public static String ExtStorageNode_Disconnect_Failed;

	public static String ExtStorageNode_Node_Name;

	public static String ExtStorageNode_RemoteFile_Not_Exist;

	public static String SaveDatabaseToFile_AllFiles;

	public static String SaveDatabaseToFile_CopyDatabase_Error;

	public static String SaveDatabaseToFile_DbFiles;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DbDevicesNLS.class);
	}

	private DbDevicesNLS() {
	}
}
