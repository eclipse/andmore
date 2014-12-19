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
package org.eclipse.andmore.wizards.buildingblocks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

/**
 * Class that implements a Content Provider for the project selection on the New
 * Project Wizard
 */
class ElementTreeContentProvider extends StandardJavaElementContentProvider {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.StandardJavaElementContentProvider#getJavaProjects
	 * (org.eclipse.jdt.core.IJavaModel)
	 */
	@Override
	protected Object[] getJavaProjects(IJavaModel jm) throws JavaModelException {
		Object[] javaProjects = super.getJavaProjects(jm);
		List<Object> androidProjects = new ArrayList<Object>();
		for (Object obj : javaProjects) {
			try {
				if ((obj instanceof IJavaProject)
						&& ((IJavaProject) obj).getProject().hasNature(IAndroidConstants.ANDROID_NATURE)) {
					androidProjects.add(obj);
				}
			} catch (CoreException ce) {
				StudioLogger.error(ElementTreeContentProvider.class, ce.getLocalizedMessage(), ce);
			}

		}
		return androidProjects.toArray();
	}
}
