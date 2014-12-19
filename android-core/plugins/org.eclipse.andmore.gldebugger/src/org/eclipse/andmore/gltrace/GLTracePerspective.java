/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace;

import org.eclipse.andmore.gltrace.views.FrameSummaryView;
import org.eclipse.andmore.gltrace.views.StateView;
import org.eclipse.andmore.gltrace.views.detail.DetailsView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class GLTracePerspective implements IPerspectiveFactory {
	private static final String STATE_FOLDER_ID = "stateFolder"; //$NON-NLS-1$
	private static final String FB_FOLDER_ID = "fbFolder"; //$NON-NLS-1$
	private static final String TEXTURE_VIEW_FOLDER_ID = "textureViewFolder"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// Create a 3 column layout
		// The first column contains the function trace in an editor.
		// The second column contains the GL State View.
		// The third column contains the texture view and the top and the
		// framebuffer at the bottom.

		// Add the OpenGL state view to the right of the editor
		IFolderLayout column2 = layout.createFolder(STATE_FOLDER_ID, IPageLayout.RIGHT, 0.65f, layout.getEditorArea());
		column2.addView(StateView.ID);

		// Add the Texture View in the 3rd column
		IFolderLayout column3 = layout.createFolder(FB_FOLDER_ID, IPageLayout.RIGHT, 0.6f, STATE_FOLDER_ID);
		column3.addView(DetailsView.ID);

		// Add the OpenGL Framebuffer view below the texture view (bottom of 3rd
		// column)
		IFolderLayout column3bottom = layout.createFolder(TEXTURE_VIEW_FOLDER_ID, IPageLayout.BOTTOM, 0.5f,
				FB_FOLDER_ID);
		column3bottom.addView(FrameSummaryView.ID);
	}
}
