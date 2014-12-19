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
package org.eclipse.andmore.android.remote;

import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.util.Collection;
import java.util.List;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DdmsRunnable;
import org.eclipse.andmore.android.StudioAndroidEventManager;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DeviceUtils;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceAdapter;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class RemoteDevicePlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.andmore.android.remote";

	/**
	 * The ID of the device declared by this plug-in
	 */
	public static final String DEVICE_ID = PLUGIN_ID + ".androidRemoteDevice";

	public static final String STATUS_ONLINE_ID = PLUGIN_ID + ".status.connected";

	public static final String WIRELESS_PAGE_CONTEXT_HELP_ID = PLUGIN_ID + ".langPage";

	/**
	 * The shared instance.
	 */
	private static RemoteDevicePlugin plugin;

	/**
	 * The service that connects the remote device.
	 */
	private static ServiceHandler connectServiceHandler = null;

	/**
	 * The service that disconnects the remote device.
	 */
	private static ServiceHandler disconnectServiceHandler = null;

	// sync Studio device status for already connected remote devices
	private static final Runnable sdkLoaderListener = new Runnable() {
		@Override
		public void run() {
			Collection<String> serialNumbers = DDMSFacade.getConnectedSerialNumbers();
			for (String serial : serialNumbers) {
				RemoteDeviceUtils.connectDevice(serial);
			}
		}
	};

	/*
	 * Listener called when a new device is connected
	 */
	private static DdmsRunnable connectedListener = new DdmsRunnable() {

		@Override
		public void run(String serialNumber) {
			RemoteDeviceUtils.connectDevice(serialNumber);
		}
	};

	/*
	 * Listener called when a device is disconnected
	 */
	private static DdmsRunnable disconnectedListener = new DdmsRunnable() {

		@Override
		public void run(String serialNumber) {
			RemoteDeviceUtils.disconnectDevice(serialNumber);
		}
	};

	/*
	 * Listener responsible for initializing the Remote Device instances right
	 * after they are loaded by TmL
	 */
	private static final IInstanceListener tmlListener = new InstanceAdapter() {
		@Override
		public void instanceLoaded(InstanceEvent e) {
			IInstance instance = e.getInstance();
			if (instance instanceof RemoteDeviceInstance) {
				IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(instance.getDeviceTypeId());
				IService service = DeviceUtils.getServiceById(device, RemoteDeviceConstants.SERVICE_INIT_ID);
				IServiceHandler handler = service.getHandler();
				try {
					handler.run(instance);
				} catch (SequoyahException e1) {
					warn("Remote Device: the instance " + instance.getName() + " is in an incorrect state ("
							+ e1.getMessage() + ").");
				}
			}
		}
	};

	// Listener that will be used to ask the user if he wants to disconnect the
	// remote devices when the Studio is being closed
	private static final IWorkbenchListener workbenchListener = new RemoteDeviceWorkbenchListener();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(RemoteDevicePlugin.class, "Starting MOTODEV Android Remote Device Plugin...");

		super.start(context);
		plugin = this;
		AndroidPlugin.getDefault().addSDKLoaderListener(sdkLoaderListener);
		StudioAndroidEventManager.asyncAddDeviceChangeListeners(connectedListener, disconnectedListener);
		InstanceEventManager.getInstance().addInstanceListener(tmlListener);
		PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);

		StudioLogger.debug(RemoteDevicePlugin.class, "Starting MOTODEV Android Remote Device Plugin started.");
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
		AndroidPlugin.getDefault().removeSDKLoaderListener(sdkLoaderListener);
		StudioAndroidEventManager.asyncRemoveDeviceChangeListeners(connectedListener, disconnectedListener);
		InstanceEventManager.getInstance().removeInstanceListener(tmlListener);
		PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static RemoteDevicePlugin getDefault() {
		return plugin;
	}

	/**
	 * Retrieves the connect service handler.
	 * 
	 * @return The currently registered connect service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getConnectServiceHandler() {
		if (connectServiceHandler == null) {
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(RemoteDeviceConstants.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(RemoteDeviceConstants.SERVICE_CONNECT_ID)) {
					connectServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return connectServiceHandler;
	}

	/**
	 * Retrieves the disconnect service handler.
	 * 
	 * @return The currently registered disconnect service handler, or <null> if
	 *         no handler is registered.
	 */
	public static ServiceHandler getDisconnectServiceHandler() {
		if (disconnectServiceHandler == null) {
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(RemoteDeviceConstants.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(RemoteDeviceConstants.SERVICE_DISCONNECT_ID)) {
					disconnectServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return disconnectServiceHandler;
	}

	/**
	 * Creates and returns a new image descriptor for an image file in this
	 * plug-in.
	 * 
	 * @param path
	 *            the relative path of the image file, relative to the root of
	 *            the plug-in; the path must be legal
	 * @return an image descriptor, or null if no image could be found
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static boolean isWifiServiceEnabled() {
		Boolean enabled = null;
		try {
			enabled = Boolean.parseBoolean(System.getProperty("enableWifiService"));
		} catch (Exception e) {
			enabled = Boolean.FALSE;
		}
		return enabled;
	}
}
