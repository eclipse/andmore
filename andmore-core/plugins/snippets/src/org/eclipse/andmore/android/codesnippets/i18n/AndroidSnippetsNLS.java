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
package org.eclipse.andmore.android.codesnippets.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * This class is the NLS component for Code Snippets plug-in
 * 
 */
public class AndroidSnippetsNLS extends NLS {
	/**
	 * The bundle location. It refers to messages.properties file inside this
	 * package
	 */
	private static final String BUNDLE_NAME = "org.eclipse.andmore.android.codesnippets.i18n.androidSnippetsNLS";

	static {
		NLS.initializeMessages(BUNDLE_NAME, AndroidSnippetsNLS.class);
	}

	public static String AndroidPermissionInsertSnippet_Msg_AddToManifest_Msg;

	public static String AndroidPermissionInsertSnippet_Msg_AddToManifest_Title;

	public static String AndroidPermissionInsertSnippet_PermissionPrefix;

	public static String AndroidPermissionInsertSnippet_PermissionSuffix;

	public static String TooltipDisplayConfigContriutionItem_ShowPreview;

	/*
	 * UI strings area
	 */
	public static String UI_Snippet_Preview;

	public static String UI_Snippet_SearchLabel;

}
