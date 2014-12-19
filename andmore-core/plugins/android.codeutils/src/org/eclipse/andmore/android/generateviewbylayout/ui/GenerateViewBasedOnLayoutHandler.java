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
package org.eclipse.andmore.android.generateviewbylayout.ui;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to modify source code (activity / fragment) based on layout xml
 */
public class GenerateViewBasedOnLayoutHandler extends AbstractCodeGeneratorHandler {
	/**
	 * Open {@link ChooseLayoutItemsDialog} dialog to select the items to
	 * generate code for (depending on the selected layout on combo box). After
	 * finish button click, it modifies Android source code (depending on
	 * Activity or Fragment selected).
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SelectionBean selectionBean = resolveSelection(event);

		if (selectionBean.isProject() || selectionBean.isAllowedClassInstance()) {
			final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
			executeCodeGenerationWizard(event, selectionBean.getJavaFile(), selectionBean.getJavaProject(),
					new ChooseLayoutItemsDialog(window.getShell()));
		} else {
			EclipseUtils.showErrorDialog(CodeUtilsNLS.GenerateViewBasedOnLayoutHandler_FillJavaActivityBasedOnLayout,
					CodeUtilsNLS.GenerateViewBasedOnLayoutHandler_SelectedClassNeitherActivityFragment);
		}

		return null;
	}

}
