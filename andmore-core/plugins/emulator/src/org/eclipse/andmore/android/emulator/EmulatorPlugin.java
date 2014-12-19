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

package org.eclipse.andmore.android.emulator;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.List;
import java.util.Properties;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DdmsRunnable;
import org.eclipse.andmore.android.StudioAndroidEventManager;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.device.AndroidDeviceUtils;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.SequoyahLogRedirector;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDevInstListener;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.device.refresh.InstancesListRefresh;
import org.eclipse.andmore.android.emulator.device.sync.DeviceViewsSync;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceAdapter;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIPlugin;
import org.eclipse.sequoyah.device.framework.ui.view.InstanceMgtView;
import org.eclipse.sequoyah.device.framework.ui.wizard.DefaultDeviceTypeMenuWizardPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EmulatorPlugin extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.emulator";

	// The shared instance
	private static EmulatorPlugin plugin;

	// The ID of the device declared by this plug-in
	public static final String DEVICE_ID = PLUGIN_ID + ".androidDevice";

	// The ID of all the status declared by this plug-in
	public static final String STATUS_ONLINE_ID = PLUGIN_ID + ".status.online";

	public static final String STATUS_OFFLINE_NO_DATA = PLUGIN_ID + ".status.offlineNoData";

	public static final String STATUS_OFFLINE = PLUGIN_ID + ".status.offline";

	public static final String STATUS_NOT_AVAILABLE = PLUGIN_ID + ".status.notavailable";

	public static final String SERVICE_INIT_ID = PLUGIN_ID + ".initEmulatorService";

	public static final String STOP_SERVICE_ID = PLUGIN_ID + ".stopService";

	public static final String START_SERVICE_ID = PLUGIN_ID + ".startService";

	private static final String DEV_MANAGER_HELP = DeviceUIPlugin.PLUGIN_ID + ".devmgr";

	private static final String NEW_DEVICE_HELP = DeviceUIPlugin.PLUGIN_ID + ".newdev";

	/**
	 * Reference the id of the extension point with the default Android Emulator
	 * definitions...
	 */
	public static String DEFAULT_EMULATOR_DEFINITION = "org.eclipse.andmore.android.emulator10.defaultEmulatorDefinitions";

	public static final String FORCE_ATTR = "force";

	public static final String EMULATOR_UNEXPECTEDLY_STOPPED = "emulator.unexpectedly.stopped";

	private static AndroidDevInstListener instanceListener;

	private static DdmsRunnable connectedListener = new DdmsRunnable() {
		@Override
		public void run(String serialNumber) {
			if (DDMSFacade.isEmulator(serialNumber)) {
				InstancesListRefresh.refresh();

				info("New Device connected at " + serialNumber);

				String vmName = DDMSFacade.getNameBySerialNumber(serialNumber);

				if (vmName != null) {
					DeviceFrameworkManager devFrameworkManager = DeviceFrameworkManager.getInstance();

					IAndroidEmulatorInstance instance = devFrameworkManager.getInstanceByName(vmName);

					if (instance instanceof AndroidDeviceInstance) {
						final AndroidDeviceInstance emulatorInstance = (AndroidDeviceInstance) instance;

						AndroidDeviceUtils.fireDummyStartTransition(emulatorInstance, serialNumber);

					}
				}
			}
		}
	};

	private static DdmsRunnable disconnectedListener = new DdmsRunnable() {
		@Override
		public void run(String serialNum) {
			if (DDMSFacade.isEmulator(serialNum)) {
				info("Device just disconnected from serial=" + serialNum);

				String vmName = DDMSFacade.getNameBySerialNumber(serialNum);

				if (vmName != null) {
					IAndroidEmulatorInstance instance = DeviceFrameworkManager.getInstance().getInstanceByName(vmName);

					if ((instance != null) && (instance.isStarted())) {
						try {
							instance.stop(true);
							DialogWithToggleUtils.showError(EMULATOR_UNEXPECTEDLY_STOPPED, EmulatorNLS.GEN_Error,
									NLS.bind(EmulatorNLS.ERR_AndroidLogicPlugin_EmulatorStopped, instance.getName()));

						} catch (Exception e) {
							error("Error trying to force the stop process on instance associated to disconnected device: "
									+ instance);
						}
					}

					if (instance instanceof AndroidDeviceInstance) {
						((AndroidDeviceInstance) instance).setNameSuffix(null);
						InstanceEventManager.getInstance()
								.notifyListeners(
										new InstanceEvent(InstanceEventType.INSTANCE_UPDATED,
												(AndroidDeviceInstance) instance));
					}
				} else {
					// This block is executed if we get a vmName == null
					// condition. This can happen if
					// ADT updates the device in a way that it makes the name
					// not accessible.
					//
					// What is needed to be done in such a case is to iterate on
					// all TmL instances, looking for
					// objects that contain serialNumber as the instance suffix.
					// This guarantees that we will not
					// leave a not consistent serial number being displayed at
					// the Instance Management view.

					for (IAndroidEmulatorInstance instance : DeviceFrameworkManager.getInstance().getAllInstances()) {
						if (instance instanceof AndroidDeviceInstance) {
							AndroidDeviceInstance androidInstance = (AndroidDeviceInstance) instance;
							String instanceSuffix = androidInstance.getNameSuffix();

							if ((instanceSuffix != null) && instanceSuffix.equals(serialNum)) {
								androidInstance.setNameSuffix(null);

								InstanceEventManager.getInstance().notifyListeners(
										new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, androidInstance));
							}
						}
					}
				}
			}
		}
	};

	private static final Runnable sdkLoaderListener = new Runnable() {
		@Override
		public void run() {
			InstancesListRefresh.refresh();
			if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
				IPreferenceStore store = getDefault().getPreferenceStore();
				boolean deviceStartupOptionsUpdated = store.getBoolean("DeviceStartupOptionsUpdated");
				if (!deviceStartupOptionsUpdated) {
					for (IAndroidEmulatorInstance instance : DeviceFrameworkManager.getInstance().getAllInstances()) {
						if (instance instanceof AndroidDeviceInstance) {
							AndroidDeviceInstance androidInstance = (AndroidDeviceInstance) instance;

							Properties emuProperties = androidInstance.getProperties();

							String commandline = emuProperties.getProperty(IDevicePropertiesConstants.commandline, "");
							if (commandline.contains("-no-window")) {
								commandline = commandline.replace("-no-window", "");
							}
							emuProperties.setProperty(IDevicePropertiesConstants.commandline, commandline);
							androidInstance.setProperties(emuProperties);

							InstanceEventManager.getInstance().notifyListeners(
									new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, androidInstance));
						}
					}
					store.setValue("DeviceStartupOptionsUpdated", true);
				}
			}
		}
	};

	private static IInstanceListener sequoyahInstanceListener = new InstanceAdapter() {
		@Override
		public void instanceUpdated(InstanceEvent e) {
			AbstractAndroidView.updateInstanceName(e.getInstance());
		}
	};

	private static ServiceHandler stopServiceHandler = null;

	private static ServiceHandler startServiceHandler = null;

	private static String stopServiceId = null;

	private static String startServiceId = null;

	/**
	 * The constructor
	 */
	public EmulatorPlugin() {
		plugin = this;
	}

	/**
	 * Activates the plug-in and initializes the logger
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(EmulatorPlugin.class, "Starting MOTODEV Android Emulator Plugin...");

		super.start(context);

		start();

		StudioLogger.debug(EmulatorPlugin.class, "MOTODEV Android Emulator Plugin started.");
	}

	private void start() {
		// Setting the TmL logger to redirect logs to the logger controlled
		// by this class
		SequoyahLogRedirector tmlLogger = new SequoyahLogRedirector();
		org.eclipse.sequoyah.vnc.utilities.logger.Logger.setLogger(tmlLogger);
		BasePlugin.getBaseDefault().setLogger(tmlLogger);

		instanceListener = new AndroidDevInstListener();
		InstanceEventManager.getInstance().addInstanceListener(instanceListener);
		StudioAndroidEventManager.asyncAddDeviceChangeListeners(connectedListener, disconnectedListener);

		AndroidPlugin.getDefault().addSDKLoaderListener(sdkLoaderListener);
		// Emulator Views synchronization
		DeviceViewsSync.getInstance().initialize();
		// Setting context sensitive help IDs for the TmL screens we use
		DefaultDeviceTypeMenuWizardPage.setHelpContextId(NEW_DEVICE_HELP);
		InstanceMgtView.setHelp(DEV_MANAGER_HELP);
		InstanceEventManager.getInstance().addInstanceListener(sequoyahInstanceListener);
		registerStopServiceId(STOP_SERVICE_ID);
		registerStartServiceId(START_SERVICE_ID);
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		AndroidPlugin.getDefault().removeSDKLoaderListener(sdkLoaderListener);
		InstanceEventManager.getInstance().removeInstanceListener(instanceListener);
		StudioAndroidEventManager.asyncRemoveDeviceChangeListeners(connectedListener, disconnectedListener);
		InstanceEventManager.getInstance().removeInstanceListener(sequoyahInstanceListener);
		unregisterStopServiceHandler();
		unregisterStartServiceHandler();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Registers a stop service id, through which the stop service handler will
	 * be found and used to delegate stop action of the instances if possible
	 * 
	 * @param stopServiceId
	 *            The stop service id to be registered
	 */
	public static void registerStopServiceId(String stopServiceId) {
		EmulatorPlugin.stopServiceId = stopServiceId;
	}

	/**
	 * Unregisters the current stop service handler and stop service id.
	 * 
	 * After this method is called, it will not be possible for the instance
	 * class to delegate the stop action to a handler.
	 */
	public static void unregisterStopServiceHandler() {
		stopServiceHandler = null;
		stopServiceId = null;
	}

	/**
	 * Retrieves the stop service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getStopServiceHandler() {
		if ((stopServiceHandler == null) && (stopServiceId != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(stopServiceId)) {
					stopServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return stopServiceHandler;
	}

	/**
	 * Registers a start service id, through which the stop service handler will
	 * be found and used to delegate start action of the instances if possible
	 * 
	 * @param stopServiceId
	 *            The stop service id to be registered
	 */
	public static void registerStartServiceId(String startServiceId) {
		EmulatorPlugin.startServiceId = startServiceId;
	}

	/**
	 * Unregisters the current start service handler and stop service id.
	 * 
	 * After this method is called, it will not be possible for the instance
	 * class to delegate the start action to a handler.
	 */
	public static void unregisterStartServiceHandler() {
		startServiceHandler = null;
		startServiceId = null;
	}

	/**
	 * Retrieves the start service handler.
	 * 
	 * @return The currently registered start service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getStartServiceHandler() {
		if ((startServiceHandler == null) && (startServiceId != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(startServiceId)) {
					startServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return startServiceHandler;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EmulatorPlugin getDefault() {
		return plugin;
	}
}
