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
 * Matheus Tait Lima (Eldorado) - Adapting to accept automatic translation
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (add array key)
 * Marcelo Marzola Bossoni (Eldorado) - Bug [326793] - Change from Table to Tree (display arrays as tree)
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.editor;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionStatus;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.TranslationInfo;
import org.eclipse.sequoyah.localization.editor.providers.DefaultOperationProvider;
import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.ProjectLocalizationManager;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Extends the DefaultOperationProvider in order to provide new, empty
 * localization files (for the editor, a new file is a new column).
 * 
 */
public class NewColumnProvider extends DefaultOperationProvider {

	/**
	 * Generates key automatically for single row instead of showing dialog
	 */
	@Override
	public RowInfo[] getNewSingleRow(int quantity) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptSingleRowName(
						projectLocalizationManager.getLocalizationProject()
								.getProject(), quantity);
	}

	/**
	 * Generates key automatically for array row instead of showing dialog
	 */
	@Override
	public RowInfo[] getNewArrayRow(int quantity) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptArrayRowName(
						projectLocalizationManager.getLocalizationProject()
								.getProject(), quantity);
	}

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
	@Override
	public void init(IProject project) throws SequoyahException {
		try {
			projectLocalizationManager = LocalizationManager.getInstance()
					.getProjectLocalizationManager(project, true);
		} catch (IOException e) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_FileMalformed);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
		if (projectLocalizationManager == null) {

			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorInitializingEditor);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.providers.
	 * DefaultOperationProvider #getNewColumn()
	 */
	@Override
	public ColumnInfo getNewColumn() {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptCollumnName(
						projectLocalizationManager.getLocalizationProject()
								.getProject());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.providers.
	 * DefaultOperationProvider #getTranslatedColumnInfo()
	 */
	@Override
	public TranslationInfo getTranslatedColumnInfo(String selectedColumn) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptTranslatedCollumnName(
						projectLocalizationManager.getLocalizationProject()
								.getProject(), selectedColumn);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.providers.
	 * DefaultOperationProvider #getTranslatedColumnsInfo()
	 */
	@Override
	public TranslationInfo[] getTranslatedColumnsInfo(String selectedColumn,
			String[] selectedKeys, String[] selectedCells, TreeColumn[] columns) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptTranslatedCollumnsName(
						projectLocalizationManager.getLocalizationProject()
								.getProject(), selectedColumn, selectedKeys,
						selectedCells, columns);

	}

	@Override
	public TranslationInfo[] getTranslatedColumnsInfo(String selectedColumn,
			String[] selectedKeys, String[] selectedCells,
			Integer[] selectedIndexes, TreeColumn[] columns) {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.promptTranslatedCollumnsName(
						projectLocalizationManager.getLocalizationProject()
								.getProject(), selectedColumn, selectedKeys,
						selectedCells, selectedIndexes, columns);

	}

}
