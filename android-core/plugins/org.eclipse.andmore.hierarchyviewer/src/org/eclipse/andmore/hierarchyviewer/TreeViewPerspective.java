/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer;

import org.eclipse.andmore.ddms.Perspective;
import org.eclipse.andmore.hierarchyviewer.views.DeviceSelectorView;
import org.eclipse.andmore.hierarchyviewer.views.LayoutView;
import org.eclipse.andmore.hierarchyviewer.views.PropertyView;
import org.eclipse.andmore.hierarchyviewer.views.TreeOverviewView;
import org.eclipse.andmore.hierarchyviewer.views.TreeViewView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class TreeViewPerspective implements IPerspectiveFactory {

	public static final String ID = "org.eclipse.andmore.hierarchyviewer.TreeViewPerspective"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);

		String editorArea = layout.getEditorArea();
		IFolderLayout folder;

		folder = layout.createFolder("properties", IPageLayout.LEFT, 0.10f, editorArea); //$NON-NLS-1$
		folder.addView(DeviceSelectorView.ID);
		folder.addView(PropertyView.ID);

		folder = layout.createFolder("main", IPageLayout.RIGHT, 0.24f, "properties"); //$NON-NLS-1$ //$NON-NLS-2$
		folder.addView(TreeViewView.ID);

		folder = layout.createFolder("panel-top", IPageLayout.RIGHT, 0.7f, "main"); //$NON-NLS-1$ //$NON-NLS-2$
		folder.addView(TreeOverviewView.ID);

		folder = layout.createFolder("panel-bottom", IPageLayout.BOTTOM, 0.5f, "panel-top"); //$NON-NLS-1$ //$NON-NLS-2$
		folder.addView(LayoutView.ID);

		layout.addShowViewShortcut(DeviceSelectorView.ID);
		layout.addShowViewShortcut(PropertyView.ID);
		layout.addShowViewShortcut(TreeOverviewView.ID);
		layout.addShowViewShortcut(LayoutView.ID);
		layout.addShowViewShortcut(TreeViewView.ID);

		layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut(PixelPerfectPespective.ID);
		layout.addPerspectiveShortcut(Perspective.ID);
	}

}
