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
package org.eclipse.andmore.android.emulator.device;

import java.io.File;

/**
 * DESCRIPTION: This interface contains the name and some default values of the
 * Device Instance Properties
 */
public interface IDevicePropertiesConstants {
	/**
	 * The key that identifies the description property of an Android Emulator
	 * device instance
	 */
	String deviceDescription = "Description";

	/**
	 * The key that identifies the emulator type property of an Android Emulator
	 * device instance
	 */
	String emulatorDefId = "Emulator_Type";

	/**
	 * The key that identifies the skin property of an Android Emulator device
	 * instance
	 */
	String skinId = "Skin_Plugin_Id";

	/**
	 * The key that identifies the timeout property of an Android Emulator
	 * device instance
	 */
	String timeout = "Timeout";

	/**
	 * The key that identifies the useVnc property of an Android Emulator device
	 * instance
	 */
	String useVnc = "UseVnc";

	/**
	 * The key that identifies the useVnc property of an Android Emulator device
	 * instance
	 */
	String useProxy = "UseProxy";

	/**
	 * The key that identifies the VM Target property of an Android Emulator
	 * device instance
	 */
	String vmTarget = "Vm_Target";

	/**
	 * The key that identifies the VM ABI type property of an Android Emulator
	 * device instance
	 */
	String abiType = "Abi_Type";

	/**
	 * The key that identifies the VM Skin property of an Android Emulator
	 * device instance
	 */
	String vmSkin = "Vm_Skin";

	/**
	 * The key that identifies the VM Path property of an Android Emulator
	 * device instance
	 */
	String vmPath = "Vm_Path";

	/**
	 * The key that identifies the command line arguments to be used when
	 * starting the emulator
	 */
	String commandline = "Command_Line";

	/**
	 * AVD Config file properties
	 */
	String configSDCardPath = "sdcard.path";

	String configSDCardSize = "sdcard.size";

	/**
	 * The default vm path
	 */
	String defaultVmPath = System.getProperty("user.home") + File.separator + ".android" + File.separator + "avd";

	String defaultVmFolderSuffix = ".avd";

	String defaultUseProxyValue = "false";

	/**
	 * The default timeout value (ms), which must be used when creating a new
	 * emulator instance
	 */
	String defaultTimeoutValue = "120";

	/**
	 * Whether to use snapshots
	 */

	String useSnapshots = "UseSnapshot";

	String defaultUseSnapshotValue = "false";

	String saveSnapshot = "SaveSnapshot";

	String dafaultSaveSnapshotValue = "false";

	String startFromSnapshot = "startFromSnapshot";

	String defaultstartFromSnapshotValue = "false";

	String defaulSaveSnapshot = "false";
}
