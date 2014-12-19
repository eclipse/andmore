/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore;

import org.eclipse.andmore.ddms.IDebuggerConnector;
import org.eclipse.andmore.internal.launch.AndroidLaunchController;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IProjectListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implementation of the com.android.ide.ddms.debuggerConnector extension point.
 */
public class DebuggerConnector implements IDebuggerConnector {
    private WorkspaceAppCache mWorkspaceAppCache;

    public DebuggerConnector() {
        mWorkspaceAppCache = new WorkspaceAppCache();
        GlobalProjectMonitor.getMonitor().addProjectListener(mWorkspaceAppCache);
    }

    @Override
    public boolean connectDebugger(String appName, int appPort, int selectedPort) {
        // search for an android project matching the process name
        IProject project = ProjectHelper.findAndroidProjectByAppName(appName);
        if (project != null) {
            AndroidLaunchController.debugRunningApp(project, appPort);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWorkspaceApp(String appName) {
        return mWorkspaceAppCache.isWorkspaceApp(appName);
    }

    /**
     * A cache of Android application name to workspace project name mappings.
     * Users can query whether an application is present in the workspace using
     * {@link #isWorkspaceApp(String)}. The cache listens to workspace changes
     * (project open/close/delete etc), and keeps its internal mappings up-to-date.<br>
     * This class is designed with the following assumptions:
     * <ul>
     *   <li> There are a significant number of calls to {@link #isWorkspaceApp(String)}, with
     *        a large number of possible application names as arguments. </li>
     *   <li> The number of projects actually present in the workspace, and the
     *        number of user initiated project changes (project open/close/delete/rename) are both
     *        relatively small. </li>
     * </ul>
     */
    static class WorkspaceAppCache implements IProjectListener {
        /** Apps known to be not in the workspace. */
        private Set<String> mAppsNotInWorkspace;

        /** Mapping of application name to project name for apps present in the workspace. */
        private Map<String, String> mAppsInWorkspace;

        public WorkspaceAppCache() {
            mAppsNotInWorkspace = new HashSet<String>();
            mAppsInWorkspace = new HashMap<String, String>();
        }

        public boolean isWorkspaceApp(String appName) {
            if (mAppsNotInWorkspace.contains(appName)) {
                return false;
            }

            String projectName = mAppsInWorkspace.get(appName);
            if (projectName == null) {
                IProject p = ProjectHelper.findAndroidProjectByAppName(appName);
                if (p == null) {
                    mAppsNotInWorkspace.add(appName);
                } else {
                    projectName = p.getName();
                    mAppsInWorkspace.put(appName, projectName);
                }
            }
            return projectName != null;
        }

        /** {@inheritDoc} */
        @Override
        public void projectRenamed(IProject project, IPath from) {
            // when a project is renamed, ideally we should just update the current
            // known mapping of app name -> project name. However, the projectRenamed
            // callback is called only after projectDeleted() and projectOpened() are called,
            // so there is really nothing we can do here..
        }

        /** {@inheritDoc} */
        @Override
        public void projectOpenedWithWorkspace(IProject project) {
            // don't do anything as the cache is lazily initialized
        }

        @Override
        public void allProjectsOpenedWithWorkspace() {
            // don't do anything as the cache is lazily initialized
        }

        /** {@inheritDoc} */
        @Override
        public void projectOpened(IProject project) {
            // A newly opened project could contribute some Android application.
            // So we invalidate the set of apps that are known to be not in the workspace, as
            // that set could be out of date now.
            mAppsNotInWorkspace.clear();
        }

        /** {@inheritDoc} */
        @Override
        public void projectDeleted(IProject project) {
            // Deletion is effectively the same as closing
            projectClosed(project);
        }

        /** {@inheritDoc} */
        @Override
        public void projectClosed(IProject project) {
            // When a project is closed, remove all mappings contributed by the project.
            Map<String, String> updatedCache = new HashMap<String, String>();

            String name = project.getName();
            for (Entry<String, String> e : mAppsInWorkspace.entrySet()) {
                if (!e.getValue().equals(name)) {
                    updatedCache.put(e.getKey(), e.getValue());
                }
            }

            mAppsInWorkspace = updatedCache;
        }
    }
}
