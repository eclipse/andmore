/*
 * Copyright (C) 2010, 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.TemplateEngine;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.io.File;
import java.util.Map;

public class NdkManager {

	public static final String NDK_LOCATION = "ndkLocation"; //$NON-NLS-1$

	public static final String LIBRARY_NAME = "libraryName"; //$NON-NLS-1$

	public static String getNdkLocation() {
		return Activator.getDefault().getPreferenceStore().getString(NDK_LOCATION);
	}

	public static boolean isNdkLocationValid() {
		String location = getNdkLocation();
		if (location.length() == 0)
			return false;

		return isValidNdkLocation(location);
	}

	public static boolean isValidNdkLocation(String location) {
		File dir = new File(location);
		if (!dir.isDirectory())
			return false;

		// Must contain the ndk-build script which we call to build
		if (!new File(dir, "ndk-build").isFile()) //$NON-NLS-1$
			return false;

		return true;
	}

	public static void addNativeSupport(final IProject project, Map<String, String> templateArgs,
			IProgressMonitor monitor) throws CoreException {
		// Launch our template to set up the project contents
		TemplateCore template = TemplateEngine.getDefault().getTemplateById("AddNdkSupport"); //$NON-NLS-1$
		Map<String, String> valueStore = template.getValueStore();
		valueStore.put("projectName", project.getName()); //$NON-NLS-1$
		valueStore.putAll(templateArgs);
		template.executeTemplateProcesses(monitor, false);

		// refresh project resources
		project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 10));
	}

}
