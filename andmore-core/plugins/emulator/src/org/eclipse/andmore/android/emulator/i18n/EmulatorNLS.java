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
package org.eclipse.andmore.android.emulator.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * DESCRIPTION: This class is the NLS component for Emulator Core plugin. It is
 * the main class for internationalization
 *
 * RESPONSIBILITY: Provide local strings for using throughout the tool
 *
 * COLABORATORS: coremessages.properties: file that contains the strings that
 * will be provided to the plugin
 *
 * USAGE: Use any of the public static variables for accessing the local strings
 */
public class EmulatorNLS extends NLS {

	/**
	 * The bundle location. It refers to messages.properties file inside this
	 * package
	 */

	static {
		NLS.initializeMessages("org.eclipse.andmore.android.emulator.i18n.emulatorNLS", EmulatorNLS.class);
	}

	/*
	 * Generic string area
	 */

	public static String GEN_Error;

	public static String GEN_Warning;

	public static String GEN_Question;

	/*
	 * Exception string area
	 */

	public static String EXC_SkinFramework_CreateIAndroidSkin;

	/*
	 * Warning string area
	 */

	public static String WARN_SkinFramework_SkinNotInstalled;

	public static String WARN_SkinFramework_InvalidInstalledSkinsNotLoaded;

	/*
	 * Error string area
	 */

	public static String ERR_SrcDestComposite_InvalidFillingBase;

	public static String ERR_SrcDestComposite_InvalidFillingPhoneNumber;

	public static String ERR_SrcDestComposite_InvalidFillingEmulator;

	/*
	 * Information string area
	 */

	/*
	 * Question string area
	 */

	/*
	 * UI string area
	 */

	public static String UI_SrcDestComposite_OriginatingRunningEmulatorLabel;

	public static String UI_SrcDestComposite_DestinationRunningEmulatorLabel;

	public static String UI_SrcDestComposite_OriginatingPhoneNumberLabel;

	public static String UI_SrcDestComposite_DestinationPhoneNumberLabel;

	/*
	 * Exception string area
	 */

	public static String EXC_AndroidEmulatorStarter_TimeoutWhileRunningProtocol;

	public static String EXC_AndroidEmulatorStarter_EmulatorStartCanceled;

	public static String EXC_AndroidEmulatorReseter_ErrorWhilePerformingDeleteOperation;

	public static String EXC_AndroidEmulatorReseter_ErrorWhilePerformingSnapshotCopyOperation;

	public static String EXC_AndroidEmulatorReseter_ErrorWhilePerformingDeleteSnapshotOperation;

	public static String EXC_AndroidEmulatorStarter_ProcessTerminated;

	public static String EXC_TimeoutWhileStarting;

	public static String EXC_VncServerNotRunning;

	public static String EXC_CouldNotStartProtocol;

	public static String EXC_AndroidExceptionHandler_CannotRunStopService;

	public static String EXC_AndroidLogicUtils_CannotStartProcess;

	public static String EXC_AndroidLogicUtils_DeviceIsOffline;

	/*
	 * Error string area
	 */

	public static String ERR_AndroidEmulatorStarter_InstanceNullPointer;

	public static String ERR_AndroidEmulatorStarter_NoLogicAvailableForStart;

	public static String ERR_AndroidLogicPlugin_EmulatorStopped;

	public static String ERR_AndroidLogicPlugin_InvalidTimeoutValue;

	public static String ERR_TransferFilesLogic_NotEnoughInformation;

	/*
	 * Question string area
	 */

	public static String QUESTION_AndroidEmulatorReseter_ConfirmationText;

	public static String QUESTION_AndroidEmulatorStopper_StopEmulatorQuestion;

	public static String QUESTION_AndroidExceptionHandler_ImpossibleToReconnect;

	public static String QUESTION_AndroidEmulatorReseter_Yes;

	public static String QUESTION_AndroidEmulatorReseter_No;

	/*
	 * Information string area
	 */

	public static String INFO_ConnectVncLogic_UserCancelledVncServerStart;

	/*
	 * Progress monitor string area
	 */

	public static String MON_AndroidEmulatorStarter_ConnectingToEmulator;

	public static String MON_AndroidEmulatorStopper_DisposingInstance;

