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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite;
import org.eclipse.andmore.android.emulator.device.ui.PropertiesMainComposite;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeEvent;
import org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite.PropertyCompositeChangeListener;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.DefaultDeviceTypeMenuWizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties;
import org.eclipse.swt.widgets.Composite;

import com.android.sdklib.IAndroidTarget;
import com.android.SdkConstants;

/**
 * DESCRIPTION: <br>
 * This class represents the first wizard page for Android Emulator Device
 * Instance creation. <br>
 * It shows all main information and validates it, setting an appropriate error
 * message when applicable. <br>
 * RESPONSIBILITY: <br>
 * - Allow user to enter main information for creating a new Android Emulator
 * Device Instance <br>
 * - Validate main information entered by user <br>
 * COLABORATORS: <br>
 * WizardPage: extends this class <br>
 * PropertiesMainComposite: uses this composite as the main widget <br>
 * USAGE: <br>
 * This wizard page must be added as the first page on the class implementing
 * the New Android Emulator Device Instance Wizard.
 */
public class WizardMainPage extends WizardPage implements IInstanceProperties {
	private PropertiesMainComposite mainComposite;

	DefaultDeviceTypeMenuWizardPage tmlPage = null;

	private PropertyCompositeChangeListener listener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {

			String errorMessage = mainComposite.getErrorMessage();

			if (errorMessage != null) {
				setErrorMessage(errorMessage);
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				setPageComplete(true);
				setMessage(EmulatorNLS.UI_WizardMainPage_PageName);
			}

		}
	};

	/**
	 * Creates a WizardMainPage object.
	 */
	public WizardMainPage() {
		super(EmulatorNLS.UI_WizardMainPage_PageName);
	}

	/**
	 * Creates the UI for this wizard page. It uses the PropertiesMainComposite
	 * only.
	 */
	@Override
	public void createControl(Composite parent) {

		// Collecting usage data for statistical purpose
		try {
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_EMULATOR_CREATION_WIZARD,
					UsageDataConstants.KIND_EMULATOR, UsageDataConstants.DESCRIPTION_DEFAULT, EmulatorPlugin.PLUGIN_ID,
					EmulatorPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable e) {
			// Do nothing, but error on the log should never prevent app from
			// working
		}

		setTitle(EmulatorNLS.UI_General_WizardTitle);
		setErrorMessage(null);
		setMessage(EmulatorNLS.UI_WizardMainPage_PageName);

		IAndroidTarget vmTarget = null;
		String vmSkin = ""; //$NON-NLS-1$
		String vmPath;
		String timeout;
		String useVnc;
		String useProxy;
		String abiType = SdkConstants.ABI_ARMEABI;
		String useSnapshot;
		String saveSnapshot;
		String startFromSnapshot;

		tmlPage = (DefaultDeviceTypeMenuWizardPage) this.getPreviousPage();

		IAndroidTarget targets[] = SdkUtils.getAllTargets();

		if ((targets != null) && (targets.length > 0)) {

			// Sort the targets array by comparing the API level of them
			Arrays.sort(targets, new Comparator<IAndroidTarget>() {

				@Override
				public int compare(IAndroidTarget o1, IAndroidTarget o2) {
					int returnValue;
					if (o1.getVersion().getApiLevel() == o2.getVersion().getApiLevel()) {
						returnValue = 0;
					} else if (o1.getVersion().getApiLevel() > o2.getVersion().getApiLevel()) {
						returnValue = 1;
					} else {
						returnValue = -1;
					}

					return returnValue;
				}

			});

			// Gets the first target with the highest API

			int maxAPILevel = targets[targets.length - 1].getVersion().getApiLevel();

			for (IAndroidTarget t : targets) {
				if (t.getVersion().getApiLevel() == maxAPILevel) {
					vmTarget = t;
					break;
				}

			}

			File skins[] = vmTarget.getSkins();
			vmSkin = vmTarget.getDefaultSkin().getName();
			List<File> skinsList = Arrays.asList(skins);

			/*
			 * Workaround to select WVGA skin on JIL SDK because HVGA skin is
			 * broken
			 */
			if (SdkUtils.isJILSdk()) {
				String tmpVmSkin = null;
				int i = 0;
				while ((tmpVmSkin == null) && (i < skins.length)) {
					if (skins[i].getName().toLowerCase().trim().equals("wvga")) {
						tmpVmSkin = skins[i].getName();
					}
					i++;
				}
				if (tmpVmSkin != null) {
					vmSkin = tmpVmSkin;
				}
			}

			if (!skinsList.contains(vmSkin)) {
				vmSkin = skins[0].getName();
			}
		}

		vmPath = IDevicePropertiesConstants.defaultVmPath;

		// get the default properties value
		Properties defaultProperties = new Properties();
		AndroidDeviceInstance.populateWithDefaultProperties(defaultProperties);
		timeout = defaultProperties.getProperty(IDevicePropertiesConstants.timeout);
		useVnc = defaultProperties.getProperty(IDevicePropertiesConstants.useVnc);
		useProxy = defaultProperties.getProperty(IDevicePropertiesConstants.useProxy);
		useSnapshot = defaultProperties.getProperty(IDevicePropertiesConstants.useSnapshots);
		saveSnapshot = defaultProperties.getProperty(IDevicePropertiesConstants.saveSnapshot);
		startFromSnapshot = defaultProperties.getProperty(IDevicePropertiesConstants.startFromSnapshot);

		// When removing the emulator definition extension, remove this
		// hardcoded set as
		// well as the constant declaration from the Activator. If the Mot skin
		// plugin is used,
		// find another way to set this variable
		mainComposite = new PropertiesMainComposite(parent, tmlPage.getInstanceName(),
				EmulatorPlugin.DEFAULT_EMULATOR_DEFINITION, timeout, Boolean.parseBoolean(useVnc),
				Boolean.parseBoolean(useProxy), Boolean.parseBoolean(useSnapshot), Boolean.parseBoolean(saveSnapshot),
				Boolean.parseBoolean(startFromSnapshot), vmTarget, vmSkin, vmPath, abiType, false, true, true);

		AbstractPropertiesComposite.addCompositeChangeListener(listener);

		setControl(mainComposite);

		if ((targets == null) || ((targets != null) && (targets.length <= 0))) {
			setMessage(EmulatorNLS.WizardMainPage_NO_SDK_CONFIGURED_MSG);
			setPageComplete(false);
			return;
		}

		String initialMessage = mainComposite.getErrorMessage();

		if (initialMessage != null) {
			setMessage(initialMessage);
		}
		setPageComplete(initialMessage == null);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			mainComposite.setName(tmlPage.getInstanceName());
		}
		super.setVisible(visible);
	}

	/**
	 * Retrieves the skin id associated with this instance
	 * 
	 * @return the skin id
	 */
	public String getSkinId() {
		return mainComposite.getSkinId();
	}

	/**
	 * Retrieves the timeout associated with this instance
	 * 
	 * @return the timeout
	 */
	public String getTimeout() {
		return mainComposite.getTimeout();
	}

	/**
	 * Retrieves the vnc option associated with this instance
	 * 
	 * @return the timeout
	 */
	public String getUseVnc() {
		return mainComposite.getUseVnc();
	}

	/**
	 * Retrieves the proxy option associated with this instance
	 * 
	 * @return the timeout
	 */
	public String getUseProxy() {
		return mainComposite.getUseProxy();
	}

	/**
	 * Retrieves the emulator definition name associated with this instance
	 * 
	 * @return the emulator definition name
	 */
	public String getEmulatorDefId() {
		// When removing the emulator definition extension, remove this
		// hardcoded set as
		// well as the constant declaration from the Activator. If the Mot skin
		// plugin is used,
		// find another way to set this variable
		return EmulatorPlugin.DEFAULT_EMULATOR_DEFINITION;
	}

	public String getUseSnapshot() {
		return mainComposite.getUseSnapshot();
	}

	public void setInstanceName(String name) {
		mainComposite.setName(name);
	}

	/**
	 * Retrieves VM target
	 */
	public IAndroidTarget getVmTarget() {
		return mainComposite.getVmTarget();
	}

	/**
	 * Retrieves VM target
	 */
	public String getAbiType() {
		return mainComposite.getAbiType();
	}

	/**
	 * Retrieves VM skin
	 */
	public String getVmSkin() {
		return mainComposite.getVmSkin();
	}

	/**
	 * Retrieves VM path
	 */
	public String getVmPath() {
		return mainComposite.getVmPath();
	}

	/**
	 * Retrieves SD Card info
	 */
	public String getSDCard() {
		return mainComposite.getSDCard();
	}

	/**
	 * Retrieves Command line
	 */
	public String getCommandLine() {
		// The command line shall be editable through the GUI
		Properties defaultProperties = new Properties();
		AndroidDeviceInstance.populateWithDefaultProperties(defaultProperties);
		return defaultProperties.getProperty(IDevicePropertiesConstants.commandline);
	}

	@Override
	public boolean isPageComplete() {
		return (mainComposite != null) && (mainComposite.getErrorMessage() == null);
	}

	@Override
	public void dispose() {
		AbstractPropertiesComposite.removeCompositeChangeListener(listener);
		setControl(null);
		if (mainComposite != null) {
			mainComposite.dispose();
			mainComposite = null;
		}

		super.dispose();
	}

	@Override
	public Properties getProperties() {
		Properties properties = new Properties();

		properties.setProperty(IDevicePropertiesConstants.timeout, this.getTimeout());
		properties.setProperty(IDevicePropertiesConstants.useVnc, this.getUseVnc());
		properties.setProperty(IDevicePropertiesConstants.useProxy, this.getUseProxy());
		properties.setProperty(IDevicePropertiesConstants.emulatorDefId, this.getEmulatorDefId());
		properties.setProperty(IDevicePropertiesConstants.skinId, this.getSkinId());
		properties.setProperty(IDevicePropertiesConstants.vmSkin, this.getVmSkin());
		properties.setProperty(IDevicePropertiesConstants.vmPath, this.getVmPath());
		properties.setProperty(IDevicePropertiesConstants.abiType, this.getAbiType());
		properties.setProperty(IDevicePropertiesConstants.useSnapshots, this.getUseSnapshot());
		properties.setProperty(IDevicePropertiesConstants.saveSnapshot, this.getSaveSnapshot());
		properties.setProperty(IDevicePropertiesConstants.startFromSnapshot, this.getstartFromSnapshot());

		if (this.getVmTarget() != null) {
			properties.setProperty(IDevicePropertiesConstants.vmTarget, this.getVmTarget().getName());
		} else {
			properties.setProperty(IDevicePropertiesConstants.vmTarget, ""); //$NON-NLS-1$
		}

		return properties;
	}

	/**
	 * @return
	 */
	public String getstartFromSnapshot() {
		return mainComposite.getstartFromSnapshot();
	}

	/**
	 * @return
	 */
	public String getSaveSnapshot() {
		return mainComposite.getSaveSnapshot();
	}
}
