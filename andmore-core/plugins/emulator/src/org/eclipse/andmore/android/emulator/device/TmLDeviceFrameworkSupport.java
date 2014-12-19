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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.emulator.core.devfrm.IDeviceFrameworkSupport;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DeviceUtils;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * DESCRIPTION: This class attaches the TmL device framework to the Android
 * Emulator plug-ins
 *
 * RESPONSIBILITY: to work with the TmL device framework
 *
 * COLABORATORS: None.
 *
 * USAGE: The class should be used by Eclipse only
 */
public class TmLDeviceFrameworkSupport implements IDeviceFrameworkSupport {
	/**
	 * @see IDeviceFrameworkSupport#getAllInstances()
	 */
	@Override
	public Collection<IAndroidEmulatorInstance> getAllInstances() {
		List<IInstance> tmlInstances = InstanceRegistry.getInstance().getInstances();
		Collection<IAndroidEmulatorInstance> androidCollection = new HashSet<IAndroidEmulatorInstance>();
		for (IInstance tmlInstance : tmlInstances) {
			if (tmlInstance instanceof IAndroidEmulatorInstance) {
				androidCollection.add((IAndroidEmulatorInstance) tmlInstance);
			}
		}

		return androidCollection;
	}

	// This should be a contribution to TmL.
	public static IStatus runService(IInstance instance, String serviceID, Map<Object, Object> arguments,
			IProgressMonitor monitor) throws SequoyahException {
		IStatus runStatus = null;
		IDeviceType deviceType = DeviceUtils.getDeviceType(instance);
		for (IService service : deviceType.getServices()) {
			if (service.getId().equals(serviceID)) {
				ServiceHandler serviceHandler = (ServiceHandler) service.getHandler();
				runStatus = serviceHandler.run(instance, arguments, monitor);
				break;
			}
		}

		return runStatus;
	}
}
