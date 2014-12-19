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
package org.eclipse.andmore.android.db.core.workspace;

import java.util.Collection;

import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.project.ProjectNode;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.IRootNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Implements the Workspace Root Node of the database tree view. It listens for
 * changes in workspace such as project deletion/open/close that reflect in
 * database model.
 */
public class WorkspaceRootNode extends AbstractTreeNode implements IRootNode, IResourceChangeListener {
	class ResourceDeltaVisior implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		@Override
		public boolean visit(IResourceDelta delta) {
			IResource res = delta.getResource();
			if (res instanceof IProject) {
				IProject project = (IProject) res;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					StudioLogger.info("Project added: " + res.getFullPath()); //$NON-NLS-1$
					addProject(project);
					break;
				case IResourceDelta.CHANGED:
					int flags = delta.getFlags();
					if ((flags & IResourceDelta.OPEN) != 0) {
						StudioLogger.info("Project opened: " + res.getFullPath()); //$NON-NLS-1$
						addProject(project);
					}
					break;
				}
			}

			return true; // visit the children
		}
	}

	public WorkspaceRootNode() {
		// listener for project changes
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		clear();
		loadContent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return getSpecificIcon("org.eclipse.jdt.ui", //$NON-NLS-1$
				"icons/full/elcl16/prj_mode.gif"); //$NON-NLS-1$
	}

	/**
	 * Loads the Android projects from the current workspace
	 * 
	 * @return list of project nodes
	 */
	public Collection<ITreeNode> loadContent() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			addProject(project);
		}
		return this.getChildren();
	}

	/**
	 * Adds a new project in the workspace and notifies the event
	 * 
	 * @param project
	 * @throws CoreException
	 */
	public void addProject(IProject project) {
		ProjectNode projectNode = null;
		try {
			if ((project != null) && (project.isOpen())
					&& (project.getNature(IAndroidConstants.ANDROID_NATURE) != null)) {
				projectNode = new ProjectNode(project, this);
				putChild(projectNode);
			}
		} catch (CoreException e) {
			StudioLogger.error(WorkspaceRootNode.class,
					"Unable to retrieve nature from project:" + project.getName(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Removes the project from workspace and notifies the event
	 * 
	 * @param project
	 */
	public void removeProject(IProject project) {
		ITreeNode projectNode = getChildById(project.getName());
		if (projectNode != null) {
			removeChild(projectNode);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org
	 * .eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource res = event.getResource();
		IProject project = null;
		switch (event.getType()) {
		case IResourceChangeEvent.PRE_CLOSE:
		case IResourceChangeEvent.PRE_DELETE:
			// project being closed or deleted => remove project from model
			StudioLogger.info("Project about to close/delete: " + res.getFullPath()); //$NON-NLS-1$
			project = res.getProject();
			removeProject(project);
			break;
		case IResourceChangeEvent.POST_CHANGE:
			try {
				event.getDelta().accept(new ResourceDeltaVisior());
			} catch (CoreException e) {
				StudioLogger.error(WorkspaceRootNode.class, "Error listening to changes in resources", e); //$NON-NLS-1$
			}
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
