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
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.IArchiveBuilder;
import com.android.sdklib.build.SealedApkException;
import com.android.sdklib.internal.build.DebugKeyProvider.KeytoolException;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AndroidPrintStream;
import org.eclipse.andmore.internal.build.AaptExecException;
import org.eclipse.andmore.internal.build.AaptParser;
import org.eclipse.andmore.internal.build.AaptResultException;
import org.eclipse.andmore.internal.build.BuildHelper;
import org.eclipse.andmore.internal.build.DexException;
import org.eclipse.andmore.internal.build.Messages;
import org.eclipse.andmore.internal.build.NativeLibInJarException;
import org.eclipse.andmore.internal.build.BuildHelper.ResourceMarker;
import org.eclipse.andmore.internal.lint.LintDeltaProcessor;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.ApkInstallManager;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.LibraryClasspathContainerInitializer;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class PostCompilerBuilder extends BaseBuilder {

    /** This ID is used in plugin.xml and in each project's .project file.
     * It cannot be changed even if the class is renamed/moved */
    public static final String ID = "org.eclipse.andmore.ApkBuilder"; //$NON-NLS-1$

    private static final String PROPERTY_CONVERT_TO_DEX = "convertToDex"; //$NON-NLS-1$
    private static final String PROPERTY_PACKAGE_RESOURCES = "packageResources"; //$NON-NLS-1$
    private static final String PROPERTY_BUILD_APK = "buildApk"; //$NON-NLS-1$

    /** Flag to pass to PostCompiler builder that sets if it runs or not.
     *  Set this flag whenever calling build if PostCompiler is to run
     */
    public final static String POST_C_REQUESTED = "RunPostCompiler"; //$NON-NLS-1$

    /**
     * Dex conversion flag. This is set to true if one of the changed/added/removed
     * file is a .class file. Upon visiting all the delta resource, if this
     * flag is true, then we know we'll have to make the "classes.dex" file.
     */
    private boolean mConvertToDex = false;

    /**
     * Package resources flag. This is set to true if one of the changed/added/removed
     * file is a resource file. Upon visiting all the delta resource, if
     * this flag is true, then we know we'll have to repackage the resources.
     */
    private boolean mPackageResources = false;

    /**
     * Final package build flag.
     */
    private boolean mBuildFinalPackage = false;

    private AndroidPrintStream mOutStream = null;
    private AndroidPrintStream mErrStream = null;


    private ResourceMarker mResourceMarker = new ResourceMarker() {
        @Override
        public void setWarning(IResource resource, String message) {
            BaseProjectHelper.markResource(resource, AdtConstants.MARKER_PACKAGING,
                    message, IMarker.SEVERITY_WARNING);
        }
    };


    public PostCompilerBuilder() {
        super();
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);

        // Get the project.
        IProject project = getProject();

        if (DEBUG_LOG) {
            AdtPlugin.log(IStatus.INFO, "%s CLEAN(POST)", project.getName());
        }

        // Clear the project of the generic markers
        removeMarkersFromContainer(project, AdtConstants.MARKER_AAPT_PACKAGE);
        removeMarkersFromContainer(project, AdtConstants.MARKER_PACKAGING);

        // also remove the files in the output folder (but not the Eclipse output folder).
        IFolder javaOutput = BaseProjectHelper.getJavaOutputFolder(project);
        IFolder androidOutput = BaseProjectHelper.getAndroidOutputFolder(project);

        if (javaOutput.equals(androidOutput) == false) {
            // get the content
            IResource[] members = androidOutput.members();
            for (IResource member : members) {
                if (member.equals(javaOutput) == false) {
                    member.delete(true /*force*/, monitor);
                }
            }
        }
    }

    // build() returns a list of project from which this project depends for future compilation.
    @Override
    protected IProject[] build(
            int kind,
            @SuppressWarnings("rawtypes") Map args,
            IProgressMonitor monitor)
            throws CoreException {
        // get a project object
        IProject project = getProject();

        if (DEBUG_LOG) {
            AdtPlugin.log(IStatus.INFO, "%s BUILD(POST)", project.getName());
        }

        // Benchmarking start
        long startBuildTime = 0;
        if (BuildHelper.BENCHMARK_FLAG) {
            // End JavaC Timer
            String msg = "BENCHMARK ADT: Ending Compilation \n BENCHMARK ADT: Time Elapsed: " +    //$NON-NLS-1$
                         (System.nanoTime() - BuildHelper.sStartJavaCTime)/Math.pow(10, 6) + "ms"; //$NON-NLS-1$
            AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, msg);
            msg = "BENCHMARK ADT: Starting PostCompilation";                                       //$NON-NLS-1$
            AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, msg);
            startBuildTime = System.nanoTime();
        }

        // list of referenced projects. This is a mix of java projects and library projects
        // and is computed below.
        IProject[] allRefProjects = null;

        try {
            // get the project info
            ProjectState projectState = Sdk.getProjectState(project);

            // this can happen if the project has no project.properties.
            if (projectState == null) {
                return null;
            }

            boolean isLibrary = projectState.isLibrary();

            // get the libraries
            List<IProject> libProjects = projectState.getFullLibraryProjects();

            IJavaProject javaProject = JavaCore.create(project);

            // get the list of referenced projects.
            List<IProject> javaProjects = ProjectHelper.getReferencedProjects(project);
            List<IJavaProject> referencedJavaProjects = BuildHelper.getJavaProjects(
                    javaProjects);

            // mix the java project and the library projects
            final int size = libProjects.size() + javaProjects.size();
            ArrayList<IProject> refList = new ArrayList<IProject>(size);
            refList.addAll(libProjects);
            refList.addAll(javaProjects);
            allRefProjects = refList.toArray(new IProject[size]);

            // get the android output folder
            IFolder androidOutputFolder = BaseProjectHelper.getAndroidOutputFolder(project);
            IFolder resOutputFolder = androidOutputFolder.getFolder(SdkConstants.FD_RES);

            // First thing we do is go through the resource delta to not
            // lose it if we have to abort the build for any reason.
            if (args.containsKey(POST_C_REQUESTED)
                    && AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
                // Skip over flag setting
            } else if (kind == FULL_BUILD) {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Start_Full_Apk_Build);

                if (DEBUG_LOG) {
                    AdtPlugin.log(IStatus.INFO, "%s full build!", project.getName());
                }

                // Full build: we do all the steps.
                mPackageResources = true;
                mConvertToDex = true;
                mBuildFinalPackage = true;
            } else {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Start_Inc_Apk_Build);

                // go through the resources and see if something changed.
                IResourceDelta delta = getDelta(project);
                if (delta == null) {
                    // no delta? Same as full build: we do all the steps.
                    mPackageResources = true;
                    mConvertToDex = true;
                    mBuildFinalPackage = true;
                } else {

                    if (ResourceManager.isAutoBuilding() && AdtPrefs.getPrefs().isLintOnSave()) {
                        // Check for errors on save/build, if enabled
                        LintDeltaProcessor.create().process(delta);
                    }

                    PatternBasedDeltaVisitor dv = new PatternBasedDeltaVisitor(
                            project, project,
                            "POST:Main");

                    ChangedFileSet manifestCfs = ChangedFileSetHelper.getMergedManifestCfs(project);
                    dv.addSet(manifestCfs);

                    ChangedFileSet resCfs = ChangedFileSetHelper.getResCfs(project);
                    dv.addSet(resCfs);

                    ChangedFileSet androidCodeCfs = ChangedFileSetHelper.getCodeCfs(project);
                    dv.addSet(androidCodeCfs);

                    ChangedFileSet javaResCfs = ChangedFileSetHelper.getJavaResCfs(project);
                    dv.addSet(javaResCfs);
                    dv.addSet(ChangedFileSetHelper.NATIVE_LIBS);

                    delta.accept(dv);

                    // save the state
                    mPackageResources |= dv.checkSet(manifestCfs) || dv.checkSet(resCfs);

                    mConvertToDex |= dv.checkSet(androidCodeCfs);

                    mBuildFinalPackage |= dv.checkSet(javaResCfs) ||
                            dv.checkSet(ChangedFileSetHelper.NATIVE_LIBS);
                }

                // check the libraries
                if (libProjects.size() > 0) {
                    for (IProject libProject : libProjects) {
                        delta = getDelta(libProject);
                        if (delta != null) {
                            PatternBasedDeltaVisitor visitor = new PatternBasedDeltaVisitor(
                                    project, libProject,
                                    "POST:Lib");

                            ChangedFileSet libResCfs = ChangedFileSetHelper.getFullResCfs(
                                    libProject);
                            visitor.addSet(libResCfs);
                            visitor.addSet(ChangedFileSetHelper.NATIVE_LIBS);
                            // FIXME: add check on the library.jar?

                            delta.accept(visitor);

                            mPackageResources |= visitor.checkSet(libResCfs);
                            mBuildFinalPackage |= visitor.checkSet(
                                    ChangedFileSetHelper.NATIVE_LIBS);
                        }
                    }
                }

                // also go through the delta for all the referenced projects
                final int referencedCount = referencedJavaProjects.size();
                for (int i = 0 ; i < referencedCount; i++) {
                    IJavaProject referencedJavaProject = referencedJavaProjects.get(i);
                    delta = getDelta(referencedJavaProject.getProject());
                    if (delta != null) {
                        IProject referencedProject = referencedJavaProject.getProject();
                        PatternBasedDeltaVisitor visitor = new PatternBasedDeltaVisitor(
                                project, referencedProject,
                                "POST:RefedProject");

                        ChangedFileSet javaResCfs = ChangedFileSetHelper.getJavaResCfs(referencedProject);
                        visitor.addSet(javaResCfs);

                        ChangedFileSet bytecodeCfs = ChangedFileSetHelper.getByteCodeCfs(referencedProject);
                        visitor.addSet(bytecodeCfs);

                        delta.accept(visitor);

                        // save the state
                        mConvertToDex |= visitor.checkSet(bytecodeCfs);
                        mBuildFinalPackage |= visitor.checkSet(javaResCfs);
                    }
                }
            }

            // store the build status in the persistent storage
            saveProjectBooleanProperty(PROPERTY_CONVERT_TO_DEX, mConvertToDex);
            saveProjectBooleanProperty(PROPERTY_PACKAGE_RESOURCES, mPackageResources);
            saveProjectBooleanProperty(PROPERTY_BUILD_APK, mBuildFinalPackage);

            // Top level check to make sure the build can move forward. Only do this after recording
            // delta changes.
            abortOnBadSetup(javaProject, projectState);

            // Get the output stream. Since the builder is created for the life of the
            // project, they can be kept around.
            if (mOutStream == null) {
                mOutStream = new AndroidPrintStream(project, null /*prefix*/,
                        AdtPlugin.getOutStream());
                mErrStream = new AndroidPrintStream(project, null /*prefix*/,
                        AdtPlugin.getOutStream());
            }

            // remove older packaging markers.
            removeMarkersFromContainer(javaProject.getProject(), AdtConstants.MARKER_PACKAGING);

            // finished with the common init and tests. Special case of the library.
            if (isLibrary) {
                // check the jar output file is present, if not create it.
                IFile jarIFile = androidOutputFolder.getFile(
                        project.getName().toLowerCase() + SdkConstants.DOT_JAR);
                if (mConvertToDex == false && jarIFile.exists() == false) {
                    mConvertToDex = true;
                }

                // also update the crunch cache always since aapt does it smartly only
                // on the files that need it.
                if (DEBUG_LOG) {
                    AdtPlugin.log(IStatus.INFO, "%s running crunch!", project.getName());
                }
                BuildHelper helper = new BuildHelper(
                        projectState,
                        mBuildToolInfo,
                        mOutStream, mErrStream,
                        false /*jumbo mode doesn't matter here*/,
                        false /*dex merger doesn't matter here*/,
                        true /*debugMode*/,
                        AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE,
                        mResourceMarker);
                updateCrunchCache(project, helper);

                // refresh recursively bin/res folder
                resOutputFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);

                if (mConvertToDex) { // in this case this means some class files changed and
                                     // we need to update the jar file.
                    if (DEBUG_LOG) {
                        AdtPlugin.log(IStatus.INFO, "%s updating jar!", project.getName());
                    }

                    // resource to the AndroidManifest.xml file
                    IFile manifestFile = project.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
                    String appPackage = AndroidManifest.getPackage(new IFileWrapper(manifestFile));

                    IFolder javaOutputFolder = BaseProjectHelper.getJavaOutputFolder(project);

                    writeLibraryPackage(jarIFile, project, appPackage, javaOutputFolder);
                    saveProjectBooleanProperty(PROPERTY_CONVERT_TO_DEX, mConvertToDex = false);

                    // refresh the bin folder content with no recursion to update the library
                    // jar file.
                    androidOutputFolder.refreshLocal(IResource.DEPTH_ONE, monitor);

                    // Also update the projects. The only way to force recompile them is to
                    // reset the library container.
                    List<ProjectState> parentProjects = projectState.getParentProjects();
                    LibraryClasspathContainerInitializer.updateProject(parentProjects);
                }

                return allRefProjects;
            }

            // Check to see if we're going to launch or export. If not, we can skip
            // the packaging and dexing process.
            if (!args.containsKey(POST_C_REQUESTED)
                    && AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Skip_Post_Compiler);
                return allRefProjects;
            } else {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Start_Full_Post_Compiler);
            }

            // first thing we do is check that the SDK directory has been setup.
            String osSdkFolder = AdtPlugin.getOsSdkFolder();

            if (osSdkFolder.length() == 0) {
                // this has already been checked in the precompiler. Therefore,
                // while we do have to cancel the build, we don't have to return
                // any error or throw anything.
                return allRefProjects;
            }

            // do some extra check, in case the output files are not present. This
            // will force to recreate them.
            IResource tmp = null;

            if (mPackageResources == false) {
                // check the full resource package
                tmp = androidOutputFolder.findMember(AdtConstants.FN_RESOURCES_AP_);
                if (tmp == null || tmp.exists() == false) {
                    mPackageResources = true;
                }
            }

            // check classes.dex is present. If not we force to recreate it.
            if (mConvertToDex == false) {
                tmp = androidOutputFolder.findMember(SdkConstants.FN_APK_CLASSES_DEX);
                if (tmp == null || tmp.exists() == false) {
                    mConvertToDex = true;
                }
            }

            // also check the final file(s)!
            String finalPackageName = ProjectHelper.getApkFilename(project, null /*config*/);
            if (mBuildFinalPackage == false) {
                tmp = androidOutputFolder.findMember(finalPackageName);
                if (tmp == null || (tmp instanceof IFile &&
                        tmp.exists() == false)) {
                    String msg = String.format(Messages.s_Missing_Repackaging, finalPackageName);
                    AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project, msg);
                    mBuildFinalPackage = true;
                }
            }

            // at this point we know if we need to recreate the temporary apk
            // or the dex file, but we don't know if we simply need to recreate them
            // because they are missing

            // refresh the output directory first
            IContainer ic = androidOutputFolder.getParent();
            if (ic != null) {
                ic.refreshLocal(IResource.DEPTH_ONE, monitor);
            }

            // we need to test all three, as we may need to make the final package
            // but not the intermediary ones.
            if (mPackageResources || mConvertToDex || mBuildFinalPackage) {
                String forceJumboStr = projectState.getProperty(
                        AdtConstants.DEX_OPTIONS_FORCEJUMBO);
                Boolean jumbo = Boolean.valueOf(forceJumboStr);

                String dexMergerStr = projectState.getProperty(
                        AdtConstants.DEX_OPTIONS_DISABLE_MERGER);
                Boolean dexMerger = Boolean.valueOf(dexMergerStr);

                BuildHelper helper = new BuildHelper(
                        projectState,
                        mBuildToolInfo,
                        mOutStream, mErrStream,
                        jumbo.booleanValue(),
                        dexMerger.booleanValue(),
                        true /*debugMode*/,
                        AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE,
                        mResourceMarker);

                IPath androidBinLocation = androidOutputFolder.getLocation();
                if (androidBinLocation == null) {
                    markProject(AdtConstants.MARKER_PACKAGING, Messages.Output_Missing,
                            IMarker.SEVERITY_ERROR);
                    return allRefProjects;
                }
                String osAndroidBinPath = androidBinLocation.toOSString();

                // resource to the AndroidManifest.xml file
                IFile manifestFile = androidOutputFolder.getFile(
                        SdkConstants.FN_ANDROID_MANIFEST_XML);

                if (manifestFile == null || manifestFile.exists() == false) {
                    // mark project and exit
                    String msg = String.format(Messages.s_File_Missing,
                            SdkConstants.FN_ANDROID_MANIFEST_XML);
                    markProject(AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
                    return allRefProjects;
                }

                // Remove the old .apk.
                // This make sure that if the apk is corrupted, then dx (which would attempt
                // to open it), will not fail.
                String osFinalPackagePath = osAndroidBinPath + File.separator + finalPackageName;
                File finalPackage = new File(osFinalPackagePath);

                // if delete failed, this is not really a problem, as the final package generation
                // handle already present .apk, and if that one failed as well, the user will be
                // notified.
                finalPackage.delete();

                // Check if we need to package the resources.
                if (mPackageResources) {
                    // also update the crunch cache always since aapt does it smartly only
                    // on the files that need it.
                    if (DEBUG_LOG) {
                        AdtPlugin.log(IStatus.INFO, "%s running crunch!", project.getName());
                    }
                    if (updateCrunchCache(project, helper) == false) {
                        return allRefProjects;
                    }

                    // refresh recursively bin/res folder
                    resOutputFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);

                    if (DEBUG_LOG) {
                        AdtPlugin.log(IStatus.INFO, "%s packaging resources!", project.getName());
                    }
                    // remove some aapt_package only markers.
                    removeMarkersFromContainer(project, AdtConstants.MARKER_AAPT_PACKAGE);

                    try {
                        helper.packageResources(manifestFile, libProjects, null /*resfilter*/,
                                0 /*versionCode */, osAndroidBinPath,
                                AdtConstants.FN_RESOURCES_AP_);
                    } catch (AaptExecException e) {
                        BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING,
                                e.getMessage(), IMarker.SEVERITY_ERROR);
                        return allRefProjects;
                    } catch (AaptResultException e) {
                        // attempt to parse the error output
                        String[] aaptOutput = e.getOutput();
                        boolean parsingError = AaptParser.parseOutput(aaptOutput, project);

                        // if we couldn't parse the output we display it in the console.
                        if (parsingError) {
                            AdtPlugin.printErrorToConsole(project, (Object[]) aaptOutput);

                            // if the exec failed, and we couldn't parse the error output (and
                            // therefore not all files that should have been marked, were marked),
                            // we put a generic marker on the project and abort.
                            BaseProjectHelper.markResource(project,
                                    AdtConstants.MARKER_PACKAGING,
                                    Messages.Unparsed_AAPT_Errors,
                                    IMarker.SEVERITY_ERROR);
                        }
                    }

                    // build has been done. reset the state of the builder
                    mPackageResources = false;

                    // and store it
                    saveProjectBooleanProperty(PROPERTY_PACKAGE_RESOURCES, mPackageResources);
                }

                String classesDexPath = osAndroidBinPath + File.separator +
                        SdkConstants.FN_APK_CLASSES_DEX;

                // then we check if we need to package the .class into classes.dex
                if (mConvertToDex) {
                    if (DEBUG_LOG) {
                        AdtPlugin.log(IStatus.INFO, "%s running dex!", project.getName());
                    }
                    try {
                        Collection<String> dxInputPaths = helper.getCompiledCodePaths();

                        helper.executeDx(javaProject, dxInputPaths, classesDexPath);
                    } catch (DexException e) {
                        String message = e.getMessage();

                        AdtPlugin.printErrorToConsole(project, message);
                        BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING,
                                message, IMarker.SEVERITY_ERROR);

                        Throwable cause = e.getCause();

                        if (cause instanceof NoClassDefFoundError
                                || cause instanceof NoSuchMethodError) {
                            AdtPlugin.printErrorToConsole(project, Messages.Incompatible_VM_Warning,
                                    Messages.Requires_1_5_Error);
                        }

                        // dx failed, we return
                        return allRefProjects;
                    }

                    // build has been done. reset the state of the builder
                    mConvertToDex = false;

                    // and store it
                    saveProjectBooleanProperty(PROPERTY_CONVERT_TO_DEX, mConvertToDex);
                }

                // now we need to make the final package from the intermediary apk
                // and classes.dex.
                // This is the default package with all the resources.

                try {
                    if (DEBUG_LOG) {
                        AdtPlugin.log(IStatus.INFO, "%s making final package!", project.getName());
                    }
                    helper.finalDebugPackage(
                            osAndroidBinPath + File.separator + AdtConstants.FN_RESOURCES_AP_,
                        classesDexPath, osFinalPackagePath, libProjects, mResourceMarker);
                } catch (KeytoolException e) {
                    String eMessage = e.getMessage();

                    // mark the project with the standard message
                    String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg,
                            IMarker.SEVERITY_ERROR);

                    // output more info in the console
                    AdtPlugin.printErrorToConsole(project,
                            msg,
                            String.format(Messages.ApkBuilder_JAVA_HOME_is_s, e.getJavaHome()),
                            Messages.ApkBuilder_Update_or_Execute_manually_s,
                            e.getCommandLine());

                    AdtPlugin.log(e, msg);

                    return allRefProjects;
                } catch (ApkCreationException e) {
                    String eMessage = e.getMessage();

                    // mark the project with the standard message
                    String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg,
                            IMarker.SEVERITY_ERROR);

                    AdtPlugin.log(e, msg);
                } catch (AndroidLocationException e) {
                    String eMessage = e.getMessage();

                    // mark the project with the standard message
                    String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg,
                            IMarker.SEVERITY_ERROR);
                    AdtPlugin.log(e, msg);
                } catch (NativeLibInJarException e) {
                    String msg = e.getMessage();

                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING,
                            msg, IMarker.SEVERITY_ERROR);

                    AdtPlugin.printErrorToConsole(project, (Object[]) e.getAdditionalInfo());
                } catch (CoreException e) {
                    // mark project and return
                    String msg = String.format(Messages.Final_Archive_Error_s, e.getMessage());
                    AdtPlugin.printErrorToConsole(project, msg);
                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg,
                            IMarker.SEVERITY_ERROR);
                    AdtPlugin.log(e, msg);
                } catch (DuplicateFileException e) {
                    String msg1 = String.format(
                            "Found duplicate file for APK: %1$s\nOrigin 1: %2$s\nOrigin 2: %3$s",
                            e.getArchivePath(), e.getFile1(), e.getFile2());
                    String msg2 = String.format(Messages.Final_Archive_Error_s, msg1);
                    AdtPlugin.printErrorToConsole(project, msg2);
                    BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg2,
                            IMarker.SEVERITY_ERROR);
                }

                // we are done.

                // refresh the bin folder content with no recursion.
                androidOutputFolder.refreshLocal(IResource.DEPTH_ONE, monitor);

                // build has been done. reset the state of the builder
                mBuildFinalPackage = false;

                // and store it
                saveProjectBooleanProperty(PROPERTY_BUILD_APK, mBuildFinalPackage);

                // reset the installation manager to force new installs of this project
                ApkInstallManager.getInstance().resetInstallationFor(project);

                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, getProject(),
                        "Build Success!");
            }
        } catch (AbortBuildException e) {
            return allRefProjects;
        } catch (Exception exception) {
            // try to catch other exception to actually display an error. This will be useful
            // if we get an NPE or something so that we can at least notify the user that something
            // went wrong.

            // first check if this is a CoreException we threw to cancel the build.
            if (exception instanceof CoreException) {
                if (((CoreException)exception).getStatus().getSeverity() == IStatus.CANCEL) {
                    // Project is already marked with an error. Nothing to do
                    return allRefProjects;
                }
            }

            String msg = exception.getMessage();
            if (msg == null) {
                msg = exception.getClass().getCanonicalName();
            }

            msg = String.format("Unknown error: %1$s", msg);
            AdtPlugin.logAndPrintError(exception, project.getName(), msg);
            markProject(AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
        }

        // Benchmarking end
        if (BuildHelper.BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Ending PostCompilation. \n BENCHMARK ADT: Time Elapsed: " + //$NON-NLS-1$
                         ((System.nanoTime() - startBuildTime)/Math.pow(10, 6)) + "ms";              //$NON-NLS-1$
            AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, msg);
            // End Overall Timer
            msg = "BENCHMARK ADT: Done with everything! \n BENCHMARK ADT: Time Elapsed: " +          //$NON-NLS-1$
                  (System.nanoTime() - BuildHelper.sStartOverallTime)/Math.pow(10, 6) + "ms";        //$NON-NLS-1$
            AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, msg);
        }

        return allRefProjects;
    }

    private static class JarBuilder implements IArchiveBuilder {

        private static Pattern R_PATTERN = Pattern.compile("R(\\$.*)?\\.class"); //$NON-NLS-1$
        private static String BUILD_CONFIG_CLASS = "BuildConfig.class"; //$NON-NLS-1$

        private final byte[] buffer = new byte[1024];
        private final JarOutputStream mOutputStream;
        private final String mAppPackage;

        JarBuilder(JarOutputStream outputStream, String appPackage) {
            mOutputStream = outputStream;
            mAppPackage = appPackage.replace('.', '/');
        }

        public void addFile(IFile file, IFolder rootFolder) throws ApkCreationException {
            // we only package class file from the output folder
            if (SdkConstants.EXT_CLASS.equals(file.getFileExtension()) == false) {
                return;
            }

            IPath packageApp = file.getParent().getFullPath().makeRelativeTo(
                    rootFolder.getFullPath());

            String name = file.getName();
            // Ignore the library's R/Manifest/BuildConfig classes.
            if (mAppPackage.equals(packageApp.toString()) &&
                            (BUILD_CONFIG_CLASS.equals(name) ||
                            R_PATTERN.matcher(name).matches())) {
                return;
            }

            IPath path = file.getFullPath().makeRelativeTo(rootFolder.getFullPath());
            try {
                addFile(file.getContents(), file.getLocalTimeStamp(), path.toString());
            } catch (ApkCreationException e) {
                throw e;
            } catch (Exception e) {
                throw new ApkCreationException(e, "Failed to add %s", file);
            }
        }

        @Override
        public void addFile(File file, String archivePath) throws ApkCreationException,
                SealedApkException, DuplicateFileException {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                long lastModified = file.lastModified();
                addFile(inputStream, lastModified, archivePath);
            } catch (ApkCreationException e) {
                throw e;
            } catch (Exception e) {
                throw new ApkCreationException(e, "Failed to add %s", file);
            }
        }

        private void addFile(InputStream content, long lastModified, String archivePath)
                throws IOException, ApkCreationException {
            // create the jar entry
            JarEntry entry = new JarEntry(archivePath);
            entry.setTime(lastModified);

            try {
                // add the entry to the jar archive
                mOutputStream.putNextEntry(entry);

                // read the content of the entry from the input stream, and write
                // it into the archive.
                int count;
                while ((count = content.read(buffer)) != -1) {
                    mOutputStream.write(buffer, 0, count);
                }
            } finally {
                try {
                    if (content != null) {
                        content.close();
                    }
                } catch (Exception e) {
                    throw new ApkCreationException(e, "Failed to close stream");
                }
            }
        }
    }

    /**
     * Updates the crunch cache if needed and return true if the build must continue.
     */
    private boolean updateCrunchCache(IProject project, BuildHelper helper) {
        try {
            helper.updateCrunchCache();
        } catch (AaptExecException e) {
            BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING,
                    e.getMessage(), IMarker.SEVERITY_ERROR);
            return false;
        } catch (AaptResultException e) {
            // attempt to parse the error output
            String[] aaptOutput = e.getOutput();
            boolean parsingError = AaptParser.parseOutput(aaptOutput, project);
            // if we couldn't parse the output we display it in the console.
            if (parsingError) {
                AdtPlugin.printErrorToConsole(project, (Object[]) aaptOutput);
            }
        }

        return true;
    }

    /**
     * Writes the library jar file.
     * @param jarIFile the destination file
     * @param project the library project
     * @param appPackage the library android package
     * @param javaOutputFolder the JDT output folder.
     */
    private void writeLibraryPackage(IFile jarIFile, IProject project, String appPackage,
            IFolder javaOutputFolder) {

        JarOutputStream jos = null;
        try {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.put(Attributes.Name.CLASS_PATH, "Android ADT"); //$NON-NLS-1$
            mainAttributes.putValue("Created-By", "1.0 (Android)"); //$NON-NLS-1$  //$NON-NLS-2$
            jos = new JarOutputStream(
                    new FileOutputStream(jarIFile.getLocation().toFile()), manifest);

            JarBuilder jarBuilder = new JarBuilder(jos, appPackage);

            // write the class files
            writeClassFilesIntoJar(jarBuilder, javaOutputFolder, javaOutputFolder);

            // now write the standard Java resources from the output folder
            ApkBuilder.addSourceFolder(jarBuilder, javaOutputFolder.getLocation().toFile());

            saveProjectBooleanProperty(PROPERTY_CONVERT_TO_DEX, mConvertToDex);
        } catch (Exception e) {
            AdtPlugin.log(e, "Failed to write jar file %s", jarIFile.getLocation().toOSString());
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                    // pass
                }
            }
        }
    }

    private void writeClassFilesIntoJar(JarBuilder builder, IFolder folder, IFolder rootFolder)
            throws CoreException, IOException, ApkCreationException {
        IResource[] members = folder.members();
        for (IResource member : members) {
            if (member.getType() == IResource.FOLDER) {
                writeClassFilesIntoJar(builder, (IFolder) member, rootFolder);
            } else if (member.getType() == IResource.FILE) {
                IFile file = (IFile) member;
                builder.addFile(file, rootFolder);
            }
        }
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();

        // load the build status. We pass true as the default value to
        // force a recompile in case the property was not found
        mConvertToDex = loadProjectBooleanProperty(PROPERTY_CONVERT_TO_DEX, true);
        mPackageResources = loadProjectBooleanProperty(PROPERTY_PACKAGE_RESOURCES, true);
        mBuildFinalPackage = loadProjectBooleanProperty(PROPERTY_BUILD_APK, true);
    }

    @Override
    protected void abortOnBadSetup(
            @NonNull IJavaProject javaProject,
            @Nullable ProjectState projectState) throws AbortBuildException, CoreException {
        super.abortOnBadSetup(javaProject, projectState);

        IProject iProject = getProject();

        // do a (hopefully quick) search for Precompiler type markers. Those are always only
        // errors.
        stopOnMarker(iProject, AdtConstants.MARKER_AAPT_COMPILE, IResource.DEPTH_INFINITE,
                false /*checkSeverity*/);
        stopOnMarker(iProject, AdtConstants.MARKER_AIDL, IResource.DEPTH_INFINITE,
                false /*checkSeverity*/);
        stopOnMarker(iProject, AdtConstants.MARKER_RENDERSCRIPT, IResource.DEPTH_INFINITE,
                false /*checkSeverity*/);
        stopOnMarker(iProject, AdtConstants.MARKER_ANDROID, IResource.DEPTH_ZERO,
                false /*checkSeverity*/);

        // do a search for JDT markers. Those can be errors or warnings
        stopOnMarker(iProject, IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
                IResource.DEPTH_INFINITE, true /*checkSeverity*/);
        stopOnMarker(iProject, IJavaModelMarker.BUILDPATH_PROBLEM_MARKER,
                IResource.DEPTH_INFINITE, true /*checkSeverity*/);
    }
}
