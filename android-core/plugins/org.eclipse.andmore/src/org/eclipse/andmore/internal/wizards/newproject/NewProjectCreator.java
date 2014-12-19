/*
 * Copyright (C) 2007 The Android Open Source Project
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

import static com.android.SdkConstants.FN_PROJECT_PROPERTIES;
import static com.android.sdklib.internal.project.ProjectProperties.PROPERTY_LIBRARY;
import static org.eclipse.core.resources.IResource.DEPTH_ZERO;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.res2.ValueXmlHelper;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.io.StreamException;
import com.android.resources.Density;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.AndroidNature;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The actual project creator invoked from the New Project Wizard
 * <p/>
 * Note: this class is public so that it can be accessed from unit tests.
 * It is however an internal class. Its API may change without notice.
 * It should semantically be considered as a private final class.
 */
public class NewProjectCreator  {

    private static final String PARAM_SDK_TOOLS_DIR = "ANDROID_SDK_TOOLS";          //$NON-NLS-1$
    private static final String PARAM_ACTIVITY = "ACTIVITY_NAME";                   //$NON-NLS-1$
    private static final String PARAM_APPLICATION = "APPLICATION_NAME";             //$NON-NLS-1$
    private static final String PARAM_PACKAGE = "PACKAGE";                          //$NON-NLS-1$
    private static final String PARAM_IMPORT_RESOURCE_CLASS = "IMPORT_RESOURCE_CLASS"; //$NON-NLS-1$
    private static final String PARAM_PROJECT = "PROJECT_NAME";                     //$NON-NLS-1$
    private static final String PARAM_STRING_NAME = "STRING_NAME";                  //$NON-NLS-1$
    private static final String PARAM_STRING_CONTENT = "STRING_CONTENT";            //$NON-NLS-1$
    private static final String PARAM_IS_NEW_PROJECT = "IS_NEW_PROJECT";            //$NON-NLS-1$
    private static final String PARAM_SAMPLE_LOCATION = "SAMPLE_LOCATION";          //$NON-NLS-1$
    private static final String PARAM_SOURCE = "SOURCE";                            //$NON-NLS-1$
    private static final String PARAM_SRC_FOLDER = "SRC_FOLDER";                    //$NON-NLS-1$
    private static final String PARAM_SDK_TARGET = "SDK_TARGET";                    //$NON-NLS-1$
    private static final String PARAM_IS_LIBRARY = "IS_LIBRARY";                    //$NON-NLS-1$
    private static final String PARAM_MIN_SDK_VERSION = "MIN_SDK_VERSION";          //$NON-NLS-1$
    // Warning: The expanded string PARAM_TEST_TARGET_PACKAGE must not contain the
    // string "PACKAGE" since it collides with the replacement of PARAM_PACKAGE.
    private static final String PARAM_TEST_TARGET_PACKAGE = "TEST_TARGET_PCKG";     //$NON-NLS-1$
    private static final String PARAM_TARGET_SELF = "TARGET_SELF";                  //$NON-NLS-1$
    private static final String PARAM_TARGET_MAIN = "TARGET_MAIN";                  //$NON-NLS-1$
    private static final String PARAM_TARGET_EXISTING = "TARGET_EXISTING";          //$NON-NLS-1$
    private static final String PARAM_REFERENCE_PROJECT = "REFERENCE_PROJECT";      //$NON-NLS-1$

    private static final String PH_ACTIVITIES = "ACTIVITIES";                       //$NON-NLS-1$
    private static final String PH_USES_SDK = "USES-SDK";                           //$NON-NLS-1$
    private static final String PH_INTENT_FILTERS = "INTENT_FILTERS";               //$NON-NLS-1$
    private static final String PH_STRINGS = "STRINGS";                             //$NON-NLS-1$
    private static final String PH_TEST_USES_LIBRARY = "TEST-USES-LIBRARY";         //$NON-NLS-1$
    private static final String PH_TEST_INSTRUMENTATION = "TEST-INSTRUMENTATION";   //$NON-NLS-1$

    private static final String BIN_DIRECTORY =
        SdkConstants.FD_OUTPUT + AdtConstants.WS_SEP;
    private static final String BIN_CLASSES_DIRECTORY =
        SdkConstants.FD_OUTPUT + AdtConstants.WS_SEP +
        SdkConstants.FD_CLASSES_OUTPUT + AdtConstants.WS_SEP;
    private static final String RES_DIRECTORY =
        SdkConstants.FD_RESOURCES + AdtConstants.WS_SEP;
    private static final String ASSETS_DIRECTORY =
        SdkConstants.FD_ASSETS + AdtConstants.WS_SEP;
    private static final String DRAWABLE_DIRECTORY =
        SdkConstants.FD_RES_DRAWABLE + AdtConstants.WS_SEP;
    private static final String DRAWABLE_XHDPI_DIRECTORY =
            SdkConstants.FD_RES_DRAWABLE + '-' + Density.XHIGH.getResourceValue() +
            AdtConstants.WS_SEP;
    private static final String DRAWABLE_HDPI_DIRECTORY =
            SdkConstants.FD_RES_DRAWABLE + '-' + Density.HIGH.getResourceValue() +
            AdtConstants.WS_SEP;
    private static final String DRAWABLE_MDPI_DIRECTORY =
        SdkConstants.FD_RES_DRAWABLE + '-' + Density.MEDIUM.getResourceValue() +
        AdtConstants.WS_SEP;
    private static final String DRAWABLE_LDPI_DIRECTORY =
        SdkConstants.FD_RES_DRAWABLE + '-' + Density.LOW.getResourceValue() +
        AdtConstants.WS_SEP;
    private static final String LAYOUT_DIRECTORY =
        SdkConstants.FD_RES_LAYOUT + AdtConstants.WS_SEP;
    private static final String VALUES_DIRECTORY =
        SdkConstants.FD_RES_VALUES + AdtConstants.WS_SEP;
    private static final String GEN_SRC_DIRECTORY =
        SdkConstants.FD_GEN_SOURCES + AdtConstants.WS_SEP;

    private static final String TEMPLATES_DIRECTORY = "templates/"; //$NON-NLS-1$
    private static final String TEMPLATE_MANIFEST = TEMPLATES_DIRECTORY
            + "AndroidManifest.template"; //$NON-NLS-1$
    private static final String TEMPLATE_ACTIVITIES = TEMPLATES_DIRECTORY
            + "activity.template"; //$NON-NLS-1$
    private static final String TEMPLATE_USES_SDK = TEMPLATES_DIRECTORY
            + "uses-sdk.template"; //$NON-NLS-1$
    private static final String TEMPLATE_INTENT_LAUNCHER = TEMPLATES_DIRECTORY
            + "launcher_intent_filter.template"; //$NON-NLS-1$
    private static final String TEMPLATE_TEST_USES_LIBRARY = TEMPLATES_DIRECTORY
            + "test_uses-library.template"; //$NON-NLS-1$
    private static final String TEMPLATE_TEST_INSTRUMENTATION = TEMPLATES_DIRECTORY
            + "test_instrumentation.template"; //$NON-NLS-1$



