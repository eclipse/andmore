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

package org.eclipse.andmore.android.emulator.core.devfrm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * DESCRIPTION: This class manages the device frameworks that extend the
 * deviceFramework extension
 *
 * RESPONSIBILITY: Retrieve all deviceFramework extension data and provide a
 * compiled view of the information provided by each extension implementer
 *
 * COLABORATORS: None.
 *
 * USAGE: Use each public method to get the desired information
 */
public class DeviceFrameworkManager {
	/*
	 * Extension point related ids section
	 */
	private static final String DEV_FRAMEWORK_EXTENSION_POINT_ID = EmulatorPlugin.PLUGIN_ID + ".deviceFramework";

	private static final String DEV_FRAMEWORK_ELEM = "deviceFramework";

	private static final String DEV_FRAMEWORK_IMPL_CLASS_ATTR = "class";

	/**
	 * This is a singleton class. The instance is stored in this attribute
	 */
	private static DeviceFrameworkManager instance;

	/**
	 * A collection containing the classes provided by each extension
	 * implementer for retrieving framework data
	 */
	private Collection<IDeviceFrameworkSupport> allFrameworks = new HashSet<IDeviceFrameworkSupport>();

	/**
	 * Singleton private constructor
	 */
	private DeviceFrameworkManager() {
		populateModel();
	}

	/**
	 * Gets the instance of the class
	 * 
	 * @return The instance of the class
	 */
	public static DeviceFrameworkManager getInstance() {
		if (instance == null) {
			instance = new DeviceFrameworkManager();
		}

		return instance;
	}

	/**
	 * Retrieves all instances managed by every device framework which
	 * contributes with the deviceFramework extension point
	 * 
	 * @return A collection containing all instances from all frameworks
	 */
	public Collection<IAndroidEmulatorInstance> getAllInstances() {
		Collection<IAndroidEmulatorInstance> allInstancesSet = new LinkedHashSet<IAndroidEmulatorInstance>();
		for (IDeviceFrameworkSupport devFramework : allFrameworks) {
			Collection<IAndroidEmulatorInstance> devFrmInstances = devFramework.getAllInstances();
			if (devFrmInstances != null) {
				allInstancesSet.addAll(devFrmInstances);
			}
		}

		return allInstancesSet;
	}

	/**
	 * Retrieve all registered and available instances
	 * 
	 * @return List containing all registered and available instances
	 */
	public Collection<IAndroidEmulatorInstance> getAvailableInstances() {
		Collection<IAndroidEmulatorInstance> allInstances = getAllInstances();
		Collection<IAndroidEmulatorInstance> enabledInstances = new ArrayList<IAndroidEmulatorInstance>(
				allInstances.size());

		for (IAndroidEmulatorInstance emulatorInstance : allInstances) {
			if (emulatorInstance.isAvailable()) {
				enabledInstances.add(emulatorInstance);
			}
		}
		return enabledInstances;
	}

	/**
	 * Retrieve a collection of names of all the IAndroidEmulatorInstance of all
	 * Device frameworks...
	 * 
	 * @return A collection of all instances of IAndroidEmulatorInstance.
	 */
	public Collection<String> getAllInstanceNames() {
		Collection<String> allInstancesNames = new LinkedHashSet<String>();
		for (IDeviceFrameworkSupport devFramework : allFrameworks) {
			for (IAndroidEmulatorInstance instance : devFramework.getAllInstances()) {
				allInstancesNames.add(instance.getName());
			}
		}

		return allInstancesNames;

	}

