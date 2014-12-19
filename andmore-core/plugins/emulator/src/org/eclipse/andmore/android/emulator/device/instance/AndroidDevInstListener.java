/* Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.android.emulator.device.instance;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.utils.EmulatorCoreUtils;
import org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceAdapter;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.model.IInstance;

/**
 * DESCRIPTION: Implementation of IInstanceListener for device related actions
 * that depend on the TmL instance registry state. <br>
 * RESPONSIBILITY: Guarantee that the emulator views are updated Run the
 * initialization service when an instance is loaded <br>
 * COLABORATORS: None. <br>
 * USAGE: This class shall be used by Eclipse only.
 */
public class AndroidDevInstListener extends InstanceAdapter {

	/**
	 * @see IInstanceListener#instanceLoaded(InstanceEvent)
	 */
	@Override
	public void instanceLoaded(InstanceEvent e) {
		IInstance instance = e.getInstance();

		if (instance instanceof IAndroidEmulatorInstance) {
			// The service definition defined (by convention) that
			// stopped-dirty is the success state, and not available
			// is the failure state. The exception is being thrown for
			// the framework to set the state correctly.
			if (instance.getStatus().equals(DevicePlugin.SEQUOYAH_STATUS_OFF)) {
				instance.setStatus(EmulatorPlugin.STATUS_NOT_AVAILABLE);
			}
		}
	}

	/**
	 * @see IInstanceListener#instanceDeleted(InstanceEvent)
	 */
	@Override
	public void instanceDeleted(InstanceEvent ev) {
		IInstance instance = ev.getInstance();
		if (instance instanceof AndroidDeviceInstance) {
			SdkUtils.deleteVm(instance.getName());
		}
	}

	/**
	 * @see IInstanceListener#instanceTransitioned(InstanceEvent)
	 */
	@Override
	public void instanceTransitioned(InstanceEvent e) {
		IInstance instance = e.getInstance();

		if (instance instanceof AndroidDeviceInstance) {
			final AndroidDeviceInstance androidDevice = (AndroidDeviceInstance) instance;
			StudioLogger.info("The android device instance status was updated: " + instance + " Status: "
					+ instance.getStatus());

			if (androidDevice.isStarted()) {
				String transitionId = e.getTransitionId();
				if ((transitionId != null) && transitionId.equals("org.eclipse.andmore.android.emulator.startService")) {
					// If it is coming from other state than the started,
					// connect to VNC server
					StudioLogger.info("The emulator " + instance
							+ " transitioned to started state. Try to estabilish a VNC connection...");

					new Thread(new Runnable() {
						@Override
						public void run() {
							AbstractAndroidView.showView();
							EmulatorCoreUtils.refreshEmulatorViews();
						}
					}).start();
				}
			} else if (instance.getStatus().equals(EmulatorPlugin.STATUS_OFFLINE)) {
				androidDevice.resetRuntimeVariables();
				EmulatorCoreUtils.refreshEmulatorViews();
			}

		}

	}

}