    private static final String TEMPLATE_STRINGS = TEMPLATES_DIRECTORY
            + "strings.template"; //$NON-NLS-1$
    private static final String TEMPLATE_STRING = TEMPLATES_DIRECTORY
            + "string.template"; //$NON-NLS-1$
    private static final String PROJECT_ICON = "ic_launcher.png"; //$NON-NLS-1$
    private static final String ICON_XHDPI = "ic_launcher_xhdpi.png"; //$NON-NLS-1$
    private static final String ICON_HDPI = "ic_launcher_hdpi.png"; //$NON-NLS-1$
    private static final String ICON_MDPI = "ic_launcher_mdpi.png"; //$NON-NLS-1$
    private static final String ICON_LDPI = "ic_launcher_ldpi.png"; //$NON-NLS-1$

    private static final String STRINGS_FILE = "strings.xml";       //$NON-NLS-1$

    private static final String STRING_RSRC_PREFIX = SdkConstants.STRING_PREFIX;
    private static final String STRING_APP_NAME = "app_name";       //$NON-NLS-1$
    private static final String STRING_HELLO_WORLD = "hello";       //$NON-NLS-1$

    private static final String[] DEFAULT_DIRECTORIES = new String[] {
            BIN_DIRECTORY, BIN_CLASSES_DIRECTORY, RES_DIRECTORY, ASSETS_DIRECTORY };
    private static final String[] RES_DIRECTORIES = new String[] {
            DRAWABLE_DIRECTORY, LAYOUT_DIRECTORY, VALUES_DIRECTORY };
    private static final String[] RES_DENSITY_ENABLED_DIRECTORIES = new String[] {
            DRAWABLE_XHDPI_DIRECTORY,
            DRAWABLE_HDPI_DIRECTORY, DRAWABLE_MDPI_DIRECTORY, DRAWABLE_LDPI_DIRECTORY,
            LAYOUT_DIRECTORY, VALUES_DIRECTORY };

    private static final String JAVA_ACTIVITY_TEMPLATE = "java_file.template";  //$NON-NLS-1$
    private static final String LAYOUT_TEMPLATE = "layout.template";            //$NON-NLS-1$
    private static final String MAIN_LAYOUT_XML = "main.xml";                   //$NON-NLS-1$

    private final NewProjectWizardState mValues;
    private final IRunnableContext mRunnableContext;

    /**
     * Creates a new {@linkplain NewProjectCreator}
     * @param values the wizard state with initial project parameters
     * @param runnableContext the context to run project creation in
     */
    public NewProjectCreator(NewProjectWizardState values, IRunnableContext runnableContext) {
        mValues = values;
        mRunnableContext = runnableContext;
    }

    /**
     * Before actually creating the project for a new project (as opposed to using an
     * existing project), we check if the target location is a directory that either does
     * not exist or is empty.
     *
     * If it's not empty, ask the user for confirmation.
     *
     * @param destination The destination folder where the new project is to be created.
     * @return True if the destination doesn't exist yet or is an empty directory or is
     *         accepted by the user.
     */
    private boolean validateNewProjectLocationIsEmpty(IPath destination) {
        File f = new File(destination.toOSString());
        if (f.isDirectory() && f.list().length > 0) {
            return AdtPlugin.displayPrompt("New Android Project",
                    "You are going to create a new Android Project in an existing, non-empty, directory. Are you sure you want to proceed?");
        }
        return true;
    }

    /**
     * Structure that describes all the information needed to create a project.
     * This is collected from the pages by {@link NewProjectCreator#createAndroidProjects()}
     * and then used by
     * {@link NewProjectCreator#createProjectAsync(IProgressMonitor, ProjectInfo, ProjectInfo)}.
     */
    private static class ProjectInfo {
        private final IProject mProject;
        private final IProjectDescription mDescription;
        private final Map<String, Object> mParameters;
        private final HashMap<String, String> mDictionary;

        public ProjectInfo(IProject project,
                IProjectDescription description,
                Map<String, Object> parameters,
                HashMap<String, String> dictionary) {
                    mProject = project;
                    mDescription = description;
                    mParameters = parameters;
                    mDictionary = dictionary;
        }

        public IProject getProject() {
            return mProject;
        }

        public IProjectDescription getDescription() {
            return mDescription;
        }

        public Map<String, Object> getParameters() {
            return mParameters;
        }

        public HashMap<String, String> getDictionary() {
            return mDictionary;
        }
    }

