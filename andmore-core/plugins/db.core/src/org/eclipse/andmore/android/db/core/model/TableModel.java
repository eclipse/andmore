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
package org.eclipse.andmore.android.db.core.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;

/**
 * This class represents a database table
 */
public class TableModel {

	private String name = "SampleTable";

	private List<Field> fields;

	public TableModel() {
		fields = new LinkedList<Field>();
	}

	/**
	 * Creates a new Table representation
	 * 
	 * @param name
	 *            The table name
	 * @param fields
	 *            The table fields
	 */
	public TableModel(String name, List<Field> fields) {
		this.name = name;
		this.fields = fields;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the fields
	 */
	public List<Field> getFields() {
		return fields;
	}

	/**
	 * @param fields
	 *            the fields to set
	 */
	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	public void addField(Field field) {
		fields.add(field);

	}

	public void removeField(Field field) {
		fields.remove(field);

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

	public String getErrorMessage() {

		Iterator<Field> it = fields.iterator();
		String msg = null;

		while (it.hasNext() && (msg == null)) {
			Field field = it.next();

			msg = field.getErrorMessage();

			if ((msg == null) && field.getName().trim().contains(" ")) //$NON-NLS-1$
			{
				msg = DbCoreNLS.Table_ErrorUnamedColumns;
			} else if (msg == null) {

				Iterator<Field> iterator = fields.iterator();

				while (iterator.hasNext() && (msg == null)) {
					Field testField = iterator.next();
					if (field != testField) {
						if (field.getName().equalsIgnoreCase(testField.getName())) {
							msg = DbCoreNLS.Table_ErrorConflictingNames + field.getName() + ", " + testField.getName();
						} else if (field.isPrimaryKey() && testField.isPrimaryKey()) {
							msg = DbCoreNLS.Table_ErrorMoreThanOnePrimaryKey + field.getName()
									+ ", " + testField.getName(); //$NON-NLS-1$
						}

					}

				}
			}
		}
		return msg;
	}
}
