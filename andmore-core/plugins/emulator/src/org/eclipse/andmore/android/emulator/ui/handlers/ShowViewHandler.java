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
package org.eclipse.andmore.android.emulator.ui.handlers;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.ui.view.AndroidView;
import org.eclipse.andmore.android.emulator.ui.view.MainDisplayView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;

/**
 * DESCRIPTION: This class is a handler for the show view actions
 *
 * RESPONSIBILITY: Execute the show view operations on demand
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public class ShowViewHandler extends AbstractHandler implements IHandlerConstants {
	/**
	 * @see org.eclipse.core.commands.IHandler#execute(ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String viewId = event.getParameter(IHandlerConstants.ACTIVE_VIEW_PARAMETER);

		try {
			if (viewId.equals(AndroidView.ANDROID_VIEW_ID)) {
				info("Showing Main Display View by command execution");
				EclipseUtils.showView(MainDisplayView.EMULATOR_MAIN_DISPLAY_VIEW_ID);
			} else if (viewId.equals(MainDisplayView.EMULATOR_MAIN_DISPLAY_VIEW_ID)) {
				info("Showing Android View by command execution");
				EclipseUtils.showView(AndroidView.ANDROID_VIEW_ID);
			} else {
				info("User tried to open an unknown view. Ignoring the action.");
			}
		} catch (PartInitException e) {
			error("The views that were requested to be opened are not accessible programatically");
			EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_AndroidView_ViewNotFound);
		}

		return null;
	}
}
