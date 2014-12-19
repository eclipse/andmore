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
package org.eclipse.andmore.android.wizards.monkey;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * DESCRIPTION: <br>
 * This class implements the device selection dialog <br>
 * RESPONSIBILITY: <br>
 * Provides a dialog populated with the available device instances <br>
 * COLABORATORS: <br>
 * None. <br>
 * USAGE: <br>
 * This class is intended to be used by Eclipse only
 */
public class DeviceSelectionDialog extends ElementListSelectionDialog {
	private static final String DEV_SELECTION_CONTEXT_HELP_ID = AndroidPlugin.PLUGIN_ID + ".deviceSelectionDialog";

	/**
	 * Default constructor
	 * 
	 * @param parent
	 *            Parent shell
	 * @param description
	 *            Dialog description
	 */
	public DeviceSelectionDialog(Shell parent, String description) {
		super(parent, new LabelProvider() {
			@Override
			public String getText(Object element) {
				String result = "";
				if (element instanceof ISerialNumbered) {
					ISerialNumbered serialNumbered = (ISerialNumbered) element;
					result = serialNumbered.getFullName();
				}
				return result;
			}

		});

		this.setTitle(AndroidNLS.UI_MonkeyComposite_SelectDeviceScreenTitle);
		this.setMessage(description);

		Collection<ISerialNumbered> instances = DevicesManager.getInstance().getAllDevices();

		if ((instances != null) && (instances.size() > 0)) {

			Collection<ISerialNumbered> filteredInstances = new LinkedList<ISerialNumbered>();

			for (ISerialNumbered instance : instances) {
				if (DDMSFacade.isDeviceOnline(instance.getSerialNumber())) {
					filteredInstances.add(instance);
				}
			}
			Object[] filteredInstancesArray = filteredInstances.toArray();
			this.setElements(filteredInstancesArray);
		}

		this.setHelpAvailable(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DEV_SELECTION_CONTEXT_HELP_ID);
	}
}
