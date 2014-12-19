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

import java.util.Properties;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.andmore.android.remote.ui.RemotePropertiesComposite.RemotePropertiesChangedListener;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page for Android Remote Devices.
 */
public class RemoteDevicePropertiesPage extends PropertyPage implements RemotePropertiesChangedListener,
		IWorkbenchPropertyPage {

	private RemotePropertiesComposite composite;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		String host = "";
		String port = "";
		String timeout = "";

		IInstance instance = null;

		IAdaptable adaptable = getElement();
		if (adaptable instanceof IInstance) {
			instance = (IInstance) adaptable;
			Properties prop = instance.getProperties();
			String propHost = prop.getProperty(RemoteDeviceInstance.PROPERTY_HOST);
			String propPort = prop.getProperty(RemoteDeviceInstance.PROPERTY_PORT);
			String propTimeout = prop.getProperty(RemoteDeviceInstance.PROPERTY_TIMEOUT);
			host = (propHost != null) ? propHost : "";
			port = (propPort != null) ? propPort : "";
			timeout = (propTimeout != null) ? propTimeout : "";
		}

		composite = new RemotePropertiesComposite(parent, host, port, timeout, (ISerialNumbered) instance);
		composite.addPropertyChangeListener(this);
		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				composite.removePropertyChangeListener(RemoteDevicePropertiesPage.this);
			}
		});

		setErrorMessage(composite.getErrorMessage());
		noDefaultAndApplyButton();

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.remote.ui.RemotePropertiesComposite.
	 * RemotePropertiesChangedListener#propertiesChanged()
	 */
	@Override
	public void propertiesChanged() {
		setErrorMessage(composite.getErrorMessage());
		setValid(isValid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		String host = composite.getHost();
		int port = composite.getPort();
		int timeout = composite.getTimeout();

		IAdaptable adaptable = getElement();
		if (adaptable instanceof IInstance) {
			IInstance instance = (IInstance) adaptable;
			Properties prop = instance.getProperties();
			prop.setProperty(RemoteDeviceInstance.PROPERTY_HOST, host);
			prop.setProperty(RemoteDeviceInstance.PROPERTY_PORT, Integer.toString(port));
			prop.setProperty(RemoteDeviceInstance.PROPERTY_TIMEOUT, Integer.toString(timeout));
		}

		return super.performOk();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#isValid()
	 */
	@Override
	public boolean isValid() {
		return (composite.getErrorMessage() == null);
	}

}
