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
package org.eclipse.andmore.android.mat;

import java.util.List;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.mat";

	// The shared instance
	private static Activator plugin;

	// Dump HPROF command handler
	private static ServiceHandler dumpHPROFHandler = null;

	// Dump HPRFO service ID
	private static final String SERVICE_DUMP_HPROF_ID = PLUGIN_ID + ".dumpHprofService";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Retrieves the deploy service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getDumpHPROFHandler() {
		if (dumpHPROFHandler == null) {
			// find the appropriate service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(SERVICE_DUMP_HPROF_ID)) {
					dumpHPROFHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return dumpHPROFHandler;
	}

}
