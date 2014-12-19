/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.monitor;

import com.android.ide.eclipse.monitor.SdkToolsLocator.SdkInstallStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;

public class SdkLocationChooserDialog extends Dialog {
    private static final String TITLE = "Android Device Monitor";
    private static final String DEFAULT_MESSAGE = "Provide the path to the Android SDK";

    private Label mStatusLabel;
    private Text mTextBox;
    private String mPath;

    public SdkLocationChooserDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(TITLE);

        Composite c = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
        c.setLayout(new GridLayout(2, false));
        c.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label l = new Label(c, SWT.NONE);
        l.setText(DEFAULT_MESSAGE);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

        mTextBox = new Text(c, SWT.BORDER);
        mTextBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridDataFactory.fillDefaults()
                       .hint(SwtUtils.getFontWidth(mTextBox) * 80, SWT.DEFAULT)
                       .applyTo(mTextBox);
        mTextBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validateInstall();
            }
        });

        Button browse = new Button(c, SWT.PUSH);
        browse.setText("Browse");
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.OPEN);
                dlg.setText("Android SDK location");
                String dir = dlg.open();
                if (dir != null) {
                    mTextBox.setText(dir);
                    validateInstall();
                }
            }
        });

        mStatusLabel = new Label(c, SWT.WRAP);
        mStatusLabel.setText("");
        mStatusLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
        GridDataFactory.fillDefaults().span(2, 1).applyTo(mStatusLabel);

        return super.createDialogArea(parent);
    }

    private void validateInstall() {
        SdkToolsLocator locator = new SdkToolsLocator(new File(mTextBox.getText()));
        SdkInstallStatus status = locator.isValidInstallation();
        if (status.isValid()) {
            mStatusLabel.setText("");
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        } else {
            mStatusLabel.setText(status.getErrorMessage());
            mStatusLabel.pack();
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    @Override
    public boolean close() {
        mPath = mTextBox.getText();
        return super.close();
    }

    public String getPath() {
        return mPath;
    }
}
