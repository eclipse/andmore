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

import org.eclipse.andmore.android.devices.AbstractDeviceDropSupportHandler;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

public class HandsetDropSupportHandler extends AbstractDeviceDropSupportHandler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport#canDrop
	 * (org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.swt.dnd.TransferData, org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public boolean canDrop(IInstance instance, TransferData data, DropTargetEvent event) {
		return super.canDrop(instance, data, event) && HandsetPlugin.STATUS_ONLINE_ID.equals(instance.getStatus());
	}
}
