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

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.Sdk.ITargetChangeListener;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/** Page where you choose the application name, activity name, and optional test project info */
public class ApplicationInfoPage extends WizardPage implements SelectionListener, ModifyListener,
        ITargetChangeListener {
    private static final String JDK_15 = "1.5"; //$NON-NLS-1$
    private final static String DUMMY_PACKAGE = "your.package.namespace";

    /** Suffix added by default to activity names */
    static final String ACTIVITY_NAME_SUFFIX = "Activity"; //$NON-NLS-1$

    private final NewProjectWizardState mValues;

    private Text mApplicationText;
    private Text mPackageText;
    private Text mActivityText;
    private Button mCreateActivityCheckbox;
    private Combo mSdkCombo;

    private boolean mIgnore;
    private Button mCreateTestCheckbox;
    private Text mTestProjectNameText;
    private Text mTestApplicationText;
    private Text mTestPackageText;
    private Label mTestProjectNameLabel;
    private Label mTestApplicationLabel;
    private Label mTestPackageLabel;

    /**
     * Create the wizard.
     */
    ApplicationInfoPage(NewProjectWizardState values) {
        super("appInfo"); //$NON-NLS-1$
        mValues = values;

        setTitle("Application Info");
        setDescription("Configure the new Android Project");
        AndmoreAndroidPlugin.getDefault().addTargetListener(this);
    }

    /**
     * Create contents of the wizard.
     */
    @Override
    @SuppressWarnings("unused") // Eclipse marks SWT constructors with side effects as unused
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        Label applicationLabel = new Label(container, SWT.NONE);
        applicationLabel.setText("Application Name:");

        mApplicationText = new Text(container, SWT.BORDER);
        mApplicationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mApplicationText.addModifyListener(this);

        Label packageLabel = new Label(container, SWT.NONE);
        packageLabel.setText("Package Name:");

        mPackageText = new Text(container, SWT.BORDER);
        mPackageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mPackageText.addModifyListener(this);

        if (mValues.mode != Mode.TEST) {
            mCreateActivityCheckbox = new Button(container, SWT.CHECK);
            mCreateActivityCheckbox.setText("Create Activity:");
            mCreateActivityCheckbox.addSelectionListener(this);

            mActivityText = new Text(container, SWT.BORDER);
            mActivityText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            mActivityText.addModifyListener(this);
        }

        Label minSdkLabel = new Label(container, SWT.NONE);
        minSdkLabel.setText("Minimum SDK:");

        mSdkCombo = new Combo(container, SWT.NONE);
        GridData gdSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gdSdkCombo.widthHint = 200;
        mSdkCombo.setLayoutData(gdSdkCombo);
        mSdkCombo.addSelectionListener(this);
        mSdkCombo.addModifyListener(this);

        onSdkLoaded();

        setControl(container);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        mCreateTestCheckbox = new Button(container, SWT.CHECK);
        mCreateTestCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mCreateTestCheckbox.setText("Create a Test Project");
        mCreateTestCheckbox.addSelectionListener(this);

        mTestProjectNameLabel = new Label(container, SWT.NONE);
        mTestProjectNameLabel.setText("Test Project Name:");

        mTestProjectNameText = new Text(container, SWT.BORDER);
        mTestProjectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mTestProjectNameText.addModifyListener(this);

        mTestApplicationLabel = new Label(container, SWT.NONE);
        mTestApplicationLabel.setText("Test Application:");

        mTestApplicationText = new Text(container, SWT.BORDER);
        mTestApplicationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mTestApplicationText.addModifyListener(this);

        mTestPackageLabel = new Label(container, SWT.NONE);
        mTestPackageLabel.setText("Test Package:");

        mTestPackageText = new Text(container, SWT.BORDER);
        mTestPackageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mTestPackageText.addModifyListener(this);
    }

    /** Controls whether the options for creating a paired test project should be shown */
    private void showTestOptions(boolean visible) {
        if (mValues.mode == Mode.SAMPLE) {
            visible = false;
        }

        mCreateTestCheckbox.setVisible(visible);
        mTestProjectNameLabel.setVisible(visible);
        mTestProjectNameText.setVisible(visible);
        mTestApplicationLabel.setVisible(visible);
        mTestApplicationText.setVisible(visible);
        mTestPackageLabel.setVisible(visible);
        mTestPackageText.setVisible(visible);
    }

    /** Controls whether the options for creating a paired test project should be enabled */
    private void enableTestOptions(boolean enabled) {
        mTestProjectNameLabel.setEnabled(enabled);
        mTestProjectNameText.setEnabled(enabled);
        mTestApplicationLabel.setEnabled(enabled);
        mTestApplicationText.setEnabled(enabled);
        mTestPackageLabel.setEnabled(enabled);
        mTestPackageText.setEnabled(enabled);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            try {
                mIgnore = true;
                if (mValues.applicationName != null) {
                    mApplicationText.setText(mValues.applicationName);
                }
                if (mValues.packageName != null) {
                    mPackageText.setText(mValues.packageName);
                } else {
                    mPackageText.setText(DUMMY_PACKAGE);
                }

                if (mValues.mode != Mode.TEST) {
                    mCreateActivityCheckbox.setSelection(mValues.createActivity);
                    mActivityText.setEnabled(mValues.createActivity);
                    if (mValues.activityName != null) {
                        mActivityText.setText(mValues.activityName);
                    }
                }
                if (mValues.minSdk != null && mValues.minSdk.length() > 0) {
                    mSdkCombo.setText(mValues.minSdk);
                }

                showTestOptions(mValues.mode == Mode.ANY);
                enableTestOptions(mCreateTestCheckbox.getSelection());

                if (mValues.testProjectName != null) {
                    mTestProjectNameText.setText(mValues.testProjectName);
                }
                if (mValues.testApplicationName != null) {
                    mTestApplicationText.setText(mValues.testApplicationName);
                }
                if (mValues.testProjectName != null) {
                    mTestPackageText.setText(mValues.testProjectName);
                }
            } finally {
                mIgnore = false;
            }
        }

        // Start focus with the package name, since the other fields are typically assigned
        // reasonable defaults
        mPackageText.setFocus();
        mPackageText.selectAll();

        validatePage();
    }

    protected void setSdkTargets(Collection<IAndroidTarget> targets, IAndroidTarget target) {
        if (targets == null) {
            targets = Collections.emptySet();
        }
        int selectionIndex = -1;
        String[] items = new String[targets.size()];
        int i = -1;
        for (IAndroidTarget candidate : targets) {
            items[++i] = targetLabel(candidate);
            if (candidate == target) {
                selectionIndex = i;
            }
        }
        try {
            mIgnore = true;
            mSdkCombo.setItems(items);
            mSdkCombo.setData(targets);
            if (selectionIndex != -1) {
                mSdkCombo.select(selectionIndex);
            }
        } finally {
            mIgnore = false;
        }
    }

    private String targetLabel(IAndroidTarget target) {
        // In the minimum SDK chooser, show the targets with api number and description,
        // such as "11 (Android 3.0)"
        return String.format("%1$s (%2$s)", target.getVersion().getApiString(),
                target.getFullName());
    }

    @Override
    public void dispose() {
        AndmoreAndroidPlugin.getDefault().removeTargetListener(this);
        super.dispose();
    }

    @Override
    public boolean isPageComplete() {
        // This page is only needed when creating new projects
        if (mValues.useExisting || mValues.mode != Mode.ANY) {
            return true;
        }

        // Ensure that we reach this page
        if (mValues.packageName == null) {
            return false;
        }

        return super.isPageComplete();
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mSdkCombo) {
            mValues.minSdk = mSdkCombo.getText().trim();
            IAndroidTarget[] targets = (IAndroidTarget[]) mSdkCombo.getData();
            // An editable combo will treat item selection the same way as a user edit,
            // so we need to see if the string looks like a labeled version
            int index = mSdkCombo.getSelectionIndex();
            if (index != -1) {
                if (index >= 0 && index < targets.length) {
                    IAndroidTarget target = targets[index];
                    if (targetLabel(target).equals(mValues.minSdk)) {
                        mValues.minSdk = target.getVersion().getApiString();
                    }
                }
            }

            // Ensure that we never pick up the (Android x.y) suffix shown in combobox
            // for readability
            int separator = mValues.minSdk.indexOf(' ');
            if (separator != -1) {
                mValues.minSdk = mValues.minSdk.substring(0, separator);
            }
            mValues.minSdkModifiedByUser = true;
            mValues.updateSdkTargetToMatchMinSdkVersion();
        } else if (source == mApplicationText) {
            mValues.applicationName = mApplicationText.getText().trim();
            mValues.applicationNameModifiedByUser = true;

            if (!mValues.testApplicationNameModified) {
                mValues.testApplicationName = suggestTestApplicationName(mValues.applicationName);
                try {
                    mIgnore = true;
                    mTestApplicationText.setText(mValues.testApplicationName);
                } finally {
                    mIgnore = false;
                }
            }

        } else if (source == mPackageText) {
            mValues.packageName = mPackageText.getText().trim();
            mValues.packageNameModifiedByUser = true;

            if (!mValues.testPackageModified) {
                mValues.testPackageName = suggestTestPackage(mValues.packageName);
                try {
                    mIgnore = true;
                    mTestPackageText.setText(mValues.testPackageName);
                } finally {
                    mIgnore = false;
                }
            }
        } else if (source == mActivityText) {
            mValues.activityName = mActivityText.getText().trim();
            mValues.activityNameModifiedByUser = true;
        } else if (source == mTestApplicationText) {
            mValues.testApplicationName = mTestApplicationText.getText().trim();
            mValues.testApplicationNameModified = true;
        } else if (source == mTestPackageText) {
            mValues.testPackageName = mTestPackageText.getText().trim();
            mValues.testPackageModified = true;
        } else if (source == mTestProjectNameText) {
            mValues.testProjectName = mTestProjectNameText.getText().trim();
            mValues.testProjectModified = true;
        }

        validatePage();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();

        if (source == mCreateActivityCheckbox) {
            mValues.createActivity = mCreateActivityCheckbox.getSelection();
            mActivityText.setEnabled(mValues.createActivity);
        } else if (source == mSdkCombo) {
            int index = mSdkCombo.getSelectionIndex();
            IAndroidTarget[] targets = (IAndroidTarget[]) mSdkCombo.getData();
            if (index != -1) {
                if (index >= 0 && index < targets.length) {
                    IAndroidTarget target = targets[index];
                    // Even though we are showing the logical version name, we place the
                    // actual api number as the minimum SDK
                    mValues.minSdk = target.getVersion().getApiString();
                }
            } else {
                String text = mSdkCombo.getText();
                boolean found = false;
                for (IAndroidTarget target : targets) {
                    if (targetLabel(target).equals(text)) {
                        mValues.minSdk = target.getVersion().getApiString();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    mValues.minSdk = text;
                }
            }
        } else if (source == mCreateTestCheckbox) {
            mValues.createPairProject = mCreateTestCheckbox.getSelection();
            enableTestOptions(mValues.createPairProject);
            if (mValues.createPairProject) {
                if (mValues.testProjectName == null || mValues.testProjectName.length() == 0) {
                    mValues.testProjectName = suggestTestProjectName(mValues.projectName);
                }
                if (mValues.testApplicationName == null ||
                        mValues.testApplicationName.length() == 0) {
                    mValues.testApplicationName =
                            suggestTestApplicationName(mValues.applicationName);
                }
                if (mValues.testPackageName == null || mValues.testPackageName.length() == 0) {
                    mValues.testPackageName = suggestTestPackage(mValues.packageName);
                }

                try {
                    mIgnore = true;
                    mTestProjectNameText.setText(mValues.testProjectName);
                    mTestApplicationText.setText(mValues.testApplicationName);
                    mTestPackageText.setText(mValues.testPackageName);
                } finally {
                    mIgnore = false;
                }
            }
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    private void validatePage() {
        IStatus status = validatePackage(mValues.packageName);
        if (status == null || status.getSeverity() != IStatus.ERROR) {
            IStatus validActivity = validateActivity();
            if (validActivity != null) {
                status = validActivity;
            }
        }
        if (status == null || status.getSeverity() != IStatus.ERROR) {
            IStatus validMinSdk = validateMinSdk();
            if (validMinSdk != null) {
                status = validMinSdk;
            }
        }

        if (status == null || status.getSeverity() != IStatus.ERROR) {
            IStatus validSourceFolder = validateSourceFolder();
            if (validSourceFolder != null) {
                status = validSourceFolder;
            }
        }

        // If creating a test project to go along with the main project, also validate
        // the additional test project parameters
        if (status == null || status.getSeverity() != IStatus.ERROR) {
            if (mValues.createPairProject) {
                IStatus validTestProject = ProjectNamePage.validateProjectName(
                        mValues.testProjectName);
                if (validTestProject != null) {
                    status = validTestProject;
                }

                if (status == null || status.getSeverity() != IStatus.ERROR) {
                    IStatus validTestLocation = validateTestProjectLocation();
                    if (validTestLocation != null) {
                        status = validTestLocation;
                    }
                }

                if (status == null || status.getSeverity() != IStatus.ERROR) {
                    IStatus validTestPackage = validatePackage(mValues.testPackageName);
                    if (validTestPackage != null) {
                        status = new Status(validTestPackage.getSeverity(),
                                AndmoreAndroidPlugin.PLUGIN_ID,
                                validTestPackage.getMessage() + " (in test package)");
                    }
                }

                if (status == null || status.getSeverity() != IStatus.ERROR) {
                    if (mValues.projectName.equals(mValues.testProjectName)) {
                        status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                             "The main project name and the test project name must be different.");
                    }
                }
            }
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

    private IStatus validateTestProjectLocation() {
        assert mValues.createPairProject;

        // Validate location
        Path path = new Path(mValues.projectLocation.getPath());
        if (!mValues.useExisting) {
            if (!mValues.useDefaultLocation) {
                // If not using the default value validate the location.
                URI uri = URIUtil.toURI(path.toOSString());
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IProject handle = workspace.getRoot().getProject(mValues.testProjectName);
                IStatus locationStatus = workspace.validateProjectLocationURI(handle, uri);
                if (!locationStatus.isOK()) {
                    return locationStatus;
                }
                // The location is valid as far as Eclipse is concerned (i.e. mostly not
                // an existing workspace project.) Check it either doesn't exist or is
                // a directory that is empty.
                File f = path.toFile();
                if (f.exists() && !f.isDirectory()) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            "A directory name must be specified.");
                } else if (f.isDirectory()) {
                    // However if the directory exists, we should put a
                    // warning if it is not empty. We don't put an error
                    // (we'll ask the user again for confirmation before
                    // using the directory.)
                    String[] l = f.list();
                    if (l != null && l.length != 0) {
                        return new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                                "The selected output directory is not empty.");
                    }
                }
            } else {
                IPath destPath = path.removeLastSegments(1).append(mValues.testProjectName);
                File dest = destPath.toFile();
                if (dest.exists()) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            String.format(
                                    "There is already a file or directory named \"%1$s\" in the selected location.",
                            mValues.testProjectName));
                }
            }
        }

        return null;
    }

    private IStatus validateSourceFolder() {
        // This check does nothing when creating a new project.
        // This check is also useless when no activity is present or created.
        mValues.sourceFolder = SdkConstants.FD_SOURCES;
        if (!mValues.useExisting || !mValues.createActivity) {
            return null;
        }

        String osTarget = mValues.activityName;
        if (osTarget.indexOf('.') == -1) {
            osTarget = mValues.packageName + File.separator + osTarget;
        } else if (osTarget.indexOf('.') == 0) {
            osTarget = mValues.packageName + osTarget;
        }
        osTarget = osTarget.replace('.', File.separatorChar) + SdkConstants.DOT_JAVA;

        File projectDir = mValues.projectLocation;
        File[] allDirs = projectDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (allDirs != null) {
            boolean found = false;
            for (File f : allDirs) {
                Path path = new Path(f.getAbsolutePath());
                File java_activity = path.append(osTarget).toFile();
                if (java_activity.isFile()) {
                    mValues.sourceFolder = f.getName();
                    found = true;
                    break;
                }
            }

            if (!found) {
                String projectPath = projectDir.getPath();
                if (allDirs.length > 0) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            String.format("%1$s can not be found under %2$s.", osTarget,
                            projectPath));
                } else {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            String.format("No source folders can be found in %1$s.",
                            projectPath));
                }
            }
        }

        return null;
    }

    private IStatus validateMinSdk() {
        // Validate min SDK field
        // If the min sdk version is empty, it is always accepted.
        if (mValues.minSdk == null || mValues.minSdk.length() == 0) {
            return null;
        }

        IAndroidTarget target = mValues.target;
        if (target == null) {
            return null;
        }

        // If the current target is a preview, explicitly indicate minSdkVersion
        // must be set to this target name.
        if (target.getVersion().isPreview() && !target.getVersion().equals(mValues.minSdk)) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    String.format(
                            "The SDK target is a preview. Min SDK Version must be set to '%s'.",
                            target.getVersion().getCodename()));
        }

        if (!target.getVersion().equals(mValues.minSdk)) {
            return new Status(target.getVersion().isPreview() ? IStatus.ERROR : IStatus.WARNING,
                    AndmoreAndroidPlugin.PLUGIN_ID,
                    "The API level for the selected SDK target does not match the Min SDK Version."
                    );
        }

        return null;
    }

    public static IStatus validatePackage(String packageFieldContents) {
        // Validate package
        if (packageFieldContents == null || packageFieldContents.length() == 0) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Package name must be specified.");
        } else if (packageFieldContents.equals(DUMMY_PACKAGE)) {
            // The dummy package name is just a placeholder package (which isn't even valid
            // because it contains the reserved Java keyword "package") but we want to
            // make the error message say that a proper package should be entered rather than
            // what's wrong with this specific package. (And the reason we provide a dummy
            // package rather than a blank line is to make it more clear to beginners what
            // we're looking for.
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Package name must be specified.");
        }
        // Check it's a valid package string
        IStatus status = JavaConventions.validatePackageName(packageFieldContents, JDK_15,
                JDK_15);
        if (!status.isOK()) {
            return status;
        }

        // The Android Activity Manager does not accept packages names with only one
        // identifier. Check the package name has at least one dot in them (the previous rule
        // validated that if such a dot exist, it's not the first nor last characters of the
        // string.)
        if (packageFieldContents.indexOf('.') == -1) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Package name must have at least two identifiers.");
        }

        return null;
    }

    public static IStatus validateClass(String className) {
        if (className == null || className.length() == 0) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Class name must be specified.");
        }
        if (className.indexOf('.') != -1) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Enter just a class name, not a full package name");
        }
        return JavaConventions.validateJavaTypeName(className, JDK_15, JDK_15);
    }

    private IStatus validateActivity() {
        // Validate activity (if creating an activity)
        if (!mValues.createActivity) {
            return null;
        }

        return validateActivity(mValues.activityName);
    }

    /**
     * Validates the given activity name
     *
     * @param activityFieldContents the activity name to validate
     * @return a status for whether the activity name is valid
     */
    public static IStatus validateActivity(String activityFieldContents) {
        // Validate activity field
        if (activityFieldContents == null || activityFieldContents.length() == 0) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Activity name must be specified.");
        } else if (ACTIVITY_NAME_SUFFIX.equals(activityFieldContents)) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, "Enter a valid activity name");
        } else if (activityFieldContents.contains("..")) { //$NON-NLS-1$
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Package segments in activity name cannot be empty (..)");
        }
        // The activity field can actually contain part of a sub-package name
        // or it can start with a dot "." to indicates it comes from the parent package
        // name.
        String packageName = "";  //$NON-NLS-1$
        int pos = activityFieldContents.lastIndexOf('.');
        if (pos >= 0) {
            packageName = activityFieldContents.substring(0, pos);
            if (packageName.startsWith(".")) { //$NON-NLS-1$
                packageName = packageName.substring(1);
            }

            activityFieldContents = activityFieldContents.substring(pos + 1);
        }

        // the activity field can contain a simple java identifier, or a
        // package name or one that starts with a dot. So if it starts with a dot,
        // ignore this dot -- the rest must look like a package name.
        if (activityFieldContents.length() > 0 && activityFieldContents.charAt(0) == '.') {
            activityFieldContents = activityFieldContents.substring(1);
        }

        // Check it's a valid activity string
        IStatus status = JavaConventions.validateTypeVariableName(activityFieldContents, JDK_15,
                JDK_15);
        if (!status.isOK()) {
            return status;
        }

        // Check it's a valid package string
        if (packageName.length() > 0) {
            status = JavaConventions.validatePackageName(packageName, JDK_15, JDK_15);
            if (!status.isOK()) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        status.getMessage() + " (in the activity name)");
            }
        }

        return null;
    }

    // ---- Implement ITargetChangeListener ----

    @Override
    public void onSdkLoaded() {
        if (mSdkCombo == null) {
            return;
        }

        // Update the sdk target selector with the new targets

        // get the targets from the sdk
        Collection<IAndroidTarget> targets = null;
        if (Sdk.getCurrent() != null) {
            targets = Sdk.getCurrent().getTargets();
        }
        setSdkTargets(targets, mValues.target);
    }

    @Override
    public void onProjectTargetChange(IProject changedProject) {
        // Ignore
    }

    @Override
    public void onTargetLoaded(IAndroidTarget target) {
        // Ignore
    }

    public static String suggestTestApplicationName(String applicationName) {
        if (applicationName == null) {
            applicationName = ""; //$NON-NLS-1$
        }
        if (applicationName.indexOf(' ') != -1) {
            return applicationName + " Test"; //$NON-NLS-1$
        } else {
            return applicationName + "Test"; //$NON-NLS-1$
        }
    }

    public static String suggestTestProjectName(String projectName) {
        if (projectName == null) {
            projectName = ""; //$NON-NLS-1$
        }
        if (projectName.length() > 0 && Character.isUpperCase(projectName.charAt(0))) {
            return projectName + "Test"; //$NON-NLS-1$
        } else {
            return projectName + "-test"; //$NON-NLS-1$
        }
    }


    public static String suggestTestPackage(String packagePath) {
        if (packagePath == null) {
            packagePath = ""; //$NON-NLS-1$
        }
        return packagePath + ".test"; //$NON-NLS-1$
    }
}
