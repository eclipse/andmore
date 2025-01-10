/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.persistence;

import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationProject;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;

/**
 * 
 */
public class ProjectPersistenceManager {

	private LocalizationProject project;

	public ProjectPersistenceManager(LocalizationProject project) {
		this.project = project;
	}

	private boolean dataLoaded;

	/**
	 * 
	 */
	public void saveData() {

	}

	/**
	 * 
	 */
	public void loadData() {

	}

	/**
	 * @param file
	 */
	public void loadDataForFile(StringLocalizationFile file) {

	}

	/**
	 * @return
	 */
	public boolean hasData() {
		return false;
	}

	/**
	 * @return
	 */
	public boolean hasMetadata() {
		return false;
	}

	/**
	 * @return
	 */
	public boolean hasExtraInfo() {
		return false;
	}

	/**
	 * 
	 */
	public void clearAllData() {

	}

	/**
	 * @param file
	 * @return
	 */
	public StringLocalizationFile clearDataForFile(StringLocalizationFile file) {
		return null;
	}

	/**
	 * @param _class
	 */
	public void clearClassReferences(Class _class) {

	}

	/**
	 * @param file
	 */
	public void removeOrphanKeysForFile(StringLocalizationFile file) {

	}

	/**
	 * 
	 */
	public void removeAllOrphanKeys() {

	}

	/**
	 * @return
	 */
	public boolean isAllDataLoaded() {
		return false;
	}

	/**
	 * @param file
	 * @return
	 */
	public boolean isFileDataLoaded(StringLocalizationFile file) {
		return false;
	}

}
