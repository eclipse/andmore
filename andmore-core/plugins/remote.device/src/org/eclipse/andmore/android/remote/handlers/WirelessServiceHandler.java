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

import java.net.InetAddress;
import java.util.Map;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.ui.wireless.WirelessWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.ui.PlatformUI;

/**
 * Handler which launches the Over the Air wizard.
 */
public class WirelessServiceHandler extends ServiceHandler {
	private static final int TIMEOUT_REACH_IP = 30000;

	// Min SDK version that supports tcpip connection mode
	private static final int MIN_SDK_VERSION = 6;

	/**
	 * Get the wireless service handler.
	 */
	@Override
	public IServiceHandler newInstance() {
		return new WirelessServiceHandler();
	}

	/**
	 * Get the phone IP, validate it and launch the Wireless wizard.
	 */
	@Override
	public IStatus runService(final IInstance instance, Map<Object, Object> arguments, final IProgressMonitor monitor) {

		final SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);
		subMonitor.beginTask(RemoteDeviceNLS.WirelessServiceHandler_MsgLaunchingWirelessConnection, 1000);

		final ISerialNumbered device = (ISerialNumbered) instance;

		int deviceSdkVersion = -1;
		try {
			deviceSdkVersion = Integer.parseInt(DDMSFacade.getDeviceProperty(device.getSerialNumber(),
					"ro.build.version.sdk"));

			subMonitor.worked(100);
		} catch (Exception e) {
			StudioLogger.error(WirelessServiceHandler.class, "Problems trying to retrieve handset's sdk version.", e);
		}

		// if it was not possible to retrieve the sdk version
		// try to execute the service anyway
		if ((!subMonitor.isCanceled()) && (deviceSdkVersion < MIN_SDK_VERSION) && (deviceSdkVersion != -1)) {
			EclipseUtils.showErrorDialog(RemoteDeviceNLS.WirelessWizard_TitleWirelessConnectionModeWizard,
					RemoteDeviceNLS.ERR_WirelessWizard_NOT_VALID_SDK);
		} else {

			if (!subMonitor.isCanceled()) {

				subMonitor.setTaskName(RemoteDeviceNLS.WirelessServiceHandler_MsgRetrievingDeviceIPNumber);

				// retrieve the IP and validate it
				final String host = DDMSFacade.getWirelessIPfromHandset(device.getSerialNumber(), monitor);

				subMonitor.worked(300);
				if (host == null) {
					EclipseUtils.showErrorDialog(RemoteDeviceNLS.WirelessWizard_TitleWirelessConnectionModeWizard,
							RemoteDeviceNLS.ERR_WirelessWizard_No_IP);
				} else {
					if (!subMonitor.isCanceled()) {
						// check whether the IP can be reached
						subMonitor.setTaskName(RemoteDeviceNLS.WirelessServiceHandler_MsgPingingIPAddress);
						InetAddress ipAddress = null;
						boolean canReachIPAddress = true;
						if (!subMonitor.isCanceled()) {
							try {
								ipAddress = InetAddress.getByName(host);
								canReachIPAddress = (ipAddress != null) && ipAddress.isReachable(TIMEOUT_REACH_IP);
								subMonitor.worked(200);
							} catch (Exception e) {
								canReachIPAddress = false;
								StudioLogger.error(this.getClass(),
										NLS.bind(RemoteDeviceNLS.ERR_WirelessWizard_Reach_IP, host), e);
							}

							if (!canReachIPAddress) {
								EclipseUtils.showErrorDialog(
										RemoteDeviceNLS.WirelessWizard_TitleWirelessConnectionModeWizard,
										NLS.bind(RemoteDeviceNLS.ERR_WirelessWizard_Reach_IP, host));
							} else {
								if (!subMonitor.isCanceled()) {
									// launch the wireless wizard
									PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
										@Override
										public void run() {
											subMonitor.worked(400);
											WirelessWizard wizard = new WirelessWizard();
											wizard.setInstance(device);
											// wizard.setIp("192.168.16.2");
											wizard.setIp(host);
											wizard.setProgressMonitor(monitor);
											WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench()
													.getActiveWorkbenchWindow().getShell(), wizard);
											dialog.open();
										}
									});
								}

							}
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
