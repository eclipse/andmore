/********************************************************************************
 * Copyright (c) 2009 Motorola Inc.
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

import java.util.Map;

import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationProject;
import org.eclipse.sequoyah.localization.tools.managers.PreferencesManager;

/**
 * 
 */
public class PersistenceManager {

	private Map<LocalizationProject, ProjectPersistenceManager> projectPersistenceManager;

	private PreferencesManager preferencesManager;

	/**
	 * @param project
	 * @return
	 */
	public ProjectPersistenceManager getProjectPersistenceManager(
			LocalizationProject project) {
		return null;
	}

	/**
	 * @param project
	 * @return
	 */
	public boolean isMetadataEnabled(LocalizationProject project) {
		return false;
	}

}
