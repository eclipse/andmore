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

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;

/**
 * This class represents a table field
 */
public class Field {
	/**
	 * This enum represents the auto increment value for primary key field. Use
	 * the toString method to retrieve the SQL syntax keyword
	 */
	public enum AutoIncrementType {
		NONE("NONE"), ASCENDING("ASC"), DESCENDING("DESC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		private String strValue;

		private AutoIncrementType(String strValue) {
			this.strValue = strValue;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return strValue;
		}
	}

	/**
	 * This enum represents the DataType of a table field Use the toString
	 * method to retrieve the SQL syntax keyword
	 */
	public enum DataType {
		INTEGER("INTEGER"), TEXT("TEXT"), REAL("REAL"), BLOB("BLOB"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		private String strValue;

		private DataType(String dataType) {
			this.strValue = dataType;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return strValue;
		}
	}

	private String name;

	private boolean primaryKey;

	private AutoIncrementType autoIncrementType;

	private DataType type;

	private String defaultValue;

	@Override
	public boolean equals(Object other) {
		boolean equals = false;

		if (other instanceof Field) {
			equals = this.getName().equals(((Field) other).getName());
		}

		return equals;
	}

	/**
	 * Creates a new field representation based on the given params
	 * 
	 * @param name
	 * @param primaryKey
	 * @param autoIncrementType
	 * @param type
	 * @param defaultValue
	 */
	public Field(String name, DataType type, boolean primaryKey, AutoIncrementType autoIncrementType,
			String defaultValue) {
		this.name = name;
		this.primaryKey = primaryKey;
		this.autoIncrementType = autoIncrementType;
		this.type = type;
		this.defaultValue = defaultValue;
	}

	public Field() {
		this("", DataType.TEXT, false, AutoIncrementType.NONE, null); //$NON-NLS-1$
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
	 * @return the primaryKey
	 */
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @param primaryKey
	 *            the primaryKey to set
	 */
	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * @return the autoIncrementType
	 */
	public AutoIncrementType getAutoIncrementType() {
		return autoIncrementType;
	}

	/**
	 * @param autoIncrementType
	 *            the autoIncrementType to set
	 */
	public void setAutoIncrementType(AutoIncrementType autoIncrementType) {
		this.autoIncrementType = autoIncrementType;
	}

	/**
	 * @return the type
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(DataType type) {
		this.type = type;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue
	 *            the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getErrorMessage() {
		String message = null;

		if (!type.equals(DataType.INTEGER) && !type.equals(DataType.REAL)
				&& !autoIncrementType.equals(AutoIncrementType.NONE)) {
			message = DbCoreNLS.Field_ErrorAutoIncrementNotAllowed;
		}

		// Validate name to don't use sqlite keywords
		if ((message == null) && !TableModel.validateName(getName())) {

			message = DbCoreNLS.AddTableFieldDialog_InvalidName + getName();
		}

		return message;
	}
}
