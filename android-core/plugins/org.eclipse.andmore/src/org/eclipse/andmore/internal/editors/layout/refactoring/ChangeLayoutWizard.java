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

import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.FQCN_RELATIVE_LAYOUT;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.SdkConstants.VIEW_MERGE;

import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gre.PaletteMetadataDescriptor;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ChangeLayoutWizard extends VisualRefactoringWizard {

    public ChangeLayoutWizard(ChangeLayoutRefactoring ref, LayoutEditorDelegate editor) {
        super(ref, editor);
        setDefaultPageTitle("Change Layout");
    }

    @Override
    protected void addUserInputPages() {
        ChangeLayoutRefactoring ref = (ChangeLayoutRefactoring) getRefactoring();
        String oldType = ref.getOldType();
        addPage(new InputPage(mDelegate.getEditor().getProject(), oldType));
    }

    /** Wizard page which inputs parameters for the {@link ChangeLayoutRefactoring} operation */
    private static class InputPage extends VisualRefactoringInputPage {
        private final IProject mProject;
        private final String mOldType;
        private Combo mTypeCombo;
        private Button mFlatten;
        private List<Pair<String, ViewElementDescriptor>> mClassNames;

        public InputPage(IProject project, String oldType) {
            super("ChangeLayoutInputPage");  //$NON-NLS-1$
            mProject = project;
            mOldType = oldType;
        }

        @Override
        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            Label fromLabel = new Label(composite, SWT.NONE);
            fromLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
            String oldTypeBase = mOldType.substring(mOldType.lastIndexOf('.') + 1);
            fromLabel.setText(String.format("Change from %1$s", oldTypeBase));

            Label typeLabel = new Label(composite, SWT.NONE);
            typeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            typeLabel.setText("New Layout Type:");

            mTypeCombo = new Combo(composite, SWT.READ_ONLY);
            mTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            SelectionAdapter selectionListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    validatePage();
                    // Hierarchy flattening only works for relative layout (and any future
                    // layouts that can also support arbitrary layouts).
                    String text = mTypeCombo.getText();
                    mFlatten.setVisible(text.equals(RELATIVE_LAYOUT) || text.equals(GRID_LAYOUT));
                }
            };
            mTypeCombo.addSelectionListener(selectionListener);
            mTypeCombo.addSelectionListener(mSelectionValidateListener);

            mFlatten = new Button(composite, SWT.CHECK);
            mFlatten.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                    false, false, 2, 1));
            mFlatten.setText("Flatten hierarchy");
            mFlatten.addSelectionListener(selectionListener);
            // Should flattening be selected by default?
            mFlatten.setSelection(true);
            mFlatten.addSelectionListener(mSelectionValidateListener);

            // We don't exclude RelativeLayout even if the current layout is RelativeLayout,
            // in case you are trying to flatten the hierarchy for a hierarchy that has a
            // RelativeLayout at the root.
            Set<String> exclude = new HashSet<String>();
            exclude.add(VIEW_INCLUDE);
            exclude.add(VIEW_MERGE);
            exclude.add(VIEW_FRAGMENT);
            boolean oldIsRelativeLayout = mOldType.equals(FQCN_RELATIVE_LAYOUT);
            boolean oldIsGridLayout = mOldType.equals(FQCN_GRID_LAYOUT);
            if (oldIsRelativeLayout || oldIsGridLayout) {
                exclude.add(mOldType);
            }
            mClassNames = WrapInWizard.addLayouts(mProject, mOldType, mTypeCombo, exclude, false);

            boolean gridLayoutAvailable = false;
            for (int i = 0; i < mTypeCombo.getItemCount(); i++) {
                if (mTypeCombo.getItem(i).equals(GRID_LAYOUT)) {
                    gridLayoutAvailable = true;
                    break;
                }
            }

            mTypeCombo.select(0);
            // The default should be GridLayout (if available) and if not RelativeLayout,
            // if available (and not the old Type)
            if (gridLayoutAvailable && !oldIsGridLayout) {
                for (int i = 0; i < mTypeCombo.getItemCount(); i++) {
                    if (mTypeCombo.getItem(i).equals(GRID_LAYOUT)) {
                        mTypeCombo.select(i);
                        break;
                    }
                }
            } else if (!oldIsRelativeLayout) {
                for (int i = 0; i < mTypeCombo.getItemCount(); i++) {
                    if (mTypeCombo.getItem(i).equals(RELATIVE_LAYOUT)) {
                        mTypeCombo.select(i);
                        break;
                    }
                }
            }
            mFlatten.setVisible(mTypeCombo.getText().equals(RELATIVE_LAYOUT)
                    || mTypeCombo.getText().equals(GRID_LAYOUT));

            setControl(composite);
            validatePage();
        }

        @Override
        protected boolean validatePage() {
            boolean ok = true;

            int selectionIndex = mTypeCombo.getSelectionIndex();
            String type = selectionIndex != -1 ? mClassNames.get(selectionIndex).getFirst() : null;
            if (type == null) {
                setErrorMessage("Select a layout type");
                ok = false; // The user has chosen a separator
            } else {
                setErrorMessage(null);

                // Record state
                ChangeLayoutRefactoring refactoring =
                    (ChangeLayoutRefactoring) getRefactoring();
                refactoring.setType(type);
                refactoring.setFlatten(mFlatten.getSelection());

                ViewElementDescriptor descriptor = mClassNames.get(selectionIndex).getSecond();
                if (descriptor instanceof PaletteMetadataDescriptor) {
                    PaletteMetadataDescriptor paletteDescriptor =
                        (PaletteMetadataDescriptor) descriptor;
                    String initializedAttributes = paletteDescriptor.getInitializedAttributes();
                    if (initializedAttributes != null && initializedAttributes.length() > 0) {
                        refactoring.setInitializedAttributes(initializedAttributes);
                    }
                } else {
                    refactoring.setInitializedAttributes(null);
                }
            }

            setPageComplete(ok);
            return ok;
        }
    }
}
