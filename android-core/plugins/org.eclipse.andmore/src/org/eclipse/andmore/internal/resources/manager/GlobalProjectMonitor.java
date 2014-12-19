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

package org.eclipse.andmore.internal.resources.manager;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;

/**
 * The Global Project Monitor tracks project file changes, and forward them to simple project,
 * file, and folder listeners.
 * Those listeners can be setup with masks to listen to particular events.
 * <p/>
 * To track project resource changes, use the monitor in the {@link ResourceManager}. It is more
 * efficient and while the global ProjectMonitor can track any file, deleted resource files
 * cannot be matched to previous {@link ResourceFile} or {@link ResourceFolder} objects by the
 * time the listeners get the event notifications.
 *
 * @see IProjectListener
 * @see IFolderListener
 * @see IFileListener
 */
public final class GlobalProjectMonitor {

    private final static GlobalProjectMonitor sThis = new GlobalProjectMonitor();

    /**
     * Classes which implement this interface provide a method that deals
     * with file change events.
     */
    public interface IFileListener {
        /**
         * Sent when a file changed.
         *
         * @param file The file that changed.
         * @param markerDeltas The marker deltas for the file.
         * @param kind The change kind. This is equivalent to
         *            {@link IResourceDelta#accept(IResourceDeltaVisitor)}
         * @param extension the extension of the file or null if the file does
         *            not have an extension
         * @param flags the {@link IResourceDelta#getFlags()} value with details
         *            on what changed in the file
         * @param isAndroidProject whether the parent project is an Android Project
         */
        public void fileChanged(@NonNull IFile file, @NonNull IMarkerDelta[] markerDeltas,
                int kind, @Nullable String extension, int flags, boolean isAndroidProject);
    }

    /**
     * Classes which implements this interface provide methods dealing with project events.
     */
    public interface IProjectListener {
        /**
         * Sent for each opened android project at the time the listener is put in place.
         * @param project the opened project.
         */
        public void projectOpenedWithWorkspace(IProject project);

        /**
         * Sent once after all Android projects have been opened,
         * at the time the listener is put in place.
         * <p/>
         * This is called after {@link #projectOpenedWithWorkspace(IProject)} has
         * been called on all known Android projects.
         */
        public void allProjectsOpenedWithWorkspace();

        /**
         * Sent when a project is opened.
         * @param project the project being opened.
         */
        public void projectOpened(IProject project);

        /**
         * Sent when a project is closed.
         * @param project the project being closed.
         */
        public void projectClosed(IProject project);

        /**
         * Sent when a project is deleted.
         * @param project the project about to be deleted.
         */
        public void projectDeleted(IProject project);

        /**
         * Sent when a project is renamed. During a project rename
         * {@link #projectDeleted(IProject)} and {@link #projectOpened(IProject)} are also called.
         * This is called last.
         *
         * @param project the new {@link IProject} object.
         * @param from the path of the project before the rename action.
         */
        public void projectRenamed(IProject project, IPath from);
    }

    /**
     * Classes which implement this interface provide a method that deals
     * with folder change events
     */
    public interface IFolderListener {
        /**
         * Sent when a folder changed.
         * @param folder The file that was changed
         * @param kind The change kind. This is equivalent to {@link IResourceDelta#getKind()}
         * @param isAndroidProject whether the parent project is an Android Project
         */
        public void folderChanged(IFolder folder, int kind, boolean isAndroidProject);
    }

    /**
     * Interface for a listener to be notified when resource change event starts and ends.
     */
    public interface IResourceEventListener {
        public void resourceChangeEventStart();
        public void resourceChangeEventEnd();
    }

    /**
     * Interface for a listener that gets passed the raw delta without processing.
     */
    public interface IRawDeltaListener {
        public void visitDelta(IResourceDelta delta);
    }

    /**
     * Base listener bundle to associate a listener to an event mask.
     */
    private static class ListenerBundle {
        /** Mask value to accept all events */
        public final static int MASK_NONE = -1;

        /**
         * Event mask. Values accepted are IResourceDelta.###
         * @see IResourceDelta#ADDED
         * @see IResourceDelta#REMOVED
         * @see IResourceDelta#CHANGED
         * @see IResourceDelta#ADDED_PHANTOM
         * @see IResourceDelta#REMOVED_PHANTOM
         * */
        int kindMask;
    }

    /**
     * Listener bundle for file event.
     */
    private static class FileListenerBundle extends ListenerBundle {

        /** The file listener */
        IFileListener listener;
    }

    /**
     * Listener bundle for folder event.
     */
    private static class FolderListenerBundle extends ListenerBundle {
        /** The file listener */
        IFolderListener listener;
    }

    private final ArrayList<FileListenerBundle> mFileListeners =
        new ArrayList<FileListenerBundle>();

    private final ArrayList<FolderListenerBundle> mFolderListeners =
        new ArrayList<FolderListenerBundle>();

