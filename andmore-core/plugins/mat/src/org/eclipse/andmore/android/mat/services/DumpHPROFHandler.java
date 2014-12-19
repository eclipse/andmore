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
package org.eclipse.andmore.android.mat.services;

import java.util.Map;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.mat.Activator;
import org.eclipse.andmore.android.mat.i18n.MatNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

public class DumpHPROFHandler extends ServiceHandler {

	@Override
	public IServiceHandler newInstance() {
		return new DumpHPROFHandler();
	}

	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arg1, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		if (instance instanceof ISerialNumbered) {
			ISerialNumbered serialNumbered = (ISerialNumbered) instance;

			final String serialNumber = serialNumbered.getSerialNumber();
			int deviceApiVersion = DDMSUtils.getDeviceApiVersion(serialNumber);

			if (deviceApiVersion > 0) {
				if (deviceApiVersion > 2) {

					Job job = new Job(MatNLS.JOB_Name_Dump_Hprof) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							return DDMSUtils.dumpHPROF(serialNumber, monitor);
						}

					};
					job.setUser(true);
					job.schedule();
				} else {
					status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, MatNLS.DumpHPROFHandler_UNSUPPORTED_DEVICE);
				}
			} else {
				status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, MatNLS.DumpHPROFHandler_DEVICE_NOT_READY);
			}

		}
		return status;
	}

	@Override
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}

}
