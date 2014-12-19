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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin.IConsoleKilledListener;
import org.eclipse.andmore.android.devices.services.i18n.ServicesNLS;
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
 * Class responsible to implement the handler for the service "ADB Shell"
 */
public class ADBShellHandler extends ServiceHandler {
	public static final String CONSOLE_NAME = "ADB Shell"; //$NON-NLS-1$

	private static final String SERIAL_PARAMETER = "-s"; //$NON-NLS-1$

	private static final String SHELL_COMMAND = "shell"; //$NON-NLS-1$

	private static final Map<String, Integer> consolesCache = new HashMap<String, Integer>();

	private static final Map<String, Process> consolesProcesses = new HashMap<String, Process>();

	public ADBShellHandler() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new ADBShellHandler();
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
	public IStatus runService(IInstance theInstance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		List<String> command = new LinkedList<String>();
		final IInstance instance = theInstance;

		File sdkPath = new File(SdkUtils.getSdkPath());
		String adbPath = SdkUtils.getAdbPath();
		File adb = new File(adbPath);

		if ((sdkPath != null) && sdkPath.exists() && sdkPath.isDirectory()) {
			if (adb.exists() && adb.isFile()) {
				if (instance instanceof ISerialNumbered) {
					final String[] serialNumber = new String[1];

					serialNumber[0] = ((ISerialNumbered) instance).getSerialNumber();

					if (serialNumber[0] == null) {
						PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

							@Override
							public void run() {
								ProgressMonitorDialog dialog = new ProgressMonitorDialog(new Shell(PlatformUI
										.getWorkbench().getDisplay()));
								try {
									dialog.run(false, false, new IRunnableWithProgress() {

										@Override
										public void run(IProgressMonitor monitor) throws InvocationTargetException,
												InterruptedException {
											int limit = 20;

											SubMonitor theMonitor = SubMonitor.convert(monitor);
											theMonitor
													.beginTask(ServicesNLS.ADBShellHandler_WaitingDeviceToLoad, limit);

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
								ServicesNLS.ERR_ADBShellHandler_CouldNotExecuteTheAdbShell);
						return status;
					}

					if (adbPath.contains(" ")) //$NON-NLS-1$
					{
						if (DeviceServicesPlugin.IS_WIN32) {
							adbPath = "\"" + adbPath + "\""; //$NON-NLS-1$ //$NON-NLS-2$
						} else {
							adbPath = adbPath.replace(" ", "\\ "); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}

					command.add(adbPath);
					command.add(SERIAL_PARAMETER);
					command.add(serialNumber[0]);
					command.add(SHELL_COMMAND);

					try {
						Integer i = consolesCache.get(serialNumber[0]);
						i = (i == null ? 1 : ++i);
						consolesCache.put(serialNumber[0], i);

						String[] cmdArray = command.toArray(new String[4]);
						StringBuffer sb = new StringBuffer();
						for (String cmd : cmdArray) {
							sb.append(cmd);
							sb.append(" "); //$NON-NLS-1$
						}

						Process p = Runtime.getRuntime().exec(cmdArray);

						String consoleName = CONSOLE_NAME + " - " + serialNumber[0]; //$NON-NLS-1$

						if (i != null) {
							consoleName += " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						}
						consolesProcesses.put(consoleName, p);
						DeviceServicesPlugin.redirectProcessStreamsToConsole(p, consoleName);
						DeviceServicesPlugin.addConsoleKilledListener(listener);
					} catch (IOException e) {
						status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
								ServicesNLS.ERR_ADBShellHandler_CouldNotExecuteTheAdbShell, e);
					}
				}

			} else {
				status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
						ServicesNLS.ERR_ADBShellHandler_MissingAdbShell);
			}
		} else {
			status = new Status(IStatus.ERROR, DeviceServicesPlugin.PLUGIN_ID,
					ServicesNLS.ERR_ADBShellHandler_AndroidSdkIsNotConfigured);
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
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}

	private final IConsoleKilledListener listener = new IConsoleKilledListener() {
		@Override
		public void consoleKilled(String name) {
			if (consolesProcesses.containsKey(name)) {
				Process p = consolesProcesses.get(name);
				p.destroy();
				DeviceServicesPlugin.removeConsoleKilledListener(listener);
				consolesProcesses.remove(name);
			}
		}
	};

}
