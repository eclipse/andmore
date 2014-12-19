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

package org.eclipse.andmore.internal.build.builders;

import com.android.SdkConstants;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.Messages;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.List;
import java.util.Map;

/**
 * Resource manager builder whose only purpose is to refresh the resource folder
 * so that the other builder use an up to date version.
 */
public class ResourceManagerBuilder extends BaseBuilder {

    public static final String ID = "org.eclipse.andmore.ResourceManagerBuilder"; //$NON-NLS-1$

    public ResourceManagerBuilder() {
        super();
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);

        // Get the project.
        IProject project = getProject();

        // Clear the project of the generic markers
        removeMarkersFromContainer(project, AdtConstants.MARKER_ADT);
    }

    // build() returns a list of project from which this project depends for future compilation.
    @SuppressWarnings("unchecked")
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        // Get the project.
        final IProject project = getProject();
        IJavaProject javaProject = JavaCore.create(project);

        // Clear the project of the generic markers
        removeMarkersFromContainer(project, AdtConstants.MARKER_ADT);

        // check for existing target marker, in which case we abort.
        // (this means: no SDK, no target, or unresolvable target.)
        try {
            abortOnBadSetup(javaProject, null);
        } catch (AbortBuildException e) {
            return null;
        }

        // Check the compiler compliance level, displaying the error message
        // since this is the first builder.
        Pair<Integer, String> result = ProjectHelper.checkCompilerCompliance(project);
        String errorMessage = null;
        switch (result.getFirst().intValue()) {
            case ProjectHelper.COMPILER_COMPLIANCE_LEVEL:
                errorMessage = Messages.Requires_Compiler_Compliance_s;
                break;
            case ProjectHelper.COMPILER_COMPLIANCE_SOURCE:
                errorMessage = Messages.Requires_Source_Compatibility_s;
                break;
            case ProjectHelper.COMPILER_COMPLIANCE_CODEGEN_TARGET:
                errorMessage = Messages.Requires_Class_Compatibility_s;
                break;
        }

        if (errorMessage != null) {
            errorMessage = String.format(errorMessage,
                    result.getSecond() == null ? "(no value)" : result.getSecond());

            if (JavaCore.VERSION_1_7.equals(result.getSecond())) {
                // If the user is trying to target 1.7 but compiling with something older,
                // the error message can be a bit misleading; instead point them in the
                // direction of updating the project's build target.
                Sdk currentSdk = Sdk.getCurrent();
                if (currentSdk != null) {
                    IAndroidTarget target = currentSdk.getTarget(project.getProject());
                    if (target != null && target.getVersion().getApiLevel() < 19) {
                        errorMessage = "Using 1.7 requires compiling with Android 4.4 " +
                                "(KitKat); currently using " + target.getVersion();
                    }

                    ProjectState projectState = Sdk.getProjectState(project);
                    if (projectState != null) {
                        BuildToolInfo buildToolInfo = projectState.getBuildToolInfo();
                        if (buildToolInfo == null) {
                            buildToolInfo = currentSdk.getLatestBuildTool();
                        }
                        if (buildToolInfo != null && buildToolInfo.getRevision().getMajor() < 19) {
                            errorMessage = "Using 1.7 requires using Android Build Tools " +
                                    "version 19 or later; currently using " +
                                    buildToolInfo.getRevision();
                        }
                    }
                }
            }

            markProject(AdtConstants.MARKER_ADT, errorMessage, IMarker.SEVERITY_ERROR);
            AdtPlugin.printErrorToConsole(project, errorMessage);

            return null;
        }

        // Check that the SDK directory has been setup.
        String osSdkFolder = AdtPlugin.getOsSdkFolder();

        if (osSdkFolder == null || osSdkFolder.length() == 0) {
            AdtPlugin.printErrorToConsole(project, Messages.No_SDK_Setup_Error);
            markProject(AdtConstants.MARKER_ADT, Messages.No_SDK_Setup_Error,
                    IMarker.SEVERITY_ERROR);

            return null;
        }

        // check the 'gen' source folder is present
        boolean hasGenSrcFolder = false; // whether the project has a 'gen' source folder setup

        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = e.getPath();
                    if (path.segmentCount() == 2 &&
                            path.segment(1).equals(SdkConstants.FD_GEN_SOURCES)) {
                        hasGenSrcFolder = true;
                        break;
                    }
                }
            }
        }

        boolean genFolderPresent = false; // whether the gen folder actually exists
        IResource resource = project.findMember(SdkConstants.FD_GEN_SOURCES);
        genFolderPresent = resource != null && resource.exists();

        if (hasGenSrcFolder == false && genFolderPresent) {
            // No source folder setup for 'gen' in the project, but there's already a
            // 'gen' resource (file or folder).
            String message;
            if (resource.getType() == IResource.FOLDER) {
                // folder exists already! This is an error. If the folder had been created
                // by the NewProjectWizard, it'd be a source folder.
                message = String.format("%1$s already exists but is not a source folder. Convert to a source folder or rename it.",
                        resource.getFullPath().toString());
            } else {
                // resource exists but is not a folder.
                message = String.format(
                        "Resource %1$s is in the way. ADT needs a source folder called 'gen' to work. Rename or delete resource.",
                        resource.getFullPath().toString());
            }

            AdtPlugin.printErrorToConsole(project, message);
            markProject(AdtConstants.MARKER_ADT, message, IMarker.SEVERITY_ERROR);

            return null;
        } else if (hasGenSrcFolder == false || genFolderPresent == false) {
            // either there is no 'gen' source folder in the project (older SDK),
            // or the folder does not exist (was deleted, or was a fresh svn checkout maybe.)

            // In case we are migrating from an older SDK, we go through the current source
            // folders and delete the generated Java files.
            List<IPath> sourceFolders = BaseProjectHelper.getSourceClasspaths(javaProject);
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IPath path : sourceFolders) {
                IResource member = root.findMember(path);
                if (member != null) {
                    removeDerivedResources(member, monitor);
                }
            }

            // create the new source folder, if needed
            IFolder genFolder = project.getFolder(SdkConstants.FD_GEN_SOURCES);
            if (genFolderPresent == false) {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        "Creating 'gen' source folder for generated Java files");
                genFolder.create(true /* force */, true /* local */,
                        new SubProgressMonitor(monitor, 10));
            }

            // add it to the source folder list, if needed only (or it will throw)
            if (hasGenSrcFolder == false) {
                IClasspathEntry[] entries = javaProject.getRawClasspath();
                entries = ProjectHelper.addEntryToClasspath(entries,
                        JavaCore.newSourceEntry(genFolder.getFullPath()));
                javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
            }

            // refresh specifically the gen folder first, as it may break the build
            // if it doesn't arrive in time then refresh the whole project as usual.
            genFolder.refreshLocal(IResource.DEPTH_ZERO, new SubProgressMonitor(monitor, 10));
            project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 10));

            // it seems like doing this fails to properly rebuild the project. the Java builder
            // running right after this builder will not see the gen folder, and will not be
            // restarted after this build. Therefore in this particular case, we start another
            // build asynchronously so that it's rebuilt after this build.
            launchJob(new Job("rebuild") {
                @Override
                protected IStatus run(IProgressMonitor m) {
                    try {
                        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, m);
                        return Status.OK_STATUS;
                    } catch (CoreException e) {
                        return e.getStatus();
                    }
                }
            });

        }

        // convert older projects which use bin as the eclipse output folder into projects
        // using bin/classes
        IFolder androidOutput = BaseProjectHelper.getAndroidOutputFolder(project);
        IFolder javaOutput = BaseProjectHelper.getJavaOutputFolder(project);
        if (androidOutput.exists() == false || javaOutput == null ||
                javaOutput.getParent().equals(androidOutput) == false) {
            // get what we want as the new java output.
            IFolder newJavaOutput = androidOutput.getFolder(SdkConstants.FD_CLASSES_OUTPUT);

            if (androidOutput.exists() == false) {
                androidOutput.create(true /*force*/, true /*local*/, monitor);
            }

            if (newJavaOutput.exists() == false) {
                newJavaOutput.create(true /*force*/, true /*local*/, monitor);
            }

            // set the java output to this project.
            javaProject.setOutputLocation(newJavaOutput.getFullPath(), monitor);

            // need to do a full build. Can't build while we're already building, so launch a
            // job to build it right after this build
            launchJob(new Job("rebuild") {
                @Override
                protected IStatus run(IProgressMonitor jobMonitor) {
                    try {
                        project.build(IncrementalProjectBuilder.CLEAN_BUILD, jobMonitor);
                        return Status.OK_STATUS;
                    } catch (CoreException e) {
                        return e.getStatus();
                    }
                }
            });
        }

        // check that we have bin/res/
        IFolder binResFolder = androidOutput.getFolder(SdkConstants.FD_RESOURCES);
        if (binResFolder.exists() == false) {
            binResFolder.create(true /* force */, true /* local */,
                    new SubProgressMonitor(monitor, 10));
            project.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 10));
        }

        // Check the preference to be sure we are supposed to refresh
        // the folders.
        if (AdtPrefs.getPrefs().getBuildForceResResfresh()) {
            AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project, Messages.Refreshing_Res);

            // refresh the res folder.
            IFolder resFolder = project.getFolder(AdtConstants.WS_RESOURCES);
            resFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            // Also refresh the assets folder to make sure the ApkBuilder
            // will now it's changed and will force a new resource packaging.
            IFolder assetsFolder = project.getFolder(AdtConstants.WS_ASSETS);
            assetsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }

        return null;
    }
}
