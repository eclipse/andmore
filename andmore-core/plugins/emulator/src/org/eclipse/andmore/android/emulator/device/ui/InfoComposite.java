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

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * DESCRIPTION: <br>
 * This class is a composite which shows all Android Emulator Device Instance
 * information. All displayed information can be edited and kept on a Properties
 * object, which can be later used to be set to an actual Android Emulator
 * Device Instance object as its new values. <br>
 * It extends the AbstractPropertiesComposite so as to use its common
 * functionalities. <br>
 * RESPONSIBILITY: <br>
 * - Show all available UI information of a Android Emulator Device Instance <br>
 * COLABORATORS: <br>
 * AbstractPropertiesComposite: extends this class <br>
 * IDevicePropertiesConstants: implements this interface in order to have direct
 * use of its constants for populating the device instance's Properties object <br>
 * USAGE: <br>
 * This composite can be used for any UI that shows all Android Emulator Device
 * Instance UI information. It can either allow of stop the editing of the name
 * of the instance, while other information is always editable. It should be
 * instantiated as a regular composite.
 */
public class InfoComposite extends AbstractPropertiesComposite implements IDevicePropertiesConstants {

	private boolean showEmulatorDefNotFoundMsg = false;

	private Properties emuPropertiesWorkingCopy;

	private String emuName;

	private PropertiesMainComposite mainComposite;

	// the listener to changes on all AbstractPropertiesComposite objects used
	// on this composite
	private PropertyCompositeChangeListener listener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {
			if (e.getSource() instanceof PropertiesMainComposite) {
				emuPropertiesWorkingCopy.setProperty(timeout, mainComposite.getTimeout());
				emuPropertiesWorkingCopy.setProperty(skinId, mainComposite.getSkinId());
				emuPropertiesWorkingCopy.setProperty(useVnc, mainComposite.getUseVnc());
				emuPropertiesWorkingCopy.setProperty(useProxy, mainComposite.getUseProxy());
				emuPropertiesWorkingCopy.setProperty(startFromSnapshot, mainComposite.getstartFromSnapshot());
				emuPropertiesWorkingCopy.setProperty(saveSnapshot, mainComposite.getSaveSnapshot());
				emuPropertiesWorkingCopy.setProperty(abiType, mainComposite.getAbiType());

				notifyCompositeChangeListeners();
			}
		}
	};

	/**
	 * Creates a InfoComposite object.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param emuProperties
	 *            the instance properties to be shown on the UI
	 * @param emuName
	 *            the name of the instance
	 * @param isNameEditable
	 *            whether the instance name should be made editable or not
	 * @param areOtherFieldsEditable
	 *            True if the user will be able to edit other data in the
	 *            composite; false otherwise
	 */
	public InfoComposite(Composite parent, Properties emuProperties, String emuName, boolean areOtherFieldsEditable) {
		super(parent);

		this.emuPropertiesWorkingCopy = (Properties) emuProperties.clone();
		this.emuName = emuName;

		createUI(areOtherFieldsEditable);

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				AbstractPropertiesComposite.removeCompositeChangeListener(listener);
				InfoComposite.this.dispose();
			}
		});
	}

	/**
	 * Creates all the UI for this composite. The listener declared as attribute
	 * to this class is added to all composites extending
	 * AbstractPropertiesComposite. Information is organized on 3 tabs:<br>
	 * - "Main" tab: uses the PropertiesMainComposite;<br>
	 * - "Advanced" tab: uses the PropertiesAdvancedComposite;<br>
	 * 
	 * @param editable
	 *            True if the user will be able to edit data in the composite;
	 *            false otherwise
	 */
	private void createUI(boolean editable) {
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginWidth = 0;
		mainLayout.marginHeight = 0;
		this.setLayout(mainLayout);

		mainComposite = new PropertiesMainComposite(this, emuName, getProperty(emulatorDefId), getProperty(timeout),
				Boolean.parseBoolean(getProperty(useVnc)), Boolean.parseBoolean(getProperty(useProxy)),
				Boolean.parseBoolean(getProperty(useSnapshots)), Boolean.parseBoolean(getProperty(saveSnapshot)),
				Boolean.parseBoolean(getProperty(startFromSnapshot)), SdkUtils.getTargetByName(getProperty(vmTarget)),
				getProperty(vmSkin), getProperty(vmPath), getProperty(abiType), true, false, editable);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		AbstractPropertiesComposite.addCompositeChangeListener(listener);
	}

	/**
	 * Retrieves the given property from the instance.
	 * 
	 * @param propertyName
	 *            the name of the property (key)
	 * 
	 * @return the property value
	 */
	private String getProperty(String propertyName) {
		String emuProperty = emuPropertiesWorkingCopy.getProperty(propertyName);
		if (emuProperty == null) {
			emuProperty = "";
		}
		return emuProperty;
	}

	/**
	 * Retrieves the (potentially) edited properties.
	 * 
	 * @return the edited properties
	 */
	public Properties getPropertiesWorkingCopy() {
		return emuPropertiesWorkingCopy;
	}

	/**
	 * Retrieves the error message associated to this composites current state.
	 * "Main" tab's error message is returned if any; if none, "Skin" tab's
	 * error message is returned if any; if none, "VM" tab's error message is
	 * returned if any; if none, <code>null</code> is returned to state there is
	 * no error with the current state.
	 * 
	 * @return the error message, or <code>null</code> if there are no errors
	 */
	@Override
	public String getErrorMessage() {
		String errorMessage = mainComposite.getErrorMessage();

		return errorMessage;
	}

	/**
	 * Retrieves the information message associated to this composites current
	 * state.
	 * 
	 * @return the information message, or <code>null</code> if there are no
	 *         information messages
	 */
	public String getInfoMessage() {
		String infoMessage = null;

		if (showEmulatorDefNotFoundMsg) {
			infoMessage = EmulatorNLS.INFO_InfoComposite_EmulatorDefinitionNotFound;
		}

		return infoMessage;
	}
}
