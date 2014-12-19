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

import com.android.tools.lint.checks.UselessViewDetector;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.refactoring.UnwrapRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("restriction") // DOM model
final class RemoveUselessViewFix extends DocumentFix {
    private RemoveUselessViewFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public boolean needsFocus() {
        return isCancelable();
    }

    @Override
    public boolean isCancelable() {
        return mId.equals(mId.equals(UselessViewDetector.USELESS_PARENT.getId()));
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        if (node instanceof Element && node.getParentNode() instanceof Element) {
            Element element = (Element) node;
            Element parent = (Element) node.getParentNode();

            if (mId.equals(UselessViewDetector.USELESS_LEAF.getId())) {
                parent.removeChild(element);
            } else {
                assert mId.equals(UselessViewDetector.USELESS_PARENT.getId());
                // Invoke refactoring
                LayoutEditorDelegate delegate =
                    LayoutEditorDelegate.fromEditor(AdtUtils.getActiveEditor());

                if (delegate != null) {
                    IFile file = (IFile) mMarker.getResource();
                    ITextSelection textSelection = new TextSelection(start,
                            end - start);
                    UnwrapRefactoring refactoring =
                            new UnwrapRefactoring(file, delegate, textSelection, null);
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
    }

    @Override
    public String getDisplayString() {
        return "Remove unnecessary view";
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImage(ISharedImages.IMG_ETOOL_DELETE);
    }
}