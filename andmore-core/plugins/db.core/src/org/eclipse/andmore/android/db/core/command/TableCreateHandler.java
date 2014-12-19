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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.ITableCreatorNode;
import org.eclipse.andmore.android.db.core.ui.wizards.CreateTableWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class TableCreateHandler extends AbstractHandler {
	private ITableCreatorNode tableCreatorNode = null;

	public TableCreateHandler() {
	}

	public TableCreateHandler(ITableCreatorNode tableCreatorNode) {
		this.tableCreatorNode = tableCreatorNode;
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
		if (tableCreatorNode == null) {
			tableCreatorNode = getSelectedItem();
		}

		// tableCreatorNode may be null if the action come from toolbar
		// and the selected item is not an ITableCreatorNode
		if (tableCreatorNode != null) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			boolean tableAdded = false;

			// loop used to validate the new table name. If it already exists
			// tell the user and open the table wizard again.
			while (!tableAdded) {
				// repeat while table not added and dialog not cancelled
				CreateTableWizard createTableWizard = new CreateTableWizard();
				Set<String> notAllowedNames = getNotAllowedNames(tableCreatorNode.getTables());
				createTableWizard.setNotAllowedNames(notAllowedNames);
				WizardDialog dialog = new WizardDialog(shell, createTableWizard);
				dialog.open();

				if (dialog.getReturnCode() == Window.OK) {
					TableModel newTable = createTableWizard.getTable();
					if (newTable != null) {
						boolean tableNameAlreadyExists = false;
						for (Table table : tableCreatorNode.getTables()) {
							if (table.getName().equalsIgnoreCase(newTable.getName())) {
								tableNameAlreadyExists = true;
								break;
							}
						}
						if (!tableNameAlreadyExists) {
							tableCreatorNode.createTable(newTable);
							tableCreatorNode = null; // clear selected node to
														// force getSelectedItem
														// to be called when
														// calling via toolbar
							tableAdded = true;
						} else {
							// notify error that table already exists
							MessageDialog.openError(
									PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
									DbCoreNLS.CreateDatabaseWizardPage_Table_Already_Exists_Title,
									NLS.bind(DbCoreNLS.CreateDatabaseWizardPage_Table_Already_Exists_Msg,
											newTable.getName()));
						}
					}
				} else {
					// exit the loop if the user cancel dialog
					break;
				}
			}

		}

		return null;
	}

	private ITableCreatorNode getSelectedItem() {
		ITableCreatorNode selectedNode = null;
		ITreeNode selectedItem = DbCoreActivator.getMOTODEVDatabaseExplorerView().getSelectedItemOnTree();

		if (selectedItem instanceof ITableCreatorNode) {
			selectedNode = (ITableCreatorNode) selectedItem;
		}

		return selectedNode;
	}

	private Set<String> getNotAllowedNames(List<Table> list) {

		Set<String> names = new HashSet<String>();

		for (Table table : list) {
			names.add(table.getName().toUpperCase());
		}
		return names;
	}
}
