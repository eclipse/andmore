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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class UpdateToolsPage extends WizardPage implements SelectionListener {
    private Button mInstallButton;
    UpdateToolsPage() {
        super("update");
        setTitle("Update Tools");
        validatePage();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(1, false));

        Label label = new Label(container, SWT.WRAP);
        GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1);
        layoutData.widthHint = NewTemplatePage.WIZARD_PAGE_WIDTH - 50;
        label.setLayoutData(layoutData);
        label.setText(
                "Your tools installation appears to be out of date (or not yet installed).\n" +
                "\n" +
                "This wizard depends on templates distributed with the Android SDK Tools.\n" +
                "\n" +
                "Please update the tools first (via Window > Android SDK Manager, or by " +
                "using the \"android\" command in a terminal window). Note that on Windows " +
                "you may need to restart the IDE, since there are some known problems where " +
                "Windows locks the files held open by the running IDE, so the updater is " +
                "unable to delete them in order to upgrade them.");

        mInstallButton = new Button(container, SWT.NONE);
        mInstallButton.setText("Check Again");
        mInstallButton.addSelectionListener(this);
    }

    @Override
    public boolean isPageComplete() {
        return isUpToDate();
    }

    static boolean isUpToDate() {
        return TemplateManager.getTemplateRootFolder() != null;
    }

    private void validatePage() {
        boolean ok = isUpToDate();
        setPageComplete(ok);
        if (ok) {
            setErrorMessage(null);
            setMessage(null);
        } else {
            setErrorMessage("The tools need to be updated via the SDK Manager");
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == mInstallButton) {
            validatePage();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
