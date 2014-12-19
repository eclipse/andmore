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

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jface.viewers.Viewer;

/**
 * Class that implements a View Filter for the package selection on the New
 * Project Wizard
 */
@SuppressWarnings("restriction")
class ElementTreeViewFilter extends TypedViewerFilter {
	private static Class<?>[] acceptedClasses = new Class[] { IJavaModel.class, IPackageFragmentRoot.class,
			IJavaProject.class };

	/**
	 * Default constructor
	 */
	public ElementTreeViewFilter() {
		super(acceptedClasses);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter#select(org.eclipse
	 * .jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		boolean select = false;
		if (element instanceof IPackageFragmentRoot) {
			try {
				select = (((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE);
			} catch (JavaModelException e) {
				StudioLogger.error(ElementTreeViewFilter.class, e.getLocalizedMessage(), e);
			}
		} else {
			select = super.select(viewer, parent, element);
		}
		return select;
	}
}
