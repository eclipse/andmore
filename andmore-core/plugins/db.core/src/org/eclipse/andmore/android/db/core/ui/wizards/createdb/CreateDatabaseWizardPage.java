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

package org.eclipse.andmore.android.db.core.ui.wizards.createdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.wizards.CreateTableWizard;
import org.eclipse.andmore.android.db.core.ui.wizards.TableLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class CreateDatabaseWizardPage extends WizardPage {

	private Text databaseName;

	private Button addButton = null;

	private Button editButton = null;

	private Button removeButton = null;

	private boolean isPageComplete = false;

	// private final String DATABASE_CONTEXT_HELP_ID = DbPlugin.PLUGIN_ID +
	// ".create_database_wizard";

	/**
	 * This page's tree viewer
	 */
	private TreeViewer viewer;

	/**
	 * Tree viewer input
	 */
	private final TreeNode[] treeNodeArray = new TreeNode[0];

	private final List<String> alreadyAvailableDbs;

	private final List<TableModel> tables = new ArrayList<TableModel>();

	private final String DATABASE_CONTEXT_HELP_ID = DbCoreActivator.PLUGIN_ID + ".create_database_wizard"; //$NON-NLS-1$

	/**
	 * @param alreadyAvailableDbs
	 * @param pageName
	 */
	protected CreateDatabaseWizardPage(final List<String> alreadyAvailableDbs) {
		super(DbCoreNLS.CreateDatabaseWizardPage_UI_PageTitle);
		setTitle(DbCoreNLS.CreateDatabaseWizardPage_UI_CreateNewDatabase);
		setMessage(DbCoreNLS.CreateDatabaseWizardPage_UI_CreateNewDBAddingItsFields);
		this.alreadyAvailableDbs = alreadyAvailableDbs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		composite.setLayout(new GridLayout(2, false));

		GridData layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);

		Composite nameComposite = new Composite(composite, SWT.FILL);
		nameComposite.setLayout(new GridLayout(2, false));
		nameComposite.setLayoutData(layoutData);

		layoutData = new GridData(SWT.LEFT, SWT.NONE, false, false);

		Label dbNameLabel = new Label(nameComposite, SWT.NONE);
		dbNameLabel.setLayoutData(layoutData);
		dbNameLabel.setText(DbCoreNLS.CreateDatabaseWizardPage_DB_Name_Label);

		layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
		databaseName = new Text(nameComposite, SWT.BORDER | SWT.SINGLE);
		databaseName.setLayoutData(layoutData);
		databaseName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
				getContainer().updateButtons();

			}
		});

		Composite emptyComposite = new Composite(composite, SWT.RIGHT);
		emptyComposite.setLayout(new GridLayout(1, false));
		emptyComposite.layout();

		Group tableGroup = new Group(composite, SWT.FILL);

		GridLayout gridLayout = new GridLayout(2, false);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);

		tableGroup.setLayout(gridLayout);
		tableGroup.setLayoutData(gridData);

		tableGroup.setText(DbCoreNLS.CreateDatabaseWizardPage_Table_Group);

		viewer = new TreeViewer(tableGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Set content and label provider
		viewer.setLabelProvider(new TableLabelProvider());
		viewer.setContentProvider(new TreeNodeContentProvider());

		viewer.setInput(treeNodeArray);

		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

		viewer.getTree().setLayoutData(layoutData);

		viewer.addSelectionChangedListener(new TreeViewerListener());

		Composite buttonBar = new Composite(tableGroup, SWT.NONE);
		layoutData = new GridData(SWT.RIGHT, SWT.TOP, false, true);
		buttonBar.setLayoutData(layoutData);

		buttonBar.setLayout(new FillLayout(SWT.VERTICAL));
		addButton = new Button(buttonBar, SWT.PUSH);
		addButton.setText(DbCoreNLS.CreateDatabaseWizardPage_Add_Button);
		addButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean tableAdded = false;

				// loop used to validate the new table name. If it already
				// exists
				// tell the user and open the table wizard again.
				while (!tableAdded) {
					CreateTableWizard createTableWizard = new CreateTableWizard();
					WizardDialog dialog = new WizardDialog(getShell(), createTableWizard);
					dialog.open();
					if (dialog.getReturnCode() == Window.OK) {
						TableModel newTable = createTableWizard.getTable();
						if (newTable != null) {
							boolean tableNameAlreadyExists = false;
							for (TableModel tableModel : tables) {
								if (tableModel.getName().equalsIgnoreCase(newTable.getName())) {
									tableNameAlreadyExists = true;
									break;
								}
							}
							if (!tableNameAlreadyExists) {
								tables.add(newTable);

								ArrayList<TreeNode> treeNodeColletion = new ArrayList<TreeNode>();
								treeNodeColletion.addAll(Arrays.asList((TreeNode[]) viewer.getInput()));
								TreeNode treeNode = new TreeNode(newTable);
								treeNodeColletion.add(treeNode);
								viewer.setInput(treeNodeColletion.toArray(new TreeNode[0]));
								tableAdded = true;
							} else {
								MessageDialog.openError(
										getShell(),
										DbCoreNLS.CreateDatabaseWizardPage_Table_Already_Exists_Title,
										NLS.bind(DbCoreNLS.CreateDatabaseWizardPage_Table_Already_Exists_Msg,
												newTable.getName()));
							}
						}
					} else {
						break;
					}
				}
			}

		});

		editButton = new Button(buttonBar, SWT.PUSH);
		editButton.setText(DbCoreNLS.CreateDatabaseWizardPage_Edit_Button);
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeNode selectedNode = null;

				if (viewer.getSelection() instanceof ITreeSelection) {
					ITreeSelection treeSelection = (ITreeSelection) viewer.getSelection();
					selectedNode = (TreeNode) treeSelection.getFirstElement();
					TableModel table = (TableModel) selectedNode.getValue();

					CreateTableWizard createTableWizard = new CreateTableWizard();
					createTableWizard.init(table);
					WizardDialog dialog = new WizardDialog(getShell(), createTableWizard);
					dialog.open();
					TableModel newTable = createTableWizard.getTable();
					if (newTable != null) {
						tables.add(newTable);
					}
					viewer.refresh();
				}
			}
		});

		removeButton = new Button(buttonBar, SWT.PUSH);
		removeButton.setText(DbCoreNLS.CreateDatabaseWizardPage_Remove_Button);
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<TreeNode> treeNodeColletion = new ArrayList<TreeNode>();
				treeNodeColletion.addAll(Arrays.asList((TreeNode[]) viewer.getInput()));

				TreeNode selectedNode = null;

				if (viewer.getSelection() instanceof ITreeSelection) {
					ITreeSelection treeSelection = (ITreeSelection) viewer.getSelection();
					selectedNode = (TreeNode) treeSelection.getFirstElement();

					treeNodeColletion.remove(selectedNode);
					viewer.setInput(treeNodeColletion.toArray(new TreeNode[0]));
				}
			}
		});

		composite.pack();
		composite.layout();
		setPageComplete(false);
		setErrorMessage(null);

		setControl(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DATABASE_CONTEXT_HELP_ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, DATABASE_CONTEXT_HELP_ID);
	}

	/**
	 * Validates the database name.
	 */
	private void validatePage() {
		DatabaseCreationFieldValidator validator = new DatabaseCreationFieldValidator(alreadyAvailableDbs);
		String errorMessage = validator.isValid(getDatabaseName());
		if (errorMessage != null) {
			setErrorMessage(errorMessage);
			isPageComplete = false;
		} else {
			setErrorMessage(null);
			isPageComplete = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return isPageComplete;
	}

	/**
	 * Return the database name that is in databaseName field.
	 * 
	 * @return
	 */
	public String getDatabaseName() {
		return databaseName.getText().trim();
	}

	/**
	 * Return tables that are in the tree viewer.
	 * 
	 * @return
	 */
	public List<TableModel> getTables() {

		List<TreeNode> treeNodeColletion = new ArrayList<TreeNode>();
		treeNodeColletion.addAll(Arrays.asList((TreeNode[]) viewer.getInput()));

		List<TableModel> tableCollection = new ArrayList<TableModel>();
		for (TreeNode node : treeNodeColletion) {
			tableCollection.add((TableModel) node.getValue());
		}
		return tableCollection;
	}

	/**
	 * Selection listener for the tree viewer
	 */
	class TreeViewerListener implements ISelectionChangedListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged
		 * (org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() instanceof ITreeSelection) {
				ITreeSelection treeSelection = (ITreeSelection) event.getSelection();

				if (!treeSelection.isEmpty()) {
					editButton.setEnabled(true);
					removeButton.setEnabled(true);
				} else {
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
				}
			}

		}

	}
}
