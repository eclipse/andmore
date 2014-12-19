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
package org.eclipse.andmore.android.handset;

import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.handset.i18n.AndroidHandsetNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * DESCRIPTION: <br>
 * This class is a handler for the 0FF->Online transition. It always returns OK <br>
 * RESPONSIBILITY: <br>
 * Fill in the gap for the 0FF->Online transition for handsets <br>
 * COLABORATORS: <br>
 * None <br>
 * USAGE: <br>
 * This class is intended to be used by TmL only
 */
public class DummyServiceHandler extends ServiceHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new DummyServiceHandler();
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
	public IStatus runService(IInstance arg0, Map<Object, Object> arg1, IProgressMonitor arg2) {
		String serialNumber = DDMSFacade.getSerialNumberByName(arg0.getName());
		int tries = 0;
		while (!DDMSFacade.isDeviceOnline(serialNumber) && ((tries >= 0) && (tries < 10))) {
			try {
				Thread.sleep(100);
				tries++;
			} catch (InterruptedException e) {
				tries = 10;
			}
		}
		Properties properties = arg0.getProperties();
		if (properties != null) {
			String target = properties.getProperty("ro.build.version.release"); //$NON-NLS-1$
			if (target != null) {
				arg0.setNameSuffix(AndroidHandsetNLS.DummyServiceHandler_androidSuffix + " " + target);
			} else {
				arg0.setNameSuffix(AndroidHandsetNLS.DummyServiceHandler_VERSION_NA);
			}
			InstanceEventManager.getInstance().notifyListeners(
					new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, arg0));
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
	public IStatus updatingService(IInstance arg0, IProgressMonitor arg1) {
		return Status.OK_STATUS;
	}
}
