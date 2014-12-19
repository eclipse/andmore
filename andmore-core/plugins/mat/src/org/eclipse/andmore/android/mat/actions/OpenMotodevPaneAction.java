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

package org.eclipse.andmore.android.mat.actions;

import org.eclipse.andmore.android.mat.Activator;
import org.eclipse.andmore.android.mat.i18n.MatNLS;
import org.eclipse.andmore.android.mat.panes.MotodevPane;
import org.eclipse.jface.action.Action;
import org.eclipse.mat.ui.editor.MultiPaneEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

@SuppressWarnings("restriction")
public class OpenMotodevPaneAction extends Action {

	// Icon image patch
	private static final String ACTION_IMAGE_PATH = "icons/android_oql.png";

	public OpenMotodevPaneAction() {
		super(MatNLS.Action_Open_Motodev_Pane, AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
				ACTION_IMAGE_PATH));
	}

	@Override
	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart part = page == null ? null : page.getActiveEditor();

		if (part instanceof MultiPaneEditor) {
			((MultiPaneEditor) part).addNewPage(MotodevPane.MOTODEV_PANE_ID, null);
		}
	}
}
