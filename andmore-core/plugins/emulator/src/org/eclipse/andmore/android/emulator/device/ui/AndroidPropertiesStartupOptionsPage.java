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

import java.util.Properties;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.SkinFramework;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOptionsMgt;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeEvent;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeListener;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * DESCRIPTION: <br>
 * This class implements the Startup Options Property Page for Android Emulator
 * Device Instances. <br>
 * It shows the Startup Options for the Android Emulator Device Instance on the
 * UI so that the user is able to edit it. <br>
 * RESPONSIBILITY: <br>
 * - Allow viewing and editing Startup Options of an Android Emulator Device
 * Instance <br>
 * COLABORATORS: <br>
 * PropertyPage: extends this class <br>
 * StartupOptionsComposite: uses this composite for exhibiting startup options
 * on the UI <br>
 * USAGE: <br>
 * This class should be defined by the plugin.xml file as a regular Eclipse
 * Property Page. It should be enabled for AndroidEmulatorInstance objects.
 *
 */
public class AndroidPropertiesStartupOptionsPage extends PropertyPage implements IWorkbenchPropertyPage,
		IDevicePropertiesConstants {

	// the Android Emulator Device Instance to which this Property Page applies
	private AndroidDeviceInstance emuInstance;

	private StartupOptionsComposite startupOptionsComposite;

	// whether this property page will need its default message to be reset
	// this happens in case the initial state of the property page when it is
	// opened is an erroneous state (any of the properties contain invalid
	// value)
	private boolean defaultMessageNeedsReset = false;

	// the default message defined by Eclipse implementation for reset purposes
	private final String defaultMessage = getMessage();

	// handle changes
	private final PropertyCompositeChangeListener compositeChangeListener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {
			String errorMessage = startupOptionsComposite.getErrorMessage();
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
		// Create Startup Options area

		SkinFramework sm = new SkinFramework();
		IAndroidSkin skin = null;
		boolean canCalculateScale = true;
		try {
			skin = sm.getSkinById(emuInstance.getSkinId(), emuInstance.getSkinPath());
		} catch (SkinException e) {
			StudioLogger.error(this.getClass(), "Error reading instance skin during startup options page creation", e);
			canCalculateScale = false;
		}

		startupOptionsComposite = new StartupOptionsComposite(parent, emuInstance.getCommandLineArguments(), skin,
				canCalculateScale);
		startupOptionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		AbstractPropertiesComposite.addCompositeChangeListener(compositeChangeListener);

		// If anything is not correct with instance property values,
		// show the error message, but as an information to follow
		// UI guidelines
		String errorMessage = startupOptionsComposite.getErrorMessage();
		setValid((errorMessage == null));
		if (errorMessage != null) {
			defaultMessageNeedsReset = true;
			setMessage(errorMessage, INFORMATION);
		}

		return startupOptionsComposite;

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
	 * Performs the OK operation by setting the edited startup options as the
	 * startup options for the Android Emulator Device Instance to which this
	 * Property Page applies (the object for which it was created).
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		save();
		return super.performOk();
	}

	/**
	 * Performs the Apply operation (which is the same as OK)
	 *
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() {
		save();
		super.performApply();
	}

	/**
	 * Save the edited startup options in the Android Emulator Device Instance
	 */
	private void save() {
		if (emuInstance != null) {
			Properties emuProperties = emuInstance.getProperties();
			emuProperties.setProperty(IDevicePropertiesConstants.commandline, StartupOptionsMgt.getParamList());
			emuInstance.setProperties(emuProperties);
			InstanceEventManager.getInstance().notifyListeners(
					new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, emuInstance));
		}
	}

	/**
	 * Set the default initial properties
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		startupOptionsComposite.reloadValues(NativeUIUtils.getDefaultCommandLine());
		super.performDefaults();
	}

	/**
	 * Remove listeners and dispose widgets
	 */
	@Override
	public void dispose() {
		AbstractPropertiesComposite.removeCompositeChangeListener(compositeChangeListener);
		startupOptionsComposite.dispose();
		startupOptionsComposite = null;
		super.dispose();
	}

}
