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

import static com.android.SdkConstants.REQUEST_FOCUS;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CustomViewFinder;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.List;

class ChangeViewWizard extends VisualRefactoringWizard {
    private static final String SEPARATOR_LABEL =
        "----------------------------------------"; //$NON-NLS-1$

    public ChangeViewWizard(ChangeViewRefactoring ref, LayoutEditorDelegate editor) {
        super(ref, editor);
        setDefaultPageTitle("Change Widget Type");
    }

    @Override
    protected void addUserInputPages() {
        ChangeViewRefactoring ref = (ChangeViewRefactoring) getRefactoring();
        List<String> oldTypes = ref.getOldTypes();
        String oldType = null;
        for (String type : oldTypes) {
            if (oldType == null) {
                oldType = type;
            } else if (!oldType.equals(type)) {
                // If the types differ, don't offer related categories
                oldType = null;
                break;
            }
        }
        addPage(new InputPage(mDelegate.getEditor().getProject(), oldType));
    }

    /** Wizard page which inputs parameters for the {@link ChangeViewRefactoring} operation */
    private static class InputPage extends VisualRefactoringInputPage {
        private final IProject mProject;
        private Combo mTypeCombo;
        private final String mOldType;
        private List<String> mClassNames;

        public InputPage(IProject project, String oldType) {
            super("ChangeViewInputPage");  //$NON-NLS-1$
            mProject = project;
            mOldType = oldType;
        }

        @Override
        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            Label typeLabel = new Label(composite, SWT.NONE);
            typeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            typeLabel.setText("New Widget Type:");

            mTypeCombo = new Combo(composite, SWT.READ_ONLY);
            mTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            mTypeCombo.addSelectionListener(mSelectionValidateListener);

            mClassNames = getWidgetTypes(mOldType, mTypeCombo);
            mTypeCombo.select(0);

            setControl(composite);
            validatePage();

            mTypeCombo.setFocus();
        }

        private List<String> getWidgetTypes(String oldType, Combo combo) {
            List<String> classNames = new ArrayList<String>();

            // Populate type combo
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mProject);
                if (target != null) {
                    // Try to pick "related" widgets to the one you have selected.
                    // For example, for an AnalogClock, display DigitalClock first.
                    // For a Text, offer EditText, AutoComplete, etc.
                    if (oldType != null) {
                        ViewMetadataRepository repository = ViewMetadataRepository.get();
                        List<String> relatedTo = repository.getRelatedTo(oldType);
                        if (relatedTo.size() > 0) {
                            for (String className : relatedTo) {
                                String base = className.substring(className.lastIndexOf('.') + 1);
                                combo.add(base);
                                classNames.add(className);
                            }
                            combo.add(SEPARATOR_LABEL);
                            classNames.add(null);
                        }
                    }

                    Pair<List<String>,List<String>> result =
                        CustomViewFinder.findViews(mProject, false);
                    List<String> customViews = result.getFirst();
                    List<String> thirdPartyViews = result.getSecond();
                    if (customViews.size() > 0) {
                        for (String view : customViews) {
                            combo.add(view);
                            classNames.add(view);
                        }
                        combo.add(SEPARATOR_LABEL);
                        classNames.add(null);
                    }

                    if (thirdPartyViews.size() > 0) {
                        for (String view : thirdPartyViews) {
                            combo.add(view);
                            classNames.add(view);
                        }
                        combo.add(SEPARATOR_LABEL);
                        classNames.add(null);
                    }

                    AndroidTargetData targetData = currentSdk.getTargetData(target);
                    if (targetData != null) {
                        // Now add ALL known layout descriptors in case the user has
                        // a special case
                        List<ViewElementDescriptor> descriptors =
                            targetData.getLayoutDescriptors().getViewDescriptors();
                        for (ViewElementDescriptor d : descriptors) {
                            String className = d.getFullClassName();
                            if (className.equals(VIEW_INCLUDE)
                                    || className.equals(VIEW_FRAGMENT)
                                    || className.equals(REQUEST_FOCUS)) {
                                continue;
                            }
                            combo.add(d.getUiName());
                            classNames.add(className);

                        }
                    }
                }
            } else {
                combo.add("SDK not initialized");
                classNames.add(null);
            }

            return classNames;
        }

        @Override
        protected boolean validatePage() {
            boolean ok = true;
            int selectionIndex = mTypeCombo.getSelectionIndex();
            String type = selectionIndex != -1 ? mClassNames.get(selectionIndex) : null;
            if (type == null) {
                setErrorMessage("Select a widget type to convert to");
                ok = false; // The user has chosen a separator
            } else {
                setErrorMessage(null);
            }

            // Record state
            ChangeViewRefactoring refactoring =
                (ChangeViewRefactoring) getRefactoring();
            refactoring.setType(type);

            setPageComplete(ok);
            return ok;
        }
    }
}
