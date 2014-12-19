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
package org.eclipse.andmore.android.emulator.device.ui;

import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeEvent;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeListener;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * DESCRIPTION: <br>
 * This class implements the Property Page for Android Emulator Device
 * Instances. <br>
 * It shows all Android Emulator Device Instance properties on the UI so that
 * the user is able to edit it (the instance name is not a property and will not
 * be editable). <br>
 * RESPONSIBILITY: <br>
 * - Allow viewing and editing of Android Emulator Device Instance properties <br>
 * COLABORATORS: <br>
 * PropertyPage: extends this class <br>
 * InfoComposite: uses this composite for exhibiting instance properties on the
 * UI <br>
 * USAGE: <br>
 * This class should be defined by the plugin.xml file as a regular Eclipse
 * Property Page. It should be enabled for AndroidEmulatorInstance objects.
 */
public class AndroidPropertiesPage extends PropertyPage implements IWorkbenchPropertyPage, IDevicePropertiesConstants {
	// the Android Emulator Device Instance to which this Property Page applies
	private AndroidDeviceInstance emuInstance;

	private InfoComposite infoComposite;

	// whether this property page will need its default message to be reset
	// this happens in case the initial state of the property page when it is
	// opened is an erroneous state (any of the properties contain invalid
	// value)
	private boolean defaultMessageNeedsReset = false;

	// the default message defined by Eclipse implementation for reset purposes
	private String defaultMessage = getMessage();

	// handle changes
	private PropertyCompositeChangeListener compositeChangeListener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {
			String errorMessage = infoComposite.getErrorMessage();
			setErrorMessage(errorMessage);
			setValid((errorMessage == null) && (getMessage() == null));

			if (defaultMessageNeedsReset) {
				defaultMessageNeedsReset = false;
				setMessage(defaultMessage);
			}
		}
	};

	/**
	 * Creates the UI contents of this Property Page. It shows the Android
	 * Emulator Device Instance properties organized into tabs.
	 */
	@Override
	protected Control createContents(Composite parent) {
		((PreferenceDialog) this.getContainer()).getTreeViewer().expandAll();

		noDefaultAndApplyButton();

		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginWidth = 0;
		mainLayout.marginHeight = 0;
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(mainLayout);

		infoComposite = new InfoComposite(composite, emuInstance.getProperties(), emuInstance.getName(),
				!emuInstance.isStarted());
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		AbstractPropertiesComposite.addCompositeChangeListener(compositeChangeListener);

		// there may be some info message for the composite
		String initialMessage = infoComposite.getInfoMessage();

		// if no info message, check if there is some error message
		if (initialMessage == null) {
			// if anything is not correct with instance property values,
			// show the error message, but as an information to follow
			// UI guidelines
			initialMessage = infoComposite.getErrorMessage();

			setValid((initialMessage == null));
		}

		if (initialMessage != null) {
			defaultMessageNeedsReset = true;
			setMessage(initialMessage, INFORMATION);
		}

		return composite;
	}

	/**
	 * Sets the element that owns the properties
	 */
	@Override
	public void setElement(IAdaptable element) {
		// save the instance for direct use
		if (element instanceof AndroidDeviceInstance) {
			emuInstance = (AndroidDeviceInstance) element;
		}

		super.setElement(element);
	}

	/**
	 * Performs the OK operation by setting the edited properties as the
	 * properties for the Android Emulator Device Instance to which this
	 * Property Page applies (the object for which it was created).
	 */
	@Override
	public boolean performOk() {
		if (emuInstance != null) {
			emuInstance.setProperties(infoComposite.getPropertiesWorkingCopy());
			InstanceEventManager.getInstance().notifyListeners(
					new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, emuInstance));
		}

		return super.performOk();
	}

	/**
	 * Remove listeners and dispose widgets
	 */
	@Override
	public void dispose() {
		AbstractPropertiesComposite.removeCompositeChangeListener(compositeChangeListener);
		infoComposite.dispose();
		super.dispose();
	}

}
