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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditorInput;
import org.eclipse.datatools.sqltools.data.internal.ui.extract.ExtractDataWizard;
import org.eclipse.datatools.sqltools.data.internal.ui.load.LoadDataWizard;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TableNode extends AbstractTreeNode implements ITableNode {

	private final Table table;

	private final DbModel model;

	/**
	 * @param table
	 * @param parent
	 */
	public TableNode(Table table, DbModel dbModel, IDbNode parent) {
		super(table.getName(), table.getName(), parent);
		this.table = table;
		this.model = dbModel;
	}

	@Override
	public IStatus browseTableContents() {
		IStatus browseTableContentsStatus = new Status(IStatus.OK, DbCoreActivator.PLUGIN_ID,
				DbCoreNLS.TableNode_BrowsingTableContentsSuccessStatus);
		final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {
			workbenchPage.openEditor(new TableDataEditorInput(table),
					"org.eclipse.datatools.sqltools.data.internal.ui.editor.tableDataEditor"); //$NON-NLS-1$

		} catch (PartInitException e) {
			// Display error message!
			browseTableContentsStatus = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID,
					DbCoreNLS.TableNode_BrowsingTableContentsErrorStatus, e);
		}
		return browseTableContentsStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void refresh() {
		clear();

		EList<Column> columns = table.getColumns();
		List<ITreeNode> columnNodes = new ArrayList<ITreeNode>(columns.size());
		for (Column column : columns) {
			ColumnNode columnNode = new ColumnNode(column, model, this);
			columnNodes.add(columnNode);
		}
		putChildren(columnNodes);
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
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return getSpecificIcon("org.eclipse.datatools.connectivity.sqm.core.ui", //$NON-NLS-1$
				"icons/table.gif"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITableNode#sampleDbContents()
	 */
	@Override
	public void sampleDbContents() {
		model.sampleContents(table);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITableNode#extractData()
	 */
	@Override
	public void extractData() {
		ExtractDataWizard wiz = new ExtractDataWizard(table);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wiz);
		dialog.create();
		dialog.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITableNode#loadData()
	 */
	@Override
	public void loadData() {
		LoadDataWizard wiz = new LoadDataWizard(table);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wiz);
		dialog.create();
		dialog.open();
	}
}
