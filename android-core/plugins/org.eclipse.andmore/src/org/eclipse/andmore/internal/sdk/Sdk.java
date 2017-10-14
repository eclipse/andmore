/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.sdk;

import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_JAR;
import static com.android.SdkConstants.FD_RES;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ddmlib.IDevice;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.sdk.LoadStatus;
import com.android.io.StreamException;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.repository.Revision;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.RepoManager;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.sdklib.repository.meta.DetailsTypes;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;
import com.android.utils.ILogger;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.build.DexWrapper;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.LibraryClasspathContainerInitializer;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IFileListener;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IProjectListener;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IResourceEventListener;
import org.eclipse.andmore.internal.sdk.ProjectState.LibraryDifference;
import org.eclipse.andmore.internal.sdk.ProjectState.LibraryState;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central point to load, manipulate and deal with the Android SDK. Only one SDK can be used
 * at the same time.
 *
 * To start using an SDK, call {@link #loadSdk(String)} which returns the instance of
 * the Sdk object.
 *
 * To get the list of platforms or add-ons present in the SDK, call {@link #getTargets()}.
 */
public final class Sdk  {
    private final static boolean DEBUG = false;

    private final static Object LOCK = new Object();

    private static Sdk sCurrentSdk = null;

    /**
     * Map associating {@link IProject} and their state {@link ProjectState}.
     * <p/>This <b>MUST NOT</b> be accessed directly. Instead use {@link #getProjectState(IProject)}.
     */
    private final static HashMap<IProject, ProjectState> sProjectStateMap =
            new HashMap<IProject, ProjectState>();

    /**
     * Data bundled using during the load of Target data.
     * <p/>This contains the {@link LoadStatus} and a list of projects that attempted
     * to compile before the loading was finished. Those projects will be recompiled
     * at the end of the loading.
     */
    private final static class TargetLoadBundle {
        LoadStatus status;
        final HashSet<IJavaProject> projectsToReload = new HashSet<IJavaProject>();
    }

    private final AndroidSdkHandler mAndroidSdkHandler;

    private final RepoManager mManager;
    private final Map<String, DexWrapper> mDexWrappers = Maps.newHashMap();
    private final AvdManager mAvdManager;
    private final DeviceManager mDeviceManager;

    /** Map associating an {@link IAndroidTarget} to an {@link AndroidTargetData} */
    private final HashMap<IAndroidTarget, AndroidTargetData> mTargetDataMap =
        new HashMap<IAndroidTarget, AndroidTargetData>();
    /** Map associating an {@link IAndroidTarget} and its {@link TargetLoadBundle}. */
    private final HashMap<IAndroidTarget, TargetLoadBundle> mTargetDataStatusMap =
        new HashMap<IAndroidTarget, TargetLoadBundle>();

    /**
     * If true the target data will never load anymore. The only way to reload them is to
     * completely reload the SDK with {@link #loadSdk(String)}
     */
    private boolean mDontLoadTargetData = false;

    private final String mDocBaseUrl;

    /**
     * Classes implementing this interface will receive notification when targets are changed.
     */
    public interface ITargetChangeListener {
        /**
         * Sent when project has its target changed.
         */
        void onProjectTargetChange(IProject changedProject);

        /**
         * Called when the targets are loaded (either the SDK finished loading when Eclipse starts,
         * or the SDK is changed).
         */
        void onTargetLoaded(IAndroidTarget target);

        /**
         * Called when the base content of the SDK is parsed.
         */
        void onSdkLoaded();
    }

    /**
     * Basic abstract implementation of the ITargetChangeListener for the case where both
     * {@link #onProjectTargetChange(IProject)} and {@link #onTargetLoaded(IAndroidTarget)}
     * use the same code based on a simple test requiring to know the current IProject.
     */
    public static abstract class TargetChangeListener implements ITargetChangeListener {
        /**
         * Returns the {@link IProject} associated with the listener.
         */
        public abstract IProject getProject();

        /**
         * Called when the listener needs to take action on the event. This is only called
         * if {@link #getProject()} and the {@link IAndroidTarget} associated with the project
         * match the values received in {@link #onProjectTargetChange(IProject)} and
         * {@link #onTargetLoaded(IAndroidTarget)}.
         */
        public abstract void reload();

        @Override
        public void onProjectTargetChange(IProject changedProject) {
            if (changedProject != null && changedProject.equals(getProject())) {
                reload();
            }
        }

        @Override
        public void onTargetLoaded(IAndroidTarget target) {
            IProject project = getProject();
            if (target != null && target.equals(Sdk.getCurrent().getTarget(project))) {
                reload();
            }
        }

        @Override
        public void onSdkLoaded() {
            // do nothing;
        }
    }

    /**
     * Returns the lock object used to synchronize all operations dealing with SDK, targets and
     * projects.
     */
    @NonNull
    public static final Object getLock() {
        return LOCK;
    }

    /**
     * Loads an SDK and returns an {@link Sdk} object if success.
     * <p/>If the SDK failed to load, it displays an error to the user.
     * @param sdkLocation the OS path to the SDK.
     */
    @Nullable
    public static Sdk loadSdk(String sdkLocation) {
        synchronized (LOCK) {
            if (sCurrentSdk != null) {
                sCurrentSdk.dispose();
                sCurrentSdk = null;
            }

            final AtomicBoolean hasWarning = new AtomicBoolean();
            final AtomicBoolean hasError = new AtomicBoolean();
            final ArrayList<String> logMessages = new ArrayList<String>();
            ILogger log = new ILogger() {
                @Override
                public void error(@Nullable Throwable throwable, @Nullable String errorFormat,
                        Object... arg) {
                    hasError.set(true);
                    if (errorFormat != null) {
                        logMessages.add(String.format("Error: " + errorFormat, arg));
                    }

                    if (throwable != null) {
                        logMessages.add(throwable.getMessage());
                    }
                }

                @Override
                public void warning(@NonNull String warningFormat, Object... arg) {
                    hasWarning.set(true);
                    logMessages.add(String.format("Warning: " + warningFormat, arg));
                }

                @Override
                public void info(@NonNull String msgFormat, Object... arg) {
                    logMessages.add(String.format(msgFormat, arg));
                }

                @Override
                public void verbose(@NonNull String msgFormat, Object... arg) {
                    info(msgFormat, arg);
                }
            };

            
            // get an SdkManager object for the location
            AndroidSdkHandler androidSdkHandler = AndroidSdkHandler.getInstance(new File(sdkLocation));
            try {
                if (androidSdkHandler == null) {
                    hasError.set(true);
                } else {
                    RepoManager manager = androidSdkHandler.getSdkManager(new FakeProgressIndicator());
                    // create the AVD Manager
                    AvdManager avdManager = null;
                    try {
                        avdManager = AvdManager.getInstance(androidSdkHandler, log);
                    } catch (AndroidLocationException e) {
                        log.error(e, "Error parsing the AVDs");
                    }
                    sCurrentSdk = new Sdk(androidSdkHandler, manager, avdManager);
                    return sCurrentSdk;
                }
            } finally {
                if (hasError.get() || hasWarning.get()) {
                    StringBuilder sb = new StringBuilder(
                            String.format("%s when loading the SDK:\n",
                                    hasError.get() ? "Error" : "Warning"));
                    for (String msg : logMessages) {
                        sb.append('\n');
                        sb.append(msg);
                    }
                    if (hasError.get()) {
                        AndmoreAndroidPlugin.printErrorToConsole("Android SDK", sb.toString());
                        AndmoreAndroidPlugin.displayError("Android SDK", sb.toString());
                    } else {
                        AndmoreAndroidPlugin.printToConsole("Android SDK", sb.toString());
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns the current {@link Sdk} object.
     */
    @Nullable
    public static Sdk getCurrent() {
        synchronized (LOCK) {
            return sCurrentSdk;
        }
    }

    /**
     * Returns the location of the current SDK as an OS path string.
     * Guaranteed to be terminated by a platform-specific path separator.
     * <p/>
     * Due to {@link File} canonicalization, this MAY differ from the string used to initialize
     * the SDK path.
     *
     * @return The SDK OS path or null if no SDK is setup.
     * @deprecated Consider using {@link #getSdkFileLocation()} instead.
     * @see #getSdkFileLocation()
     */
    @Deprecated
    @Nullable
    public String getSdkOsLocation() {
        String path = mManager == null ? null : mManager.getLocalPath().toString();
        if (path != null) {
            // For backward compatibility make sure it ends with a separator.
            // This used to be the case when the SDK Manager was created from a String path
            // but now that a File is internally used the trailing dir separator is lost.
            if (path.length() > 0 && !path.endsWith(File.separator)) {
                path = path + File.separator;
            }
        }
        return path;
    }

    /**
     * Returns the location of the current SDK as a {@link File} or null.
     *
     * @return The SDK OS path or null if no SDK is setup.
     */
    @Nullable
    public File getSdkFileLocation() {
        if (mAndroidSdkHandler == null) {
            return null;
        }
        return mAndroidSdkHandler.getLocation();
    }

    /**
     * Returns the URL to the local documentation.
     * Can return null if no documentation is found in the current SDK.
     *
     * @return A file:// URL on the local documentation folder if it exists or null.
     */
    @Nullable
    public String getDocumentationBaseUrl() {
        return mDocBaseUrl;
    }

    /**
     * Returns the list of targets that are available in the SDK.
     */
    @NonNull
    public Collection<IAndroidTarget> getTargets() {
        return mAndroidSdkHandler.getAndroidTargetManager(new FakeProgressIndicator()).getTargets(new FakeProgressIndicator());
    }

    /**
     * Queries the underlying SDK Manager to check whether the platforms or addons
     * directories have changed on-disk. Does not reload the SDK.
     * <p/>
     * This is a quick test based on the presence of the directories, their timestamps
     * and a quick checksum of the source.properties files. It's possible to have
     * false positives (e.g. if a file is manually modified in a platform) or false
     * negatives (e.g. if a platform data file is changed manually in a 2nd level
     * directory without altering the source.properties.)
     */
    public boolean haveTargetsChanged() {
        return false; //mManager.hasChanged();
    }

    /**
     * Returns a target from a hash that was generated by {@link IAndroidTarget#hashString()}.
     *
     * @param hash the {@link IAndroidTarget} hash string.
     * @return The matching {@link IAndroidTarget} or null.
     */
    @Nullable
    public IAndroidTarget getTargetFromHashString(@NonNull String hash) {
        return mAndroidSdkHandler.getAndroidTargetManager(new FakeProgressIndicator()).getTargetFromHashString(hash, new FakeProgressIndicator());
    }

    @Nullable
    public BuildToolInfo getBuildToolInfo(@Nullable String buildToolVersion) {
        if (buildToolVersion != null) {
            try {
                return mAndroidSdkHandler.getBuildToolInfo(Revision.parseRevision(buildToolVersion), new FakeProgressIndicator());
            } catch (Exception e) {
                // ignore, return null below.
            }
        }

        return null;
    }

    @Nullable
    public BuildToolInfo getLatestBuildTool() {
        return mAndroidSdkHandler.getLatestBuildTool(new FakeProgressIndicator(), true);
    }

    /**
     * Initializes a new project with a target. This creates the <code>project.properties</code>
     * file.
     * @param project the project to initialize
     * @param target the project's target.
     * @throws IOException if creating the file failed in any way.
     * @throws StreamException if processing the project property file fails
     */
    public void initProject(@Nullable IProject project, @Nullable IAndroidTarget target)
            throws IOException, StreamException {
        if (project == null || target == null) {
            return;
        }

        synchronized (LOCK) {
            // check if there's already a state?
            ProjectState state = getProjectState(project);

            ProjectPropertiesWorkingCopy properties = null;

            if (state != null) {
                properties = state.getProperties().makeWorkingCopy();
            }

            if (properties == null) {
                IPath location = project.getLocation();
                if (location == null) {  // can return null when the project is being deleted.
                    // do nothing and return null;
                    return;
                }

                properties = ProjectProperties.create(location.toOSString(), PropertyType.PROJECT);
            }

            // save the target hash string in the project persistent property
            properties.setProperty(ProjectProperties.PROPERTY_TARGET, target.hashString());
            properties.save();
        }
    }

    /**
     * Returns the {@link ProjectState} object associated with a given project.
     * <p/>
     * This method is the only way to properly get the project's {@link ProjectState}
     * If the project has not yet been loaded, then it is loaded.
     * <p/>Because this methods deals with projects, it's not linked to an actual {@link Sdk}
     * objects, and therefore is static.
     * <p/>The value returned by {@link ProjectState#getTarget()} will change as {@link Sdk} objects
     * are replaced.
     * @param project the request project
     * @return the ProjectState for the project.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static ProjectState getProjectState(IProject project) {
        if (project == null) {
            return null;
        }

        synchronized (LOCK) {
            ProjectState state = sProjectStateMap.get(project);
            if (state == null) {
                // load the project.properties from the project folder.
                IPath location = project.getLocation();
                if (location == null) {  // can return null when the project is being deleted.
                    // do nothing and return null;
                    return null;
                }

                String projectLocation = location.toOSString();

                ProjectProperties properties = ProjectProperties.load(projectLocation,
                        PropertyType.PROJECT);
                if (properties == null) {
                    // legacy support: look for default.properties and rename it if needed.
                    properties = ProjectProperties.load(projectLocation,
                            PropertyType.LEGACY_DEFAULT);

                    if (properties == null) {
                        AndmoreAndroidPlugin.log(IStatus.ERROR,
                                "Failed to load properties file for project '%s'",
                                project.getName());
                        return null;
                    } else {
                        //legacy mode.
                        // get a working copy with the new type "project"
                        ProjectPropertiesWorkingCopy wc = properties.makeWorkingCopy(
                                PropertyType.PROJECT);
                        // and save it
                        try {
                            wc.save();

                            // delete the old file.
                            ProjectProperties.delete(projectLocation, PropertyType.LEGACY_DEFAULT);

                            // make sure to use the new properties
                            properties = ProjectProperties.load(projectLocation,
                                    PropertyType.PROJECT);
                        } catch (Exception e) {
                            AndmoreAndroidPlugin.log(IStatus.ERROR,
                                    "Failed to rename properties file to %1$s for project '%s2$'",
                                    PropertyType.PROJECT.getFilename(), project.getName());
                        }
                    }
                }

                state = new ProjectState(project, properties);
                sProjectStateMap.put(project, state);

                // try to resolve the target
                if (AndmoreAndroidPlugin.getDefault().getSdkLoadStatus() == LoadStatus.LOADED) {
                    sCurrentSdk.loadTargetAndBuildTools(state);
                }
            }

            return state;
        }
    }

    /**
     * Returns the {@link IAndroidTarget} object associated with the given {@link IProject}.
     */
    @Nullable
    public IAndroidTarget getTarget(IProject project) {
        if (project == null) {
            return null;
        }

        ProjectState state = getProjectState(project);
        if (state != null) {
            return state.getTarget();
        }

        return null;
    }

    /**
     * Loads the {@link IAndroidTarget} and BuildTools for a given project.
     * <p/>This method will get the target hash string from the project properties, and resolve
     * it to an {@link IAndroidTarget} object and store it inside the {@link ProjectState}.
     * @param state the state representing the project to load.
     * @return the target that was loaded.
     */
    @Nullable
    public IAndroidTarget loadTargetAndBuildTools(ProjectState state) {
        IAndroidTarget target = null;
        if (state != null) {
            String hash = state.getTargetHashString();
            if (hash != null) {
                state.setTarget(target = getTargetFromHashString(hash));
            }

            String markerMessage = null;
            String buildToolInfoVersion = state.getBuildToolInfoVersion();
            if (buildToolInfoVersion != null) {
                BuildToolInfo buildToolsInfo = getBuildToolInfo(buildToolInfoVersion);

                if (buildToolsInfo != null) {
                    state.setBuildToolInfo(buildToolsInfo);
                } else {
                    markerMessage = String.format("Unable to resolve %s property value '%s'",
                                        ProjectProperties.PROPERTY_BUILD_TOOLS,
                                        buildToolInfoVersion);
                }
            } else {
                // this is ok, we'll use the latest one automatically.
                state.setBuildToolInfo(null);
            }

            handleBuildToolsMarker(state.getProject(), markerMessage);
        }

        return target;
    }

    /**
     * Adds or edit a build tools marker from the given project. This is done through a Job.
     * @param project the project
     * @param markerMessage the message. if null the marker is removed.
     */
    private void handleBuildToolsMarker(final IProject project, final String markerMessage) {
        Job markerJob = new Job("Android SDK: Build Tools Marker") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (project.isAccessible()) {
                        // always delete existing marker first
                        project.deleteMarkers(AndmoreAndroidConstants.MARKER_BUILD_TOOLS, true,
                                IResource.DEPTH_ZERO);

                        // add the new one if needed.
                        if (markerMessage != null) {
                            BaseProjectHelper.markProject(project,
                                    AndmoreAndroidConstants.MARKER_BUILD_TOOLS,
                                    markerMessage, IMarker.SEVERITY_ERROR,
                                    IMarker.PRIORITY_HIGH);
                        }
                    }
                } catch (CoreException e2) {
                    AndmoreAndroidPlugin.log(e2, null);
                    // Don't return e2.getStatus(); the job control will then produce
                    // a popup with this error, which isn't very interesting for the
                    // user.
                }

                return Status.OK_STATUS;
            }
        };

        // build jobs are run after other interactive jobs
        markerJob.setPriority(Job.BUILD);
        markerJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
        markerJob.schedule();
    }

    /**
     * Checks and loads (if needed) the data for a given target.
     * <p/> The data is loaded in a separate {@link Job}, and opened editors will be notified
     * through their implementation of {@link ITargetChangeListener#onTargetLoaded(IAndroidTarget)}.
     * <p/>An optional project as second parameter can be given to be recompiled once the target
     * data is finished loading.
     * <p/>The return value is non-null only if the target data has already been loaded (and in this
     * case is the status of the load operation)
     * @param target the target to load.
     * @param project an optional project to be recompiled when the target data is loaded.
     * If the target is already loaded, nothing happens.
     * @return The load status if the target data is already loaded.
     */
    @NonNull
    public LoadStatus checkAndLoadTargetData(final IAndroidTarget target, IJavaProject project) {
        boolean loadData = false;

        synchronized (LOCK) {
            if (mDontLoadTargetData) {
                return LoadStatus.FAILED;
            }

            TargetLoadBundle bundle = mTargetDataStatusMap.get(target);
            if (bundle == null) {
                bundle = new TargetLoadBundle();
                mTargetDataStatusMap.put(target,bundle);

                // set status to loading
                bundle.status = LoadStatus.LOADING;

                // add project to bundle
                if (project != null) {
                    bundle.projectsToReload.add(project);
                }

                // and set the flag to start the loading below
                loadData = true;
            } else if (bundle.status == LoadStatus.LOADING) {
                // add project to bundle
                if (project != null) {
                    bundle.projectsToReload.add(project);
                }

                return bundle.status;
            } else if (bundle.status == LoadStatus.LOADED || bundle.status == LoadStatus.FAILED) {
                return bundle.status;
            }
        }

        if (loadData) {
            Job job = new Job(String.format("Loading data for %1$s", target.getFullName())) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    AndmoreAndroidPlugin plugin = AndmoreAndroidPlugin.getDefault();
                    try {
                        IStatus status = new AndroidTargetParser(target).run(monitor);

                        IJavaProject[] javaProjectArray = null;

                        synchronized (LOCK) {
                            TargetLoadBundle bundle = mTargetDataStatusMap.get(target);

                            if (status.getCode() != IStatus.OK) {
                                bundle.status = LoadStatus.FAILED;
                                bundle.projectsToReload.clear();
                            } else {
                                bundle.status = LoadStatus.LOADED;

                                // Prepare the array of project to recompile.
                                // The call is done outside of the synchronized block.
                                javaProjectArray = bundle.projectsToReload.toArray(
                                        new IJavaProject[bundle.projectsToReload.size()]);

                                // and update the UI of the editors that depend on the target data.
                                plugin.updateTargetListeners(target);
                            }
                        }

                        if (javaProjectArray != null) {
                            ProjectHelper.updateProjects(javaProjectArray);
                        }

                        return status;
                    } catch (Throwable t) {
                        synchronized (LOCK) {
                            TargetLoadBundle bundle = mTargetDataStatusMap.get(target);
                            bundle.status = LoadStatus.FAILED;
                        }

                        AndmoreAndroidPlugin.log(t, "Exception in checkAndLoadTargetData.");    //$NON-NLS-1$
                        return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                                String.format(
                                        "Parsing Data for %1$s failed", //$NON-NLS-1$
                                        target.hashString()),
                                t);
                    }
                }
            };
            job.setPriority(Job.BUILD); // build jobs are run after other interactive jobs
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();
        }

        // The only way to go through here is when the loading starts through the Job.
        // Therefore the current status of the target is LOADING.
        return LoadStatus.LOADING;
    }

    /**
     * Return the {@link AndroidTargetData} for a given {@link IAndroidTarget}.
     */
    @Nullable
    public AndroidTargetData getTargetData(IAndroidTarget target) {
        synchronized (LOCK) {
            return mTargetDataMap.get(target);
        }
    }

    @Nullable
    public IAndroidTarget getAndroidTargetFor(AvdInfo avdInfo) {
        // TODO this is intended for testing and is clearly wrong, we probably need some ProgressIndicator -> ProgressMonitor
        FakeProgressIndicator indicator = new FakeProgressIndicator();
        return mAndroidSdkHandler.getAndroidTargetManager(indicator)
                .getTargetOfAtLeastApiLevel(avdInfo.getAndroidVersion().getApiLevel(), indicator);
    }

    /**
     * Return the {@link AndroidTargetData} for a given {@link IProject}.
     */
    @Nullable
    public AndroidTargetData getTargetData(IProject project) {
        synchronized (LOCK) {
            IAndroidTarget target = getTarget(project);
            if (target != null) {
                return getTargetData(target);
            }
        }

        return null;
    }

    /**
     * Returns a {@link DexWrapper} object to be used to execute dx commands. If dx.jar was not
     * loaded properly, then this will return <code>null</code>.
     */
    @Nullable
    public DexWrapper getDexWrapper(@Nullable BuildToolInfo buildToolInfo) {
        if (buildToolInfo == null) {
            return null;
        }
        synchronized (LOCK) {
            String dexLocation = buildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR);
            DexWrapper dexWrapper = mDexWrappers.get(dexLocation);

            if (dexWrapper == null) {
                // load DX.
                dexWrapper = new DexWrapper();
                IStatus res = dexWrapper.loadDex(dexLocation);
                if (res != Status.OK_STATUS) {
                    AndmoreAndroidPlugin.log(null, res.getMessage());
                    dexWrapper = null;
                } else {
                    mDexWrappers.put(dexLocation, dexWrapper);
                }
            }

            return dexWrapper;
        }
    }

    public void unloadDexWrappers() {
        synchronized (LOCK) {
            for (DexWrapper wrapper : mDexWrappers.values()) {
                wrapper.unload();
            }
            mDexWrappers.clear();
        }
    }

    /**
     * Returns the {@link AvdManager}. If the AvdManager failed to parse the AVD folder, this could
     * be <code>null</code>.
     */
    @Nullable
    public AvdManager getAvdManager() {
        return mAvdManager;
    }

    @NonNull
    public AndroidSdkHandler getAndroidSdkHandler() {
        return mAndroidSdkHandler;
    }

    @Nullable
    public static AndroidVersion getDeviceVersion(@NonNull IDevice device) {
        try {
            Map<String, String> props = device.getProperties();
            String apiLevel = props.get(IDevice.PROP_BUILD_API_LEVEL);
            if (apiLevel == null) {
                return null;
            }

            return new AndroidVersion(Integer.parseInt(apiLevel),
                    props.get((IDevice.PROP_BUILD_CODENAME)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    public DeviceManager getDeviceManager() {
        return mDeviceManager;
    }

    /**
     * Returns a list of {@link ProjectState} representing projects depending, directly or
     * indirectly on a given library project.
     * @param project the library project.
     * @return a possibly empty list of ProjectState.
     */
    @NonNull
    public static Set<ProjectState> getMainProjectsFor(IProject project) {
        synchronized (LOCK) {
            // first get the project directly depending on this.
            Set<ProjectState> list = new HashSet<ProjectState>();

            // loop on all project and see if ProjectState.getLibrary returns a non null
            // project.
            for (Entry<IProject, ProjectState> entry : sProjectStateMap.entrySet()) {
                if (project != entry.getKey()) {
                    LibraryState library = entry.getValue().getLibrary(project);
                    if (library != null) {
                        list.add(entry.getValue());
                    }
                }
            }

            // now look for projects depending on the projects directly depending on the library.
            HashSet<ProjectState> result = new HashSet<ProjectState>(list);
            for (ProjectState p : list) {
                if (p.isLibrary()) {
                    Set<ProjectState> set = getMainProjectsFor(p.getProject());
                    result.addAll(set);
                }
            }

            return result;
        }
    }

    @NonNull
    public Map<File, String> getExtraSamples() {
        Map<File, String> samples = new HashMap<File, String>();
        
        // TODO how do we find these properly?
        return samples;
    }

    /**
     * Unload the SDK's target data.
     *
     * If <var>preventReload</var>, this effect is final until the SDK instance is changed
     * through {@link #loadSdk(String)}.
     *
     * The goal is to unload the targets to be able to replace existing targets with new ones,
     * before calling {@link #loadSdk(String)} to fully reload the SDK.
     *
     * @param preventReload prevent the data from being loaded again for the remaining live of
     *   this {@link Sdk} instance.
     */
    public void unloadTargetData(boolean preventReload) {
        synchronized (LOCK) {
            mDontLoadTargetData = preventReload;

            // dispose of the target data.
            for (AndroidTargetData data : mTargetDataMap.values()) {
                data.dispose();
            }

            mTargetDataMap.clear();
        }
    }

    private Sdk(AndroidSdkHandler androidSdkHandler, RepoManager manager, AvdManager avdManager) {
        mManager = manager;
        mAvdManager = avdManager;
        mAndroidSdkHandler = androidSdkHandler;

        // listen to projects closing
        GlobalProjectMonitor monitor = GlobalProjectMonitor.getMonitor();
        // need to register the resource event listener first because the project listener
        // is called back during registration with project opened in the workspace.
        monitor.addResourceEventListener(mResourceEventListener);
        monitor.addProjectListener(mProjectListener);
        monitor.addFileListener(mFileListener,
                IResourceDelta.CHANGED | IResourceDelta.ADDED | IResourceDelta.REMOVED);

        // pre-compute some paths
        mDocBaseUrl = getDocumentationBaseUrl(manager.getLocalPath() +
                SdkConstants.OS_SDK_DOCS_FOLDER);

        mDeviceManager = DeviceManager.createInstance(mAndroidSdkHandler.getLocation(),
                                                      AndmoreAndroidPlugin.getDefault());

        // update whatever ProjectState is already present with new IAndroidTarget objects.
        synchronized (LOCK) {
            for (Entry<IProject, ProjectState> entry: sProjectStateMap.entrySet()) {
                loadTargetAndBuildTools(entry.getValue());
            }
        }
    }

    /**
     *  Cleans and unloads the SDK.
     */
    private void dispose() {
        GlobalProjectMonitor monitor = GlobalProjectMonitor.getMonitor();
        monitor.removeProjectListener(mProjectListener);
        monitor.removeFileListener(mFileListener);
        monitor.removeResourceEventListener(mResourceEventListener);

        // the IAndroidTarget objects are now obsolete so update the project states.
        synchronized (LOCK) {
            for (Entry<IProject, ProjectState> entry: sProjectStateMap.entrySet()) {
                entry.getValue().setTarget(null);
            }

            // dispose of the target data.
            for (AndroidTargetData data : mTargetDataMap.values()) {
                data.dispose();
            }

            mTargetDataMap.clear();
        }
    }

    void setTargetData(IAndroidTarget target, AndroidTargetData data) {
        synchronized (LOCK) {
            mTargetDataMap.put(target, data);
        }
    }

    /**
     * Returns the URL to the local documentation.
     * Can return null if no documentation is found in the current SDK.
     *
     * @param osDocsPath Path to the documentation folder in the current SDK.
     *  The folder may not actually exist.
     * @return A file:// URL on the local documentation folder if it exists or null.
     */
    private String getDocumentationBaseUrl(String osDocsPath) {
        File f = new File(osDocsPath);

        if (f.isDirectory()) {
            try {
                // Note: to create a file:// URL, one would typically use something like
                // f.toURI().toURL().toString(). However this generates a broken path on
                // Windows, namely "C:\\foo" is converted to "file:/C:/foo" instead of
                // "file:///C:/foo" (i.e. there should be 3 / after "file:"). So we'll
                // do the correct thing manually.

                String path = f.getAbsolutePath();
                if (File.separatorChar != '/') {
                    path = path.replace(File.separatorChar, '/');
                }

                // For some reason the URL class doesn't add the mandatory "//" after
                // the "file:" protocol name, so it has to be hacked into the path.
                URL url = new URL("file", null, "//" + path);  //$NON-NLS-1$ //$NON-NLS-2$
                String result = url.toString();
                return result;
            } catch (MalformedURLException e) {
                // ignore malformed URLs
            }
        }

        return null;
    }

    /**
     * Delegate listener for project changes.
     */
    private IProjectListener mProjectListener = new IProjectListener() {
        @Override
        public void projectClosed(IProject project) {
            onProjectRemoved(project, false /*deleted*/);
        }

        @Override
        public void projectDeleted(IProject project) {
            onProjectRemoved(project, true /*deleted*/);
        }

        private void onProjectRemoved(IProject removedProject, boolean deleted) {
            if (DEBUG) {
                System.out.println(">>> CLOSED: " + removedProject.getName());
            }

            // get the target project
            synchronized (LOCK) {
                // Don't use getProject() as it could create the ProjectState if it's not
                // there yet and this is not what we want. We want the current object.
                // Therefore, direct access to the map.
                ProjectState removedState = sProjectStateMap.get(removedProject);
                if (removedState != null) {
                    // 1. clear the layout lib cache associated with this project
                    IAndroidTarget target = removedState.getTarget();
                    if (target != null) {
                        // get the bridge for the target, and clear the cache for this project.
                        AndroidTargetData data = mTargetDataMap.get(target);
                        if (data != null) {
                            LayoutLibrary layoutLib = data.getLayoutLibrary();
                            if (layoutLib != null && layoutLib.getStatus() == LoadStatus.LOADED) {
                                layoutLib.clearCaches(removedProject);
                            }
                        }
                    }

                    // 2. if the project is a library, make sure to update the
                    // LibraryState for any project referencing it.
                    // Also, record the updated projects that are libraries, to update
                    // projects that depend on them.
                    for (ProjectState projectState : sProjectStateMap.values()) {
                        LibraryState libState = projectState.getLibrary(removedProject);
                        if (libState != null) {
                            // Close the library right away.
                            // This remove links between the LibraryState and the projectState.
                            // This is because in case of a rename of a project, projectClosed and
                            // projectOpened will be called before any other job is run, so we
                            // need to make sure projectOpened is closed with the main project
                            // state up to date.
                            libState.close();

                            // record that this project changed, and in case it's a library
                            // that its parents need to be updated as well.
                            markProject(projectState, projectState.isLibrary());
                        }
                    }

                    // now remove the project for the project map.
                    sProjectStateMap.remove(removedProject);
                }
            }

            if (DEBUG) {
                System.out.println("<<<");
            }
        }

        @Override
        public void projectOpened(IProject project) {
            onProjectOpened(project);
        }

        @Override
        public void projectOpenedWithWorkspace(IProject project) {
            // no need to force recompilation when projects are opened with the workspace.
            onProjectOpened(project);
        }

        @Override
        public void allProjectsOpenedWithWorkspace() {
            // Correct currently open editors
            fixOpenLegacyEditors();
        }

        private void onProjectOpened(final IProject openedProject) {

            ProjectState openedState = getProjectState(openedProject);
            if (openedState != null) {
                if (DEBUG) {
                    System.out.println(">>> OPENED: " + openedProject.getName());
                }

                synchronized (LOCK) {
                    final boolean isLibrary = openedState.isLibrary();
                    final boolean hasLibraries = openedState.hasLibraries();

                    if (isLibrary || hasLibraries) {
                        boolean foundLibraries = false;
                        // loop on all the existing project and update them based on this new
                        // project
                        for (ProjectState projectState : sProjectStateMap.values()) {
                            if (projectState != openedState) {
                                // If the project has libraries, check if this project
                                // is a reference.
                                if (hasLibraries) {
                                    // ProjectState#needs() both checks if this is a missing library
                                    // and updates LibraryState to contains the new values.
                                    // This must always be called.
                                    LibraryState libState = openedState.needs(projectState);

                                    if (libState != null) {
                                        // found a library! Add the main project to the list of
                                        // modified project
                                        foundLibraries = true;
                                    }
                                }

                                // if the project is a library check if the other project depend
                                // on it.
                                if (isLibrary) {
                                    // ProjectState#needs() both checks if this is a missing library
                                    // and updates LibraryState to contains the new values.
                                    // This must always be called.
                                    LibraryState libState = projectState.needs(openedState);

                                    if (libState != null) {
                                        // There's a dependency! Add the project to the list of
                                        // modified project, but also to a list of projects
                                        // that saw one of its dependencies resolved.
                                        markProject(projectState, projectState.isLibrary());
                                    }
                                }
                            }
                        }

                        // if the project has a libraries and we found at least one, we add
                        // the project to the list of modified project.
                        // Since we already went through the parent, no need to update them.
                        if (foundLibraries) {
                            markProject(openedState, false /*updateParents*/);
                        }
                    }
                }

                // Correct file editor associations.
                fixEditorAssociations(openedProject);

                // Fix classpath entries in a job since the workspace might be locked now.
                Job fixCpeJob = new Job("Adjusting Android Project Classpath") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            ProjectHelper.fixProjectClasspathEntries(
                                    JavaCore.create(openedProject));
                        } catch (JavaModelException e) {
                            AndmoreAndroidPlugin.log(e, "error fixing classpath entries");
                            // Don't return e2.getStatus(); the job control will then produce
                            // a popup with this error, which isn't very interesting for the
                            // user.
                        }

                        return Status.OK_STATUS;
                    }
                };

                // build jobs are run after other interactive jobs
                fixCpeJob.setPriority(Job.BUILD);
                fixCpeJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
                fixCpeJob.schedule();


                if (DEBUG) {
                    System.out.println("<<<");
                }
            }
        }

        @Override
        public void projectRenamed(IProject project, IPath from) {
            // we don't actually care about this anymore.
        }
    };

    /**
     * Delegate listener for file changes.
     */
    private IFileListener mFileListener = new IFileListener() {
        @Override
        public void fileChanged(final @NonNull IFile file, @NonNull IMarkerDelta[] markerDeltas,
                int kind, @Nullable String extension, int flags, boolean isAndroidPRoject) {
            if (!isAndroidPRoject) {
                return;
            }

            if (SdkConstants.FN_PROJECT_PROPERTIES.equals(file.getName()) &&
                    file.getParent() == file.getProject()) {
                try {
                    // reload the content of the project.properties file and update
                    // the target.
                    IProject iProject = file.getProject();

                    ProjectState state = Sdk.getProjectState(iProject);

                    // get the current target and build tools
                    IAndroidTarget oldTarget = state.getTarget();
                    boolean oldRsSupportMode = state.getRenderScriptSupportMode();

                    // get the current library flag
                    boolean wasLibrary = state.isLibrary();

                    LibraryDifference diff = state.reloadProperties();

                    // load the (possibly new) target.
                    IAndroidTarget newTarget = loadTargetAndBuildTools(state);

                    // reload the libraries if needed
                    if (diff.hasDiff()) {
                        if (diff.added) {
                            synchronized (LOCK) {
                                for (ProjectState projectState : sProjectStateMap.values()) {
                                    if (projectState != state) {
                                        // need to call needs to do the libraryState link,
                                        // but no need to look at the result, as we'll compare
                                        // the result of getFullLibraryProjects()
                                        // this is easier to due to indirect dependencies.
                                        state.needs(projectState);
                                    }
                                }
                            }
                        }

                        markProject(state, wasLibrary || state.isLibrary());
                    }

                    // apply the new target if needed.
                    if (newTarget != oldTarget ||
                            oldRsSupportMode != state.getRenderScriptSupportMode()) {
                        IJavaProject javaProject = BaseProjectHelper.getJavaProject(
                                file.getProject());
                        if (javaProject != null) {
                            ProjectHelper.updateProject(javaProject);
                        }

                        // update the editors to reload with the new target
                        AndmoreAndroidPlugin.getDefault().updateTargetListeners(iProject);
                    }
                } catch (CoreException e) {
                    // This can't happen as it's only for closed project (or non existing)
                    // but in that case we can't get a fileChanged on this file.
                }
            } else if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                // check if it's an add/remove on a jar files inside libs
                if (EXT_JAR.equals(extension) &&
                        file.getProjectRelativePath().segmentCount() == 2 &&
                        file.getParent().getName().equals(SdkConstants.FD_NATIVE_LIBS)) {
                    // need to update the project and whatever depend on it.

                    processJarFileChange(file);
                }
            }
        }

        private void processJarFileChange(final IFile file) {
            try {
                IProject iProject = file.getProject();

                if (iProject.hasNature(AndmoreAndroidConstants.NATURE_DEFAULT) == false) {
                    return;
                }

                List<IJavaProject> projectList = new ArrayList<IJavaProject>();
                IJavaProject javaProject = BaseProjectHelper.getJavaProject(iProject);
                if (javaProject != null) {
                    projectList.add(javaProject);
                }

                ProjectState state = Sdk.getProjectState(iProject);

                if (state != null) {
                    Collection<ProjectState> parents = state.getFullParentProjects();
                    for (ProjectState s : parents) {
                        javaProject = BaseProjectHelper.getJavaProject(s.getProject());
                        if (javaProject != null) {
                            projectList.add(javaProject);
                        }
                    }

                    ProjectHelper.updateProjects(
                            projectList.toArray(new IJavaProject[projectList.size()]));
                }
            } catch (CoreException e) {
                // This can't happen as it's only for closed project (or non existing)
                // but in that case we can't get a fileChanged on this file.
            }
        }
    };

    /** List of modified projects. This is filled in
     * {@link IProjectListener#projectOpened(IProject)},
     * {@link IProjectListener#projectOpenedWithWorkspace(IProject)},
     * {@link IProjectListener#projectClosed(IProject)}, and
     * {@link IProjectListener#projectDeleted(IProject)} and processed in
     * {@link IResourceEventListener#resourceChangeEventEnd()}.
     */
    private final List<ProjectState> mModifiedProjects = new ArrayList<ProjectState>();
    private final List<ProjectState> mModifiedChildProjects = new ArrayList<ProjectState>();

    private void markProject(ProjectState projectState, boolean updateParents) {
        if (mModifiedProjects.contains(projectState) == false) {
            if (DEBUG) {
                System.out.println("\tMARKED: " + projectState.getProject().getName());
            }
            mModifiedProjects.add(projectState);
        }

        // if the project is resolved also add it to this list.
        if (updateParents) {
            if (mModifiedChildProjects.contains(projectState) == false) {
                if (DEBUG) {
                    System.out.println("\tMARKED(child): " + projectState.getProject().getName());
                }
                mModifiedChildProjects.add(projectState);
            }
        }
    }

    /**
     * Delegate listener for resource changes. This is called before and after any calls to the
     * project and file listeners (for a given resource change event).
     */
    private IResourceEventListener mResourceEventListener = new IResourceEventListener() {
        @Override
        public void resourceChangeEventStart() {
            mModifiedProjects.clear();
            mModifiedChildProjects.clear();
        }

        @Override
        public void resourceChangeEventEnd() {
            if (mModifiedProjects.size() == 0) {
                return;
            }

            // first make sure all the parents are updated
            updateParentProjects();

            // for all modified projects, update their library list
            // and gather their IProject
            final List<IJavaProject> projectList = new ArrayList<IJavaProject>();
            for (ProjectState state : mModifiedProjects) {
                state.updateFullLibraryList();
                projectList.add(JavaCore.create(state.getProject()));
            }

            Job job = new Job("Android Library Update") { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    LibraryClasspathContainerInitializer.updateProjects(
                            projectList.toArray(new IJavaProject[projectList.size()]));

                    for (IJavaProject javaProject : projectList) {
                        try {
                            javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD,
                                    monitor);
                        } catch (CoreException e) {
                            // pass
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setPriority(Job.BUILD);
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();
        }
    };

    /**
     * Updates all existing projects with a given list of new/updated libraries.
     * This loops through all opened projects and check if they depend on any of the given
     * library project, and if they do, they are linked together.
     */
    private void updateParentProjects() {
        if (mModifiedChildProjects.size() == 0) {
            return;
        }

        ArrayList<ProjectState> childProjects = new ArrayList<ProjectState>(mModifiedChildProjects);
        mModifiedChildProjects.clear();
        synchronized (LOCK) {
            // for each project for which we must update its parent, we loop on the parent
            // projects and adds them to the list of modified projects. If they are themselves
            // libraries, we add them too.
            for (ProjectState state : childProjects) {
                if (DEBUG) {
                    System.out.println(">>> Updating parents of " + state.getProject().getName());
                }
                List<ProjectState> parents = state.getParentProjects();
                for (ProjectState parent : parents) {
                    markProject(parent, parent.isLibrary());
                }
                if (DEBUG) {
                    System.out.println("<<<");
                }
            }
        }

        // done, but there may be parents that are also libraries. Need to update their parents.
        updateParentProjects();
    }

    /**
     * Fix editor associations for the given project, if not already done.
     * <p/>
     * Eclipse has a per-file setting for which editor should be used for each file
     * (see {@link IDE#setDefaultEditor(IFile, String)}).
     * We're using this flag to pick between the various XML editors (layout, drawable, etc)
     * since they all have the same file name extension.
     * <p/>
     * Unfortunately, the file setting can be "wrong" for two reasons:
     * <ol>
     *   <li> The editor type was added <b>after</b> a file had been seen by the IDE.
     *        For example, we added new editors for animations and for drawables around
     *        ADT 12, but any file seen by ADT in earlier versions will continue to use
     *        the vanilla Eclipse XML editor instead.
     *   <li> A bug in ADT 14 and ADT 15 (see issue 21124) meant that files created in new
     *        folders would end up with wrong editor associations. Even though that bug
     *        is fixed in ADT 16, the fix only affects new files, it cannot retroactively
     *        fix editor associations that were set incorrectly by ADT 14 or 15.
     * </ol>
     * <p/>
     * This method attempts to fix the editor bindings retroactively by scanning all the
     * resource XML files and resetting the editor associations.
     * Since this is a potentially slow operation, this is only done "once"; we use a
     * persistent project property to avoid looking repeatedly. In the future if we add
     * additional editors, we can rev the scanned version value.
     */
    private void fixEditorAssociations(final IProject project) {
        QualifiedName KEY = new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID, "editorbinding"); //$NON-NLS-1$

        try {
            String value = project.getPersistentProperty(KEY);
            int currentVersion = 0;
            if (value != null) {
                try {
                    currentVersion = Integer.parseInt(value);
                } catch (Exception ingore) {
                }
            }

            // The target version we're comparing to. This must be incremented each time
            // we change the processing here so that a new version of the plugin would
            // try to fix existing user projects.
            final int targetVersion = 2;

            if (currentVersion >= targetVersion) {
                return;
            }

            // Set to specific version such that we can rev the version in the future
            // to trigger further scanning
            project.setPersistentProperty(KEY, Integer.toString(targetVersion));

            // Now update the actual editor associations.
            Job job = new Job("Update Android editor bindings") { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        for (IResource folderResource : project.getFolder(FD_RES).members()) {
                            if (folderResource instanceof IFolder) {
                                IFolder folder = (IFolder) folderResource;

                                for (IResource resource : folder.members()) {
                                    if (resource instanceof IFile &&
                                            resource.getName().endsWith(DOT_XML)) {
                                        fixXmlFile((IFile) resource);
                                    }
                                }
                            }
                        }

                        // TODO change AndroidManifest.xml ID too

                    } catch (CoreException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }

                    return Status.OK_STATUS;
                }

                /**
                 * Attempt to fix the editor ID for the given /res XML file.
                 */
                private void fixXmlFile(final IFile file) {
                    // Fix the default editor ID for this resource.
                    // This has no effect on currently open editors.
                    IEditorDescriptor desc = IDE.getDefaultEditor(file);

                    if (desc == null || !CommonXmlEditor.ID.equals(desc.getId())) {
                        IDE.setDefaultEditor(file, CommonXmlEditor.ID);
                    }
                }
            };
            job.setPriority(Job.BUILD);
            job.schedule();
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    /**
     * Tries to fix all currently open Android legacy editors.
     * <p/>
     * If an editor is found to match one of the legacy ids, we'll try to close it.
     * If that succeeds, we try to reopen it using the new common editor ID.
     * <p/>
     * This method must be run from the UI thread.
     */
    private void fixOpenLegacyEditors() {

        AndmoreAndroidPlugin adt = AndmoreAndroidPlugin.getDefault();
        if (adt == null) {
            return;
        }

        final IPreferenceStore store = adt.getPreferenceStore();
        int currentValue = store.getInt(AdtPrefs.PREFS_FIX_LEGACY_EDITORS);
        // The target version we're comparing to. This must be incremented each time
        // we change the processing here so that a new version of the plugin would
        // try to fix existing editors.
        final int targetValue = 1;

        if (currentValue >= targetValue) {
            return;
        }

        // To be able to close and open editors we need to make sure this is done
        // in the UI thread, which this isn't invoked from.
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                HashSet<String> legacyIds =
                    new HashSet<String>(Arrays.asList(CommonXmlEditor.LEGACY_EDITOR_IDS));

                for (IWorkbenchWindow win : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                    for (IWorkbenchPage page : win.getPages()) {
                        for (IEditorReference ref : page.getEditorReferences()) {
                            try {
                                IEditorInput input = ref.getEditorInput();
                                if (input instanceof IFileEditorInput) {
                                    IFile file = ((IFileEditorInput)input).getFile();
                                    IEditorPart part = ref.getEditor(true /*restore*/);
                                    if (part != null) {
                                        IWorkbenchPartSite site = part.getSite();
                                        if (site != null) {
                                            String id = site.getId();
                                            if (legacyIds.contains(id)) {
                                                // This editor matches one of legacy editor IDs.
                                                fixEditor(page, part, input, file, id);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }

                // Remember that we managed to do fix all editors
                store.setValue(AdtPrefs.PREFS_FIX_LEGACY_EDITORS, targetValue);
            }

            private void fixEditor(
                    IWorkbenchPage page,
                    IEditorPart part,
                    IEditorInput input,
                    IFile file,
                    String id) {
                IDE.setDefaultEditor(file, CommonXmlEditor.ID);

                boolean ok = page.closeEditor(part, true /*save*/);

                AndmoreAndroidPlugin.log(IStatus.INFO,
                    "Closed legacy editor ID %s for %s: %s", //$NON-NLS-1$
                    id,
                    file.getFullPath(),
                    ok ? "Success" : "Failed");//$NON-NLS-1$ //$NON-NLS-2$

                if (ok) {
                    // Try to reopen it with the new ID
                    try {
                        page.openEditor(input, CommonXmlEditor.ID);
                    } catch (PartInitException e) {
                        AndmoreAndroidPlugin.log(e,
                            "Failed to reopen %s",          //$NON-NLS-1$
                            file.getFullPath());
                    }
                }
            }
        });
    }
}
