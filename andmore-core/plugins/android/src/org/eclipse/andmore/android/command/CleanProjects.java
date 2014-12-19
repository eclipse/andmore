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

package org.eclipse.andmore.android.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Handler responsible for cleaning selected projects by the user.
 */
public class CleanProjects extends AbstractHandler {

	/**
	 * Job responsible for cleaning a list of projects
	 */
	private final class CleanProjectsJob extends Job {

		private List<IProject> projectList = null;

		/**
		 * @param name
		 * @param stream
		 * @param sdkPath
		 */
		private CleanProjectsJob(List<IProject> projects) {
			super(AndroidNLS.UI_CleanProjectsJob_Name);
			projectList = projects;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(AndroidNLS.UI_CleanProjectsJob_Description, projectList.size());

			for (IProject p : projectList) {
				// Try to clean it and update progress
				try {
					p.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
					monitor.worked(1);
				} catch (CoreException e) {
					// Just log the error. Not much we can do about it.
					StudioLogger.error(CleanProjectsJob.class, "Error cleaning project " + p.getName() + ". ", e);
				}
			}

			monitor.done();
			return Status.OK_STATUS;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// Retrieve the selection used in the command
		IWorkbench workbench = PlatformUI.getWorkbench();
		if ((workbench != null) && !workbench.isClosing()) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				ISelection selection = window.getSelectionService().getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sselection = (IStructuredSelection) selection;
					Iterator<?> it = sselection.iterator();

					// Construct a list of valid projects to be cleaned
					List<IProject> projectList = new ArrayList<IProject>(sselection.size());

					while (it.hasNext()) {
						Object resource = it.next();

						// Check if the selected item is a project
						if (resource instanceof IProject) {
							projectList.add(((IProject) resource));
						} else if (resource instanceof IAdaptable) {
							IAdaptable adaptable = (IAdaptable) resource;
							projectList.add((IProject) adaptable.getAdapter(IProject.class));

						}

					}

					// Instantiate the clean job and schedule it.
					if (projectList.size() > 0) {
						CleanProjectsJob job = new CleanProjectsJob(projectList);
						job.schedule();
					}

				}
			}
		}
		return null;
	}

}
