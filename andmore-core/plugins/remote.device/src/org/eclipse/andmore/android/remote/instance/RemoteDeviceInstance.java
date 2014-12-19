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
package org.eclipse.andmore.android.remote.instance;

import java.util.Collection;
import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * This class represents a Android Remote Device instance
 */
public class RemoteDeviceInstance extends AbstractMobileInstance implements ISerialNumbered, IWorkbenchAdapter {

	public static final String PROPERTY_HOST = RemoteDevicePlugin.PLUGIN_ID + ".hostProperty";

	public static final String PROPERTY_PORT = RemoteDevicePlugin.PLUGIN_ID + ".portProperty";

	public static final String PROPERTY_TIMEOUT = RemoteDevicePlugin.PLUGIN_ID + ".timeoutProperty";

	/**
	 * Property used to mark if the device shall be removed from the list when
	 * it gets disconnected.
	 */
	public static final String PROPERTY_VOLATILE = RemoteDevicePlugin.PLUGIN_ID + ".volatileProperty";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.adt.ISerialNumbered#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		String serialNumber = null;
		Properties prop = getProperties();
		if (prop != null) {
			String host = prop.getProperty(PROPERTY_HOST);
			String port = prop.getProperty(PROPERTY_PORT);
			String candidateSerial = host + ":" + port;

			Collection<String> allSerialNumbers = DDMSFacade.getConnectedSerialNumbers();
			if (allSerialNumbers.contains(candidateSerial)) {
				serialNumber = candidateSerial;
			}
		}
		return serialNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.adt.ISerialNumbered#getFullName()
	 */
	@Override
	public String getFullName() {
		if (getNameSuffix() != null) {
			return getName() + " (" + getNameSuffix() + ")";
		} else {
			return getName();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.adt.ISerialNumbered#getDeviceName()
	 */
	@Override
	public String getDeviceName() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance#toString
	 * ()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object
	 * )
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		return getName();
	}

}
