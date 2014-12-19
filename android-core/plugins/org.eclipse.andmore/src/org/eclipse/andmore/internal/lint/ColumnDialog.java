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
package org.eclipse.andmore.internal.lint;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * Dialog for editing visible columns in the {@link LintList}
 */
class ColumnDialog extends SelectionStatusDialog implements Listener, IStructuredContentProvider {
    private LintColumn[] mColumns;
    private LintColumn[] mSelectedColumns;
    private CheckboxTableViewer mViewer;

        public ColumnDialog(Shell parent, LintColumn[] fields, LintColumn[] selected) {
            super(parent);
            mColumns = fields;
            mSelectedColumns = selected;
            setTitle("Select Visible Columns");
            setHelpAvailable(false);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
            // Wide enough to accommodate the error label
            gridData.widthHint = 500;
            container.setLayoutData(gridData);

            Label lblSelectVisibleColumns = new Label(container, SWT.NONE);
            lblSelectVisibleColumns.setText("Select visible columns:");

            mViewer = CheckboxTableViewer.newCheckList(container,
                    SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
            Table table = mViewer.getTable();
            table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            mViewer.setContentProvider(this);

            mViewer.setInput(mColumns);
            mViewer.setCheckedElements(mSelectedColumns);

            validate();

            return container;
        }

        @Override
        protected void computeResult() {
            Object[] checked = mViewer.getCheckedElements();
            mSelectedColumns = new LintColumn[checked.length];
            for (int i = 0, n = checked.length; i < n; i++) {
                mSelectedColumns[i] = (LintColumn) checked[i];
            }
        }

        public LintColumn[] getSelectedColumns() {
            return mSelectedColumns;
        }

        @Override
        public void handleEvent(Event event) {
            validate();
        }

        private void validate() {
            IStatus status;
            computeResult();

            if (mViewer.getCheckedElements().length <= 1) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        "Must selected at least one column");
            } else {
                status = new Status(IStatus.OK, AdtPlugin.PLUGIN_ID, null);
            }
            updateStatus(status);
        }

        // ---- Implements IStructuredContentProvider ----

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return mColumns;
        }
    }