    /**
     * Creates the android project.
     * @return True if the project could be created.
     */
    public boolean createAndroidProjects() {
        if (mValues.importProjects != null && !mValues.importProjects.isEmpty()) {
            return importProjects();
        }

        final ProjectInfo mainData = collectMainPageInfo();
        final ProjectInfo testData = collectTestPageInfo();

        // Create a monitored operation to create the actual project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
                createProjectAsync(monitor, mainData, testData, null, true);
            }
        };

        // Run the operation in a different thread
        runAsyncOperation(op);
        return true;
    }

    /**
     * Creates the a plain Java project without typical android directories or an Android Nature.
     * This is intended for use by unit tests and not as a general-purpose Java project creator.
     * @return True if the project could be created.
     */
    @VisibleForTesting
    public boolean createJavaProjects() {
        if (mValues.importProjects != null && !mValues.importProjects.isEmpty()) {
            return importProjects();
        }

        final ProjectInfo mainData = collectMainPageInfo();
        final ProjectInfo testData = collectTestPageInfo();

        // Create a monitored operation to create the actual project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
                createProjectAsync(monitor, mainData, testData, null, false);
            }
        };

        // Run the operation in a different thread
        runAsyncOperation(op);
        return true;
    }

    /**
     * Imports a list of projects
     */
    private boolean importProjects() {
        assert mValues.importProjects != null && !mValues.importProjects.isEmpty();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final List<ProjectInfo> projectData = new ArrayList<ProjectInfo>();
        for (ImportedProject p : mValues.importProjects) {

            // Compute the project name and the package name from the manifest
            ManifestData manifest = p.getManifest();
            if (manifest == null) {
                continue;
            }
            String packageName = manifest.getPackage();
            String projectName = p.getProjectName();
            String minSdk = manifest.getMinSdkVersionString();

            final IProject project = workspace.getRoot().getProject(projectName);
            final IProjectDescription description =
                    workspace.newProjectDescription(project.getName());

            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put(PARAM_PROJECT, projectName);
            parameters.put(PARAM_PACKAGE, packageName);
            parameters.put(PARAM_SDK_TOOLS_DIR, AdtPlugin.getOsSdkToolsFolder());
            parameters.put(PARAM_IS_NEW_PROJECT, Boolean.FALSE);
            parameters.put(PARAM_SRC_FOLDER, SdkConstants.FD_SOURCES);

            parameters.put(PARAM_SDK_TARGET, p.getTarget());

            // TODO: Find out if these end up getting used in the import-path through the code!
            parameters.put(PARAM_MIN_SDK_VERSION, minSdk);
            parameters.put(PARAM_APPLICATION, STRING_RSRC_PREFIX + STRING_APP_NAME);
            final HashMap<String, String> dictionary = new HashMap<String, String>();
            dictionary.put(STRING_APP_NAME, mValues.applicationName);

            if (mValues.copyIntoWorkspace) {
                parameters.put(PARAM_SOURCE, p.getLocation());

                // TODO: Make sure it isn't *already* in the workspace!
                //IPath defaultLocation = Platform.getLocation();
                //if ((!mValues.useDefaultLocation || mValues.useExisting)
                //        && !defaultLocation.isPrefixOf(path)) {
                //IPath workspaceLocation = Platform.getLocation().append(projectName);
                //description.setLocation(workspaceLocation);
                // DON'T SET THE LOCATION: It's IMPLIED and in fact it will generate
                // an error if you set it!
            } else {
                // Create in place
                description.setLocation(new Path(p.getLocation().getPath()));
            }

            projectData.add(new ProjectInfo(project, description, parameters, dictionary));
        }

        // Create a monitored operation to create the actual project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
                createProjectAsync(monitor, null, null, projectData, true);
            }
        };

        // Run the operation in a different thread
        runAsyncOperation(op);
        return true;
    }

    /**
     * Collects all the parameters needed to create the main project.
     * @return A new {@link ProjectInfo} on success. Returns null if the project cannot be
     *    created because parameters are incorrect or should not be created because there
     *    is no main page.
     */
    private ProjectInfo collectMainPageInfo() {
        if (mValues.mode == Mode.TEST) {
            return null;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject project = workspace.getRoot().getProject(mValues.projectName);
        final IProjectDescription description = workspace.newProjectDescription(project.getName());

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_PROJECT, mValues.projectName);
        parameters.put(PARAM_PACKAGE, mValues.packageName);
        parameters.put(PARAM_APPLICATION, STRING_RSRC_PREFIX + STRING_APP_NAME);
        parameters.put(PARAM_SDK_TOOLS_DIR, AdtPlugin.getOsSdkToolsFolder());
        parameters.put(PARAM_IS_NEW_PROJECT, mValues.mode == Mode.ANY && !mValues.useExisting);
        parameters.put(PARAM_SAMPLE_LOCATION, mValues.chosenSample);
        parameters.put(PARAM_SRC_FOLDER, mValues.sourceFolder);
        parameters.put(PARAM_SDK_TARGET, mValues.target);
        parameters.put(PARAM_MIN_SDK_VERSION, mValues.minSdk);

        if (mValues.createActivity) {
            parameters.put(PARAM_ACTIVITY, mValues.activityName);
        }

        // create a dictionary of string that will contain name+content.
        // we'll put all the strings into values/strings.xml
        final HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put(STRING_APP_NAME, mValues.applicationName);

        IPath path = new Path(mValues.projectLocation.getPath());
        IPath defaultLocation = Platform.getLocation();
        if ((!mValues.useDefaultLocation || mValues.useExisting)
                && !defaultLocation.isPrefixOf(path)) {
            description.setLocation(path);
        }

        if (mValues.mode == Mode.ANY && !mValues.useExisting && !mValues.useDefaultLocation &&
                !validateNewProjectLocationIsEmpty(path)) {
            return null;
        }

        return new ProjectInfo(project, description, parameters, dictionary);
    }

    /**
     * Collects all the parameters needed to create the test project.
     *
     * @return A new {@link ProjectInfo} on success. Returns null if the project cannot be
     *    created because parameters are incorrect or should not be created because there
     *    is no test page.
     */
    private ProjectInfo collectTestPageInfo() {
        if (mValues.mode != Mode.TEST && !mValues.createPairProject) {
            return null;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        String projectName =
                mValues.mode == Mode.TEST ? mValues.projectName : mValues.testProjectName;
        final IProject project = workspace.getRoot().getProject(projectName);
        final IProjectDescription description = workspace.newProjectDescription(project.getName());

        final Map<String, Object> parameters = new HashMap<String, Object>();

        String pkg =
                mValues.mode == Mode.TEST ? mValues.packageName : mValues.testPackageName;

        parameters.put(PARAM_PACKAGE, pkg);
        parameters.put(PARAM_APPLICATION, STRING_RSRC_PREFIX + STRING_APP_NAME);
        parameters.put(PARAM_SDK_TOOLS_DIR, AdtPlugin.getOsSdkToolsFolder());
        parameters.put(PARAM_IS_NEW_PROJECT, !mValues.useExisting);
        parameters.put(PARAM_SRC_FOLDER, mValues.sourceFolder);
        parameters.put(PARAM_SDK_TARGET, mValues.target);
        parameters.put(PARAM_MIN_SDK_VERSION, mValues.minSdk);

        // Test-specific parameters
        String testedPkg = mValues.createPairProject
                ? mValues.packageName : mValues.testTargetPackageName;
        if (testedPkg == null) {
            assert mValues.testingSelf;
            testedPkg = pkg;
        }

        parameters.put(PARAM_TEST_TARGET_PACKAGE, testedPkg);

        if (mValues.testingSelf) {
            parameters.put(PARAM_TARGET_SELF, true);
        } else {
            parameters.put(PARAM_TARGET_EXISTING, true);
            parameters.put(PARAM_REFERENCE_PROJECT, mValues.testedProject);
        }

        if (mValues.createPairProject) {
            parameters.put(PARAM_TARGET_MAIN, true);
        }

        // create a dictionary of string that will contain name+content.
        // we'll put all the strings into values/strings.xml
        final HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put(STRING_APP_NAME, mValues.testApplicationName);

        // Use the same logic to determine test project location as in
        // ApplicationInfoPage#validateTestProjectLocation
        IPath path = new Path(mValues.projectLocation.getPath());
        path = path.removeLastSegments(1).append(mValues.testProjectName);
        IPath defaultLocation = Platform.getLocation();
        if ((!mValues.useDefaultLocation || mValues.useExisting)
                && !path.equals(defaultLocation)) {
            description.setLocation(path);
        }

        if (!mValues.useExisting && !mValues.useDefaultLocation &&
                !validateNewProjectLocationIsEmpty(path)) {
            return null;
        }

        return new ProjectInfo(project, description, parameters, dictionary);
    }

    /**
     * Runs the operation in a different thread and display generated
     * exceptions.
     *
     * @param op The asynchronous operation to run.
     */
    private void runAsyncOperation(WorkspaceModifyOperation op) {
        try {
            mRunnableContext.run(true /* fork */, true /* cancelable */, op);
        } catch (InvocationTargetException e) {

            AdtPlugin.log(e, "New Project Wizard failed");

            // The runnable threw an exception
            Throwable t = e.getTargetException();
            if (t instanceof CoreException) {
                CoreException core = (CoreException) t;
                if (core.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
                    // The error indicates the file system is not case sensitive
                    // and there's a resource with a similar name.
                    MessageDialog.openError(AdtPlugin.getShell(),
                            "Error", "Error: Case Variant Exists");
                } else {
                    ErrorDialog.openError(AdtPlugin.getShell(),
                            "Error", core.getMessage(), core.getStatus());
                }
            } else {
                // Some other kind of exception
                String msg = t.getMessage();
                Throwable t1 = t;
                while (msg == null && t1.getCause() != null) {
                    msg = t1.getMessage();
                    t1 = t1.getCause();
                }
                if (msg == null) {
                    msg = t.toString();
                }
                MessageDialog.openError(AdtPlugin.getShell(), "Error", msg);
            }
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the actual project(s). This is run asynchronously in a different thread.
     *
     * @param monitor An existing monitor.
     * @param mainData Data for main project. Can be null.
     * @param isAndroidProject true if the project is to be set up as a full Android project; false
     * for a plain Java project.
     * @throws InvocationTargetException to wrap any unmanaged exception and
     *         return it to the calling thread. The method can fail if it fails
     *         to create or modify the project or if it is canceled by the user.
     */
    private void createProjectAsync(IProgressMonitor monitor,
            ProjectInfo mainData,
            ProjectInfo testData,
            List<ProjectInfo> importData,
            boolean isAndroidProject)
                throws InvocationTargetException {
        monitor.beginTask("Create Android Project", 100);
        try {
            IProject mainProject = null;

            if (mainData != null) {
                mainProject = createEclipseProject(
                        new SubProgressMonitor(monitor, 50),
                        mainData.getProject(),
                        mainData.getDescription(),
                        mainData.getParameters(),
                        mainData.getDictionary(),
                        null,
                        isAndroidProject);

                if (mainProject != null) {
                    final IJavaProject javaProject = JavaCore.create(mainProject);
                    Display.getDefault().syncExec(new WorksetAdder(javaProject,
                            mValues.workingSets));
                }
            }

            if (testData != null) {
                Map<String, Object> parameters = testData.getParameters();
                if (parameters.containsKey(PARAM_TARGET_MAIN) && mainProject != null) {
                    parameters.put(PARAM_REFERENCE_PROJECT, mainProject);
                }

                IProject testProject = createEclipseProject(
                        new SubProgressMonitor(monitor, 50),
                        testData.getProject(),
                        testData.getDescription(),
                        parameters,
                        testData.getDictionary(),
                        null,
                        isAndroidProject);
                if (testProject != null) {
                    final IJavaProject javaProject = JavaCore.create(testProject);
                    Display.getDefault().syncExec(new WorksetAdder(javaProject,
                            mValues.workingSets));
                }
            }

            if (importData != null) {
                for (final ProjectInfo data : importData) {
                    ProjectPopulator projectPopulator = null;
                    if (mValues.copyIntoWorkspace) {
                        projectPopulator = new ProjectPopulator() {
                            @Override
                            public void populate(IProject project) {
                                // Copy
                                IFileSystem fileSystem = EFS.getLocalFileSystem();
                                File source = (File) data.getParameters().get(PARAM_SOURCE);
                                IFileStore sourceDir = new ReadWriteFileStore(
                                        fileSystem.getStore(source.toURI()));
                                IFileStore destDir = new ReadWriteFileStore(
                                        fileSystem.getStore(AdtUtils.getAbsolutePath(project)));
                                try {
                                    sourceDir.copy(destDir, EFS.OVERWRITE, null);
                                } catch (CoreException e) {
                                    AdtPlugin.log(e, null);
                                }
                            }
                        };
                    }
                    IProject project = createEclipseProject(
                            new SubProgressMonitor(monitor, 50),
                            data.getProject(),
                            data.getDescription(),
                            data.getParameters(),
                            data.getDictionary(),
                            projectPopulator,
                            isAndroidProject);
                    if (project != null) {
                        final IJavaProject javaProject = JavaCore.create(project);
                        Display.getDefault().syncExec(new WorksetAdder(javaProject,
                                mValues.workingSets));
                        ProjectHelper.enforcePreferredCompilerCompliance(javaProject);
                    }
                }
            }
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } catch (StreamException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }

    /** Handler which can write contents into a project */
    public interface ProjectPopulator {
        /**
         * Add contents into the given project
         *
         * @param project the project to write into
         * @throws InvocationTargetException if anything goes wrong
         */
        public void populate(IProject project) throws InvocationTargetException;
    }

    /**
     * Creates the actual project, sets its nature and adds the required folders
     * and files to it. This is run asynchronously in a different thread.
     *
     * @param monitor An existing monitor.
     * @param project The project to create.
     * @param description A description of the project.
     * @param parameters Template parameters.
     * @param dictionary String definition.
     * @param isAndroidProject true if the project is to be set up as a full Android project; false
     * for a plain Java project.
     * @return The project newly created
     * @throws StreamException
     */
    private IProject createEclipseProject(
            @NonNull IProgressMonitor monitor,
            @NonNull IProject project,
            @NonNull IProjectDescription description,
            @NonNull Map<String, Object> parameters,
            @Nullable Map<String, String> dictionary,
            @Nullable ProjectPopulator projectPopulator,
            boolean isAndroidProject)
                throws CoreException, IOException, StreamException {

        // get the project target
        IAndroidTarget target = (IAndroidTarget) parameters.get(PARAM_SDK_TARGET);
        boolean legacy = isAndroidProject && target.getVersion().getApiLevel() < 4;

        // Create project and open it
        project.create(description, new SubProgressMonitor(monitor, 10));
        if (monitor.isCanceled()) throw new OperationCanceledException();

        project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 10));

        // Add the Java and android nature to the project
        AndroidNature.setupProjectNatures(project, monitor, isAndroidProject);

        // Create folders in the project if they don't already exist
        addDefaultDirectories(project, AdtConstants.WS_ROOT, DEFAULT_DIRECTORIES, monitor);
        String[] sourceFolders;
        if (isAndroidProject) {
            sourceFolders = new String[] {
                    (String) parameters.get(PARAM_SRC_FOLDER),
                    GEN_SRC_DIRECTORY
                };
        } else {
            sourceFolders = new String[] {
                    (String) parameters.get(PARAM_SRC_FOLDER)
                };
        }
        addDefaultDirectories(project, AdtConstants.WS_ROOT, sourceFolders, monitor);

        // Create the resource folders in the project if they don't already exist.
        if (legacy) {
            addDefaultDirectories(project, RES_DIRECTORY, RES_DIRECTORIES, monitor);
        } else {
            addDefaultDirectories(project, RES_DIRECTORY, RES_DENSITY_ENABLED_DIRECTORIES, monitor);
        }

        if (projectPopulator != null) {
            try {
                projectPopulator.populate(project);
            } catch (InvocationTargetException ite) {
                AdtPlugin.log(ite, null);
            }
        }

        // Setup class path: mark folders as source folders
        IJavaProject javaProject = JavaCore.create(project);
        setupSourceFolders(javaProject, sourceFolders, monitor);

        if (((Boolean) parameters.get(PARAM_IS_NEW_PROJECT)).booleanValue()) {
            // Create files in the project if they don't already exist
            addManifest(project, parameters, dictionary, monitor);

            // add the default app icon
            addIcon(project, legacy, monitor);

            // Create the default package components
            addSampleCode(project, sourceFolders[0], parameters, dictionary, monitor);

            // add the string definition file if needed
            if (dictionary != null && dictionary.size() > 0) {
                addStringDictionaryFile(project, dictionary, monitor);
            }

            // add the default proguard config
            File libFolder = new File((String) parameters.get(PARAM_SDK_TOOLS_DIR),
                    SdkConstants.FD_LIB);
            addLocalFile(project,
                    new File(libFolder, SdkConstants.FN_PROJECT_PROGUARD_FILE),
                    // Write ProGuard config files with the extension .pro which
                    // is what is used in the ProGuard documentation and samples
                    SdkConstants.FN_PROJECT_PROGUARD_FILE,
                    monitor);

            // Set output location
            javaProject.setOutputLocation(project.getFolder(BIN_CLASSES_DIRECTORY).getFullPath(),
                    monitor);
        }

        File sampleDir = (File) parameters.get(PARAM_SAMPLE_LOCATION);
        if (sampleDir != null) {
            // Copy project
            copySampleCode(project, sampleDir, parameters, dictionary, monitor);
        }

        // Create the reference to the target project
        if (parameters.containsKey(PARAM_REFERENCE_PROJECT)) {
            IProject refProject = (IProject) parameters.get(PARAM_REFERENCE_PROJECT);
            if (refProject != null) {
                IProjectDescription desc = project.getDescription();

                // Add out reference to the existing project reference.
                // We just created a project with no references so we don't need to expand
                // the currently-empty current list.
                desc.setReferencedProjects(new IProject[] { refProject });

                project.setDescription(desc, IResource.KEEP_HISTORY,
                        new SubProgressMonitor(monitor, 10));

                IClasspathEntry entry = JavaCore.newProjectEntry(
                        refProject.getFullPath(), //path
                        new IAccessRule[0], //accessRules
                        false, //combineAccessRules
                        new IClasspathAttribute[0], //extraAttributes
                        false //isExported

                );
                ProjectHelper.addEntryToClasspath(javaProject, entry);
            }
        }

        if (isAndroidProject) {
            Sdk.getCurrent().initProject(project, target);
        }

        // Fix the project to make sure all properties are as expected.
        // Necessary for existing projects and good for new ones to.
        ProjectHelper.fixProject(project);

        Boolean isLibraryProject = (Boolean) parameters.get(PARAM_IS_LIBRARY);
        if (isLibraryProject != null && isLibraryProject.booleanValue()
                && Sdk.getCurrent() != null && project.isOpen()) {
            ProjectState state = Sdk.getProjectState(project);
            if (state != null) {
                // make a working copy of the properties
                ProjectPropertiesWorkingCopy properties =
                        state.getProperties().makeWorkingCopy();

                properties.setProperty(PROPERTY_LIBRARY, Boolean.TRUE.toString());
                try {
                    properties.save();
                    IResource projectProp = project.findMember(FN_PROJECT_PROPERTIES);
                    if (projectProp != null) {
                        projectProp.refreshLocal(DEPTH_ZERO, new NullProgressMonitor());
                    }
                } catch (Exception e) {
                    String msg = String.format(
                            "Failed to save %1$s for project %2$s",
                            SdkConstants.FN_PROJECT_PROPERTIES, project.getName());
                    AdtPlugin.log(e, msg);
                }
            }
        }

        return project;
    }

    /**
     * Creates a new project
     *
     * @param monitor An existing monitor.
     * @param project The project to create.
     * @param target the build target to associate with the project
     * @param projectPopulator a handler for writing the template contents
     * @param isLibrary whether this project should be marked as a library project
     * @param projectLocation the location to write the project into
     * @param workingSets Eclipse working sets, if any, to add the project to
     * @throws CoreException if anything goes wrong
     */
    public static void create(
            @NonNull IProgressMonitor monitor,
            @NonNull final IProject project,
            @NonNull IAndroidTarget target,
            @Nullable final ProjectPopulator projectPopulator,
            boolean isLibrary,
            @NonNull String projectLocation,
            @NonNull final IWorkingSet[] workingSets)
                throws CoreException {
        final NewProjectCreator creator = new NewProjectCreator(null, null);

        final Map<String, String> dictionary = null;
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_SDK_TARGET, target);
        parameters.put(PARAM_SRC_FOLDER, SdkConstants.FD_SOURCES);
        parameters.put(PARAM_IS_NEW_PROJECT, false);
        parameters.put(PARAM_SAMPLE_LOCATION, null);
        parameters.put(PARAM_IS_LIBRARY, isLibrary);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription description = workspace.newProjectDescription(project.getName());

        if (projectLocation != null) {
            IPath path = new Path(projectLocation);
            IPath parent = new Path(path.toFile().getParent());
            IPath workspaceLocation = Platform.getLocation();
            if (!workspaceLocation.equals(parent)) {
                description.setLocation(path);
            }
        }

        IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor submonitor) throws CoreException {
                try {
                    creator.createEclipseProject(submonitor, project, description, parameters,
                            dictionary, projectPopulator, true);
                } catch (IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                            "Unexpected error while creating project", e));
                } catch (StreamException e) {
                    throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                            "Unexpected error while creating project", e));
                }
                if (workingSets != null && workingSets.length > 0) {
                    IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
                    if (javaProject != null) {
                        Display.getDefault().syncExec(new WorksetAdder(javaProject,
                                workingSets));
                    }
                }
            }
        };

        ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
    }

    /**
     * Adds default directories to the project.
     *
     * @param project The Java Project to update.
     * @param parentFolder The path of the parent folder. Must end with a
     *        separator.
     * @param folders Folders to be added.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to create the directories in
     *         the project.
     */
    private void addDefaultDirectories(IProject project, String parentFolder,
            String[] folders, IProgressMonitor monitor) throws CoreException {
        for (String name : folders) {
            if (name.length() > 0) {
                IFolder folder = project.getFolder(parentFolder + name);
                if (!folder.exists()) {
                    folder.create(true /* force */, true /* local */,
                            new SubProgressMonitor(monitor, 10));
                }
            }
        }
    }

    /**
     * Adds the manifest to the project.
     *
     * @param project The Java Project to update.
     * @param parameters Template Parameters.
     * @param dictionary String List to be added to a string definition
     *        file. This map will be filled by this method.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addManifest(IProject project, Map<String, Object> parameters,
            Map<String, String> dictionary, IProgressMonitor monitor)
            throws CoreException, IOException {

        // get IFile to the manifest and check if it's not already there.
        IFile file = project.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (!file.exists()) {

            // Read manifest template
            String manifestTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_MANIFEST);

            // Replace all keyword parameters
            manifestTemplate = replaceParameters(manifestTemplate, parameters);

            if (manifestTemplate == null) {
                // Inform the user there will be not manifest.
                AdtPlugin.logAndPrintError(null, "Create Project" /*TAG*/,
                        "Failed to generate the Android manifest. Missing template %s",
                        TEMPLATE_MANIFEST);
                // Abort now, there's no need to continue
                return;
            }

            if (parameters.containsKey(PARAM_ACTIVITY)) {
                // now get the activity template
                String activityTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_ACTIVITIES);

                // If the activity name doesn't contain any dot, it's in the form
                // "ClassName" and we need to expand it to ".ClassName" in the XML.
                String name = (String) parameters.get(PARAM_ACTIVITY);
                if (name.indexOf('.') == -1) {
                    // Duplicate the parameters map to avoid changing the caller
                    parameters = new HashMap<String, Object>(parameters);
                    parameters.put(PARAM_ACTIVITY, "." + name); //$NON-NLS-1$
                }

                // Replace all keyword parameters to make main activity.
                String activities = replaceParameters(activityTemplate, parameters);

                // set the intent.
                String intent = AdtPlugin.readEmbeddedTextFile(TEMPLATE_INTENT_LAUNCHER);

                if (activities != null) {
                    if (intent != null) {
                        // set the intent to the main activity
                        activities = activities.replaceAll(PH_INTENT_FILTERS, intent);
                    }

                    // set the activity(ies) in the manifest
                    manifestTemplate = manifestTemplate.replaceAll(PH_ACTIVITIES, activities);
                }
            } else {
                // remove the activity(ies) from the manifest
                manifestTemplate = manifestTemplate.replaceAll(PH_ACTIVITIES, "");  //$NON-NLS-1$
            }

            // Handle the case of the test projects
            if (parameters.containsKey(PARAM_TEST_TARGET_PACKAGE)) {
                // Set the uses-library needed by the test project
                String usesLibrary = AdtPlugin.readEmbeddedTextFile(TEMPLATE_TEST_USES_LIBRARY);
                if (usesLibrary != null) {
                    manifestTemplate = manifestTemplate.replaceAll(
                            PH_TEST_USES_LIBRARY, usesLibrary);
                }

                // Set the instrumentation element needed by the test project
                String instru = AdtPlugin.readEmbeddedTextFile(TEMPLATE_TEST_INSTRUMENTATION);
                if (instru != null) {
                    manifestTemplate = manifestTemplate.replaceAll(
                            PH_TEST_INSTRUMENTATION, instru);
                }

                // Replace PARAM_TEST_TARGET_PACKAGE itself now
                manifestTemplate = replaceParameters(manifestTemplate, parameters);

            } else {
                // remove the unused entries
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_USES_LIBRARY, "");     //$NON-NLS-1$
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_INSTRUMENTATION, "");  //$NON-NLS-1$
            }

            String minSdkVersion = (String) parameters.get(PARAM_MIN_SDK_VERSION);
            if (minSdkVersion != null && minSdkVersion.length() > 0) {
                String usesSdkTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_USES_SDK);
                if (usesSdkTemplate != null) {
                    String usesSdk = replaceParameters(usesSdkTemplate, parameters);
                    manifestTemplate = manifestTemplate.replaceAll(PH_USES_SDK, usesSdk);
                }
            } else {
                manifestTemplate = manifestTemplate.replaceAll(PH_USES_SDK, "");
            }

            // Reformat the file according to the user's formatting settings
            manifestTemplate = reformat(XmlFormatStyle.MANIFEST, manifestTemplate);

            // Save in the project as UTF-8
            InputStream stream = new ByteArrayInputStream(
                    manifestTemplate.getBytes("UTF-8")); //$NON-NLS-1$
            file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }

    /**
     * Adds the string resource file.
     *
     * @param project The Java Project to update.
     * @param strings The list of strings to be added to the string file.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addStringDictionaryFile(IProject project,
            Map<String, String> strings, IProgressMonitor monitor)
            throws CoreException, IOException {

        // create the IFile object and check if the file doesn't already exist.
        IFile file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                                     + VALUES_DIRECTORY + AdtConstants.WS_SEP + STRINGS_FILE);
        if (!file.exists()) {
            // get the Strings.xml template
            String stringDefinitionTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_STRINGS);

            // get the template for one string
            String stringTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_STRING);

            // get all the string names
            Set<String> stringNames = strings.keySet();

            // loop on it and create the string definitions
            StringBuilder stringNodes = new StringBuilder();
            for (String key : stringNames) {
                // get the value from the key
                String value = strings.get(key);

                // Escape values if necessary
                value = ValueXmlHelper.escapeResourceString(value);

                // place them in the template
                String stringDef = stringTemplate.replace(PARAM_STRING_NAME, key);
                stringDef = stringDef.replace(PARAM_STRING_CONTENT, value);

                // append to the other string
                if (stringNodes.length() > 0) {
                    stringNodes.append('\n');
                }
                stringNodes.append(stringDef);
            }

            // put the string nodes in the Strings.xml template
            stringDefinitionTemplate = stringDefinitionTemplate.replace(PH_STRINGS,
                                                                        stringNodes.toString());

            // reformat the file according to the user's formatting settings
            stringDefinitionTemplate = reformat(XmlFormatStyle.RESOURCE, stringDefinitionTemplate);

            // write the file as UTF-8
            InputStream stream = new ByteArrayInputStream(
                    stringDefinitionTemplate.getBytes("UTF-8")); //$NON-NLS-1$
            file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }

    /** Reformats the given contents with the current formatting settings */
    private String reformat(XmlFormatStyle style, String contents) {
        if (AdtPrefs.getPrefs().getUseCustomXmlFormatter()) {
            EclipseXmlFormatPreferences formatPrefs = EclipseXmlFormatPreferences.create();
            return EclipseXmlPrettyPrinter.prettyPrint(contents, formatPrefs, style,
                    null /*lineSeparator*/);
        } else {
            return contents;
        }
    }

    /**
     * Adds default application icon to the project.
     *
     * @param project The Java Project to update.
     * @param legacy whether we're running in legacy mode (no density support)
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     */
    private void addIcon(IProject project, boolean legacy, IProgressMonitor monitor)
            throws CoreException {
        if (legacy) { // density support
            // do medium density icon only, in the default drawable folder.
            IFile file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                    + DRAWABLE_DIRECTORY + AdtConstants.WS_SEP + PROJECT_ICON);
            if (!file.exists()) {
                addFile(file, AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON_MDPI), monitor);
            }
        } else {
            // do all 4 icons.
            IFile file;

            // extra high density
            file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                    + DRAWABLE_XHDPI_DIRECTORY + AdtConstants.WS_SEP + PROJECT_ICON);
            if (!file.exists()) {
                addFile(file, AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON_XHDPI), monitor);
            }

            // high density
            file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                    + DRAWABLE_HDPI_DIRECTORY + AdtConstants.WS_SEP + PROJECT_ICON);
            if (!file.exists()) {
                addFile(file, AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON_HDPI), monitor);
            }

            // medium density
            file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                    + DRAWABLE_MDPI_DIRECTORY + AdtConstants.WS_SEP + PROJECT_ICON);
            if (!file.exists()) {
                addFile(file, AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON_MDPI), monitor);
            }

            // low density
            file = project.getFile(RES_DIRECTORY + AdtConstants.WS_SEP
                    + DRAWABLE_LDPI_DIRECTORY + AdtConstants.WS_SEP + PROJECT_ICON);
            if (!file.exists()) {
                addFile(file, AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON_LDPI), monitor);
            }
        }
    }

    /**
     * Creates a file from a data source.
     * @param dest the file to write
     * @param source the content of the file.
     * @param monitor the progress monitor
     * @throws CoreException
     */
    private void addFile(IFile dest, byte[] source, IProgressMonitor monitor) throws CoreException {
        if (source != null) {
            // Save in the project
            InputStream stream = new ByteArrayInputStream(source);
            dest.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }

    /**
     * Creates the package folder and copies the sample code in the project.
     *
     * @param project The Java Project to update.
     * @param parameters Template Parameters.
     * @param dictionary String List to be added to a string definition
     *        file. This map will be filled by this method.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addSampleCode(IProject project, String sourceFolder,
            Map<String, Object> parameters, Map<String, String> dictionary,
            IProgressMonitor monitor) throws CoreException, IOException {
        // create the java package directories.
        IFolder pkgFolder = project.getFolder(sourceFolder);
        String packageName = (String) parameters.get(PARAM_PACKAGE);

        // The PARAM_ACTIVITY key will be absent if no activity should be created,
        // in which case activityName will be null.
        String activityName = (String) parameters.get(PARAM_ACTIVITY);

        Map<String, Object> java_activity_parameters = new HashMap<String, Object>(parameters);
        java_activity_parameters.put(PARAM_IMPORT_RESOURCE_CLASS, "");  //$NON-NLS-1$

        if (activityName != null) {

            String resourcePackageClass = null;

            // An activity name can be of the form ".package.Class", ".Class" or FQDN.
            // The initial dot is ignored, as it is always added later in the templates.
            int lastDotIndex = activityName.lastIndexOf('.');

            if (lastDotIndex != -1) {

                // Resource class
                if (lastDotIndex > 0) {
                    resourcePackageClass = packageName + '.' + SdkConstants.FN_RESOURCE_BASE;
                }

                // Package name
                if (activityName.startsWith(".")) {  //$NON-NLS-1$
                    packageName += activityName.substring(0, lastDotIndex);
                } else {
                    packageName = activityName.substring(0, lastDotIndex);
                }

                // Activity Class name
                activityName = activityName.substring(lastDotIndex + 1);
            }

            java_activity_parameters.put(PARAM_ACTIVITY, activityName);
            java_activity_parameters.put(PARAM_PACKAGE, packageName);
            if (resourcePackageClass != null) {
                String importResourceClass = "\nimport " + resourcePackageClass + ";";  //$NON-NLS-1$ // $NON-NLS-2$
                java_activity_parameters.put(PARAM_IMPORT_RESOURCE_CLASS, importResourceClass);
            }
        }

        String[] components = packageName.split(AdtConstants.RE_DOT);
        for (String component : components) {
            pkgFolder = pkgFolder.getFolder(component);
            if (!pkgFolder.exists()) {
                pkgFolder.create(true /* force */, true /* local */,
                        new SubProgressMonitor(monitor, 10));
            }
        }

        if (activityName != null) {
            // create the main activity Java file
            String activityJava = activityName + SdkConstants.DOT_JAVA;
            IFile file = pkgFolder.getFile(activityJava);
            if (!file.exists()) {
                copyFile(JAVA_ACTIVITY_TEMPLATE, file, java_activity_parameters, monitor, false);
            }

            // create the layout file (if we're creating an
            IFolder layoutfolder = project.getFolder(RES_DIRECTORY).getFolder(LAYOUT_DIRECTORY);
            file = layoutfolder.getFile(MAIN_LAYOUT_XML);
            if (!file.exists()) {
                copyFile(LAYOUT_TEMPLATE, file, parameters, monitor, true);
                dictionary.put(STRING_HELLO_WORLD, String.format("Hello World, %1$s!",
                        activityName));
            }
        }
    }

    private void copySampleCode(IProject project, File sampleDir,
            Map<String, Object> parameters, Map<String, String> dictionary,
            IProgressMonitor monitor) throws CoreException {
        // Copy the sampleDir into the project directory recursively
        IFileSystem fileSystem = EFS.getLocalFileSystem();
        IFileStore sourceDir = new ReadWriteFileStore(
                                        fileSystem.getStore(sampleDir.toURI()));
        IFileStore destDir   = new ReadWriteFileStore(
                                        fileSystem.getStore(AdtUtils.getAbsolutePath(project)));
        sourceDir.copy(destDir, EFS.OVERWRITE, null);
    }

    /**
     * In a sample we never duplicate source files as read-only.
     * This creates a store that read files attributes and doesn't set the r-o flag.
     */
    private static class ReadWriteFileStore extends FileStoreAdapter {

        public ReadWriteFileStore(IFileStore store) {
            super(store);
        }

        // Override when reading attributes
        @Override
        public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
            IFileInfo info = super.fetchInfo(options, monitor);
            info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
            return info;
        }

        // Override when writing attributes
        @Override
        public void putInfo(IFileInfo info, int options, IProgressMonitor storeMonitor)
                throws CoreException {
            info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
            super.putInfo(info, options, storeMonitor);
        }

        @Deprecated
        @Override
        public IFileStore getChild(IPath path) {
            IFileStore child = super.getChild(path);
            if (!(child instanceof ReadWriteFileStore)) {
                child = new ReadWriteFileStore(child);
            }
            return child;
        }

        @Override
        public IFileStore getChild(String name) {
            return new ReadWriteFileStore(super.getChild(name));
        }
    }

    /**
     * Adds a file to the root of the project
     * @param project the project to add the file to.
     * @param destName the name to write the file as
     * @param source the file to add. It'll keep the same filename once copied into the project.
     * @param monitor the monitor to report progress to
     * @throws FileNotFoundException if the file to be added does not exist
     * @throws CoreException if writing the file does not work
     */
    public static void addLocalFile(IProject project, File source, String destName,
            IProgressMonitor monitor) throws FileNotFoundException, CoreException {
        IFile dest = project.getFile(destName);
        if (dest.exists() == false) {
            FileInputStream stream = new FileInputStream(source);
            dest.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }

    /**
     * Adds the given folder to the project's class path.
     *
     * @param javaProject The Java Project to update.
     * @param sourceFolders Template Parameters.
     * @param monitor An existing monitor.
     * @throws JavaModelException if the classpath could not be set.
     */
    private void setupSourceFolders(IJavaProject javaProject, String[] sourceFolders,
            IProgressMonitor monitor) throws JavaModelException {
        IProject project = javaProject.getProject();

        // get the list of entries.
        IClasspathEntry[] entries = javaProject.getRawClasspath();

        // remove the project as a source folder (This is the default)
        entries = removeSourceClasspath(entries, project);

        // add the source folders.
        for (String sourceFolder : sourceFolders) {
            IFolder srcFolder = project.getFolder(sourceFolder);

            // remove it first in case.
            entries = removeSourceClasspath(entries, srcFolder);
            entries = ProjectHelper.addEntryToClasspath(entries,
                    JavaCore.newSourceEntry(srcFolder.getFullPath()));
        }

        javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
    }


    /**
     * Removes the corresponding source folder from the class path entries if
     * found.
     *
     * @param entries The class path entries to read. A copy will be returned.
     * @param folder The parent source folder to remove.
     * @return A new class path entries array.
     */
    private IClasspathEntry[] removeSourceClasspath(IClasspathEntry[] entries, IContainer folder) {
        if (folder == null) {
            return entries;
        }
        IClasspathEntry source = JavaCore.newSourceEntry(folder.getFullPath());
        int n = entries.length;
        for (int i = n - 1; i >= 0; i--) {
            if (entries[i].equals(source)) {
                IClasspathEntry[] newEntries = new IClasspathEntry[n - 1];
                if (i > 0) System.arraycopy(entries, 0, newEntries, 0, i);
                if (i < n - 1) System.arraycopy(entries, i + 1, newEntries, i, n - i - 1);
                n--;
                entries = newEntries;
            }
        }
        return entries;
    }


    /**
     * Copies the given file from our resource folder to the new project.
     * Expects the file to the US-ASCII or UTF-8 encoded.
     *
     * @throws CoreException from IFile if failing to create the new file.
     * @throws MalformedURLException from URL if failing to interpret the URL.
     * @throws FileNotFoundException from RandomAccessFile.
     * @throws IOException from RandomAccessFile.length() if can't determine the
     *         length.
     */
    private void copyFile(String resourceFilename, IFile destFile,
            Map<String, Object> parameters, IProgressMonitor monitor, boolean reformat)
            throws CoreException, IOException {

        // Read existing file.
        String template = AdtPlugin.readEmbeddedTextFile(
                TEMPLATES_DIRECTORY + resourceFilename);

        // Replace all keyword parameters
        template = replaceParameters(template, parameters);

        if (reformat) {
            // Guess the formatting style based on the file location
            XmlFormatStyle style = EclipseXmlPrettyPrinter
                    .getForFile(destFile.getProjectRelativePath());
            if (style != null) {
                template = reformat(style, template);
            }
        }

        // Save in the project as UTF-8
        InputStream stream = new ByteArrayInputStream(template.getBytes("UTF-8")); //$NON-NLS-1$
        destFile.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }

    /**
     * Replaces placeholders found in a string with values.
     *
     * @param str the string to search for placeholders.
     * @param parameters a map of <placeholder, Value> to search for in the string
     * @return A new String object with the placeholder replaced by the values.
     */
    private String replaceParameters(String str, Map<String, Object> parameters) {

        if (parameters == null) {
            AdtPlugin.log(IStatus.ERROR,
                    "NPW replace parameters: null parameter map. String: '%s'", str);  //$NON-NLS-1$
            return str;
        } else if (str == null) {
            AdtPlugin.log(IStatus.ERROR,
                    "NPW replace parameters: null template string");  //$NON-NLS-1$
            return str;
        }

        for (Entry<String, Object> entry : parameters.entrySet()) {
            if (entry != null && entry.getValue() instanceof String) {
                Object value = entry.getValue();
                if (value == null) {
                    AdtPlugin.log(IStatus.ERROR,
                    "NPW replace parameters: null value for key '%s' in template '%s'",  //$NON-NLS-1$
                    entry.getKey(),
                    str);
                } else {
                    str = str.replaceAll(entry.getKey(), (String) value);
                }
            }
        }

        return str;
    }

    private static class WorksetAdder implements Runnable {
        private final IJavaProject mProject;
        private final IWorkingSet[] mWorkingSets;

        private WorksetAdder(IJavaProject project, IWorkingSet[] workingSets) {
            mProject = project;
            mWorkingSets = workingSets;
        }

        @Override
        public void run() {
            if (mWorkingSets.length > 0 && mProject != null
                    && mProject.exists()) {
                PlatformUI.getWorkbench().getWorkingSetManager()
                        .addToWorkingSets(mProject, mWorkingSets);
            }
        }
    }
}
