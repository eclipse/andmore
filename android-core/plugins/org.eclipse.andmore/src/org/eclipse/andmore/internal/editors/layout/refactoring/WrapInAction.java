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

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

/**
 * Action executed when the "Wrap In" menu item is invoked.
 */
public class WrapInAction extends VisualRefactoringAction {
    @Override
    public void run(IAction action) {
        if ((mTextSelection != null || mTreeSelection != null) && mFile != null) {
            WrapInRefactoring ref = new WrapInRefactoring(mFile, mDelegate,
                    mTextSelection, mTreeSelection);
            RefactoringWizard wizard = new WrapInWizard(ref, mDelegate);
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
            try {
                op.run(mWindow.getShell(), wizard.getDefaultPageTitle());
            } catch (InterruptedException e) {
                // Interrupted. Pass.
            }
        }
    }

    public static IAction create(LayoutEditorDelegate editorDelegate) {
        return create("Wrap in Container...", editorDelegate, WrapInAction.class);
    }
}
