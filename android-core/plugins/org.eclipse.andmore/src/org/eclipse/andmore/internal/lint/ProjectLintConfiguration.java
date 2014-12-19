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
package org.eclipse.andmore.internal.lint;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.DefaultConfiguration;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import java.io.File;

/** Configuration for Lint in Eclipse projects */
class ProjectLintConfiguration extends DefaultConfiguration {
    private boolean mFatalOnly;

    private final static QualifiedName CONFIGURATION_NAME = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "lintconfig"); //$NON-NLS-1$

    @VisibleForTesting
    ProjectLintConfiguration(LintClient client, Project project,
            Configuration parent, boolean fatalOnly) {
        super(client, project, parent);
        mFatalOnly = fatalOnly;
    }

    private static ProjectLintConfiguration create(LintClient client, IProject project,
            Configuration parent, boolean fatalOnly) {
        File dir = AdtUtils.getAbsolutePath(project).toFile();
        Project lintProject = client.getProject(dir, dir);
        return new ProjectLintConfiguration(client, lintProject, parent, fatalOnly);
    }

    public static ProjectLintConfiguration get(LintClient client, IProject project,
            boolean fatalOnly) {
        // Don't cache fatal-only configurations: they're only used occasionally and typically
        // not repeatedly
        if (fatalOnly) {
            return create(client, project, GlobalLintConfiguration.get(), true);
        }

        ProjectLintConfiguration configuration = null;
        try {
            Object value = project.getSessionProperty(CONFIGURATION_NAME);
            configuration = (ProjectLintConfiguration) value;
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }
        if (configuration == null) {
            configuration = create(client, project, GlobalLintConfiguration.get(), false);
            try {
                project.setSessionProperty(CONFIGURATION_NAME, configuration);
            } catch (CoreException e) {
                AdtPlugin.log(e, "Can't store lint configuration");
            }
        }
        return configuration;
    }

    @Override
    public @NonNull Severity getSeverity(@NonNull Issue issue) {
        Severity severity = super.getSeverity(issue);
        if (mFatalOnly && severity != Severity.FATAL) {
            return Severity.IGNORE;
        }
        return severity;
    }
}