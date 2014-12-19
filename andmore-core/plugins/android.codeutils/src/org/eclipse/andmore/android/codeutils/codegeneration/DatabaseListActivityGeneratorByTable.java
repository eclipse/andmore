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

package org.eclipse.andmore.android.codeutils.codegeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.model.IDatabaseSampleActivityParametersWizardCollector;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.datatools.modelbase.sql.datatypes.SQLDataType;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Helper class to create Activity based on Sqlite tables. It uses one file as
 * template and substitutes parameters enclosed by hash-sign (#) Warning: The
 * tables named "ANDROID_METADATA" and "sqlite_sequence" will be ignored as it
 * is an special table in Android apps context.
 */
public class DatabaseListActivityGeneratorByTable implements IDatabaseSampleActivityParametersWizardCollector {

	private static final String CLASS_EXTENTION_TYPE = "Activity";

	private String databaseName = null;

	private String sqlOpenHelperClassName = null;

	private String sqlOpenHelperPackageName = null;

	private boolean createOpenHelper = false;

	private Table table = null;

	private final List<Column> selectedColumns = new ArrayList<Column>();

	// includes only items that are not default, that is with the java cursor
	// type varies from the return type
	private static Map<String, String> SQLTYPE_TO_CURSOR_TYPE_METHOD = new HashMap<String, String>();

	private static Map<String, String> SQLTYPE_TO_RETURN_TYPE = new HashMap<String, String>();

	static {
		SQLTYPE_TO_CURSOR_TYPE_METHOD.put("BLOB", "Byte");
		SQLTYPE_TO_CURSOR_TYPE_METHOD.put("INTEGER", "Int");

		SQLTYPE_TO_RETURN_TYPE.put("CHAR", "String");
		SQLTYPE_TO_RETURN_TYPE.put("VARCHAR", "String");
		SQLTYPE_TO_RETURN_TYPE.put("TEXT", "String");
		SQLTYPE_TO_RETURN_TYPE.put("SMALLINT", "Short");
		SQLTYPE_TO_RETURN_TYPE.put("INTEGER", "Integer");
		SQLTYPE_TO_RETURN_TYPE.put("BIGINT", "Long");
		SQLTYPE_TO_RETURN_TYPE.put("REAL", "Float");
		SQLTYPE_TO_RETURN_TYPE.put("FLOAT", "Double");
		SQLTYPE_TO_RETURN_TYPE.put("DOUBLE", "Double");
		SQLTYPE_TO_RETURN_TYPE.put("BINARY", "byte[]");
		SQLTYPE_TO_RETURN_TYPE.put("VARBINARY", "byte[]");
		SQLTYPE_TO_RETURN_TYPE.put("LONG VARBINARY", "byte[]");
		SQLTYPE_TO_RETURN_TYPE.put("IMAGE", "byte[]");
		SQLTYPE_TO_RETURN_TYPE.put("BLOB", "byte[]");
	}

	/**
	 * @return the table created by this class.
	 * */
	@Override
	public Table getTable() {
		return this.table;
	}

	/**
	 * Sets the initial table to be used.
	 * */
	@Override
	public void setTable(Table table) {
		this.table = table;
	}

	@Override
	public String getTableName() {
		return getTable().getName();
	}

	/**
	 * Authority to access content URI (it is based on package name and content
	 * provider name)
	 * 
	 * @param packageName
	 *            the package name of the authority.
	 * @param activityName
	 *            the activity name of the authority.
	 * @return the authority using package and activity name.
	 */
	protected String getAuthority(String packageName, String activityName) {
		return packageName + "." + activityName.toLowerCase();
	}

	public String getClassName() {
		String className = getTableName() + CLASS_EXTENTION_TYPE;
		return className;
	}

	@Override
	public String getDatabaseName() {
		return this.databaseName;
	}

	@Override
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * @return a string representing a list of the column names.
	 */
	@Override
	public String getColumnsNames() {
		StringBuilder buf = new StringBuilder();
		String columnsResult = "";
		for (int i = 0; i < selectedColumns.size(); i++) {
			int colIndex = i + 1;
			String colSt = "COL_" + colIndex;
			if ((colIndex > 1) && (colIndex <= selectedColumns.size())) {
				colSt = ", " + colSt;
			}
			columnsResult += colSt;
		}
		buf.append(columnsResult);
		return buf.toString();
	}

	/**
	 * @return constants to get on table list
	 */
	@Override
	public String getConstColumnsNames() {
		StringBuilder buf = new StringBuilder();

		ListIterator<Column> columnsIter = selectedColumns.listIterator();
		int index = 1;
		String constSt = "private final String ";
		while (columnsIter.hasNext()) {
			Column column = columnsIter.next();
			String colSt = constSt + "COL_" + index + " = " + "\"" + column.getName() + "\";";
			buf.append("\t" + colSt + "\n");
			index++;
		}
		return buf.toString();
	}

	/**
	 * Creates add columns
	 * 
	 * @throws AndroidException
	 *             when the type of a column is not valid.
	 */
	@Override
	public String getCursorValues() throws AndroidException {
		StringBuilder buf = new StringBuilder();

		ListIterator<Column> columnsIter = selectedColumns.listIterator();
		Integer index = 1;
		String templateSt = "#returnType# col#index# = cursor.get#CursorType#(cursor.getColumnIndex(COL_#index#));";
		while (columnsIter.hasNext()) {
			String cursorSt = templateSt;
			Column column = columnsIter.next();
			String columnName = column.getName().toLowerCase();
			String sqltype = null;
			SQLDataType type = column.getContainedType();
			if (type != null) {
				sqltype = type.getName();
			} else {
				throw new AndroidException("Column " + columnName + " does not have a recognized type");
			}

			String javaReturnType = SQLTYPE_TO_RETURN_TYPE.get(sqltype);
			cursorSt = cursorSt.replace("#returnType#", javaReturnType);
			cursorSt = cursorSt.replaceAll("#index#", index.toString());
			String getType = "";
			if (SQLTYPE_TO_CURSOR_TYPE_METHOD.containsKey(sqltype)) {
				// non-default rule
				getType = SQLTYPE_TO_CURSOR_TYPE_METHOD.get(sqltype);
			} else {
				// default rule - getX where X is the java type
				getType = SQLTYPE_TO_RETURN_TYPE.get(sqltype);
				getType = getType.replace("[]", "");
				// change first letter to upper case
				String firstElem = "" + getType.charAt(0);
				getType = getType.replace(firstElem, firstElem.toUpperCase());
			}
			cursorSt = cursorSt.replaceAll("#CursorType#", getType);
			index++;
			buf.append("\t" + cursorSt + "\n");
		}
		return buf.toString();
	}

	/**
	 * Creates add columns to row statements
	 */
	@Override
	public String getAddColumnsToRow() {
		StringBuilder buf = new StringBuilder();

		String constSt = "addToRow(col#index# , row);";
		for (int i = 0; i < selectedColumns.size(); i++) {
			String replacedText = constSt.replace("#index#", Integer.toString(i + 1));
			buf.append("\t" + replacedText + "\n");
		}
		return buf.toString();
	}

	/**
	 * @return a list of wizard pages required to create activities based on
	 *         database list templates.
	 * */
	@Override
	public List<IWizardPage> getWizardPages() {
		List<IWizardPage> contributedPages = new ArrayList<IWizardPage>();
		contributedPages.add(new CreateSampleDatabaseActivityPage());
		contributedPages.add(new CreateSampleDatabaseActivityColumnsPage());
		contributedPages.add(new DefineSqlOpenHelperPage());
		return contributedPages;
	}

	@Override
	public void setSelectedColumns(List<Column> selectedColumns) {
		this.selectedColumns.addAll(selectedColumns);
	}

	/**
	 * Creates Sql Open Helper required to transfer db file and make the
	 * activity work correctly
	 */
	@Override
	public void createSqlOpenHelper(IProject project, IProgressMonitor monitor) {
		boolean createOpenHelper = true;
		boolean createContentProvider = false;
		boolean isOverrideContentProviders = false;
		boolean generateDao = false;
		String contentProvidersPackageName = null; // not used

		// create deployer
		try {
			// sub monitor
			SubMonitor subMonitor = SubMonitor.convert(monitor, 10);

			DatabaseUtils.createDatabaseManagementClasses(project, databaseName, createOpenHelper,
					createContentProvider, sqlOpenHelperPackageName, contentProvidersPackageName,
					getSqlOpenHelperClassName(), isOverrideContentProviders, generateDao, subMonitor.newChild(10),
					false);
		} catch (Exception e) {
			StudioLogger.error(DatabaseListActivityGeneratorByTable.class,
					CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, e);
			IStatus status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
			EclipseUtils.showErrorDialog(CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE,
					CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, status);
		}
	}

	@Override
	public String getSqlOpenHelperClassName() {
		return this.sqlOpenHelperClassName;
	}

	@Override
	public void setSqlOpenHelperClassName(String sqlOpenHelperClassName) {
		this.sqlOpenHelperClassName = sqlOpenHelperClassName;
	}

	@Override
	public void setSqlOpenHelperPackageName(String sqlOpenHelperPackageName) {
		this.sqlOpenHelperPackageName = sqlOpenHelperPackageName;
	}

	/**
	 * @return The fully qualified name of the generated class to be used as
	 *         {@code import} statement.
	 * */
	@Override
	public String getImports() {
		String imports = "";
		if (createOpenHelper) {
			imports = "import " + this.sqlOpenHelperPackageName + "." + this.sqlOpenHelperClassName + ";";
		}
		return imports;
	}

	/**
	 * @return True if the open helper classes will be created. Otherwise,
	 *         returns false.
	 * */
	@Override
	public boolean createOpenHelper() {
		return this.createOpenHelper;
	}

	/**
	 * Set whether the open helper classes should be created.
	 * */
	@Override
	public void setCreateOpenHelper(boolean createOpenHelper) {
		this.createOpenHelper = createOpenHelper;
	}

	/**
	 * @return A string representing the necessary code to retrieve an android
	 *         readable database of the database created in this class.
	 * */
	@Override
	public String getReadableDatabase() {
		StringBuilder buf = new StringBuilder();

		if (createOpenHelper) {
			buf.append(sqlOpenHelperClassName + " helper = new " + sqlOpenHelperClassName
					+ "(getApplicationContext(),true);" + "\n");
			buf.append("checkDB = helper.getReadableDatabase();" + "\n");
		} else {
			buf.append("String myPath = DB_PATH + DB_NAME;" + "\n");
			buf.append("checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);" + "\n");
		}
		return buf.toString();
	}
}
