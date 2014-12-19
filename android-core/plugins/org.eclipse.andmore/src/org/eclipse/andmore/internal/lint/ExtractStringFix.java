/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.lint;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.refactorings.extractstring.ExtractStringRefactoring;
import org.eclipse.andmore.internal.refactorings.extractstring.ExtractStringWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Node;

/**
 * Fix for extracting strings.
 * <p>
 * TODO: Look for existing string values, and if it matches one of the
 * existing Strings offer to just replace it with the given string!
 */
@SuppressWarnings("restriction") // DOM model
final class ExtractStringFix extends DocumentFix {
    private ExtractStringFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public boolean needsFocus() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        IEditorPart editorPart = AdtUtils.getActiveEditor();
        if (editorPart instanceof AndroidXmlEditor) {
            IFile file = (IFile) mMarker.getResource();
            ITextSelection selection = new TextSelection(start, end - start);

            ExtractStringRefactoring refactoring =
                new ExtractStringRefactoring(file, editorPart, selection);
            RefactoringWizard wizard = new ExtractStringWizard(refactoring, file.getProject());
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
            try {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                op.run(window.getShell(), wizard.getDefaultPageTitle());
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public String getDisplayString() {
        return "Extract String";
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImage(ISharedImages.IMG_OBJ_ADD);
    }
}