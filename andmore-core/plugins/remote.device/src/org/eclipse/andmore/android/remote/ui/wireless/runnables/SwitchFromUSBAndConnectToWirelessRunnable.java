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
package org.eclipse.andmore.android.remote.ui.wireless.runnables;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.andmore.android.remote.ui.wireless.WirelessWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;

/**
 * Service which switches the device to TCP/IP, add it as a remote device to the
 * Device Management and connect to it via the wireless network.
 */
public class SwitchFromUSBAndConnectToWirelessRunnable implements IRunnableWithProgress {

	private final WirelessWizard wirelessWizard;

	/**
	 * Constructor which passes the {@link Wizard} page.
	 * 
	 * @param wirelessWizard
	 *            Wizard paged
	 */
	public SwitchFromUSBAndConnectToWirelessRunnable(WirelessWizard wirelessWizard) {
		this.wirelessWizard = wirelessWizard;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		boolean isInstanceCreated = false;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);
		subMonitor.beginTask(RemoteDeviceNLS.SwitchFromUSBAndConnectToWirelessRunnable_MsgCreatingWirelessRemoteDevice,
				1000);

		RemoteDeviceInstance remoteDeviceInstance = null;
		IStatus status = Status.OK_STATUS;

		// get connection timeout
		int connectionTimeout = Integer.valueOf(this.wirelessWizard.getProperties().getProperty(
				RemoteDeviceInstance.PROPERTY_TIMEOUT));

		try {
			subMonitor.worked(100);

			if (!subMonitor.isCanceled()) {
				// switch device connection from USB to TCP/IP
				try {
					status = DDMSFacade.switchUSBtoTcpIp(this.wirelessWizard.getInstance().getDeviceName(),
							this.wirelessWizard.getInstance().getSerialNumber(), this.wirelessWizard.getProperties()
									.getProperty(RemoteDeviceInstance.PROPERTY_PORT), connectionTimeout, subMonitor
									.newChild(300));
				} catch (IOException se) {
					// log error, adjust the status and throw the exception
					status = handleStatusAndLogDuringException(se);
					throw new InvocationTargetException(se, se.getMessage());
				}
			}

			remoteDeviceInstance = null;
			if (!(status.getSeverity() == IStatus.ERROR) && !subMonitor.isCanceled()) {
				subMonitor
						.setTaskName(RemoteDeviceNLS.SwitchFromUSBAndConnectToWirelessRunnable_CreatingRemoteDeviceInstance);

				// verify if there already is an instance created with the same
				// name
				List<IInstance> instanceByName = InstanceRegistry.getInstance().getInstancesByName(
						this.wirelessWizard.getDeviceName());

				if ((instanceByName != null) && (!instanceByName.isEmpty())) {
					remoteDeviceInstance = (RemoteDeviceInstance) instanceByName.get(0);
				} else {
					// create the new remote device instance
					try {
						remoteDeviceInstance = (RemoteDeviceInstance) InstanceManager.createInstance(
								this.wirelessWizard.getDeviceName(),
								"org.eclipse.andmore.android.remote.androidRemoteDevice", //$NON-NLS-1$
								DevicePlugin.SEQUOYAH_STATUS_OFF, this.wirelessWizard.getProperties());
					} catch (SequoyahException se) {
						// log error, adjust the status and throw the exception
						status = handleStatusAndLogDuringException(se);
						throw new InvocationTargetException(se, se.getMessage());
					}

					if (!subMonitor.isCanceled()) {
						// add instance to the DDMS and set the flag
						InstanceRegistry.getInstance().addInstance(remoteDeviceInstance);
						isInstanceCreated = true;
					}
				}
				subMonitor.worked(300);
			}

			if (!subMonitor.isCanceled()) {
				subMonitor
						.setTaskName(RemoteDeviceNLS.SwitchFromUSBAndConnectToWirelessRunnable_ConnectingToWifiDevice);

				int timeoutAux = (connectionTimeout > 60) ? connectionTimeout : 60;

				long timeoutLimit = System.currentTimeMillis() + (timeoutAux * 1000);

				// after the adb mode is switched to tcpip the handset takes a
				// while to
				// be available for connection, that is why this while exists
				while ((!DDMSFacade.isDeviceOnline(remoteDeviceInstance.getSerialNumber()))
						&& (System.currentTimeMillis() < timeoutLimit)) {
					// connect the remote device via TCP/IP
					try {
						status = DDMSFacade.connectTcpIp(remoteDeviceInstance, this.wirelessWizard.getProperties()
								.getProperty(RemoteDeviceInstance.PROPERTY_HOST), this.wirelessWizard.getProperties()
								.getProperty(RemoteDeviceInstance.PROPERTY_PORT), connectionTimeout, subMonitor
								.newChild(300));
					} catch (IOException ioe) {
						status = handleStatusAndLogDuringException(ioe);
						throw new InvocationTargetException(ioe, ioe.getMessage());
					}
				}
			}
			// in case the status has errors, throw InvocationTargetException
			if ((status != null) && (status.getSeverity() == IStatus.ERROR) && (!subMonitor.isCanceled())) {
				if (status.getException() != null) {
					throw new InvocationTargetException(status.getException());
				} else {
					throw new InvocationTargetException(
							new Exception(
									RemoteDeviceNLS.SwitchFromUSBAndConnectToWirelessRunnable_MsgNotPossibleToConvertUSBToTCPIP));
				}
			}

			// treat the case where the monitor is canceled - throw
			// InterruptedException as stated in this method
			if (monitor.isCanceled()) {
				throw new InterruptedException("The switching to TCP/IP connection mode was canceled by the user.");
			}
		} finally {
			// remove the device in case it exists and was added to the Device
			// Management View
			if ((remoteDeviceInstance != null) && (isInstanceCreated)
					&& ((status.getSeverity() == IStatus.ERROR) || (subMonitor.isCanceled()))) {
				InstanceRegistry.getInstance().removeInstance(remoteDeviceInstance);
			}
		}
	}

	/*
	 * Log the exception and get the error status.
	 * 
	 * @param exception The exception to be treated.
	 * 
	 * @return Returns the Error status.
	 */
	private IStatus handleStatusAndLogDuringException(Exception exception) {
		StudioLogger.error(this.getClass(), exception.getMessage(), exception);
		return new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, exception.getMessage());
	}
}