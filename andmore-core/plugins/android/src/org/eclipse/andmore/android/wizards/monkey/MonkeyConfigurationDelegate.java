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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * Performs launch for a Monkey Launch Configuration.
 */
public class MonkeyConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		String deviceName = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
				(String) null);
		ISerialNumbered serialNumber = DevicesManager.getInstance().getDeviceByName(deviceName);

		String otherCmds = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_OTHER_CMDS, (String) null)
				+ " " + configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_EVENT_COUNT_NAME, (String) null);

		if (serialNumber != null) {

			ArrayList<String> t = new ArrayList<String>();

			List<String> defaultValue = null;
			List<String> c = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_SELECTED_PACKAGES,
					defaultValue);
			if (c != null) {
				for (int i = 0; i < c.size(); i++) {
					t.add(c.get(i));
				}
			}

			DDMSUtils.runMonkey(serialNumber.getSerialNumber(), t, otherCmds);

		}
	}

}
