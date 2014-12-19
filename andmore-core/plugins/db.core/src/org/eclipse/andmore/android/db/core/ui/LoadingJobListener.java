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
package org.eclipse.andmore.android.db.core.ui;

import java.util.List;

import org.eclipse.andmore.android.common.utilities.PluginUtils;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.ui.view.MOTODEVDatabaseExplorerView;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

/**
 * This class is responsible to set the loading attribute on a tree node when a
 * loadingJob is scheduled and set it to false before updating the treeView
 * after the loadingJob finished execution. The node child count is update on
 * job done. TreeView contentProvider will take care of the rest.
 */
public final class LoadingJobListener extends JobChangeAdapter {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.jobs.JobChangeAdapter#scheduled(org.eclipse.
	 * core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void scheduled(IJobChangeEvent event) {
		Job job = event.getJob();
		if (job instanceof AbstractLoadingNodeJob) {
			AbstractLoadingNodeJob loadingNodeJob = (AbstractLoadingNodeJob) job;
			ITreeNode node = loadingNodeJob.getNode();

			// Since load job has been scheduled, the loading flag must be set
			// to true (This will help the content provider to show the loading
			// node).
			node.setLoading(true);
		}
		super.scheduled(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.
	 * runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void done(IJobChangeEvent event) {
		MOTODEVDatabaseExplorerView view = DbCoreActivator.getMOTODEVDatabaseExplorerView();
		if (view != null) {
			final TreeViewer treeViewer = view.getTreeViewer();
			Job job = event.getJob();
			if (job instanceof AbstractLoadingNodeJob) {
				AbstractLoadingNodeJob loadingNodeJob = (AbstractLoadingNodeJob) job;
				ITreeNode node = loadingNodeJob.getNode();

				// Job is done, so we can set the loading flag to false.
				node.setLoading(false);
				final ITreeNode[] treeNodeContainer = { node };

				// TreeViewer operations must be executed on the UI Thread.
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						ITreeNode node = treeNodeContainer[0];
						List<ITreeNode> children = node.getChildren();
						if (PluginUtils.getOS() != PluginUtils.OS_LINUX) {
							treeViewer.setChildCount(node, 0);
						}
						int size = children.size();
						if (size > 0) {
							((ILazyTreeContentProvider) treeViewer.getContentProvider()).updateElement(node, 0); // Force
																													// removal
																													// of
																													// loading
																													// node.
						}
						// updating the child count is sufficient to allow the
						// tree to call the content provider and retrieve the
						// new nodes.
						treeViewer.setChildCount(node, size);
						// updating the node so if needed the label/icon will be
						// updated
						treeViewer.update(node, null);
					}
				});
			}
			super.done(event);
		}
	}
}