	/**
	 * Retrieves the first occurrence of a IAndroidEmulatorInstance with the
	 * given name provided by any framework.
	 * 
	 * @param name
	 *            of the emulator instance to be retrieved.
	 * @return reference to a IAndroidEmulatorInstance with the given name or a
	 *         null is there are no emulator instance with the given name.
	 */
	public IAndroidEmulatorInstance getInstanceByName(String name) {
		IAndroidEmulatorInstance instanceToReturn = null;

		for (IAndroidEmulatorInstance instance : getAllInstances()) {
			if (Platform.getOS().equals(Platform.WS_WIN32)) {
				if (instance.getName().toLowerCase().equals(name.toLowerCase())) {
					instanceToReturn = instance;
					break;
				}
			} else {
				if (instance.getName().equals(name)) {
					instanceToReturn = instance;
					break;
				}
			}

		}
		return instanceToReturn;

	}

	/**
	 * Retrieves all <b>started</b> instances managed by every device framework
	 * which contributes with the deviceFramework extension point
	 * 
	 * @return A collection containing all started instances from all frameworks
	 */
	public Collection<IAndroidEmulatorInstance> getAllStartedInstances() {
		Collection<IAndroidEmulatorInstance> startedInstancesSet = new HashSet<IAndroidEmulatorInstance>();
		for (IDeviceFrameworkSupport devFramework : allFrameworks) {
			Collection<IAndroidEmulatorInstance> devFrmInstances = devFramework.getAllInstances();
			if (devFrmInstances != null) {
				for (IAndroidEmulatorInstance instance : devFrmInstances) {
					if (instance.isStarted()) {
						startedInstancesSet.add(instance);
					}
				}
			}
		}

		return startedInstancesSet;
	}

	/**
	 * Retrieves all <b>connected</b> instances managed by every device
	 * framework which contributes with the deviceFramework extension point
	 * 
	 * @return A collection containing all connected instances from all
	 *         frameworks
	 */
	public Collection<IAndroidEmulatorInstance> getAllConnectedInstances() {
		Collection<IAndroidEmulatorInstance> connectedInstancesSet = new HashSet<IAndroidEmulatorInstance>();
		for (IDeviceFrameworkSupport devFramework : allFrameworks) {
			Collection<IAndroidEmulatorInstance> devFrmInstances = devFramework.getAllInstances();
			if (devFrmInstances != null) {
				for (IAndroidEmulatorInstance instance : devFrmInstances) {
					if (instance.isConnected()) {
						connectedInstancesSet.add(instance);
					}
				}
			}
		}

		return connectedInstancesSet;
	}

	/**
	 * Retrieves all started instances host addresses managed by every device
	 * framework which contributes with the deviceFramework extension point
	 * 
	 * @return A collection containing all instances from all frameworks
	 */
	public Set<String> getAllStartedInstancesHosts() {
		Set<String> hostSet = new HashSet<String>();
		for (IDeviceFrameworkSupport devFramework : allFrameworks) {
			Collection<IAndroidEmulatorInstance> devFrmInstances = devFramework.getAllInstances();
			if (devFrmInstances != null) {
				for (IAndroidEmulatorInstance instance : devFrmInstances) {
					if (instance.isStarted()) {
						hostSet.add(instance.getInstanceIdentifier());
					}
				}
			}
		}

		return hostSet;
	}

	/**
	 * Populates the allFrameworks collection with framework contributed classes
	 * for retrieving framework information.
	 */
	private void populateModel() {
		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = extReg.getExtensionPoint(DEV_FRAMEWORK_EXTENSION_POINT_ID);
		IExtension[] extensions = extPoint.getExtensions();

		for (IExtension aExtension : extensions) {
			IConfigurationElement[] configElements = aExtension.getConfigurationElements();
			for (IConfigurationElement aConfig : configElements) {
				if (aConfig.getName().equals(DEV_FRAMEWORK_ELEM)) {
					try {
						IDeviceFrameworkSupport devFramework = (IDeviceFrameworkSupport) aConfig
								.createExecutableExtension(DEV_FRAMEWORK_IMPL_CLASS_ATTR);
						if (devFramework != null) {
							allFrameworks.add(devFramework);
						}
					} catch (CoreException e) {
						// Do nothing.
						// If a device framework cannot be instantiated, it will
						// not be plugged to emulator core plugin.
					}
				}
			}
		}
	}
}
