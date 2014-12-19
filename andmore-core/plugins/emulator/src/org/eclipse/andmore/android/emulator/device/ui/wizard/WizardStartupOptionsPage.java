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
package org.eclipse.andmore.android.emulator.device.ui.wizard;

import java.io.File;
import java.util.Properties;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.SkinFramework;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOptionsMgt;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite;
import org.eclipse.andmore.android.emulator.device.ui.StartupOptionsComposite;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeEvent;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeListener;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * DESCRIPTION: <br>
 * This class represents the second wizard page for Android Emulator Device
 * Instance creation. <br>
 * It shows all startup options information and validates it, setting an
 * appropriate error message when applicable. <br>
 * RESPONSIBILITY: <br>
 * - Allow user to enter startup options information for creating a new Android
 * Emulator Device Instance <br>
 * - Validates tartup options information entered by user <br>
 * COLABORATORS: <br>
 * WizardPage: extends this class <br>
 * StartupOptionsMainComposite: uses this composite as the main widget <br>
 * USAGE: <br>
 * This wizard page must be added as the second page on the class implementing
 * the New Android Emulator Device Instance Wizard.
 */
public class WizardStartupOptionsPage extends WizardPage implements IInstanceProperties {
	private StartupOptionsComposite startupOptionsComposite;

	private IAndroidSkin skin;

	// handle changes
	private final PropertyCompositeChangeListener compositeChangeListener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {
			String errorMessage = startupOptionsComposite.getErrorMessage();
			if (errorMessage != null) {
				setErrorMessage(errorMessage);
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				setPageComplete(true);
			}
		}
	};

	/**
	 * Creates a WizardMainPage object.
	 */
	public WizardStartupOptionsPage() {
		super(EmulatorNLS.UI_WizardStartupOptionsPage_PageMessage);

	}

	/**
	 * Creates the UI for this wizard page. It uses the PropertiesMainComposite
	 * only.
	 */
	@Override
	public void createControl(Composite parent) {
		// Get selected Skin name
		WizardMainPage page = (WizardMainPage) this.getPreviousPage();
		boolean canCalculateScale = true;
		try {
			if (page.getSkinId() != null) {
				SkinFramework sm = new SkinFramework();
				skin = sm.getSkinById(page.getSkinId(), new File(page.getVmTarget().getLocation() + "skins"
						+ File.separator + page.getVmSkin()));
			}
		} catch (SkinException e) {
			StudioLogger.error(this.getClass(), "Error reading instance skin during startup options page creation", e);
			canCalculateScale = false;
		}

		setTitle(EmulatorNLS.UI_General_WizardTitle);
		setErrorMessage(null);
		if (getMessage() != null) {
			setMessage(EmulatorNLS.UI_WizardStartupOptionsPage_PageMessage);
		}

		// Define layout
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginTop = 0;
		mainLayout.marginWidth = 0;
		mainLayout.marginHeight = 0;

		// Create Startup Options area
		startupOptionsComposite = new StartupOptionsComposite(parent, NativeUIUtils.getDefaultCommandLine(), skin,
				canCalculateScale);

		AbstractPropertiesComposite.addCompositeChangeListener(compositeChangeListener);

		// Set layout
		startupOptionsComposite.setLayout(mainLayout);

		setControl(startupOptionsComposite);

		setPageComplete(true);
	}

	@Override
	public boolean isPageComplete() {
		boolean isComplete = true;
		if (startupOptionsComposite != null) {
			isComplete = (startupOptionsComposite.getErrorMessage() == null);
		}
		return isComplete;
	}

	@Override
	public void dispose() {
		AbstractPropertiesComposite.removeCompositeChangeListener(compositeChangeListener);
		setControl(null);
		if (startupOptionsComposite != null) {
			startupOptionsComposite.dispose();
			startupOptionsComposite = null;
		}
		super.dispose();
	}

	@Override
	public Properties getProperties() {
		Properties properties = new Properties();

		if (startupOptionsComposite == null) {
			properties.setProperty(IDevicePropertiesConstants.commandline, NativeUIUtils.getDefaultCommandLine());

		} else {
			properties.setProperty(IDevicePropertiesConstants.commandline, StartupOptionsMgt.getParamList());
		}

		return properties;
	}
}
