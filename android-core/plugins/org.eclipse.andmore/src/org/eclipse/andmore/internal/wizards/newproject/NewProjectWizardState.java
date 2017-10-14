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
import com.android.annotations.Nullable;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.utils.Pair;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkingSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The {@link NewProjectWizardState} holds the state used by the various pages
 * in the {@link NewProjectWizard} and its variations, and it can also be used
 * to pass project information to the {@link NewProjectCreator}.
 */
public class NewProjectWizardState {
    /** The mode to run the wizard in: creating test, or sample, or plain project */
    public Mode mode;

    /**
     * If true, the project should be created from an existing codebase (pointed
     * to by the {@link #projectLocation} or in the case of sample projects, the
     * {@link #chosenSample}. Otherwise, create a brand new project from scratch.
     */
    public boolean useExisting;

    /**
     * Whether new projects should be created into the default project location
     * (e.g. in the Eclipse workspace) or not
     */
    public boolean useDefaultLocation = true;

    /** The build target SDK */
    public IAndroidTarget target;
    /** True if the user has manually modified the target */
    public boolean targetModifiedByUser;

    /** The location to store projects into */
    public File projectLocation = new File(Platform.getLocation().toOSString());
    /** True if the project location name has been manually edited by the user */
    public boolean projectLocationModifiedByUser;

    /** The name of the project */
    public String projectName = ""; //$NON-NLS-1$
    /** True if the project name has been manually edited by the user */
    public boolean projectNameModifiedByUser;

    /** The application name */
    public String applicationName;
    /** True if the application name has been manually edited by the user */
    public boolean applicationNameModifiedByUser;

    /** The package path */
    public String packageName;
    /** True if the package name has been manually edited by the user */
    public boolean packageNameModifiedByUser;

    /** True if a new activity should be created */
    public boolean createActivity;

    /** The name of the new activity to be created */
    public String activityName;
    /** True if the activity name has been manually edited by the user */
    public boolean activityNameModifiedByUser;

    /** The minimum SDK version to use with the project (may be null or blank) */
    public String minSdk;
    /** True if the minimum SDK version has been manually edited by the user */
    public boolean minSdkModifiedByUser;
    /**
     * A list of paths to each of the available samples for the current SDK.
     * The pair is (String: sample display name => File: sample directory).
     * Note we want a list, not a map since we might have duplicates.
     * */
    public List<Pair<String, File>> samples = new ArrayList<Pair<String, File>>();
    /** Path to the currently chosen sample */
    public File chosenSample;

    /** The name of the source folder, relative to the project root */
    public String sourceFolder = SdkConstants.FD_SOURCES;
    /** The set of chosen working sets to use when creating the project */
    public IWorkingSet[] workingSets = new IWorkingSet[0];

    /**
     * A reference to a different project that the current test project will be
     * testing.
     */
    public IProject testedProject;
    /**
     * If true, this test project should be testing itself, otherwise it will be
     * testing the project pointed to by {@link #testedProject}.
     */
    public boolean testingSelf;

    // NOTE: These apply only to creating paired projects; when isTest is true
    // we're using
    // the normal fields above
    /**
     * If true, create a test project along with this plain project which will
     * be testing the plain project. (This flag only applies when creating
     * normal projects.)
     */
    public boolean createPairProject;
    /**
     * The application name of the test application (only applies when
     * {@link #createPairProject} is true)
     */
    public String testApplicationName;
    /**
     * True if the testing application name has been modified by the user (only
     * applies when {@link #createPairProject} is true)
     */
    public boolean testApplicationNameModified;
    /**
     * The package name of the test application (only applies when
     * {@link #createPairProject} is true)
     */
    public String testPackageName;
    /**
     * True if the testing package name has been modified by the user (only
     * applies when {@link #createPairProject} is true)
     */
    public boolean testPackageModified;
    /**
     * The project name of the test project (only applies when
     * {@link #createPairProject} is true)
     */
    public String testProjectName;
    /**
     * True if the testing project name has been modified by the user (only
     * applies when {@link #createPairProject} is true)
     */
    public boolean testProjectModified;
    /** Package name of the tested app */
    public String testTargetPackageName;

    /**
     * Copy project into workspace? This flag only applies when importing
     * projects (creating projects from existing source)
     */
    public boolean copyIntoWorkspace;

    /**
     * List of projects to be imported. Null if not importing projects.
     */
    @Nullable
    public List<ImportedProject> importProjects;

    /**
     * Creates a new {@link NewProjectWizardState}
     *
     * @param mode the mode to run the wizard in
     */
    public NewProjectWizardState(Mode mode) {
        this.mode = mode;
        if (mode == Mode.SAMPLE) {
            useExisting = true;
        } else if (mode == Mode.TEST) {
            createActivity = false;
        }
    }

