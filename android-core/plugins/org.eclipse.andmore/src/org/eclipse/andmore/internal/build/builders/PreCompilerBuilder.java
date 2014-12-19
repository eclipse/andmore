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
import com.android.ide.common.xml.ManifestData;
import com.android.io.StreamException;
import com.android.manifmerger.ManifestMerger;
import com.android.manifmerger.MergerLog;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.build.RenderScriptChecker;
import com.android.sdklib.build.RenderScriptProcessor;
import com.android.sdklib.internal.build.BuildConfigGenerator;
import com.android.sdklib.internal.build.SymbolLoader;
import com.android.sdklib.internal.build.SymbolWriter;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.android.utils.Pair;
import com.android.xml.AndroidManifest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.AaptParser;
import org.eclipse.andmore.internal.build.AidlProcessor;
import org.eclipse.andmore.internal.build.Messages;
import org.eclipse.andmore.internal.build.RenderScriptLauncher;
import org.eclipse.andmore.internal.build.RsSourceChangeHandler;
import org.eclipse.andmore.internal.build.SourceProcessor;
import org.eclipse.andmore.internal.lint.EclipseLintClient;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.FixLaunchConfig;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.project.XmlErrorHandler.BasicXmlErrorListener;
import org.eclipse.andmore.internal.resources.manager.IdeScanningContext;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AdtManifestMergeCallback;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Pre Java Compiler.
 * This incremental builder performs 2 tasks:
 * <ul>
 * <li>compiles the resources located in the res/ folder, along with the
 * AndroidManifest.xml file into the R.java class.</li>
 * <li>compiles any .aidl files into a corresponding java file.</li>
 * </ul>
 *
 */
public class PreCompilerBuilder extends BaseBuilder {

    /** This ID is used in plugin.xml and in each project's .project file.
     * It cannot be changed even if the class is renamed/moved */
    public static final String ID = "org.eclipse.andmore.PreCompilerBuilder"; //$NON-NLS-1$

    /** Flag to pass to PreCompiler builder that the build is a release build.
     */
    public final static String RELEASE_REQUESTED = "android.releaseBuild"; //$NON-NLS-1$

    private static final String PROPERTY_PACKAGE = "manifestPackage"; //$NON-NLS-1$
    private static final String PROPERTY_MERGE_MANIFEST = "mergeManifest"; //$NON-NLS-1$
    private static final String PROPERTY_COMPILE_RESOURCES = "compileResources"; //$NON-NLS-1$
    private static final String PROPERTY_COMPILE_BUILDCONFIG = "createBuildConfig"; //$NON-NLS-1$
    private static final String PROPERTY_BUILDCONFIG_MODE = "buildConfigMode"; //$NON-NLS-1$

    private static final boolean MANIFEST_MERGER_ENABLED_DEFAULT = false;
    private static final String MANIFEST_MERGER_PROPERTY = "manifestmerger.enabled"; //$NON-NLS-1$

    /** Merge Manifest Flag. Computed from resource delta, reset after action is taken.
     * Stored persistently in the project. */
    private boolean mMustMergeManifest = false;
    /** Resource compilation Flag. Computed from resource delta, reset after action is taken.
     * Stored persistently in the project. */
    private boolean mMustCompileResources = false;
    /** BuildConfig Flag. Computed from resource delta, reset after action is taken.
     * Stored persistently in the project. */
    private boolean mMustCreateBuildConfig = false;
    /** BuildConfig last more Flag. Computed from resource delta, reset after action is taken.
     * Stored persistently in the project. */
    private boolean mLastBuildConfigMode;

    /** cache of the java package defined in the manifest */
    private String mManifestPackage;

    /** Output folder for generated Java File. Created on the Builder init
     * @see #startupOnInitialize()
     */
    private IFolder mGenFolder;

    /**
     * Progress monitor used at the end of every build to refresh the content of the 'gen' folder
     * and set the generated files as derived.
     */
    private DerivedProgressMonitor mDerivedProgressMonitor;

    private AidlProcessor mAidlProcessor;
    private RsSourceChangeHandler mRenderScriptSourceChangeHandler;

    /**
     * Progress monitor waiting the end of the process to set a persistent value
     * in a file. This is typically used in conjunction with <code>IResource.refresh()</code>,
     * since this call is asynchronous, and we need to wait for it to finish for the file
     * to be known by eclipse, before we can call <code>resource.setPersistentProperty</code> on
     * a new file.
     */
    private static class DerivedProgressMonitor implements IProgressMonitor {
        private boolean mCancelled = false;
        private boolean mDone = false;
        private final IFolder mGenFolder;

        public DerivedProgressMonitor(IFolder genFolder) {
            mGenFolder = genFolder;
        }

        void reset() {
            mDone = false;
        }

        @Override
        public void beginTask(String name, int totalWork) {
        }

        @Override
        public void done() {
            if (mDone == false) {
                mDone = true;
                processChildrenOf(mGenFolder);
            }
        }

        private void processChildrenOf(IFolder folder) {
            IResource[] list;
            try {
                list = folder.members();
            } catch (CoreException e) {
                return;
            }

            for (IResource member : list) {
                if (member.exists()) {
                    if (member.getType() == IResource.FOLDER) {
                        processChildrenOf((IFolder) member);
                    }

                    try {
                        member.setDerived(true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        // This really shouldn't happen since we check that the resource
                        // exist.
                        // Worst case scenario, the resource isn't marked as derived.
                    }
                }
            }
        }

        @Override
        public void internalWorked(double work) {
        }

        @Override
        public boolean isCanceled() {
            return mCancelled;
        }

        @Override
        public void setCanceled(boolean value) {
            mCancelled = value;
        }

        @Override
        public void setTaskName(String name) {
        }

        @Override
        public void subTask(String name) {
        }

        @Override
        public void worked(int work) {
        }
    }

    public PreCompilerBuilder() {
        super();
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
            AdtPlugin.log(IStatus.INFO, "%s BUILD(PRE)", project.getName());
        }

