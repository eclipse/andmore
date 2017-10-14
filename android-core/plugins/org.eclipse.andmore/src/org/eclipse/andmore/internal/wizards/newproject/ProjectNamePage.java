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

import static com.android.SdkConstants.FN_PROJECT_PROGUARD_FILE;
import static com.android.SdkConstants.OS_SDK_TOOLS_LIB_FOLDER;
import static org.eclipse.andmore.AdtUtils.capitalize;
import static org.eclipse.andmore.internal.wizards.newproject.ApplicationInfoPage.ACTIVITY_NAME_SUFFIX;

import com.android.SdkConstants;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.VersionCheck;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.TextProcessor;
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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import java.io.File;
import java.net.URI;

/**
 * Initial page shown when creating projects which asks for the project name,
 * the the location of the project, working sets, etc.
 */
public class ProjectNamePage extends WizardPage implements SelectionListener, ModifyListener {
    private final NewProjectWizardState mValues;
    /** Flag used when setting button/text state manually to ignore listener updates */
    private boolean mIgnore;
    /** Last user-browsed location, static so that it be remembered for the whole session */
    private static String sCustomLocationOsPath = "";  //$NON-NLS-1$
    private static boolean sAutoComputeCustomLocation = true;

    private Text mProjectNameText;
    private Text mLocationText;
    private Button mCreateSampleRadioButton;
    private Button mCreateNewButton;
    private Button mUseDefaultCheckBox;
    private Button mBrowseButton;
    private Label mLocationLabel;
    private WorkingSetGroup mWorkingSetGroup;
    /**
     * Whether we've made sure the Tools are up to date (enough that all the
     * resources required by the New Project wizard are present -- we don't
     * necessarily check for newer versions than that here; that's done by
     * {@link VersionCheck}, though that check doesn't <b>enforce</b> an update
     * since it needs to allow the user to proceed to access the SDK manager
     * etc.)
     */
    private boolean mCheckedSdkUptodate;

    /**
     * Create the wizard.
     * @param values current wizard state
     */
    ProjectNamePage(NewProjectWizardState values) {
        super("projectNamePage"); //$NON-NLS-1$
        mValues = values;

        setTitle("Create Android Project");
        setDescription("Select project name and type of project");
        mWorkingSetGroup = new WorkingSetGroup();
        setWorkingSets(new IWorkingSet[0]);
    }

    void init(IStructuredSelection selection, IWorkbenchPart activePart) {
        setWorkingSets(WorkingSetHelper.getSelectedWorkingSet(selection, activePart));
    }

