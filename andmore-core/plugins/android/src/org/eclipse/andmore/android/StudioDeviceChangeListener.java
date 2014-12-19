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
package org.eclipse.andmore.android;

import org.eclipse.ui.IStartup;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;

/**
 * DESCRIPTION: The device change listener to be used by the whole MOTODEV
 * Studio for Android tool. Other plugins should not register listeners in DDMS.
 * Instead, use DDMSFacade
 *
 * RESPONSIBILITY: Delegate the deviceConnected and deviceDisconnected events to
 * the registered runnables
 *
 * COLABORATORS: None.
 *
 * USAGE: This class shall be used by DDMS and DDMSFacade only
 */
public class StudioDeviceChangeListener implements IDeviceChangeListener, IStartup {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
		// Adding the listener in the early startup to guarantee that we will
		// receive
		// events for the devices that were already online before starting the
		// workbench
		AndroidDebugBridge.addDeviceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#deviceChanged
	 * (com.android.ddmlib.Device, int)
	 */
	@Override
	public void deviceChanged(IDevice device, int i) {
		if (i == IDevice.CHANGE_STATE) {
			// a handset should only be instantiated when its state change from
			// OFFLINE to ONLINE
			// to avoid the problem of a remote device on the OFFLINE state be
			// presented as an ONLINE handset
			if ((device.getState() == DeviceState.ONLINE) && (!device.isEmulator())) {
				DDMSFacade.deviceConnected(device);
			}
			DDMSFacade.deviceStatusChanged(device);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#deviceConnected
	 * (com.android.ddmlib.Device)
	 */
	@Override
	public void deviceConnected(IDevice device) {
		// handsets should not be instantiated right after connection because at
		// that time
		// they appear on the OFFLINE state
		if (device.isEmulator()) {
			DDMSFacade.deviceConnected(device);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#
	 * deviceDisconnected(com.android.ddmlib.Device)
	 */
	@Override
	public void deviceDisconnected(IDevice device) {
		DDMSFacade.deviceDisconnected(device);
	}
}
