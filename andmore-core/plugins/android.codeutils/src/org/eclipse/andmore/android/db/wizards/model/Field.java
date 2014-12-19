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

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;

/**
 * This class abstracts a field in a database table.
 * */
public class Field {
	/**
	 * A primary key may be set to be updated incrementally, decrementally or
	 * none.
	 * */
	public enum PrimaryKeyBehaviour {
		NONE, INCREMENTAL, DECREMENTAL
	};

	private String type;

	private String name;

	private String defaultValue;

	private boolean isPrimary;

	private PrimaryKeyBehaviour primaryKeyBehaviour;

	/**
	 * @return The behavior of the primary key as described in
	 *         {@link PrimaryKeyBehaviour}.
	 * */
	public PrimaryKeyBehaviour getPrimaryKeyBehaviour() {
		return primaryKeyBehaviour;
	}

	/**
	 * Set the primary key behavior as described in {@link PrimaryKeyBehaviour}.
	 * */
	public void setPrimaryKeyBehaviour(PrimaryKeyBehaviour primaryKeyBehaviour) {
		this.primaryKeyBehaviour = primaryKeyBehaviour;
	}

	/**
	 * Constructor that initializes the field name.
	 * */
	public Field(String name) {
		this.name = name;
	}

	/**
	 * @return field type.
	 * */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return field name.
	 * */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return The default value used to fill in the field when it has no value
	 *         in insert statements.
	 * */
	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @return True if this field belongs to the primary key set. Otherwise,
	 *         returns false.
	 * */
	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	/**
	 * @return <ul>
	 *         <li>empty string, if {@link PrimaryKeyBehaviour} is set to
	 *         {@link PrimaryKeyBehaviour#NONE}.</li>
	 *         <li>ASC, if {@link PrimaryKeyBehaviour} is set to
	 *         {@link PrimaryKeyBehaviour#INCREMENTAL}.</li>
	 *         <li>DESC, if {@link PrimaryKeyBehaviour} is set to
	 *         {@link PrimaryKeyBehaviour#DECREMENTAL}.</li>
	 *         </ul>
	 * */
	public String getKeyBehaviourQuery() {
		String[] query = { "", " ASC", " DESC" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		};

		return query[getPrimaryKeyBehaviour().ordinal()];

	}

	/**
	 * @return An string describing field property error and {@code null} if
	 *         there are no errors.
	 * */
	public String getErrorMessage() {
		String message = null;

		if (!type.equals("INTEGER") && !type.equals("REAL") //$NON-NLS-1$ //$NON-NLS-2$
				&& !primaryKeyBehaviour.equals(PrimaryKeyBehaviour.NONE)) {
			message = CodeUtilsNLS.Field_ErrorAutoIncrementNotAllowed;
		}

		// Validate name to don't use sqlite keywords
		if ((message == null) && !Table.validateName(getName())) {

			message = CodeUtilsNLS.AddTableFieldDialog_InvalidName + getName();
		}

		return message;
	}

}
