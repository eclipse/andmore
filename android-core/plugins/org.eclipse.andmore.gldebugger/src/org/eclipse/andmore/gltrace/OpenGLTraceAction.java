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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import java.io.File;

public class OpenGLTraceAction implements IWorkbenchWindowActionDelegate {
	private static String sLoadFromFolder = System.getProperty("user.home");

	@Override
	public void run(IAction action) {
		openTrace();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

	private void openTrace() {
		Shell shell = Display.getDefault().getActiveShell();
		FileDialog fd = new FileDialog(shell, SWT.OPEN);

		fd.setText("Open Trace File");
		fd.setFilterPath(sLoadFromFolder);
		fd.setFilterExtensions(new String[] { "*.gltrace" });

		String fname = fd.open();
		if (fname == null || fname.trim().length() == 0) {
			return;
		}

		sLoadFromFolder = new File(fname).getParent().toString();

		openEditorFor(fname);
	}

	private void openEditorFor(String fname) {
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(fname));
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error opening GL Trace File",
					"Unexpected error while opening GL Trace file: " + e.getMessage());
		}
	}
}
