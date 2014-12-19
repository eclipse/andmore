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
package org.eclipse.andmore.android.emulator.logic;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.nativeos.IDevicePropertiesOSConstants;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IViewPart;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class StartEmulatorProcessLogic implements IAndroidLogic {
	/**
     * 
     */
	private static final String EMULATOR_NO_SNAPSHOT_LOAD = "-no-snapshot-load";

	/**
     * 
     */
	private static final String EMULATOR_NO_SNAPSHOT_SAVE = "-no-snapshot-save";

	/**
     * 
     */
	private static final String EMULATOR_HTTP_PROXY_PARAMETER = "-http-proxy";

	// Proxy constants
	private static final String PROXY_AT = "@";

	private static final String PROXY_COLON = ":";

	private static final String PROXY_HTTP = "http://";

	private static final String EMULATOR_VIEW = "org.eclipse.andmore.android.emulator.androidView";

	// Strings used to build the command line for launching the emulator process
	private static final String ARM_EMULATOR_RELATIVE_PATH = "/tools/emulator-arm";

	private static final String x86_EMULATOR_RELATIVE_PATH = "/tools/emulator-x86";

	private static final String EMULATOR_RELATIVE_PATH = "/tools/emulator";

	private static final String EMULATOR_VM_PARAMETER = "-avd";

	private static String selectedEmulatorPath = "";

	@Override
	public void execute(final IAndroidLogicInstance instance, int timeout, IProgressMonitor monitor)
			throws InstanceStartException, StartTimeoutException, StartCancelledException {

		long timeoutLimit = AndroidLogicUtils.getTimeoutLimit(timeout);

		info("Starting the Android Emulator process: " + instance);
		instance.setWindowHandle(0);

		File userData = instance.getUserdata();

		if (userData != null) {
			File userdataDir = userData.getParentFile();
			if ((userdataDir != null) && (!userdataDir.exists())) {
				userdataDir.mkdirs();
			}
		}

		selectedEmulatorPath = retrieveEmulatorExecutableName(instance);

		File emulatorExe = new File(SdkUtils.getSdkPath(), selectedEmulatorPath);

		List<String> cmdList = new LinkedList<String>();

		cmdList.add(emulatorExe.getAbsolutePath());
		cmdList.add(EMULATOR_VM_PARAMETER);
		cmdList.add(instance.getName());

		Properties propArgs = instance.getCommandLineArgumentsAsProperties();
		IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
		String adtEmuOptions = store.getString(AdtPrefs.PREFS_EMU_OPTIONS);

		StringTokenizer adtOptionsTokenizer = new StringTokenizer(adtEmuOptions, " ");
		while (adtOptionsTokenizer.hasMoreTokens()) {
			String nextToken = adtOptionsTokenizer.nextToken();
			cmdList.add(nextToken);
		}

		for (Object key : propArgs.keySet()) {
			String value = propArgs.getProperty(key.toString());

			if (key.equals("other")) {
				StringTokenizer stringTokenizer = new StringTokenizer(value, " ");
				while (stringTokenizer.hasMoreTokens()) {
					cmdList.add(stringTokenizer.nextToken());
				}
			} else {
				if ((value.trim().length() > 0) && !value.equals(Boolean.TRUE.toString())) {
					cmdList.add(key.toString());
					if (Platform.getOS().equals(Platform.OS_MACOSX)) {
						if (value.contains(" ")) {
							value = "\"" + value + "\"";
						}
					} else {
						if (value.contains("\\")) {
							value = value.replace("\\", "\\\\");
						}

						if (value.contains(" ")) {
							value = value.replace(" ", "\\ ");
						}
					}

					cmdList.add(value);
				} else if ((value.trim().length() > 0) && value.equals(Boolean.TRUE.toString())) {
					cmdList.add(key.toString());
				}
			}
		}

		// add proxy in case it is needed
		Properties properties = instance.getProperties();
		if (properties != null) {
			String useProxy = properties.getProperty(IDevicePropertiesConstants.useProxy,
					IDevicePropertiesConstants.defaultUseProxyValue);
			if (Boolean.TRUE.toString().equals(useProxy)) {
				addEmulatorProxyParameter(cmdList);
			}
		}

		StringBuffer cmdLog = new StringBuffer("");

		boolean httpProxyParamFound = false;
		boolean logHttpProxyUsage = false;
		for (String param : cmdList) {
			// Do not log -http-proxy information
			if (!httpProxyParamFound) {
				if (!param.equals(EMULATOR_HTTP_PROXY_PARAMETER)) {
					if (param.startsWith(emulatorExe.getAbsolutePath())) {
						// do not log emulator full path
						cmdLog.append(selectedEmulatorPath + " ");
					} else {
						cmdLog.append(param + " ");
					}
				} else {
					httpProxyParamFound = true;
					logHttpProxyUsage = true;
				}
			} else {
				httpProxyParamFound = false;
			}
		}

		// Append http proxy usage to log
		if (logHttpProxyUsage) {
			cmdLog.append("\nProxy settings are being used by the started emulator (-http-proxy parameter).");
		}
		// add command to not start from snapshot
		if (properties != null) {
			String startFromSnapshot = properties.getProperty(IDevicePropertiesConstants.startFromSnapshot,
					IDevicePropertiesConstants.defaultstartFromSnapshotValue);
			if (Boolean.FALSE.toString().equals(startFromSnapshot)) {
				cmdList.add(EMULATOR_NO_SNAPSHOT_LOAD);
			}
		}

		// Add the command to not save snapshot
		if (properties != null) {
			String saveSnapshot = properties.getProperty(IDevicePropertiesConstants.saveSnapshot,
					IDevicePropertiesConstants.defaulSaveSnapshot);
			if (Boolean.FALSE.toString().equals(saveSnapshot)) {
				cmdList.add(EMULATOR_NO_SNAPSHOT_SAVE);
			}
		}

		Process p;
		try {
			p = AndroidLogicUtils.executeProcess(cmdList.toArray(new String[0]), cmdLog.toString());
		} catch (AndroidException e) {
			throw new InstanceStartException(e);
		}
		info("Wait until and emulator with the VM " + instance.getName() + " is up ");

		AndroidLogicUtils.testProcessStatus(p);
		instance.setProcess(p);
		instance.setComposite(null);

		final String avdName = instance.getName();

		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			Collection<IViewPart> openedAndroidViews = EclipseUtils.getAllOpenedViewsWithId(EMULATOR_VIEW);

			if (!openedAndroidViews.isEmpty()) {
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						long windowHandle = -1;
						long timeOutToFindWindow = System.currentTimeMillis() + 30000;

						do {
							try {
								Thread.sleep(250);
							} catch (InterruptedException e) {
								// do nothing
							}

							try {
								AndroidLogicUtils.testTimeout(timeOutToFindWindow, "");
							} catch (StartTimeoutException e) {
								debug("Emulator window could not be found, instance :" + avdName);
								break;
							}

							try {
								int port = AndroidLogicUtils.getEmulatorPort(DDMSFacade.getSerialNumberByName(instance
										.getName()));
								if (port > 0) {
									windowHandle = NativeUIUtils.getWindowHandle(instance.getName(), port);
								}

							} catch (Exception t) {
								t.getCause().getMessage();
								System.out.println(t.getCause().getMessage());
							}
						} while (windowHandle <= 0);

						if (windowHandle > 0) {
							instance.setWindowHandle(windowHandle);
							NativeUIUtils.hideWindow(windowHandle);
						}
					}
				};
				Thread getHandleThread = new Thread(runnable, "Window Handle Thread");
				getHandleThread.start();
			}
		}

		if (instance.getProperties().getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
				.equals(Boolean.TRUE.toString())) {
			do {
				try {
					Thread.sleep(450);
				} catch (InterruptedException e) {
					// do nothing
				}

				AndroidLogicUtils.testCanceled(monitor);
				try {
					AndroidLogicUtils
							.testTimeout(timeoutLimit, NLS.bind(EmulatorNLS.EXC_TimeoutWhileStarting, avdName));
				} catch (StartTimeoutException e) {
					debug("Emulator start timeout has been reached, instance :" + avdName + " has device: "
							+ instance.hasDevice() + "isOnline? "
							+ DDMSFacade.isDeviceOnline(DDMSFacade.getSerialNumberByName(avdName)));
					throw e;
				}
			} while (!isEmulatorReady(avdName));

		}

		Thread t = new Thread("Process Error") {
			@Override
			public void run() {
				boolean shouldTryAgain = true;
				for (int i = 0; (i < 90) && shouldTryAgain; i++) {
					try {
						sleep(500);
						Process p = instance.getProcess();
						if (p != null) {
							AndroidLogicUtils.testProcessStatus(p);
						}
					} catch (Exception e) {
						StudioLogger.info(StartEmulatorProcessLogic.class,
								"Trying to stop the emulator process: execution stopped too early");
						DialogWithToggleUtils.showError(EmulatorPlugin.EMULATOR_UNEXPECTEDLY_STOPPED,
								EmulatorNLS.GEN_Error,
								NLS.bind(EmulatorNLS.ERR_AndroidLogicPlugin_EmulatorStopped, instance.getName()));
						shouldTryAgain = false;
						try {
							instance.stop(true);
						} catch (InstanceStopException ise) {
							StudioLogger.error(StartEmulatorProcessLogic.class,
									"Error trying to stop instance on process error", ise);
						}
					}
				}
			}
		};
		t.start();

		debug("Emulator instance is now up and running... " + instance);
	}

	/**
	 * retrives the emulator executable name according to abi type property
	 * 
	 * @param instance
	 * @return
	 */
	private static String retrieveEmulatorExecutableName(IAndroidLogicInstance instance) {
		String emulatorPath = null;

		Properties prop = instance.getProperties();
		String abiType = prop.getProperty("Abi_Type");

		if ((abiType == null) || (abiType.equals(""))) {
			emulatorPath = EMULATOR_RELATIVE_PATH;
		} else if (abiType.toLowerCase().contains("arm")) {
			emulatorPath = ARM_EMULATOR_RELATIVE_PATH;
		} else {
			emulatorPath = x86_EMULATOR_RELATIVE_PATH;
		}

		File emulatorExe = new File(SdkUtils.getSdkPath(), emulatorPath + ".exe");

		if (!emulatorExe.exists()) {
			emulatorPath = EMULATOR_RELATIVE_PATH;
		}

		return emulatorPath;
	}

	/**
	 * Retrieve the Proxy service.
	 * 
	 * @return IProxyService instance.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private IProxyService retrieveProxyService() {
		IProxyService proxyService = null;

		ServiceReference service = EmulatorPlugin.getDefault().getBundle().getBundleContext()
				.getServiceReference(IProxyService.class.getCanonicalName());
		if (service != null) {
			proxyService = (IProxyService) EmulatorPlugin.getDefault().getBundle().getBundleContext()
					.getService(service);
		}

		return proxyService;
	}

	/**
	 * Add the http-proxy parameter to the emulator command line.
	 * 
	 * @param cmdList
	 *            List holding the commands to be called.
	 */
	private void addEmulatorProxyParameter(List<String> cmdList) {
		IProxyService proxyService = retrieveProxyService();
		if (proxyService != null) {
			String host = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE).getHost();
			int port = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE).getPort();
			boolean isAuthenticationRequired = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE)
					.isRequiresAuthentication();
			String userId = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE).getUserId();
			String password = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE).getPassword();

			// there must be a host in order to access via proxy
			if (host != null) {
				cmdList.add(EMULATOR_HTTP_PROXY_PARAMETER);

				// add proxy info to the command list - authentication needed
				if (isAuthenticationRequired) {
					cmdList.add(PROXY_HTTP + userId + PROXY_COLON + password + PROXY_AT + host + PROXY_COLON
							+ Integer.valueOf(port).toString());
				}
				// add proxy info to the command list - no authentication needed
				else {
					cmdList.add(PROXY_HTTP + host + PROXY_COLON + Integer.valueOf(port).toString());
				}
			}
		}
	}

	private boolean isEmulatorReady(String avdName) {
		String serialNum = DDMSFacade.getSerialNumberByName(avdName);
		return DDMSFacade.isDeviceOnline(serialNum);
	}
}