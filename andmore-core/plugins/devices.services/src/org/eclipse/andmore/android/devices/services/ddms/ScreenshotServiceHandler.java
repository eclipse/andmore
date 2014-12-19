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
 */package org.eclipse.andmore.android.devices.services.ddms;

import java.util.Map;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

public class ScreenshotServiceHandler extends ServiceHandler {
	@Override
	public IServiceHandler newInstance() {
		return new ScreenshotServiceHandler();
	}

	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arg1, IProgressMonitor arg2) {
		if (instance instanceof ISerialNumbered) {
			final String serialNumber = ((ISerialNumbered) instance).getSerialNumber();
			Job job = new Job("Screenshot") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					DDMSUtils.takeScreenshot(serialNumber);
					return Status.OK_STATUS;
				}

			};
			job.schedule();
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}

}
