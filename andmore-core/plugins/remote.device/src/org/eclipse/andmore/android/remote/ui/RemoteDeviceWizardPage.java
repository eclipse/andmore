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

import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.andmore.android.remote.ui.RemotePropertiesComposite.RemotePropertiesChangedListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard Page to be used by TmL to create a new Device Remove Instance.
 */
public class RemoteDeviceWizardPage extends WizardPage implements IInstanceProperties, RemotePropertiesChangedListener {
	private RemotePropertiesComposite composite;

	/**
	 * Creates a RemoteDeviceWizardPage object.
	 */
	public RemoteDeviceWizardPage() {
		super(RemoteDeviceNLS.UI_RemoteDeviceWizardPage_WizardName);
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
		setTitle(RemoteDeviceNLS.UI_RemoteDeviceWizardPage_Title);
		setMessage(RemoteDeviceNLS.UI_RemoteDeviceWizardPage_Description);

		composite = new RemotePropertiesComposite(parent);
		composite.addPropertyChangeListener(this);
		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				composite.removePropertyChangeListener(RemoteDeviceWizardPage.this);
				composite = null;
				RemoteDeviceWizardPage.this.setControl(null);
			}
		});

		setPageComplete(composite.getErrorMessage() == null);
		setControl(composite);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return (composite != null) && (composite.getErrorMessage() == null);
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
		setPageComplete(isPageComplete());
	}
}
