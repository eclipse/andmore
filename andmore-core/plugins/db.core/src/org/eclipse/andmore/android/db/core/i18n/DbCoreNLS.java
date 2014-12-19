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
package org.eclipse.andmore.android.db.core.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * This class is the NLS component for DB Core plug-in
 */
public class DbCoreNLS extends NLS {
	/**
	 * The bundle location. It refers to messages.properties file inside this
	 * package
	 */
	private static final String BUNDLE_NAME = "org.eclipse.andmore.android.db.core.i18n.dbcoreNLS"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, DbCoreNLS.class);
	}

	/*
	 * DB Management classes wizard
	 */

	public static String DatabaseCreationFieldValidator_DB_Already_Exists_Msg;

	public static String DatabaseCreationFieldValidator_ValidChars;

	public static String DatabaseExplorerTreeLabelProvider_Error_Tooltip_Prefix;

	public static String Field_ErrorAutoIncrementNotAllowed;

	public static String AbstractTreeNode_Loading_Job_Name;

	public static String AddTableFieldDialog_InvalidName;

	public static String Table_ErrorUnamedColumns;

	public static String Table_ErrorConflictingNames;

	public static String Table_ErrorMoreThanOnePrimaryKey;

	public static String TableNode_BrowsingTableContentsErrorStatus;

	public static String TableNode_BrowsingTableContentsSuccessStatus;

	public static String FilesystemRootNode_Error_Mapping_Description;

	public static String FilesystemRootNode_Map_Successful;

	public static String FilesystemRootNode_Mapped_Db_Not_Found;

	public static String FilesystemRootNode_Unmapping_Successful;

	public static String FilesystemRootNode_UnmappingList_Error;

	public static String FilesystemRootNode_UnmappingList_Successful;

	public static String AddTableFieldDialog_FieldDefaultValueLabel;

	public static String AddTableFieldDialog_FieldNameLabel;

	public static String AddTableFieldDialog_FieldTypeLabel;

	public static String AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_DecrementalLabel;

	public static String AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_IncrementalLabel;

	public static String AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_NoneLabel;

	public static String AddTableFieldDialog_PrimaryKeyAutomaticBehaviourLabel;

	public static String AddTableFieldDialog_PrimaryKeyLabel;

	/*
	 * Error strings
	 */

	public static String DbModel_Could_Not_Delete_DbFile;

	public static String DbModel_Could_Not_Disconnect_Profile;

	public static String DbModel_Could_Not_Execute_Statement;

	public static String DbModel_Not_Valid_Database;

	public static String DbModel_Sampling_Contents_From;

	public static String DbNode_Canceled_Save_Operation;

	public static String DbNode_Close_Editor_Msg;

	public static String DbNode_Close_Editor_Msg_Title;

	public static String DbNode_CouldNotDeleteTable;

	public static String DbNode_Tooltip_Prefix;

	/*
	 * UI Strings
	 */

	public static String Invalid_Db_Error;

	public static String MapDatabaseHandler_Title_Error;

	/*
	 * Console Strings
	 */

	public static String ColumnNode_UnknownType;

	public static String CreateDatabaseWizardPage_Add_Button;

	public static String CreateDatabaseWizardPage_DB_Name_Label;

	public static String CreateDatabaseWizardPage_Edit_Button;

	public static String CreateDatabaseWizardPage_Remove_Button;

	public static String CreateDatabaseWizardPage_Table_Already_Exists_Msg;

	public static String CreateDatabaseWizardPage_Table_Already_Exists_Title;

	public static String CreateDatabaseWizardPage_Table_Group;

	public static String CreateDatabaseWizardPage_UI_PageTitle;

	public static String CreateDatabaseWizardPage_UI_CreateNewDatabase;

	public static String CreateDatabaseWizardPage_UI_CreateNewDBAddingItsFields;

	public static String CreateTableWizard_UI_Message_ErrorCreatingTable;

	public static String CreateTableWizardPage_AddEditField_DialogTitle;

	public static String CreateTableWizardPage_UI_Add;

	public static String CreateTableWizardPage_UI_CreateNewTable;

	public static String CreateTableWizardPage_UI_CreateNewTableAddingItsFields;

	public static String CreateTableWizardPage_UI_PageTitle;

	public static String CreateTableWizardPage_UI_Default;

	public static String CreateTableWizardPage_UI_Edit;

	public static String CreateTableWizardPage_UI_InvalidTableName;

	public static String CreateTableWizardPage_UI_Name;

	public static String CreateTableWizardPage_UI_Primary;

	public static String CreateTableWizardPage_UI_Remove;

	public static String CreateTableWizardPage_UI_TableName;

	public static String CreateTableWizardPage_UI_TableNameCannotBeEmpty;

	public static String CreateTableWizardPage_UI_Type;

	public static String CreateTableWizardPage_UI_YouMustSupplyAtLeastOneField;

	/*
	 * Database deployment
	 */

	public static String TableWizardLabelProvider_isPrimary_False;

	public static String TableWizardLabelProvider_isPrimary_true;

	/* Map and unmap databases on file system */

	public static String UI_UnmapDatabaseAction_Title;

	public static String DeleteDatabaseHandler_ConfirmationQuestionDialog_Description;

	public static String DeleteDatabaseHandler_ConfirmationQuestionDialog_Title;

	public static String DeleteDatabaseHandler_CouldNotDeleteDatabase;

	public static String DeleteTableHandler_ConfirmationQuestionDialog_Description;

	public static String DeleteTableHandler_ConfirmationQuestionDialog_Title;

	public static String UI_DeleteProjectDialogTitle;

	public static String UI_DeleteProjectDialogMsg;

	public static String ERR_CreateDatabaseWizardPage_TableAlreadyExistTitle;

	public static String UI_CreateDatabaseWizard_ChangePerspectiveQuestion;

	public static String UI_CreateDatabaseWizard_ChangePerspectiveTitle;

	public static String LoadingNode_nodeName;

	public static String ProjectNode_Error_While_Creating_DB;

	public static String ProjectNode_Failed_ToVerify_If_DB_Is_Valid;

	public static String RefreshNodeHandler_RefreshingNode_Error_Msg;

	public static String RefreshNodeHandler_RefreshingNode_Msg_Title;

	public static String UI_CreateDatabaseWizardPage_CreateDatabase_Error_New;

	public static String UI_CreateDatabaseWizardPage_CreateDatabase_Error;

	public static String UnmapDatabaseHandler_Error_Description;

	public static String UnmapDatabaseHandler_Error_Title;
}
