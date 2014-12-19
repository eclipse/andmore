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

import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;

/**
 * Class that implements a Validator for the project/package selection on the
 * New Project Wizard
 */
@SuppressWarnings("restriction")
class ElementTreeValidator extends TypedElementSelectionValidator {
	private static Class<?>[] acceptedClasses = new Class[] { IPackageFragmentRoot.class, IJavaProject.class };

	public ElementTreeValidator() {
		super(acceptedClasses, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator#
	 * isSelectedValid(java.lang.Object)
	 */
	@Override
	public boolean isSelectedValid(Object element) {
		boolean isValid = false;
		try {
			if (element instanceof IJavaProject) {
				IJavaProject jproject = (IJavaProject) element;
				IPath path = jproject.getProject().getFullPath();
				isValid = (jproject.findPackageFragmentRoot(path) != null);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) element;

				boolean isSrc = (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE);
				boolean isGen = packageFragmentRoot.getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER)
						&& (packageFragmentRoot.getParent() instanceof IJavaProject);

				isValid = isSrc && !isGen;
			} else {
				isValid = true;
			}
		} catch (JavaModelException e) {
			StudioLogger.error(ElementTreeValidator.class, e.getLocalizedMessage(), e);
		}
		return isValid;
	}
}