        // For the PreCompiler, only the library projects are considered Referenced projects,
        // as only those projects have an impact on what is generated by this builder.
        IProject[] result = null;

        IFolder resOutFolder = null;

        try {
            assert mDerivedProgressMonitor != null;

            mDerivedProgressMonitor.reset();

            // get the project info
            ProjectState projectState = Sdk.getProjectState(project);

            // this can happen if the project has no project.properties.
            if (projectState == null) {
                return null;
            }

            boolean isLibrary = projectState.isLibrary();

            IAndroidTarget projectTarget = projectState.getTarget();

            // get the libraries
            List<IProject> libProjects = projectState.getFullLibraryProjects();
            result = libProjects.toArray(new IProject[libProjects.size()]);

            IJavaProject javaProject = JavaCore.create(project);

            // Top level check to make sure the build can move forward.
            abortOnBadSetup(javaProject, projectState);

            // now we need to get the classpath list
            List<IPath> sourceFolderPathList = BaseProjectHelper.getSourceClasspaths(javaProject);

            IFolder androidOutputFolder = BaseProjectHelper.getAndroidOutputFolder(project);

            resOutFolder = getResOutFolder(androidOutputFolder);

            setupSourceProcessors(javaProject, projectState, sourceFolderPathList,
                    androidOutputFolder);

            PreCompilerDeltaVisitor dv = null;
            String javaPackage = null;
            String minSdkVersion = null;

            if (kind == FULL_BUILD) {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Start_Full_Pre_Compiler);

                if (DEBUG_LOG) {
                    AdtPlugin.log(IStatus.INFO, "%s full build!", project.getName());
                }

                // do some clean up.
                doClean(project, monitor);

                mMustMergeManifest = true;
                mMustCompileResources = true;
                mMustCreateBuildConfig = true;

                mAidlProcessor.prepareFullBuild(project);
                mRenderScriptSourceChangeHandler.prepareFullBuild();
            } else {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Start_Inc_Pre_Compiler);

                // Go through the resources and see if something changed.
                // Even if the mCompileResources flag is true from a previously aborted
                // build, we need to go through the Resource delta to get a possible
                // list of aidl files to compile/remove.
                IResourceDelta delta = getDelta(project);
                if (delta == null) {
                    mMustCompileResources = true;

                    mAidlProcessor.prepareFullBuild(project);
                    mRenderScriptSourceChangeHandler.prepareFullBuild();
                } else {
                    dv = new PreCompilerDeltaVisitor(this, sourceFolderPathList,
                            mAidlProcessor.getChangeHandler(),
                            mRenderScriptSourceChangeHandler);
                    delta.accept(dv);

                    // Check to see if Manifest.xml, Manifest.java, or R.java have changed:
                    mMustCompileResources |= dv.getCompileResources();
                    mMustMergeManifest |= dv.hasManifestChanged();

                    // Notify the ResourceManager:
                    ResourceManager resManager = ResourceManager.getInstance();

                    if (ResourceManager.isAutoBuilding()) {
                        ProjectResources projectResources = resManager.getProjectResources(project);

                        IdeScanningContext context = new IdeScanningContext(projectResources,
                                project, true);

                        boolean wasCleared = projectResources.ensureInitialized();

                        if (!wasCleared) {
                            resManager.processDelta(delta, context);
                        }

                        // Check whether this project or its dependencies (libraries) have
                        // resources that need compilation
                        if (wasCleared || context.needsFullAapt()) {
                            mMustCompileResources = true;

                            // Must also call markAaptRequested on the project to not just
                            // store "aapt required" on this project, but also on any projects
                            // depending on this project if it's a library project
                            ResourceManager.markAaptRequested(project);
                        }

                        // Update error markers in the source editor
                        if (!mMustCompileResources) {
                            context.updateMarkers(false /* async */);
                        }
                    } // else: already processed the deltas in ResourceManager's IRawDeltaListener

                    mAidlProcessor.doneVisiting(project);

                    // get the java package from the visitor
                    javaPackage = dv.getManifestPackage();
                    minSdkVersion = dv.getMinSdkVersion();
                }
            }

            // Has anyone marked this project as needing aapt? Typically done when
            // one of the library projects this project depends on has changed
            mMustCompileResources |= ResourceManager.isAaptRequested(project);

            // if the main manifest didn't change, then we check for the library
            // ones (will trigger manifest merging too)
            if (libProjects.size() > 0) {
                for (IProject libProject : libProjects) {
                    IResourceDelta delta = getDelta(libProject);
                    if (delta != null) {
                        PatternBasedDeltaVisitor visitor = new PatternBasedDeltaVisitor(
                                project, libProject,
                                "PRE:LibManifest"); //$NON-NLS-1$
                        visitor.addSet(ChangedFileSetHelper.MANIFEST);

                        ChangedFileSet textSymbolCFS = null;
                        if (isLibrary == false) {
                            textSymbolCFS = ChangedFileSetHelper.getTextSymbols(
                                    libProject);
                            visitor.addSet(textSymbolCFS);
                        }

                        delta.accept(visitor);

                        mMustMergeManifest |= visitor.checkSet(ChangedFileSetHelper.MANIFEST);

                        if (textSymbolCFS != null) {
                            mMustCompileResources |= visitor.checkSet(textSymbolCFS);
                        }

                        // no need to test others if we have all flags at true.
                        if (mMustMergeManifest &&
                                (mMustCompileResources || textSymbolCFS == null)) {
                            break;
                        }
                    }
                }
            }

