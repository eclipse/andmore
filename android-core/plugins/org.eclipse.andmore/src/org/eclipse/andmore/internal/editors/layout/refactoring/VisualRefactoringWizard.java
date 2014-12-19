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
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public abstract class VisualRefactoringWizard extends RefactoringWizard {
    protected final LayoutEditorDelegate mDelegate;

    public VisualRefactoringWizard(Refactoring refactoring, LayoutEditorDelegate editor) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
        mDelegate = editor;
    }

    @Override
    public boolean performFinish() {
        mDelegate.getEditor().setIgnoreXmlUpdate(true);
        try {
            return super.performFinish();
        } finally {
            mDelegate.getEditor().setIgnoreXmlUpdate(false);
            mDelegate.refreshXmlModel();
        }
    }

    protected abstract static class VisualRefactoringInputPage extends UserInputWizardPage {
        public VisualRefactoringInputPage(String name) {
            super(name);
        }

        /**
         * Listener which can be attached on any widget in the wizard page to force
         * modifications of the associated widget to validate the page again
         */
        protected ModifyListener mModifyValidateListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validatePage();
            }
        };

        /**
         * Listener which can be attached on any widget in the wizard page to force
         * selection changes of the associated widget to validate the page again
         */
        protected SelectionAdapter mSelectionValidateListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validatePage();
            }
        };

        protected abstract boolean validatePage();
    }
}
