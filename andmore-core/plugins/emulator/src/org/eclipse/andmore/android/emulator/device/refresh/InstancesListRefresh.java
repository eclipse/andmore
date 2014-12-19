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
package org.eclipse.andmore.android.emulator.device.refresh;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Collection;
import java.util.Properties;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.device.AndroidDeviceUtils;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDevInstBuilder;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;

/**
 * This class is responsible for refreshing the TML Instances List It checks if
 * the user has created more Android VMs by himself
 * 
 */
public class InstancesListRefresh {

	/**
	 * If the number of Android VMs is different from the number of TML
	 * Instances, the TML Instances list is updated
	 */
	public static synchronized void refresh() {

		SdkUtils.reloadAvds();

		DeviceFrameworkManager devFramework = DeviceFrameworkManager.getInstance();

		if (SdkUtils.getCurrentSdk() != null) {
			final Collection<String> vmInstances = SdkUtils.getAllVmNames();
			final Collection<String> validVmInstances = SdkUtils.getAllValidVmNames();
			final Collection<String> emulatorInstances = devFramework.getAllInstanceNames();

			createAndUpdateEmulatorInstances(vmInstances, validVmInstances, emulatorInstances);
		}
	}

	/**
	 * Creates Emulator instances to represent every VM available in the system.
	 * 
	 * @param validVmInstances
	 **/
	public static void createAndUpdateEmulatorInstances(Collection<String> vmInstances,
			Collection<String> validVmInstances, Collection<String> emulatorInstances) {

		IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);

		for (String instanceName : vmInstances) {
			/*
			 * In case the is no TmL instances for a given VM, create the TmL
			 * Instance
			 */
			if (!emulatorInstances.contains(instanceName)) {

				Properties instanceProperties = new Properties();

				AndroidDeviceInstance.populateWithVMInfo(instanceName, instanceProperties);

				AndroidDeviceInstance.populateWithDefaultProperties(instanceProperties);

				AndroidDevInstBuilder projectBuilder = new AndroidDevInstBuilder(instanceName, instanceProperties);

				try {
					InstanceManager.createProject(device, projectBuilder, new NullProgressMonitor());
				} catch (SequoyahException e) {
					error("There was an error while creating an emulator instance: " + instanceName + ". Message: "
							+ e.getMessage());
				}

				refreshStatus(validVmInstances, instanceName);

				info("Added instance " + instanceName + " using default emulator definitions ");
			}

		}

		for (String emulatorInstance : emulatorInstances) {
			refreshStatus(validVmInstances, emulatorInstance);
		}

	}

	public static void refreshStatus(Collection<String> vmInstances, String instanceName) {
		/*
		 * Refresh status
		 */
		IAndroidEmulatorInstance instance = DeviceFrameworkManager.getInstance().getInstanceByName(instanceName);

		AndroidDeviceInstance androidDeviceInstance = (AndroidDeviceInstance) instance;

		AndroidDeviceInstance
				.populateWithVMInfo(androidDeviceInstance.getName(), androidDeviceInstance.getProperties());

		String currentStatus = androidDeviceInstance.getStatus();

		if (androidDeviceInstance.hasDevice()) {
			if ((androidDeviceInstance.getStatus().equals(EmulatorPlugin.STATUS_NOT_AVAILABLE))
					|| (androidDeviceInstance.getStatus().equals(DevicePlugin.SEQUOYAH_STATUS_OFF))) {
				if (!EmulatorPlugin.STATUS_OFFLINE_NO_DATA.equals(currentStatus)) {
					androidDeviceInstance.setNameSuffix(null);
					androidDeviceInstance.setStatus(EmulatorPlugin.STATUS_OFFLINE_NO_DATA);
				}
			}
			AndroidDeviceUtils.fireDummyStartTransition(androidDeviceInstance, androidDeviceInstance.getSerialNumber());
		} else {

			if (vmInstances.contains(androidDeviceInstance.getName())) {
				if (androidDeviceInstance.isClean()) {
					if (!EmulatorPlugin.STATUS_OFFLINE_NO_DATA.equals(currentStatus)) {
						androidDeviceInstance.setNameSuffix(null);
						androidDeviceInstance.setStatus(EmulatorPlugin.STATUS_OFFLINE_NO_DATA);
					}
				} else {
					if (!EmulatorPlugin.STATUS_OFFLINE.equals(currentStatus)) {
						androidDeviceInstance.setNameSuffix(null);
						androidDeviceInstance.setStatus(EmulatorPlugin.STATUS_OFFLINE);
					}
				}
			} else {
				if (!EmulatorPlugin.STATUS_NOT_AVAILABLE.equals(currentStatus)) {
					androidDeviceInstance.setNameSuffix(null);
					androidDeviceInstance.setStatus(EmulatorPlugin.STATUS_NOT_AVAILABLE);
				}
			}
		}
	}
}