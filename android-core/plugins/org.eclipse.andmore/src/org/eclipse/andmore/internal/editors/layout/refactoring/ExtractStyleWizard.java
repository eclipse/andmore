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

import static org.eclipse.jface.viewers.StyledString.DECORATIONS_STYLER;
import static org.eclipse.jface.viewers.StyledString.QUALIFIER_STYLER;

import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ExtractStyleWizard extends VisualRefactoringWizard {
    public ExtractStyleWizard(ExtractStyleRefactoring ref, LayoutEditorDelegate editor) {
        super(ref, editor);
        setDefaultPageTitle(ref.getName());
    }

    @Override
    protected void addUserInputPages() {
        String initialName = "styleName";
        addPage(new InputPage(mDelegate.getEditor().getProject(), initialName));
    }

    /**
     * Wizard page which inputs parameters for the {@link ExtractStyleRefactoring}
     * operation
     */
    private static class InputPage extends VisualRefactoringInputPage {
        private final IProject mProject;
        private final String mSuggestedName;
        private Text mNameText;
        private Table mTable;
        private Button mRemoveExtracted;
        private Button mSetStyle;
        private Button mRemoveAll;
        private Button mExtend;
        private CheckboxTableViewer mCheckedView;

        private String mParentStyle;
        private Set<Attr> mInSelection;
        private List<Attr> mAllAttributes;
        private int mElementCount;
        private Map<Attr, Integer> mFrequencyCount;
        private Set<Attr> mShown;
        private List<Attr> mInitialChecked;
        private List<Attr> mAllChecked;
        private List<Map.Entry<String, List<Attr>>> mRoot;
        private Map<String, List<Attr>> mAvailableAttributes;

        public InputPage(IProject project, String suggestedName) {
            super("ExtractStyleInputPage");
            mProject = project;
            mSuggestedName = suggestedName;
        }

        @Override
        public void createControl(Composite parent) {
            initialize();

            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            Label nameLabel = new Label(composite, SWT.NONE);
            nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            nameLabel.setText("Style Name:");

            mNameText = new Text(composite, SWT.BORDER);
            mNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            mNameText.addModifyListener(mModifyValidateListener);

            mRemoveExtracted = new Button(composite, SWT.CHECK);
            mRemoveExtracted.setSelection(true);
            mRemoveExtracted.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
            mRemoveExtracted.setText("Remove extracted attributes");
            mRemoveExtracted.addSelectionListener(mSelectionValidateListener);

            mRemoveAll = new Button(composite, SWT.CHECK);
            mRemoveAll.setSelection(false);
            mRemoveAll.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
            mRemoveAll.setText("Remove all extracted attributes regardless of value");
            mRemoveAll.addSelectionListener(mSelectionValidateListener);

            boolean defaultSetStyle = false;
            if (mParentStyle != null) {
                mExtend = new Button(composite, SWT.CHECK);
                mExtend.setSelection(true);
                mExtend.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
                mExtend.setText(String.format("Extend %1$s", mParentStyle));
                mExtend.addSelectionListener(mSelectionValidateListener);
                defaultSetStyle = true;
            }

            mSetStyle = new Button(composite, SWT.CHECK);
            mSetStyle.setSelection(defaultSetStyle);
            mSetStyle.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
            mSetStyle.setText("Set style attribute on extracted elements");
            mSetStyle.addSelectionListener(mSelectionValidateListener);

            new Label(composite, SWT.NONE);
            new Label(composite, SWT.NONE);

            Label tableLabel = new Label(composite, SWT.NONE);
            tableLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
            tableLabel.setText("Choose style attributes to extract:");

            mCheckedView = CheckboxTableViewer.newCheckList(composite, SWT.BORDER
                    | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
            mTable = mCheckedView.getTable();
            mTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
            ((GridData) mTable.getLayoutData()).heightHint = 200;

            mCheckedView.setContentProvider(new ArgumentContentProvider());
            mCheckedView.setLabelProvider(new ArgumentLabelProvider());
            mCheckedView.setInput(mRoot);
            final Object[] initialSelection = mInitialChecked.toArray();
            mCheckedView.setCheckedElements(initialSelection);

            mCheckedView.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    // Try to disable other elements that conflict with this
                    boolean isChecked = event.getChecked();
                    if (isChecked) {
                        Attr attribute = (Attr) event.getElement();
                        List<Attr> list = mAvailableAttributes.get(attribute.getLocalName());
                        for (Attr other : list) {
                            if (other != attribute && mShown.contains(other)) {
                                mCheckedView.setChecked(other, false);
                            }
                        }
                    }

                    validatePage();
                }
            });

            // Select All / Deselect All
            Composite buttonForm = new Composite(composite, SWT.NONE);
            buttonForm.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            rowLayout.marginTop = 0;
            rowLayout.marginLeft = 0;
            buttonForm.setLayout(rowLayout);
            Button checkAllButton = new Button(buttonForm, SWT.FLAT);
            checkAllButton.setText("Select All");
            checkAllButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // Select "all" (but not conflicting settings)
                    mCheckedView.setCheckedElements(mAllChecked.toArray());
                    validatePage();
                }
            });
            Button uncheckAllButton = new Button(buttonForm, SWT.FLAT);
            uncheckAllButton.setText("Deselect All");
            uncheckAllButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mCheckedView.setAllChecked(false);
                    validatePage();
                }
            });

            // Initialize UI:
            if (mSuggestedName != null) {
                mNameText.setText(mSuggestedName);
            }

            setControl(composite);
            validatePage();
        }

        private void initialize() {
            ExtractStyleRefactoring ref = (ExtractStyleRefactoring) getRefactoring();

            mElementCount = ref.getElements().size();

            mParentStyle = ref.getParentStyle();

            // Set up data structures needed by the wizard -- to compute the actual
            // attributes to list in the wizard (there could be multiple attributes
            // of the same name (on different elements) and we only want to show one, etc.)

            Pair<Map<String, List<Attr>>, Set<Attr>> result = ref.getAvailableAttributes();
            // List of all available attributes on the selected elements
            mAvailableAttributes = result.getFirst();
            // Set of attributes that overlap the text selection, or all attributes if
            // wizard is invoked from GUI context
            mInSelection = result.getSecond();

            // The root data structure, which we set as the table root. The content provider
            // will produce children from it. This is the entry set of a map from
            // attribute name to list of attribute nodes for that attribute name.
            mRoot = new ArrayList<Map.Entry<String, List<Attr>>>(
                mAvailableAttributes.entrySet());

            // Sort the items by attribute name -- the attribute name is the key
            // in the entry set above.
            Collections.sort(mRoot, new Comparator<Map.Entry<String, List<Attr>>>() {
                @Override
                public int compare(Map.Entry<String, List<Attr>> e1,
                        Map.Entry<String, List<Attr>> e2) {
                    return e1.getKey().compareTo(e2.getKey());
                }
            });

            // Set of attributes actually included in the list shown to the user.
            // (There could be many additional "aliasing" nodes on other elements
            // with the same name.) Note however that we DO show multiple attribute
            // occurrences of the same attribute name: precisely one for each unique -value-
            // of that attribute.
            mShown = new HashSet<Attr>();

            // The list of initially checked attributes.
            mInitialChecked = new ArrayList<Attr>();

            // The list of attributes to be checked if "Select All" is chosen (this is not
            // the same as *all* attributes, since we need to exclude any conflicts)
            mAllChecked = new ArrayList<Attr>();

            // All attributes.
            mAllAttributes = new ArrayList<Attr>();

            // Frequency count, from attribute to integer. Attributes that do not
            // appear in the list have frequency 1, not 0.
            mFrequencyCount = new HashMap<Attr, Integer>();

            for (Map.Entry<String, List<Attr>> entry : mRoot) {
                // Iterate over all attributes of the same name, and sort them
                // by value. This will make it easy to list each -unique- value in the
                // wizard.
                List<Attr> attrList = entry.getValue();
                Collections.sort(attrList, new Comparator<Attr>() {
                    @Override
                    public int compare(Attr a1, Attr a2) {
                        return a1.getValue().compareTo(a2.getValue());
                    }
                });

                // We need to compute a couple of things: the frequency for all identical
                // values (and stash them in the frequency map), and record the first
                // attribute with a particular value into the list of attributes to
                // be shown.
                Attr prevAttr = null;
                String prev = null;
                List<Attr> uniqueValueAttrs = new ArrayList<Attr>();
                for (Attr attr : attrList) {
                    String value = attr.getValue();
                    if (value.equals(prev)) {
                        Integer count = mFrequencyCount.get(prevAttr);
                        if (count == null) {
                            count = Integer.valueOf(2);
                        } else {
                            count = Integer.valueOf(count.intValue() + 1);
                        }
                        mFrequencyCount.put(prevAttr, count);
                    } else {
                        uniqueValueAttrs.add(attr);
                        prev = value;
                        prevAttr = attr;
                    }
                }

                // Sort the values by frequency (and for equal frequencies, alphabetically
                // by value)
                Collections.sort(uniqueValueAttrs, new Comparator<Attr>() {
                    @Override
                    public int compare(Attr a1, Attr a2) {
                        Integer f1 = mFrequencyCount.get(a1);
                        Integer f2 = mFrequencyCount.get(a2);
                        if (f1 == null) {
                            f1 = Integer.valueOf(1);
                        }
                        if (f2 == null) {
                            f2 = Integer.valueOf(1);
                        }
                        int delta = f2.intValue() - f1.intValue();
                        if (delta != 0) {
                            return delta;
                        } else {
                            return a1.getValue().compareTo(a2.getValue());
                        }
                    }
                });

                // Add the items in order, and select those attributes that overlap
                // the selection
                mAllAttributes.addAll(uniqueValueAttrs);
                mShown.addAll(uniqueValueAttrs);
                Attr first = uniqueValueAttrs.get(0);
                mAllChecked.add(first);
                if (mInSelection.contains(first)) {
                    mInitialChecked.add(first);
                }
            }
        }

        @Override
        protected boolean validatePage() {
            boolean ok = true;

            String text = mNameText.getText().trim();

            if (text.length() == 0) {
                setErrorMessage("Provide a name for the new style");
                ok = false;
            } else {
                ResourceNameValidator validator = ResourceNameValidator.create(false, mProject,
                        ResourceType.STYLE);
                String message = validator.isValid(text);
                if (message != null) {
                    setErrorMessage(message);
                    ok = false;
                }
            }

            Object[] checkedElements = mCheckedView.getCheckedElements();
            if (checkedElements.length == 0) {
                setErrorMessage("Choose at least one attribute to extract");
                ok = false;
            }

            if (ok) {
                setErrorMessage(null);

                // Record state
                ExtractStyleRefactoring refactoring = (ExtractStyleRefactoring) getRefactoring();
                refactoring.setStyleName(text);
                refactoring.setRemoveExtracted(mRemoveExtracted.getSelection());
                refactoring.setRemoveAll(mRemoveAll.getSelection());
                refactoring.setApplyStyle(mSetStyle.getSelection());
                if (mExtend != null && mExtend.getSelection()) {
                    refactoring.setParent(mParentStyle);
                }
                List<Attr> attributes = new ArrayList<Attr>();
                for (Object o : checkedElements) {
                    attributes.add((Attr) o);
                }
                refactoring.setChosenAttributes(attributes);
            }

            setPageComplete(ok);
            return ok;
        }

        private class ArgumentLabelProvider extends StyledCellLabelProvider {
            public ArgumentLabelProvider() {
            }

            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                Attr attribute = (Attr) element;

                StyledString styledString = new StyledString();
                styledString.append(attribute.getLocalName());
                styledString.append(" = ", QUALIFIER_STYLER);
                styledString.append(attribute.getValue());

                if (mElementCount > 1) {
                    Integer f = mFrequencyCount.get(attribute);
                    String s = String.format(" (in %d/%d elements)",
                            f != null ? f.intValue(): 1, mElementCount);
                    styledString.append(s, DECORATIONS_STYLER);
                }
                cell.setText(styledString.toString());
                cell.setStyleRanges(styledString.getStyleRanges());
                super.update(cell);
            }
        }

        private class ArgumentContentProvider implements IStructuredContentProvider {
            public ArgumentContentProvider() {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement == mRoot) {
                    return mAllAttributes.toArray();
                }

                return new Object[0];
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
        }
    }
}
