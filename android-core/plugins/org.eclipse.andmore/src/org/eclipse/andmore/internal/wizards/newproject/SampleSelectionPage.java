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
package org.eclipse.andmore.internal.wizards.newproject;

import com.android.SdkConstants;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/** Page where the user can select a sample to "instantiate" */
class SampleSelectionPage extends WizardPage implements SelectionListener, ModifyListener {
    private final NewProjectWizardState mValues;
    private boolean mIgnore;

    private Table mTable;
    private TableViewer mTableViewer;
    private IAndroidTarget mCurrentSamplesTarget;
    private Text mSampleProjectName;

    /**
     * Create the wizard.
     */
    SampleSelectionPage(NewProjectWizardState values) {
        super("samplePage"); //$NON-NLS-1$
        setTitle("Select Sample");
        setDescription("Select which sample to create");
        mValues = values;
    }

    /**
     * Create contents of the wizard.
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        mTableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        mTable = mTableViewer.getTable();
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gridData.heightHint = 300;
        mTable.setLayoutData(gridData);
        mTable.addSelectionListener(this);

        setControl(container);

        Label projectLabel = new Label(container, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        projectLabel.setText("Project Name:");

        mSampleProjectName = new Text(container, SWT.BORDER);
        mSampleProjectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mSampleProjectName.addModifyListener(this);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            if (mValues.projectName != null) {
                try {
                    mIgnore = true;
                    mSampleProjectName.setText(mValues.projectName);
                } finally {
                    mIgnore = false;
                }
            }

            // Update samples list if the SDK target has changed (or if it hasn't yet
            // been populated)
            if (mCurrentSamplesTarget != mValues.target) {
                mCurrentSamplesTarget = mValues.target;
                updateSamples();
            }

            validatePage();
        }
    }

    private void updateSamples() {
        IBaseLabelProvider labelProvider = new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                return AdtPlugin.getAndroidLogo();
            }

            @Override
            public String getText(Object element) {
                if (element instanceof Pair<?, ?>) {
                    Object name = ((Pair<?, ?>) element).getFirst();
                    return name.toString();
                }
                return element.toString(); // Fallback. Should not happen.
            }
        };

        mTableViewer.setContentProvider(new ArrayContentProvider());
        mTableViewer.setLabelProvider(labelProvider);

        if (mValues.samples != null && mValues.samples.size() > 0) {
            Object[] samples = mValues.samples.toArray();
            mTableViewer.setInput(samples);

            mTable.select(0);
            selectSample(mValues.samples.get(0).getSecond());
            extractNamesFromAndroidManifest();
        }
    }

    private void selectSample(File sample) {
        mValues.chosenSample = sample;
        if (sample != null && !mValues.projectNameModifiedByUser) {
            mValues.projectName = sample.getName();
            if (SdkConstants.FD_SAMPLE.equals(mValues.projectName) &&
                    sample.getParentFile() != null) {
                mValues.projectName = sample.getParentFile().getName() + '_' + mValues.projectName;
            }
            try {
                mIgnore = true;
                mSampleProjectName.setText(mValues.projectName);
            } finally {
                mIgnore = false;
            }
            updatedProjectName();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        if (e.getSource() == mTable) {
            extractNamesFromAndroidManifest();
            int index = mTable.getSelectionIndex();
            if (index >= 0) {
                Object[] roots = (Object[]) mTableViewer.getInput();
                selectSample(((Pair<String, File>) roots[index]).getSecond());
            } else {
                selectSample(null);
            }
        } else {
            assert false : e.getSource();
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        if (e.getSource() == mSampleProjectName) {
            mValues.projectName = mSampleProjectName.getText().trim();
            mValues.projectNameModifiedByUser = true;
            updatedProjectName();
        }

        validatePage();
    }

    private void updatedProjectName() {
        if (mValues.useDefaultLocation) {
            mValues.projectLocation = Platform.getLocation().toFile();
        }
    }

    /**
     * A sample was selected. Update the location field, manifest and validate.
     * Extract names from an android manifest.
     * This is done only if the user selected the "use existing source" and a manifest xml file
     * can actually be found in the custom user directory.
     */
    private void extractNamesFromAndroidManifest() {
        if (mValues.chosenSample == null || !mValues.chosenSample.isDirectory()) {
            return;
        }

        Path path = new Path(mValues.chosenSample.getPath());
        mValues.extractFromAndroidManifest(path);
    }

    @Override
    public boolean isPageComplete() {
        if (mValues.mode != Mode.SAMPLE) {
            return true;
        }

        // Ensure that when creating samples, the Finish button isn't enabled until
        // the user has reached and completed this page
        if (mValues.chosenSample == null) {
            return false;
        }

        return super.isPageComplete();
    }

    private void validatePage() {
        IStatus status = null;
        if (mValues.samples == null || mValues.samples.size() == 0) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "The chosen SDK does not contain any samples");
        } else if (mValues.chosenSample == null) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, "Choose a sample");
        } else if (!mValues.chosenSample.exists()) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format("Sample does not exist: %1$s", mValues.chosenSample.getPath()));
        } else {
            status = ProjectNamePage.validateProjectName(mValues.projectName);
        }

        // -- update UI & enable finish if there's no error
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
}
