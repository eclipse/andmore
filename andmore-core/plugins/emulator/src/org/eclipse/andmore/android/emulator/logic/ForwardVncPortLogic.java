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

import java.io.IOException;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ForwardVncPortLogic implements IAndroidLogic {

	@Override
	public void execute(IAndroidLogicInstance instance, int timeout, IProgressMonitor monitor) throws IOException,
			InstanceStartException {
		int port = AndroidLogicUtils.getVncServerPortFoward(instance.getInstanceIdentifier());

		if (instance instanceof ISerialNumbered) {
			String serialNumber = ((ISerialNumbered) instance).getSerialNumber();
			try {
				AndroidLogicUtils.testDeviceStatus(serialNumber);
			} catch (AndroidException e) {
				throw new InstanceStartException(e.getMessage());
			}

			boolean forwardOk = DDMSFacade.createForward(serialNumber, port, 5901);
			if (!forwardOk) {
				throw new IOException("Could not forward VNC port 5901 to " + port + " on " + instance.getName());
			}
		}
	}
}
