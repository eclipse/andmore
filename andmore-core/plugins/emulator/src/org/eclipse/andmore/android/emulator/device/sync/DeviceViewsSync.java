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
package org.eclipse.andmore.android.emulator.device.sync;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.ui.view.InstanceMgtView;
import org.eclipse.sequoyah.device.framework.ui.view.model.InstanceSelectionChangeEvent;
import org.eclipse.sequoyah.device.framework.ui.view.model.InstanceSelectionChangeListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

public class DeviceViewsSync {

	/**
	 * DeviceViewsSync unique instance
	 */
	private static DeviceViewsSync instance = null;

	/**
	 * Views
	 */
	public static final int EMULATOR_VIEW = 0; // Emulator View

	public static final int DEVICE_VIEW = 1; // Device Management View

	public static final int DDMS_VIEW = 2; // DDMS Device View

	/**
	 * Methods used to update the Views
	 */
	private Method[] syncMethods = null;

	/**
	 * During the synchronization, it stores the instance that shall be set to
	 * avoid loops
	 */
	private static String syncInstance = null;

	/**
	 * Singleton
	 * 
	 * @return DeviceViewsSync
	 */
	public static DeviceViewsSync getInstance() {
		if (instance == null) {
			instance = new DeviceViewsSync();
		}
		return instance;
	}

	/*
	 * Constructor
	 * 
	 * Define the synchronization methods Define the methods that retrieve the
	 * current selection in a View
	 */
	@SuppressWarnings("rawtypes")
	private DeviceViewsSync() {

		try {

			/*
			 * Register methods that update each view
			 */
			Class parameterTypes[] = new Class[1];
			parameterTypes[0] = String.class;

			syncMethods = new Method[3];

			syncMethods[EMULATOR_VIEW] = this.getClass().getDeclaredMethod("syncEmulatorView", parameterTypes);
			syncMethods[DEVICE_VIEW] = this.getClass().getDeclaredMethod("syncDeviceView", parameterTypes);
			syncMethods[DDMS_VIEW] = this.getClass().getDeclaredMethod("syncDDMSView", parameterTypes);
		} catch (Exception e) {
			StudioLogger.error("Could not add syncronization method: " + e.getMessage());
		}

	}

	/**
	 * Add listeners to events that must initiate the synchronization procedures
	 * 
	 * #1) Emulator View
	 * 
	 * #2) Device Management View
	 * 
	 * #3) DDMS Device View
	 */
	public void initialize() {

		/*
		 * Synchronization #1 Add listener to Emulator View tab switch event
		 */
		AbstractAndroidView.addTabSwitchListener(new Listener() {
			@Override
			public void handleEvent(Event event) {

				IAndroidEmulatorInstance activeInstance = AbstractAndroidView.getActiveInstance();
				if (activeInstance != null) {
					String selectedInstanceName = activeInstance.getName();
					if ((selectedInstanceName != null) && (!selectedInstanceName.equals(syncInstance))) {
						sync(EMULATOR_VIEW, selectedInstanceName);
					}
				}

			}
		});

		/*
		 * Synchronization #2
		 */
		InstanceMgtView.addInstanceSelectionChangeListener(new InstanceSelectionChangeListener() {
			@Override
			public void instanceSelectionChanged(InstanceSelectionChangeEvent event) {

				IInstance instance = event.getInstance();
				if ((instance != null) && (EmulatorPlugin.STATUS_ONLINE_ID.equals(instance.getStatus()))) {
					String selectedInstanceName = instance.getName();
					if ((selectedInstanceName != null) && (!selectedInstanceName.equals(syncInstance))) {
						sync(DEVICE_VIEW, selectedInstanceName);
					}
				}

			}
		});

		/*
		 * Synchronization #3
		 */
		DdmsPlugin.getDefault().addSelectionListener(new DdmsPlugin.ISelectionListener() {
			@Override
			public void selectionChanged(Client client) {
				// none
			}

			@Override
			public void selectionChanged(IDevice device) {

				if (device != null) {
					String selectedInstanceName = device.getAvdName();
					if ((selectedInstanceName != null) && (!selectedInstanceName.equals(syncInstance))) {
						sync(DDMS_VIEW, selectedInstanceName);
					}
				}

			}
		});
	}

	/*
	 * Run the synchronization procedures
	 * 
	 * @param fireSyncView the View that has been changed and requires others to
	 * synchronize
	 * 
	 * @param instanceName the Device Instance name
	 */
	private void sync(Integer fireSyncView, String instanceName) {
		syncInstance = instanceName;

		Object arglist[] = new Object[1];
		arglist[0] = instanceName;

		for (int i = 0; i < syncMethods.length; i++) {
			if (i != fireSyncView) {
				try {
					syncMethods[i].invoke(this, arglist);
				} catch (Exception e) {
					StudioLogger.error("Could not call syncronization method for " + i + " : " + e.getMessage());
				}
			}
		}

		syncInstance = null;

	}

	/*
	 * Synchronize the Emulator View by setting the selected instance
	 * 
	 * @param instanceName the Device Instance name
	 */
	@SuppressWarnings("unused")
	private void syncEmulatorView(String instanceName) {
		try {
			IAndroidEmulatorInstance emulatorInstance = DeviceFrameworkManager.getInstance().getInstanceByName(
					instanceName);
			if (emulatorInstance != null) {
				AbstractAndroidView.setInstance(emulatorInstance);
			} else {
				StudioLogger.warn("Could not synchronize with Emulator View: " + instanceName
						+ " not in DeviceFrameworkManager model");
			}

		} catch (Exception e) {
			StudioLogger.error("Could not synchronize with Emulator View: " + e.getMessage());
		}
	}

	/*
	 * Synchronize the Device Management View by setting the selected instance
	 * 
	 * @param instanceName the Device Instance name
	 */
	@SuppressWarnings("unused")
	private void syncDeviceView(String instanceName) {

		try {
			InstanceRegistry registry = InstanceRegistry.getInstance();
			List<IInstance> tmlInstances = registry.getInstancesByName(instanceName);
			if (tmlInstances.size() > 0) {
				IInstance tmlInstance = tmlInstances.get(0);
				InstanceMgtView.setSeletectedInstance(tmlInstance);
			} else {
				StudioLogger.warn("Could not synchronize with Device Management View: " + instanceName
						+ " not in TmL InstanceManager model");
			}
		} catch (Exception e) {
			StudioLogger.error("Could not synchronize with Device Management View: " + e.getMessage());
		}

	}

	/*
	 * Synchronize the DDMS Device View by setting the selected instance
	 * 
	 * @param instanceName the Device Instance name
	 */
	@SuppressWarnings("unused")
	private void syncDDMSView(String instanceName) {
		try {
			IDevice device = DDMSFacade.getDeviceWithVmName(instanceName);
			if (device != null) {
				DdmsPlugin.getDefault().selectionChanged(device, null);
			} else {
				StudioLogger
						.warn("Could not synchronize with DDMS Devices View: Could not retrieve Device object from ADT model");
			}
		} catch (Exception e) {
			StudioLogger.error("Could not synchronize with DDMS Devices View: " + e.getMessage());
		}

	}
}
