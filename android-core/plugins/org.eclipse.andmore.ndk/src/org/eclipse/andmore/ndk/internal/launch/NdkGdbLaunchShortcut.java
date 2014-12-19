/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.ndk.internal.launch;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.launch.AndroidLaunchController;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.ndk.internal.NdkHelper;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

@SuppressWarnings("restriction")
// for adt.internal classes
public class NdkGdbLaunchShortcut implements ILaunchShortcut {
	@Override
	public void launch(ISelection selection, String mode) {
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		Object s = ((IStructuredSelection) selection).getFirstElement();
		if (!(s instanceof IAdaptable)) {
			return;
		}

		IResource r = (IResource) ((IAdaptable) s).getAdapter(IResource.class);
		if (r == null) {
			return;
		}

		IProject project = r.getProject();
		if (project == null) {
			return;
		}

		// verify that this is a non library Android project
		ProjectState state = Sdk.getProjectState(project);
		if (state == null || state.isLibrary()) {
			return;
		}

		// verify that this project has C/C++ nature
		if (!CoreModel.hasCCNature(project) && !CoreModel.hasCNature(project)) {
			AdtPlugin.printErrorToConsole(project,
					String.format("Selected project (%s) does not have C/C++ nature. "
							+ "To add native support, right click on the project, "
							+ "Android Tools -> Add Native Support", project.getName()));
			return;
		}

		debugProject(project, mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
	}

	private void debugProject(IProject project, String mode) {
		// obtain existing native debug config for project
		ILaunchConfiguration config = AndroidLaunchController.getLaunchConfig(project,
				NdkGdbLaunchDelegate.LAUNCH_TYPE_ID);
		if (config == null) {
			return;
		}

		// Set the ndk gdb specific launch attributes in the config (if
		// necessary)
		if (!hasNdkAttributes(config)) {
			try {
				config = setNdkDefaults(config, project);
			} catch (CoreException e) {
				AdtPlugin.printErrorToConsole(project, "Unable to create launch configuration for project.");
				return;
			}
		}

		// launch
		DebugUITools.launch(config, mode);
	}

	private boolean hasNdkAttributes(ILaunchConfiguration config) {
		try {
			// All NDK launch configurations have ATTR_REMOTE_TCP set to true
			boolean isRemote = config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, false);
			return isRemote;
		} catch (CoreException e) {
			return false;
		}
	}

	private ILaunchConfiguration setNdkDefaults(ILaunchConfiguration config, IProject project) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		NdkHelper.setLaunchConfigDefaults(wc);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		return wc.doSave();
	}
}
