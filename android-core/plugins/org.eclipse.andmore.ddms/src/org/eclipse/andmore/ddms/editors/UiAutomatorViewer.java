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

package org.eclipse.andmore.ddms.editors;

import com.android.uiautomator.UiAutomatorHelper.UiAutomatorResult;
import com.android.uiautomator.UiAutomatorModel;
import com.android.uiautomator.UiAutomatorView;

import org.eclipse.andmore.base.InstallDetails;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class UiAutomatorViewer extends EditorPart {
	private String mFilePath;
	private UiAutomatorView mView;

	@Override
	public void doSave(IProgressMonitor arg0) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// we use a IURIEditorInput to allow opening files not within the
		// workspace
		if (!(input instanceof IURIEditorInput)) {
			throw new PartInitException("UI Automator Hierarchy View: unsupported input type.");
		}

		setSite(site);
		setInput(input);
		mFilePath = ((IURIEditorInput) input).getURI().getPath();

		// set the editor part name to be the name of the file.
		File f = new File(mFilePath);
		setPartName(f.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);

		mView = new UiAutomatorView(c, SWT.BORDER);
		mView.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (mFilePath == null) {
			return;
		}

		UiAutomatorModel model = null;
		File modelFile = new File(mFilePath);
		try {
			model = new UiAutomatorModel(modelFile);
		} catch (Exception e) {
			MessageDialog.openError(parent.getShell(), "Error opening " + mFilePath,
					"Unexpected error while parsing input: " + e.getMessage());
			return;
		}

		mView.setModel(model, modelFile, null);
	}

	@Override
	public void setFocus() {
	}

	public static boolean openEditor(final UiAutomatorResult r) {
		final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(r.uiHierarchy.getAbsolutePath()));
		if (!fileStore.fetchInfo().exists()) {
			return false;
		}

		final AtomicBoolean status = new AtomicBoolean(false);

		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window == null) {
					return;
				}

				IWorkbenchPage page = window.getActivePage();
				if (page == null) {
					return;
				}

				// try to switch perspectives if possible
				if (page.isEditorAreaVisible() == false && InstallDetails.isAdtInstalled()) {
					try {
						workbench.showPerspective("org.eclipse.jdt.ui.JavaPerspective", window); //$NON-NLS-1$
					} catch (WorkbenchException e) {
					}
				}

				IEditorPart editor = null;
				try {
					editor = IDE.openEditorOnFileStore(page, fileStore);
				} catch (PartInitException e) {
					return;
				}

				if (!(editor instanceof UiAutomatorViewer)) {
					return;
				}

				((UiAutomatorViewer) editor).setModel(r.model, r.uiHierarchy, r.screenshot);
				status.set(true);
			}
		});

		return status.get();
	}

	protected void setModel(UiAutomatorModel model, File modelFile, Image screenshot) {
		mView.setModel(model, modelFile, screenshot);
	}
}
