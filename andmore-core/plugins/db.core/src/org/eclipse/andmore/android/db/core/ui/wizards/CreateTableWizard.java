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

import java.util.Set;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Class that represents the Create Table Wizard
 */
public class CreateTableWizard extends Wizard {
	private TableModel table = null;

	private CreateTableWizardPage tableWizardPage = null;

	private Set<String> notAllowedNames = null;

	private static final String WIZARD_BANNER = "icons/wizban/create_table.png"; //$NON-NLS-1$

	public CreateTableWizard() {
		setWindowTitle(DbCoreNLS.CreateTableWizardPage_UI_PageTitle);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID,
				WIZARD_BANNER));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		table = tableWizardPage.getTable();
		boolean isOK = true;
		if ((notAllowedNames != null) && notAllowedNames.contains(tableWizardPage.getTable().getName().toUpperCase())) {
			MessageDialog.openError(getShell(), DbCoreNLS.CreateTableWizard_UI_Message_ErrorCreatingTable,
					DbCoreNLS.ERR_CreateDatabaseWizardPage_TableAlreadyExistTitle);
			isOK = false;
		}

		return isOK;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		tableWizardPage = new CreateTableWizardPage();
		addPage(tableWizardPage);

		if (table != null) {
			tableWizardPage.setTable(table);
		}
	}

	/**
	 * Used when the user wants to retrieve a table object and not create the
	 * table itself
	 * 
	 * @param table
	 */
	public void init(TableModel table) {
		this.table = table;
	}

	/**
	 * @return the table
	 */
	public TableModel getTable() {
		return table;
	}

	public void setNotAllowedNames(Set<String> notAllowedNames) {
		this.notAllowedNames = notAllowedNames;

	}

}
