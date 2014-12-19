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

package org.eclipse.andmore.android.codeutils.db.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.db.deployment.ContentProviderDeployer;
import org.eclipse.andmore.android.db.deployment.ContentProviderDeployerByTable;
import org.eclipse.andmore.android.db.deployment.DatabaseDeployer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.emf.common.util.EList;

/**
 * Helper class to create ContentProviders based on Sqlite tables. It uses one
 * file as template and substitutes parameters enclosed by hash-sign (#).
 * Warning: The tables named "ANDROID_METADATA" and "sqlite_sequence" will be
 * ignored as it is an special table in Android apps context.
 */
public class ContentProviderGeneratorByTable extends AbstractCodeGeneratorByTable {
	private String dbName = null;

	/**
	 * Creates a new content provider given a
	 * {@link org.eclipse.datatools.modelbase.sql.tables.Table Table} and the
	 * name of the database which the table is attached to.
	 * 
	 * @param table
	 *            The table used in the content provider.
	 * @param dbName
	 *            The database name in which the table is attached to.
	 * */
	public ContentProviderGeneratorByTable(Table table, String dbName) {
		super(table);
		this.dbName = dbName;
	}

	/**
	 * Creates content provider based on table from Sqlite
	 * 
	 * @param project
	 * @param addCreateTableStatement
	 *            NOT USED ON 1.3.0 (for future implementation)
	 * @param addDropTableStatementOnUpdate
	 *            NOT USED ON 1.3.0 (for future implementation)
	 * @param overrideContentProviderIfExists
	 *            If <code>true</code> overrides content provider in case it
	 *            exists
	 * @param contentProviderPackageName
	 *            null to use default (.persistence inside project main
	 *            package), otherwise the file path to place
	 * @param databaseOpenHelperPackageName
	 *            Database Open Helper package name
	 * @param databaseOpenHelperClassName
	 *            Database Open Helper class name
	 * @throws IOException
	 * @throws CoreException
	 * @throws AndroidException
	 */
	public void createContentProvider(IProject project, boolean addCreateTableStatement,
			boolean addDropTableStatementOnUpdate, boolean overrideContentProviderIfExists,
			String contentProviderPackageName, String databaseOpenHelperPackageName,
			String databaseOpenHelperClassName, List<String> tableNameForClasses) throws IOException, CoreException,
			AndroidException {
		if (tableNameForClasses == null) {
			tableNameForClasses = new ArrayList<String>();
		}

		if ((getTableName() != null) && !getTableName().equalsIgnoreCase(ANDROID_METADATA)
				&& !getTableName().equalsIgnoreCase(SQLITE_SEQUENCES)) {
			String packageName = "";
			if (contentProviderPackageName != null) {
				// use package defined by user
				packageName = contentProviderPackageName;
			} else {
				// use default package
				packageName = getPackageName(project);
			}

			String openHelperPackageName = "";
			if (databaseOpenHelperPackageName != null) {
				// use package defined by user
				openHelperPackageName = databaseOpenHelperPackageName;
			} else {
				// use default package
				openHelperPackageName = getPackageName(project);
			}

			String contentProviderName = getJavaStyleTableName(tableNameForClasses) + "ContentProvider";
			String authority = getAuthority(packageName, contentProviderName);

			// create parameters and copy content provider class to the android
			// project
			Map<String, String> contentProviderParameters = new HashMap<String, String>();

			contentProviderParameters.put(ContentProviderDeployer.ANDROID_PROJECT_PACKAGE_NAME, packageName);
			contentProviderParameters.put(ContentProviderDeployer.CONTENT_PROVIDER_CLASS_NAME, contentProviderName);
			contentProviderParameters.put(ContentProviderDeployer.CONTENT_PROVIDER_AUTHORITY, authority);
			contentProviderParameters.put("#databaseOpenHelperPackageName#", openHelperPackageName);
			contentProviderParameters.put("#databaseOpenHelperClassName#", databaseOpenHelperClassName);
			contentProviderParameters.put(DatabaseDeployer.DATABASE_NAME, getDbName());
			contentProviderParameters.put("#tableName#", getTableName());
			contentProviderParameters.put("#tableNameUpperCase#", getTableName().toUpperCase());
			contentProviderParameters.put("#tableNameLowerCase#", getTableName().toLowerCase());
			contentProviderParameters.put("#uriConstants#", getUriConstants());
			contentProviderParameters.put("#constIndexesProjectMap#", getConstIndexesProjectMap());

			contentProviderParameters.put("#constContentValuesKeys#", getConstContentValuesKeys());
			contentProviderParameters.put("#defaultSortOrder#", getDefaultSortOrder());
			contentProviderParameters.put("#queryUrlCases#", getQueryUrlCases());
			contentProviderParameters.put("#typeRecordCases#", getTypeRecordCases(packageName));
			contentProviderParameters.put("#insertStatementCases#", getInsertStatementCases());
			contentProviderParameters.put("#deleteStatementCases#", getDeleteStatementCases());
			contentProviderParameters.put("#updateStatementCases#", getUpdateStatementCases());
			contentProviderParameters.put("#UrlMatcherStatementCases#", getUrlMatcherStatementCases());
			contentProviderParameters.put("#ProjectionMapStatementCases#", getProjectionMapStatementCases());

			ContentProviderDeployer.copyContentProviderHelperClassToProject(project, contentProviderParameters,
					ContentProviderDeployerByTable.CONTENT_PROVIDER_BY_TABLE_CLASS_LOCATION, true,
					overrideContentProviderIfExists, new NullProgressMonitor());
		}
	}

