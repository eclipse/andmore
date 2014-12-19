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
package org.eclipse.andmore.android.emulator.device;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.device.refresh.InstancesListRefresh;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.ui.IStartup;

/**
 * This startup intent to iterate over the list of Android Emulator instances
 * and change the emulator ID due the change of the plugin ids for the 1.3.0
 * release
 *
 */
public class SequoyahInstanceBackward implements IStartup {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
		boolean refreshNeeded = false;
		List<IInstance> instances = new ArrayList<IInstance>(InstanceRegistry.getInstance().getInstances());
		for (IInstance oldInstance : instances) {
			if (oldInstance.getDeviceTypeId().equals("org.eclipse.andmore.android.emulator.device.androidDevice")) {
				try {
					InstanceRegistry.getInstance().addInstance(
							InstanceManager.createInstance(oldInstance.getName(),
									"org.eclipse.andmore.android.emulator.androidDevice",
									DevicePlugin.SEQUOYAH_STATUS_OFF, oldInstance.getProperties()));
					InstanceRegistry.getInstance().removeInstance(oldInstance);
				} catch (SequoyahException e) {
					StudioLogger.error(SequoyahInstanceBackward.class,
							"An error ocurred trying to backward old instance: " + oldInstance.getName(), e);
				}

				refreshNeeded = true;
			}
		}
		if (refreshNeeded) {
			InstancesListRefresh.refresh();
		}
	}

}
