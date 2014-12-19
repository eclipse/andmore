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

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.datatools.modelbase.sql.tables.Table;

/**
 * 
 * Base class with common methods to generate persistence classes.
 *
 */
public abstract class AbstractCodeGeneratorByTable {
	/**
	 * Table name that will not generate a class as it is android specific
	 * table.
	 */
	public String ANDROID_METADATA = "ANDROID_METADATA";

	/**
	 * Table name that will not generate a class as it is android specific
	 * table.
	 */
	public String SQLITE_SEQUENCES = "sqlite_sequence";

	private Table table;

	/**
	 * Constructor that initializes the
	 * {@link org.eclipse.datatools.modelbase.sql.tables.Table Table} that will
	 * have its code generated.
	 * */
	public AbstractCodeGeneratorByTable(Table table) {
		this.table = table;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	/**
	 * Content provider package name (based on project name).
	 * 
	 * @param project
	 *            The project in which the table is contained.
	 * @return The package name that will hold the generated classes.
	 * @throws CoreException
	 *             Exception thrown in case there are problems handling the
	 *             android project.
	 * @throws AndroidException
	 *             Exception thrown in case there are problems handling the
	 *             android project.
	 */
	protected String getPackageName(IProject project) throws AndroidException, CoreException {
		// get android application name
		AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);
		ManifestNode manifestNode = androidManifestFile != null ? androidManifestFile.getManifestNode() : null;
		String appNamespace = manifestNode.getPackageName().toLowerCase();
		// return the android application name along with a persistence constant
		return appNamespace + ".persistence";
	}

	protected String getTableName() {
		return table.getName();
	}

	/**
	 * Authority to access content URI (it is based on package name and content
	 * provider name).
	 * 
	 * @param packageName
	 *            The package name that compounds the authority.
	 * @param contentProviderName
	 *            The content provider that compounds the authority.
	 * @return The authority URI.
	 */
	protected String getAuthority(String packageName, String contentProviderName) {
		return packageName + "." + contentProviderName.toLowerCase();
	}
}
