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
package org.eclipse.andmore.traceview.editors;

import com.android.traceview.ColorController;
import com.android.traceview.DmTraceReader;
import com.android.traceview.MethodData;
import com.android.traceview.ProfileView;
import com.android.traceview.ProfileView.MethodHandler;
import com.android.traceview.SelectionController;
import com.android.traceview.TimeLineView;
import com.android.traceview.TraceReader;
import com.android.traceview.TraceUnits;

import org.eclipse.andmore.ddms.JavaSourceRevealer;
import org.eclipse.andmore.traceview.TraceviewPlugin;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class TraceviewEditor extends EditorPart implements MethodHandler {

	private Composite mParent;
	private String mFilename;
	private Composite mContents;

	@Override
	public void doSave(IProgressMonitor monitor) {
		// We do not modify the file
	}

	/*
	 * Copied from org.eclipse.ui.texteditor.AbstractDecoratedTextEditor.
	 */
	/**
	 * Checks whether there given file store points to a file in the workspace.
	 * Only returns a workspace file if there's a single match.
	 *
	 * @param fileStore
	 *            the file store
	 * @return the <code>IFile</code> that matches the given file store
	 */
	private IFile getWorkspaceFile(IFileStore fileStore) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IFile[] files = workspaceRoot.findFilesForLocationURI(fileStore.toURI());
		if (files != null && files.length == 1)
			return files[0];
		return null;
	}

	/*
	 * Based on the performSaveAs() method defined in class
	 * org.eclipse.ui.texteditor.AbstractDecoratedTextEditor of the
	 * org.eclipse.ui.editors plugin.
	 */
	@Override
	public void doSaveAs() {
		Shell shell = getSite().getShell();
		final IEditorInput input = getEditorInput();

		final IEditorInput newInput;

		if (input instanceof FileEditorInput) {
			// the file is part of the current workspace
			FileEditorInput fileEditorInput = (FileEditorInput) input;
			SaveAsDialog dialog = new SaveAsDialog(shell);

			IFile original = fileEditorInput.getFile();
			if (original != null) {
				dialog.setOriginalFile(original);
			}

			dialog.create();

			if (original != null && !original.isAccessible()) {
				String message = String.format("The original file ''%s'' has been deleted or is not accessible.",
						original.getName());
				dialog.setErrorMessage(null);
				dialog.setMessage(message, IMessageProvider.WARNING);
			}

			if (dialog.open() == Window.CANCEL) {
				return;
			}

			IPath filePath = dialog.getResult();
			if (filePath == null) {
				return;
			}

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IFile file = workspace.getRoot().getFile(filePath);

			if (copy(shell, fileEditorInput.getURI(), file.getLocationURI()) == null) {
				return;
			}

			try {
				file.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			newInput = new FileEditorInput(file);
			setInput(newInput);
			setPartName(newInput.getName());
		} else if (input instanceof FileStoreEditorInput) {
			// the file is not part of the current workspace
			FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) input;
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			IPath oldPath = URIUtil.toPath(fileStoreEditorInput.getURI());
			if (oldPath != null) {
				dialog.setFileName(oldPath.lastSegment());
				dialog.setFilterPath(oldPath.toOSString());
			}

			String path = dialog.open();
			if (path == null) {
				return;
			}

			// Check whether file exists and if so, confirm overwrite
			final File localFile = new File(path);
			if (localFile.exists()) {
				MessageDialog overwriteDialog = new MessageDialog(shell, "Save As", null, String.format(
						"%s already exists.\nDo you want to replace it?", path), MessageDialog.WARNING, new String[] {
						IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 1); // 'No'
																						// is
																						// the
																						// default
				if (overwriteDialog.open() != Window.OK) {
					return;
				}
			}

			IFileStore destFileStore = copy(shell, fileStoreEditorInput.getURI(), localFile.toURI());
			if (destFileStore != null) {
				IFile file = getWorkspaceFile(destFileStore);
				if (file != null) {
					newInput = new FileEditorInput(file);
				} else {
					newInput = new FileStoreEditorInput(destFileStore);
				}
				setInput(newInput);
				setPartName(newInput.getName());
			}
		}
	}

	private IFileStore copy(Shell shell, URI source, URI dest) {
		IFileStore destFileStore = null;
		IFileStore sourceFileStore = null;
		try {
			destFileStore = EFS.getStore(dest);
			sourceFileStore = EFS.getStore(source);
			sourceFileStore.copy(destFileStore, EFS.OVERWRITE, null);
		} catch (CoreException ex) {
			String title = "Problems During Save As...";
			String msg = String.format("Save could not be completed. %s", ex.getMessage());
			MessageDialog.openError(shell, title, msg);
			return null;
		}
		return destFileStore;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// The contract of init() mentions we need to fail if we can't
		// understand the input.
		if (input instanceof FileEditorInput) {
			// We try to open a file that is part of the current workspace
			FileEditorInput fileEditorInput = (FileEditorInput) input;
			mFilename = fileEditorInput.getPath().toOSString();
			setSite(site);
			setInput(input);
			setPartName(input.getName());
		} else if (input instanceof FileStoreEditorInput) {
			// We try to open a file that is not part of the current workspace
			FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) input;
			mFilename = fileStoreEditorInput.getURI().getPath();
			setSite(site);
			setInput(input);
			setPartName(input.getName());
		} else {
			throw new PartInitException("Input is not of type FileEditorInput " + //$NON-NLS-1$
					"nor FileStoreEditorInput: " + //$NON-NLS-1$
					input == null ? "null" : input.toString()); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void createPartControl(Composite parent) {
		mParent = parent;
		try {
			TraceReader reader = new DmTraceReader(mFilename, false);
			reader.getTraceUnits().setTimeScale(TraceUnits.TimeScale.MilliSeconds);

			mContents = new Composite(mParent, SWT.NONE);

			Display display = mContents.getDisplay();
			ColorController.assignMethodColors(display, reader.getMethods());
			SelectionController selectionController = new SelectionController();

			GridLayout gridLayout = new GridLayout(1, false);
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 0;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 0;
			mContents.setLayout(gridLayout);

			Color darkGray = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			// Create a sash form to separate the timeline view (on top)
			// and the profile view (on bottom)
			SashForm sashForm1 = new SashForm(mContents, SWT.VERTICAL);
			sashForm1.setBackground(darkGray);
			sashForm1.SASH_WIDTH = 3;
			GridData data = new GridData(GridData.FILL_BOTH);
			sashForm1.setLayoutData(data);

			// Create the timeline view
			new TimeLineView(sashForm1, reader, selectionController);

			// Create the profile view
			new ProfileView(sashForm1, reader, selectionController).setMethodHandler(this);
		} catch (IOException e) {
			Label l = new Label(parent, 0);
			l.setText("Failed to read the stack trace.");

			Status status = new Status(IStatus.ERROR, TraceviewPlugin.PLUGIN_ID, "Failed to read the stack trace.", e);
			TraceviewPlugin.getDefault().getLog().log(status);
		}

		mParent.layout();
	}

	@Override
	public void setFocus() {
		mParent.setFocus();
	}

	// ---- MethodHandler methods

	@Override
	public void handleMethod(MethodData method) {
		String methodName = method.getMethodName();
		String className = method.getClassName().replaceAll("/", "."); //$NON-NLS-1$ //$NON-NLS-2$
		String fqmn = className + "." + methodName; //$NON-NLS-1$

		JavaSourceRevealer.revealMethod(fqmn, null, -1, null);
	}
}
