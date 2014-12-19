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
package org.eclipse.andmore.android.devices.services.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin.IConsoleKilledListener;
import org.eclipse.andmore.android.devices.services.i18n.ServicesNLS;
import org.eclipse.andmore.android.utilities.TelnetFrameworkAndroid;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Class responsible to implement the handler for the service "Emulator Console"
 */
public class EmulatorConsoleHandler extends ServiceHandler {
	private static final Map<String, Integer> consolesCache = new HashMap<String, Integer>();

	private static final Map<String, TelnetFrameworkAndroid> telnetsCache = new HashMap<String, TelnetFrameworkAndroid>();

	private final IConsoleKilledListener listener = new IConsoleKilledListener() {
		@Override
		public void consoleKilled(String name) {
			if (telnetsCache.containsKey(name)) {
				TelnetFrameworkAndroid telnet = telnetsCache.get(name);
				if (telnet.isConnected()) {
					try {
						telnet.disconnect();
					} catch (IOException e) {
						EclipseUtils.showInformationDialog(ServicesNLS.GEN_Warning,
								ServicesNLS.WARN_EmulatorConsoleHandler_CouldNotCloseTheConsoleConnection);
					}
				}
				telnetsCache.remove(name);
				DeviceServicesPlugin.removeConsoleKilledListener(listener);
			}
		}
	};

	public static final String CONSOLE_NAME = "Emulator Console";

	private static final String LOCALHOST = "localhost";

	/**
	 * Constructor
	 */
	public EmulatorConsoleHandler() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new EmulatorConsoleHandler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#runService
	 * (org.eclipse.sequoyah.device.framework.model.IInstance, java.util.Map,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runService(final IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		if (instance instanceof ISerialNumbered) {
			// Retrieve the emulator port from its serial number
			Pattern pattern = Pattern.compile("emulator-([0-9]+)");
			final String[] serialNumber = new String[1];

			serialNumber[0] = ((ISerialNumbered) instance).getSerialNumber();
			if (serialNumber[0] == null) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					@Override
					public void run() {
						ProgressMonitorDialog dialog = new ProgressMonitorDialog(new Shell(PlatformUI.getWorkbench()
								.getDisplay()));
						try {
							dialog.run(false, false, new IRunnableWithProgress() {

								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException,
										InterruptedException {
									int limit = 20;

									SubMonitor theMonitor = SubMonitor.convert(monitor);
									theMonitor.beginTask(ServicesNLS.ADBShellHandler_WaitingDeviceToLoad, limit);

									int times = 0;

									while ((serialNumber[0] == null) && (times < limit)) {
										theMonitor.worked(1);
										Thread.sleep(500);
										serialNumber[0] = ((ISerialNumbered) instance).getSerialNumber();
										times++;
									}

									theMonitor.setWorkRemaining(0);
								}
							});
						} catch (Exception e) {
							// do nothing
						}
					}
				});
			}

			// Fix a condition that Studio holds the UI thread forever
			if (serialNumber[0] == null) {
				status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
						ServicesNLS.ERR_EmulatorConsoleHandler_CouldNotOpenTheConsoleShell);
				return status;
			}

			Matcher matcher = pattern.matcher(serialNumber[0]);
			if (matcher.matches()) {
				String port = matcher.group(1);
				final TelnetFrameworkAndroid telnet = new TelnetFrameworkAndroid();
				try {
					Integer i = consolesCache.get(serialNumber[0]);
					i = (i == null ? 1 : ++i);
					consolesCache.put(serialNumber[0], i);

					telnet.connect(LOCALHOST, Integer.parseInt(port));
					InputStream in = telnet.getInputStream();
					OutputStream out = telnet.getOutputStream();

					String consoleName = CONSOLE_NAME + " - " + serialNumber[0];

					if (i != null) {
						consoleName += " (" + i + ")";
					}

					telnetsCache.put(consoleName, telnet);
					DeviceServicesPlugin.addConsoleKilledListener(listener);
					DeviceServicesPlugin.redirectStreamsToConsole(in, out, consoleName);
				} catch (IOException e) {
					status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
							ServicesNLS.ERR_EmulatorConsoleHandler_CouldNotOpenTheConsoleShell, e);
				}
			}
		} else {
			status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
					ServicesNLS.ERR_EmulatorConsoleHandler_CouldNotRetrieveTheEmulatorPort);
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * updatingService(org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}
}
