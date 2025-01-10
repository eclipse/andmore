/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (validate key) 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.editor;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionStatus;
import org.eclipse.sequoyah.localization.editor.providers.ICellValidator;
import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.ProjectLocalizationManager;

/**
 * Validate a cell value according to the localization schema rules.
 * 
 */
public class CellValidator implements ICellValidator {

	/*
	 * The Project Localization Manager used as a source to get all information
	 * provided by this class
	 */
	private static ProjectLocalizationManager projectLocalizationManager = null;

	/**
	 * Instantiate the Project Localization Manager
	 * 
	 * @throws SequoyahException
	 */
	public void init(IProject project) throws SequoyahException {
		try {
			projectLocalizationManager = LocalizationManager.getInstance()
					.getProjectLocalizationManager(project, true);
		} catch (IOException e) {

		}
		if (projectLocalizationManager == null) {

			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorInitializingEditor);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
	}

	/**
	 * Creates a new CellValidator instance
	 */
	public CellValidator() {
		// Do nothing
	}

	/**
	 * Return the validation status of a cell. A cell will be considered invalid
	 * only if it is empty and it belongs to the default
	 */
	public IStatus isCellValid(String columnID, String key, String value) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.isValueValid(columnID, key, value);
	}

	public IStatus isKeyValid(String key) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.isKeyValid(key);
	}

}
