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

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * DESCRIPTION: <br>
 * This class represents a handset TmL instance <br>
 * RESPONSIBILITY: <br>
 * Keep handset data/properties <br>
 * COLABORATORS: <br>
 * None <br>
 * USAGE: <br>
 * This class is declared by the plugin.xml for the Android Handsets
 * declaration.
 */
public class AndroidHandsetInstance extends AbstractMobileInstance implements ISerialNumbered, IWorkbenchAdapter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.adt.ISerialNumbered#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		return getName();
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

	@Override
	public String getFullName() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object arg0) {
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
	public ImageDescriptor getImageDescriptor(Object arg0) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object arg0) {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object arg0) {
		return null;
	}

}
