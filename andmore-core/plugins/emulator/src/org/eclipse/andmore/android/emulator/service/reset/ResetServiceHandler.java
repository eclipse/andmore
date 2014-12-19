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
package org.eclipse.andmore.android.emulator.service.reset;

import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.emulator.logic.reset.AndroidEmulatorReseter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * DESCRIPTION: This class plugs the reset procedure to a TmL service
 *
 * RESPONSIBILITY: Provide access to the reset feature from TmL device framework
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public class ResetServiceHandler extends ServiceHandler {

	private boolean userAgreed;

	@Override
	public IServiceHandler newInstance() {
		return new ResetServiceHandler();
	}

	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {

		boolean force = false;

		if (arguments != null) {
			Object forceObj = arguments.get(EmulatorPlugin.FORCE_ATTR);
			if (forceObj instanceof Boolean) {
				force = ((Boolean) forceObj).booleanValue();
			}
		}

		IStatus status = Status.OK_STATUS;
		if (!force && !userAgreed) {
			status = Status.CANCEL_STATUS;
		}

		if (status.isOK() && instance instanceof IAndroidLogicInstance) {
			status = AndroidEmulatorReseter.resetInstance((IAndroidLogicInstance) instance);
		}

		// Collecting usage data for statistical purposes
		try {
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_EMULATOR_RESET, UsageDataConstants.KIND_EMULATOR,
					UsageDataConstants.DESCRIPTION_DEFAULT, EmulatorPlugin.PLUGIN_ID, EmulatorPlugin.getDefault()
							.getBundle().getVersion().toString());
		} catch (Throwable e) {
			// Do nothing, but error on the log should never prevent app from
			// working
		}
		return status;
	}

	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		StudioLogger.info("Updating reset service");
		return Status.OK_STATUS;
	}

	@Override
	public IStatus singleInit(List<IInstance> instances) {
		int reset = EclipseUtils.showInformationDialog(EmulatorNLS.GEN_Warning,
				EmulatorNLS.QUESTION_AndroidEmulatorReseter_ConfirmationText,
				new String[] { EmulatorNLS.QUESTION_AndroidEmulatorReseter_Yes,
						EmulatorNLS.QUESTION_AndroidEmulatorReseter_No }, MessageDialog.WARNING);
		userAgreed = reset == Window.OK;

		return Status.OK_STATUS;
	}

}
