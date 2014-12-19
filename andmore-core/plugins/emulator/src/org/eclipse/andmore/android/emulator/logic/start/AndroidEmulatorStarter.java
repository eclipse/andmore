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
package org.eclipse.andmore.android.emulator.logic.start;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Map;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic.LogicMode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * DESCRIPTION: This class contains the business layer of the Android Emulator
 * start procedure
 *
 * RESPONSIBILITY: Start any Android Emulator
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the public method to start a Android Emulator
 */
public class AndroidEmulatorStarter {

	/**
	 * Starts this instance, after creating a clean VM copy at the provided
	 * location. Besides that, adds a new Android Emulator viewer inside the
	 * Android view. This method provides automatic VM startup.
	 *
	 * @param instance
	 *            The Android device instance
	 * @param monitor
	 *            A progress monitor that will give the user feedback about this
	 *            long running operation
	 * 
	 * @return the status of the operation (OK, Cancel or Error+ErrorMessage)
	 */
	public static IStatus startInstance(IAndroidLogicInstance instance, Map<Object, Object> arguments,
			IProgressMonitor monitor) {
		if (instance == null) {
			error("Abort start operation. Instance is null.");
			return new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID,
					EmulatorNLS.ERR_AndroidEmulatorStarter_InstanceNullPointer);
		}

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		IStatus status = Status.OK_STATUS;

		if (!instance.isStarted()) {
			int timeout = instance.getTimeout();
			if (timeout <= 0) {
				status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, NLS.bind(
						EmulatorNLS.ERR_AndroidLogicPlugin_InvalidTimeoutValue, new Object[] { timeout }));
				return status;
			}

			try {
				try {
					LogicMode mode = LogicMode.START_MODE;
					if (arguments != null) {
						Object modeObject = arguments.get(LogicMode.class);
						if (modeObject instanceof LogicMode) {
							mode = (LogicMode) modeObject;
						}
					}

					AbstractStartAndroidEmulatorLogic logic = instance.getStartLogic();
					if (logic != null) {
						debug("Retrieved start logic: " + logic);
						logic.execute(instance, mode, timeout, monitor);
						AndroidLogicUtils.testCanceled(monitor);
					} else {
						error("Cannot start emulator because a logic is not provided for that.");
						throw new InstanceStartException(
								EmulatorNLS.ERR_AndroidEmulatorStarter_NoLogicAvailableForStart);
					}
				} catch (Exception e) {
					error("An exception happened while trying to execute the start emulator logic for " + instance
							+ " Try to rollback by executing the stop process...");

					try {
						instance.stop(true);
					} catch (InstanceStopException e1) {
						error("There was an error while forcing the stop the instance");
					}

					throw e;
				}
			} catch (StartTimeoutException e) {
				error("A timeout has happeded during the start Android Emulator Instance. " + instance + "Cause: "
						+ e.getMessage());
				status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, e.getMessage(), e);
			} catch (InstanceStartException e) {
				error("It was not possible to start the Android Emulator Instance. " + instance + "Cause: "
						+ e.getMessage());
				status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, e.getMessage());
			} catch (StartCancelledException e) {
				info("Start operation was cancelled." + instance);
				status = Status.CANCEL_STATUS;
			} catch (Exception e) {
				error("Unknown exception while starting the emulator instance: " + instance + " Cause: "
						+ e.getMessage());

				status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, e.getMessage());
			}
		}
		return status;
	}
}
