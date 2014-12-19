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
package org.eclipse.andmore.android.common.utilities.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Class that contains the localized messages to be used through the Utilities
 * package
 */
public class UtilitiesNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.common.utilities.i18n.utilitiesNLS", UtilitiesNLS.class);
	}

	public static String ERR_Gen_ErrorTitle;

	/*
	 * Exception strings area
	 */
	public static String EXC_PluginUtils_ErrorGettingTheExecutableFromExtensionPoint;

	public static String EXC_FileUtil_ErrorSettingTheFileEncoding;

	public static String EXC_FileUtil_ErrorWritingTheFile;

	public static String EXC_FileUtil_CannotOverwriteTheFile;

	public static String EXC_FileUtil_TheFileCannotBeRead;

	public static String EXC_Project_CannotCreateFolderReadOnlyWorkspace;

	/*
	 * UI strings area
	 */

	public static String UI_EclipseUtils_OpenDetails;

	public static String UI_EclipseUtils_CloseDetails;

	public static String UI_Project_Creating_Folder_Task;

	public static String UI_Project_Verifying_Folder_Task;

	public static String UI_DoNotShowMEAgain;

	public static String UI_AlwaysProceed;

	/*
	 * Manifest file
	 */
	public static String ERR_AndroidProjectManifest_AndroidManifestDoesNotExist;

	public static String EXC_CommentNode_ChildNodesCannotBeAddedToACommentNode;

	public static String ERR_AndroidManifestNodeParser_ErrorParsingPriority;

	public static String ERR_AndroidManifestNodeParser_ErrorParsingInitOrder;

	public static String WARN_AndroidManifestNode_TheNodeContainsAnInvalidAttribute;

	public static String EXC_AndroidManifestFile_ErrorCreatingTheDocumentBuilder;

	public static String EXC_AndroidManifestFile_ErrorFormattingTheXMLOutput;

	public static String EXC_AndroidManifestNodeParser_ErrorParsingTheXMLFile;

	public static String EXC_AndroidManifestNodeParser_ErrorReadingTheXMLContent;

	public static String ERR_AndroidManifestNodeParser_ErrorParsingVersionCode;

	public static String ERR_AndroidManifestFile_TheFileAndroidManifestXmlIsMalFormed;

	/*
	 * Project Utils
	 */
	public static String ProjectUtils_AddLibsProgress_ConfiguringClassPaths;

	public static String ProjectUtils_AddLibsProgress_ConfiguringProjects;

	public static String ProjectUtils_AddLibsProgress_ErrorSettingClasspaths;

	public static String ProjectUtils_AddLibsProgress_PreparingPaths;

	public static String UI_ProjectCreationSupport_Creating_Strings_Task;

	/*
	 * Add remove buttons GUI
	 */
	public static String UI_AddRemoveButtons_AddButtonLabel;

	public static String UI_AddRemoveButtons_RemoveButtonLabel;

	public static String AddInputRemoveButtons_InputButtonLabel;

	/*
	 * Android Utils
	 */
	public static String AndroidUtils_ErrorReadingDefaultPropertiesFile;

	/*
	 * UI File Chooser
	 */
	public static String UI_FileChooser_Dialog_Message;

	public static String UI_FileChooser_Dialog_Title;

	public static String UI_FileChooser_Filesystem;

	public static String UI_FileChooser_Workspace;

	/*
	 * UI General
	 */
	public static String UI_General_BrowseButtonLabel;

	public static String UI_General_ProjectLabel;

	/*
	 * Project Chooser
	 */
	public static String ProjectChooser_UI_ChooseAProject;

	public static String ProjectChooser_UI_Selection;

	/*
	 * Android Utils - error messages
	 */
	public static String AndroidUtils_ERROR_DEFAULTPROPERTIESNOTFOUND;

	public static String AndroidUtils_ERROR_GETINTENTPERMISSIONSBYREFLECTION_MESSAGE;

	public static String AndroidUtils_ERROR_GETINTENTPERMISSIONSBYREFLECTION_TITLE;

	public static String AndroidUtils_ERROR_SDK_TARGETPLATFORM_NOTFOUND;

	public static String AndroidUtils_ERROR_SDKPATHNOTFOUND;

	public static String AndroidUtils_NotPossibleToGetAPIVersionNumber_Error;

	public static String AndroidUtils_NotPossibleToReachPermissionsFile_Error;

	public static String FileUtil_File_Exists_Message;

	public static String FileUtil_File_Exists_Title;

	public static String FileUtil_Get_MD5_Algorithm_Failed;

	public static String FileUtil_MD5_Calculation_Failed;

	/*
	 * UI Utils
	 */
	public static String Passwordinput_Enterpassword_Label;

	public static String Passwordinput_Enternewpassword_Label;

	public static String Passwordinput_Reenterpassword_Label;

	public static String Passwordinput_Title;

	public static String Passwordinput_Error_Passwordnotmatch;

	public static String PasswordProvider_SaveThisPassword;

	public static String Passwordinput_Error_PasswordMinimumSize;

	public static String LoginPasswordDialogCreator_DialogTItle0;

	public static String SDKLoginPasswordDialog_LoginInformationMessage;

	public static String SDKLoginPasswordDialog_PasswordLabel;

	public static String SDKLoginPasswordDialog_UsernameLabel;

	/*
	 * Http Utils
	 */
	public static String HttpUtils_MonitorTask_ContactingSite;

	public static String HttpUtils_MonitorTask_PreparingConnection;

	public static String HttpUtils_MonitorTask_RetrievingSiteContent;

	public static String HttpUtils_MonitorTask_WaitingAuthentication;

}