	/**
	 * @param tableNameForClasses
	 *            This list contains names that were used before and should not
	 *            be used anymore
	 * @return The table name in java style.
	 */
	private String getJavaStyleTableName(List<String> tableNameForClasses) {
		String originalTableName = getTableName();

		// this loop will guarantee that the table name do not starts with _
		while (originalTableName.charAt(0) == '_') {
			originalTableName = originalTableName.substring(1);
		}

		char[] charList = originalTableName.toLowerCase().toCharArray();
		charList[0] = Character.toUpperCase(charList[0]);

		// will all character after _ in upper case, than remove all _
		if (originalTableName.contains("_")) {
			for (int i = 1; i < charList.length; i++) {
				if (charList[i] == '_') {
					if ((i + 1) < charList.length) {
						charList[i + 1] = Character.toUpperCase(charList[i + 1]);
					}
				}
			}
			originalTableName = new String(charList);
			originalTableName = originalTableName.replace("_", "");
		} else {
			originalTableName = new String(charList);
		}

		// will search tableNameForClasses to verify if there was a table with
		// the same name
		// created before. If it was, we will put a counter in the end of the
		// name.
		String newName = originalTableName;
		int count = 1;
		for (String name : tableNameForClasses) {
			if (name.equals(newName)) {
				count++;
				newName = originalTableName + count;
			}
		}

		tableNameForClasses.add(newName);

		return newName;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * @return The list of Uri constants to access items on query from Content
	 *         Provider.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getUriConstants() {
		StringBuilder buf = new StringBuilder();
		String declaration = "public static final Uri ";
		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String uriVarName = column.getName().toUpperCase() + "_FIELD_CONTENT_URI";
					String parseStatement = "";
					if (column.getName().equalsIgnoreCase("_ID")) {
						parseStatement = "Uri.parse(\"content://\"+AUTHORITY+\"/\"+TABLE_NAME.toLowerCase());";
					} else {
						parseStatement = "Uri.parse(\"content://\"+AUTHORITY+\"/\"+TABLE_NAME.toLowerCase()+\"/"
								+ column.getName().toLowerCase() + "\");";
					}
					String uriDeclarationStatement = declaration + uriVarName + " = " + parseStatement;
					buf.append(uriDeclarationStatement + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * @return The list of indexes to access items on query from Content
	 *         Provider.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getConstIndexesProjectMap() {
		int initialIndex = 1;
		StringBuilder buf = new StringBuilder();
		String declaration = "private static final int ";
		String tableUpperCase = getTableName().toUpperCase();
		String constSt = declaration + tableUpperCase + " = " + initialIndex + ";";
		buf.append(constSt + "\n");
		initialIndex++;
		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String constStByTable = declaration + tableUpperCase + "_" + column.getName().toUpperCase() + " = "
							+ initialIndex + ";";
					buf.append(constStByTable + "\n");
					initialIndex++;
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Default sort order will be set to the first primary key column. If there
	 * is not, it will set as the first column name found.
	 * 
	 * @return java code to declare a sql string to order select results using
	 *         the default column to sort the rows.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getDefaultSortOrder() {
		Column firstColumn = null;
		Column chosenDefaultColumn = null;
		StringBuilder buf = new StringBuilder();
		String declaration = "public static final String DEFAULT_SORT_ORDER = ";
		Table table = this.getTable();
		if (table != null) {
			EList columns = table.getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					int index = columnsIter.nextIndex();
					Column column = columnsIter.next();
					if (index == 0) {
						// if no column is primary key, set it first column as
						// default to sort
						firstColumn = column;
					}
					if (column.isPartOfPrimaryKey()) {
						chosenDefaultColumn = column;
						break;
					}
				}
			}
		}
		if (chosenDefaultColumn == null) {
			chosenDefaultColumn = firstColumn;
		}
		String defaultSortSt = declaration + "\"" + chosenDefaultColumn.getName() + " ASC\";";
		buf.append(defaultSortSt + "\n");
		return buf.toString();
	}

	/**
	 * @return The URLs to be used on queries.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getQueryUrlCases() {
		StringBuilder buf = new StringBuilder();
		String caseSt = "case " + getTableName().toUpperCase() + ":";
		buf.append(caseSt + "\n");
		String setTablesSt = "qb.setTables(TABLE_NAME);";
		String setProjectionSt = "qb.setProjectionMap(" + getTableName().toUpperCase() + "_PROJECTION_MAP);";
		String breakSt = "break;";
		buf.append("\t" + setTablesSt + "\n");
		buf.append("\t" + setProjectionSt + "\n");
		buf.append("\t" + breakSt + "\n");

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String caseColumnSt = "case " + getTableName().toUpperCase() + "_" + column.getName().toUpperCase()
							+ ":";
					buf.append(caseColumnSt + "\n");
					String setTablesColumnsSt = "qb.setTables(TABLE_NAME);";
					buf.append("\t" + setTablesColumnsSt + "\n");
					String setAppendWhereSt = "";
					if (column.getName().equalsIgnoreCase("_ID")) {
						// obrigatory primary key on android
						setAppendWhereSt = "qb.appendWhere(\"" + column.getName().toLowerCase()
								+ "=\" + url.getPathSegments().get(1));";
					} else {
						setAppendWhereSt = "qb.appendWhere(\"" + column.getName().toLowerCase()
								+ "='\" + url.getPathSegments().get(2)+\"'\");";
					}
					buf.append("\t" + setAppendWhereSt + "\n");
					buf.append("\t" + breakSt + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Get types for multiple (records for the entire Bean - use dir cursor) or
	 * single items (simple columns into the Bean - use item cursor)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getTypeRecordCases(String packageName) {
		StringBuilder buf = new StringBuilder();
		String caseSt = "case " + getTableName().toUpperCase() + ":";
		buf.append(caseSt + "\n");
		String returnMultipleSt = "return " + "\"vnd.android.cursor.dir/vnd." + packageName + "."
				+ getTableName().toLowerCase() + "\"" + ";";
		buf.append("\t" + returnMultipleSt + "\n");

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String caseColumnSt = "case " + getTableName().toUpperCase() + "_" + column.getName().toUpperCase()
							+ ":";
					buf.append(caseColumnSt + "\n");
					String returnSingleSt = "return " + "\"vnd.android.cursor.item/vnd." + packageName + "."
							+ getTableName().toLowerCase() + "\"" + ";";
					buf.append("\t" + returnSingleSt + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * @return The insert statements generated based on table columns.
	 */
	private String getInsertStatementCases() {
		// Empty for now - Let method because users can ask something in the
		// future
		return "";
	}

	/**
	 * 
	 * @return The delete statements generated based on table columns.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getDeleteStatementCases() {
		StringBuilder buf = new StringBuilder();
		String caseSt = "case " + getTableName().toUpperCase() + ":";
		buf.append(caseSt + "\n");
		String countSt = "count = mDB.delete(TABLE_NAME, where, whereArgs);";
		buf.append("\t" + countSt + "\n");
		buf.append("\t" + "break;" + "\n");

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String caseColumnSt = "case " + getTableName().toUpperCase() + "_" + column.getName().toUpperCase()
							+ ":";
					buf.append(caseColumnSt + "\n");
					String segmentSt = "";
					if (column.getName().equalsIgnoreCase("_ID")) {
						segmentSt = "segment = url.getPathSegments().get(1);";
					} else {
						// non-id items
						segmentSt = "segment = \"'\" + url.getPathSegments().get(2) + \"'\";";
					}
					buf.append("\t" + segmentSt + "\n");
					String countStByColumn = "count = mDB.delete(TABLE_NAME, \""
							+ column.getName().toLowerCase()
							+ "=\" + segment + (!TextUtils.isEmpty(where) ? \" AND (\" + where + ')' : \"\"), whereArgs);";
					buf.append("\t" + countStByColumn + "\n");
					buf.append("\t" + "break;" + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * @return Update statements generated based on table columns.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getUpdateStatementCases() {
		StringBuilder buf = new StringBuilder();
		String caseSt = "case " + getTableName().toUpperCase() + ":";
		buf.append(caseSt + "\n");
		String countSt = "count = mDB.update(TABLE_NAME, values, where, whereArgs);";
		buf.append("\t" + countSt + "\n");
		buf.append("\t" + "break;" + "\n");

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String caseColumnSt = "case " + getTableName().toUpperCase() + "_" + column.getName().toUpperCase()
							+ ":";
					buf.append(caseColumnSt + "\n");
					String segmentSt = "";
					if (column.getName().equalsIgnoreCase("_ID")) {
						segmentSt = "segment = url.getPathSegments().get(1);";
					} else {
						// non-id items
						segmentSt = "segment = \"'\" + url.getPathSegments().get(2) + \"'\";";
					}
					buf.append("\t" + segmentSt + "\n");
					String countStByColumn = "count = mDB.update(TABLE_NAME, values, \""
							+ column.getName().toLowerCase()
							+ "=\" + segment + (!TextUtils.isEmpty(where) ? \" AND (\" + where + ')' : \"\"), whereArgs);";
					buf.append("\t" + countStByColumn + "\n");
					buf.append("\t" + "break;" + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * @return The matcher to recognize the pattern developer is trying to query
	 *         the data from ContentProvider.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getUrlMatcherStatementCases() {
		StringBuilder buf = new StringBuilder();
		buf.append("URL_MATCHER.addURI(AUTHORITY, TABLE_NAME.toLowerCase(), " + getTableName().toUpperCase() + ");"
				+ "\n");

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();

				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					if (column.getName().equalsIgnoreCase("_ID")) {
						buf.append("URL_MATCHER.addURI(AUTHORITY, TABLE_NAME.toLowerCase()" + "+\"/#\", "
								+ getTableName().toUpperCase() + "_" + column.getName().toUpperCase() + ");" + "\n");
					} else {
						// non-id items
						buf.append("URL_MATCHER.addURI(AUTHORITY, TABLE_NAME.toLowerCase()" + "+\"/"
								+ column.getName().toLowerCase() + "\"+\"/*\", " + getTableName().toUpperCase() + "_"
								+ column.getName().toUpperCase() + ");" + "\n");
					}
				}
			}
		}
		return buf.toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String getConstContentValuesKeys() {
		StringBuilder buf = new StringBuilder();
		String declaration = "\tpublic static final String ";
		Table table = this.getTable();
		if (table != null) {
			EList columns = table.getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					String contValuesKeysSt = declaration + column.getName().toUpperCase() + " = \"" + column.getName()
							+ "\";";
					buf.append(contValuesKeysSt + "\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Initialize maps to get table items into the static constructor from the
	 * Provider.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getProjectionMapStatementCases() {
		StringBuilder buf = new StringBuilder();

		Table table = getTable();
		if (table != null) {
			EList columns = getTable().getColumns();
			if ((columns != null) && (columns.size() > 0)) {
				ListIterator<Column> columnsIter = columns.listIterator();
				while (columnsIter.hasNext()) {
					Column column = columnsIter.next();
					buf.append(getTableName().toUpperCase() + "_PROJECTION_MAP.put(" + column.getName().toUpperCase()
							+ "," + "\"" + column.getName().toLowerCase() + "\");" + "\n");
				}
			}
		}
		return buf.toString();
	}
}
