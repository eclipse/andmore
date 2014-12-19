/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.draw9patch;

import static com.android.SdkConstants.DOT_9PNG;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage;
import org.eclipse.andmore.internal.editors.draw9patch.ui.ImageViewer;
import org.eclipse.andmore.internal.editors.draw9patch.ui.MainFrame;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Draw9Patch editor part.
 */
public class Draw9PatchEditor extends EditorPart implements ImageViewer.UpdateListener {

    private IProject mProject = null;

    private FileEditorInput mFileEditorInput = null;

    private String mFileName = null;

    private NinePatchedImage mNinePatchedImage = null;

    private MainFrame mMainFrame = null;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());

        // The contract of init() mentions we need to fail if we can't
        // understand the input.
        if (input instanceof FileEditorInput) {
            // We try to open a file that is part of the current workspace
            mFileEditorInput = (FileEditorInput) input;
            mFileName = mFileEditorInput.getName();
            mProject = mFileEditorInput.getFile().getProject();
        } else {
            throw new PartInitException("Input is not of type FileEditorInput " + //$NON-NLS-1$
                    "nor FileStoreEditorInput: " + //$NON-NLS-1$
                    input == null ? "null" : input.toString()); //$NON-NLS-1$
        }

    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public void doSaveAs() {
        IPath relativePath = null;
        if ((relativePath = showSaveAsDialog()) != null) {
            mFileEditorInput = new FileEditorInput(ResourcesPlugin.getWorkspace().getRoot()
                    .getFile(relativePath));
            mFileName = mFileEditorInput.getName();
            setInput(mFileEditorInput);

            doSave(new NullProgressMonitor());
        }
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        boolean hasNinePatchExtension = mFileName.endsWith(DOT_9PNG);
        boolean doConvert = false;

        if (!hasNinePatchExtension) {
            String patchedName = NinePatchedImage.getNinePatchedFileName(mFileName);
            doConvert = MessageDialog
                    .openQuestion(AdtPlugin.getDisplay().getActiveShell(),
                            "Warning",
                            String.format(
                                    "The file \"%s\" doesn't seem to be a 9-patch file. \n"
                                            + "Do you want to convert and save as \"%s\" ?",
                                    mFileName, patchedName));

            if (doConvert) {
                IFile destFile = mProject.getFile(NinePatchedImage.getNinePatchedFileName(
                        mFileEditorInput.getFile().getProjectRelativePath().toOSString()));
                if (!destFile.exists()) {
                    mFileEditorInput = new FileEditorInput(destFile);
                    mFileName = mFileEditorInput.getName();
                } else {
                    IPath relativePath = null;
                    if ((relativePath = showSaveAsDialog()) != null) {
                        mFileEditorInput = new FileEditorInput(ResourcesPlugin.getWorkspace()
                                .getRoot().getFile(relativePath));
                        mFileName = mFileEditorInput.getName();
                    } else {
                        doConvert = false;
                    }
                }
            }
        }

        if (hasNinePatchExtension || doConvert) {
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] {
                mNinePatchedImage.getRawImageData()
            };

            IFile file = mFileEditorInput.getFile();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            loader.save(outputStream, SWT.IMAGE_PNG);
            byte[] byteArray = outputStream.toByteArray();

            try {
                if (file.exists()) {
                    file.setContents(new ByteArrayInputStream(byteArray), true, false, monitor);
                } else {
                    file.create(new ByteArrayInputStream(byteArray), true, monitor);
                }

                mNinePatchedImage.clearDirtyFlag();

                AdtPlugin.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        setPartName(mFileName);
                        firePropertyChange(PROP_DIRTY);
                    }
                });
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            }
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        mMainFrame = new MainFrame(parent, SWT.NULL);

        ImageViewer imageViewer = mMainFrame.getImageEditorPanel().getImageViewer();
        imageViewer.addUpdateListener(this);

        mNinePatchedImage = imageViewer.loadFile(mFileEditorInput.getPath().toOSString());
        if (mNinePatchedImage.hasNinePatchExtension()) {
            if (!mNinePatchedImage.ensure9Patch() && showConvertMessageBox(mFileName)) {
                // Reload image
                mNinePatchedImage = imageViewer.loadFile(mFileEditorInput.getPath().toOSString());
                mNinePatchedImage.convertToNinePatch();
            }
        } else {
            mNinePatchedImage.convertToNinePatch();
        }

        imageViewer.startDisplay();

        parent.layout();
    }

    @Override
    public void setFocus() {
        mMainFrame.forceFocus();
    }

    @Override
    public boolean isDirty() {
        return mNinePatchedImage.isDirty();
    }

    @Override
    public void update(NinePatchedImage image) {
        if (image.isDirty()) {
            firePropertyChange(PROP_DIRTY);
        }
    }

    private IPath showSaveAsDialog() {
        SaveAsDialog dialog = new SaveAsDialog(AdtPlugin.getDisplay().getActiveShell());

        IFile dest = mProject.getFile(NinePatchedImage.getNinePatchedFileName(
                mFileEditorInput.getFile().getProjectRelativePath().toOSString()));
        dialog.setOriginalFile(dest);

        dialog.create();

        if (dialog.open() == Window.CANCEL) {
            return null;
        }

        return dialog.getResult();
    }

    private static boolean showConvertMessageBox(String fileName) {
        return MessageDialog.openQuestion(
                AdtPlugin.getDisplay().getActiveShell(),
                "Warning",
                String.format("The file \"%s\" doesn't seem to be a 9-patch file. \n"
                        + "Do you want to convert?", fileName));
    }
}
