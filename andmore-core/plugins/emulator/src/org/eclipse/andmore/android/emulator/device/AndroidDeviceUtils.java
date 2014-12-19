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

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic.LogicMode;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;

public class AndroidDeviceUtils {
	public static synchronized void fireDummyStartTransition(AndroidDeviceInstance instance, String serialNumber) {
		// if instance is not already started, is not starting and is already
		// associated to a VM...
		boolean instanceStarted = instance.isStarted();
		boolean instanceIsStarting = instance.getStateMachineHandler().isTransitioning();
		boolean vmAlreadyUp = instance.hasDevice();
		instance.setNameSuffix(serialNumber + ", " + instance.getTarget());
		InstanceEventManager.getInstance().notifyListeners(
				new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, instance));

		if (vmAlreadyUp && !instanceStarted && !instanceIsStarting) {
			info("The TmL Instance is not started/Starting, but the emulator/VM is already online. Execute a dummy start service to force a transition to start status...");

			Map<Object, Object> attributes = new LinkedHashMap<Object, Object>();
			attributes.put(LogicMode.class, LogicMode.DO_NOTHING);
			try {
				EmulatorPlugin.getStartServiceHandler().run(instance, attributes, new NullProgressMonitor());
			} catch (Exception e) {
				error("Failed to run the dummy start service on " + instance + " : " + e.getMessage());
			}
		}
	}

	/**
	 * Verifies whether or not a given AndroidDeviceInstance is running.
	 * 
	 * @param androidInstance
	 * @return true if instance is running, false otherwise.
	 */
	public static boolean isInstanceStarting(AndroidDeviceInstance androidInstance) {
		return androidInstance.getStateMachineHandler().isTransitioning();
	}
}
