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

package org.eclipse.andmore.android.launch.ui;

import java.util.Collection;
import java.util.Properties;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.launch.LaunchPlugin;
import org.eclipse.andmore.android.launch.LaunchUtils;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * DESCRIPTION: This class implements the device selection dialog
 *
 * RESPONSIBILITY: Provides a dialog populated with the available device
 * instances
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public class DeviceSelectionDialog extends ElementListSelectionDialog {
	private static final String DEV_SELECTION_CONTEXT_HELP_ID = LaunchPlugin.PLUGIN_ID + ".deviceSelectionDialog";

	/**
	 * Default constructor
	 * 
	 * @param parent
	 *            Parent shell
	 * @param description
	 *            Dialog description
	 */
	public DeviceSelectionDialog(Shell parent, String description, final IProject project) {
		super(parent, new LabelProvider() {
			@Override
			public String getText(Object element) {
				String result = "";
				if (element instanceof ISerialNumbered) {
					ISerialNumbered serialNumbered = (ISerialNumbered) element;
					result = serialNumbered.getDeviceName();
					if (serialNumbered instanceof IAndroidEmulatorInstance) {
						IAndroidEmulatorInstance emulatorInstance = (IAndroidEmulatorInstance) serialNumbered;
						int emulatorApi = emulatorInstance.getAPILevel();
						String emulatorTarget = emulatorInstance.getTarget();
						result += " (" + emulatorTarget + ", Api version " + emulatorApi + ")";
					} else if (serialNumbered instanceof IInstance) {
						IInstance instance = (IInstance) serialNumbered;
						Properties properties = instance.getProperties();
						if (properties != null) {
							String target = properties.getProperty("ro.build.version.release"); //$NON-NLS-1$
							if (target != null) {
								result += " (Android " + target + ")";
							}
						}
					}
				}
				return result;
			}

			@Override
			public Image getImage(Object element) {

				Image img = null;

				ISerialNumbered serialNumbered = (ISerialNumbered) element;
				IStatus compatible = LaunchUtils.isCompatible(project, serialNumbered);

				// notify the warning state
				if (compatible.getSeverity() == IStatus.WARNING) {
					img = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
				}

				return img;
			}
		});

		this.setTitle(LaunchNLS.UI_LaunchComposite_SelectDeviceScreenTitle);
		this.setMessage(description);

		Collection<ISerialNumbered> instances = DevicesManager.getInstance().getAllDevicesSorted();
		if ((project != null) && (instances != null) && (instances.size() > 0)) {
			Collection<ISerialNumbered> filteredInstances = LaunchUtils.filterInstancesByProject(instances, project);
			Object[] filteredInstancesArray = filteredInstances.toArray();
			this.setElements(filteredInstancesArray);
		}

		this.setHelpAvailable(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DEV_SELECTION_CONTEXT_HELP_ID);
	}
}
