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

package org.eclipse.andmore.internal.editors.layout;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IFileListener;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IResourceEventListener;
import org.eclipse.andmore.internal.resources.manager.ResourceManager.IResourceListener;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Monitor for file changes that could trigger a layout redraw, or a UI update
 */
public final class LayoutReloadMonitor {

    // singleton, enforced by private constructor.
    private final static LayoutReloadMonitor sThis = new LayoutReloadMonitor();

    /**
     * Map of listeners by IProject.
     */
    private final Map<IProject, List<ILayoutReloadListener>> mListenerMap =
        new HashMap<IProject, List<ILayoutReloadListener>>();

    public final static class ChangeFlags {
        public boolean code = false;
        /** any non-layout resource changes */
        public boolean resources = false;
        public boolean rClass = false;
        public boolean localeList = false;
        public boolean manifest = false;

        boolean isAllTrue() {
            return code && resources && rClass && localeList && manifest;
        }
    }

    /**
     * List of projects having received a resource change.
     */
    private final Map<IProject, ChangeFlags> mProjectFlags = new HashMap<IProject, ChangeFlags>();

    /**
     * Classes which implement this interface provide a method to respond to resource changes
     * triggering a layout redraw
     */
    public interface ILayoutReloadListener {
        /**
         * Sent when the layout needs to be redrawn
         *
         * @param flags a {@link ChangeFlags} object indicating what type of resource changed.
         * @param libraryModified <code>true</code> if the changeFlags are not for the project
         * associated with the listener, but instead correspond to a library.
         */
        void reloadLayout(ChangeFlags flags, boolean libraryModified);
    }

    /**
     * Returns the single instance of {@link LayoutReloadMonitor}.
     */
    public static LayoutReloadMonitor getMonitor() {
        return sThis;
    }

    private LayoutReloadMonitor() {
        // listen to resource changes. Used for non-layout resource (trigger a redraw), or
        // any resource folder (trigger a locale list refresh)
        ResourceManager.getInstance().addListener(mResourceListener);

        // also listen for .class file changed in case the layout has custom view classes.
        GlobalProjectMonitor monitor = GlobalProjectMonitor.getMonitor();
        monitor.addFileListener(mFileListener,
                IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED);

        monitor.addResourceEventListener(mResourceEventListener);
    }

    /**
     * Adds a listener for a given {@link IProject}.
     * @param project
     * @param listener
     */
    public void addListener(IProject project, ILayoutReloadListener listener) {
        synchronized (mListenerMap) {
            List<ILayoutReloadListener> list = mListenerMap.get(project);
            if (list == null) {
                list = new ArrayList<ILayoutReloadListener>();
                mListenerMap.put(project, list);
            }

            list.add(listener);
        }
    }

