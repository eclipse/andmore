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
package org.eclipse.andmore.android.db.core.ui.wizards.createdb;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.osgi.util.NLS;

/**
 * This class represents a database name validator. The name must not not
 * already exists in the Database View tree and must follow a certain pattern.
 */
public class DatabaseCreationFieldValidator implements IInputValidator {

	/**
	 * The regular expression pattern used to validate the database name
	 * provided by the user
	 */
	private static final String VALIDATOR_PATTERN = "[a-zA-Z0-9 ._-]+(.db)?"; //$NON-NLS-1$

	/**
	 * The extension of the database files at the device
	 */
	private static final String DB_EXTENSION = ".db"; //$NON-NLS-1$

	/**
	 * Expression pattern used to validate the database name provided by the
	 * user
	 */
	private final Pattern pattern = Pattern.compile(VALIDATOR_PATTERN);

	private final List<String> alreadyAvailableDbs;

	public DatabaseCreationFieldValidator(final List<String> alreadyAvailableDbs) {
		this.alreadyAvailableDbs = alreadyAvailableDbs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IInputValidator#isValid(java.lang.String)
	 */
	@Override
	public String isValid(String newText) {
		String msg = null;
		Matcher patternMatcher = pattern.matcher(newText);
		if (!patternMatcher.matches()) {
			msg = DbCoreNLS.DatabaseCreationFieldValidator_ValidChars;
		} else
		// check whether the database already exists
		{
			String dbFileName = newText + DB_EXTENSION;
			if (alreadyAvailableDbs.contains(dbFileName)) {
				msg = NLS.bind(DbCoreNLS.DatabaseCreationFieldValidator_DB_Already_Exists_Msg, dbFileName);
			}
		}

		return msg;
	}
}
