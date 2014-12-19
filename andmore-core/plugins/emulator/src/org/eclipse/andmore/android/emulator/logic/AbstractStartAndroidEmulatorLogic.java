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
package org.eclipse.andmore.android.emulator.logic;

import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractStartAndroidEmulatorLogic implements IAndroidLogic {

	public static enum LogicMode {
		START_MODE, TRANSFER_AND_CONNECT_VNC, RESTART_VNC_SERVER, DO_NOTHING;
	}

	@Override
	public final void execute(IAndroidLogicInstance instance, int timeout, IProgressMonitor monitor)
			throws InstanceStartException, StartCancelledException, StartTimeoutException, IOException {
		this.execute(instance, LogicMode.START_MODE, timeout, monitor);
	}

	public final void execute(IAndroidLogicInstance instance, LogicMode mode, int timeout, IProgressMonitor monitor)
			throws InstanceStartException, StartCancelledException, StartTimeoutException, IOException

	{
		for (IAndroidLogic logic : getLogicCollection(instance, mode)) {
			long timeoutLimit = AndroidLogicUtils.getTimeoutLimit(timeout);
			AndroidLogicUtils.testCanceled(monitor);
			AndroidLogicUtils.testTimeout(timeoutLimit,
					NLS.bind(EmulatorNLS.EXC_TimeoutWhileStarting, instance.getName()));
			info("Executing " + logic.getClass().getSimpleName() + " for " + instance);
			long startTime = System.currentTimeMillis();
			logic.execute(instance, timeout, monitor);
			long endTime = System.currentTimeMillis();
			int duration = (int) (endTime - startTime);
			timeout = timeout - duration;
		}
	}

	public abstract Collection<IAndroidLogic> getLogicCollection(IAndroidLogicInstance instance, LogicMode mode);

}
