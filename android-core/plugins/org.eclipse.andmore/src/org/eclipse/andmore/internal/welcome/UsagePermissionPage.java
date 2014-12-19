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

package org.eclipse.andmore.internal.welcome;

import com.android.sdkstats.SdkStatsPermissionDialog;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import java.net.URL;

/** Page which displays the permission dialog for collecting usage statistics */
public class UsagePermissionPage extends WizardPage implements SelectionListener {
    private Link mLink;
    private Button mYesRadio;
    private Button mNoRadio;

    /**
     * Create the wizard.
     */
    public UsagePermissionPage() {
        super("usageData");
        setTitle("Contribute Usage Statistics?");
        setDescription(SdkStatsPermissionDialog.NOTICE_TEXT);
    }

    /**
     * Create contents of the wizard.
     *
     * @param parent parent to create page into
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(1, false));

        Label label = new Label(container, SWT.WRAP);
        GridData gd_lblByChoosingTo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_lblByChoosingTo.widthHint = 580;
        label.setLayoutData(gd_lblByChoosingTo);
        label.setText(SdkStatsPermissionDialog.BODY_TEXT);

        Label blankLine = new Label(container, SWT.NONE);

        Label questionLabel = new Label(container, SWT.NONE);
        questionLabel.setText("Send usage statistics to Google?");

        mYesRadio = new Button(container, SWT.RADIO);
        mYesRadio.setText("Yes");
        mYesRadio.addSelectionListener(this);

        mNoRadio = new Button(container, SWT.RADIO);
        mNoRadio.setText("No");
        mNoRadio.addSelectionListener(this);

        Label laterLabel = new Label(container, SWT.WRAP);
        GridData gdLaterLabel = new GridData(SWT.FILL, SWT.BOTTOM, false, true, 1, 1);
        gdLaterLabel.widthHint = 580;
        laterLabel.setLayoutData(gdLaterLabel);
        laterLabel.setText("If you later decide to change this setting, you can do so in the " +
               "options panel under Android > Usage Stats");

        mLink = new Link(container, SWT.NONE);
        mLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mLink.setText(SdkStatsPermissionDialog.PRIVACY_POLICY_LINK_TEXT);
        mLink.addSelectionListener(this);

        validatePage();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        mYesRadio.setFocus();
    }

    boolean isUsageCollectionApproved() {
        return mYesRadio.getSelection();
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.getSource() == mLink) {
            try {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWebBrowser browser = workbench.getBrowserSupport().getExternalBrowser();
                browser.openURL(new URL(event.text));
            } catch (Exception e) {
                String message = String.format("Could not open browser. Vist\n%1$s\ninstead.",
                        event.text);
                MessageDialog.openError(getWizard().getContainer().getShell(),
                        "Browser Error", message);
            }
        } else {
            // Radio buttons selected
            validatePage();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    private void validatePage() {
        String error = null;

        if (!mYesRadio.getSelection() && !mNoRadio.getSelection()) {
            error = "Select Yes or No";
        }

        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }
}
