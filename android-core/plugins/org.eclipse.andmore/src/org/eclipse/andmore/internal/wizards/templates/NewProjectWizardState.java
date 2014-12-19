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

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.CATEGORY_PROJECTS;

import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState;
import org.eclipse.ui.IWorkingSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Value object which holds the current state of the wizard pages for the
 * {@link NewProjectWizard}
 */
public class NewProjectWizardState {
    /** Creates a new {@link NewProjectWizardState} */
    public NewProjectWizardState() {
        template = TemplateHandler.createFromName(CATEGORY_PROJECTS,
                "NewAndroidApplication"); //$NON-NLS-1$
    }

    /** The template handler instantiating the project */
    public final TemplateHandler template;

    /** The name of the project */
    public String projectName;

    /** The derived name of the activity, if any */
    public String activityName;

    /** The derived title of the activity, if any */
    public String activityTitle;

    /** The application name */
    public String applicationName;

    /** The package name */
    public String packageName;

    /** Whether the project name has been edited by the user */
    public boolean projectModified;

    /** Whether the package name has been edited by the user */
    public boolean packageModified;

    /** Whether the activity name has been edited by the user */
    public boolean activityNameModified;

    /** Whether the activity title has been edited by the user */
    public boolean activityTitleModified;

    /** Whether the application name has been edited by the user */
    public boolean applicationModified;

    /** The compilation target to use for this project */
    public IAndroidTarget target;

    /** The minimum SDK API level, as a string (if the API is a preview release with a codename) */
    public String minSdk;

    /** The minimum SDK API level to use */
    public int minSdkLevel;

    /** The target SDK level */
    public int targetSdkLevel = AdtUtils.getHighestKnownApiLevel();

    /** Whether this project should be marked as a library project */
    public boolean isLibrary;

    /** Whether to create an activity (if so, the activity state is stored in
     * {@link #activityValues}) */
    public boolean createActivity = true;

    /** Whether a custom icon should be created instead of just reusing the default (if so,
     * the icon wizard state is stored in {@link #iconState}) */
    public boolean createIcon = true;

    // Delegated wizards

    /** State for the asset studio wizard, used to create custom icons */
    public CreateAssetSetWizardState iconState = new CreateAssetSetWizardState();

    /** State for the template wizard, used to embed an activity template */
    public NewTemplateWizardState activityValues = new NewTemplateWizardState();

    /** Whether a custom location should be used */
    public boolean useDefaultLocation = true;

    /** Folder where the project should be created. */
    public String projectLocation;

    /** Configured parameters, by id */
    public final Map<String, Object> parameters = new HashMap<String, Object>();

    /** The set of chosen working sets to use when creating the project */
    public IWorkingSet[] workingSets = new IWorkingSet[0];

    /**
     * Returns the build target API level
     *
     * @return the build target API level
     */
    public int getBuildApi() {
        return target != null ? target.getVersion().getApiLevel() : minSdkLevel;
    }
}
