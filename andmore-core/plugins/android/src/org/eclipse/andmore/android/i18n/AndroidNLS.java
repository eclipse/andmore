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

package org.eclipse.andmore.android.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Class that contains the localized messages to be used through the Android SDK
 * support and project creation
 */
public class AndroidNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.i18n.androidNLS", AndroidNLS.class);
	}

	public static String AbstractDeviceDropSupportHandler_ApplicationsFailed;

	public static String AbstractDeviceDropSupportHandler_InstallingApksJobName;

	/*
	 * Abstract Device strings
	 */
	public static String AbstractDevicePropertyPage_CVS_Export;

	public static String AbstractDevicePropertyPage_Error_Message;

	public static String AbstractDevicePropertyPage_Error_Message_Valid_File;

	public static String AbstractDevicePropertyPage_Error_Title;

	public static String AbstractDevicePropertyPage_Property;

	public static String AbstractDevicePropertyPage_Value;

	public static String AndroidProject_MsgSDKVersionIsPreview;

	public static String MonkeyWizardPage_CountCommand;

	public static String MotodevHProfDumpHandler_saveHProfFile;

	public static String MotodevHProfDumpHandler_warnAboutHprofSavePrefMsg;

	public static String MotodevHProfDumpHandler_warnAboutHprofSavePrefTitle;

	public static String MotodevStudioPropertyPage_ChangeProguardSettingsProblem;

	public static String UninstallAppWizardPage_ColumnPackageKiind;

	public static String UninstallAppWizardPage_ColumnPackageName;

	public static String UninstallAppWizardPage_Loading_Applications;

	public static String UninstallAppWizardPage_PageTitle;

	public static String UninstallAppWizardPage_PageDescription;

	public static String UninstallAppWizardPage_SystemLabel;

	public static String UninstallAppWizardPage_UserLabel;

	public static String DDMSFacade_MsgConnectingToDeviceViaTCPIP;

	public static String DDMSFacade_MsgSwitchingDeviceFromTCPIPToUSB;

	public static String DDMSFacade_MsgSwitchingFromUSBConnection;

	public static String DDMSFacade_MsgTimeoutReachedSwitchingFromTCPToUSB;

	public static String DDMSFacade_Remote_File_Not_Found;

	public static String DumpHprofPage_PageTitle;

	public static String DumpHprofPage_PageDescription;

	public static String DumpHprofPage_ColumnAppName;

	public static String DumpHPROFWizardPage__Message_LoadingRunningApplications;

	public static String NewAndroidProjectWizard_Message_CreatingAndroidProject;

	public static String NewAndroidProjectWizard_OPhonePromptMessage;

	public static String NewAndroidProjectWizard_OPhonePromptTitle;

	public static String DumpHprofFile_GeneratingMemoryAnalysisOutput;

	public static String DumpHprofFile_CreatingTempFile;

	public static String DumpHprofFile_GettingRunningApplications;

	public static String DumpHprofFile_SettingApplicationToAnalyse;

	public static String DumpHprofFile_DumpingHprofFile;

	public static String DumpHprofFile_GettingFileFromRemoteDevice;

	public static String DumpHprofFile_OpeningMemoryAnalysisFile;

	public static String DumpHprofFile_SavingFile;

	public static String DumpHprofFile_SavingTempFile;

	/*
	 * Warning strings area
	 */
	public static String WRN_Obfuscation_ProjectLocationContainWhitespaces;

	/*
	 * Error strings
	 */

	public static String ERR_CommandError;

	public static String ERR_DDMSFacade_UninstallPackageException;

	public static String ERR_DDMSFacade_UninstallPackage;

	public static String ERR_DDMSFacade_UninstallPackageError;

	public static String ERR_DDMSFacade_SerialNumberNullPointer;

	public static String ERR_DDMSFacade_IncompatibleFileLists;

	public static String ERR_DDMSFacade_MonkeyError;

	public static String ERR_DDMSFacade_FileNotFound;

	public static String ERR_GenericTimeout;

	public static String ERR_Localization_NoProjects_Title;

	public static String ERR_Localization_NoProjects_Description;

	public static String ERR_Localization_NoFiles_Title;

	public static String ERR_Localization_NoFiles_Description;

	public static String ERR_Localization_XMLMalformed_Title;

	public static String ERR_Localization_XMLMalformed_Description;

	public static String ERR_MonkeyWizardPage_Package;

	public static String ERR_MonkeyWizardPage_CountCommand;

	public static String ERR_MonkeyWizardPage_Device;

	public static String ERR_PropertiesMainComposite_Monkey_NoQuotes;

	public static String ERR_PropertiesMainComposite_Monkey_TextBlank;

	public static String ERR_PropertiesMainComposite_Monkey_NumberRequired;

	public static String ERR_PropertiesMainComposite_Monkey_NumberMustBePositiveInteger;

	public static String ERR_PropertiesMainComposite_Monkey_NumberIntRange;

	public static String ERR_PropertiesMainComposite_Monkey_NumberMustBeInteger;

	public static String ERR_PropertiesMainComposite_Monkey_PathRequired;

	public static String ERR_PropertiesMainComposite_Monkey_PathDirNotExist;

	public static String ERR_PropertiesMainComposite_Monkey_PathMustBeDir;

	public static String ERR_PropertiesMainComposite_Monkey_PathFileNotExist;

	public static String ERR_PropertiesMainComposite_Monkey_PathMustBeFile;

	public static String ERR_PropertiesMainComposite_Monkey_PathIncorrectFileType;

	public static String ERR_RemoteDevice_TimeoutWhileConnecting;

	public static String ERR_RemoteDevice_TimeoutWhileDisconnecting;

	public static String EXC_SdkUtils_CannotCreateTheVMInstance;

	public static String EXC_NewAndroidProjectWizard_AnErrorHasOccurredWhenCreatingTheProject;

	/*
	 * UI strings
	 */
	public static String UI_GenericErrorDialogTitle;

	public static String UI_Preferences_Dialogs_Group_Title;

	public static String UI_Preferences_Dialogs_Group_Message;

	public static String UI_Preferences_Dialogs_Group_Button;

	public static String UI_Preferences_Dialogs_Clean_Message;

	public static String UI_InstallApp_InstallApp;

	public static String UI_UninstallApp_SucessDialogTitle;

	public static String UI_UninstallApp_Message;

	public static String UI_UninstallApp_ERRDialogTitle;

	public static String UI_UninstallApp_ERRUninstallApp;

	public static String UI_ChangeLang_Language;

	public static String UI_ChangeLang_Country;

	public static String UI_ChangeLang_Restart_Device_Manually;

	public static String UI_Project_Selection;

	public static String UI_General_BrowseButtonLabel;

	public static String UI_NewAndroidWidgetProjectWizard_TitleNewProjectWizard;

	public static String UI_NewAndroidWidgetProjectMainPage_TitleCreateProject;

	public static String UI_NewAndroidWidgetProjectMainPage_WizardProjectDescription;

	public static String UI_NewAndroidWidgetProjectMainPage_SubtitleCreateProject;

	public static String UI_NewAndroidWidgetProjectMainPage_LabelContents;

	public static String UI_NewAndroidProjectMainPage_SubtitleCreateProject;

	public static String UI_NewAndroidProjectMainPage_TitleCreateProject;

	public static String UI_NewAndroidProjectMainPage_WizardProjectDescription;

	public static String UI_NewAndroidProjectMainPage_LabelContents;

	public static String UI_NewAndroidProjectMainPage_LabelApplication;

	public static String UI_NewAndroidProjectMainPage_LabelTarget;

	public static String UI_NewAndroidProjectWizard_TitleNewProjectWizard;

	public static String UI_SampleSelectionPage_TitleSourcePage;

	public static String UI_SampleSelectionPage_WizardTitle;

	public static String UI_SampleSelectionPage_WizardDescription;

	public static String UI_SampleSelectionPage_SamplesTreeLabel;

	public static String UI_LocationGroup_NewProjectRadioLabel;

	public static String UI_LocationGroup_NewFromSampleRadioLabel;

	public static String UI_LocationGroup_NewFromExistentProjectRadioLabel;

	public static String UI_LocationGroup_UseDefaultLocationCheckLabel;

	public static String UI_LocationGroup_LocationLabel;

	public static String UI_LocationGroup_BrowseDialogMessage;

	public static String UI_ProjectNameGroup_ProjectNameLabel;

	public static String UI_SdkTargetSelector_SdkTargetNameColumn;

	public static String UI_SdkTargetSelector_VendorNameColumn;

	public static String UI_SdkTargetSelector_APILevelColumn;

	public static String UI_SdkTargetSelector_SDKVersionColumn;

	public static String UI_SdkTargetSelector_EmptyValue;

	public static String UI_SdkTargetSelector_NoTargetAvailable;

	public static String UI_ApplicationGroup_PackageNameLabel;

	public static String UI_ApplicationGroup_ActivityNameLabel;

	public static String UI_ApplicationGroup_ApplicationNameLabel;

	public static String UI_ApplicationGroup_MinSDKVersionLabel;

	public static String UI_DeployWizard_SelectMessage;

	public static String UI_DeployWizard_WizardDescription;

	public static String UI_DeployWizard_WizardTitle;

	public static String UI_DeployWizard_BrowseButtonText;

	public static String UI_DeployWizard_PackageText;

	public static String UI_DeployWizardPage_ReplaceApp;

	public static String UI_DeployWizardPage_NotSignedMessage;

	public static String UI_DeployWizardPage_PackageIsAFolder;

	public static String UI_DeployWizardPage_InvalidPath;

	public static String UI_DeployWizardPage_FileDoesNotExist;

	public static String UI_DeployWizardPage_UninstallApp;

	public static String UI_DeployWizardPage_DoNothingApp;

	public static String UI_MonkeyOptions_CommandLine;

	public static String UI_MonkeyWizardOptionsPage_PageMessage;

	public static String UI_MonkeyComposite_DeviceNameLabel;

	public static String UI_MonkeyComposite_SelectDeviceScreenTitle;

	public static String UI_MonkeyComposite_SelectDeviceScreenMessage;

	public static String UI_MonkeyComposite_TabMainName;

	public static String UI_MonkeyComposite_TabOtherCmdName;

	public static String UI_MonkeyError_Msg;

	public static String UI_MonkeyError_Title;

	public static String UI_Hprof_Handler_Dialog_Error_Title;

	public static String UI_Hprof_Handler_Dialog_Unable_to_create_Hprof;

	public static String UI_Hprof_Handler_Dialog_Unable_to_download_Hprof;

	public static String UI_Hprof_Handler_Dialog_Error_Check_Log_Cat;

	public static String UI_Hprof_Handler_Dialog_Unable_to_Save_Hprof_Data;

	public static String UI_Hprof_Handler_Save_Prompt;

	public static String EmulatorPreferencePage_EmulatorViewGroup;

	public static String EmulatorPreferencePage_UnembedCheckBox;

	public static String EmulatorPreferencePage_UnembedNote;

	public static String UI_CleanProjectsJob_Name;

	public static String UI_CleanProjectsJob_Description;

	/*
	 * Console
	 */
	public static String CON_ConsolePush;

	public static String CON_ConsolePull;

	/*
	 * Model strings area
	 */

	public static String SdkUtils_COULD_NOT_REPAIR_AVD;

	public static String TableWithLoadingInfo__UI_LoadingData;

	public static String UI_ProjectCreation_NativeSupport;

	public static String ERR_WirelessRemoteDevice_TimeoutWhileConnecting;

	public static String ObfuscateProjectsHandler_1;

	public static String ObfuscateProjectsHandler_2;

	public static String ObfuscateProjectsHandler_3;

	public static String UI_ProjectCreation_Obfuscate;

	public static String UI_ProjectPropertyPage_Obfuscate;

	public static String UI_ProjectPropertyPage_ObfuscateGroup;

	public static String UI_Logger_ApplicationValidatorFolder;

	/*
	 * Android Project
	 */
	public static String EXC_AndroidProject_NoSamplesAvailable;

	public static String EXC_AndroidProject_AnErrorHasOccurredWhenCreatingTheProject;

	public static String EXC_AndroidProject_InvalidMinimumSdkVersion;

	public static String ERR_AndroidProject_ProjectNameMustBeSpecified;

	public static String ERR_AndroidProject_InvalidProjectName;

	public static String ERR_AndroidProject_ProjectNameTooLong;

	public static String ERR_AndroidProject_InvalidCharsInPackageName;

	public static String ERR_AndroidProject_EmptyPackageName;

	public static String ERR_AndroidProject_ActivityNameMustBeSpecified;

	public static String ERR_AndroidProject_InvalidApplicationName;

	public static String ERR_AndroidProject_InvalidActivityName;

	public static String ERR_AndroidProject_DuplicatedProjectNameInWorkspace;

	public static String ERR_AndroidProject_FileNotFoundError;

	public static String ERR_AndroidProject_EmptySourceLocation;

	public static String ERR_AndroidProject_LocationContainsWhitespaces;

	public static String ERR_AndroidProject_ASdkTargetMustBeSpecified;

	public static String ERR_AndroidProject_InvalidSdkVersion;

	public static String ERR_AndroidProject_InvalidApiLevel;

	public static String ERR_AndroidProject_InvalidPackageName;

	public static String WARN_AndroidProject_ApplicationNameIsEmpty;

	/*
	 * Project Creation
	 */
	public static String EXC_ProjectCreationSupport_CannotCreateProjectReadOnlyWorkspace;

	public static String EXC_ProjectCreationSupport_CannotCreateFolderReadOnlyWorkspace;

	public static String EXC_ProjectCreationSupport_CannotCreateProjectReadOnlyDestination;

	public static String UI_ProjectCreationSupport_CopyingSamplesMonitorTaskTitle;

	public static String UI_ProjectCreationSupport_CopyingSamplesMonitorMessage;

	public static String UI_ProjectCreationSupport_Creating_Directory_Task;

	public static String UI_ProjectCreationSupport_Creating_Manifest_File_Task;

	public static String UI_ProjectCreationSupport_NonEmptyFolder;

	public static String UI_ProjectCreationSupport_Preparing_Java_Packages_Task;

	public static String UI_ProjectCreationSupport_Preparing_Source_Folders_Task;

	public static String UI_ProjectCreationSupport_Preparing_Template_File_Task;

	public static String UI_ProjectCreationSupport_Reading_Template_File_Task;

	public static String UI_ProjectCreationSupport_Verifying_Directory_Task;

	public static String UI_ProjectCreationSupport_Configuring_Sample_Source_Task;

	public static String UI_ProjectCreationSupport_Configuring_Project_Icon_Task;

	public static String UI_ProjectCreationSupport_Configuring_Sample_Activity_Task;

	public static String UI_ProjectCreationSupport_Configuring_Sample_Widget_Provider;

	public static String ERR_ProjectCreationSupport_CaseVariantExistsError;

	public static String GEN_ProjectCreationSupport_HelloWorldSimple;

	public static String GEN_ProjectCreationSupport_HelloWorldWithName;

	public static String GEN_Error;
}
