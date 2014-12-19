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

package org.eclipse.andmore.internal.assetstudio;

import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.internal.project.ProjectChooserHelper;
import org.eclipse.andmore.internal.project.ProjectChooserHelper.ProjectCombo;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Page for choosing the type of asset to create, as well as the target project */
public class ChooseAssetTypePage extends WizardPage implements SelectionListener, ModifyListener {
    private final CreateAssetSetWizardState mValues;
    private ProjectCombo mProjectButton;
    private Button mClipboardButton;
    private Text mNameText;
    private boolean mNameModified;
    private Label mResourceName;

    /**
     * Create the wizard.
     */
    public ChooseAssetTypePage(CreateAssetSetWizardState values) {
        super("chooseAssetTypePage");
        mValues = values;
        setTitle("Choose Icon Set Type");
        setDescription("Select the type of icon set to create:");
    }

    /**
     * Create contents of the wizard.
     *
     * @param parent the parent composite
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(3, false));

        for (AssetType type : AssetType.values()) {
            Button button = new Button(container, SWT.RADIO);
            button.setData(type);
            button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
            button.setSelection(type == mValues.type);
            button.setText(type.getDisplayName());
            button.addSelectionListener(this);
        }

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gdSeparator = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
        gdSeparator.heightHint = 20;
        separator.setLayoutData(gdSeparator);

        Label projectLabel = new Label(container, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        projectLabel.setText("Project:");

        ProjectChooserHelper helper =
                new ProjectChooserHelper(getShell(), null /* filter */);
        mProjectButton = new ProjectCombo(helper, container, mValues.project);
        mProjectButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mProjectButton.addSelectionListener(this);

        Label assetLabel = new Label(container, SWT.NONE);
        assetLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        assetLabel.setText("Icon Name:");

        mNameText = new Text(container, SWT.BORDER);
        mNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mNameText.addModifyListener(this);

        Label resourceLabel = new Label(container, SWT.NONE);
        resourceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        resourceLabel.setText("Resource:");

        mResourceName = new Label(container, SWT.NONE);
        mResourceName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mClipboardButton = new Button(container, SWT.FLAT);
        mClipboardButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mClipboardButton.setText("Copy Name to Clipboard");

        mClipboardButton.addSelectionListener(this);

        updateAssetType();
        validatePage();
        parent.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                mNameText.setFocus();
            }
        });
    }

    private void updateAssetType() {
        if (!mNameModified) {
            // Default name suggestion, possibly as a suffix, e.g. "ic_menu_<name>"
            String replace = "name";
            String suggestedName = String.format(mValues.type.getDefaultNameFormat(), replace);
            mNameText.setText(suggestedName);
            mValues.outputName = suggestedName;

            updateResourceLabel();
            mNameModified = false;
            int start = suggestedName.indexOf(replace);
            if (start != -1) {
                mNameText.setSelection(start, start + replace.length());
            } else {
                mNameText.selectAll();
            }
        } else {
            mNameText.selectAll();
        }
    }

    private void updateResourceLabel() {
        mResourceName.setText("@drawable/" + getOutputName()); //$NON-NLS-1$
    }

    @Override
    public boolean canFlipToNextPage() {
        return mValues.project != null;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mProjectButton) {
            mValues.project = mProjectButton.getSelectedProject();
            validatePage();
        } else if (source == mClipboardButton) {
            Clipboard clipboard = new Clipboard(getShell().getDisplay());
            TextTransfer textTransfer = TextTransfer.getInstance();
            clipboard.setContents(
                    new Object[] { mResourceName.getText() },
                    new Transfer[] { textTransfer });
            clipboard.dispose();
        } else if (source instanceof Button) {
            // User selected a different asset type to be created
            Object data = ((Button) source).getData();
            if (data instanceof AssetType) {
                mValues.type = (AssetType) data;
                CreateAssetSetWizardState.sLastType = mValues.type;
                updateAssetType();
            }
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void modifyText(ModifyEvent e) {
        Object source = e.getSource();
        if (source == mNameText) {
            mNameModified = true;
            mValues.outputName = mNameText.getText().trim();
            updateResourceLabel();
        }

        validatePage();
    }

    private String getOutputName() {
        return mNameText.getText().trim();
    }

    private void validatePage() {
        String error = null;

        if (mValues.project == null) {
            error = "Please select an Android project.";
        } else {
            String outputName = getOutputName();
            if (outputName == null || outputName.length() == 0) {
                error = "Please enter a name";
            } else {
                ResourceNameValidator validator =
                        ResourceNameValidator.create(true, ResourceFolderType.DRAWABLE);
                error = validator.isValid(outputName);
            }
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
