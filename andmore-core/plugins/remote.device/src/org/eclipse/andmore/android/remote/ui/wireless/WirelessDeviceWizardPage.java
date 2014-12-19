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

import java.util.Properties;

import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.andmore.android.remote.ui.wireless.WirelessPropertiesComposite.WirelessPropertiesChangedListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Wizard Page to be used by TmL to create a new Wireless Device Remove Instance
 */
public class WirelessDeviceWizardPage extends WizardPage implements IInstanceProperties,
		WirelessPropertiesChangedListener {
	private WirelessPropertiesComposite composite;

	/**
	 * Creates a WirelessDeviceWizardPage object.
	 */
	public WirelessDeviceWizardPage() {
		super(RemoteDeviceNLS.UI_WirelessWizard_Name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties#
	 * getProperties()
	 */
	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.put(RemoteDeviceInstance.PROPERTY_HOST, composite.getHost());
		props.put(RemoteDeviceInstance.PROPERTY_PORT, Integer.toString(composite.getPort()));
		props.put(RemoteDeviceInstance.PROPERTY_TIMEOUT, Integer.toString(composite.getTimeout()));
		return props;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		setTitle(RemoteDeviceNLS.UI_WirelessInformationPage_Title);
		setMessage(RemoteDeviceNLS.UI_WirelessInformationPage_Description);

		composite = new WirelessPropertiesComposite(parent, ((WirelessWizard) getWizard()).getIp(),
				((WirelessWizard) getWizard()).getInstance());
		composite.addPropertyChangeListener(this);
		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				composite.removePropertyChangeListener(WirelessDeviceWizardPage.this);
				composite = null;
				WirelessDeviceWizardPage.this.setControl(null);
			}
		});
		setControl(composite);
		setStatusMessage();

		// adjust the wizard page size - one could also use:
		// this.getWizard().getContainer().getShell().computeSize(500, 500);
		this.getWizardShell().setSize(this.getWizardShell().computeSize(750, SWT.DEFAULT));
	}

	private Shell getWizardShell() {
		return this.getWizard().getContainer().getShell();
	}

	/*
	 * Set the {@link IStatus} message for this wizard.
	 */
	private void setStatusMessage() {
		IStatus status = composite.getStatus();
		switch (status.getSeverity()) {
		case IStatus.ERROR:
			setErrorMessage(status.getMessage());
			setMessage(null);
			break;
		case IStatus.WARNING:
			setErrorMessage(null);
			setMessage(status.getMessage(), IMessageProvider.WARNING);
			break;
		case IStatus.OK:
			setErrorMessage(null);
			setMessage(status.getMessage(), IMessageProvider.INFORMATION);
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return (composite != null) && (composite.getStatus() != null)
				&& (composite.getStatus().getSeverity() != IStatus.ERROR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.remote.ui.RemotePropertiesComposite.
	 * RemotePropertiesChangedListener#propertiesChanged()
	 */
	@Override
	public void propertiesChanged() {
		setStatusMessage();
		setPageComplete(isPageComplete());
	}

	/**
	 * Get the device name.
	 * 
	 * @return The device name.
	 */
	public String getDeviceName() {
		return composite != null ? composite.getDeviceName() : "";
	}
}