            // store the build status in the persistent storage
            saveProjectBooleanProperty(PROPERTY_MERGE_MANIFEST, mMustMergeManifest);
            saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, mMustCompileResources);
            saveProjectBooleanProperty(PROPERTY_COMPILE_BUILDCONFIG, mMustCreateBuildConfig);

            // if there was some XML errors, we just return w/o doing
            // anything since we've put some markers in the files anyway.
            if (dv != null && dv.mXmlError) {
                AdtPlugin.printErrorToConsole(project, Messages.Xml_Error);

                return result;
            }

            if (projectState.getRenderScriptSupportMode()) {
                FullRevision minBuildToolsRev = new FullRevision(19,0,3);
                if (mBuildToolInfo.getRevision().compareTo(minBuildToolsRev) == -1) {
                    String msg = "RenderScript support mode requires Build-Tools 19.0.3 or later.";
                    AdtPlugin.printErrorToConsole(project, msg);
                    markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                    return result;
                }
            }

            // get the manifest file
            IFile manifestFile = ProjectHelper.getManifest(project);

            if (manifestFile == null) {
                String msg = String.format(Messages.s_File_Missing,
                        SdkConstants.FN_ANDROID_MANIFEST_XML);
                AdtPlugin.printErrorToConsole(project, msg);
                markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                return result;

                // TODO: document whether code below that uses manifest (which is now guaranteed
                // to be null) will actually be executed or not.
            }

            // lets check the XML of the manifest first, if that hasn't been done by the
            // resource delta visitor yet.
            if (dv == null || dv.getCheckedManifestXml() == false) {
                BasicXmlErrorListener errorListener = new BasicXmlErrorListener();
                try {
                    ManifestData parser = AndroidManifestHelper.parseUnchecked(
                            new IFileWrapper(manifestFile),
                            true /*gather data*/,
                            errorListener);

                    if (errorListener.mHasXmlError == true) {
                        // There was an error in the manifest, its file has been marked
                        // by the XmlErrorHandler. The stopBuild() call below will abort
                        // this with an exception.
                        String msg = String.format(Messages.s_Contains_Xml_Error,
                                SdkConstants.FN_ANDROID_MANIFEST_XML);
                        AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project, msg);
                        markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                        return result;
                    }

                    // Get the java package from the parser.
                    // This can be null if the parsing failed because the resource is out of sync,
                    // in which case the error will already have been logged anyway.
                    if (parser != null) {
                        javaPackage = parser.getPackage();
                        minSdkVersion = parser.getMinSdkVersionString();
                    }
                } catch (StreamException e) {
                    handleStreamException(e);

                    return result;
                } catch (ParserConfigurationException e) {
                    String msg = String.format(
                            "Bad parser configuration for %s: %s",
                            manifestFile.getFullPath(),
                            e.getMessage());

                    handleException(e, msg);
                    return result;

                } catch (SAXException e) {
                    String msg = String.format(
                            "Parser exception for %s: %s",
                            manifestFile.getFullPath(),
                            e.getMessage());

                    handleException(e, msg);
                    return result;
                } catch (IOException e) {
                    String msg = String.format(
                            "I/O error for %s: %s",
                            manifestFile.getFullPath(),
                            e.getMessage());

                    handleException(e, msg);
                    return result;
                }
            }

            int minSdkValue = -1;

            if (minSdkVersion != null) {
                try {
                    minSdkValue = Integer.parseInt(minSdkVersion);
                } catch (NumberFormatException e) {
                    // it's ok, it means minSdkVersion contains a (hopefully) valid codename.
                }

                AndroidVersion targetVersion = projectTarget.getVersion();

                // remove earlier marker from the manifest
                removeMarkersFromResource(manifestFile, AdtConstants.MARKER_ADT);

                if (minSdkValue != -1) {
                    String codename = targetVersion.getCodename();
                    if (codename != null) {
                        // integer minSdk when the target is a preview => fatal error
                        String msg = String.format(
                                "Platform %1$s is a preview and requires application manifest to set %2$s to '%1$s'",
                                codename, AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                                msg, IMarker.SEVERITY_ERROR);
                        return result;
                    } else if (minSdkValue > targetVersion.getApiLevel()) {
                        // integer minSdk is too high for the target => warning
                        String msg = String.format(
                                "Attribute %1$s (%2$d) is higher than the project target API level (%3$d)",
                                AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION,
                                minSdkValue, targetVersion.getApiLevel());
                        AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project, msg);
                        BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                                msg, IMarker.SEVERITY_WARNING);
                    }
                } else {
                    // looks like the min sdk is a codename, check it matches the codename
                    // of the platform
                    String codename = targetVersion.getCodename();
                    if (codename == null) {
                        // platform is not a preview => fatal error
                        String msg = String.format(
                                "Manifest attribute '%1$s' is set to '%2$s'. Integer is expected.",
                                AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION, minSdkVersion);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                                msg, IMarker.SEVERITY_ERROR);
                        return result;
                    } else if (codename.equals(minSdkVersion) == false) {
                        // platform and manifest codenames don't match => fatal error.
                        String msg = String.format(
                                "Value of manifest attribute '%1$s' does not match platform codename '%2$s'",
                                AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION, codename);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                                msg, IMarker.SEVERITY_ERROR);
                        return result;
                    }

                    // if we get there, the minSdkVersion is a codename matching the target
                    // platform codename. In this case we set minSdkValue to the previous API
                    // level, as it's used by source processors.
                    minSdkValue = targetVersion.getApiLevel();
                }
            } else if (projectTarget.getVersion().isPreview()) {
                // else the minSdkVersion is not set but we are using a preview target.
                // Display an error
                String codename = projectTarget.getVersion().getCodename();
                String msg = String.format(
                        "Platform %1$s is a preview and requires application manifests to set %2$s to '%1$s'",
                        codename, AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION);
                AdtPlugin.printErrorToConsole(project, msg);
                BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT, msg,
                        IMarker.SEVERITY_ERROR);
                return result;
            }

            if (javaPackage == null || javaPackage.length() == 0) {
                // looks like the AndroidManifest file isn't valid.
                String msg = String.format(Messages.s_Doesnt_Declare_Package_Error,
                        SdkConstants.FN_ANDROID_MANIFEST_XML);
                AdtPlugin.printErrorToConsole(project, msg);
                BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                        msg, IMarker.SEVERITY_ERROR);

                return result;
            } else if (javaPackage.indexOf('.') == -1) {
                // The application package name does not contain 2+ segments!
                String msg = String.format(
                        "Application package '%1$s' must have a minimum of 2 segments.",
                        SdkConstants.FN_ANDROID_MANIFEST_XML);
                AdtPlugin.printErrorToConsole(project, msg);
                BaseProjectHelper.markResource(manifestFile, AdtConstants.MARKER_ADT,
                        msg, IMarker.SEVERITY_ERROR);

                return result;
            }

            // at this point we have the java package. We need to make sure it's not a different
            // package than the previous one that were built.
            if (javaPackage.equals(mManifestPackage) == false) {
                // The manifest package has changed, the user may want to update
                // the launch configuration
                if (mManifestPackage != null) {
                    AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                            Messages.Checking_Package_Change);

                    FixLaunchConfig flc = new FixLaunchConfig(project, mManifestPackage,
                            javaPackage);
                    flc.start();
                }

                // record the new manifest package, and save it.
                mManifestPackage = javaPackage;
                saveProjectStringProperty(PROPERTY_PACKAGE, mManifestPackage);

                // force a clean
                doClean(project, monitor);
                mMustMergeManifest = true;
                mMustCompileResources = true;
                mMustCreateBuildConfig = true;
                mAidlProcessor.prepareFullBuild(project);
                mRenderScriptSourceChangeHandler.prepareFullBuild();

                saveProjectBooleanProperty(PROPERTY_MERGE_MANIFEST, mMustMergeManifest);
                saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, mMustCompileResources);
                saveProjectBooleanProperty(PROPERTY_COMPILE_BUILDCONFIG, mMustCreateBuildConfig);
            }

            try {
                handleBuildConfig(args);
            } catch (IOException e) {
                handleException(e, "Failed to create BuildConfig class");
                return result;
            }

            // merge the manifest
            if (mMustMergeManifest) {
                boolean enabled = MANIFEST_MERGER_ENABLED_DEFAULT;
                String propValue = projectState.getProperty(MANIFEST_MERGER_PROPERTY);
                if (propValue != null) {
                    enabled = Boolean.valueOf(propValue);
                }

                if (mergeManifest(androidOutputFolder, libProjects, enabled) == false) {
                    return result;
                }
            }

            List<File> libProjectsOut = new ArrayList<File>(libProjects.size());
            for (IProject libProject : libProjects) {
                libProjectsOut.add(
                        BaseProjectHelper.getAndroidOutputFolder(libProject)
                            .getLocation().toFile());
            }

            // run the source processors
            int processorStatus = SourceProcessor.COMPILE_STATUS_NONE;


            try {
                processorStatus |= mAidlProcessor.compileFiles(this,
                        project, projectTarget, sourceFolderPathList,
                        libProjectsOut, monitor);
            } catch (Throwable t) {
                handleException(t, "Failed to run aidl. Check workspace log for detail.");
                return result;
            }

            try {
                processorStatus |= compileRs(minSdkValue, projectState, androidOutputFolder,
                        resOutFolder, monitor);
            } catch (Throwable t) {
                handleException(t, "Failed to run renderscript. Check workspace log for detail.");
                return result;
            }

            // if a processor created some resources file, force recompilation of the resources.
            if ((processorStatus & SourceProcessor.COMPILE_STATUS_RES) != 0) {
                mMustCompileResources = true;
                // save the current state before attempting the compilation
                saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, mMustCompileResources);
            }

            // handle the resources, after the processors are run since some (renderscript)
            // generate resources.
            boolean compiledTheResources = mMustCompileResources;
            if (mMustCompileResources) {
                if (DEBUG_LOG) {
                    AdtPlugin.log(IStatus.INFO, "%s compiling resources!", project.getName());
                }

                IFile proguardFile = null;
                if (projectState.getProperty(ProjectProperties.PROPERTY_PROGUARD_CONFIG) != null) {
                    proguardFile = androidOutputFolder.getFile(AdtConstants.FN_AAPT_PROGUARD);
                }

                handleResources(project, javaPackage, projectTarget, manifestFile, resOutFolder,
                        libProjects, isLibrary, proguardFile);
            }

            if (processorStatus == SourceProcessor.COMPILE_STATUS_NONE &&
                    compiledTheResources == false) {
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.Nothing_To_Compile);
            }
        } catch (AbortBuildException e) {
            return result;
        } finally {
            // refresh the 'gen' source folder. Once this is done with the custom progress
            // monitor to mark all new files as derived
            mGenFolder.refreshLocal(IResource.DEPTH_INFINITE, mDerivedProgressMonitor);
            if (resOutFolder != null) {
                resOutFolder.refreshLocal(IResource.DEPTH_INFINITE, mDerivedProgressMonitor);
            }
        }

        return result;
    }

    private IFolder getResOutFolder(IFolder androidOutputFolder) {
        return androidOutputFolder.getFolder(AdtConstants.WS_BIN_RELATIVE_BC);
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);

        if (DEBUG_LOG) {
            AdtPlugin.log(IStatus.INFO, "%s CLEAN(PRE)", getProject().getName());
        }

        doClean(getProject(), monitor);
        if (mGenFolder != null) {
            mGenFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    private void doClean(IProject project, IProgressMonitor monitor) throws CoreException {
        AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                Messages.Removing_Generated_Classes);

        // remove all the derived resources from the 'gen' source folder.
        if (mGenFolder != null && mGenFolder.exists()) {
            // gen folder should not be derived, but previous version could set it to derived
            // so we make sure this isn't the case (or it'll get deleted by the clean)
            mGenFolder.setDerived(false, monitor);

            removeDerivedResources(mGenFolder, monitor);
        }

        // Clear the project of the generic markers
        removeMarkersFromContainer(project, AdtConstants.MARKER_AAPT_COMPILE);
        removeMarkersFromContainer(project, AdtConstants.MARKER_XML);
        removeMarkersFromContainer(project, AdtConstants.MARKER_AIDL);
        removeMarkersFromContainer(project, AdtConstants.MARKER_RENDERSCRIPT);
        removeMarkersFromContainer(project, AdtConstants.MARKER_MANIFMERGER);
        removeMarkersFromContainer(project, AdtConstants.MARKER_ANDROID);

        // Also clean up lint
        EclipseLintClient.clearMarkers(project);

        // clean the project repo
        ProjectResources res = ResourceManager.getInstance().getProjectResources(project);
        res.clear();
    }

    @Override
    protected void startupOnInitialize() {
        try {
            super.startupOnInitialize();

            IProject project = getProject();

            // load the previous IFolder and java package.
            mManifestPackage = loadProjectStringProperty(PROPERTY_PACKAGE);

            // get the source folder in which all the Java files are created
            mGenFolder = project.getFolder(SdkConstants.FD_GEN_SOURCES);
            mDerivedProgressMonitor = new DerivedProgressMonitor(mGenFolder);

            // Load the current compile flags. We ask for true if not found to force a recompile.
            mMustMergeManifest = loadProjectBooleanProperty(PROPERTY_MERGE_MANIFEST, true);
            mMustCompileResources = loadProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, true);
            mMustCreateBuildConfig = loadProjectBooleanProperty(PROPERTY_COMPILE_BUILDCONFIG, true);
            Boolean v = ProjectHelper.loadBooleanProperty(project, PROPERTY_BUILDCONFIG_MODE);
            if (v == null) {
                // no previous build config mode? force regenerate
                mMustCreateBuildConfig = true;
            } else {
                mLastBuildConfigMode = v;
            }

        } catch (Throwable throwable) {
            AdtPlugin.log(throwable, "Failed to finish PrecompilerBuilder#startupOnInitialize()");
        }
    }

    private void setupSourceProcessors(@NonNull IJavaProject javaProject,
            @NonNull ProjectState projectState,
            @NonNull List<IPath> sourceFolderPathList,
            @NonNull IFolder androidOutputFolder) {
        if (mAidlProcessor == null) {
            mAidlProcessor = new AidlProcessor(javaProject, mBuildToolInfo, mGenFolder);
        } else {
            mAidlProcessor.setBuildToolInfo(mBuildToolInfo);
        }

        List<File> sourceFolders = Lists.newArrayListWithCapacity(sourceFolderPathList.size());
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        for (IPath path : sourceFolderPathList) {
            IResource resource = root.findMember(path);
            if (resource != null && resource.exists() && resource.getType() == IResource.FOLDER) {
                IPath fullPath = resource.getLocation();
                if (fullPath != null) {
                    sourceFolders.add(fullPath.toFile());
                }
            }
        }

        RenderScriptChecker checker = new RenderScriptChecker(sourceFolders,
                androidOutputFolder.getLocation().toFile());
        mRenderScriptSourceChangeHandler = new RsSourceChangeHandler(checker);
    }

    private int compileRs(int minSdkValue,
            @NonNull ProjectState projectState,
            @NonNull IFolder androidOutputFolder,
            @NonNull IFolder resOutFolder,
            @NonNull IProgressMonitor monitor)
            throws IOException, InterruptedException {
        if (!mRenderScriptSourceChangeHandler.mustCompile()) {
            return SourceProcessor.COMPILE_STATUS_NONE;
        }

        RenderScriptChecker checker = mRenderScriptSourceChangeHandler.getChecker();

        List<File> inputs = checker.findInputFiles();
        List<File> importFolders = checker.getSourceFolders();
        File buildFolder = androidOutputFolder.getLocation().toFile();


        // get the renderscript target
        int rsTarget = minSdkValue == -1 ? 11 : minSdkValue;
        String rsTargetStr = projectState.getProperty(ProjectProperties.PROPERTY_RS_TARGET);
        if (rsTargetStr != null) {
            try {
                rsTarget = Integer.parseInt(rsTargetStr);
            } catch (NumberFormatException e) {
                handleException(e, String.format(
                        "Property %s is not an integer.",
                        ProjectProperties.PROPERTY_RS_TARGET));
                return SourceProcessor.COMPILE_STATUS_NONE;
            }
        }

        RenderScriptProcessor processor = new RenderScriptProcessor(
                inputs,
                importFolders,
                buildFolder,
                mGenFolder.getLocation().toFile(),
                resOutFolder.getLocation().toFile(),
                new File(buildFolder, SdkConstants.FD_RS_OBJ),
                new File(buildFolder, SdkConstants.FD_RS_LIBS),
                mBuildToolInfo,
                rsTarget,
                false /*debugBuild, always false for now*/,
                3,
                projectState.getRenderScriptSupportMode());

        // clean old dependency files fiest
        checker.cleanDependencies();

        // then clean old output files
        processor.cleanOldOutput(checker.getOldOutputs());

        RenderScriptLauncher launcher = new RenderScriptLauncher(
                getProject(),
                mGenFolder,
                resOutFolder,
                monitor,
                AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE /*verbose*/);

        // and run the build
        processor.build(launcher);

        return SourceProcessor.COMPILE_STATUS_CODE | SourceProcessor.COMPILE_STATUS_RES;
    }

    @SuppressWarnings("deprecation")
    private void handleBuildConfig(@SuppressWarnings("rawtypes") Map args)
            throws IOException, CoreException {
        boolean debugMode = !args.containsKey(RELEASE_REQUESTED);

        BuildConfigGenerator generator = new BuildConfigGenerator(
                mGenFolder.getLocation().toOSString(), mManifestPackage, debugMode);

        if (mMustCreateBuildConfig == false) {
            // check the file is present.
            IFolder folder = getGenManifestPackageFolder();
            if (folder.exists(new Path(BuildConfigGenerator.BUILD_CONFIG_NAME)) == false) {
                mMustCreateBuildConfig = true;
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, getProject(),
                        String.format("Class %1$s is missing!",
                                BuildConfigGenerator.BUILD_CONFIG_NAME));
            } else if (debugMode != mLastBuildConfigMode) {
                // else if the build mode changed, force creation
                mMustCreateBuildConfig = true;
                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, getProject(),
                        String.format("Different build mode, must update %1$s!",
                                BuildConfigGenerator.BUILD_CONFIG_NAME));
            }
        }

        if (mMustCreateBuildConfig) {
            if (DEBUG_LOG) {
                AdtPlugin.log(IStatus.INFO, "%s generating BuilderConfig!", getProject().getName());
            }

            AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, getProject(),
                    String.format("Generating %1$s...", BuildConfigGenerator.BUILD_CONFIG_NAME));
            generator.generate();

            mMustCreateBuildConfig = false;
            saveProjectBooleanProperty(PROPERTY_COMPILE_BUILDCONFIG, mMustCreateBuildConfig);
            saveProjectBooleanProperty(PROPERTY_BUILDCONFIG_MODE, mLastBuildConfigMode = debugMode);
        }
    }

    private boolean mergeManifest(IFolder androidOutFolder, List<IProject> libProjects,
            boolean enabled) throws CoreException {
        if (DEBUG_LOG) {
            AdtPlugin.log(IStatus.INFO, "%s merging manifests!", getProject().getName());
        }

        IFile outFile = androidOutFolder.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
        IFile manifest = getProject().getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);

        // remove existing markers from the manifest.
        // FIXME: only remove from manifest once the markers are put there.
        removeMarkersFromResource(getProject(), AdtConstants.MARKER_MANIFMERGER);

        // If the merging is not enabled or if there's no library then we simply copy the
        // manifest over.
        if (enabled == false || libProjects.size() == 0) {
            try {
                new FileOp().copyFile(manifest.getLocation().toFile(),
                        outFile.getLocation().toFile());

                outFile.refreshLocal(IResource.DEPTH_INFINITE, mDerivedProgressMonitor);

                saveProjectBooleanProperty(PROPERTY_MERGE_MANIFEST, mMustMergeManifest = false);
            } catch (IOException e) {
                handleException(e, "Failed to copy Manifest");
                return false;
            }
        } else {
            final ArrayList<String> errors = new ArrayList<String>();

            // TODO change MergerLog.wrapSdkLog by a custom IMergerLog that will create
            // and maintain error markers.
            ManifestMerger merger = new ManifestMerger(
                MergerLog.wrapSdkLog(new ILogger() {
                    @Override
                    public void warning(@NonNull String warningFormat, Object... args) {
                        AdtPlugin.printToConsole(getProject(), String.format(warningFormat, args));
                    }

                    @Override
                    public void info(@NonNull String msgFormat, Object... args) {
                        AdtPlugin.printToConsole(getProject(), String.format(msgFormat, args));
                    }

                    @Override
                    public void verbose(@NonNull String msgFormat, Object... args) {
                        info(msgFormat, args);
                    }

                    @Override
                    public void error(@Nullable Throwable t, @Nullable String errorFormat,
                            Object... args) {
                        errors.add(String.format(errorFormat, args));
                    }
                }),
                new AdtManifestMergeCallback());

            File[] libManifests = new File[libProjects.size()];
            int libIndex = 0;
            for (IProject lib : libProjects) {
                libManifests[libIndex++] = lib.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML)
                        .getLocation().toFile();
            }

            if (merger.process(
                    outFile.getLocation().toFile(),
                    manifest.getLocation().toFile(),
                    libManifests,
                    null /*injectAttributes*/, null /*packageOverride*/) == false) {
                if (errors.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : errors) {
                        sb.append(s).append('\n');
                    }

                    markProject(AdtConstants.MARKER_MANIFMERGER, sb.toString(),
                            IMarker.SEVERITY_ERROR);

                } else if (errors.size() == 1) {
                    markProject(AdtConstants.MARKER_MANIFMERGER, errors.get(0),
                            IMarker.SEVERITY_ERROR);
                } else {
                    markProject(AdtConstants.MARKER_MANIFMERGER, "Unknown error merging manifest",
                            IMarker.SEVERITY_ERROR);
                }
                return false;
            }

            outFile.refreshLocal(IResource.DEPTH_INFINITE, mDerivedProgressMonitor);
            saveProjectBooleanProperty(PROPERTY_MERGE_MANIFEST, mMustMergeManifest = false);
        }

        return true;
    }

    /**
     * Handles resource changes and regenerate whatever files need regenerating.
     * @param project the main project
     * @param javaPackage the app package for the main project
     * @param projectTarget the target of the main project
     * @param manifest the {@link IFile} representing the project manifest
     * @param libProjects the library dependencies
     * @param isLibrary if the project is a library project
     * @throws CoreException
     * @throws AbortBuildException
     */
    private void handleResources(IProject project, String javaPackage, IAndroidTarget projectTarget,
            IFile manifest, IFolder resOutFolder, List<IProject> libProjects, boolean isLibrary,
            IFile proguardFile) throws CoreException, AbortBuildException {
        // get the resource folder
        IFolder resFolder = project.getFolder(AdtConstants.WS_RESOURCES);

        // get the file system path
        IPath outputLocation = mGenFolder.getLocation();
        IPath resLocation = resFolder.getLocation();
        IPath manifestLocation = manifest == null ? null : manifest.getLocation();

        // those locations have to exist for us to do something!
        if (outputLocation != null && resLocation != null
                && manifestLocation != null) {
            String osOutputPath = outputLocation.toOSString();
            String osResPath = resLocation.toOSString();
            String osManifestPath = manifestLocation.toOSString();

            // remove the aapt markers
            removeMarkersFromResource(manifest, AdtConstants.MARKER_AAPT_COMPILE);
            removeMarkersFromContainer(resFolder, AdtConstants.MARKER_AAPT_COMPILE);

            AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                    Messages.Preparing_Generated_Files);

            // we need to figure out where to store the R class.
            // get the parent folder for R.java and update mManifestPackageSourceFolder
            IFolder mainPackageFolder = getGenManifestPackageFolder();

            // handle libraries
            ArrayList<IFolder> libResFolders = Lists.newArrayList();
            ArrayList<Pair<File, String>> libRFiles = Lists.newArrayList();
            if (libProjects != null) {
                for (IProject lib : libProjects) {
                    IFolder libResFolder = lib.getFolder(SdkConstants.FD_RES);
                    if (libResFolder.exists()) {
                        libResFolders.add(libResFolder);
                    }

                    try {
                        // get the package of the library, and if it's different form the
                        // main project, generate the R class for it too.
                        String libJavaPackage = AndroidManifest.getPackage(new IFolderWrapper(lib));
                        if (libJavaPackage.equals(javaPackage) == false) {

                            IFolder libOutput = BaseProjectHelper.getAndroidOutputFolder(lib);
                            File libOutputFolder = libOutput.getLocation().toFile();

                            libRFiles.add(Pair.of(
                                    new File(libOutputFolder, "R.txt"),
                                    libJavaPackage));

                        }
                    } catch (Exception e) {
                    }
                }
            }

            String proguardFilePath = proguardFile != null ?
                    proguardFile.getLocation().toOSString(): null;

            File resOutFile = resOutFolder.getLocation().toFile();
            String resOutPath = resOutFile.isDirectory() ? resOutFile.getAbsolutePath() : null;

            execAapt(project, projectTarget, osOutputPath, resOutPath, osResPath, osManifestPath,
                    mainPackageFolder, libResFolders, libRFiles, isLibrary, proguardFilePath);
        }
    }

    /**
     * Executes AAPT to generate R.java/Manifest.java
     * @param project the main project
     * @param projectTarget the main project target
     * @param osOutputPath the OS output path for the generated file. This is the source folder, not
     * the package folder.
     * @param osResPath the OS path to the res folder for the main project
     * @param osManifestPath the OS path to the manifest of the main project
     * @param packageFolder the IFolder that will contain the generated file. Unlike
     * <var>osOutputPath</var> this is the direct parent of the generated files.
     * If <var>customJavaPackage</var> is not null, this must match the new destination triggered
     * by its value.
     * @param libResFolders the list of res folders for the library.
     * @param libRFiles a list of R files for the libraries.
     * @param isLibrary if the project is a library project
     * @param proguardFile an optional path to store proguard information
     * @throws AbortBuildException
     */
    @SuppressWarnings("deprecation")
    private void execAapt(IProject project, IAndroidTarget projectTarget, String osOutputPath,
            String osBcOutPath, String osResPath, String osManifestPath, IFolder packageFolder,
            ArrayList<IFolder> libResFolders, List<Pair<File, String>> libRFiles,
            boolean isLibrary, String proguardFile)
            throws AbortBuildException {

        // We actually need to delete the manifest.java as it may become empty and
        // in this case aapt doesn't generate an empty one, but instead doesn't
        // touch it.
        IFile manifestJavaFile = packageFolder.getFile(SdkConstants.FN_MANIFEST_CLASS);
        manifestJavaFile.getLocation().toFile().delete();

        // launch aapt: create the command line
        ArrayList<String> array = new ArrayList<String>();

        String aaptPath = mBuildToolInfo.getPath(BuildToolInfo.PathId.AAPT);

        array.add(aaptPath);
        array.add("package"); //$NON-NLS-1$
        array.add("-m"); //$NON-NLS-1$
        if (AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE) {
            array.add("-v"); //$NON-NLS-1$
        }

        if (isLibrary) {
            array.add("--non-constant-id"); //$NON-NLS-1$
        }

        if (libResFolders.size() > 0) {
            array.add("--auto-add-overlay"); //$NON-NLS-1$
        }

        // If a library or has libraries, generate a text version of the R symbols.
        File outputFolder = BaseProjectHelper.getAndroidOutputFolder(project).getLocation()
                .toFile();

        if (isLibrary || !libRFiles.isEmpty()) {
            array.add("--output-text-symbols"); //$NON-NLS-1$
            array.add(outputFolder.getAbsolutePath());
        }

        array.add("-J"); //$NON-NLS-1$
        array.add(osOutputPath);
        array.add("-M"); //$NON-NLS-1$
        array.add(osManifestPath);
        if (osBcOutPath != null) {
            array.add("-S"); //$NON-NLS-1$
            array.add(osBcOutPath);
        }
        array.add("-S"); //$NON-NLS-1$
        array.add(osResPath);
        for (IFolder libResFolder : libResFolders) {
            array.add("-S"); //$NON-NLS-1$
            array.add(libResFolder.getLocation().toOSString());
        }

        array.add("-I"); //$NON-NLS-1$
        array.add(projectTarget.getPath(IAndroidTarget.ANDROID_JAR));

        // use the proguard file
        if (proguardFile != null && proguardFile.length() > 0) {
            array.add("-G");
            array.add(proguardFile);
        }

        if (AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE) {
            StringBuilder sb = new StringBuilder();
            for (String c : array) {
                sb.append(c);
                sb.append(' ');
            }
            String cmd_line = sb.toString();
            AdtPlugin.printToConsole(project, cmd_line);
        }

        // launch
        try {
            // launch the command line process
            Process process = Runtime.getRuntime().exec(
                    array.toArray(new String[array.size()]));

            // list to store each line of stderr
            ArrayList<String> stdErr = new ArrayList<String>();

            // get the output and return code from the process
            int returnCode = grabProcessOutput(process, stdErr);

            // attempt to parse the error output
            boolean parsingError = AaptParser.parseOutput(stdErr, project);

            // if we couldn't parse the output we display it in the console.
            if (parsingError) {
                if (returnCode != 0) {
                    AdtPlugin.printErrorToConsole(project, stdErr.toArray());
                } else {
                    AdtPlugin.printBuildToConsole(BuildVerbosity.NORMAL,
                            project, stdErr.toArray());
                }
            }

            if (returnCode != 0) {
                // if the exec failed, and we couldn't parse the error output
                // (and therefore not all files that should have been marked,
                // were marked), we put a generic marker on the project and abort.
                if (parsingError) {
                    markProject(AdtConstants.MARKER_ADT,
                            Messages.Unparsed_AAPT_Errors, IMarker.SEVERITY_ERROR);
                } else if (stdErr.size() == 0) {
                    // no parsing error because sdterr was empty. We still need to put
                    // a marker otherwise there's no user visible feedback.
                    markProject(AdtConstants.MARKER_ADT,
                            String.format(Messages.AAPT_Exec_Error_d, returnCode),
                            IMarker.SEVERITY_ERROR);
                }

                AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
                        Messages.AAPT_Error);

                // abort if exec failed.
                throw new AbortBuildException();
            }

            // now if the project has libraries, R needs to be created for each libraries
            // unless this is a library.
            if (isLibrary == false && !libRFiles.isEmpty()) {
                File rFile = new File(outputFolder, SdkConstants.FN_RESOURCE_TEXT);
                // if the project has no resources, the file could not exist.
                if (rFile.isFile()) {
                    // Load the full symbols from the full R.txt file.
                    SymbolLoader fullSymbolValues = new SymbolLoader(rFile);
                    fullSymbolValues.load();

                    Multimap<String, SymbolLoader> libMap = ArrayListMultimap.create();

                    // First pass processing the libraries, collecting them by packageName,
                    // and ignoring the ones that have the same package name as the application
                    // (since that R class was already created).

                    for (Pair<File, String> lib : libRFiles) {
                        String libPackage = lib.getSecond();
                        File rText = lib.getFirst();

                        if (rText.isFile()) {
                            // load the lib symbols
                            SymbolLoader libSymbols = new SymbolLoader(rText);
                            libSymbols.load();

                            // store these symbols by associating them with the package name.
                            libMap.put(libPackage, libSymbols);
                        }
                    }

                    // now loop on all the package names, merge all the symbols to write,
                    // and write them
                    for (String packageName : libMap.keySet()) {
                        Collection<SymbolLoader> symbols = libMap.get(packageName);

                        SymbolWriter writer = new SymbolWriter(osOutputPath, packageName,
                                fullSymbolValues);
                        for (SymbolLoader symbolLoader : symbols) {
                            writer.addSymbolsToWrite(symbolLoader);
                        }
                        writer.write();
                    }
                }
            }

        } catch (IOException e1) {
            // something happen while executing the process,
            // mark the project and exit
            String msg;
            String path = array.get(0);
            if (!new File(path).exists()) {
                msg = String.format(Messages.AAPT_Exec_Error_s, path);
            } else {
                String description = e1.getLocalizedMessage();
                if (e1.getCause() != null && e1.getCause() != e1) {
                    description = description + ": " + e1.getCause().getLocalizedMessage();
                }
                msg = String.format(Messages.AAPT_Exec_Error_Other_s, description);
            }

            markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

            // Add workaround for the Linux problem described here:
            //    http://developer.android.com/sdk/installing.html#troubleshooting
            // There are various posts on StackOverflow elsewhere where people are asking
            // about aapt failing to run, so even though this is documented in the
            // Troubleshooting section add an error message to help with this
            // scenario.
            if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_LINUX
                    && System.getProperty("os.arch").endsWith("64") //$NON-NLS-1$ //$NON-NLS-2$
                    && new File(aaptPath).exists()
                    && new File("/usr/bin/apt-get").exists()) {     //$NON-NLS-1$
                markProject(AdtConstants.MARKER_ADT,
                        "Hint: On 64-bit systems, make sure the 32-bit libraries are installed: \"sudo apt-get install ia32-libs\" or on some systems, \"sudo apt-get install lib32z1\"",
                        IMarker.SEVERITY_ERROR);
                // Note - this uses SEVERITY_ERROR even though it's really SEVERITY_INFO because
                // we want this error message to show up adjacent to the aapt error message
                // (and Eclipse sorts by priority)
            }

            // This interrupts the build.
            throw new AbortBuildException();
        } catch (InterruptedException e) {
            // we got interrupted waiting for the process to end...
            // mark the project and exit
            String msg = String.format(Messages.AAPT_Exec_Error_s, array.get(0));
            markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

            // This interrupts the build.
            throw new AbortBuildException();
        } finally {
            // we've at least attempted to run aapt, save the fact that we don't have to
            // run it again, unless there's a new resource change.
            saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES,
                    mMustCompileResources = false);
            ResourceManager.clearAaptRequest(project);
        }
    }

    /**
     * Creates a relative {@link IPath} from a java package.
     * @param javaPackageName the java package.
     */
    private IPath getJavaPackagePath(String javaPackageName) {
        // convert the java package into path
        String[] segments = javaPackageName.split(AdtConstants.RE_DOT);

        StringBuilder path = new StringBuilder();
        for (String s : segments) {
           path.append(AdtConstants.WS_SEP_CHAR);
           path.append(s);
        }

        return new Path(path.toString());
    }

    /**
     * Returns an {@link IFolder} (located inside the 'gen' source folder), that matches the
     * package defined in the manifest. This {@link IFolder} may not actually exist
     * (aapt will create it anyway).
     * @return the {@link IFolder} that will contain the R class or null if
     * the folder was not found.
     * @throws CoreException
     */
    private IFolder getGenManifestPackageFolder() throws CoreException {
        // get the path for the package
        IPath packagePath = getJavaPackagePath(mManifestPackage);

        // get a folder for this path under the 'gen' source folder, and return it.
        // This IFolder may not reference an actual existing folder.
        return mGenFolder.getFolder(packagePath);
    }
}
