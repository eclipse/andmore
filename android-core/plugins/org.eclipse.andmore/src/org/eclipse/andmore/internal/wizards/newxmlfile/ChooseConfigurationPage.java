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
package org.eclipse.andmore.internal.wizards.newxmlfile;

import com.android.SdkConstants;
import com.android.ide.common.resources.configuration.ResourceQualifier;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.ui.ConfigurationSelector;
import org.eclipse.andmore.internal.ui.ConfigurationSelector.ConfigurationState;
import org.eclipse.andmore.internal.ui.ConfigurationSelector.SelectorMode;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileCreationPage.TypeInfo;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileWizard.Values;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Second page of the {@link NewXmlFileWizard}.
 * <p>
 * This page is used for choosing the current configuration or specific resource
 * folder.
 */
public class ChooseConfigurationPage extends WizardPage {
    private Values mValues;
    private Text mWsFolderPathTextField;
    private ConfigurationSelector mConfigSelector;
    private boolean mInternalWsFolderPathUpdate;
    private boolean mInternalConfigSelectorUpdate;

    /** Absolute destination folder root, e.g. "/res/" */
    static final String RES_FOLDER_ABS = AdtConstants.WS_RESOURCES + AdtConstants.WS_SEP;
    /** Relative destination folder root, e.g. "res/" */
    static final String RES_FOLDER_REL = SdkConstants.FD_RESOURCES + AdtConstants.WS_SEP;

