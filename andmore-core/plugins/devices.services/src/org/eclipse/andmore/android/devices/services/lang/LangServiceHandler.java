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
package org.eclipse.andmore.android.devices.services.lang;

import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.devices.services.DeviceServicesPlugin;
import org.eclipse.andmore.android.devices.services.lang.model.LangWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.ui.PlatformUI;

/**
 * This class plugs the change language procedure to a TmL service
 */
public class LangServiceHandler extends ServiceHandler {
	private String languageID;

	private String countryID;

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new LangServiceHandler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#runService
	 * (org.eclipse.sequoyah.device.framework.model.IInstance, java.util.Map,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;

		final String serialNumber = ((ISerialNumbered) instance).getSerialNumber();

		DDMSUtils.changeLanguage(serialNumber, languageID, countryID);

		// Collecting usage data for statistical purpose
		try {
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_EMULATOR_LANGUAGE, UsageDataConstants.KIND_EMULATOR,
					languageID, DeviceServicesPlugin.PLUGIN_ID, DeviceServicesPlugin.getDefault().getBundle()
							.getVersion().toString());
		} catch (Throwable e) {
			// Do nothing, but error on the log should never prevent app from
			// working
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * updatingService(org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		StudioLogger.info("Updating change language service");
		return Status.OK_STATUS;
	}

	@Override
	public IStatus singleInit(final List<IInstance> instances) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					LangWizard wizard = new LangWizard();
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), wizard);
					dialog.setPageSize(250, 75);

					int ret = dialog.open();

					if (ret == Window.OK) {
						languageID = wizard.getlanguageId();
						countryID = wizard.getcountryId();
					} else {
						languageID = null;
						countryID = null;
					}

				} catch (Exception e) {
					StudioLogger.error("Change Language TmL Service: could not open UI");
				}
			}
		});

		return super.singleInit(instances);
	}
}
