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
package org.eclipse.andmore.android.db.core.command;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.project.ProjectNode;
import org.eclipse.andmore.android.db.core.ui.DbNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.CreateDatabaseManagementClassesAction;
import org.eclipse.andmore.android.db.core.ui.view.MOTODEVDatabaseExplorerView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the command to create database management classes using
 * an instance of {@link IDbNode} object.
 */
public class CreateDatabaseManagementClassesHandler extends AbstractHandler implements IHandler {

	private IDbNode selectedDbNode = null;

	private ProjectNode selectedProjectNode;

	private void setSelectedNode() {

		this.selectedDbNode = null;

		this.selectedProjectNode = null;

		MOTODEVDatabaseExplorerView view = DbCoreActivator.getMOTODEVDatabaseExplorerView();

		if (view != null) {
			// if the Database view is active, looks for the db and project
			// nodes
			ITreeNode items = view.getSelectedItemOnTree();

			if (items != null) {

				if (items instanceof IDbNode) {

					this.selectedDbNode = (IDbNode) items;

				} else if (items instanceof ProjectNode) {
					this.selectedProjectNode = (ProjectNode) items;
				}
			}
		} else {
			// looks for the project and db resources at the package explorer
			// view
			IWorkbench workbench = PlatformUI.getWorkbench();

			if ((workbench != null) && !workbench.isClosing()) {

				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

				if (window != null) {
					ISelection selection = window.getSelectionService().getSelection();
					IStructuredSelection structureSelection = null;
					if (selection instanceof IStructuredSelection) {
						structureSelection = (IStructuredSelection) selection;
					} else {
						structureSelection = new StructuredSelection();
					}

					Object selectionElement = structureSelection.getFirstElement();

					if (selectionElement != null) {

						// the wizard was requested from a click on .db file or
						// project
						IResource resource = null;
						// in case the item as a resource, retrieve it
						if (selectionElement instanceof IResource) {
							resource = (IResource) selectionElement;
						}

						else if (selectionElement instanceof IAdaptable) {
							try {
								resource = (IResource) ((IAdaptable) selectionElement).getAdapter(IResource.class);
							} catch (Exception e) {
								StudioLogger.error(CreateDatabaseManagementClassesAction.class, e.getMessage());
							}
						}

						if (resource != null) {
							this.selectedProjectNode = new ProjectNode(resource.getProject(), null);

							if (resource instanceof IFile) {
								try {
									this.selectedDbNode = new DbNode(new Path(resource.getLocation().toFile()
											.getAbsolutePath()), this.selectedProjectNode);
								} catch (MotodevDbException e) {
									StudioLogger.error(CreateDatabaseManagementClassesAction.class, e.getMessage());
								}
							}
						}
					}

				}
			}
		}
	}

	/**
	 * Constructor when there is NOT a node selected on
	 * {@link MOTODEVDatabaseExplorerView}
	 */
	public CreateDatabaseManagementClassesHandler() {
		setSelectedNode();
	}

	/**
	 * Constructor when there is a node selected on
	 * {@link MOTODEVDatabaseExplorerView}
	 * 
	 * @param node
	 */
	public CreateDatabaseManagementClassesHandler(IDbNode node) {
		this.selectedDbNode = node;
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
		CreateDatabaseManagementClassesAction action = new CreateDatabaseManagementClassesAction();
		setSelectedNode();
		action.setDbNodeSelected(selectedDbNode);

		action.setProjectNodeSelected(selectedProjectNode);

		action.run();

		return null;
	}

}
