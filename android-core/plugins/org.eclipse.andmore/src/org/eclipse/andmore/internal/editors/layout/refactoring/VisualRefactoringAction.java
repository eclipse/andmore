/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.internal.editors.layout.refactoring;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;

abstract class VisualRefactoringAction implements IWorkbenchWindowActionDelegate {
    protected IWorkbenchWindow mWindow;
    protected ITextSelection mTextSelection;
    protected ITreeSelection mTreeSelection;
    protected LayoutEditorDelegate mDelegate;
    protected IFile mFile;

    /**
     * Keep track of the current workbench window.
     */
    @Override
    public void init(IWorkbenchWindow window) {
        mWindow = window;
    }

    @Override
    public void dispose() {
    }

    /**
     * Examine the selection to determine if the action should be enabled or not.
     * <p/>
     * Keep a link to the relevant selection structure
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // Look for selections in XML and in the layout UI editor

        // Note, two kinds of selections are returned here:
        // ITextSelection on a Java source window
        // IStructuredSelection in the outline or navigator
        // This simply deals with the refactoring based on a non-empty selection.
        // At that point, just enable the action and later decide if it's valid when it actually
        // runs since we don't have access to the AST yet.

        mTextSelection = null;
        mTreeSelection = null;
        mFile = null;

        IEditorPart editor = null;

        if (selection instanceof ITextSelection) {
            mTextSelection = (ITextSelection) selection;
            editor = AdtUtils.getActiveEditor();
            mFile = getSelectedFile(editor);
        } else if (selection instanceof ITreeSelection) {
             Object firstElement = ((ITreeSelection)selection).getFirstElement();
             if (firstElement instanceof CanvasViewInfo) {
                 mTreeSelection = (ITreeSelection) selection;
                 editor = AdtUtils.getActiveEditor();
                 mFile = getSelectedFile(editor);
             }
        }

        mDelegate = LayoutEditorDelegate.fromEditor(editor);

        action.setEnabled((mTextSelection != null || mTreeSelection != null)
                && mFile != null && mDelegate != null);
    }

    /**
     * Create a new instance of our refactoring and a wizard to configure it.
     */
    @Override
    public abstract void run(IAction action);

    /**
     * Returns the active {@link IFile} (hopefully matching our selection) or null.
     * The file is only returned if it's a file from a project with an Android nature.
     * <p/>
     * At that point we do not try to analyze if the selection nor the file is suitable
     * for the refactoring. This check is performed when the refactoring is invoked since
     * it can then produce meaningful error messages as needed.
     */
    private IFile getSelectedFile(IEditorPart editor) {
        if (editor != null) {
            IEditorInput input = editor.getEditorInput();

            if (input instanceof FileEditorInput) {
                FileEditorInput fi = (FileEditorInput) input;
                IFile file = fi.getFile();
                if (file.exists()) {
                    IProject proj = file.getProject();
                    try {
                        if (proj != null && proj.hasNature(AdtConstants.NATURE_DEFAULT)) {
                            return file;
                        }
                    } catch (CoreException e) {
                        // ignore
                    }
                }
            }
        }

        return null;
    }

    public static IAction create(String title, LayoutEditorDelegate editorDelegate,
            Class<? extends VisualRefactoringAction> clz) {
        return new ActionWrapper(title, editorDelegate, clz);
    }

    private static class ActionWrapper extends Action {
        private Class<? extends VisualRefactoringAction> mClass;
        private LayoutEditorDelegate mEditorDelegate;

        ActionWrapper(String title, LayoutEditorDelegate editorDelegate,
                Class<? extends VisualRefactoringAction> clz) {
            super(title);
            mEditorDelegate = editorDelegate;
            mClass = clz;
        }

        @Override
        public void run() {
            VisualRefactoringAction action;
            try {
                action = mClass.newInstance();
            } catch (Exception e) {
                AdtPlugin.log(e, null);
                return;
            }
            IEditorSite site = mEditorDelegate.getEditor().getEditorSite();
            action.init(site.getWorkbenchWindow());
            ISelection selection = site.getSelectionProvider().getSelection();
            action.selectionChanged(ActionWrapper.this, selection);
            if (isEnabled()) {
                action.run(ActionWrapper.this);
            }
        }
    }
}
