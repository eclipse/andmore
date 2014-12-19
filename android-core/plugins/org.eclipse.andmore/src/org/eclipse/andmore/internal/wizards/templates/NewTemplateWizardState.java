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

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_BUILD_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_COPY_ICONS;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API_LEVEL;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_PACKAGE_NAME;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_TARGET_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.IS_LIBRARY_PROJECT;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.IS_NEW_PROJECT;
import static org.eclipse.andmore.internal.wizards.templates.NewTemplateWizard.BLANK_ACTIVITY;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.assetstudio.ConfigureAssetSetPage;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Value object which holds the current state of the wizard pages for the
 * {@link NewTemplateWizard}
 */
public class NewTemplateWizardState {
    /** Template handler responsible for instantiating templates and reading resources */
    private TemplateHandler mTemplateHandler;

    /** Configured parameters, by id */
    public final Map<String, Object> parameters = new HashMap<String, Object>();

    /** Configured defaults for the parameters, by id */
    public final Map<String, String> defaults = new HashMap<String, String>();

    /** Ids for parameters which should be hidden (because the client wizard already
     * has information for these parameters) */
    public final Set<String> hidden = new HashSet<String>();

    /**
     * The chosen project (which may be null if the wizard page is being
     * embedded in the new project wizard)
     */
    public IProject project;

    /** The minimum API level to use for this template */
    public int minSdkLevel;

    /** Location of the template being created */
    private File mTemplateLocation;

    /**
     * State for the asset studio wizard, used to create custom icons provided
     * the icon requests it with an {@code <icons>} element
     */
    private CreateAssetSetWizardState mIconState;

    /**
     * Create a new state object for use by the {@link NewTemplatePage}
     */
    public NewTemplateWizardState() {
        parameters.put(IS_NEW_PROJECT, false);
    }

    @NonNull
    TemplateHandler getTemplateHandler() {
        if (mTemplateHandler == null) {
            File inputPath;
            if (mTemplateLocation != null) {
                inputPath = mTemplateLocation;
            } else {
                // Default
                inputPath = TemplateManager.getTemplateLocation(BLANK_ACTIVITY);
            }
            mTemplateHandler = TemplateHandler.createFromPath(inputPath);
        }

        return mTemplateHandler;
    }

    /** Sets the current template */
    void setTemplateLocation(File file) {
        if (!file.equals(mTemplateLocation)) {
            mTemplateLocation = file;
            mTemplateHandler = null;
        }
    }

    /** Returns the current template */
    File getTemplateLocation() {
        return mTemplateLocation;
    }

    /** Returns the min SDK version to use */
    int getMinSdk() {
        if (project == null) {
            return -1;
        }
        ManifestInfo manifest = ManifestInfo.get(project);
        return manifest.getMinSdkVersion();
    }

    /** Returns the build API version to use */
    int getBuildApi() {
        if (project == null) {
            return -1;
        }
        IAndroidTarget target = Sdk.getCurrent().getTarget(project);
        if (target != null) {
            return target.getVersion().getApiLevel();
        }

        return getMinSdk();
    }

    /** Computes the changes this wizard will make */
    @NonNull
    List<Change> computeChanges() {
        if (project == null) {
            return Collections.emptyList();
        }

        ManifestInfo manifest = ManifestInfo.get(project);
        parameters.put(ATTR_PACKAGE_NAME, manifest.getPackage());
        parameters.put(ATTR_MIN_API, manifest.getMinSdkName());
        parameters.put(ATTR_MIN_API_LEVEL, manifest.getMinSdkVersion());
        parameters.put(ATTR_TARGET_API, manifest.getTargetSdkVersion());
        parameters.put(ATTR_BUILD_API, getBuildApi());
        parameters.put(ATTR_COPY_ICONS, mIconState == null);
        ProjectState projectState = Sdk.getProjectState(project);
        parameters.put(IS_LIBRARY_PROJECT,
                projectState != null ? projectState.isLibrary() : false);

        TemplateHandler.addDirectoryParameters(parameters, project);

        List<Change> changes = getTemplateHandler().render(project, parameters);

        if (mIconState != null) {
            String title = String.format("Generate icons (res/drawable-<density>/%1$s.png)",
                    mIconState.outputName);
            changes.add(new NullChange(title) {
                @Override
                public Change perform(IProgressMonitor pm) throws CoreException {
                    ConfigureAssetSetPage.generateIcons(mIconState.project,
                            mIconState, false, null);

                    // Not undoable: just return null instead of an undo-change.
                    return null;
                }
            });

        }

        return changes;
    }

    @NonNull
    CreateAssetSetWizardState getIconState() {
        if (mIconState == null) {
            TemplateHandler handler = getTemplateHandler();
            if (handler != null) {
                TemplateMetadata template = handler.getTemplate();
                if (template != null) {
                    mIconState = template.getIconState(project);
                }
            }
        }

        return mIconState;
    }

    /**
     * Updates the icon state, such as the output name, based on other parameter settings
     * @param evaluator the string evaluator, or null if none exists
     */
    public void updateIconState(@Nullable StringEvaluator evaluator) {
        TemplateMetadata template = getTemplateHandler().getTemplate();
        if (template != null) {
            if (evaluator == null) {
                evaluator = new StringEvaluator();
            }
            template.updateIconName(template.getParameters(), evaluator);
        }
    }
}