    /**
     * Create contents of the wizard.
     * @param parent the parent to add the page to
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(3, false));

        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Project Name:");

        mProjectNameText = new Text(container, SWT.BORDER);
        mProjectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mProjectNameText.addModifyListener(this);

        if (mValues.mode != Mode.TEST) {
            mCreateNewButton = new Button(container, SWT.RADIO);
            mCreateNewButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
            mCreateNewButton.setText("Create new project in workspace");
            mCreateNewButton.addSelectionListener(this);

            // TBD: Should we hide this completely, and make samples something you only invoke
            // from the "New Sample Project" wizard?
            mCreateSampleRadioButton = new Button(container, SWT.RADIO);
            mCreateSampleRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
                    3, 1));
            mCreateSampleRadioButton.setText("Create project from existing sample");
            mCreateSampleRadioButton.addSelectionListener(this);
        }

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        mUseDefaultCheckBox = new Button(container, SWT.CHECK);
        mUseDefaultCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mUseDefaultCheckBox.setText("Use default location");
        mUseDefaultCheckBox.addSelectionListener(this);

        mLocationLabel = new Label(container, SWT.NONE);
        mLocationLabel.setText("Location:");

        mLocationText = new Text(container, SWT.BORDER);
        mLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mLocationText.addModifyListener(this);

        mBrowseButton = new Button(container, SWT.NONE);
        mBrowseButton.setText("Browse...");
        mBrowseButton.addSelectionListener(this);

        Composite group = mWorkingSetGroup.createControl(container);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            try {
                mIgnore = true;
                if (mValues.projectName != null) {
                        mProjectNameText.setText(mValues.projectName);
                    mProjectNameText.setFocus();
                }
                if (mValues.mode == Mode.ANY || mValues.mode == Mode.TEST) {
                    if (mValues.useExisting) {
                        assert false; // This is now handled by the separate import wizard
                    } else if (mCreateNewButton != null) {
                        mCreateNewButton.setSelection(true);
                    }
                } else if (mValues.mode == Mode.SAMPLE) {
                    mCreateSampleRadioButton.setSelection(true);
                }
                if (mValues.projectLocation != null) {
                    mLocationText.setText(mValues.projectLocation.getPath());
                }
                mUseDefaultCheckBox.setSelection(mValues.useDefaultLocation);
                updateLocationState();
            } finally {
                mIgnore = false;
            }
        }

        validatePage();
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();

        if (source == mProjectNameText) {
            onProjectFieldModified();
            if (!mValues.useDefaultLocation && !mValues.projectLocationModifiedByUser) {
                updateLocationPathField(null);
            }
        } else if (source == mLocationText) {
            mValues.projectLocationModifiedByUser = true;
            if (!mValues.useDefaultLocation) {
                File f = new File(mLocationText.getText().trim());
                mValues.projectLocation = f;
                if (f.exists() && f.isDirectory() && !f.equals(mValues.projectLocation)) {
                    updateLocationPathField(mValues.projectLocation.getPath());
                }
            }
        }

        validatePage();
    }

    private void onProjectFieldModified() {
        mValues.projectName = mProjectNameText.getText().trim();
        mValues.projectNameModifiedByUser = true;

        if (!mValues.applicationNameModifiedByUser) {
            mValues.applicationName = capitalize(mValues.projectName);
            if (!mValues.testApplicationNameModified) {
                mValues.testApplicationName =
                        ApplicationInfoPage.suggestTestApplicationName(mValues.applicationName);
            }
        }
        if (!mValues.activityNameModifiedByUser) {
            String name = capitalize(mValues.projectName);
            mValues.activityName = stripWhitespace(name) + ACTIVITY_NAME_SUFFIX;
        }
        if (!mValues.testProjectModified) {
            mValues.testProjectName =
                    ApplicationInfoPage.suggestTestProjectName(mValues.projectName);
        }
        if (!mValues.projectLocationModifiedByUser) {
            updateLocationPathField(null);
        }
    }

    private static String stripWhitespace(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();

        if (source == mCreateNewButton && mCreateNewButton != null
                && mCreateNewButton.getSelection()) {
            mValues.useExisting = false;
            if (mValues.mode == Mode.SAMPLE) {
                // Only reset the mode if we're toggling from sample back to create new
                // or create existing. We can only come to the sample state when we're in
                // ANY mode. (In particular, we don't want to switch to ANY if you're
                // in test mode.
                mValues.mode = Mode.ANY;
            }
            updateLocationState();
        } else if (source == mCreateSampleRadioButton && mCreateSampleRadioButton.getSelection()) {
            mValues.useExisting = true;
            mValues.useDefaultLocation = true;
            if (!mUseDefaultCheckBox.getSelection()) {
                try {
                    mIgnore = true;
                    mUseDefaultCheckBox.setSelection(true);
                } finally {
                    mIgnore = false;
                }
            }
            mValues.mode = Mode.SAMPLE;
            updateLocationState();
        } else if (source == mUseDefaultCheckBox) {
            mValues.useDefaultLocation = mUseDefaultCheckBox.getSelection();
            updateLocationState();
        } else if (source == mBrowseButton) {
            onOpenDirectoryBrowser();
        }

        validatePage();
    }

   /**
    * Enables or disable the location widgets depending on the user selection:
    * the location path is enabled when using the "existing source" mode (i.e. not new project)
    * or in new project mode with the "use default location" turned off.
    */
   private void updateLocationState() {
       boolean isNewProject = !mValues.useExisting;
       boolean isCreateFromSample = mValues.mode == Mode.SAMPLE;
       boolean useDefault = mValues.useDefaultLocation && !isCreateFromSample;
       boolean locationEnabled = (!isNewProject || !useDefault) && !isCreateFromSample;

       mUseDefaultCheckBox.setEnabled(isNewProject);
       mLocationLabel.setEnabled(locationEnabled);
       mLocationText.setEnabled(locationEnabled);
       mBrowseButton.setEnabled(locationEnabled);

       updateLocationPathField(null);
   }

