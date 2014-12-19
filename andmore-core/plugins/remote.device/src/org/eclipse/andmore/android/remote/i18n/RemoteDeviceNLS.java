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
package org.eclipse.andmore.android.remote.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * This class is the NLS component for this plug-in.
 */
public class RemoteDeviceNLS extends NLS {
	/**
	 * The bundle location. It refers to messages.properties file inside this
	 * package
	 */

	static {
		NLS.initializeMessages("org.eclipse.andmore.android.remote.i18n.remoteDeviceNLS", RemoteDeviceNLS.class);
	}

	public static String UI_Host;

	public static String UI_Name;

	public static String UI_Timeout;

	public static String UI_RemoteDeviceWizardPage_WizardName;

	public static String UI_RemoteDeviceWizardPage_Title;

	public static String UI_RemoteDeviceWizardPage_Description;

	public static String UI_WirelessWizard_Name;

	public static String UI_WirelessInformationPage_Title;

	public static String UI_WirelessInformationPage_Description;

	public static String ERR_WirelessDeviceWizardPage_Name;

	public static String ERR_RemoteDeviceWizardPage_IP;

	public static String ERR_RemoteDeviceWizardPage_Port;

	public static String ERR_RemoteDeviceWizardPage_Timeout;

	public static String ERR_RemoteDeviceWizardPage_Duplicated;

	public static String QUESTION_ConnectedRemoteDevicesOnClose_Title;

	public static String QUESTION_ConnectedRemoteDevicesOnClose_Text;

	public static String ERR_ConnectToRemote_AdbStart;

	public static String ERR_DisconnectToRemote_AdbStart;

	public static String SwitchFromUSBAndConnectToWirelessRunnable_CreatingRemoteDeviceInstance;

	public static String SwitchFromUSBAndConnectToWirelessRunnable_MsgCreatingWirelessRemoteDevice;

	public static String SwitchFromUSBAndConnectToWirelessRunnable_MsgNotPossibleToConvertUSBToTCPIP;

	public static String SwitchFromUSBAndConnectToWirelessRunnable_ConnectingToWifiDevice;

	public static String WirelessPropertiesComposite_MsgPortNumberEqualOrHigherThan;

	public static String WirelessServiceHandler_MsgLaunchingWirelessConnection;

	public static String WirelessServiceHandler_MsgPingingIPAddress;

	public static String WirelessServiceHandler_MsgRetrievingDeviceIPNumber;

	public static String WirelessWizard_MsgErrorProblemsSwitchingDeviceToTCPIP;

	public static String WirelessWizard_TitleWirelessConnectionModeWizard;

	public static String WirelessWizard_WirelessDeviceCreatedSuccessfully;

	public static String ERR_WirelessWizard_Reach_IP;

	public static String ERR_WirelessWizard_No_IP;

	public static String ERR_WirelessWizard_NOT_VALID_SDK;

	public static String UI_Port;

	public static String USBModeServiceHandler_2;

	public static String USBModeServiceHandler_MsgItWasNotPossibleToSwitchDeviceToUSBMode;

	public static String USBModeServiceHandler_MsgItWasNotPossibleToSwitchToUSBMode;

	public static String USBModeServiceHandler_MsgStartingProcessOfSwitchingToUSBMode;

	public static String USBModeServiceHandler_MsgSuccess;

	public static String USBModeServiceHandler_MsgSwithcingTCPToUSB;

	public static String USBModeServiceHandler_MsgUnableToSwitchToUSBDueToSDKVersion;

	public static String ERR_RemoteDeviceWizardPage_WirelessDuplicated;

	public static String Title_ReturningToUSBConnectionDialog;
}
