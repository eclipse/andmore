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
package org.eclipse.andmore.android.wizards.project;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.android.sdklib.IAndroidTarget;

/**
 * Class that implements a content provider for the Samples Tree viewers.
 */
@SuppressWarnings("restriction")
class TreeContentProvider implements ITreeContentProvider {
	AndroidProject project = null;

	public TreeContentProvider(AndroidProject project) {
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */
	@Override
	public Object[] getChildren(Object arg0) {
		Object[] objects;

		if (arg0 instanceof IAndroidTarget) {
			objects = SdkUtils.getSamples((IAndroidTarget) arg0);
		} else {
			objects = new Object[0];
		}
		return objects;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 * )
	 */
	@Override
	public Object getParent(Object arg0) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	@Override
	public boolean hasChildren(Object arg0) {
		Object[] obj = getChildren(arg0);
		return obj == null ? false : obj.length > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	@Override
	public Object[] getElements(Object arg0) {
		Object[] objs = null;

		if (arg0 instanceof Sdk) {
			Sdk sdk = (Sdk) arg0;
			Object[] targets = SdkUtils.getTargets(sdk);
			if (targets.length > 0) {
				for (Object o : targets) {
					IAndroidTarget target = (IAndroidTarget) o;
					if (target.equals(project.getSdkTarget())) {
						objs = SdkUtils.getSamples(target);
					}
				}
			} else {
				objs = new Object[0];
			}
		}
		return objs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// do nothing
	}
}
