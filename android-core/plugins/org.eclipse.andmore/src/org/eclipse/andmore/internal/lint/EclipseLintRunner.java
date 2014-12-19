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
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.IssueRegistry;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Eclipse implementation for running lint on workspace files and projects.
 */
public class EclipseLintRunner {
    static final String MARKER_CHECKID_PROPERTY = "checkid";    //$NON-NLS-1$

    /**
     * Runs lint and updates the markers, and waits for the result. Returns
     * true if fatal errors were found.
     *
     * @param resources the resources (project, folder or file) to be analyzed
     * @param source if checking a single source file, the source file
     * @param doc the associated document, if known, or null
     * @param fatalOnly if true, only report fatal issues (severity=error)
     * @return true if any fatal errors were encountered.
     */
    private static boolean runLint(
            @NonNull List<? extends IResource> resources,
            @Nullable IResource source,
            @Nullable IDocument doc,
            boolean fatalOnly) {
        resources = addLibraries(resources);
        LintJob job = (LintJob) startLint(resources, source,  doc, fatalOnly,
                false /*show*/);
        try {
            job.join();
            boolean fatal = job.isFatal();

            if (fatal) {
                LintViewPart.show(resources);
            }

            return fatal;
        } catch (InterruptedException e) {
            AdtPlugin.log(e, null);
        }

        return false;
    }

    /**
     * Runs lint and updates the markers. Does not wait for the job to finish -
     * just returns immediately.
     *
     * @param resources the resources (project, folder or file) to be analyzed
     * @param source if checking a single source file, the source file. When
     *            single checking an XML file, this is typically the same as the
     *            file passed in the list in the first parameter, but when
     *            checking the .class files of a Java file for example, the
     *            .class file and all the inner classes of the Java file are
     *            passed in the first parameter, and the corresponding .java
     *            source file is passed here.
     * @param doc the associated document, if known, or null
     * @param fatalOnly if true, only report fatal issues (severity=error)
     * @param show if true, show the results in a {@link LintViewPart}
     * @return the job running lint in the background.
     */
    public static Job startLint(
            @NonNull List<? extends IResource> resources,
            @Nullable IResource source,
            @Nullable IDocument doc,
            boolean fatalOnly,
            boolean show) {
        IssueRegistry registry = EclipseLintClient.getRegistry();
        EclipseLintClient client = new EclipseLintClient(registry, resources, doc, fatalOnly);
        return startLint(client, resources, source, show);
    }

    /**
     * Runs lint and updates the markers. Does not wait for the job to finish -
     * just returns immediately.
     *
     * @param client the lint client receiving issue reports etc
     * @param resources the resources (project, folder or file) to be analyzed
     * @param source if checking a single source file, the source file. When
     *            single checking an XML file, this is typically the same as the
     *            file passed in the list in the first parameter, but when
     *            checking the .class files of a Java file for example, the
     *            .class file and all the inner classes of the Java file are
     *            passed in the first parameter, and the corresponding .java
     *            source file is passed here.
     * @param show if true, show the results in a {@link LintViewPart}
     * @return the job running lint in the background.
     */
    public static Job startLint(
            @NonNull EclipseLintClient client,
            @NonNull List<? extends IResource> resources,
            @Nullable IResource source,
            boolean show) {
        if (resources != null && !resources.isEmpty()) {
            if (!AdtPrefs.getPrefs().getSkipLibrariesFromLint()) {
                resources = addLibraries(resources);
            }

            cancelCurrentJobs(false);

            LintJob job = new LintJob(client, resources, source);
            job.schedule();

            if (show) {
                // Show lint view where the results are listed
                LintViewPart.show(resources);
            }
            return job;
        }

        return null;
    }

    /**
     * Run Lint for an Export APK action. If it succeeds (no fatal errors)
     * returns true, and if it fails it will display an error message and return
     * false.
     *
     * @param shell the parent shell to show error messages in
     * @param project the project to run lint on
     * @return true if the lint run succeeded with no fatal errors
     */
    public static boolean runLintOnExport(Shell shell, IProject project) {
        if (AdtPrefs.getPrefs().isLintOnExport()) {
            boolean fatal = EclipseLintRunner.runLint(Collections.singletonList(project),
                    null, null, true /*fatalOnly*/);
            if (fatal) {
                MessageDialog.openWarning(shell,
                        "Export Aborted",
                        "Export aborted because fatal lint errors were found. These " +
                        "are listed in the Lint View. Either fix these before " +
                        "running Export again, or turn off \"Run full error check " +
                        "when exporting app\" in the Android > Lint Error Checking " +
                        "preference page.");
                return false;
            }
        }

        return true;
    }

    /** Cancels the current lint jobs, if any, and optionally waits for them to finish */
    static void cancelCurrentJobs(boolean wait) {
        // Cancel any current running jobs first
        Job[] currentJobs = LintJob.getCurrentJobs();
        for (Job job : currentJobs) {
            job.cancel();
        }

        if (wait) {
            for (Job job : currentJobs) {
                try {
                    job.join();
                } catch (InterruptedException e) {
                    AdtPlugin.log(e, null);
                }
            }
        }
    }

    /** If the resource list contains projects, add in any library projects as well */
    private static List<? extends IResource> addLibraries(List<? extends IResource> resources) {
        if (resources != null && !resources.isEmpty()) {
            boolean haveProjects = false;
            for (IResource resource : resources) {
                if (resource instanceof IProject) {
                    haveProjects = true;
                    break;
                }
            }

            if (haveProjects) {
                List<IResource> result = new ArrayList<IResource>();
                Map<IProject, IProject> allProjects = new IdentityHashMap<IProject, IProject>();
                List<IProject> projects = new ArrayList<IProject>();
                for (IResource resource : resources) {
                    if (resource instanceof IProject) {
                        IProject project = (IProject) resource;
                        allProjects.put(project, project);
                        projects.add(project);
                    } else {
                        result.add(resource);
                    }
                }
                for (IProject project : projects) {
                    ProjectState state = Sdk.getProjectState(project);
                    if (state != null) {
                        for (IProject library : state.getFullLibraryProjects()) {
                            allProjects.put(library, library);
                        }
                    }
                }
                for (IProject project : allProjects.keySet()) {
                    result.add(project);
                }

                return result;
            }
        }

        return resources;
    }
}
