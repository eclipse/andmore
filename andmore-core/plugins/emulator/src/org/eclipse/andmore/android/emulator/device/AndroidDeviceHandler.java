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

import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;

/**
 * DESCRIPTION: <br>
 * This class represents a TmL IDeviceHandler for Android Emulator Instances. <br>
 * RESPONSIBILITY: <br>
 * - Create an IInstance object for Android Emulator Device Instances <br>
 * COLABORATORS: <br>
 * IDeviceHandler: implements this interface <br>
 * USAGE: <br>
 * This class is declared by the plugin.xml for the Android Emulator Device
 * Instance declaration.
 */
public class AndroidDeviceHandler implements IDeviceHandler {

	/**
	 * Creates an Android Emulator Device Instance with the given id.
	 * 
	 * @param id
	 *            the instance id
	 */
	@Override
	public IInstance createDeviceInstance(String id) {
		IInstance instance = new AndroidDeviceInstance();
		instance.setId(id);
		return instance;
	}

	@Override
	public IDeviceLauncher createDeviceLauncher(IInstance instance) {
		return null;
	}

}
