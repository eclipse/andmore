/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Augusto Gorri (Eldorado) - Bug [323036] - Add support to other localizable resources
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android;

import org.eclipse.sequoyah.localization.android.i18n.Messages;

/**
 * Interface for sharing constants used in different places.
 * 
 */
public interface IAndroidLocalizationSchemaConstants {

	/*
	 * Android resources info
	 */
	public String RESOURCES_FOLDER = "res"; //$NON-NLS-1$

	public String PREFERED_LANGUAGES_XML_PATH = "resource/prefered_languages.xml"; //$NON-NLS-1$

	public String DEFAULT_LOCALE_TOOLTIP = Messages.AndroidLocalizationSchema_Default_Andr_Localization_File_Tooltip;

	public static final String LOCALIZATION_FILES_FOLDER = "values"; //$NON-NLS-1$

	public static final String LOCALIZATION_FILE_NAME = "strings.xml"; //$NON-NLS-1$

	public String FILE_EXTENSION = "xml"; //$NON-NLS-1$

	public String LF_REGULAR_EXPRESSION = RESOURCES_FOLDER + "/" //$NON-NLS-1$
			+ LOCALIZATION_FILES_FOLDER + ".*" + "/" + LOCALIZATION_FILE_NAME; //$NON-NLS-1$ //$NON-NLS-2$

	/*
	 * Android localization file tags and attributes
	 */
	public String XML_RESOURCES_TAG = "resources"; //$NON-NLS-1$

	public String XML_STRING_TAG = "string"; //$NON-NLS-1$

	public String XML_STRING_ARRAY_TAG = "string-array"; //$NON-NLS-1$

	public String XML_STRING_ARRAY_ITEM_TAG = "item"; //$NON-NLS-1$

	public String XML_STRING_ATTR_NAME = "name"; //$NON-NLS-1$

	public String NEW_COLUMN_TITLE = Messages.AndroidNewColumnProvider_NewColumnTitle;

	public String NEW_TRANSLATE_COLUMN_TITLE = Messages.AndroidTranslatedColumnProvider_NewColumnTitle;

	public String TRANSLATE_CELLS_TITLE = Messages.AndroidTranslateCells_DialogTitle;

	public String NEW_ROW_TITLE = Messages.AndroidNewRow_DialogTitle;

	public String NEW_COLUMN_DESCRIPTION = Messages.AndroidNewColumnProvider_NewColumnDescription;

	public String NEW_COLUMN_TEXT = AndroidLocalizationSchema.LOCALIZATION_FILES_FOLDER;

	public String NEW_COLUMN_INVALID_ID = Messages.AndroidNewColumnProvider_InvalidNewColumID;

	public String MANDATORY_ID = AndroidLocalizationSchema.LOCALIZATION_FILES_FOLDER;

	public String QUALIFIER_SEP = "-"; //$NON-NLS-1$

}
