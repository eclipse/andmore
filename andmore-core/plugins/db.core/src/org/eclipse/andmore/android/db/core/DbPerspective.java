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
package org.eclipse.andmore.android.db.core;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

/**
 * The MOTODEV Database Perspective factory
 */
public class DbPerspective implements IPerspectiveFactory {
	private static String VIEW_SQL_RESULTS = "org.eclipse.datatools.sqltools.result.resultView"; //$NON-NLS-1$

	private static String VIEW_TML_DEV_MGT = "org.eclipse.sequoyah.device.framework.ui.InstanceMgtView"; //$NON-NLS-1$

	public static String VIEW_MOTODEV_DATABASE = "org.eclipse.andmore.android.db.databaseView"; //$NON-NLS-1$

	private static String VIEW_FILE_EXPLORER = "com.android.ide.eclipse.ddms.views.FileExplorerView"; //$NON-NLS-1$

	private static String VIEW_CONSOLE = "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$

	private static String ACTIONSET_LAUNCH = "org.eclipse.debug.ui.launchActionSet"; //$NON-NLS-1$

	private static String ACTIONSET_NAVIGATE = "org.eclipse.ui.NavigateActionSet"; //$NON-NLS-1$

	private static String PERSPECTIVE_MOTODEV = "org.eclipse.andmore.android.perspective"; //$NON-NLS-1$

	private static String VIEW_SNIPPETS = "org.eclipse.wst.common.snippets.internal.ui.SnippetsView"; //$NON-NLS-1$

	private static String VIEW_ANDROID_EMULATOR = "org.eclipse.andmore.android.emulator.androidView"; //$NON-NLS-1$

	/**
	 * Creates the initial layout for a page.
	 *
	 * @param layout
	 *            the page layout
	 *
	 * @see IPerspectiveFactory#createInitialLayout(IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea); //$NON-NLS-1$
		left.addView(VIEW_MOTODEV_DATABASE);

		IFolderLayout leftBottom = layout.createFolder("leftBottom", IPageLayout.BOTTOM, 0.59f, "left"); //$NON-NLS-1$ //$NON-NLS-2$
		leftBottom.addView(VIEW_SNIPPETS);

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.7f, editorArea); //$NON-NLS-1$
		bottom.addView(VIEW_SQL_RESULTS);
		bottom.addView(VIEW_TML_DEV_MGT);
		bottom.addView(VIEW_CONSOLE);

		IPlaceholderFolderLayout right = layout.createPlaceholderFolder("right", IPageLayout.RIGHT, 0.7f, editorArea); //$NON-NLS-1$
		right.addPlaceholder(VIEW_FILE_EXPLORER);
		right.addPlaceholder(VIEW_ANDROID_EMULATOR);

		layout.addActionSet(ACTIONSET_LAUNCH);
		layout.addActionSet(ACTIONSET_NAVIGATE);

		layout.addShowViewShortcut(VIEW_MOTODEV_DATABASE);
		layout.addShowViewShortcut(VIEW_SQL_RESULTS);
		layout.addShowViewShortcut(VIEW_TML_DEV_MGT);
		layout.addShowViewShortcut(VIEW_FILE_EXPLORER);
		layout.addShowViewShortcut(VIEW_CONSOLE);

		layout.addPerspectiveShortcut(PERSPECTIVE_MOTODEV);
	}
}
