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
package org.eclipse.andmore.android.db.core.ui.action;

import org.eclipse.andmore.android.codeutils.wizards.DatabaseManagementClassesCreationWizard;
import org.eclipse.andmore.android.db.core.project.ProjectNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * This class is responsible for executing the action of creating the classes
 * responsible for deploying a database file automatically.
 */
public class CreateDatabaseManagementClassesAction extends Action {

	private IDbNode dbNodeSelected;

	private ProjectNode dbProjectNodeSelected;

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		IResource resource = null;
		IWorkbench workbench = PlatformUI.getWorkbench();
		if ((workbench != null) && !workbench.isClosing()) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				if (dbNodeSelected != null) {
					// db node selected => get parent to retrieve the resource
					// or the project (if the resource does not exist)
					if (dbNodeSelected.getParent() instanceof ProjectNode) {
						ProjectNode pNode = (ProjectNode) dbNodeSelected.getParent();
						resource = ResourcesPlugin.getWorkspace().getRoot().getProject(pNode.getName())
								.getFile(ProjectNode.DB_FOLDER + IPath.SEPARATOR + dbNodeSelected.getName());
						if (!resource.exists()) {
							resource = resource.getProject();
						}
					}
				} else if (dbProjectNodeSelected != null) {

					ProjectNode pNode = dbProjectNodeSelected;
					resource = ResourcesPlugin.getWorkspace().getRoot().getProject(pNode.getName())
							.getFile(ProjectNode.DB_FOLDER + IPath.SEPARATOR + dbProjectNodeSelected.getName());
					if (!resource.exists()) {
						resource = resource.getProject();
					}
				} else {
					// db node not selected on tree => try to get resource based
					// on selection from package explorer
					Object selectionElement = getSelectionElement(window);
					if (selectionElement == null) {
						// the wizard was requested to open from MOTODEV menu -
						// open wizard without selecting project or .db file
						resource = null;
					} else {
						// there is an item selected, get the resource
						// associated
						resource = getResourceFromSelection(selectionElement);
					}

				}
				openDialogBasedOnResourceSelected(resource, window);
			}
		}

	}

	/**
	 * Opens dialog based on the resource selected
	 * 
	 * @param resource
	 * @param window
	 */
	private void openDialogBasedOnResourceSelected(IResource resource, IWorkbenchWindow window) {
		WizardDialog dialog = null;
		if (resource != null) {
			// in case there is a resource, go on
			if (resource instanceof IFile) {
				// get wizard for database file
				dialog = new WizardDialog(window.getShell(), new DatabaseManagementClassesCreationWizard(
						resource.getProject(), resource));
			} else {
				// get wizard with no database file
				dialog = new WizardDialog(window.getShell(), new DatabaseManagementClassesCreationWizard(
						resource.getProject(), null));
			}
		} else {
			// resource is null, set the wizard with nothing selected
			dialog = new WizardDialog(window.getShell(), new DatabaseManagementClassesCreationWizard(null, null));
		}
		if (dialog != null) {
			// open the wizard
			dialog.open();
		}
	}

	/**
	 * Get the resource based on the selection of item inside workbench
	 * 
	 * @param selectionElement
	 * @return
	 */
	private IResource getResourceFromSelection(Object selectionElement) {
		// the wizard was requested from a click on .db file or project
		IResource resource = null;
		// in case the item as a resource, retrieve it
		if (selectionElement instanceof IResource) {
			resource = (IResource) selectionElement;
		}
		// in case the item is an adaptable, retrieve it
		else if (selectionElement instanceof IAdaptable) {
			try {
				resource = (IResource) ((IAdaptable) selectionElement).getAdapter(IResource.class);
			} catch (Exception e) {
				// Do nothing, return null;
			}
		}
		return resource;
	}

	/**
	 * Get selected item base on selection of item inside workbench
	 * 
	 * @param window
	 * @return
	 */
	private Object getSelectionElement(IWorkbenchWindow window) {
		ISelection selection = window.getSelectionService().getSelection();
		IStructuredSelection structureSelection = null;
		if (selection instanceof IStructuredSelection) {
			structureSelection = (IStructuredSelection) selection;
		} else {
			structureSelection = new StructuredSelection();
		}
		Object selectionElement = structureSelection.getFirstElement();
		return selectionElement;
	}

	/**
	 * @param dbNodeSelected
	 *            the dbNodeSelected to set
	 */
	public void setDbNodeSelected(IDbNode dbNodeSelected) {
		this.dbNodeSelected = dbNodeSelected;
	}

	/**
	 * @return the dbNodeSelected
	 */
	protected IDbNode getDbNodeSelected() {
		return dbNodeSelected;
	}

	public void setProjectNodeSelected(ProjectNode selectedProjectNode) {
		this.dbProjectNodeSelected = selectedProjectNode;
	}
}
