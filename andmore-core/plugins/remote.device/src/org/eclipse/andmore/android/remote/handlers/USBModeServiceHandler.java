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
package org.eclipse.andmore.android.remote.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * Handler which switches back a remote device from TCP/IP to USB mode.
 */
public class USBModeServiceHandler extends ServiceHandler {
	// Min SDK version that supports tcpip connection mode
	private static final int MIN_SDK_VERSION = 6;

	/**
	 * Get the wireless service handler.
	 */
	@Override
	public IServiceHandler newInstance() {
		return new USBModeServiceHandler();
	}

	/**
	 * Get the phone IP, validate it and launch the Wireless wizard.
	 */
	@Override
	public IStatus runService(final IInstance instance, Map<Object, Object> arguments, final IProgressMonitor monitor) {

		final SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);
		subMonitor.beginTask(RemoteDeviceNLS.USBModeServiceHandler_MsgStartingProcessOfSwitchingToUSBMode, 1000);

		final ISerialNumbered device = (ISerialNumbered) instance;

		int deviceSdkVersion = -1;
		try {
			deviceSdkVersion = Integer.parseInt(DDMSFacade.getDeviceProperty(device.getSerialNumber(),
					"ro.build.version.sdk")); //$NON-NLS-1$

			subMonitor.worked(100);
		} catch (Exception e) {
			StudioLogger.error(USBModeServiceHandler.class, RemoteDeviceNLS.USBModeServiceHandler_2, e);
		}

		// if it was not possible to retrieve the sdk version
		// try to execute the service anyway
		if ((!subMonitor.isCanceled()) && (deviceSdkVersion < MIN_SDK_VERSION) && (deviceSdkVersion != -1)) {
			EclipseUtils.showErrorDialog(RemoteDeviceNLS.Title_ReturningToUSBConnectionDialog,
					RemoteDeviceNLS.USBModeServiceHandler_MsgUnableToSwitchToUSBDueToSDKVersion);
		} else {

			if (!subMonitor.isCanceled()) {

				subMonitor.setTaskName(RemoteDeviceNLS.WirelessServiceHandler_MsgRetrievingDeviceIPNumber);

				// retrieve the IP, Port and timeout and validate it
				Properties properties = instance.getProperties();
				String host = properties.getProperty(RemoteDeviceInstance.PROPERTY_HOST);
				String port = properties.getProperty(RemoteDeviceInstance.PROPERTY_PORT);
				String timeout = properties.getProperty(RemoteDeviceInstance.PROPERTY_TIMEOUT);

				subMonitor.worked(600);
				if (host == null) {
					EclipseUtils.showErrorDialog(RemoteDeviceNLS.Title_ReturningToUSBConnectionDialog,
							RemoteDeviceNLS.ERR_WirelessWizard_No_IP);
				}

				else {
					if (!subMonitor.isCanceled()) {
						subMonitor.setTaskName(RemoteDeviceNLS.USBModeServiceHandler_MsgSwithcingTCPToUSB);
						// switch the device from connection mode from TCP/IP to
						// USB
						try {
							IStatus status = DDMSFacade.switchFromTCPConnectionModeToUSBConnectionMode(device, host,
									port, Integer.parseInt(timeout), subMonitor.newChild(300));

							// in case the status is not OK, show an error
							// message
							if ((status != null) && (status.getSeverity() == IStatus.ERROR)) {
								EclipseUtils
										.showErrorDialog(
												RemoteDeviceNLS.Title_ReturningToUSBConnectionDialog,
												NLS.bind(
														RemoteDeviceNLS.USBModeServiceHandler_MsgItWasNotPossibleToSwitchDeviceToUSBMode,
														host), status);
							} else {
								// show a success message
								EclipseUtils.showInformationDialog(
										RemoteDeviceNLS.Title_ReturningToUSBConnectionDialog,
										NLS.bind(RemoteDeviceNLS.USBModeServiceHandler_MsgSuccess, host));
							}
						} catch (IOException e) {
							StudioLogger.error(this.getClass(), NLS.bind(
									"It was not possible to switch the android device {0} connection mode to USB.", //$NON-NLS-1$
									device.getDeviceName()), e);
							EclipseUtils.showErrorDialog(RemoteDeviceNLS.Title_ReturningToUSBConnectionDialog, NLS
									.bind(RemoteDeviceNLS.USBModeServiceHandler_MsgItWasNotPossibleToSwitchToUSBMode,
											device.getDeviceName()));
						}
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * Simply Return an OK Status.
	 */
	@Override
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}

	@Override
	public void setService(IService service) {
		super.setService(service);
		if (service != null) {
			service.setVisible(RemoteDevicePlugin.isWifiServiceEnabled());
		}
	}
}
