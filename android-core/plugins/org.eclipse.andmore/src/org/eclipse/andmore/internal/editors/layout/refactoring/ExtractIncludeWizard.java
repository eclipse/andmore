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

import com.android.resources.ResourceType;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class ExtractIncludeWizard extends VisualRefactoringWizard {
    public ExtractIncludeWizard(ExtractIncludeRefactoring ref, LayoutEditorDelegate editor) {
        super(ref, editor);
        setDefaultPageTitle(ref.getName());
    }

    @Override
    protected void addUserInputPages() {
        ExtractIncludeRefactoring ref = (ExtractIncludeRefactoring) getRefactoring();
        String initialName = ref.getInitialName();
        IFile sourceFile = ref.getSourceFile();
        addPage(new InputPage(mDelegate.getEditor().getProject(), sourceFile, initialName));
    }

    /** Wizard page which inputs parameters for the {@link ExtractIncludeRefactoring} operation */
    private static class InputPage extends VisualRefactoringInputPage {
        private final IProject mProject;
        private final IFile mSourceFile;
        private final String mSuggestedName;
        private Text mNameText;
        private Button mReplaceAllOccurrences;

        public InputPage(IProject project, IFile sourceFile, String suggestedName) {
            super("ExtractIncludeInputPage");
            mProject = project;
            mSourceFile = sourceFile;
            mSuggestedName = suggestedName;
        }

        @Override
        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            Label nameLabel = new Label(composite, SWT.NONE);
            nameLabel.setText("New Layout Name:");
            nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

            mNameText = new Text(composite, SWT.BORDER);
            mNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            mNameText.addModifyListener(mModifyValidateListener);

            mReplaceAllOccurrences = new Button(composite, SWT.CHECK);
            mReplaceAllOccurrences.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                    false, false, 2, 1));
            mReplaceAllOccurrences.setText(
                    "Replace occurrences in all layouts with include to new layout");
            mReplaceAllOccurrences.setEnabled(true);
            mReplaceAllOccurrences.setSelection(true);
            mReplaceAllOccurrences.addSelectionListener(mSelectionValidateListener);

            // Initialize UI:
            if (mSuggestedName != null) {
                mNameText.setText(mSuggestedName);
            }

            setControl(composite);
            validatePage();
        }

        @Override
        protected boolean validatePage() {
            boolean ok = true;

            String text = mNameText.getText().trim();

            if (text.length() == 0) {
                setErrorMessage("Provide a name for the new layout");
                ok = false;
            } else {
                ResourceNameValidator validator = ResourceNameValidator.create(false, mProject,
                        ResourceType.LAYOUT);
                String message = validator.isValid(text);
                if (message != null) {
                    setErrorMessage(message);
                    ok = false;
                }
            }

            if (ok) {
                setErrorMessage(null);

                // Record state
                ExtractIncludeRefactoring refactoring =
                    (ExtractIncludeRefactoring) getRefactoring();
                refactoring.setLayoutName(text);
                refactoring.setReplaceOccurrences(mReplaceAllOccurrences.getSelection());
            }

            setPageComplete(ok);
            return ok;
        }
    }
}
