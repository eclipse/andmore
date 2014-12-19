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

package org.eclipse.andmore.android.db.wizards.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;

/**
 * An abstraction of a database table.
 * */
public class Table {
	private String tableName = null;

	private final List<Field> fields = new ArrayList<Field>();

	@Override
	public String toString() {
		String query = "("; //$NON-NLS-1$
		String primaryKey = " , PRIMARY KEY ("; //$NON-NLS-1$

		boolean hasPrimary = false;

		Iterator<Field> it = fields.iterator();
		Field fie;
		while (it.hasNext()) {

			fie = it.next();
			query += fie.getName() + " " //$NON-NLS-1$
					+ fie.getType() + " " //$NON-NLS-1$
					+ ((!(fie.getDefaultValue().equals("") || fie.isPrimary())) //$NON-NLS-1$
					? " default \'" : "") //$NON-NLS-1$ //$NON-NLS-2$
					+ fie.getDefaultValue() + ((!(fie.getDefaultValue().equals("") || fie.isPrimary())) ? "\' " //$NON-NLS-1$ //$NON-NLS-2$
							: ""); //$NON-NLS-1$
			if (fie.isPrimary()) {
				hasPrimary = true;
				primaryKey += fie.getName() + fie.getKeyBehaviourQuery() + " )"; //$NON-NLS-1$

			}
			if (it.hasNext())

			{
				query += " , "; //$NON-NLS-1$
			}

		}

		query += (hasPrimary ? primaryKey : "") + " )"; //$NON-NLS-1$ //$NON-NLS-2$

		return query;

	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields.clear();
		if (fields != null) {
			for (Field field : fields) {
				this.fields.add(field);
			}
		}
	}

	/**
	 * Add {@code field} to the table.
	 * */
	public void addField(Field field) {

		fields.add(field);

	}

	/**
	 * Remove {@code field} from the table.
	 * */
	public void removeField(Field obj) {
		fields.remove(obj);

	}

	/**
	 * @return If this table has some error in its fields, like unnamed columns,
	 *         a string describing the error is returned.
	 * */
	public String getErrorMessage() {

		Iterator<Field> it = fields.iterator();
		String msg = null;

		while (it.hasNext() && (msg == null)) {
			Field field = it.next();

			msg = field.getErrorMessage();

			if ((msg == null) && field.getName().trim().contains(" ")) //$NON-NLS-1$
			{
				msg = CodeUtilsNLS.Table_ErrorUnamedColumns;
			} else if (msg == null) {

				Iterator<Field> iterator = fields.iterator();

				while (iterator.hasNext() && (msg == null)) {
					Field testField = iterator.next();
					if (field != testField) {
						if (field.getName().equalsIgnoreCase(testField.getName())) {
							msg = CodeUtilsNLS.Table_ErrorConflictingNames + field.getName() + ", "
									+ testField.getName();
						} else if (field.isPrimary() && testField.isPrimary()) {
							msg = CodeUtilsNLS.Table_ErrorMoreThanOnePrimaryKey + field.getName()
									+ ", " + testField.getName(); //$NON-NLS-1$
						}

					}

				}
			}
		}
		return msg;
	}

	/**
	 * Validates the name to be different from SQLite keywords. Reference:
	 * http://www.sqlite.org/lang_keywords.html
	 * 
	 * @param name
	 *            The table name
	 * @return True is a valid table name, false otherwise
	 */
	public static boolean validateName(String name) {

		boolean isValid = true;
		String[] keywords = { "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC",
				"ATTACH", "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BY", "CASCADE", "CASE", "CAST", "CHECK",
				"COLLATE", "COLUMN", "COMMIT", "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_DATE",
				"CURRENT_TIME", "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC",
				"DETACH", "DISTINCT", "DROP", "EACH", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUSIVE", "EXISTS",
				"EXPLAIN", "FAIL", "FOR", "FOREIGN", "FROM", "FULL", "GLOB", "GROUP", "HAVING", "IF", "IGNORE",
				"IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER", "INSERT", "INSTEAD", "INTERSECT", "INTO",
				"IS", "ISNULL", "JOIN", "KEY", "LEFT", "LIKE", "LIMIT", "MATCH", "NATURAL", "NO", "NOT", "NOTNULL",
				"NULL", "OF", "OFFSET", "ON", "OR", "ORDER", "OUTER", "PLAN", "PRAGMA", "PRIMARY", "QUERY", "RAISE",
				"REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME", "REPLACE", "RESTRICT", "RIGHT", "ROLLBACK",
				"ROW", "SAVEPOINT", "SELECT", "SET", "TABLE", "TEMP", "TEMPORARY", "THEN", "TO", "TRANSACTION",
				"TRIGGER", "UNION", "UNIQUE", "UPDATE", "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL", "WHEN", "WHERE" };

		for (String keyword : keywords) {

			// Found. It is a keyword
			if (keyword.toLowerCase().compareTo(name.toLowerCase()) == 0) {

				isValid = false;
			}
		}

		return isValid;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName
	 *            the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName == null ? null : tableName.trim();
	}
}
