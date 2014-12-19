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
package org.eclipse.andmore.internal.refactorings.core;

import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.R_CLASS;

import com.android.resources.ResourceType;

import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.Set;

@SuppressWarnings("restriction") // JDT refactoring UI
class RenameResourcePage extends TextInputWizardPage implements SelectionListener {
    private Label mXmlLabel;
    private Label mJavaLabel;
    private Button mUpdateReferences;
    private boolean mCanClear;
    private ResourceType mType;
    private ResourceNameValidator mValidator;

    /**
     * Create the wizard.
     * @param type the type of the resource to be renamed
     * @param initial initial renamed value
     * @param canClear whether the dialog should allow clearing the field
     */
    public RenameResourcePage(ResourceType type, String initial, boolean canClear) {
        super(type.getName(), true, initial);
        mType = type;
        mCanClear = canClear;

        mValidator = ResourceNameValidator.create(false /*allowXmlExtension*/,
                (Set<String>) null, mType);
    }

    @SuppressWarnings("unused") // SWT constructors aren't really unused, they have side effects
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        initializeDialogUnits(container);
        container.setLayout(new GridLayout(2, false));
        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        nameLabel.setText("New Name:");
        Text text = super.createTextInputField(container);
        text.selectAll();
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        Label xmlLabel = new Label(container, SWT.NONE);
        xmlLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        xmlLabel.setText("XML:");
        mXmlLabel = new Label(container, SWT.NONE);
        mXmlLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        Label javaLabel = new Label(container, SWT.NONE);
        javaLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        javaLabel.setText("Java:");
        mJavaLabel = new Label(container, SWT.NONE);
        mJavaLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        mUpdateReferences = new Button(container, SWT.CHECK);
        mUpdateReferences.setSelection(true);
        mUpdateReferences.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mUpdateReferences.setText("Update References");
        mUpdateReferences.addSelectionListener(this);

        Dialog.applyDialogFont(container);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            RenameResourceProcessor processor = getProcessor();
            String newName = processor.getNewName();
            if (newName != null && newName.length() > 0
                    && !newName.equals(getInitialValue())) {
                Text textField = getTextField();
                textField.setText(newName);
                textField.setSelection(0, newName.length());
            }
        }

        super.setVisible(visible);
    }

    @Override
    protected RefactoringStatus validateTextField(String newName) {
        if (newName.isEmpty() && isEmptyInputValid()) {
            getProcessor().setNewName("");
            return RefactoringStatus.createWarningStatus(
                    "The resource definition will be deleted");
        }

        String error = mValidator.isValid(newName);
        if (error != null) {
            return RefactoringStatus.createErrorStatus(error);
        }

        RenameResourceProcessor processor = getProcessor();
        processor.setNewName(newName);
        return processor.checkNewName(newName);
    }

    private RenameResourceProcessor getProcessor() {
        RenameRefactoring refactoring = (RenameRefactoring) getRefactoring();
        return (RenameResourceProcessor) refactoring.getProcessor();
    }

    @Override
    protected boolean isEmptyInputValid() {
        return mCanClear;
    }

    @Override
    protected boolean isInitialInputValid() {
        RenameResourceProcessor processor = getProcessor();
        return processor.getNewName() != null
                && !processor.getNewName().equals(processor.getCurrentName());
    }

    @Override
    protected void textModified(String text) {
        super.textModified(text);
        if (mXmlLabel != null && mJavaLabel != null) {
            String xml = PREFIX_RESOURCE_REF + mType.getName() + '/' + text;
            String java = R_CLASS + '.' + mType.getName() + '.' + text;
            if (text.isEmpty()) {
                xml = java = "";
            }
            mXmlLabel.setText(xml);
            mJavaLabel.setText(java);
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == mUpdateReferences) {
            RenameResourceProcessor processor = getProcessor();
            boolean update = mUpdateReferences.getSelection();
            processor.setUpdateReferences(update);
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
