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

import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.actions.AddSupportJarAction;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
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

import java.io.File;
import java.net.URL;
import java.util.List;

class InstallDependencyPage extends WizardPage implements SelectionListener {
    /**
     * The compatibility library. This is the only library the templates
     * currently support. The appearance of any other dependency in this
     * template will be flagged as a validation error (and the user encouraged
     * to upgrade to a newer ADT
     */
    static final String SUPPORT_LIBRARY_NAME = "android-support-v4"; //$NON-NLS-1$

    /** URL containing more info */
    private static final String URL =
            "http://developer.android.com/tools/extras/support-library.html"; //$NON-NLS-1$

    private Button mCheckButton;
    private Button mInstallButton;
    private Link mLink;
    private TemplateMetadata mTemplate;

    InstallDependencyPage() {
        super("dependency"); //$NON-NLS-1$
        setTitle("Install Dependencies");
    }

    void setTemplate(TemplateMetadata template) {
        if (template != mTemplate) {
            mTemplate = template;
            if (getControl() != null) {
                validatePage();
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            updateVersionLabels();
            validatePage();
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(2, false));
        // Remaining contents are created lazily, since this page is always added to
        // the page list, but typically not shown

        Label dependLabel = new Label(container, SWT.WRAP);
        GridData gd_dependLabel = new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1);
        gd_dependLabel.widthHint = NewTemplatePage.WIZARD_PAGE_WIDTH - 50;
        dependLabel.setLayoutData(gd_dependLabel);
        dependLabel.setText("This template depends on the Android Support library, which is " +
                "either not installed, or the template depends on a more recent version than " +
                "the one you have installed.");

        mLink = new Link(container, SWT.NONE);
        mLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
        mLink.setText("<a href=\"" + URL + "\">" + URL + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        mLink.addSelectionListener(this);

        Label lblNewLabel_1 = new Label(container, SWT.NONE);
        lblNewLabel_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        requiredLabel = new Label(container, SWT.NONE);
        requiredLabel.setText("Required version:");

        mRequiredVersion = new Label(container, SWT.NONE);
        mRequiredVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        installedLabel = new Label(container, SWT.NONE);
        installedLabel.setText("Installed version:");

        mInstalledVersion = new Label(container, SWT.NONE);
        mInstalledVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        Label descLabel = new Label(container, SWT.WRAP);
        GridData gd_descLabel = new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1);
        gd_descLabel.widthHint = 550;
        descLabel.setLayoutData(gd_descLabel);
        descLabel.setText(
                "You can install or upgrade it by clicking the Install button below, or " +
                "alternatively, you can install it outside of Eclipse with the SDK Manager, " +
                "then click on \"Check Again\" to proceed.");

        mInstallButton = new Button(container, SWT.NONE);
        mInstallButton.setText("Install/Upgrade");
        mInstallButton.addSelectionListener(this);

        mCheckButton = new Button(container, SWT.NONE);
        mCheckButton.setText("Check Again");
        mCheckButton.addSelectionListener(this);

        mInstallButton.setFocus();
    }

    private void showNextPage() {
        validatePage();
        if (isPageComplete()) {
            // Finish button will be enabled now
            mInstallButton.setEnabled(false);
            mCheckButton.setEnabled(false);

            IWizard wizard = getWizard();
            IWizardPage next = wizard.getNextPage(this);
            if (next != null) {
                wizard.getContainer().showPage(next);
            }
        }
    }

    @Override
    public boolean isPageComplete() {
        if (mTemplate == null) {
            return true;
        }

        return super.isPageComplete() && isInstalled();
    }

    private boolean isInstalled() {
        return isInstalled(mTemplate.getDependencies());
    }

    static String sCachedName;
    static int sCachedVersion;
    private Label requiredLabel;
    private Label installedLabel;
    private Label mRequiredVersion;
    private Label mInstalledVersion;

    public static boolean isInstalled(List<Pair<String, Integer>> dependencies) {
        for (Pair<String, Integer> dependency : dependencies) {
            String name = dependency.getFirst();
            int required = dependency.getSecond();

            int installed = -1;
            if (SUPPORT_LIBRARY_NAME.equals(name)) {
                installed = getInstalledSupportLibVersion();
            }

            if (installed == -1) {
                return false;
            }
            if (required > installed) {
                return false;
            }
        }

        return true;
    }

    private static int getInstalledSupportLibVersion() {
        if (SUPPORT_LIBRARY_NAME.equals(sCachedName)) {
            return sCachedVersion;
        } else {
            int version = AddSupportJarAction.getInstalledRevision();
            sCachedName = SUPPORT_LIBRARY_NAME;
            sCachedVersion = version;
            return version;
        }
    }

    private void updateVersionLabels() {
        int version = getInstalledSupportLibVersion();
        if (version == -1) {
            mInstalledVersion.setText("Not installed");
        } else {
            mInstalledVersion.setText(Integer.toString(version));
        }

        if (mTemplate != null) {
            for (Pair<String, Integer> dependency : mTemplate.getDependencies()) {
                String name = dependency.getFirst();
                if (name.equals(SUPPORT_LIBRARY_NAME)) {
                    int required = dependency.getSecond();
                    mRequiredVersion.setText(Integer.toString(required));
                    break;
                }
            }
        }
    }

    private void validatePage() {
        if (mTemplate == null) {
            return;
        }

        IStatus status = null;

        List<Pair<String, Integer>> dependencies = mTemplate.getDependencies();
        if (dependencies.size() > 1 || dependencies.size() == 1
                && !dependencies.get(0).getFirst().equals(SUPPORT_LIBRARY_NAME)) {
            status = new Status(IStatus.WARNING, AdtPlugin.PLUGIN_ID,
                    "Unsupported template dependency: Upgrade your Android Eclipse plugin");
        }

        setPageComplete(status == null || status.getSeverity() != IStatus.ERROR);
        if (status != null) {
            setMessage(status.getMessage(),
                    status.getSeverity() == IStatus.ERROR
                        ? IMessageProvider.ERROR : IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mCheckButton) {
            sCachedName = null;
            if (isInstalled()) {
                showNextPage();
            }
            updateVersionLabels();
        } else if (source == mInstallButton) {
            sCachedName = null;
            for (Pair<String, Integer> dependency : mTemplate.getDependencies()) {
                String name = dependency.getFirst();
                if (SUPPORT_LIBRARY_NAME.equals(name)) {
                    int version = dependency.getSecond();
                    File installed = AddSupportJarAction.installSupport(version);
                    if (installed != null) {
                        showNextPage();
                    }
                    updateVersionLabels();
                }
            }
        } else if (source == mLink) {
            try {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWebBrowser browser = workbench.getBrowserSupport().getExternalBrowser();
                browser.openURL(new URL(URL));
            } catch (Exception ex) {
                String message = String.format("Could not open browser. Vist\n%1$s\ninstead.",
                        URL);
                MessageDialog.openError(getShell(), "Browser Error", message);
            }
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
