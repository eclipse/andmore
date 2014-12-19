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
package org.eclipse.andmore.android.help.handlers;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

public class OpenOnlineHelpStudioHandler extends AbstractHandler {

	private static final String HREF = "/com.motorola.studio.android.tooldocs.studio.helpbase/topics/c_android-studio.html";

	@Override
	public final Object execute(final ExecutionEvent event) {
		try {
			final IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();

			if (helpSystem != null) {
				helpSystem.displayHelpResource(HREF);
			}

		} catch (Exception e) {
			StudioLogger.error("Error opening Help Contents through MOTODEV menu: " + e.getMessage());
		}
		return null;
	}

}
