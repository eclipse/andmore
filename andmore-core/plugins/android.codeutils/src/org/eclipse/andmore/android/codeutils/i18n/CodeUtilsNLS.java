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

package org.eclipse.andmore.android.codeutils.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Class that contains the localized messages to be used through the Android SDK
 * support and project creation
 */
public class CodeUtilsNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.codeutils.i18n.codeUtilsNLS", CodeUtilsNLS.class);
	}

	/*
	 * Generic strings
	 */

	public static String GenerateMenuCodeDialog_DefaultMessage;

	public static String GenerateMenuCodeDialog_InflatedMessage;

	public static String GenerateMenuCodeDialog_Error_MenuFolderDoesNotExist;

	public static String GenerateMenuCodeDialog_MenuFileLabel;

	public static String GenerateMenuCodeDialog_NoSuitableClasses;

	public static String GenerateMenuCodeDialog_NoSuitableMenus;

	public static String GenerateMenuCodeDialog_NoSuitableProjects;

	public static String GenerateMenuCodeDialog_ProjectLabel;

	public static String GenerateMenuCodeDialog_ShellTitle;

	public static String GenerateMenuCodeDialog_TargetClassLabel;

	public static String GenerateMenuCodeDialog_Title;

	public static String GenerateMenuCodeHandler_Error_CannotRetrieveClassInformation;

	public static String GenerateMenuCodeHandler_SelectedClassNeitherActivityFragment;

	public static String GenerateViewBasedOnLayoutHandler_FillJavaActivityBasedOnLayout;

	public static String GenerateViewBasedOnLayoutHandler_SelectedClassNeitherActivityFragment;

	public static String MenuHandlerCodeGenerator_AddingOnCreateAndSetHasOptionMenu;

	public static String GenerateMenuCodeDialog_Class_Error;

	public static String MenuHandlerCodeGenerator_AddingOnCreateOptionsMenu;

	public static String MenuHandlerCodeGenerator_AddingOnOptionsItemSelected;

	public static String MenuHandlerCodeGenerator_InvalidJavaCharacterInAndroidOnClickAttribute;

	public static String MethodVisitor_InvalidFormatForFragmentOnCreateView;

	public static String NewActivityWizard_MessageSomeProblemsOccurredWhileBuildingProject;

	public static String NewBuildingBlocksWizardPage_PermissionLabel;

	public static String NewLauncherWizardPage_ActionTypeDialogMessage;

	public static String NewLauncherWizardPage_ActionTypeDialogTitle;

	public static String NewLauncherWizardPage_CategoryTypeDialogMessage;

	public static String NewLauncherWizardPage_CategoryTypeDialogTitle;

	public static String NewLauncherWizardPage_InputDialogValidationMessage;

	public static String JavaViewBasedOnLayoutModifier_InsertingCode;

	public static String JavaViewBasedOnLayoutModifier_CreatingImports;

	public static String JavaViewBasedOnLayoutModifier_AddingAttributes;

	public static String JavaViewBasedOnLayoutModifier_AddingOnClickMethodFromXML;

	public static String JavaViewBasedOnLayoutModifier_AddingMethodToHandleButton;

	public static String JavaViewBasedOnLayoutModifier_AddingFindingViewById;

	public static String JavaViewBasedOnLayoutModifier_AddingGalleryHandler;

	public static String JavaViewBasedOnLayoutModifier_AddingSeekbarHandler;

	public static String JavaViewBasedOnLayoutModifier_AddingSetOnClickListener;

	public static String JavaViewBasedOnLayoutModifier_AddingSpinnerHandler;

	public static String JavaViewBasedOnLayoutModifier_AddingOnKeyHandler;

	public static String JavaViewBasedOnLayoutModifier_AddingOnClickHandler;

	public static String JavaViewBasedOnLayoutModifier_AddingRatingBarHandler;

	public static String JDTUtils_FragmentOnCreateViewWithProblemsOrWithWrongFormat;

	public static String JDTUtils_MalformedXMLWhenFilenameAvailable_Error;

	public static String JDTUtils_MalformedMenuXMLWhenFilenameAvailable_Error;

	public static String JDTUtils_GenerateCodeForMenuVisitingCode_Error;

	public static String JDTUtils_MalformedXMLWhenFilenameNotAvailable_Error;

	public static String UI_ChooseLayoutItemsDialog_Dialog_Title;

	public static String UI_ChooseLayoutItemsDialog_Error_onCreate_Not_Declared;

	public static String UI_ChooseLayoutItemsDialog_No_Gui_Items_Available;

	/*
	 * Error strings
	 */
	public static String ERR_BuildingBlockCreation_ErrorMessage;

	public static String ERR_ContentProvider_InvalidAuthoritySelection;

	public static String ERR_NewProviderMainPage_ErrorMessageAlreadyExists;

	public static String ERR_NewProviderMainPage_ErrorMessageDefaultName;

	public static String ERR_NewProviderMainPage_InvalidCharactersInAuthority;

	public static String ERR_NewBuildingBlocksWizardPage_PackageMustHaveAtLeastTwoIdentifiers;

	public static String ERR_NewBuildingBlocksWizardPage_SelectAnAndroidProject;

	public static String ERR_NewBuildingBlocksWizardPage_SelectAValidSourceFolder;

	public static String ERR_NewBuildingBlocksWizardPage_CannotUseTheGenFolderAsSourceFolder;

	public static String ERR_NewBuildingBlocksWizardPage_PackageAndClassAlreadyExist;

	public static String ERR_NewBuildingBlocksWizardPage_InvalidTypeName;

	public static String ERR_NewBuildingBlocksWizardPage_InvalidPackageName;

	public static String ERR_NewBuildingBlocksWizardPage_FileNameTooLong;

	public static String ERR_NewBuildingBlocksWizardPage_OneOrMoreErrorsWhenParsingManifest;

	public static String ERR_NewBuildingBlocksWizardPage_CannotProceedWithTheBuildingBlockCreation;

	/*
	 * Exception strings
	 */
	public static String EXC_Service_CannotCreateTheServiceClass;

	public static String EXC_Service_CannotUpdateTheManifestFile;

	public static String EXC_Service_CannotCreateTheServiceLabel;

	public static String EXC_Activity_CannotCreateTheActivityClass;

	public static String EXC_Activity_CannotUpdateTheManifestFile;

	public static String EXC_Activity_CannotCreateTheActivityLabel;

	public static String EXC_Receiver_CannotCreateTheReceiverClass;

	public static String EXC_Receiver_CannotUpdateTheManifestFile;

	public static String EXC_WidgetProvider_CannotCopyTemplateFiles;

	public static String EXC_ContentProvider_CannotCreateTheContentProviderClass;

	public static String EXC_ContentProvider_CannotUpdateTheManifestFile;

	public static String EXC_ContentProvider_CannotCreateTheContentProviderLabel;

	public static String EXC_JavaClass_ErrorFormattingSourceCode;

	public static String EXC_JavaClass_ErrorApplyingCommentsToCode;

	/*
	 * UI strings
	 */
	public static String UI_GenericErrorDialogTitle;

	public static String UI_Common_UpdatingTheStringsResourceFile;

	public static String UI_Common_SavingTheAndroidManifestXMLFile;

	public static String UI_Common_UpdatingTheAndroidManifestXMLFile;

	public static String UI_Service_CreatingTheServiceJavaClass;

	public static String UI_Receiver_CreatingTheReceiverJavaClass;

	public static String UI_ContentProvider_CreatingTheContentProviderJavaClass;

	public static String UI_Activity_CreatingTheActivityJavaClass;

	public static String UI_WidgetProvider_CreatingTheWidgetProviderJavaClass;

	public static String UI_GenerateSampleListError;

	public static String UI_NewWidgetProviderWizard_WizardTitle;

	public static String UI_NewWidgetProviderMainPage_PageTitle;

	public static String UI_NewWidgetProviderMainPage_DefaultWizardDescription;

	public static String UI_NewWidgetProviderMainPage_WizardTitle;

	public static String UI_NewActivityMainPage_DescriptionCreateActivity;

	public static String UI_NewActivityMainPage_DescriptionCreateActivityBasedOnTemplate;

	public static String UI_NewActivityMainPage_TitleActivity;

	public static String UI_NewActivityMainPage_TitleActivityBasedOnTemplate;

	public static String UI_NewActivityMainPage_PageTitle;

	public static String UI_NewActivityMainPage_CheckMainButton;

	public static String UI_DefineSqlOpenHelperPage_Title;

	public static String UI_CreateNewActivityBasedOnTemplateLink;

	public static String UI_NewActivityWizard_TitleNewActivityWizard;

	public static String UI_NewActivityWizard_TitleNewActivityBasedOnTemplateWizard;

	public static String UI_NewBuildingBlocksWizardPage_WizardTitle;

	public static String UI_NewBuildingBlocksWizardPage_MessageChooseFolder;

	public static String UI_NewBuildingBlocksWizardPage_TextLabel;

	public static String UI_NewBuildingBlocksWizardPage_ButtonNameDefault;

	public static String UI_NewBuildingBlocksWizardPage_QuestionWhichMethodCreate;

	public static String UI_NewProviderMainPage_WizardTitle;

	public static String UI_NewProviderMainPage_LabelAuthorities;

	public static String UI_NewProviderMainPage_TitleNewAuthority;

	public static String UI_NewProviderMainPage_MessageAvoidConflicts;

	public static String UI_NewProviderMainPage_OptionUseDefault;

	public static String UI_NewProviderMainPage_SubtitleCreateContentProvider;

	public static String UI_NewProviderMainPage_TitleContentProvider;

	public static String UI_NewProviderWizard_WizardTitle;

	public static String UI_NewServiceMainPage_WizardTitle;

	public static String UI_NewServiceMainPage_SubtitleCreateService;

	public static String UI_NewServiceMainPage_TitleService;

	public static String UI_NewServiceWizard_WizardTitle;

	public static String UI_SampleSelectionPage_SamplesTreeLabel;

	public static String UI_SampleSelectionPage_SamplesDescriptionPane;

	public static String UI_SampleSelectionPage_Description;

	public static String UI_SampleSelectionPage_Description_JavaFile;

	public static String UI_SampleSelectionPage_Description_LayoutFile;

	public static String UI_SampleSelectionPage_Description_StringFile;

	public static String UI_SampleSelectionPage_Description_XMLFile;

	public static String UI_SampleSelectionPage_Description_AnimFile;

	public static String UI_SampleSelectionPage_Description_DrawableFile;

	public static String UI_SampleSelectionPage_Description_MenuFile;

	public static String UI_SampleSelectionPage_ErrorParsingStringXml;

	public static String UI_NewReceiverMainPage_DefaultWizardDescription;

	public static String UI_NewReceiverMainPage_WizardTitle;

	public static String UI_NewReceiverMainPage_PageTitle;

	public static String UI_NewLauncherWizardPage_ActionLabel;

	public static String UI_NewLauncherWizardPage_ActionSelectionDialogTitle;

	public static String UI_NewLauncherWizardPage_ActionSelectionDialogMessage;

	public static String UI_NewLauncherWizardPage_CategoryLabel;

	public static String UI_NewLauncherWizardPage_CategorySelectionDialogTitle;

	public static String UI_NewLauncherWizardPage_CategorySelectionDialogMessage;

	public static String UI_NewLauncherWizardPage_IntentFilterGroupName;

	public static String UI_NewReceiverWizard_WizardTitle;

	/*
	 * Model strings area
	 */
	public static String MODEL_Common_ToDoPutYourCodeHere;

	public static String MODEL_ServiceClass_ServiceDescription;

	public static String MODEL_ServiceClass_OnBindMethodDescription;

	public static String MODEL_ServiceClass_OnCreateMethodDescription;

	public static String MODEL_ServiceClass_OnStartMethodDescription;

	public static String MODEL_ContentProviderClass_ContentProviderDescription;

	public static String MODEL_ContentProviderClass_DeleteMethodDescription;

	public static String MODEL_ContentProviderClass_GetTypeMethodDescription;

	public static String MODEL_ContentProviderClass_InsertMethodDescription;

	public static String MODEL_ContentProviderClass_OnCreateMethodDescription;

	public static String MODEL_ContentProviderClass_QueryMethodDescription;

	public static String MODEL_ContentProviderClass_UpdateMethodDescription;

	public static String MODEL_ContentProviderClass_ContentUriDescription;

	public static String MODEL_BroadcastReceiverClass_BroadcastReceiverDescription;

	public static String MODEL_BroadcastReceiverClass_onReceiveMethodDescription;

	public static String MODEL_ActivityClass_ActivityDescription;

	public static String MODEL_ActivityClass_OnCreateMethodDescription;

	public static String MODEL_ActivityClass_OnStartMethodDescription;

	public static String UI_ListActivityPage_Preview;

	public static String UI_ListActivityPage_TitleWizard;

	public static String EXC_ResourceFile_ErrorFormattingTheXMLOutput;

	/*
	 * Resource file
	 */
	public static String EXC_ResourceFile_ErrorCreatingTheDocumentBuilder;

	public static String EXC_AbstractResourceFileParser_ErrorParsingTheXMLFile;

	public static String EXC_AbstractResourceFileParser_ErrorReadingTheXMLContent;

	/*
	 * Activity based on template
	 */
	public static String UI_ActivityBasedOnTemplateSupport_Configuring_Sample_Source_Task;

	/*
	 * DB
	 */
	public static String UI_CreateSampleDatabaseActivityColumnsPageName;

	public static String UI_ActivityWizard_Title;

	public static String UI_CreateSampleDatabaseActivityColumnsPage_SelectAllButton;

	public static String UI_CreateSampleDatabaseActivityColumnsPage_DeselectAllButton;

	public static String UI_CreateSampleDatabaseActivityColumnsPage_Default_Message;

	public static String UI_CreateSampleDatabaseActivityPageName;

	public static String UI_CreateSampleDatabaseActivityPage_No_Database_Found_Information;

	public static String UI_CreateSampleDatabaseActivityPage_Default_Message;

	public static String DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE;

	public static String UI_PersistenceWizardPageCreateNewSQLOpenHelper;

	public static String UI_PersistenceWizardPageSQLOpenHelperGroupTitle;

	public static String UI_DefineSqlOpenHelperPage_WarningNoOpenHelperSelected;

	public static String UI_DefineSqlOpenHelperPage_Default_Message;

	/*
	 * DB Management classes wizard
	 */
	public static String Db_GenerateManagementClassesError;

	public static String UI_PersistenceWizardPageDescriptionDeploy;

	public static String UI_PersistenceWizardPageTitleDeploy;

	public static String UI_PersistenceWizardPageSelectProjectTitle;

	public static String UI_PersistenceWizardPageDatabaseFileGroupTitle;

	public static String UI_PersistenceWizardGenerateContentProvidersForEachTable;

	public static String UI_PersistenceWizardPageContentProviderGroupTitle;

	public static String UI_PersistenceWizardOverrideContentProvidersIfAlreadyExists;

	public static String DatabaseManagementClassesCreationMainPage_UI_OpenHelperPackageNameMustNotBeEmpty;

	public static String DatabaseManagementClassesCreationMainPage_UI_ContentProvidersPackageNameMustNotBeEmpty;

	public static String UI_PersistenceWizardPageThereMustBeASelectedProject;

	public static String UI_PersistenceWizardPageTheEnteredProjectIsInvalid;

	public static String UI_PersistenceWizardPageThereMustBeASelectedDatabaseFile;

	public static String UI_PersistenceWizardPageTheEnteredPathIsInvalid;

	public static String UI_PersistenceWizardPageFileDoesNotExist;

	public static String UI_PersistenceWizardPageFileNotValid;

	public static String UI_PersistenceWizardPageFileNotEvaluated;

	public static String UI_PersistenceWizardPageFileTooLarge;

	public static String UI_PersistenceWizardPageTheDatabaseFileWillBeCopiedToProjectsAssetsFolder;

	public static String UI_PersistenceWizard_ChangePerspectiveToJava_DialogMessage;

	public static String UI_PersistenceWizard_ChangePerspectiveToJava_DialogTitle;

	public static String UI_PersistenceWizard_ChangePerspectiveToMOTODEVStudioAndroid_DialogTitle;

	public static String UI_PersistenceWizard_ChangePerspectiveToMOTODEVStudioAndroid_DialogMessage;

	public static String Field_ErrorAutoIncrementNotAllowed;

	public static String AddTableFieldDialog_InvalidName;

	public static String Table_ErrorUnamedColumns;

	public static String Table_ErrorConflictingNames;

	public static String Table_ErrorMoreThanOnePrimaryKey;

	public static String FillOnSaveInstanceStateDialog_DialogDescription;

	public static String FillOnSaveInstanceStateDialog_DialogTitle;

	public static String FillOnSaveInstanceStateDialog_ShellTitle;

	public static String FindViewByIdCodeGenerator_CompatibilityModeClassNeedToExtendFragmentActivityError;

	public static String ChooseLayoutItemsDialog_DefaultMessage;

	public static String ChooseLayoutItemsDialog_FillActivityBasedOnLayout;

	public static String ChooseLayoutItemsDialog_GenerateDefaultListeners;

	public static String ChooseLayoutItemsDialog_Gui_Items_Available_No_Id;

	public static String ChooseLayoutItemsDialog_GUIItems;

	public static String ChooseLayoutItemsDialog_Id;

	public static String ChooseLayoutItemsDialog_Project;

	public static String ChooseLayoutItemsDialog_SaveState;

	public static String ChooseLayoutItemsDialog_SaveStateTooltip;

	public static String ChooseLayoutItemsDialog_SourceLayoutFile;

	public static String ChooseLayoutItemsDialog_TargetClass;

	public static String ChooseLayoutItemsDialog_TryToGenerateCodeWhenThereIsAnError;

	public static String ChooseLayoutItemsDialog_Type;

	public static String ChooseLayoutItemsDialog_VariableName;

	public static String ChooseLayoutItemsDialog_VariableNameInUse_Error;

	public static String UI_UnselectAll;

	public static String UI_SelectAll;

	public static String Info_ChooseLayoutItemsDialog_Project_Nature;

	public static String Info_ChooseLayoutItemsDialog_Available_Classes;

	public static String AbstractLayoutItemsDialog_Error_No_Class_Found;

	public static String AbstractLayoutItemsDialog_Error_No_Layout_Found;

	public static String AbstractLayoutItemsDialog_Error_No_Projects_Found;

	public static String SaveStateCodeGenerator_AddingCodeSaveRestoreUIState;

	public static String DATABASE_DEPLOY_SUCCESS_MESSAGE;

	public static String DATABASE_DEPLOY_SUCCESS_MESSAGE_TITLE;

	public static String DATABASE_DEPLOY_ERROR_CONNECTING_DATABASE;

	public static String DATABASE_DEPLOY_CREATING_ANDROID_METADATA_TABLE;

	public static String DATABASE_ERROR_EXECUTING_STATEMENT;

}