    private final ArrayList<IProjectListener> mProjectListeners = new ArrayList<IProjectListener>();

    private final ArrayList<IResourceEventListener> mEventListeners =
        new ArrayList<IResourceEventListener>();

    private final ArrayList<IRawDeltaListener> mRawDeltaListeners =
        new ArrayList<IRawDeltaListener>();

    private IWorkspace mWorkspace;

    private boolean mIsAndroidProject;

    /**
     * Delta visitor for resource changes.
     */
    private final class DeltaVisitor implements IResourceDeltaVisitor {

        @Override
        public boolean visit(IResourceDelta delta) {
            // Find the other resource listeners to notify
            IResource r = delta.getResource();
            int type = r.getType();
            if (type == IResource.FILE) {
                int kind = delta.getKind();
                // notify the listeners.
                for (FileListenerBundle bundle : mFileListeners) {
                    if (bundle.kindMask == ListenerBundle.MASK_NONE
                            || (bundle.kindMask & kind) != 0) {
                        try {
                            bundle.listener.fileChanged((IFile)r, delta.getMarkerDeltas(), kind,
                                    r.getFileExtension(), delta.getFlags(), mIsAndroidProject);
                        } catch (Throwable t) {
                            AdtPlugin.log(t,"Failed to call IFileListener.fileChanged");
                        }
                    }
                }
                return false;
            } else if (type == IResource.FOLDER) {
                int kind = delta.getKind();
                // notify the listeners.
                for (FolderListenerBundle bundle : mFolderListeners) {
                    if (bundle.kindMask == ListenerBundle.MASK_NONE
                            || (bundle.kindMask & kind) != 0) {
                        try {
                            bundle.listener.folderChanged((IFolder)r, kind, mIsAndroidProject);
                        } catch (Throwable t) {
                            AdtPlugin.log(t,"Failed to call IFileListener.folderChanged");
                        }
                    }
                }
                return true;
            } else if (type == IResource.PROJECT) {
                IProject project = (IProject)r;

                try {
                    mIsAndroidProject = project.hasNature(AdtConstants.NATURE_DEFAULT);
                } catch (CoreException e) {
                    // this can only happen if the project does not exist or is not open, neither
                    // of which can happen here since we are processing changes in the project
                    // or at worst a project post-open event.
                    return false;
                }

                if (mIsAndroidProject == false) {
                    // for non android project, skip the project listeners but return true
                    // to visit the children and notify the IFileListeners
                    return true;
                }

                int flags = delta.getFlags();

                if ((flags & IResourceDelta.OPEN) != 0) {
                    // the project is opening or closing.

                    if (project.isOpen()) {
                        // notify the listeners.
                        for (IProjectListener pl : mProjectListeners) {
                            try {
                                pl.projectOpened(project);
                            } catch (Throwable t) {
                                AdtPlugin.log(t,"Failed to call IProjectListener.projectOpened");
                            }
                        }
                    } else {
                        // notify the listeners.
                        for (IProjectListener pl : mProjectListeners) {
                            try {
                                pl.projectClosed(project);
                            } catch (Throwable t) {
                                AdtPlugin.log(t,"Failed to call IProjectListener.projectClosed");
                            }
                        }
                    }

                    if ((flags & IResourceDelta.MOVED_FROM) != 0) {
                        IPath from = delta.getMovedFromPath();
                        // notify the listeners.
                        for (IProjectListener pl : mProjectListeners) {
                            try {
                                pl.projectRenamed(project, from);
                            } catch (Throwable t) {
                                AdtPlugin.log(t,"Failed to call IProjectListener.projectRenamed");
                            }
                        }
                    }
                }
            }

            return true;
        }
    }

    public static GlobalProjectMonitor getMonitor() {
        return sThis;
    }


