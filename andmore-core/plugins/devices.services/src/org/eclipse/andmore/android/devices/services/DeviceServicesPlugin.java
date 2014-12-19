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
package org.eclipse.andmore.android.devices.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.services.console.ADBShellHandler;
import org.eclipse.andmore.android.devices.services.console.EmulatorConsoleHandler;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DeviceServicesPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.andmore.android.devices.services";

	public static final String DEPLOY_SERVICE_ID = "org.eclipse.andmore.android.devices.services.deployService";

	public static final String UNINSTALL_APP_SERVICE_ID = "org.eclipse.andmore.android.devices.services.uninstallAppService";

	private static ServiceHandler deployServiceHandler = null;

	private static ServiceHandler uninstallAppServiceHandler = null;

	public static final boolean IS_WIN32 = Platform.getOS().equals(Platform.OS_WIN32);

	private static String EMULATOR_CONSOLE_SERVICE_ID = "org.eclipse.andmore.android.devices.services.emulatorConsole";

	private static String ADB_SHELL_SERVICE_ID = "org.eclipse.andmore.android.devices.services.adbShell";

	private static DeviceServicesPlugin plugin;

	private static final String SCREENSHOT_SERVICE_ID = PLUGIN_ID + ".takescreenshot";

	private static final String MONKEY_SERVICE_ID = PLUGIN_ID + ".monkey";

	private static ServiceHandler screenshotServiceHandler = null;

	private static ServiceHandler monkeyServiceHandler = null;

	private static ServiceHandler adbShellServiceHandler = null;

	private static ServiceHandler emulatorConsoleServiceHandler = null;

	public static final String ANDROID_LANG_SERVICE_ID = PLUGIN_ID + ".changeLanguageService";

	public static final String LANG_PAGE_CONTEXT_HELP_ID = PLUGIN_ID + ".langPage";

	private static Collection<IConsoleKilledListener> listeners = new ArrayList<IConsoleKilledListener>();

	private final IWorkbenchListener workbenchListener = new IWorkbenchListener() {

		// killllll all consoles
		@Override
		public boolean preShutdown(IWorkbench workbench, boolean forced) {
			List<IConsole> consolesToClose = new ArrayList<IConsole>();
			List<IConsoleKilledListener> copy = new ArrayList<IConsoleKilledListener>(listeners);
			for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles().clone()) {
				if (console.getName().contains(EmulatorConsoleHandler.CONSOLE_NAME)
						|| console.getName().contains(ADBShellHandler.CONSOLE_NAME)) {
					for (IConsoleKilledListener listener : copy) {
						listener.consoleKilled(console.getName());
					}

					consolesToClose.add(console);
				}

			}
			if (consolesToClose.size() > 0) {

				ConsolePlugin.getDefault().getConsoleManager().removeConsoles(consolesToClose.toArray(new IConsole[0]));
			}
			return true;
		}

		@Override
		public void postShutdown(IWorkbench workbench) {
			// do nothing;
		}
	};

	public interface IConsoleKilledListener {
		void consoleKilled(String consoleName);
	}

	public static void addConsoleKilledListener(IConsoleKilledListener listener) {
		/*
		 * Keep the entire list, even if elements are the same object. This will
		 * ensure that the last console killed will have their proper listener.
		 */
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public static void removeConsoleKilledListener(IConsoleKilledListener listener) {
		listeners.remove(listener);
	}

	public DeviceServicesPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(DeviceServicesPlugin.class, "Starting MOTODEV Android Device Services Plugin...");

		super.start(context);
		plugin = this;
		PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);

		StudioLogger.debug(DeviceServicesPlugin.class, "MOTODEV Android Device Services Plugin started.");
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
		PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DeviceServicesPlugin getDefault() {
		return plugin;
	}

	/**
	 * Creates a console for a process
	 * 
	 * @param p
	 *            The process
	 */
	public static void redirectProcessStreamsToConsole(Process p, String consoleName) {
		InputStream processIn = p.getInputStream();
		OutputStream processOut = p.getOutputStream();
		redirectStreamsToConsole(processIn, processOut, consoleName);
	}

	/**
	 * Creates a console for a process
	 * 
	 * @param p
	 *            The process
	 */
	public static void redirectStreamsToConsole(final InputStream in, final OutputStream out, final String consoleName) {
		final IOConsole console = new IOConsole(consoleName, null);

		console.activate();
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });

		final IOConsoleOutputStream consoleOut = console.newOutputStream();
		final IOConsoleInputStream consoleIn = console.getInputStream();

		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean carriageReturn = false;

				while (true) {
					try {
						int byteRead = in.read();
						if (byteRead == -1) {
							throw new Exception();
						}

						if (carriageReturn && (byteRead != 13)) {
							consoleOut.write(13);
							consoleOut.write(byteRead);
							carriageReturn = false;
						} else if (!carriageReturn && (byteRead == 13)) {
							carriageReturn = true;
						} else {
							consoleOut.write(byteRead);
							carriageReturn = false;
						}
						consoleOut.flush();
					} catch (Exception e) {
						ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { console });

						Collection<IConsoleKilledListener> cloneListeners = new ArrayList<IConsoleKilledListener>(
								listeners);
						for (IConsoleKilledListener listener : cloneListeners) {
							listener.consoleKilled(consoleName);
						}

						break;
					}
				}
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						int byteRead = consoleIn.read();
						out.write(byteRead);
						out.flush();
					} catch (Exception e) {
						break;
					}
				}
			}
		}).start();
	}

	/**
	 * Retrieves the adb shell service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getADBShellServiceHandler() {
		if ((adbShellServiceHandler == null) && (ADB_SHELL_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device =

			DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();

			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(ADB_SHELL_SERVICE_ID)) {
					adbShellServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}
		return adbShellServiceHandler;
	}

	/**
	 * Retrieves the emulator console service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getEmulatorConsoleServiceHandler() {
		if ((emulatorConsoleServiceHandler == null) && (EMULATOR_CONSOLE_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(EMULATOR_CONSOLE_SERVICE_ID)) {
					emulatorConsoleServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return emulatorConsoleServiceHandler;
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

	/**
	 * Retrieves the deploy service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getDeployServiceHandler() {
		if ((deployServiceHandler == null) && (DEPLOY_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(DEPLOY_SERVICE_ID)) {
					deployServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return deployServiceHandler;
	}

	/**
	 * Retrieves the deploy service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getUninstallAppServiceHandler() {
		if ((uninstallAppServiceHandler == null) && (UNINSTALL_APP_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(UNINSTALL_APP_SERVICE_ID)) {
					uninstallAppServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return uninstallAppServiceHandler;
	}

	/**
	 * Retrieves the deploy service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getScreenshotServiceHandler() {
		if ((screenshotServiceHandler == null) && (SCREENSHOT_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			for (IService service : services) {
				IServiceHandler handler = service.getHandler();
				if (handler.getService().getId().equals(SCREENSHOT_SERVICE_ID)) {
					screenshotServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return screenshotServiceHandler;
	}

	/**
	 * Retrieves the monkey service handler.
	 * 
	 * @return The currently registered stop service handler, or <null> if no
	 *         handler is registered.
	 */
	public static ServiceHandler getMonkeyServiceHandler() {
		if ((monkeyServiceHandler == null) && (MONKEY_SERVICE_ID != null)) {
			// find the appropriate stop service handler
			IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(EmulatorPlugin.DEVICE_ID);
			List<IService> services = device.getServices();
			IServiceHandler handler = null;
			for (IService service : services) {
				handler = service.getHandler();
				if (handler.getService().getId().equals(MONKEY_SERVICE_ID)) {
					monkeyServiceHandler = (ServiceHandler) handler;
					break;
				}
			}
		}

		return monkeyServiceHandler;
	}
}
