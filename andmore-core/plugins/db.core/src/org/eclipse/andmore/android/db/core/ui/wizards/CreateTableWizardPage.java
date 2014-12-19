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
package org.eclipse.andmore.android.db.core.ui.wizards;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.Field;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class CreateTableWizardPage extends WizardPage {
	private TableViewer viewer;

	private Text tableName;

	private TableModel table = null;

	private final String TABLE_CONTEXT_HELP_ID = DbCoreActivator.PLUGIN_ID + ".create_table_wizard"; //$NON-NLS-1$

	protected CreateTableWizardPage() {
		super(DbCoreNLS.CreateTableWizardPage_UI_PageTitle);
		setTitle(DbCoreNLS.CreateTableWizardPage_UI_CreateNewTable);
		setMessage(DbCoreNLS.CreateTableWizardPage_UI_CreateNewTableAddingItsFields);
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

		Label tableNameLabel = new Label(nameComposite, SWT.NONE);
		tableNameLabel.setLayoutData(layoutData);
		tableNameLabel.setText(DbCoreNLS.CreateTableWizardPage_UI_TableName);

		layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
		tableName = new Text(nameComposite, SWT.BORDER | SWT.SINGLE);
		tableName.setLayoutData(layoutData);
		tableName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (viewer != null) {
					viewer.refresh();

					validatePage();

					((TableModel) viewer.getInput()).setName(tableName.getText());
				}

			}
		});

		if ((table != null) && (table.getName() != null)) {
			tableName.setText(table.getName());
		}

		Composite emptyComposite = new Composite(composite, SWT.RIGHT);
		emptyComposite.setLayout(new GridLayout(1, false));
		emptyComposite.layout();

		viewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.FULL_SELECTION);
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(DbCoreNLS.CreateTableWizardPage_UI_Name);
		column.getColumn().setResizable(true);
		column.getColumn().setWidth(100);

		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(DbCoreNLS.CreateTableWizardPage_UI_Type);
		column.getColumn().setResizable(true);
		column.getColumn().setWidth(100);

		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(DbCoreNLS.CreateTableWizardPage_UI_Default);
		column.getColumn().setResizable(true);
		column.getColumn().setWidth(100);

		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(DbCoreNLS.CreateTableWizardPage_UI_Primary);
		column.getColumn().setResizable(true);
		column.getColumn().setWidth(100);

		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setLayoutData(layoutData);

		viewer.setContentProvider(new TableWizardContentProvider());
		viewer.setLabelProvider(new TableWizardLabelProvider());

		Composite buttonBar = new Composite(composite, SWT.NONE);
		layoutData = new GridData(SWT.RIGHT, SWT.TOP, false, true);
		buttonBar.setLayoutData(layoutData);

		buttonBar.setLayout(new FillLayout(SWT.VERTICAL));
		Button add = new Button(buttonBar, SWT.PUSH);
		add.setText(DbCoreNLS.CreateTableWizardPage_UI_Add);
		add.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				AddTableFieldDialog diag = new AddTableFieldDialog(getShell());

				if (diag.open() == Window.OK) {
					((TableModel) viewer.getInput()).addField(diag.getField());
					viewer.refresh();
					validatePage();
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		final Button edit = new Button(buttonBar, SWT.PUSH);
		edit.setText(DbCoreNLS.CreateTableWizardPage_UI_Edit);
		edit.setEnabled(false);
		edit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (viewer.getTable().getSelectionCount() == 1) {
					AddTableFieldDialog diag = new AddTableFieldDialog(getShell(), ((TableModel) viewer.getInput())
							.getFields().get(viewer.getTable().getSelectionIndex()));
					if (diag.open() == Window.OK) {
						viewer.update(diag.getField(), null);
						validatePage();
					}

				}

			}
		});

		final Button remove = new Button(buttonBar, SWT.PUSH);
		remove.setText(DbCoreNLS.CreateTableWizardPage_UI_Remove);
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection structuredSelection = (IStructuredSelection) selection;

					for (Object obj : structuredSelection.toList()) {
						if (obj instanceof Field) {
							((TableModel) viewer.getInput()).removeField(((Field) obj));
						}
					}
					viewer.refresh();
					validatePage();

				}
			}
		});

		if (table == null) {
			table = new TableModel();
		}
		viewer.setInput(table);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int selectionCount = viewer.getTable().getSelectionCount();
				remove.setEnabled(selectionCount > 0);
				edit.setEnabled(selectionCount == 1);

			}
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (viewer.getTable().getSelectionCount() == 1) {
					AddTableFieldDialog diag = new AddTableFieldDialog(getShell(), ((TableModel) viewer.getInput())
							.getFields().get(viewer.getTable().getSelectionIndex()));
					if (diag.open() == Window.OK) {

						viewer.update(diag.getField(), null);
						validatePage();
					}

				}

			}
		});
		viewer.refresh();

		composite.pack();
		composite.layout();
		setPageComplete(false);
		setErrorMessage(null);

		setControl(composite);

		// table fields will be not empty at this point when user is editing a
		// table, so page must be validated to enable finish button
		if (!table.getFields().isEmpty()) {
			validatePage();
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, TABLE_CONTEXT_HELP_ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, TABLE_CONTEXT_HELP_ID);
	}

	public void setTable(TableModel table) {
		this.table = table;
	}

	public TableModel getTable() {
		return table;
	}

	public String getTableName() {
		return tableName.getText();
	}

	private void validatePage() {
		String errMsg = null;

		if (tableName.getText().trim().length() == 0) {
			errMsg = DbCoreNLS.CreateTableWizardPage_UI_TableNameCannotBeEmpty;
		}

		if (((errMsg == null) && (tableName.getText().trim().contains(" ")))) //$NON-NLS-1$
		{
			errMsg = DbCoreNLS.CreateTableWizardPage_UI_InvalidTableName;
		}

		// Validate table name to don't use sqlite keyword
		if (!TableModel.validateName(tableName.getText())) {
			errMsg = DbCoreNLS.CreateTableWizardPage_UI_InvalidTableName;
		}

		if ((errMsg == null) && (viewer.getTable().getItemCount() == 0)) {
			errMsg = DbCoreNLS.CreateTableWizardPage_UI_YouMustSupplyAtLeastOneField;
		}
		if (errMsg == null) {
			errMsg = ((TableModel) viewer.getInput()).getErrorMessage();
		}

		setErrorMessage(errMsg);
		setPageComplete(errMsg == null);

	}

}
