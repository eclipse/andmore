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
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [352375] - Let translators contribute with translate dialog
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [353518] - Return messages from translator errors
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * ID's for all strings that will be shown to the user. Used for localization
 * purposes
 * 
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.sequoyah.localization.tools.i18n.messages"; //$NON-NLS-1$

	public static String StringEditorInput_FileMalformed;

	public static String StringEditorInput_ErrorInitializingEditor;

	public static String StringEditorInput_ErrorManagerNotInitialized;

	public static String StringEditorInput_ErrorTranslatingCells;

	public static String StringEditorInput_ErrorTranslatingFromFile;

	public static String LocaleAttribute_Exception_AllowedTypesNeeded;

	public static String LocaleAttribute_Exception_NameCannotBeNull;

	public static String LocaleAttribute_Exception_NotAllowedMethod;

	public static String LocaleAttribute_Exception_TypeDontExist;

	public static String LocaleAttribute_Exception_ValueNotAllowed;

	public static String LocaleAttribute_Exception_ValueNotAllowed2;

	public static String LocaleAttribute_Exception_ValueNotNull;

	public static String LocaleAttribute_Exception_ValueNotNull2;

	public static String Warning_NoDefaultFile;

	public static String TranslateColumnInputDialog_0;

	public static String TranslateColumnInputDialog_Error_ToOrFromNotSet;

	public static String TranslateColumnsInputDialog_Error_NoDestinationColumnSelected;

	public static String TranslateColumnsInputDialog_Error_NoLanguageSet;

	public static String TranslateColumnsInputDialog_Error_NoTranslatorsAvailable;

	public static String TranslationDialog_LanguageAreaLabel;

	public static String TranslationDialog_FromLanguage;

	public static String TranslationDialog_From;

	public static String TranslationDialog_ToLanguage;

	public static String TranslationDialog_To;

	public static String TranslationDialog_SelectAll;

	public static String TranslationDialog_DeselectAll;

	public static String TranslationDialog_ImageOKTooltip;

	public static String TranslationDialog_ImageWARNTooltip;

	public static String TranslationDialog_NoColumns;

	public static String TranslationProgress_FetchingInformation;

	public static String TranslationProgress_Connecting;

	public static String Translator_Text;

	public static String Service_Text;

	public static String NewRowDialog_AddNew;

	public static String NewRowDialog_String;

	public static String NewRowDialog_RowKey;

	public static String NewRowDialog_Entries;

	public static String NewRowDialog_Array;

	public static String NewRowDialog_NewArray;

	public static String NewRowDialog_AddToArray;

	public static String NewRowDialog_Error_RowKeyHasBlankSpaces;

	public static String NetworkLinkText;

	public static String NetworkLinkLink;

	public static String ParsingAnswer;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
