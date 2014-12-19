/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer;

import com.android.hierarchyviewerlib.HierarchyViewerDirector;
import com.android.hierarchyviewerlib.device.IHvDevice;
import com.android.hierarchyviewerlib.models.Window;

import org.eclipse.andmore.hierarchyviewer.views.PixelPerfectTreeView;
import org.eclipse.andmore.hierarchyviewer.views.PropertyView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

public class HierarchyViewerPluginDirector extends HierarchyViewerDirector {

	public static HierarchyViewerDirector createDirector() {
		return sDirector = new HierarchyViewerPluginDirector();
	}

	@Override
	public void executeInBackground(final String taskName, final Runnable task) {
		Job job = new Job(taskName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				task.run();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT);
		job.setRule(mSchedulingRule);
		job.schedule();
	}

	private ISchedulingRule mSchedulingRule = new ISchedulingRule() {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}

	};

	@Override
	public String getAdbLocation() {
		return HierarchyViewerPlugin.getPlugin().getPreferenceStore().getString(HierarchyViewerPlugin.ADB_LOCATION);
	}

	@Override
	public void loadViewTreeData(Window window) {
		super.loadViewTreeData(window);

		// The windows tab hides the property tab, so let's bring the property
		// tab
		// forward.

		IWorkbenchWindow[] windows = HierarchyViewerPlugin.getPlugin().getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow currentWindow : windows) {
			IWorkbenchPage page = currentWindow.getActivePage();
			if (page.getPerspective().getId().equals(TreeViewPerspective.ID)) {
				try {
					IWorkbenchPart part = page.findView(PropertyView.ID);
					if (part != null) {
						page.showView(PropertyView.ID);
					}
				} catch (PartInitException e) {

				}
			}
		}
	}

	@Override
	public void loadPixelPerfectData(IHvDevice device) {
		super.loadPixelPerfectData(device);

		// The windows tab hides the tree tab, so let's bring the tree tab
		// forward.

		IWorkbenchWindow[] windows = HierarchyViewerPlugin.getPlugin().getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			IWorkbenchPage page = window.getActivePage();
			if (page.getPerspective().getId().equals(PixelPerfectPespective.ID)) {
				try {
					IWorkbenchPart part = page.findView(PixelPerfectTreeView.ID);
					if (part != null) {
						page.showView(PixelPerfectTreeView.ID);
					}
				} catch (PartInitException e) {

				}
			}
		}
	}
}
