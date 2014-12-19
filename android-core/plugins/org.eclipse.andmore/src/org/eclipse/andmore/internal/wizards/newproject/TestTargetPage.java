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

import com.android.ide.common.xml.ManifestData;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.project.ProjectChooserHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredList;

/**
 * Page shown when creating a test project which lets you choose between testing
 * yourself and testing a different project
 */
class TestTargetPage extends WizardPage implements SelectionListener {
    private final NewProjectWizardState mValues;
    /** Flag used when setting button/text state manually to ignore listener updates */
    private boolean mIgnore;
    private String mLastExistingPackageName;

    private Button mCurrentRadioButton;
    private Button mExistingRadioButton;
    private FilteredList mProjectList;
    private boolean mPageShown;

    /**
     * Create the wizard.
     */
    TestTargetPage(NewProjectWizardState values) {
        super("testTargetPage"); //$NON-NLS-1$
        setTitle("Select Test Target");
        setDescription("Choose a project to test");
        mValues = values;
    }

    /**
     * Create contents of the wizard.
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(2, false));

        mCurrentRadioButton = new Button(container, SWT.RADIO);
        mCurrentRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mCurrentRadioButton.setText("This project");
        mCurrentRadioButton.addSelectionListener(this);

        mExistingRadioButton = new Button(container, SWT.RADIO);
        mExistingRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mExistingRadioButton.setText("An existing Android project:");
        mExistingRadioButton.addSelectionListener(this);

        ILabelProvider labelProvider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT);
        mProjectList = new FilteredList(container,
                SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE, labelProvider,
                true /*ignoreCase*/, false /*allowDuplicates*/, true /* matchEmptyString*/);
        mProjectList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        mProjectList.addSelectionListener(this);
    }

    private void initializeList() {
        ProjectChooserHelper helper = new ProjectChooserHelper(getShell(), null /*filter*/);
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);
        IJavaProject[] androidProjects = helper.getAndroidProjects(javaModel);
        mProjectList.setElements(androidProjects);
        if (mValues.testedProject != null) {
            for (IJavaProject project : androidProjects) {
                if (project.getProject() == mValues.testedProject) {
                    mProjectList.setSelection(new Object[] { project });
                    break;
                }
            }
        } else {
            // No initial selection: force the user to choose
            mProjectList.setSelection(new int[0]);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        mPageShown = true;

        if (visible) {
            try {
                mIgnore = true;
                mCurrentRadioButton.setSelection(mValues.testingSelf);
                mExistingRadioButton.setSelection(!mValues.testingSelf);
                mProjectList.setEnabled(!mValues.testingSelf);

                if (mProjectList.isEmpty()) {
                    initializeList();
                }
                if (!mValues.testingSelf) {
                    mProjectList.setFocus();
                    IProject project = getSelectedProject();
                    if (project != null) {
                        // The FilteredList seems to -insist- on selecting the first item
                        // in the list, even when the selection is explicitly set to an empty
                        // array. This means the user is looking at a selection, so we need
                        // to also go ahead and select this item in the model such that the
                        // two agree, even if we would have preferred to have no initial
                        // selection.
                        mValues.testedProject = project;
                    }
                }
            } finally {
                mIgnore = false;
            }
        }

        validatePage();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mExistingRadioButton) {
            mProjectList.setEnabled(true);
            mValues.testingSelf = false;
            setExistingProject(getSelectedProject());
            mProjectList.setFocus();
        } else if (source == mCurrentRadioButton) {
            mProjectList.setEnabled(false);
            mValues.testingSelf = true;
            mValues.testedProject = null;
        } else {
            // The event must be from the project list, which unfortunately doesn't
            // pass itself as the selection event, it passes a reference to some internal
            // table widget that it uses, so we check for this case last
            IProject project = getSelectedProject();
            if (project != mValues.testedProject) {
                setExistingProject(project);
            }
        }

        validatePage();
    }

    private IProject getSelectedProject() {
        Object[] selection = mProjectList.getSelection();
        IProject project = selection != null && selection.length == 1
            ? ((IJavaProject) selection[0]).getProject() : null;
        return project;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    private void setExistingProject(IProject project) {
        mValues.testedProject = project;

        // Try to update the application, package, sdk target and minSdkVersion accordingly
        if (project != null &&
                (!mValues.applicationNameModifiedByUser ||
                 !mValues.packageNameModifiedByUser     ||
                 !mValues.targetModifiedByUser          ||
                 !mValues.minSdkModifiedByUser)) {
            ManifestData manifestData = AndroidManifestHelper.parseForData(project);
            if (manifestData != null) {
                String appName = String.format("%1$sTest", project.getName());
                String packageName = manifestData.getPackage();
                String minSdkVersion = manifestData.getMinSdkVersionString();
                IAndroidTarget sdkTarget = null;
                if (Sdk.getCurrent() != null) {
                    sdkTarget = Sdk.getCurrent().getTarget(project);
                }

                if (packageName == null) {
                    packageName = "";  //$NON-NLS-1$
                }
                mLastExistingPackageName = packageName;

                if (!mValues.projectNameModifiedByUser) {
                    mValues.projectName = appName;
                }

                if (!mValues.applicationNameModifiedByUser) {
                    mValues.applicationName = appName;
                }

                if (!mValues.packageNameModifiedByUser) {
                    packageName += ".test";  //$NON-NLS-1$
                    mValues.packageName = packageName;
                }

                if (!mValues.targetModifiedByUser && sdkTarget != null) {
                    mValues.target = sdkTarget;
                }

                if (!mValues.minSdkModifiedByUser) {
                    if (minSdkVersion != null || sdkTarget != null) {
                        mValues.minSdk = minSdkVersion;
                    }
                    if (sdkTarget == null) {
                        mValues.updateSdkTargetToMatchMinSdkVersion();
                    }
                }
            }
        }

        updateTestTargetPackageField(mLastExistingPackageName);
    }

    /**
     * Updates the test target package name
     *
     * When using the "self-test" option, the packageName argument is ignored and the
     * current value from the project package is used.
     *
     * Otherwise the packageName is used if it is not null.
     */
    private void updateTestTargetPackageField(String packageName) {
        if (mValues.testingSelf) {
            mValues.testTargetPackageName = mValues.packageName;
        } else if (packageName != null) {
            mValues.testTargetPackageName = packageName;
        }
    }

    @Override
    public boolean isPageComplete() {
        // Ensure that the user sees the page and makes a selection
        if (!mPageShown) {
            return false;
        }

        return super.isPageComplete();
    }

    private void validatePage() {
        String error = null;

        if (!mValues.testingSelf) {
            if (mValues.testedProject == null) {
                error = "Please select an existing Android project as a test target.";
            } else if (mValues.projectName.equals(mValues.testedProject.getName())) {
                error = "The main project name and the test project name must be different.";
            }
        }

        // -- update UI & enable finish if there's no error
        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }
}
