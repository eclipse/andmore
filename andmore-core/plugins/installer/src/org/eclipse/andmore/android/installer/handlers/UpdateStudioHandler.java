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
package org.eclipse.andmore.android.installer.handlers;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.andmore.android.installer.jobs.UpdateStudioJob;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateStudioHandler implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		// Nothing to do

	}

	@Override
	public void dispose() {
		// Nothing to do

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Job progressJob = UpdateStudioJob.getInstance();

		if ((progressJob != null)
				&& ((progressJob.getState() == Job.WAITING) || (progressJob.getState() == Job.RUNNING))) {
			EclipseUtils.showInformationDialog(InstallerNLS.UpdateStudio_UpdateAlreadyRunningTitle,
					InstallerNLS.UpdateStudio_UpdateAlreadyRunningMsg);
		} else {
			progressJob = UpdateStudioJob.createJob(InstallerNLS.UpdateStudio_CheckingForUpdatesJobDescription);
			progressJob.setUser(true);
			progressJob.schedule();
		}

		return null;
	}

	@Override
	public boolean isEnabled() {

		return true;
	}

	@Override
	public boolean isHandled() {

		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		// Nothing to do
	}

}