    /**
     * Removes a listener for a given {@link IProject}.
     */
    public void removeListener(IProject project, ILayoutReloadListener listener) {
        synchronized (mListenerMap) {
            List<ILayoutReloadListener> list = mListenerMap.get(project);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    /**
     * Removes a listener, no matter which {@link IProject} it was associated with.
     */
    public void removeListener(ILayoutReloadListener listener) {
        synchronized (mListenerMap) {

            for (List<ILayoutReloadListener> list : mListenerMap.values()) {
                Iterator<ILayoutReloadListener> it = list.iterator();
                while (it.hasNext()) {
                    ILayoutReloadListener i = it.next();
                    if (i == listener) {
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * Implementation of the {@link IFileListener} as an internal class so that the methods
     * do not appear in the public API of {@link LayoutReloadMonitor}.
     *
     * This is only to detect code and manifest change. Resource changes (located in res/)
     * is done through {@link #mResourceListener}.
     */
    private IFileListener mFileListener = new IFileListener() {
        /*
         * Callback for IFileListener. Called when a file changed.
         * This records the changes for each project, but does not notify listeners.
         */
        @Override
        public void fileChanged(@NonNull IFile file, @NonNull IMarkerDelta[] markerDeltas,
                int kind, @Nullable String extension, int flags, boolean isAndroidProject) {
            // This listener only cares about .class files and AndroidManifest.xml files
            if (!(SdkConstants.EXT_CLASS.equals(extension)
                    || SdkConstants.EXT_XML.equals(extension)
                        && SdkConstants.FN_ANDROID_MANIFEST_XML.equals(file.getName()))) {
                return;
            }

            // get the file's project
            IProject project = file.getProject();

            if (isAndroidProject) {
                // project is an Android project, it's the one being affected
                // directly by its own file change.
                processFileChanged(file, project, extension);
            } else {
                // check the projects depending on it, if they are Android project, update them.
                IProject[] referencingProjects = project.getReferencingProjects();

                for (IProject p : referencingProjects) {
                    try {
                        boolean hasAndroidNature = p.hasNature(AdtConstants.NATURE_DEFAULT);
                        if (hasAndroidNature) {
                            // the changed project is a dependency on an Android project,
                            // update the main project.
                            processFileChanged(file, p, extension);
                        }
                    } catch (CoreException e) {
                        // do nothing if the nature cannot be queried.
                    }
                }
            }
        }

        /**
         * Processes a file change for a given project which may or may not be the file's project.
         * @param file the changed file
         * @param project the project impacted by the file change.
         */
        private void processFileChanged(IFile file, IProject project, String extension) {
            // if this project has already been marked as modified, we do nothing.
            ChangeFlags changeFlags = mProjectFlags.get(project);
            if (changeFlags != null && changeFlags.isAllTrue()) {
                return;
            }

            // here we only care about code change (so change for .class files).
            // Resource changes is handled by the IResourceListener.
            if (SdkConstants.EXT_CLASS.equals(extension)) {
                if (file.getName().matches("R[\\$\\.](.*)")) {
                    // this is a R change!
                    if (changeFlags == null) {
                        changeFlags = new ChangeFlags();
                        mProjectFlags.put(project, changeFlags);
                    }

                    changeFlags.rClass = true;
                } else {
                    // this is a code change!
                    if (changeFlags == null) {
                        changeFlags = new ChangeFlags();
                        mProjectFlags.put(project, changeFlags);
                    }

                    changeFlags.code = true;
                }
            } else if (SdkConstants.FN_ANDROID_MANIFEST_XML.equals(file.getName()) &&
                    file.getParent().equals(project)) {
                // this is a manifest change!
                if (changeFlags == null) {
                    changeFlags = new ChangeFlags();
                    mProjectFlags.put(project, changeFlags);
                }

                changeFlags.manifest = true;
            }
        }
    };

    /**
     * Implementation of the {@link IResourceEventListener} as an internal class so that the methods
     * do not appear in the public API of {@link LayoutReloadMonitor}.
     */
    private IResourceEventListener mResourceEventListener = new IResourceEventListener() {
        /*
         * Callback for ResourceMonitor.IResourceEventListener. Called at the beginning of a
         * resource change event. This is called once, while fileChanged can be
         * called several times.
         *
         */
        @Override
        public void resourceChangeEventStart() {
            // nothing to be done here, it all happens in the resourceChangeEventEnd
        }

        /*
         * Callback for ResourceMonitor.IResourceEventListener. Called at the end of a resource
         * change event. This is where we notify the listeners.
         */
        @Override
        public void resourceChangeEventEnd() {
            // for each IProject that was changed, we notify all the listeners.
            for (Entry<IProject, ChangeFlags> entry : mProjectFlags.entrySet()) {
                IProject project = entry.getKey();

                // notify the project itself.
                notifyForProject(project, entry.getValue(), false);

                // check if the project is a library, and if it is search for what other
                // project depends on this one (directly or not)
                ProjectState state = Sdk.getProjectState(project);
                if (state != null && state.isLibrary()) {
                    Set<ProjectState> mainProjects = Sdk.getMainProjectsFor(project);
                    for (ProjectState mainProject : mainProjects) {
                        // always give the changeflag of the modified project.
                        notifyForProject(mainProject.getProject(), entry.getValue(), true);
                    }
                }
            }

            // empty the list.
            mProjectFlags.clear();
        }

        /**
         * Notifies the listeners for a given project.
         * @param project the project for which the listeners must be notified
         * @param flags the change flags to pass to the listener
         * @param libraryChanged a flag indicating if the change flags are for the give project,
         * or if they are for a library dependency.
         */
        private void notifyForProject(IProject project, ChangeFlags flags,
                boolean libraryChanged) {
            synchronized (mListenerMap) {
                List<ILayoutReloadListener> listeners = mListenerMap.get(project);

                if (listeners != null) {
                    for (ILayoutReloadListener listener : listeners) {
                        try {
                            listener.reloadLayout(flags, libraryChanged);
                        } catch (Throwable t) {
                            AdtPlugin.log(t, "Failed to call ILayoutReloadListener.reloadLayout");
                        }
                    }
                }
            }
        }
    };

    /**
     * Implementation of the {@link IResourceListener} as an internal class so that the methods
     * do not appear in the public API of {@link LayoutReloadMonitor}.
     */
    private IResourceListener mResourceListener = new IResourceListener() {

        @Override
        public void folderChanged(IProject project, ResourceFolder folder, int eventType) {
            // if this project has already been marked as modified, we do nothing.
            ChangeFlags changeFlags = mProjectFlags.get(project);
            if (changeFlags != null && changeFlags.isAllTrue()) {
                return;
            }

            // this means a new resource folder was added or removed, which can impact the
            // locale list.
            if (changeFlags == null) {
                changeFlags = new ChangeFlags();
                mProjectFlags.put(project, changeFlags);
            }

            changeFlags.localeList = true;
        }

        @Override
        public void fileChanged(IProject project, ResourceFile file, int eventType) {
            // if this project has already been marked as modified, we do nothing.
            ChangeFlags changeFlags = mProjectFlags.get(project);
            if (changeFlags != null && changeFlags.isAllTrue()) {
                return;
            }

            // now check that the file is *NOT* a layout file (those automatically trigger a layout
            // reload and we don't want to do it twice.)
            Collection<ResourceType> resTypes = file.getResourceTypes();

            // it's unclear why but there has been cases of resTypes being empty!
            if (resTypes.size() > 0) {
                // this is a resource change, that may require a layout redraw!
                if (changeFlags == null) {
                    changeFlags = new ChangeFlags();
                    mProjectFlags.put(project, changeFlags);
                }

                changeFlags.resources = true;
            }
        }
    };
}