    /**
     * Display a directory browser and update the location path field with the selected path
     */
    private void onOpenDirectoryBrowser() {

        String existingDir = mLocationText.getText().trim();

        // Disable the path if it doesn't exist
        if (existingDir.length() == 0) {
            existingDir = null;
        } else {
            File f = new File(existingDir);
            if (!f.exists()) {
                existingDir = null;
            }
        }

        DirectoryDialog directoryDialog = new DirectoryDialog(mLocationText.getShell());
        directoryDialog.setMessage("Browse for folder");
        directoryDialog.setFilterPath(existingDir);
        String dir = directoryDialog.open();

        if (dir != null) {
            updateLocationPathField(dir);
            validatePage();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
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

    /**
     * Updates the location directory path field.
     * <br/>
     * When custom user selection is enabled, use the absDir argument if not null and also
     * save it internally. If absDir is null, restore the last saved absDir. This allows the
     * user selection to be remembered when the user switches from default to custom.
     * <br/>
     * When custom user selection is disabled, use the workspace default location with the
     * current project name. This does not change the internally cached absDir.
     *
     * @param absDir A new absolute directory path or null to use the default.
     */
    private void updateLocationPathField(String absDir) {
        boolean isNewProject = !mValues.useExisting || mValues.mode == Mode.SAMPLE;
        boolean useDefault = mValues.useDefaultLocation;
        boolean customLocation = !isNewProject || !useDefault;

        if (!mIgnore) {
            try {
                mIgnore = true;
                if (customLocation) {
                    if (absDir != null) {
                        // We get here if the user selected a directory with the "Browse" button.
                        // Disable auto-compute of the custom location unless the user selected
                        // the exact same path.
                        sAutoComputeCustomLocation = sAutoComputeCustomLocation &&
                                                     absDir.equals(sCustomLocationOsPath);
                        sCustomLocationOsPath = TextProcessor.process(absDir);
                    } else  if (sAutoComputeCustomLocation ||
                                (!isNewProject && !new File(sCustomLocationOsPath).isDirectory())) {
                        // As a default import location, just suggest the home directory; the user
                        // needs to point to a project to import.
                        // TODO: Open file chooser automatically?
                        sCustomLocationOsPath = System.getProperty("user.home"); //$NON-NLS-1$
                    }
                    if (!mLocationText.getText().equals(sCustomLocationOsPath)) {
                        mLocationText.setText(sCustomLocationOsPath);
                        mValues.projectLocation = new File(sCustomLocationOsPath);
                    }
                } else {
                    String value = Platform.getLocation().append(mValues.projectName).toString();
                    value = TextProcessor.process(value);
                    if (!mLocationText.getText().equals(value)) {
                        mLocationText.setText(value);
                        mValues.projectLocation = new File(value);
                    }
                }
            } finally {
                mIgnore = false;
            }
        }

        if (mValues.useExisting && mValues.projectLocation != null
                && mValues.projectLocation.exists() && mValues.mode != Mode.SAMPLE) {
            mValues.extractFromAndroidManifest(new Path(mValues.projectLocation.getPath()));
            if (!mValues.projectNameModifiedByUser && mValues.projectName != null) {
                try {
                    mIgnore = true;
                    mProjectNameText.setText(mValues.projectName);
                } finally {
                    mIgnore = false;
                }
            }
        }
    }

    private void validatePage() {
        IStatus status = null;

        // Validate project name -- unless we're creating a sample, in which case
        // the user will get a chance to pick the name on the Sample page
        if (mValues.mode != Mode.SAMPLE) {
            status = validateProjectName(mValues.projectName);
        }

        if (status == null || status.getSeverity() != IStatus.ERROR) {
            IStatus validLocation = validateLocation();
            if (validLocation != null) {
                status = validLocation;
            }
        }

        if (!mCheckedSdkUptodate) {
            // Ensure that we have a recent enough version of the Tools that the right templates
            // are available
            File file = new File(AndmoreAndroidPlugin.getOsSdkFolder(), OS_SDK_TOOLS_LIB_FOLDER
                    + File.separator + FN_PROJECT_PROGUARD_FILE);
            if (!file.exists()) {
                status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        String.format("You do not have the latest version of the "
                        + "SDK Tools installed: Please update. (Missing %1$s)", file.getPath()));
            } else {
                mCheckedSdkUptodate = true;
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

    private IStatus validateLocation() {
        if (mValues.mode == Mode.SAMPLE) {
            // Samples are always created in the default directory
            return null;
        }

        // Validate location
        Path path = new Path(mValues.projectLocation.getPath());
        if (!mValues.useExisting) {
            if (!mValues.useDefaultLocation) {
                // If not using the default value validate the location.
                URI uri = URIUtil.toURI(path.toOSString());
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IProject handle = workspace.getRoot().getProject(mValues.projectName);
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
                // Otherwise validate the path string is not empty
                if (mValues.projectLocation.getPath().length() == 0) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            "A directory name must be specified.");
                }
                File dest = path.toFile();
                if (dest.exists()) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            String.format(
                                    "There is already a file or directory named \"%1$s\" in the selected location.",
                            mValues.projectName));
                }
            }
        } else {
            // Must be an existing directory
            File f = path.toFile();
            if (!f.isDirectory()) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        "An existing directory name must be specified.");
            }

