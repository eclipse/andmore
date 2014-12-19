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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.project.ProjectNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode;
import org.eclipse.andmore.android.db.core.ui.view.MOTODEVDatabaseExplorerView;
import org.eclipse.andmore.android.db.core.ui.wizards.createdb.CreateDatabaseWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DbCreateHandler extends AbstractHandler implements IHandler {
	private IDbCreatorNode dbCreatorNode;

	/**
	 * @param dbCreatorNode
	 */
	public DbCreateHandler() {
		setDbCreatorNode();
	}

	/**
	 * @param dbCreatorNode
	 */
	public DbCreateHandler(IDbCreatorNode dbCreatorNode) {
		this.dbCreatorNode = dbCreatorNode;
	}

	private void setDbCreatorNode() {

		this.dbCreatorNode = null;

		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActivePart();

		MOTODEVDatabaseExplorerView view = DbCoreActivator.getMOTODEVDatabaseExplorerView();

		if ((view != null) && activePart.equals(view)) {
			ITreeNode items = view.getSelectedItemOnTree();

			if (items != null) {

				if (items instanceof IDbCreatorNode) {

					this.dbCreatorNode = (IDbCreatorNode) items;

					ITreeNode node = view.getSelectedItemOnTree();

					if ((node != null)) {
						try {
							IDbCreatorNode dbCreatorNode = (IDbCreatorNode) node;

							if (node instanceof IDbCreatorNode) {
								this.dbCreatorNode = dbCreatorNode;
							}
						} catch (Exception e) {
							StudioLogger.info(DbCreateHandler.class, e.getMessage());
						}

					}
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
								StudioLogger.error(DbCreateHandler.class, e.getMessage());
							}
						}

						if (resource != null) {
							this.dbCreatorNode = new ProjectNode(resource.getProject(), null);
						}
					}

				}
			}
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
		setDbCreatorNode();

		if (this.dbCreatorNode != null) {
			List<ITreeNode> children = dbCreatorNode.getChildren();
			List<String> alreadyAvailableDbs;
			alreadyAvailableDbs = new ArrayList<String>(children.size());
			for (ITreeNode child : children) {
				alreadyAvailableDbs.add(child.getName());
			}

			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			CreateDatabaseWizard createDbWizard = new CreateDatabaseWizard(alreadyAvailableDbs);
			WizardDialog dialog = new WizardDialog(shell, createDbWizard);
			dialog.create();
			dialog.open();

			String dbName = createDbWizard.getDbName();
			List<TableModel> tables = createDbWizard.getTables();
			IStatus status = null;
			try {
				status = dbCreatorNode.createDb(dbName, tables);
				if (dbCreatorNode instanceof ProjectNode) {
					((ProjectNode) dbCreatorNode).refreshAssetsFolder();
				}
			} catch (Exception e) {
				StudioLogger.error(DbCreateHandler.class, e.getMessage());
			}
			if ((status != null) && (!status.isOK())) {
				EclipseUtils.showErrorDialog(DbCoreNLS.UI_CreateDatabaseWizardPage_CreateDatabase_Error,
						NLS.bind(DbCoreNLS.UI_CreateDatabaseWizardPage_CreateDatabase_Error_New, dbName), status);
			}

		}
		return null;
	}

}