    /**
     * Starts the resource monitoring.
     * @param ws The current workspace.
     * @return The monitor object.
     */
    public static GlobalProjectMonitor startMonitoring(IWorkspace ws) {
        if (sThis != null) {
            ws.addResourceChangeListener(sThis.mResourceChangeListener,
                    IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
            sThis.mWorkspace = ws;
        }
        return sThis;
    }

    /**
     * Stops the resource monitoring.
     * @param ws The current workspace.
     */
    public static void stopMonitoring(IWorkspace ws) {
        if (sThis != null) {
            ws.removeResourceChangeListener(sThis.mResourceChangeListener);

            synchronized (sThis) {
                sThis.mFileListeners.clear();
                sThis.mProjectListeners.clear();
            }
        }
    }

    /**
     * Adds a file listener.
     * @param listener The listener to receive the events.
     * @param kindMask The event mask to filter out specific events.
     * {@link ListenerBundle#MASK_NONE} will forward all events.
     * See {@link ListenerBundle#kindMask} for more values.
     */
    public synchronized void addFileListener(IFileListener listener, int kindMask) {
        FileListenerBundle bundle = new FileListenerBundle();
        bundle.listener = listener;
        bundle.kindMask = kindMask;

        mFileListeners.add(bundle);
    }

    /**
     * Removes an existing file listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeFileListener(IFileListener listener) {
        for (int i = 0 ; i < mFileListeners.size() ; i++) {
            FileListenerBundle bundle = mFileListeners.get(i);
            if (bundle.listener == listener) {
                mFileListeners.remove(i);
                return;
            }
        }
    }

    /**
     * Adds a folder listener.
     * @param listener The listener to receive the events.
     * @param kindMask The event mask to filter out specific events.
     * {@link ListenerBundle#MASK_NONE} will forward all events.
     * See {@link ListenerBundle#kindMask} for more values.
     */
    public synchronized void addFolderListener(IFolderListener listener, int kindMask) {
        FolderListenerBundle bundle = new FolderListenerBundle();
        bundle.listener = listener;
        bundle.kindMask = kindMask;

        mFolderListeners.add(bundle);
    }

    /**
     * Removes an existing folder listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeFolderListener(IFolderListener listener) {
        for (int i = 0 ; i < mFolderListeners.size() ; i++) {
            FolderListenerBundle bundle = mFolderListeners.get(i);
            if (bundle.listener == listener) {
                mFolderListeners.remove(i);
                return;
            }
        }
    }

    /**
     * Adds a project listener.
     * @param listener The listener to receive the events.
     */
    public synchronized void addProjectListener(IProjectListener listener) {
        mProjectListeners.add(listener);

        // we need to look at the opened projects and give them to the listener.

        // get the list of opened android projects.
        IWorkspaceRoot workspaceRoot = mWorkspace.getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);
        IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects(javaModel,
                null /*filter*/);


        notifyResourceEventStart();

        for (IJavaProject androidProject : androidProjects) {
            listener.projectOpenedWithWorkspace(androidProject.getProject());
        }

        listener.allProjectsOpenedWithWorkspace();

        notifyResourceEventEnd();
    }

    /**
     * Removes an existing project listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeProjectListener(IProjectListener listener) {
        mProjectListeners.remove(listener);
    }

    /**
     * Adds a resource event listener.
     * @param listener The listener to receive the events.
     */
    public synchronized void addResourceEventListener(IResourceEventListener listener) {
        mEventListeners.add(listener);
    }

    /**
     * Removes an existing Resource Event listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeResourceEventListener(IResourceEventListener listener) {
        mEventListeners.remove(listener);
    }

    /**
     * Adds a raw delta listener.
     * @param listener The listener to receive the deltas.
     */
    public synchronized void addRawDeltaListener(IRawDeltaListener listener) {
        mRawDeltaListeners.add(listener);
    }

    /**
     * Removes an existing Raw Delta listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeRawDeltaListener(IRawDeltaListener listener) {
        mRawDeltaListeners.remove(listener);
    }

    private void notifyResourceEventStart() {
        for (IResourceEventListener listener : mEventListeners) {
            try {
                listener.resourceChangeEventStart();
            } catch (Throwable t) {
                AdtPlugin.log(t,"Failed to call IResourceEventListener.resourceChangeEventStart");
            }
        }
    }

    private void notifyResourceEventEnd() {
        for (IResourceEventListener listener : mEventListeners) {
            try {
                listener.resourceChangeEventEnd();
            } catch (Throwable t) {
                AdtPlugin.log(t,"Failed to call IResourceEventListener.resourceChangeEventEnd");
            }
        }
    }

    private IResourceChangeListener mResourceChangeListener = new IResourceChangeListener() {
        /**
         * Processes the workspace resource change events.
         *
         * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
         */
        @Override
        public synchronized void resourceChanged(IResourceChangeEvent event) {
            // notify the event listeners of a start.
            notifyResourceEventStart();

            if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
                // a project is being deleted. Lets get the project object and remove
                // its compiled resource list.
                IResource r = event.getResource();
                IProject project = r.getProject();

                // notify the listeners.
                for (IProjectListener pl : mProjectListeners) {
                    try {
                        if (project.hasNature(AdtConstants.NATURE_DEFAULT)) {
                            try {
                                pl.projectDeleted(project);
                            } catch (Throwable t) {
                                AdtPlugin.log(t,"Failed to call IProjectListener.projectDeleted");
                            }
                        }
                    } catch (CoreException e) {
                        // just ignore this project.
                    }
                }
            } else {
                // this a regular resource change. We get the delta and go through it with a visitor.
                IResourceDelta delta = event.getDelta();

                // notify the raw delta listeners
                for (IRawDeltaListener listener : mRawDeltaListeners) {
                    listener.visitDelta(delta);
                }

                DeltaVisitor visitor = new DeltaVisitor();
                try {
                    delta.accept(visitor);
                } catch (CoreException e) {
                }
            }

            // we're done, notify the event listeners.
            notifyResourceEventEnd();
        }
    };
}
