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
package org.eclipse.andmore.android.packaging.ui.i18n;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.andmore.android.packaging.ui.i18n.messages"; //$NON-NLS-1$

	public static String PACKAGE_EXPORT_WIZARD_AREA_UNSIGNEDPACKAGE_WARNING;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

	public static String EXPORT_WIZARD_TILE;

	public static String EXPORT_WIZARD_DESCRIPTION;

	public static String PACKAGE_EXPORT_WIZARD_AREA_DESCRIPTION;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SELECT_ALL_BUTTON;

	public static String PACKAGE_EXPORT_WIZARD_AREA_DESELECT_ALL_BUTTON;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGNING_TAB_TEXT;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_CHECK_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_DESTINATION_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_PACKAGE_DESTINATION_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_BROWSE_BUTTON_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_KEYSTORE_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_KEY_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_USE_DEFAULT_DESTINATION;

	public static String PACKAGE_EXPORT_WIZARD_AREA_READONLY_MESSAGE;

	public static String PACKAGE_EXPORT_WIZARD_AREA_READONLY_TITLE;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_NO_KEYSTORE_AVAILABLE;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_NO_KEYSTORE_OR_KEY_SELECTED;

	public static String PACKAGE_EXPORT_WIZARD_AREA_SIGN_BUTTON_OTHER_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_CREATE_DIRECTORIES_BOX_TITLE;

	public static String PACKAGE_EXPORT_WIZARD_AREA_CREATE_DIRECTORIES_BOX_MESSAGE;

	public static String PACKAGE_EXPORT_WIZARD_AREA_PROJECTS_WITH_ERRORS_SELECTED;

	public static String PACKAGE_EXPORT_WIZARD_AREA_FINISH_ACTION_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_EXPORTING_ACTION_LABEL;

	public static String PACKAGE_EXPORT_WIZARD_AREA_ERROR_PROJECT_BUILD;

	public static String PACKAGE_EXPORT_WIZARD_AREA_ERROR_DESTINATION_CHECK;

	public static String SELECTOR_MESSAGE_NO_SELECTION;

	public static String SELECTOR_MESSAGE_LOCATION_ERROR_INVALID;

	public static String SELECTOR_MESSAGE_LOCATION_ERROR_NOT_DIRECTORY;

	public static String SELECTOR_MESSAGE_LOCATION_ERROR_INVALID_DEVICE;

	public static String SELECTOR_MESSAGE_LOCATION_ERROR_PATH_TOO_LONG;

	public static String PackageExportWizardArea_AddKeyButton_Text;

	public static String PackageExportWizardArea_ErrorSigningPackage;

	public static String PackageExportWizardArea_ErrorWritingSignedPackageFile;

	public static String PackageExportWizardArea_MenuItem_AddNew;

	public static String PackageExportWizardArea_MenuItem_UseExistent;

	public static String PackageExportWizardArea_WrongKeystoreTypeDialogMessage;

	public static String PackageExportWizardArea_WrongKeystoreTypeDialogTitle;
}
