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


import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.wizards.newproject.WorkingSetGroup;
import org.eclipse.andmore.internal.wizards.newproject.WorkingSetHelper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import java.io.File;

/**
 * Second wizard page in the "New Project From Template" wizard
 */
public class ProjectContentsPage extends WizardPage
        implements ModifyListener, SelectionListener, FocusListener {

    private final NewProjectWizardState mValues;

    private boolean mIgnore;
    private Button mCustomIconToggle;
    private Button mLibraryToggle;

    private Button mUseDefaultLocationToggle;
    private Label mLocationLabel;
    private Text mLocationText;
    private Button mChooseLocationButton;
    private static String sLastProjectLocation = System.getProperty("user.home"); //$NON-NLS-1$
    private Button mCreateActivityToggle;
    private WorkingSetGroup mWorkingSetGroup;

    ProjectContentsPage(NewProjectWizardState values) {
        super("newAndroidApp"); //$NON-NLS-1$
        mValues = values;
        setTitle("New Android Application");
        setDescription("Configure Project");

        mWorkingSetGroup = new WorkingSetGroup();
        setWorkingSets(new IWorkingSet[0]);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayout gl_container = new GridLayout(4, false);
        gl_container.horizontalSpacing = 10;
        container.setLayout(gl_container);

        mCustomIconToggle = new Button(container, SWT.CHECK);
        mCustomIconToggle.setSelection(true);
        mCustomIconToggle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mCustomIconToggle.setText("Create custom launcher icon");
        mCustomIconToggle.setSelection(mValues.createIcon);
        mCustomIconToggle.addSelectionListener(this);

        mCreateActivityToggle = new Button(container, SWT.CHECK);
        mCreateActivityToggle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
                4, 1));
        mCreateActivityToggle.setText("Create activity");
        mCreateActivityToggle.setSelection(mValues.createActivity);
        mCreateActivityToggle.addSelectionListener(this);

        new Label(container, SWT.NONE).setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        mLibraryToggle = new Button(container, SWT.CHECK);
        mLibraryToggle.setSelection(true);
        mLibraryToggle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mLibraryToggle.setText("Mark this project as a library");
        mLibraryToggle.setSelection(mValues.isLibrary);
        mLibraryToggle.addSelectionListener(this);

        // Blank line
        new Label(container, SWT.NONE).setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        mUseDefaultLocationToggle = new Button(container, SWT.CHECK);
        mUseDefaultLocationToggle.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mUseDefaultLocationToggle.setText("Create Project in Workspace");
        mUseDefaultLocationToggle.addSelectionListener(this);

        mLocationLabel = new Label(container, SWT.NONE);
        mLocationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mLocationLabel.setText("Location:");

        mLocationText = new Text(container, SWT.BORDER);
        mLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mLocationText.addModifyListener(this);

        mChooseLocationButton = new Button(container, SWT.NONE);
        mChooseLocationButton.setText("Browse...");
        mChooseLocationButton.addSelectionListener(this);
        mChooseLocationButton.setEnabled(false);
        setUseCustomLocation(!mValues.useDefaultLocation);

        new Label(container, SWT.NONE).setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        Composite group = mWorkingSetGroup.createControl(container);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            try {
                mIgnore = true;
                mUseDefaultLocationToggle.setSelection(mValues.useDefaultLocation);
                mLocationText.setText(mValues.projectLocation);
            } finally {
                mIgnore = false;
            }
        }

        validatePage();
    }

    private void setUseCustomLocation(boolean en) {
        mValues.useDefaultLocation = !en;
        mUseDefaultLocationToggle.setSelection(!en);
        if (!en) {
            updateProjectLocation(mValues.projectName);
        }

        mLocationLabel.setEnabled(en);
        mLocationText.setEnabled(en);
        mChooseLocationButton.setEnabled(en);
    }

    void init(IStructuredSelection selection, IWorkbenchPart activePart) {
        setWorkingSets(WorkingSetHelper.getSelectedWorkingSet(selection, activePart));
    }

    /**
     * Returns the working sets to which the new project should be added.
     *
     * @return the selected working sets to which the new project should be added
     */
    private IWorkingSet[] getWorkingSets() {
        return mWorkingSetGroup.getSelectedWorkingSets();
    }

    /**
     * Sets the working sets to which the new project should be added.
     *
     * @param workingSets the initial selected working sets
     */
    private void setWorkingSets(IWorkingSet[] workingSets) {
        assert workingSets != null;
        mWorkingSetGroup.setWorkingSets(workingSets);
    }

    @Override
    public IWizardPage getNextPage() {
        // Sync working set data to the value object, since the WorkingSetGroup
        // doesn't let us add listeners to do this lazily
        mValues.workingSets = getWorkingSets();

        return super.getNextPage();
    }

    // ---- Implements ModifyListener ----

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mLocationText) {
            mValues.projectLocation = mLocationText.getText().trim();
        }

        validatePage();
    }


    /** If the project should be created in the workspace, then update the project location
     * based on the project name. */
    private void updateProjectLocation(String projectName) {
        if (projectName == null) {
            projectName = "";
        }

        boolean useDefaultLocation = mUseDefaultLocationToggle.getSelection();

        if (useDefaultLocation) {
            IPath workspace = Platform.getLocation();
            String projectLocation = workspace.append(projectName).toOSString();
            mLocationText.setText(projectLocation);
            mValues.projectLocation = projectLocation;
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mCustomIconToggle) {
            mValues.createIcon = mCustomIconToggle.getSelection();
        } else if (source == mLibraryToggle) {
            mValues.isLibrary = mLibraryToggle.getSelection();
        } else if (source == mCreateActivityToggle) {
            mValues.createActivity = mCreateActivityToggle.getSelection();
        } else if (source == mUseDefaultLocationToggle) {
            boolean useDefault = mUseDefaultLocationToggle.getSelection();
            setUseCustomLocation(!useDefault);
        } else if (source == mChooseLocationButton) {
            String dir = promptUserForLocation(getShell());
            if (dir != null) {
                mLocationText.setText(dir);
                mValues.projectLocation = dir;
            }
        }

        validatePage();
    }

    private String promptUserForLocation(Shell shell) {
        DirectoryDialog dd = new DirectoryDialog(getShell());
        dd.setMessage("Select folder where project should be created");

        String curLocation = mLocationText.getText().trim();
        if (!curLocation.isEmpty()) {
            dd.setFilterPath(curLocation);
        } else if (sLastProjectLocation != null) {
            dd.setFilterPath(sLastProjectLocation);
        }

        String dir = dd.open();
        if (dir != null) {
            sLastProjectLocation = dir;
        }

        return dir;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    // ---- Implements FocusListener ----

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    // Validation

    void validatePage() {
        IStatus status = validateProjectLocation();

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

    static IStatus validateLocationInWorkspace(NewProjectWizardState values) {
        if (values.useDefaultLocation) {
            return null;
        }

        // Validate location
        if (values.projectName != null) {
            File dest = Platform.getLocation().append(values.projectName).toFile();
            if (dest.exists()) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, String.format(
                   "There is already a file or directory named \"%1$s\" in the selected location.",
                        values.projectName));
            }
        }

        return null;
    }

    private IStatus validateProjectLocation() {
        if (mValues.useDefaultLocation) {
            return validateLocationInWorkspace(mValues);
        }

        String location = mLocationText.getText();
        if (location.trim().isEmpty()) {
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Provide a valid file system location where the project should be created.");
        }

        File f = new File(location);
        if (f.exists()) {
            if (!f.isDirectory()) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format("'%s' is not a valid folder.", location));
            }

            File[] children = f.listFiles();
            if (children != null && children.length > 0) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format("Folder '%s' is not empty.", location));
            }
        }

        // if the folder doesn't exist, then make sure that the parent
        // exists and is a writable folder
        File parent = f.getParentFile();
        if (!parent.exists()) {
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format("Folder '%s' does not exist.", parent.getName()));
        }

        if (!parent.isDirectory()) {
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format("'%s' is not a folder.", parent.getName()));
        }

        if (!parent.canWrite()) {
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format("'%s' is not writeable.", parent.getName()));
        }

        return null;
    }
}