	public static String MON_AndroidEmulatorStopper_StopVm;

	public static String MON_AndroidEmulatorStarter_Canceling;

	public static String DPISCALECALCULATOR_Error_MonitorDpi;

	public static String DPISCALECALCULATOR_Error_MonitorSize;

	public static String DPISCALECALCULATOR_Error_ScreenSize;

	public static String DPISCALECALCULATOR_MonitorDpi_Label;

	public static String DPISCALECALCULATOR_MonitorDpiSize_Label;

	public static String DPISCALECALCULATOR_MonitorDpivalue_Label;

	public static String DPISCALECALCULATOR_Regex_TwoDigits;

	public static String DPISCALECALCULATOR_ResultGroup_Title;

	public static String DPISCALECALCULATOR_ResultMonitorDpi_Label;

	public static String DPISCALECALCULATOR_ResultScale_Label;

	public static String DPISCALECALCULATOR_ScreenSize_Label;

	public static String DPISCALECALCULATOR_Title;

	/*
	 * Error string area
	 */
	public static String ERR_PropertiesMainComposite_MissingTimeoutValue;

	public static String ERR_PropertiesMainComposite_TimeoutValueIsNotPositiveInteger;

	public static String ERR_PropertiesMainComposite_MissingSDCardPath;

	public static String ERR_PropertiesMainComposite_MissingSDCardSize;

	public static String ERR_PropertiesMainComposite_SDCardPathIsNotValid;

	public static String ERR_PropertiesMainComposite_SDCardSizeIsNotPositiveInteger;

	public static String ERR_PropertiesMainComposite_ABINotAvailable;

	// Startup options - all
	public static String ERR_PropertiesMainComposite_StartupOpt_NoQuotes;

	// Startup options - text
	public static String ERR_PropertiesMainComposite_StartupOpt_TextBlank;

	// Startup options - number
	public static String ERR_PropertiesMainComposite_StartupOpt_NumberRequired;

	public static String ERR_PropertiesMainComposite_StartupOpt_NumberMustBeInteger;

	public static String ERR_PropertiesMainComposite_StartupOpt_NumberMustBePositiveInteger;

	public static String ERR_PropertiesMainComposite_StartupOpt_NumberIntRange;

	// Startup options - path
	public static String ERR_PropertiesMainComposite_StartupOpt_PathRequired;

	public static String ERR_PropertiesMainComposite_StartupOpt_PathDirNotExist;

	public static String ERR_PropertiesMainComposite_StartupOpt_PathMustBeDir;

	public static String ERR_PropertiesMainComposite_StartupOpt_PathFileNotExist;

	public static String ERR_PropertiesMainComposite_StartupOpt_PathMustBeFile;

	public static String ERR_PropertiesMainComposite_StartupOpt_PathIncorrectFileType;

	/*
	 * Info string area
	 */
	public static String INFO_InfoComposite_EmulatorDefinitionNotFound;

	/*
	 * UI string area
	 */
	public static String UI_General_BrowseButtonLabel;

	public static String UI_General_WizardTitle;

	public static String UI_PropertiesMainComposite_NameLabel;

	public static String UI_PropertiesMainComposite_EmulatorWindowMode_GroupTitle;

	public static String UI_PropertiesMainComposite_EmulatorWindowMode_NativeLabel;

	public static String UI_PropertiesMainComposite_EmulatorWindowMode_VncLabel;

	public static String UI_PropertiesMainComposite_TimeoutLabel;

	public static String UI_WizardMainPage_PageName;

	public static String UI_WizardStartupOptionsPage_PageMessage;

	public static String UI_AndroidDeviceInstance_StopInstanceJob;

	public static String UI_DpiScale_Calculator;

	/*
	 * Wizard - VM area
	 */
	public static String UI_PropertiesMainComposite_TargetLabel;

	public static String UI_PropertiesMainComposite_SkinLabel;

	public static String UI_PropertiesMainComposite_PathLabel;

	public static String UI_PropertiesMainComposite_SDCardLabel;

	public static String UI_PropertiesMainComposite_SDCardNoneLabel;

	public static String UI_PropertiesMainComposite_SDCardExistingLabel;

	public static String UI_PropertiesMainComposite_SDCardNewLabel;

