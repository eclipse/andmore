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
package org.eclipse.andmore.android.perspective;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * Command to open the MOTODEV Web Resources in a Web Browser Editor
 */
public class OpenWebResources extends AbstractHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (!Platform.getOS().equals(Platform.OS_LINUX)) {
			IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (activeWindow != null) {
				IWorkbenchPage activePage = activeWindow.getActivePage();
				MotodevStudioAndroidPerspective.openStudioInfoOnWebBrowserEditor(activePage);
			}
		} else {

			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();

			/*
			 * open the browser
			 */
			IWebBrowser browser;
			try {
				browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.LOCATION_BAR
						| IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EXTERNAL, "MOTODEV",
						null, null);

				browser.openURL(MotodevStudioAndroidPerspective.getWebResourcesURL());

			} catch (PartInitException e) {
				StudioLogger.error("Error opening the Web Resources page: " + e.getMessage());
			}

		}
		return null;
	}

}
