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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_XML;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Job to check lint on a set of resources */
public final class LintJob extends Job {
    /** Job family */
    private static final Object FAMILY_RUN_LINT = new Object();
    private final EclipseLintClient mClient;
    private final List<? extends IResource> mResources;
    private final IResource mSource;
    private final IssueRegistry mRegistry;
    private LintDriver mLint;
    private boolean mFatal;

    public LintJob(
            @NonNull EclipseLintClient client,
            @NonNull List<? extends IResource> resources,
            @Nullable IResource source,
            @NonNull IssueRegistry registry) {
        super("Running Android Lint");
        mClient = client;
        mResources = resources;
        mSource = source;
        mRegistry = registry;
    }

    public LintJob(
            @NonNull EclipseLintClient client,
            @NonNull List<? extends IResource> resources,
            @Nullable IResource source) {
        this(client, resources, source, EclipseLintClient.getRegistry());
    }

    @Override
    public boolean belongsTo(Object family) {
        return family == FAMILY_RUN_LINT;
    }

    @Override
    protected void canceling() {
        super.canceling();
        if (mLint != null) {
            mLint.cancel();
        }
    }

    @Override
    @NonNull
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Looking for errors", IProgressMonitor.UNKNOWN);
            EnumSet<Scope> scope = null;
            List<File> files = new ArrayList<File>(mResources.size());
            for (IResource resource : mResources) {
                File file = AdtUtils.getAbsolutePath(resource).toFile();
                files.add(file);

                if (resource instanceof IProject && mSource == null) {
                    scope = Scope.ALL;
                } else {
                    String name = resource.getName();
                    if (SdkUtils.endsWithIgnoreCase(name, DOT_XML)) {
                        if (name.equals(SdkConstants.FN_ANDROID_MANIFEST_XML)) {
                            scope = EnumSet.of(Scope.MANIFEST);
                        } else {
                            scope = Scope.RESOURCE_FILE_SCOPE;
                        }
                    } else if (name.endsWith(DOT_JAVA) && resource instanceof IFile) {
                        if (scope != null) {
                            if (!scope.contains(Scope.JAVA_FILE)) {
                                scope = EnumSet.copyOf(scope);
                                scope.add(Scope.JAVA_FILE);
                            }
                        } else {
                            scope = Scope.JAVA_FILE_SCOPE;
                        }
                    } else if (name.endsWith(DOT_CLASS) && resource instanceof IFile) {
                        if (scope != null) {
                            if (!scope.contains(Scope.CLASS_FILE)) {
                                scope = EnumSet.copyOf(scope);
                                scope.add(Scope.CLASS_FILE);
                            }
                        } else {
                            scope = Scope.CLASS_FILE_SCOPE;
                        }
                    } else {
                        return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, IStatus.ERROR,
                            "Only XML & Java files are supported for single file lint", null); //$NON-NLS-1$
                    }
                }
            }
            if (scope == null) {
                scope = Scope.ALL;
            }
            if (mSource == null) {
                assert !Scope.checkSingleFile(scope) : scope + " with " + mResources;
            }
            // Check single file?
            if (mSource != null) {
                // Delete specific markers
                IMarker[] markers = EclipseLintClient.getMarkers(mSource);
                for (IMarker marker : markers) {
                    String id = marker.getAttribute(EclipseLintRunner.MARKER_CHECKID_PROPERTY, "");
                    Issue issue = mRegistry.getIssue(id);
                    if (issue == null) {
                        continue;
                    }
                    if (issue.getImplementation().isAdequate(scope)) {
                        marker.delete();
                    }
                }
                mClient.setSearchForSuperClasses(true);
            } else {
                EclipseLintClient.clearMarkers(mResources);
            }

            mLint = new LintDriver(mRegistry, mClient);
            mLint.analyze(new LintRequest(mClient, files).setScope(scope));
            mFatal = mClient.hasFatalErrors();
            return Status.OK_STATUS;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, IStatus.ERROR,
                              "Failed", e); //$NON-NLS-1$
        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
    }

    /**
     * Returns true if a fatal error was encountered
     *
     * @return true if a fatal error was encountered
     */
    public boolean isFatal() {
        return mFatal;
    }

    /**
     * Returns the associated lint client
     *
     * @return the associated lint client
     */
    @NonNull
    public EclipseLintClient getLintClient() {
        return mClient;
    }

    /** Returns the current lint jobs, if any (never returns null but array may be empty) */
    @NonNull
    static Job[] getCurrentJobs() {
        IJobManager jobManager = Job.getJobManager();
        return jobManager.find(LintJob.FAMILY_RUN_LINT);
    }
}