            // Check there's an android manifest in the directory
            String osPath = path.append(SdkConstants.FN_ANDROID_MANIFEST_XML).toOSString();
            File manifestFile = new File(osPath);
            if (!manifestFile.isFile()) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        String.format(
                                "Choose a valid Android code directory\n" +
                                "(%1$s not found in %2$s.)",
                                SdkConstants.FN_ANDROID_MANIFEST_XML, f.getName()));
            }

            // Parse it and check the important fields.
            ManifestData manifestData = AndroidManifestHelper.parseForData(osPath);
            if (manifestData == null) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        String.format("File %1$s could not be parsed.", osPath));
            }
            String packageName = manifestData.getPackage();
            if (packageName == null || packageName.length() == 0) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        String.format("No package name defined in %1$s.", osPath));
            }
            Activity[] activities = manifestData.getActivities();
            if (activities == null || activities.length == 0) {
                // This is acceptable now as long as no activity needs to be
                // created
                if (mValues.createActivity) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            String.format("No activity name defined in %1$s.", osPath));
                }
            }

            // If there's already a .project, tell the user to use import instead.
            if (path.append(".project").toFile().exists()) {  //$NON-NLS-1$
                return new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                        "An Eclipse project already exists in this directory.\n" +
                        "Consider using File > Import > Existing Project instead.");
            }
        }

        return null;
    }

    public static IStatus validateProjectName(String projectName) {
        if (projectName == null || projectName.length() == 0) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Project name must be specified");
        } else {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IStatus nameStatus = workspace.validateName(projectName, IResource.PROJECT);
            if (!nameStatus.isOK()) {
                return nameStatus;
            } else {
                // Note: the case-sensitiveness of the project name matters and can cause a
                // conflict *later* when creating the project resource, so let's check it now.
                for (IProject existingProj : workspace.getRoot().getProjects()) {
                    if (projectName.equalsIgnoreCase(existingProj.getName())) {
                        return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                                "A project with that name already exists in the workspace");
                    }
                }
            }
        }

        return null;
    }

    @Override
    public IWizardPage getNextPage() {
        // Sync working set data to the value object, since the WorkingSetGroup
        // doesn't let us add listeners to do this lazily
        mValues.workingSets = getWorkingSets();

        return super.getNextPage();
    }
}
