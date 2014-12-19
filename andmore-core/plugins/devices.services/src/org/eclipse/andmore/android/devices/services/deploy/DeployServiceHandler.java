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
package org.eclipse.andmore.android.devices.services.deploy;

import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.InstallPackageBean;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.services.i18n.ServicesNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * DESCRIPTION: This class plugs the deploy procedure to a TmL service
 *
 * RESPONSIBILITY: Implements the actions that will be triggered when user chose
 * to Install a Android Application on an emulator instance.
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 * */
public class DeployServiceHandler extends ServiceHandler {
	protected InstallPackageBean installBean = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new DeployServiceHandler();
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
		if (instance instanceof ISerialNumbered) {
			final String serialNumber = ((ISerialNumbered) instance).getSerialNumber();
			Job j = new Job(ServicesNLS.JOB_Name_Install_Application + " (" + serialNumber + ")") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					return DDMSUtils.installPackage(serialNumber, installBean);
				}
			};
			j.schedule();
		}

		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * updatingService(org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		StudioLogger.info("Updating reset service");
		return Status.OK_STATUS;
	}

	@Override
	public IStatus singleInit(List<IInstance> instances) {

		this.installBean = DDMSUtils.installPackageWizard();

		return super.singleInit(instances);
	}
}
