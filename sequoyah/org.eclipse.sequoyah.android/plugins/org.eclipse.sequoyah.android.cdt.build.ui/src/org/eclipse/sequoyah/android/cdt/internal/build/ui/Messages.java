/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Carlos Alberto Souto Junior (Eldorado)
 * 
 * Contributors:
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME =
            "org.eclipse.sequoyah.android.cdt.internal.build.ui.messages"; //$NON-NLS-1$

    public static String AddNativeProjectPage__Message_AddingNativeSupport;

    public static String AddNativeProjectPage_ChangePerspectiveDialogQuestion;

    public static String AddNativeProjectPage_ChangePerspectiveDialogTitle;

    public static String AddNativeProjectPage_empty_library_name_msg;

    public static String AddNativeProjectPage_library_name_group_text;

    public static String AddNativeProjectPage_native_page_description;

    public static String AddNativeProjectPage_native_page_title;

    public static String AddNativeProjectPage_ndk_invalid_path_msg;

    public static String AddNativeProjectPage_ndk_location_group_text;

    public static String AddNativeProjectPage_ndk_preference_link_text;

    public static String AddNativeProjectPage_project_group_text;

    public static String AddNativeProjectPage_project_invalid_msg;

    public static String AddNativeWizard__Message_UnexpectedErrorWhileAddingNativeSupport;

    public static String AddNativeWizard_native_wizard_title;

    public static String NDKCompletionProposal_QuickFixProposal0;

    public static String NDKNativeMethodDetectionVisitor_ProblemName0;

    public static String NDKPreferencePage_invalid_NDK_path_msg;

    public static String NDKPreferencePage_NDK_location;

    public static String JNI_C_FILES_CREATION_MONITOR_TASK_NAME;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_TASK_NAME;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_STEP0;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_STEP1;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_STEP2;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_DIRECTORY_SELECTION_TITLE;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_CREATED;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_ERROR;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_NEED_TO_REWRITE_TITLE;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_NEED_TO_REWRITE_MESSAGE;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_CREATED_AFTER_REWRITE_MESSAGE;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_ONLYHEADER_CREATED_MESSAGE;

    public static String JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_SOURCEHEADER_CREATED_MESSAGE;

    public static String ERR_JNI_JDK_Not_Found;

    public static String ERR_JNI_JDK_Not_Found_Dialog_Title;

    public static String AddNativeProjectAction_InvalidProjectLocation_Title;

    public static String AddNativeProjectAction_InvalidProjectLocation_Message;

    public static String PROJECTCHOOSER_TITLE;

    public static String PROJECTCHOOSER_MESSAGE;

    public static String PROJECTCHOOSER_PROJECT_LABEL;

    public static String PROJECTCHOOSER_BROWSE_BUTTON;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}