    /**
     * Extract information (package name, application name, minimum SDK etc) from
     * the given Android project.
     *
     * @param path the path to the project to extract information from
     */
    public void extractFromAndroidManifest(Path path) {
        String osPath = path.append(SdkConstants.FN_ANDROID_MANIFEST_XML).toOSString();
        if (!(new File(osPath).exists())) {
            return;
        }

        ManifestData manifestData = AndroidManifestHelper.parseForData(osPath);
        if (manifestData == null) {
            return;
        }

        String newPackageName = null;
        Activity activity = null;
        String newActivityName = null;
        String minSdkVersion = null;
        try {
            newPackageName = manifestData.getPackage();
            minSdkVersion = manifestData.getMinSdkVersionString();

            // try to get the first launcher activity. If none, just take the first activity.
            activity = manifestData.getLauncherActivity();
            if (activity == null) {
                Activity[] activities = manifestData.getActivities();
                if (activities != null && activities.length > 0) {
                    activity = activities[0];
                }
            }
        } catch (Exception e) {
            // ignore exceptions
        }

        if (newPackageName != null && newPackageName.length() > 0) {
            packageName = newPackageName;
        }

        if (activity != null) {
            newActivityName = AndroidManifest.extractActivityName(activity.getName(),
                    newPackageName);
        }

        if (newActivityName != null && newActivityName.length() > 0) {
            activityName = newActivityName;
            // we are "importing" an existing activity, not creating a new one
            createActivity = false;

            // If project name and application names are empty, use the activity
            // name as a default. If the activity name has dots, it's a part of a
            // package specification and only the last identifier must be used.
            if (newActivityName.indexOf('.') != -1) {
                String[] ids = newActivityName.split(AndmoreAndroidConstants.RE_DOT);
                newActivityName = ids[ids.length - 1];
            }
            if (projectName == null || projectName.length() == 0 ||
                    !projectNameModifiedByUser) {
                projectName = newActivityName;
                projectNameModifiedByUser = false;
            }
            if (applicationName == null || applicationName.length() == 0 ||
                    !applicationNameModifiedByUser) {
                applicationNameModifiedByUser = false;
                applicationName = newActivityName;
            }
        } else {
            activityName = ""; //$NON-NLS-1$

            // There is no activity name to use to fill in the project and application
            // name. However if there's a package name, we can use this as a base.
            if (newPackageName != null && newPackageName.length() > 0) {
                // Package name is a java identifier, so it's most suitable for
                // an application name.

                if (applicationName == null || applicationName.length() == 0 ||
                        !applicationNameModifiedByUser) {
                    applicationName = newPackageName;
                }

                // For the project name, remove any dots
                newPackageName = newPackageName.replace('.', '_');
                if (projectName == null || projectName.length() == 0 ||
                        !projectNameModifiedByUser) {
                    projectName = newPackageName;
                }

            }
        }

        if (mode == Mode.ANY && useExisting) {
            updateSdkTargetToMatchProject(path.toFile());
        }

        minSdk = minSdkVersion;
        minSdkModifiedByUser = false;
    }

    /**
     * Try to find an SDK Target that matches the current MinSdkVersion.
     *
     * There can be multiple targets with the same sdk api version, so don't change
     * it if it's already at the right version. Otherwise pick the first target
     * that matches.
     */
    public void updateSdkTargetToMatchMinSdkVersion() {
        IAndroidTarget currentTarget = target;
        if (currentTarget != null && currentTarget.getVersion().equals(minSdk)) {
            return;
        }

        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            for (IAndroidTarget t : sdk.getTargets()) {
                if (t.getVersion().equals(minSdk)) {
                    target = t;
                    return;
                }
            }
        }
    }

    /**
     * Updates the SDK to reflect the SDK required by the project at the given
     * location
     *
     * @param location the location of the project
     */
    public void updateSdkTargetToMatchProject(File location) {
        // Select the target matching the manifest's sdk or build properties, if any
        IAndroidTarget foundTarget = null;
        // This is the target currently in the UI
        IAndroidTarget currentTarget = target;
        String projectPath = location.getPath();

        // If there's a current target defined, we do not allow to change it when
        // operating in the create-from-sample mode -- since the available sample list
        // is tied to the current target, so changing it would invalidate the project we're
        // trying to load in the first place.
        if (!targetModifiedByUser) {
            ProjectProperties p = ProjectProperties.load(projectPath,
                    PropertyType.PROJECT);
            if (p != null) {
                String v = p.getProperty(ProjectProperties.PROPERTY_TARGET);
                IAndroidTarget desiredTarget = Sdk.getCurrent().getTargetFromHashString(v);
                // We can change the current target if:
                // - we found a new desired target
                // - there is no current target
                // - or the current target can't run the desired target
                if (desiredTarget != null &&
                        (currentTarget == null || !desiredTarget.canRunOn(currentTarget))) {
                    foundTarget = desiredTarget;
                }
            }

            Sdk sdk = Sdk.getCurrent();
            Collection<IAndroidTarget> targets = sdk == null ? Collections.<IAndroidTarget>emptySet() : sdk.getTargets();

            if (foundTarget == null && minSdk != null) {
                // Otherwise try to match the requested min-sdk-version if we find an
                // exact match, regardless of the currently selected target.
                for (IAndroidTarget existingTarget : targets) {
                    if (existingTarget != null &&
                            existingTarget.getVersion().equals(minSdk)) {
                        foundTarget = existingTarget;
                        break;
                    }
                }
            }

            if (foundTarget == null) {
                // Or last attempt, try to match a sample project location and use it
                // if we find an exact match, regardless of the currently selected target.
                for (IAndroidTarget existingTarget : targets) {
                    if (existingTarget != null &&
                            projectPath.startsWith(existingTarget.getLocation())) {
                        foundTarget = existingTarget;
                        break;
                    }
                }
            }
        }

        if (foundTarget != null) {
            target = foundTarget;
        }
    }

    /**
     * Type of project being offered/created by the wizard
     */
    public enum Mode {
        /** Create a sample project. Testing options are not presented. */
        SAMPLE,

        /**
         * Create a test project, either testing itself or some other project.
         * Note that even if in the {@link #ANY} mode, a test project can be
         * created as a *paired* project with the main project, so this flag
         * only means that we are creating *just* a test project
         */
        TEST,

        /**
         * Create an Android project, which can be a plain project, optionally
         * with a paired test project, or a sample project (the first page
         * contains toggles for choosing which
         */
        ANY;
    }
}
