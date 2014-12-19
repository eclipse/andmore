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
package org.eclipse.andmore.android.remote.ui;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.remote.RemoteDeviceConstants;
import org.eclipse.andmore.android.remote.RemoteDeviceUtils;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.osgi.util.NLS;
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
 * Composite containing the Android Remote Device properties for edition
 */
public class RemotePropertiesComposite extends Composite {

	private String host;

	private int port;

	private int timeout;

	// DEVICE - if it already exist
	ISerialNumbered device;

	private final Collection<RemotePropertiesChangedListener> listeners = new LinkedHashSet<RemotePropertiesChangedListener>();

	// IP validation
	private static final String ZERO_TO_255_PATTERN = "((\\d)|(\\d\\d)|([0-1]\\d\\d)|(2[0-4]\\d)|(25[0-5]))";

	private static final String IP_PATTERN = ZERO_TO_255_PATTERN + "\\." + ZERO_TO_255_PATTERN + "\\."
			+ ZERO_TO_255_PATTERN + "\\." + ZERO_TO_255_PATTERN;

	// Error messages
	private static final String HOST_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_IP;

	private static final String PORT_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_Port;

	private static final String TIMEOUT_ERR_MESSAGE = RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_Timeout;

	/**
	 * Listener that must be implemented by others who want to monitor changes
	 * in this composite
	 */
	public interface RemotePropertiesChangedListener {
		public void propertiesChanged();
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent composite
	 */
	public RemotePropertiesComposite(Composite parent) {
		this(parent, "", "", "", null);
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
	public RemotePropertiesComposite(Composite parent, String initialHost, String initialPort, String initialTimeout,
			ISerialNumbered device) {
		super(parent, SWT.NONE);

		this.device = device;
		this.host = (((initialHost != null) && (!initialHost.equals(""))) ? initialHost : null);
		this.port = (((initialPort != null) && (!initialPort.equals(""))) ? Integer.parseInt(initialPort) : -1);
		this.timeout = (((initialTimeout != null) && (!initialTimeout.equals(""))) ? Integer.parseInt(initialTimeout)
				: -1);

		// Set Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, RemoteDeviceConstants.HELP_ID);

		setLayout(new GridLayout(2, false));

		Label hostLabel = new Label(this, SWT.NONE);
		hostLabel.setText(RemoteDeviceNLS.UI_Host);
		GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
		hostLabel.setLayoutData(data);

		final Text hostText = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		hostText.setLayoutData(data);
		hostText.addModifyListener(new ModifyListener() {
			private final Pattern p = Pattern.compile(IP_PATTERN);

			@Override
			public void modifyText(ModifyEvent e) {
				String candidateHost = hostText.getText();
				if (candidateHost != null) {
					Matcher m = p.matcher(candidateHost);
					if (m.matches()) {
						host = candidateHost;
					} else {
						host = "";
					}
					notifyListeners();
				}
			}
		});
		hostText.setText(initialHost);
		hostText.setFocus();

		Label portLabel = new Label(this, SWT.NONE);
		portLabel.setText(RemoteDeviceNLS.UI_Port);
		data = new GridData(SWT.FILL, SWT.FILL, false, false);
		portLabel.setLayoutData(data);

		final Text portText = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		portText.setLayoutData(data);
		portText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String portStr = portText.getText();
				try {
					port = Integer.parseInt(portStr);
				} catch (NumberFormatException e1) {
					port = -1;
				} finally {
					notifyListeners();
				}
			}
		});
		portText.setText(initialPort);

		Label timeoutLabel = new Label(this, SWT.NONE);
		timeoutLabel.setText(RemoteDeviceNLS.UI_Timeout);
		data = new GridData(SWT.FILL, SWT.FILL, false, false);
		timeoutLabel.setLayoutData(data);

		final Text timeoutText = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		timeoutText.setLayoutData(data);
		timeoutText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String timeoutStr = timeoutText.getText();
				try {
					timeout = Integer.parseInt(timeoutStr);
				} catch (NumberFormatException e1) {
					timeout = -1;
				} finally {
					notifyListeners();
				}
			}
		});
		timeoutText.setText((!initialTimeout.equals("")) ? initialTimeout : String
				.valueOf(RemoteDeviceConstants.DEFAULT_TIMEOUT));

	}

	/**
	 * Add a listener which will be notified when there is a change in the
	 * composite
	 * 
	 * @param listener
	 *            a listener which will be notified when there is a change in
	 *            the composite
	 */
	public void addPropertyChangeListener(RemotePropertiesChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
	public void removePropertyChangeListener(RemotePropertiesChangedListener listener) {
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
	 * Get the error message associated with the current state, if any
	 * 
	 * @return the error message associated with the current state, if any
	 */
	public String getErrorMessage() {
		String errorMsg = null;

		if (timeout < 0) {
			errorMsg = TIMEOUT_ERR_MESSAGE;
		}
		if (port < 0) {
			errorMsg = PORT_ERR_MESSAGE;
		} else if ((port < RemoteDeviceConstants.MINIMUM_PORT_NUMBER)
				|| (port > RemoteDeviceConstants.MAXIMUM_PORT_NUMBER)) {
			errorMsg = NLS.bind(RemoteDeviceNLS.WirelessPropertiesComposite_MsgPortNumberEqualOrHigherThan,
					RemoteDeviceConstants.MINIMUM_PORT_NUMBER, RemoteDeviceConstants.MAXIMUM_PORT_NUMBER);
		}
		if (host != null) {
			if (host.equals("")) {
				errorMsg = HOST_ERR_MESSAGE;
			}
		}

		// check if host:port already exist
		Collection<ISerialNumbered> existentRemoteDeviceInstances = DevicesManager.getInstance().getInstancesByType(
				RemoteDeviceInstance.class);
		for (ISerialNumbered device : existentRemoteDeviceInstances) {
			if (RemoteDeviceUtils.hasSameHostAndPort(device, host, port)) {
				if ((this.device == null)
						|| ((this.device != null) && (!this.device.getDeviceName().equals(device.getDeviceName())))) {
					errorMsg = NLS.bind(RemoteDeviceNLS.ERR_RemoteDeviceWizardPage_Duplicated, device.getDeviceName());

					break;
				}
			}
		}

		return errorMsg;
	}

	/*
	 * Notify change listeners that there was a change in the values
	 */
	private void notifyListeners() {
		synchronized (listeners) {
			for (RemotePropertiesChangedListener listener : listeners) {
				listener.propertiesChanged();
			}
		}
	}
}
