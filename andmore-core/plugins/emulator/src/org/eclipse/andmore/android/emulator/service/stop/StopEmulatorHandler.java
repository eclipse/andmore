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
package org.eclipse.andmore.android.emulator.service.stop;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.util.Map;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.stop.AndroidEmulatorStopper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * DESCRIPTION: This class plugs the stop procedure to a TmL service
 *
 * RESPONSIBILITY: Provide access to the stop feature from TmL device framework
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public class StopEmulatorHandler extends ServiceHandler {
	@Override
	public IServiceHandler newInstance() {
		return new StopEmulatorHandler();
	}

	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		debug("Executing the stop emulator service... Instance:" + instance + " Arguments:" + arguments);

		IStatus status = Status.OK_STATUS;

		// actually stop emulator
		if (!(instance instanceof AndroidDeviceInstance)) {
			error("Aborting start service. This is not an Android Emulator instance...");

			status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID,
					EmulatorNLS.ERR_StopEmulatorHandler_NotAnAndroidEmulator);
		} else {
			boolean force = false;
			if (arguments != null) {
				Object forceObj = arguments.get(EmulatorPlugin.FORCE_ATTR);
				if (forceObj instanceof Boolean) {
					force = ((Boolean) forceObj).booleanValue();
				}
			}

			boolean stopPerformed = AndroidEmulatorStopper.stopInstance((AndroidDeviceInstance) instance, force, true,
					monitor);

			if (!stopPerformed) {
				// user decided not to stop; return a cancel status
				status = Status.CANCEL_STATUS;
			} else {
				instance.setNameSuffix(null);
				InstanceEventManager.getInstance().notifyListeners(
						new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, instance));
			}
		}

		debug("Finished the execution of the stop emulator service: " + instance + " status: " + status);

		// Collecting usage data for statistical purposes
		try {
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_EMULATOR_STOP, UsageDataConstants.KIND_EMULATOR,
					status.toString(), EmulatorPlugin.PLUGIN_ID, EmulatorPlugin.getDefault().getBundle().getVersion()
							.toString());
		} catch (Throwable e) {
			// Do nothing, but error on the log should never prevent app from
			// working
		}

		return status;
	}

	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}
}
