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

import com.android.tools.lint.checks.UseCompoundDrawableDetector;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.refactoring.UseCompoundDrawableRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Node;

/** Quickfix for the {@link UseCompoundDrawableDetector} */
@SuppressWarnings("restriction") // DOM model
class UseCompoundDrawableDetectorFix extends DocumentFix {
    protected UseCompoundDrawableDetectorFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public String getDisplayString() {
        return "Convert to a compound drawable";
    }

    @Override
    public Image getImage() {
        return AdtPlugin.getAndroidLogo();
    }

    @Override
    public boolean needsFocus() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public boolean isBulkCapable() {
        return false;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node,
            int start, int end) {

        // Invoke refactoring
        LayoutEditorDelegate delegate =
                LayoutEditorDelegate.fromEditor(AdtUtils.getActiveEditor());

        if (delegate != null) {
            IFile file = (IFile) mMarker.getResource();
            ITextSelection textSelection = new TextSelection(start,
                    end - start);
            UseCompoundDrawableRefactoring refactoring =
                    new UseCompoundDrawableRefactoring(file, delegate, textSelection, null);
            RefactoringWizard wizard = refactoring.createWizard();
            RefactoringWizardOpenOperation op =
                    new RefactoringWizardOpenOperation(wizard);
            try {
                IWorkbenchWindow window = PlatformUI.getWorkbench().
                        getActiveWorkbenchWindow();
                op.run(window.getShell(), wizard.getDefaultPageTitle());
            } catch (InterruptedException e) {
            }
        }
    }
}
