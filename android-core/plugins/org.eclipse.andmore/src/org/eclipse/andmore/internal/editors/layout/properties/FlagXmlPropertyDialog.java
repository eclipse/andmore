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
package org.eclipse.andmore.internal.editors.layout.properties;

import com.android.annotations.NonNull;
import com.google.common.base.Splitter;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.core.utils.ui.dialogs.ResizableDialog;

import java.util.ArrayList;
import java.util.List;

class FlagXmlPropertyDialog extends ResizableDialog
implements IStructuredContentProvider, ICheckStateListener, SelectionListener, KeyListener {
    private final String mTitle;
    private final XmlProperty mProperty;
    private final String[] mFlags;
    private final boolean mIsRadio;

    private Table mTable;
    private CheckboxTableViewer mViewer;

    FlagXmlPropertyDialog(
            @NonNull Shell parentShell,
            @NonNull String title,
            boolean isRadio,
            @NonNull String[] flags,
            @NonNull XmlProperty property) {
        super(parentShell, AdtPlugin.getDefault());
        mTitle = title;
        mIsRadio = isRadio;
        mFlags = flags;
        mProperty = property;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(mTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        mViewer = CheckboxTableViewer.newCheckList(container,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
        mTable = mViewer.getTable();
        mTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Composite workaround = PropertyFactory.addWorkaround(container);
        if (workaround != null) {
            workaround.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        }

        mViewer.setContentProvider(this);
        mViewer.setInput(mFlags);

        String current = mProperty.getStringValue();
        if (current != null) {
            Object[] checked = null;
            if (mIsRadio) {
                checked = new String[] { current };
            } else {
                List<String> flags = new ArrayList<String>();
                for (String s : Splitter.on('|').omitEmptyStrings().trimResults().split(current)) {
                    flags.add(s);
                }
                checked = flags.toArray(new String[flags.size()]);
            }
            mViewer.setCheckedElements(checked);
        }
        if (mFlags.length > 0) {
            mTable.setSelection(0);
        }

        if (mIsRadio) {
            // Enforce single-item selection
            mViewer.addCheckStateListener(this);
        }
        mTable.addSelectionListener(this);
        mTable.addKeyListener(this);

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getDefaultSize() {
        return new Point(450, 400);
    }

    @Override
    protected void okPressed() {
        // Apply the value
        ExecutionUtils.runLog(new RunnableEx() {
            @Override
            public void run() throws Exception {
                StringBuilder sb = new StringBuilder(30);
                for (Object o : mViewer.getCheckedElements()) {
                    if (sb.length() > 0) {
                        sb.append('|');
                    }
                    sb.append((String) o);
                }
                String value = sb.length() > 0 ? sb.toString() : null;
                mProperty.setValue(value);
            }
        });

        // close dialog
        super.okPressed();
    }

    // ---- Implements IStructuredContentProvider ----

    @Override
    public Object[] getElements(Object inputElement) {
        return (Object []) inputElement;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    // ---- Implements ICheckStateListener ----

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        // Try to disable other elements that conflict with this
        boolean isChecked = event.getChecked();
        if (isChecked) {
            Object selected = event.getElement();
            for (Object other : mViewer.getCheckedElements()) {
                if (other != selected) {
                    mViewer.setChecked(other, false);
                }
            }
        } else {

        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        if (e.item instanceof TableItem) {
            TableItem item = (TableItem) e.item;
            item.setChecked(!item.getChecked());
        }
    }

    // ---- Implements KeyListener ----

    @Override
    public void keyPressed(KeyEvent e) {
        // Let space toggle checked state
        if (e.keyCode == ' ' /* SWT.SPACE requires Eclipse 3.7 */) {
            if (mTable.getSelectionCount() == 1) {
                TableItem item = mTable.getSelection()[0];
                item.setChecked(!item.getChecked());
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