    /**
     * Create the wizard.
     *
     * @param values value object holding current wizard state
     */
    public ChooseConfigurationPage(NewXmlFileWizard.Values values) {
        super("chooseConfig");
        mValues = values;
        setTitle("Choose Configuration Folder");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            if (mValues.folderPath != null) {
                mWsFolderPathTextField.setText(mValues.folderPath);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        // This UI is maintained with WindowBuilder.

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout(2, false /* makeColumnsEqualWidth */));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // label before configuration selector
        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        label.setText("Optional: Choose a specific configuration to limit the XML to:");

        // configuration selector
        mConfigSelector = new ConfigurationSelector(composite, SelectorMode.DEFAULT);
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gd.verticalAlignment = SWT.FILL;
        gd.horizontalAlignment = SWT.FILL;
        gd.horizontalSpan = 2;
        gd.heightHint = ConfigurationSelector.HEIGHT_HINT;
        mConfigSelector.setLayoutData(gd);
        mConfigSelector.setOnChangeListener(new ConfigurationChangeListener());

        // Folder name: [text]
        String tooltip = "The folder where the file will be generated, relative to the project.";

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gdSeparator = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        gdSeparator.heightHint = 10;
        separator.setLayoutData(gdSeparator);
        Label folderLabel = new Label(composite, SWT.NONE);
        folderLabel.setText("Folder:");
        folderLabel.setToolTipText(tooltip);

        mWsFolderPathTextField = new Text(composite, SWT.BORDER);
        mWsFolderPathTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mWsFolderPathTextField.setToolTipText(tooltip);
        mWsFolderPathTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                onWsFolderPathUpdated();
            }
        });

        setControl(composite);

        mConfigSelector.setConfiguration(mValues.configuration);
    }

    /**
     * Callback called when the Folder text field is changed, either programmatically
     * or by the user.
     */
    private void onWsFolderPathUpdated() {
        if (mInternalWsFolderPathUpdate) {
            return;
        }

        String wsFolderPath = mWsFolderPathTextField.getText();

        // This is a custom path, we need to sanitize it.
        // First it should start with "/res/". Then we need to make sure there are no
        // relative paths, things like "../" or "./" or even "//".
        wsFolderPath = wsFolderPath.replaceAll("/+\\.\\./+|/+\\./+|//+|\\\\+|^/+", "/");  //$NON-NLS-1$ //$NON-NLS-2$
        wsFolderPath = wsFolderPath.replaceAll("^\\.\\./+|^\\./+", "");                   //$NON-NLS-1$ //$NON-NLS-2$
        wsFolderPath = wsFolderPath.replaceAll("/+\\.\\.$|/+\\.$|/+$", "");               //$NON-NLS-1$ //$NON-NLS-2$

        // We get "res/foo" from selections relative to the project when we want a "/res/foo" path.
        if (wsFolderPath.startsWith(RES_FOLDER_REL)) {
            wsFolderPath = RES_FOLDER_ABS + wsFolderPath.substring(RES_FOLDER_REL.length());

            mInternalWsFolderPathUpdate = true;
            mWsFolderPathTextField.setText(wsFolderPath);
            mInternalWsFolderPathUpdate = false;
        }

        mValues.folderPath = wsFolderPath;

        if (wsFolderPath.startsWith(RES_FOLDER_ABS)) {
            wsFolderPath = wsFolderPath.substring(RES_FOLDER_ABS.length());

            int pos = wsFolderPath.indexOf(AdtConstants.WS_SEP_CHAR);
            if (pos >= 0) {
                wsFolderPath = wsFolderPath.substring(0, pos);
            }

            String[] folderSegments = wsFolderPath.split(SdkConstants.RES_QUALIFIER_SEP);

            if (folderSegments.length > 0) {
                String folderName = folderSegments[0];

                // update config selector
                mInternalConfigSelectorUpdate = true;
                mConfigSelector.setConfiguration(folderSegments);
                mInternalConfigSelectorUpdate = false;

                IWizardPage previous = ((NewXmlFileWizard) getWizard()).getPreviousPage(this);
                if (previous instanceof NewXmlFileCreationPage) {
                    NewXmlFileCreationPage p = (NewXmlFileCreationPage) previous;
                    p.selectTypeFromFolder(folderName);
                }
            }
        }

        validatePage();
    }

    /**
     * Callback called when the configuration has changed in the {@link ConfigurationSelector}.
     */
    private class ConfigurationChangeListener implements Runnable {
        @Override
        public void run() {
            if (mInternalConfigSelectorUpdate) {
                return;
            }

            resetFolderPath(true /*validate*/);
        }
    }

    /**
     * Reset the current Folder path based on the UI selection
     * @param validate if true, force a call to {@link #validatePage()}.
     */
    private void resetFolderPath(boolean validate) {
        TypeInfo type = mValues.type;
        if (type != null) {
            mConfigSelector.getConfiguration(mValues.configuration);
            StringBuilder sb = new StringBuilder(RES_FOLDER_ABS);
            sb.append(mValues.configuration.getFolderName(type.getResFolderType()));

            mInternalWsFolderPathUpdate = true;
            String newPath = sb.toString();
            mValues.folderPath = newPath;
            mWsFolderPathTextField.setText(newPath);
            mInternalWsFolderPathUpdate = false;

            if (validate) {
                validatePage();
            }
        }
    }

    /**
     * Returns the destination folder path relative to the project or an empty string.
     *
     * @return the currently edited folder
     */
    public String getWsFolderPath() {
        return mWsFolderPathTextField == null ? "" : mWsFolderPathTextField.getText(); //$NON-NLS-1$
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;
        String warning = null;

        // -- validate folder configuration
        if (error == null) {
            ConfigurationState state = mConfigSelector.getState();
            if (state == ConfigurationState.INVALID_CONFIG) {
                ResourceQualifier qual = mConfigSelector.getInvalidQualifier();
                if (qual != null) {
                    error =
                      String.format("The qualifier '%1$s' is invalid in the folder configuration.",
                            qual.getName());
                }
            } else if (state == ConfigurationState.REGION_WITHOUT_LANGUAGE) {
                error = "The Region qualifier requires the Language qualifier.";
            }
        }

        // -- validate generated path
        if (error == null) {
            String wsFolderPath = getWsFolderPath();
            if (!wsFolderPath.startsWith(RES_FOLDER_ABS)) {
                error = String.format("Target folder must start with %1$s.", RES_FOLDER_ABS);
            }
        }

        // -- validate destination file doesn't exist
        if (error == null) {
            IFile file = mValues.getDestinationFile();
            if (file != null && file.exists()) {
                warning = "The destination file already exists";
            }
        }

        // -- update UI & enable finish if there's no error
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
