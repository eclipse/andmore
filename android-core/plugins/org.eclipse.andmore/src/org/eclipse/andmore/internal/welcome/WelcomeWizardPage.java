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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtPlugin.CheckSdkErrorHandler;
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
import java.util.concurrent.atomic.AtomicReference;

/** Main page shown in the {@link WelcomeWizard} */
public class WelcomeWizardPage extends WizardPage implements ModifyListener, SelectionListener {
    private Text mExistingDirText;
    private Button mExistingDirButton;
    private Button mInstallLatestCheckbox;
    private Button mInstallCommonCheckbox;
    private Button mInstallNewRadio;
    private Button mUseExistingRadio;
    private Text mNewDirText;
    private Button mNewDirButton;

    /**
     * Create the wizard.
     */
    public WelcomeWizardPage() {
        super("welcomePage");
        setTitle("Welcome to Android Development");
        setDescription("Configure SDK");
    }

    /**
     * Create contents of the wizard.
     * @param parent parent widget to add page to
     */
    @Override
    @SuppressWarnings("unused") // SWT constructors have side effects so "new Label" is not unused
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(4, false));

        Label overviewLabel = new Label(container, SWT.WRAP | SWT.SHADOW_NONE);
        GridData gdOverviewLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
        gdOverviewLabel.widthHint = 580;
        overviewLabel.setLayoutData(gdOverviewLabel);
        overviewLabel.setText("To develop for Android, you need an Android SDK, and at least one version of the Android APIs to compile against. You may also want additional versions of Android to test with.");

        Label spacing = new Label(container, SWT.NONE);
        spacing.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        mInstallNewRadio = new Button(container, SWT.RADIO);
        mInstallNewRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mInstallNewRadio.setSelection(true);
        mInstallNewRadio.setText("Install new SDK");
        mInstallNewRadio.addSelectionListener(this);

        Label indentLabel = new Label(container, SWT.NONE);
        GridData gdIndentLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdIndentLabel.widthHint = 20;
        indentLabel.setLayoutData(gdIndentLabel);

        mInstallLatestCheckbox = new Button(container, SWT.CHECK);
        mInstallLatestCheckbox.setSelection(true);
        mInstallLatestCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3,
                1));
        mInstallLatestCheckbox.setText("Install the latest available version of Android APIs (supports all the latest features)");
        mInstallLatestCheckbox.addSelectionListener(this);

        new Label(container, SWT.NONE);
        mInstallCommonCheckbox = new Button(container, SWT.CHECK);
        mInstallCommonCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3,
                1));
        mInstallCommonCheckbox.setText("Install Android 2.2, a version which is supported by ~96% phones and tablets");
        mInstallCommonCheckbox.addSelectionListener(this);

        new Label(container, SWT.NONE);
        Label addHintLabel = new Label(container, SWT.NONE);
        addHintLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        addHintLabel.setText("     (You can add additional platforms using the SDK Manager.)");

        new Label(container, SWT.NONE);
        Label targetLabel = new Label(container, SWT.NONE);
        targetLabel.setText("Target Location:");

        mNewDirText = new Text(container, SWT.BORDER);
        mNewDirText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        String defaultPath = System.getProperty("user.home") + File.separator + "android-sdks"; //$NON-NLS-1$
        mNewDirText.setText(defaultPath);
        mNewDirText.addModifyListener(this);

        mNewDirButton = new Button(container, SWT.FLAT);
        mNewDirButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mNewDirButton.setText("Browse...");
        mNewDirButton.addSelectionListener(this);

        Label spacing2 = new Label(container, SWT.NONE);
        spacing2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        mUseExistingRadio = new Button(container, SWT.RADIO);
        mUseExistingRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mUseExistingRadio.setText("Use existing SDKs");
        mUseExistingRadio.addSelectionListener(this);

        new Label(container, SWT.NONE);
        Label installationLabel = new Label(container, SWT.NONE);
        installationLabel.setText("Existing Location:");

        mExistingDirText = new Text(container, SWT.BORDER);
        mExistingDirText.setEnabled(false);
        mExistingDirText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mExistingDirText.addModifyListener(this);

        mExistingDirButton = new Button(container, SWT.FLAT);
        mExistingDirButton.setEnabled(false);
        mExistingDirButton.setText("Browse...");
        mExistingDirButton.addSelectionListener(this);
    }

    boolean isCreateNew() {
        return mInstallNewRadio.getSelection();
    }

    boolean isInstallLatest() {
        return mInstallLatestCheckbox.getSelection();
    }

    boolean isInstallCommon() {
        return mInstallCommonCheckbox.getSelection();
    }

    File getPath() {
        Text text = isCreateNew() ? mNewDirText : mExistingDirText;
        return new File(text.getText());
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();

        if (source == mExistingDirButton) {
            DirectoryDialog dialog = new DirectoryDialog(mExistingDirButton.getShell(), SWT.OPEN);
            String file = dialog.open();
            String path = mExistingDirText.getText().trim();
            if (path.length() > 0) {
                // TODO: Shouldn't this be done before the open() call?
                dialog.setFilterPath(path);
            }
            if (file != null) {
                mExistingDirText.setText(file);
            }
        } else if (source == mNewDirButton) {
            DirectoryDialog dialog = new DirectoryDialog(mNewDirButton.getShell(), SWT.OPEN);
            String path = mNewDirText.getText().trim();
            if (path.length() > 0) {
                dialog.setFilterPath(path);
            }
            String file = dialog.open();
            if (file != null) {
                mNewDirText.setText(file);
            }
        } else if (source == mInstallNewRadio) {
            mExistingDirButton.setEnabled(false);
            mExistingDirText.setEnabled(false);
            mNewDirButton.setEnabled(true);
            mNewDirText.setEnabled(true);
        } else if (source == mUseExistingRadio) {
            mExistingDirButton.setEnabled(true);
            mExistingDirText.setEnabled(true);
            mNewDirButton.setEnabled(false);
            mNewDirText.setEnabled(false);
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void modifyText(ModifyEvent e) {
        validatePage();
    }

    private void validatePage() {
        String error = null;
        String warning = null;

        if (isCreateNew()) {
            // Make sure that the target installation directory is empty or doesn't exist
            // (and that it can be created)
            String path = mNewDirText.getText().trim();
            if (path.length() == 0) {
                error = "Please enter a new directory to install the SDK into";
            } else {
                File file = new File(path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        if (!file.canWrite()) {
                            error = "Missing write permission in target directory";
                        }
                        File[] children = file.listFiles();
                        if (children != null && children.length > 0) {
                            warning = "The directory is not empty";
                        }
                    } else {
                        error = "The target must be a directory";
                    }
                } else {
                    File parent = file.getParentFile();
                    if (parent == null || !parent.exists()) {
                        error = "The parent directory does not exist";
                    } else if (!parent.canWrite()) {
                        error = "No write permission in parent directory";
                    }
                }
            }

            if (error == null && !mInstallLatestCheckbox.getSelection()
                    && !mInstallCommonCheckbox.getSelection()) {
                error = "You must choose at least one Android version to install";
            }
        } else {
            // Make sure that the existing installation directory exists and is valid
            String path = mExistingDirText.getText().trim();
            if (path.length() == 0) {
                error = "Please enter an existing SDK installation directory";
            } else {
                File file = new File(path);
                if (!file.exists()) {
                    error = "The chosen installation directory does not exist";
                } else {
                    final AtomicReference<String> errorReference = new AtomicReference<String>();
                    final AtomicReference<String> warningReference = new AtomicReference<String>();
                    AdtPlugin.getDefault().checkSdkLocationAndId(path,
                            new AdtPlugin.CheckSdkErrorHandler() {
                        @Override
                        public boolean handleError(
                                CheckSdkErrorHandler.Solution solution,
                                String message) {
                            message = message.replaceAll("\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
                            errorReference.set(message);
                            return false;  // Apply/OK must be disabled
                        }

                        @Override
                        public boolean handleWarning(
                                CheckSdkErrorHandler.Solution solution,
                                String message) {
                            message = message.replaceAll("\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
                            warningReference.set(message);
                            return true;  // Apply/OK must be enabled
                        }
                    });
                    error = errorReference.get();
                    if (warning == null) {
                        warning = warningReference.get();
                    }
                }
            }
        }

        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else if (warning != null) {
            setMessage(warning, IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }
}
