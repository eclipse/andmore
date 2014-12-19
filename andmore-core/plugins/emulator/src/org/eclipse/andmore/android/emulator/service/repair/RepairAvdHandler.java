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
package org.eclipse.andmore.android.emulator.service.repair;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.util.Map;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.device.instance.AndroidDeviceInstance;
import org.eclipse.andmore.android.emulator.device.refresh.InstancesListRefresh;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdInfo.AvdStatus;

/**
 * This is responsible for repair damaged avds.
 */
public class RepairAvdHandler extends ServiceHandler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new RepairAvdHandler();
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
	public IStatus runService(IInstance instance, Map<Object, Object> argumentos, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		if (!(instance instanceof AndroidDeviceInstance)) {
			error(EmulatorNLS.RepairAvdHandler_Not_Android_Instance);

			status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID,
					EmulatorNLS.ERR_StopEmulatorHandler_NotAnAndroidEmulator);
		} else {
			AndroidDeviceInstance androidDeviceInstance = (AndroidDeviceInstance) instance;
			AvdInfo avdInfo = SdkUtils.getVm(androidDeviceInstance.getName());
			if ((avdInfo != null) && isAvdRepairable(avdInfo.getStatus())) {
				status = SdkUtils.repairAvd(avdInfo);
				if (status.getSeverity() != IStatus.OK) {
					error(getClass(), "IOException ocurred during repairAvd operation"); //$NON-NLS-1$
				}
			} else {
				status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID,
						EmulatorNLS.RepairAvdHandler_AVD_NOT_REPAIRABLE);
			}
		}
		if (status.getSeverity() == IStatus.OK) {
			InstancesListRefresh.refresh();
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * updatingService(org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}

	private boolean isAvdRepairable(AvdStatus avdStatus) {
		return avdStatus == AvdStatus.ERROR_IMAGE_DIR;
	}
}
