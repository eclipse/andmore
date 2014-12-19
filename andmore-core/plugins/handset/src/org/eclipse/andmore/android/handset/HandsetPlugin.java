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
package org.eclipse.andmore.android.handset;

import java.util.Collection;
import java.util.Properties;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DdmsRunnable;
import org.eclipse.andmore.android.StudioAndroidEventManager;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class HandsetPlugin extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.handset";

	public static final String HANDSET_DEVICE_TYPE_ID = PLUGIN_ID + ".androidHandset";

	public static final String STATUS_ONLINE_ID = PLUGIN_ID + ".status.handsetonline";

	public static final String SERVICE_INIT_ID = PLUGIN_ID + ".initHandsetService";

	private static final Runnable sdkLoaderListener = new Runnable() {
		@Override
		public void run() {
			Collection<String> serialNumbers = DDMSFacade.getConnectedSerialNumbers();
			for (String serial : serialNumbers) {
				createInstance(serial);
			}
		}
	};

	// The shared instance
	private static HandsetPlugin plugin;

	private static DdmsRunnable connectedListener = new DdmsRunnable() {

		@Override
		public void run(String serialNumber) {
			createInstance(serialNumber);
		}
	};

	private static DdmsRunnable disconnectedListener = new DdmsRunnable() {

		@Override
		public void run(String serialNumber) {
			deleteInstance(serialNumber);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(HandsetPlugin.class, "Starting MOTODEV Android Handset Plugin...");

		super.start(context);
		plugin = this;
		StudioAndroidEventManager.asyncAddDeviceChangeListeners(connectedListener, disconnectedListener);
		AndroidPlugin.getDefault().addSDKLoaderListener(sdkLoaderListener);

		StudioLogger.debug(HandsetPlugin.class, "MOTODEV Android Handset Plugin started.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		AndroidPlugin.getDefault().removeSDKLoaderListener(sdkLoaderListener);
		StudioAndroidEventManager.asyncRemoveDeviceChangeListeners(connectedListener, disconnectedListener);
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static HandsetPlugin getDefault() {
		return plugin;
	}

	/**
	 * Creates a TmL instance for the given handset device
	 * 
	 * @param serialNumber
	 *            The serial number of the device to create a TmL instance for
	 */
	private static void createInstance(String serialNumber) {
		if (!DDMSFacade.isEmulator(serialNumber) && !DDMSFacade.isRemote(serialNumber)) {

			try {
				Properties instanceProperties = DDMSFacade.getDeviceProperties(serialNumber);

				HandsetInstanceBuilder projectBuilder = new HandsetInstanceBuilder(serialNumber, instanceProperties);

				DevicesManager.getInstance().createInstanceForDevice(serialNumber,
						HandsetPlugin.HANDSET_DEVICE_TYPE_ID, projectBuilder, HandsetPlugin.SERVICE_INIT_ID);
			} catch (SequoyahException e) {
				StudioLogger
						.error(HandsetPlugin.class, "Failed to create a TmL instance for device " + serialNumber, e);
			}
		}
	}

	/**
	 * Destroys the TmL instance of the given handset device
	 * 
	 * @param device
	 *            The device to delete the correspondent TmL instance
	 */
	private static void deleteInstance(String serialNumber) {
		if (!DDMSFacade.isEmulator(serialNumber) && !DDMSFacade.isRemote(serialNumber)) {
			DevicesManager.getInstance().deleteInstanceOfDevice(serialNumber);
		}
	}

}
