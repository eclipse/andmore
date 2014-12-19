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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.ui.PlatformUI;

/**
 * Class that contains business methods and utilities.
 */
public class RemoteDeviceUtils {

	/**
	 * Handle Remote Device connection.
	 * 
	 * @param serialNumber
	 *            the serial number of the connected device
	 */
	public static void connectDevice(final String serialNumber) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				/*
				 * Check if it's a remote device
				 */
				if (DDMSFacade.isRemote(serialNumber)) {

					ISerialNumbered instance = DevicesManager.getInstance().getDeviceBySerialNumber(serialNumber);

					boolean isTransitioning = ((instance != null) ? ((AbstractMobileInstance) instance)
							.getStateMachineHandler().isTransitioning() : false);

					StudioLogger.debug("Handle remote device connected event. Serial Number: " + serialNumber
							+ " Instance: " + instance + " Transitioning: " + isTransitioning);

					/*
					 * If the instance exists and is transitioning, so skip this
					 * method, the connect handler will change the instance
					 * status
					 */
					if ((instance == null) || ((instance != null) && (!isTransitioning))) {

						/*
						 * This method is necessary because sometimes (for
						 * example when the connection is refuses) the device
						 * appears in the adb devices list but it's not in the
						 * "online" state
						 */
						boolean onlineDevice = waitForDeviceToBeOnline(serialNumber, instance);

						if (onlineDevice) {
							/*
							 * If the device instance already exists
							 */
							if (instance == null) {

								try {

									StudioLogger
											.debug("Connecting Remote Device: device doesn't exist, create a new instance");

									DevicesManager.getInstance().createInstanceForDevice(serialNumber,
											RemoteDeviceConstants.DEVICE_ID, getInstanceBuilder(serialNumber),
											RemoteDeviceConstants.SERVICE_INIT_ID);
								} catch (SequoyahException e) {
									StudioLogger
											.error("Connecting Remote Device: error while creating device instance "
													+ e.getMessage());
								}
							}

							try {
								instance = DevicesManager.getInstance().getDeviceBySerialNumber(serialNumber);

								StudioLogger.debug("Connecting Remote Device: the TmL service will be called");

								Map<Object, Object> arguments = new HashMap<Object, Object>();
								arguments.put(RemoteDeviceConstants.DUMMY_TRANSITION, true);
								RemoteDevicePlugin.getConnectServiceHandler().run((IInstance) instance, arguments);
							} catch (Exception e) {
								StudioLogger.error("Error when running TmL connect service: " + e.getMessage());
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Handle Remote Device disconnection
	 * 
	 * @param serialNumber
	 *            the serial number of the disconnected device
	 */
	public static void disconnectDevice(String serialNumber) {
		if (DDMSFacade.isRemote(serialNumber)) {

			ISerialNumbered instance = DevicesManager.getInstance().getDeviceBySerialNumber(serialNumber);

			StudioLogger.debug("Handle remote device disconnected event. Serial Number: " + serialNumber
					+ " Instance: " + instance);

			if (instance != null) {
				Object volatileProperty = ((RemoteDeviceInstance) instance).getProperties().get(
						RemoteDeviceInstance.PROPERTY_VOLATILE);
				boolean isVolatile = ((volatileProperty != null) ? ((Boolean) volatileProperty).booleanValue() : false);

				if (!isVolatile) {
					try {
						StudioLogger
								.debug("Disconnecting Remote Device: the device is NOT volatile, the TmL service will be called");

						Map<Object, Object> arguments = new HashMap<Object, Object>();
						arguments.put(RemoteDeviceConstants.DUMMY_TRANSITION, true);
						RemoteDevicePlugin.getDisconnectServiceHandler().run((IInstance) instance, arguments);
					} catch (Exception e) {
						StudioLogger.error("Error when running TmL disconnect service: " + e.getMessage());
					}
				} else {
					StudioLogger.debug("Disconnecting Remote Device: the device is volatile, it will be deleted");
					DevicesManager.getInstance().deleteInstanceOfDevice(serialNumber);
				}

			}

		}

	}

	/*
	 * Wait until the device status becomes online
	 * 
	 * @param serialNumber device serial number
	 * 
	 * @param instance TmL instance, if it exists
	 * 
	 * @return true if the device became online, false otherwise
	 */
	private static boolean waitForDeviceToBeOnline(String serialNumber, ISerialNumbered instance) {
		StudioLogger.debug("Wait device to be online: " + serialNumber);

		boolean instanceOnline = false;
		long timeoutLimit = 0;

		if (instance != null) {
			Properties prop = ((IInstance) instance).getProperties();
			String timeout = prop.getProperty(RemoteDeviceInstance.PROPERTY_TIMEOUT);
			timeoutLimit = System.currentTimeMillis() + (Integer.parseInt(timeout) * 1000);
		} else {
			timeoutLimit = System.currentTimeMillis() + (RemoteDeviceConstants.DEFAULT_TIMEOUT * 1000);

		}

		while ((instanceOnline = DDMSFacade.isDeviceOnline(serialNumber)) == false) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				StudioLogger.error("Wait for device to be online: thread has been interrupted");
			}

			try {
				testTimeout(timeoutLimit);
			} catch (TimeoutException e) {
				StudioLogger.warn("Timeout reached wile wating device to be online: " + serialNumber);
				break;
			}

		}
		return instanceOnline;

	}

	/*
	 * Get the instance builder needed by TmL in order to create a new Remote
	 * Device instance
	 * 
	 * @param serialNumber serial number of the Remote Device that shall be
	 * added
	 * 
	 * @return the instance builder needed by TmL to create a new Remote Device
	 * instance
	 */
	private static RemoteDeviceInstanceBuilder getInstanceBuilder(String serialNumber) {

		RemoteDeviceInstanceBuilder instanceBuilder = null;

		String[] serialNumberParts = serialNumber.split(":");
		String host = serialNumberParts[0];
		String port = serialNumberParts[1];

		Properties props = new Properties();
		props.put(RemoteDeviceInstance.PROPERTY_HOST, host);
		props.put(RemoteDeviceInstance.PROPERTY_PORT, port);
		props.put(RemoteDeviceInstance.PROPERTY_TIMEOUT, String.valueOf(RemoteDeviceConstants.DEFAULT_TIMEOUT));

		// mark this instance as volatile
		props.put(RemoteDeviceInstance.PROPERTY_VOLATILE, true);

		instanceBuilder = new RemoteDeviceInstanceBuilder(serialNumber, props);

		return instanceBuilder;
	}

	/*
	 * Compare the device instance with a pair host:port to check if the device
	 * has the same host:port
	 * 
	 * @param device the device to be analyzed
	 * 
	 * @param host host IP or name
	 * 
	 * @param port port number
	 * 
	 * @return true if the the device has the same host:port, false otherwise
	 */
	public static boolean hasSameHostAndPort(ISerialNumbered device, String host, int port) {
		boolean returnValue = false;

		String deviceHost = ((RemoteDeviceInstance) device).getProperties().getProperty(
				RemoteDeviceInstance.PROPERTY_HOST);
		String devicePort = ((RemoteDeviceInstance) device).getProperties().getProperty(
				RemoteDeviceInstance.PROPERTY_PORT);

		if ((host.equals(deviceHost)) && (String.valueOf(port).equals(devicePort))) {
			returnValue = true;
		}

		return returnValue;

	}

	/**
	 * Execute a command.
	 * 
	 * @param cmd
	 *            Array of strings holding the command to be executed.
	 * 
	 * @return The {@link IStatus} of the command execution.
	 * 
	 * @throws IOException
	 *             Exception thrown in case there are problems executing the
	 *             command.
	 */
	public static IStatus executeCommand(String[] cmd) throws IOException {
		IStatus status = Status.OK_STATUS;

		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(cmd);

		try {
			// wait for the command to finish its execution
			process.waitFor();
		} catch (InterruptedException e) {
			StudioLogger.error(RemoteDeviceUtils.class, "Problems executing the command");
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, "Problems executing the command", e);
		}
		// in case the is a problem with the command execution, create an error
		// status
		if (process.exitValue() != 0) {
			StudioLogger.error(RemoteDeviceUtils.class, "The IP was not found");
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, "The IP was not found");
		}

		return status;
	}

	/*
	 * Checks if the timeout limit has reached
	 * 
	 * @param timeoutLimit The system time limit that cannot be overtaken, in
	 * milliseconds
	 * 
	 * @throws StartTimeoutException When the system time limit is overtaken
	 */
	private static void testTimeout(long timeoutLimit) throws TimeoutException {
		if (System.currentTimeMillis() > timeoutLimit) {
			throw new TimeoutException();
		}
	}
}
