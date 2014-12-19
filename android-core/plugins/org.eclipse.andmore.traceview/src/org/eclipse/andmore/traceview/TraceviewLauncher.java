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

package org.eclipse.andmore.traceview;

import org.eclipse.andmore.ddms.ITraceviewLauncher;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;

public class TraceviewLauncher implements ITraceviewLauncher {

	@Override
	public boolean openFile(String osPath) {
		final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(osPath));
		if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
			// before we open the file in an editor window, we make sure the
			// current
			// workbench page has an editor area (typically the ddms perspective
			// doesn't).
			final IWorkbench workbench = PlatformUI.getWorkbench();
			Display display = workbench.getDisplay();
			final boolean[] result = new boolean[] { false };
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();
					if (page.isEditorAreaVisible() == false) {
						IAdaptable input;
						if (page != null)
							input = page.getInput();
						else
							input = ResourcesPlugin.getWorkspace().getRoot();
						try {
							workbench.showPerspective("org.eclipse.debug.ui.DebugPerspective", window, input);
						} catch (WorkbenchException e) {
						}
					}

					try {
						result[0] = IDE.openEditorOnFileStore(page, fileStore) != null;
					} catch (PartInitException e) {
						// return false below
					}
				}
			});

			return result[0];
		}

		return false;
	}
}
