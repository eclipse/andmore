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

package org.eclipse.andmore.android.model;

import java.util.List;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Helper class to create Activity based on Sqlite tables.
 */
public interface IDatabaseSampleActivityParametersWizardCollector {

	public void setDatabaseName(String databaseName);

	public void setTable(Table table);

	public void setSelectedColumns(List<Column> selectedColumns);

	public String getDatabaseName();

	public String getTableName();

	public Table getTable();

	public String getColumnsNames();

	public String getConstColumnsNames();

	public String getCursorValues() throws AndroidException;

	public String getAddColumnsToRow();

	/**
	 * Get import to the package and class name for Sql Open Helper
	 * 
	 * @return import statement
	 */
	public String getImports();

	public void setSqlOpenHelperClassName(String sqlOpenHelperClassName);

	public void setSqlOpenHelperPackageName(String sqlOpenHelperPackageName);

	public String getSqlOpenHelperClassName();

	public boolean createOpenHelper();

	public void setCreateOpenHelper(boolean createOpenHelper);

	public String getReadableDatabase();

	/**
	 * Add pages that contributes to fill parameters to create activity sample
	 * 
	 * @return
	 */
	public List<IWizardPage> getWizardPages();

	/**
	 * Creates Sql Open Helper required to transfer db file and make the
	 * activity work correctly
	 * 
	 * @param project
	 * @param monitor
	 */
	public void createSqlOpenHelper(IProject project, IProgressMonitor monitor);
}
