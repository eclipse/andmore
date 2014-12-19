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
package org.eclipse.andmore.internal.wizards.templates;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/** For template developers: Test local template directory */
public class TemplateTestPage extends WizardPage
        implements SelectionListener, ModifyListener {
    private Text mLocation;
    private Button mButton;
    private static String sLocation; // Persist between repeated invocations
    private Button mProjectToggle;
    private File mTemplate;

    TemplateTestPage() {
        super("testWizardPage"); //$NON-NLS-1$
        setTitle("Wizard Tester");
        setDescription("Test a new template");
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        Label label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        label.setText("Template Location:");

        mLocation = new Text(container, SWT.BORDER);
        GridData gd_mLocation = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_mLocation.widthHint = 400;
        mLocation.setLayoutData(gd_mLocation);
        if (sLocation != null) {
            mLocation.setText(sLocation);
        }
        mLocation.addModifyListener(this);

        mButton = new Button(container, SWT.FLAT);
        mButton.setText("...");

        mProjectToggle = new Button(container, SWT.CHECK);
        mProjectToggle.setEnabled(false);
        mProjectToggle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mProjectToggle.setText("Full project template");
        new Label(container, SWT.NONE);
        mButton.addSelectionListener(this);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        validatePage();
    }

    private boolean validatePage() {
        String error = null;

        String path = mLocation.getText().trim();
        if (path == null || path.length() == 0) {
            error = "Select a template directory";
            mTemplate = null;
        } else {
            mTemplate = new File(path);
            if (!mTemplate.exists()) {
                error = String.format("%1$s does not exist", path);
            } else {
                // Preserve across wizard sessions
                sLocation = path;

                if (mTemplate.isDirectory()) {
                    if (!new File(mTemplate, TemplateHandler.TEMPLATE_XML).exists()) {
                        error = String.format("Not a template: missing template.xml file in %1$s ",
                                path);
                    }
                } else {
                    if (mTemplate.getName().equals(TemplateHandler.TEMPLATE_XML)) {
                        mTemplate = mTemplate.getParentFile();
                    } else {
                        error = String.format("Select a directory containing a template");
                    }
                }
            }
        }

        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }

        return error == null;
    }

    @Override
    public void modifyText(ModifyEvent e) {
        validatePage();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == mButton) {
            DirectoryDialog dialog = new DirectoryDialog(mButton.getShell(), SWT.OPEN);
            String path = mLocation.getText().trim();
            if (path.length() > 0) {
                dialog.setFilterPath(path);
            }
            String file = dialog.open();
            if (file != null) {
                mLocation.setText(file);
            }
        }

        validatePage();
    }

    File getLocation() {
        return mTemplate;
    }

    boolean isProjectTemplate() {
        return mProjectToggle.getSelection();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