	public static String UI_PropertiesMainComposite_SDCardPathLabel;

	public static String UI_PropertiesMainComposite_SDCardSizeLabel;

	public static String UI_PropertiesMainComposite_PathGroupTitle;

	public static String UI_PropertiesMainComposite_UseDefaultPath;

	/*
	 * Wizard - VM area errors
	 */
	public static String ERR_PropertiesMainComposite_VmTargetEmpty;

	public static String ERR_PropertiesMainComposite_VmSkinEmpty;

	public static String ERR_PropertiesMainComposite_VmPathInvalid;

	/*
	 * Question string area
	 */
	public static String WizardMainPage_NO_SDK_CONFIGURED_MSG;

	public static String UI_SdkSetup_CreateAVD_Title;

	public static String UI_SdkSetup_CreateAVD_Message;

	/*
	 * Exception string area
	 */

	public static String EXC_General_CannotRunStopService;

	public static String EXC_AncroidView_CannotRunMultipleStopServices;

	public static String EXC_AndroidView_ErrorStartingScreens;

	public static String EXC_AbstractZoomHandler_InstanceNotFound;

	public static String EXC_AndroidView_ViewNotFound;

	/*
	 * Warning string area
	 */

	/*
	 * Error string area
	 */

	public static String ERR_AndroidView_ProtocolImplementerNotSupported;

	public static String EXC_AbstractAndroidView_ViewNotAccessibleProgramatically;

	/*
	 * Information string area
	 */

	/*
	 * Question string area
	 */

	public static String QUESTION_AndroidView_StopAllInstancesOnDisposeTitle;

	public static String QUESTION_AndroidView_StopAllInstancesOnDisposeMessage;

	public static String QUESTION_AbstractAndroidView_OpenViewForStartedEmulatorsTitle;

	public static String QUESTION_AbstractAndroidView_OpenViewForStartedEmulatorsMessage;

	public static String QUESTION_RunningInstancesOnClose_Title;

	public static String QUESTION_RunningInstancesOnClose_Text;

	public static String QUESTION_NativeWindow_LooseOriginalScale_Title;

	public static String QUESTION_NativeWindow_LooseOriginalScale_Text;

	/*
	 * Warn string area
	 */
	public static String WARN_RunningInstancesOnClose_Linux_Title;

	public static String WARN_RunningInstancesOnClose_Linux_Text;

	/*
	 * UI string area
	 */

	public static String UI_AbstractAndroidView_StopInstanceJob;

	public static String UI_LayoutContributionItem_NoLayoutsAvailable;

	/*
	 * Progress monitor string area
	 */
	public static String ERR_CannotConnectToVNC;

	public static String ERR_StopEmulatorHandler_NotAnAndroidEmulator;

	public static String ERR_StartEmulatorHandler_NotAnAndroidEmulator;

	public static String ERR_AndroidSkinTranslator_ErrorReadingKeycodeFile;

	public static String ERR_AndroidSkin_NoLayoutLoaded;

	public static String ERR_AndroidSkin_ProvidedSkinPathIsNotADirectory;

	public static String ERR_AndroidSkin_InvalidLayoutProvided;

	public static String ERR_LayoutFileParser_BracketsDoNotMatch;

	public static String ERR_LayoutFileParser_LayoutFileCouldNotBeRead;

	public static String PropertiesMainComposite_ABITypeLabel;

	public static String PropertiesMainComposite_ProxySettings_CheckboxLabel;

	public static String PropertiesMainComposite_ProxySettings_GroupTitle;

	public static String PropertiesMainComposite_ProxySettings_LinkToPreference;

	public static String PropertiesMainComposite_SaveSnapshot;

	public static String PropertiesMainComposite_SDCard_Size_Invalid_Integer;

	public static String PropertiesMainComposite_SnapshotSettings;

	public static String PropertiesMainComposite_startFromSnapshot;

	public static String PropertiesMainComposite_UseSnapshot;

	public static String RepairAvdHandler_AVD_NOT_REPAIRABLE;

	public static String RepairAvdHandler_Not_Android_Instance;

	public static String StartupOptionsComposite_Error_Loading_Skin_Cant_Calculate_Scale;
}
