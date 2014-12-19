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

import org.eclipse.andmore.internal.launch.MainLaunchConfigTab;
import org.eclipse.andmore.internal.project.ProjectChooserHelper.IProjectChooserFilter;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

@SuppressWarnings("restriction")
public class NdkMainLaunchConfigTab extends MainLaunchConfigTab {
	private static class NdkProjectOnlyFilter implements IProjectChooserFilter {
		@Override
		public boolean accept(IProject project) {
			ProjectState state = Sdk.getProjectState(project);
			if (state == null) {
				return false;
			}

			return !state.isLibrary() && (CoreModel.hasCCNature(project) || CoreModel.hasCNature(project));
		}

		@Override
		public boolean useCache() {
			return true;
		}
	}

	@Override
	protected IProjectChooserFilter getProjectFilter() {
		return new NdkProjectOnlyFilter();
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);

		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, mProjText.getText().trim());
	}
}
