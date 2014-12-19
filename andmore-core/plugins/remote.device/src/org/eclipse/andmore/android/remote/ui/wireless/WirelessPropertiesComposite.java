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

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.remote.RemoteDeviceConstants;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.andmore.android.remote.RemoteDeviceUtils;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Composite containing the Android Wireless Remote Device properties for
 * edition.
 */
public class WirelessPropertiesComposite extends Composite {
	/**
	 * Empty String
	 */
	private static final String EMPTY_STRING = "";

	/**
	 * Default invalid integer
	 */
	private static final int DEFAULT_INVALID_INTEGER = -1;

	private String name;

	private final String host;

	private int port;

	private int timeout;

	// DEVICE - if it already exist
	ISerialNumbered device;

	private IProgressMonitor monitor;

	private final Collection<WirelessPropertiesChangedListener> listeners = new LinkedHashSet<WirelessPropertiesChangedListener>();

	// Error messages
	private static final String NAME_ERR_MESSAGE = RemoteDeviceNLS.ERR_WirelessDeviceWizardPage_Name;

	private static final String HOST_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_IP;

	private static final String PORT_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_Port;

	private static final String TIMEOUT_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_Timeout;

	private final Text nameText;

	private final Text portText;

	private final Text timeoutText;

	/*
	 * Listener used for handle Timeout text change.
	 */
	private final class TimeoutModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			String timeoutStr = timeoutText.getText();
			try {
				timeout = Integer.parseInt(timeoutStr);
			} catch (NumberFormatException e1) {
				timeout = DEFAULT_INVALID_INTEGER;
			} finally {
				notifyListeners();
			}
		}
	}

	/*
	 * Listener used for handle Port text change.
	 */
	private final class PortModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			String portStr = portText.getText();
			try {
				port = Integer.parseInt(portStr);
				// manage the case where the entered IP/Port matches of the an
				// existing device
				manageSameIPAndPortOfRemoteDevice();
			} catch (NumberFormatException e1) {
				port = DEFAULT_INVALID_INTEGER;
			} finally {
				notifyListeners();
			}
		}
	}

	/*
	 * Listener used for handle Name text change.
	 */
	private final class NameModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			name = nameText.getText();
			notifyListeners();
		}
	}

	/**
	 * Listener that must be implemented by others who want to monitor changes
	 * in this composite
	 */
	public interface WirelessPropertiesChangedListener {
		public void propertiesChanged();
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent composite
	 */
	public WirelessPropertiesComposite(Composite parent, String host, ISerialNumbered device) {
		this(parent, host, EMPTY_STRING, EMPTY_STRING, device);
	}

	/**
	 * Create contents of the composite
	 * 
	 * @param parent
	 *            the parent composite
	 * @param initialHost
	 *            initial value for host
	 * @param initialPort
	 *            initial value for port number
	 * @param initialTiomeout
	 *            initial value for timeout
	 */
	public WirelessPropertiesComposite(Composite parent, String initialHost, String initialPort, String initialTimeout,
			ISerialNumbered device) {
		super(parent, SWT.NONE);
		this.name = device.getDeviceName() + RemoteDeviceConstants.DEFAULT_WIRELESS_SUFIX;
		this.host = initialHost;
		this.port = RemoteDeviceConstants.DEFAULT_PORT;
		this.timeout = RemoteDeviceConstants.DEFAULT_TIMEOUT;

		// Set Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, RemoteDeviceConstants.WIRELESS_HELP_ID);
		setLayout(new GridLayout(2, false));

		// add device name
		Label nameLabel = new Label(this, SWT.NONE);
		nameLabel.setText(RemoteDeviceNLS.UI_Name);
		nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		// add device text
		nameText = new Text(this, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nameText.addModifyListener(new NameModifyListener());
		nameText.setText(name);

		// add IP name
		Label hostLabel = new Label(this, SWT.NONE);
		hostLabel.setText(RemoteDeviceNLS.UI_Host);
		hostLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		// add IP text
		Text hostText = new Text(this, SWT.BORDER);
		hostText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		hostText.setEnabled(false);
		hostText.setText(host);

		// add port name
		Label portLabel = new Label(this, SWT.NONE);
		portLabel.setText(RemoteDeviceNLS.UI_Port);
		portLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		// add port text
		portText = new Text(this, SWT.BORDER);
		portText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		portText.setText(String.valueOf(port));
		portText.addModifyListener(new PortModifyListener());
		portText.setText(String.valueOf(port));

		// add timeout label
		Label timeoutLabel = new Label(this, SWT.NONE);
		timeoutLabel.setText(RemoteDeviceNLS.UI_Timeout);
		timeoutLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		// add timeout text
		timeoutText = new Text(this, SWT.BORDER);
		timeoutText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		timeoutText.addModifyListener(new TimeoutModifyListener());
		timeoutText.setText((!initialTimeout.equals(EMPTY_STRING)) ? initialTimeout : String
				.valueOf(RemoteDeviceConstants.DEFAULT_TIMEOUT));

		// manage the case where the entered IP/Port matches of the an existing
		// device
		manageSameIPAndPortOfRemoteDevice();
	}

	/**
	 * Add a listener which will be notified when there is a change in the
	 * composite
	 * 
	 * @param wirelessDeviceWizardPage
	 *            a listener which will be notified when there is a change in
	 *            the composite
	 */
	public void addPropertyChangeListener(WirelessPropertiesChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list
	 * 
	 * @param wirelessDeviceWizardPage
	 *            the listener to be removed
	 */
	public void removePropertyChangeListener(WirelessPropertiesChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Get the configured host
	 * 
	 * @return the configured host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get the configured timeout
	 * 
	 * @return the configured timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Get the configured port number
	 * 
	 * @return the configured port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the status associated with the current state.
	 * 
	 * @return The {@link IStatus}.
	 */
	public IStatus getStatus() {
		IStatus status = new Status(IStatus.OK, RemoteDevicePlugin.PLUGIN_ID,
				RemoteDeviceNLS.UI_WirelessInformationPage_Description);
		String errorMessage = null;
		boolean isValidationOK = true;

		if ((name == null) || name.equals("")) {
			errorMessage = NAME_ERR_MESSAGE;
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
			isValidationOK = false;
		}

		if (isValidationOK && (port < 0)) {
			errorMessage = PORT_ERR_MESSAGE;
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
			isValidationOK = false;
		}
		if ((isValidationOK && (port < RemoteDeviceConstants.MINIMUM_PORT_NUMBER))
				|| (port > RemoteDeviceConstants.MAXIMUM_PORT_NUMBER)) {
			errorMessage = NLS.bind(RemoteDeviceNLS.WirelessPropertiesComposite_MsgPortNumberEqualOrHigherThan,
					RemoteDeviceConstants.MINIMUM_PORT_NUMBER, RemoteDeviceConstants.MAXIMUM_PORT_NUMBER);
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
			isValidationOK = false;
		}
		if (isValidationOK && (timeout < 0)) {
			errorMessage = TIMEOUT_ERR_MESSAGE;
			status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
			isValidationOK = false;
		}
		if (isValidationOK && (host != null)) {
			if (host.equals(EMPTY_STRING)) {
				errorMessage = HOST_ERR_MESSAGE;
				status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
				isValidationOK = false;
			}
		}

		if (isValidationOK) {
			// check if host:port already exist
			Collection<ISerialNumbered> existentRemoteDeviceInstances = DevicesManager.getInstance()
					.getInstancesByType(RemoteDeviceInstance.class);
			for (ISerialNumbered device : existentRemoteDeviceInstances) {
				if (RemoteDeviceUtils.hasSameHostAndPort(device, host, port)) {
					if ((this.device == null)
							|| ((this.device != null) && (!this.device.getDeviceName().equals(device.getDeviceName())))) {
						errorMessage = NLS.bind(RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_WirelessDuplicated,
								device.getDeviceName());
						status = new Status(IStatus.WARNING, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
						isValidationOK = false;
						break;
					}
				}
			}
		}

		if (isValidationOK && (name != null)) {
			// this verification is only applied if the IP/Port does not
			// coincide with another application
			if (getRemoteDeviceWithSameIPAndPort() == null) {
				InstanceRegistry registry = InstanceRegistry.getInstance();
				if (!name.equals(EMPTY_STRING)) {
					if (!(registry.getInstancesByName(name).size() == 0)) {
						errorMessage = DeviceUIResources.SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;
						status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
						isValidationOK = false;
					} else if (!AbstractMobileInstance.validName(name)) {
						errorMessage = DeviceUIResources.SEQUOYAH_Instance_Name_Invalid_Error;
						status = new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID, errorMessage);
						isValidationOK = false;
					}
				}
			}
		}

		return status;
	}

	/*
	 * Notify change listeners that there was a change in the values
	 */
	private void notifyListeners() {
		synchronized (listeners) {
			for (WirelessPropertiesChangedListener listener : listeners) {
				listener.propertiesChanged();
			}
		}
	}

	/*
	 * Manage the case in which was inputed the same IP and port of an existing
	 * remote device.
	 */
	private void manageSameIPAndPortOfRemoteDevice() {
		ISerialNumbered matchedDevice = getRemoteDeviceWithSameIPAndPort();
		// if there is a device which matches the IP/Port of this wizard, set
		// its Name and disable the field
		if (matchedDevice != null) {
			nameText.setText(matchedDevice.getDeviceName());
			nameText.setEnabled(false);
		}
		// since there is no device, enable the name field
		else {
			nameText.setEnabled(true);
		}
	}

	/*
	 * Returns a device which matches the IP/Port of this wizard, in cas there
	 * is any.
	 * 
	 * @return get the {@link ISerialNumbered} device which matches the IP/Port
	 * of this wizard.
	 */
	private ISerialNumbered getRemoteDeviceWithSameIPAndPort() {
		ISerialNumbered matchedDevice = null;
		Collection<ISerialNumbered> existentRemoteDeviceInstances = DevicesManager.getInstance().getInstancesByType(
				RemoteDeviceInstance.class);
		for (ISerialNumbered device : existentRemoteDeviceInstances) {
			if (RemoteDeviceUtils.hasSameHostAndPort(device, host, port)) {
				if ((this.device == null)
						|| ((this.device != null) && (!this.device.getDeviceName().equals(device.getDeviceName())))) {
					matchedDevice = device;
					break;
				}
			}
		}
		return matchedDevice;
	}

	/**
	 * Get the {@link IProgressMonitor}
	 * 
	 * @return Return the monitor
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
	 * Get the device name.
	 * 
	 * @return Device name
	 */
	public String getDeviceName() {
		return this.name;
	}
}
