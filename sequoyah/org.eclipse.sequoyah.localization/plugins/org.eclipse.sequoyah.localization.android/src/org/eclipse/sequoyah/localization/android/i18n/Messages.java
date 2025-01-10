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
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * ID's for all strings that will be shown to the user. Used for localization
 * purposes
 * 
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.sequoyah.localization.android.i18n.messages"; //$NON-NLS-1$

	public static String AndroidLocalizationSchema_6;

	public static String AndroidLocalizationSchema_Default_Andr_Localization_File_Tooltip;

	public static String AndroidLocalizationSchema_NewArrayKeyPrefix;

	public static String AndroidLocalizationSchema_NewStringKeyPrefix;

	public static String StringLocalizationFileManager_Exception_CouldNotLoadFile;

	public static String AndroidNewColumnProvider_Dialog_FileAlreadyExists;

	public static String AndroidNewColumnProvider_InvalidNewColumID;

	public static String AndroidNewColumnProvider_NewColumnDescription;

	public static String AndroidNewColumnProvider_NewColumnTitle;

	public static String AndroidStringEditorInput_EditorTooltip;

	public static String AndroidTranslatedColumnProvider_NewColumnTitle;

	public static String AndroidTranslateCells_DialogTitle;

	public static String AndroidNewRow_DialogTitle;

	// Status message
	public static String EmptyKey_Discouraged;

	// Exception texts
	public static String Unknown_Andr_Type;

	public static String Invalid_Andr_Value;

	public static String Invalid_Andr_Value_Size;

	public static String Invalid_Andr_Key_Name;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
