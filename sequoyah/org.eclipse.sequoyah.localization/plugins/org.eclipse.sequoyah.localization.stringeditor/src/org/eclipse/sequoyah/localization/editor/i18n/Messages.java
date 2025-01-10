/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug (289236) - Editor Permitting create 2 columns with same id
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Matheus Tait Lima (Eldorado) - Adapting localization plugins to accept automatic translations
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (add array key)
 * Daniel Drigo Pastore (Eldorado) - Bug [326793] - Adding array status tooltip message
 * Matheus Lima (Eldorado) - Bug [326793] -  Fixed action description
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.i18n;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.sequoyah.localization.editor.i18n.messages"; //$NON-NLS-1$

	public static String AddArrayAction_DescriptionPrefix;

	public static String AddArrayItemAction_DescriptionPrefix;

	public static String AddMultipleItemsAction_0;

	public static String AddMultipleItemsAction_2;

	public static String AddMultipleItemsAction_4;

	public static String AddMultipleItemsAction_6;

	public static String AddSingleStringAction_DescriptionPrefix;

	public static String StringEditorColumnLabelProvider_0;

	public static String CellEditingSupport_0;

	public static String CellEditingSupport_1;

	public static String CellEditingSupport_2;

	public static String CloneOperation_0;

	public static String EditCellOperation_0;

	public static String EditKeyOperation_OperationName;

	public static String StringEditorPart_1;

	public static String StringEditorPart_10;

	public static String StringEditorPart_11;

	public static String StringEditorPart_2;

	public static String StringEditorPart_3;

	public static String StringEditorPart_4;

	public static String StringEditorPart_5;

	public static String StringEditorPart_6;

	public static String StringEditorPart_7;

	public static String StringEditorPart_8;

	public static String StringEditorPart_9;

	public static String StringEditorPart_Apply;

	public static String StringEditorPart_TranslateCellActionName;

	public static String TranslateCellAction_NoResultsMessage;

	public static String TranslateCellAction_NoResultsTitle;

	public static String TranslationProgress_Connecting;

	public static String DefaultOperationProvider_NewColumnDefault;

	public static String DefaultOperationProvider_NewRowDefault;

	public static String DefaultOperationProvider_NewColumnDescription;

	public static String DefaultOperationProvider_NewColumnErrorNotEmpty;

	public static String DefaultOperationProvider_NewColumnTitle;

	public static String DefaultOperationProvider_NewRowDescription;

	public static String DefaultOperationProvider_NewRowErrorNotEmpty;

	public static String DefaultOperationProvider_NewRowTitle;

	public static String StringEditorPart_AddColumnActionName;

	public static String StringEditorPart_AddColumnOperationName;

	public static String StringEditorPart_AddSingleStringActionName;

	public static String StringEditorPart_AddSingleStringOperationName;

	public static String StringEditorPart_AddArrayActionName;

	public static String StringEditorPart_AddArrayOperationName;

	public static String StringEditorPart_AddArrayItemActionName;

	public static String StringEditorPart_AddArrayItemOperationName;

	public static String StringEditorPart_HideAllColumnsActionName;

	public static String StringEditorPart_HighlightChangesLabel;

	public static String StringEditorPart_ExpandRows;

	public static String StringEditorPart_KeyAlreadyExistsErrorMessage;

	public static String StringEditorPart_KeyLabel;

	public static String StringEditorPart_KeyTooltip;

	public static String StringEditorPart_OptionsText;

	public static String StringEditorPart_RemoveColumnActionName;

	public static String StringEditorPart_RemoveColumnOperationName;

	public static String StringEditorPart_RemoveColumnQuestionMessage;

	public static String StringEditorPart_RemoveKeyActionName;

	public static String StringEditorPart_RemoveKeyOperationName;

	public static String StringEditorPart_RevertColumnActionName;

	public static String StringEditorPart_RevertColumnActionOperationName;

	public static String StringEditorPart_RevertColumnActionTooltip;

	public static String StringEditorPart_SearchLabel;

	public static String StringEditorPart_ShowAllColumnsActionName;

	public static String StringEditorPart_ShowCellCommentsButtonText;

	public static String StringEditorPart_ShowColumnsSubmenuLabel;

	public static String StringEditorPart_CloneActionName;

	public static String StringEditorPart_FilterByKeyLabel;

	public static String StringEditorPart_EditorTitle;

	public static String StringEditorPart_ColumnAlreadyExistTitle;

	public static String StringEditorPart_ColumnAlreadyExistMessage;

	public static String StringEditorPart_FileChangedTitle;

	public static String StringEditorPart_FileChangedDescription;

	public static String StringEditorPart_UpdateConflictTitle;

	public static String StringEditorPart_UpdateConflictDescription;

	public static String StringEditorPart_TranslateActionName;

	public static String StringEditorPart_TranslationError;

	public static String StringEditorPart_TranslationErrorCheckConnetion;

	public static String StringEditorPart_TranslationErrorTargetExists;

	public static String StringEditorViewerEditableTooltipSupport_TypeYourComment;

	public static String StringEditorPart_ErrorOpeningEditor;

	public static String StringEditorPart_ErrorOpeningEditorUnknown;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
