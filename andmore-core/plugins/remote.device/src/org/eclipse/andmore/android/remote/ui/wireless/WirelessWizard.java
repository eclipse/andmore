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

package org.eclipse.andmore.android.remote.ui.wireless;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.ui.wireless.runnables.SwitchFromUSBAndConnectToWirelessRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;

/**
 * Switch to Wireless Connection Mode.
 */
public class WirelessWizard extends Wizard {
	// Wizard icon
	private final String WIRELESS_WIZARD_IMAGE_PATH = "icons/wireless_wizard-icon-64x64.png"; //$NON-NLS-1$

	WirelessDeviceWizardPage informationPage;

	private ISerialNumbered instance;

	private String host;

	private IProgressMonitor monitor;

	/**
	 * Default constructor.
	 */
	public WirelessWizard() {
		super.setDefaultPageImageDescriptor(RemoteDevicePlugin.getImageDescriptor(WIRELESS_WIZARD_IMAGE_PATH));
		this.setWindowTitle(RemoteDeviceNLS.UI_WirelessWizard_Name);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		informationPage = new WirelessDeviceWizardPage();
		addPage(informationPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		boolean isProcessOK = true;
		try {
			// execute connection switch and show success message in case
			// everything went fine
			getContainer().run(true, true, new SwitchFromUSBAndConnectToWirelessRunnable(this));
			EclipseUtils.showInformationDialog(RemoteDeviceNLS.WirelessWizard_TitleWirelessConnectionModeWizard,
					RemoteDeviceNLS.WirelessWizard_WirelessDeviceCreatedSuccessfully);
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_REMOTE_WIRELESS,
					UsageDataConstants.KIND_REMOTE_DEVICE, UsageDataConstants.DESCRIPTION_DEFAULT,
					RemoteDevicePlugin.PLUGIN_ID, RemoteDevicePlugin.getDefault().getBundle().getVersion().toString());
		} catch (InvocationTargetException ite) {
			// treat case where something went wrong - log, show an error
			// message and set the wizard flag
			StudioLogger.error(this.getClass(), "Problems switching device to TCP/IP.", ite); //$NON-NLS-1$
			IStatus status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, ite.getTargetException() != null ? ite
					.getTargetException().getMessage() : ite.getMessage());
			EclipseUtils.showErrorDialog(RemoteDeviceNLS.WirelessWizard_TitleWirelessConnectionModeWizard,
					RemoteDeviceNLS.WirelessWizard_MsgErrorProblemsSwitchingDeviceToTCPIP, status);
			isProcessOK = false;
		} catch (InterruptedException ie) {
			// action was canceled by the user, therefore do not close the
			// wizard
			isProcessOK = false;
		}

		return isProcessOK;
	}

	public void setInstance(ISerialNumbered instance) {
		this.instance = instance;
	}

	public ISerialNumbered getInstance() {
		return instance;
	}

	/**
	 * @return the monitor
	 */
	public IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	/**
	 * @param monitor
	 *            the monitor to set
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	/**
	 * Get the {@link Properties} associated with this {@link Wizard}.
	 * 
	 * @return Return the related {@link Properties}.
	 */
	public Properties getProperties() {
		return informationPage != null ? informationPage.getProperties() : null;
	}

	/**
	 * @param host
	 */
	public void setIp(String host) {
		this.host = host;
	}

	/**
	 * @param host
	 */
	public String getIp() {
		return host;
	}

	public String getDeviceName() {
		return informationPage.getDeviceName();
	}
}
