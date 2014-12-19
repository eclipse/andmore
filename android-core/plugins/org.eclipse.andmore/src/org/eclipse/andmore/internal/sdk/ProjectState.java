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

package org.eclipse.andmore.internal.sdk;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Centralized state for Android Eclipse project.
 * <p>This gives raw access to the properties (from <code>project.properties</code>), as well
 * as direct access to target and library information.
 *
 * This also gives access to library information.
 *
 * {@link #isLibrary()} indicates if the project is a library.
 * {@link #hasLibraries()} and {@link #getLibraries()} give access to the libraries through
 * instances of {@link LibraryState}. A {@link LibraryState} instance is a link between a main
 * project and its library. Theses instances are owned by the {@link ProjectState}.
 *
 * {@link #isMissingLibraries()} will indicate if the project has libraries that are not resolved.
 * Unresolved libraries are libraries that do not have any matching opened Eclipse project.
 * When there are missing libraries, the {@link LibraryState} instance for them will return null
 * for {@link LibraryState#getProjectState()}.
 *
 */
public final class ProjectState {

    /**
     * A class that represents a library linked to a project.
     * <p/>It does not represent the library uniquely. Instead the {@link LibraryState} is linked
     * to the main project which is accessible through {@link #getMainProjectState()}.
     * <p/>If a library is used by two different projects, then there will be two different
     * instances of {@link LibraryState} for the library.
     *
     * @see ProjectState#getLibrary(IProject)
     */
    public final class LibraryState {
        private String mRelativePath;
        private ProjectState mProjectState;
        private String mPath;

        private LibraryState(String relativePath) {
            mRelativePath = relativePath;
        }

        /**
         * Returns the {@link ProjectState} of the main project using this library.
         */
        public ProjectState getMainProjectState() {
            return ProjectState.this;
        }

        /**
         * Closes the library. This resets the IProject from this object ({@link #getProjectState()} will
         * return <code>null</code>), and updates the main project data so that the library
         * {@link IProject} object does not show up in the return value of
         * {@link ProjectState#getFullLibraryProjects()}.
         */
        public void close() {
            mProjectState.removeParentProject(getMainProjectState());
            mProjectState = null;
            mPath = null;

            getMainProjectState().updateFullLibraryList();
        }

        private void setRelativePath(String relativePath) {
            mRelativePath = relativePath;
        }

        private void setProject(ProjectState project) {
            mProjectState = project;
            mPath = project.getProject().getLocation().toOSString();
            mProjectState.addParentProject(getMainProjectState());

            getMainProjectState().updateFullLibraryList();
        }

        /**
         * Returns the relative path of the library from the main project.
         * <p/>This is identical to the value defined in the main project's project.properties.
         */
        public String getRelativePath() {
            return mRelativePath;
        }

        /**
         * Returns the {@link ProjectState} item for the library. This can be null if the project
         * is not actually opened in Eclipse.
         */
        public ProjectState getProjectState() {
            return mProjectState;
        }

        /**
         * Returns the OS-String location of the library project.
         * <p/>This is based on location of the Eclipse project that matched
         * {@link #getRelativePath()}.
         *
         * @return The project location, or null if the project is not opened in Eclipse.
         */
        public String getProjectLocation() {
            return mPath;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LibraryState) {
                // the only thing that's always non-null is the relative path.
                LibraryState objState = (LibraryState)obj;
                return mRelativePath.equals(objState.mRelativePath) &&
                        getMainProjectState().equals(objState.getMainProjectState());
            } else if (obj instanceof ProjectState || obj instanceof IProject) {
                return mProjectState != null && mProjectState.equals(obj);
            } else if (obj instanceof String) {
                return normalizePath(mRelativePath).equals(normalizePath((String) obj));
            }

            return false;
        }

        @Override
        public int hashCode() {
            return normalizePath(mRelativePath).hashCode();
        }
    }

    private final IProject mProject;
    private final ProjectProperties mProperties;
    private IAndroidTarget mTarget;
    private BuildToolInfo mBuildToolInfo;

    /**
     * list of libraries. Access to this list must be protected by
     * <code>synchronized(mLibraries)</code>, but it is important that such code do not call
     * out to other classes (especially those protected by {@link Sdk#getLock()}.)
     */
    private final ArrayList<LibraryState> mLibraries = new ArrayList<LibraryState>();
    /** Cached list of all IProject instances representing the resolved libraries, including
     * indirect dependencies. This must never be null. */
    private List<IProject> mLibraryProjects = Collections.emptyList();
    /**
     * List of parent projects. When this instance is a library ({@link #isLibrary()} returns
     * <code>true</code>) then this is filled with projects that depends on this project.
     */
    private final ArrayList<ProjectState> mParentProjects = new ArrayList<ProjectState>();

    ProjectState(IProject project, ProjectProperties properties) {
        if (project == null || properties == null) {
            throw new NullPointerException();
        }

        mProject = project;
        mProperties = properties;

        // load the libraries
        synchronized (mLibraries) {
            int index = 1;
            while (true) {
                String propName = ProjectProperties.PROPERTY_LIB_REF + Integer.toString(index++);
                String rootPath = mProperties.getProperty(propName);

                if (rootPath == null) {
                    break;
                }

                mLibraries.add(new LibraryState(convertPath(rootPath)));
            }
        }
    }

    public IProject getProject() {
        return mProject;
    }

    public ProjectProperties getProperties() {
        return mProperties;
    }

    public @Nullable String getProperty(@NonNull String name) {
        if (mProperties != null) {
            return mProperties.getProperty(name);
        }

        return null;
    }

    public void setTarget(IAndroidTarget target) {
        mTarget = target;
    }

    /**
     * Returns the project's target's hash string.
     * <p/>If {@link #getTarget()} returns a valid object, then this returns the value of
     * {@link IAndroidTarget#hashString()}.
     * <p/>Otherwise this will return the value of the property
     * {@link ProjectProperties#PROPERTY_TARGET} from {@link #getProperties()} (if valid).
     * @return the target hash string or null if not found.
     */
    public String getTargetHashString() {
        if (mTarget != null) {
            return mTarget.hashString();
        }

        return mProperties.getProperty(ProjectProperties.PROPERTY_TARGET);
    }

    public IAndroidTarget getTarget() {
        return mTarget;
    }

    public void setBuildToolInfo(BuildToolInfo buildToolInfo) {
        mBuildToolInfo = buildToolInfo;
    }

    public BuildToolInfo getBuildToolInfo() {
        return mBuildToolInfo;
    }

    /**
     * Returns the build tools version from the project's properties.
     * @return the value or null
     */
    @Nullable
    public String getBuildToolInfoVersion() {
        return mProperties.getProperty(ProjectProperties.PROPERTY_BUILD_TOOLS);
    }

    public boolean getRenderScriptSupportMode() {
        String supportModeValue = mProperties.getProperty(ProjectProperties.PROPERTY_RS_SUPPORT);
        if (supportModeValue != null) {
            return Boolean.parseBoolean(supportModeValue);
        }

        return false;
    }

    public static class LibraryDifference {
        public boolean removed = false;
        public boolean added = false;

        public boolean hasDiff() {
            return removed || added;
        }
    }

    /**
     * Reloads the content of the properties.
     * <p/>This also reset the reference to the target as it may have changed, therefore this
     * should be followed by a call to {@link Sdk#loadTarget(ProjectState)}.
     *
     * <p/>If the project libraries changes, they are updated to a certain extent.<br>
     * Removed libraries are removed from the state list, and added to the {@link LibraryDifference}
     * object that is returned so that they can be processed.<br>
     * Added libraries are added to the state (as new {@link LibraryState} objects), but their
     * IProject is not resolved. {@link ProjectState#needs(ProjectState)} should be called
     * afterwards to properly initialize the libraries.
     *
     * @return an instance of {@link LibraryDifference} describing the change in libraries.
     */
    public LibraryDifference reloadProperties() {
        mTarget = null;
        mProperties.reload();

        // compare/reload the libraries.

        // if the order change it won't impact the java part, so instead try to detect removed/added
        // libraries.

        LibraryDifference diff = new LibraryDifference();

        synchronized (mLibraries) {
            List<LibraryState> oldLibraries = new ArrayList<LibraryState>(mLibraries);
            mLibraries.clear();

            // load the libraries
            int index = 1;
            while (true) {
                String propName = ProjectProperties.PROPERTY_LIB_REF + Integer.toString(index++);
                String rootPath = mProperties.getProperty(propName);

                if (rootPath == null) {
                    break;
                }

                // search for a library with the same path (not exact same string, but going
                // to the same folder).
                String convertedPath = convertPath(rootPath);
                boolean found = false;
                for (int i = 0 ; i < oldLibraries.size(); i++) {
                    LibraryState libState = oldLibraries.get(i);
                    if (libState.equals(convertedPath)) {
                        // it's a match. move it back to mLibraries and remove it from the
                        // old library list.
                        found = true;
                        mLibraries.add(libState);
                        oldLibraries.remove(i);
                        break;
                    }
                }

                if (found == false) {
                    diff.added = true;
                    mLibraries.add(new LibraryState(convertedPath));
                }
            }

            // whatever's left in oldLibraries is removed.
            diff.removed = oldLibraries.size() > 0;

            // update the library with what IProjet are known at the time.
            updateFullLibraryList();
        }

        return diff;
    }

    /**
     * Returns the list of {@link LibraryState}.
     */
    public List<LibraryState> getLibraries() {
        synchronized (mLibraries) {
            return Collections.unmodifiableList(mLibraries);
        }
    }

    /**
     * Returns all the <strong>resolved</strong> library projects, including indirect dependencies.
     * The list is ordered to match the library priority order for resource processing with
     * <code>aapt</code>.
     * <p/>If some dependencies are not resolved (or their projects is not opened in Eclipse),
     * they will not show up in this list.
     * @return the resolved projects as an unmodifiable list. May be an empty.
     */
    public List<IProject> getFullLibraryProjects() {
        return mLibraryProjects;
    }

    /**
     * Returns whether this is a library project.
     */
    public boolean isLibrary() {
        String value = mProperties.getProperty(ProjectProperties.PROPERTY_LIBRARY);
        return value != null && Boolean.valueOf(value);
    }

    /**
     * Returns whether the project depends on one or more libraries.
     */
    public boolean hasLibraries() {
        synchronized (mLibraries) {
            return mLibraries.size() > 0;
        }
    }

    /**
     * Returns whether the project is missing some required libraries.
     */
    public boolean isMissingLibraries() {
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                if (state.getProjectState() == null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the {@link LibraryState} object for a given {@link IProject}.
     * </p>This can only return a non-null object if the link between the main project's
     * {@link IProject} and the library's {@link IProject} was done.
     *
     * @return the matching LibraryState or <code>null</code>
     *
     * @see #needs(ProjectState)
     */
    public LibraryState getLibrary(IProject library) {
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                ProjectState ps = state.getProjectState();
                if (ps != null && ps.getProject().equals(library)) {
                    return state;
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link LibraryState} object for a given <var>name</var>.
     * </p>This can only return a non-null object if the link between the main project's
     * {@link IProject} and the library's {@link IProject} was done.
     *
     * @return the matching LibraryState or <code>null</code>
     *
     * @see #needs(IProject)
     */
    public LibraryState getLibrary(String name) {
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                ProjectState ps = state.getProjectState();
                if (ps != null && ps.getProject().getName().equals(name)) {
                    return state;
                }
            }
        }

        return null;
    }


    /**
     * Returns whether a given library project is needed by the receiver.
     * <p/>If the library is needed, this finds the matching {@link LibraryState}, initializes it
     * so that it contains the library's {@link IProject} object (so that
     * {@link LibraryState#getProjectState()} does not return null) and then returns it.
     *
     * @param libraryProject the library project to check.
     * @return a non null object if the project is a library dependency,
     * <code>null</code> otherwise.
     *
     * @see LibraryState#getProjectState()
     */
    public LibraryState needs(ProjectState libraryProject) {
        // compute current location
        File projectFile = mProject.getLocation().toFile();

        // get the location of the library.
        File libraryFile = libraryProject.getProject().getLocation().toFile();

        // loop on all libraries and check if the path match
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                if (state.getProjectState() == null) {
                    File library = new File(projectFile, state.getRelativePath());
                    try {
                        File absPath = library.getCanonicalFile();
                        if (absPath.equals(libraryFile)) {
                            state.setProject(libraryProject);
                            return state;
                        }
                    } catch (IOException e) {
                        // ignore this library
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns whether the project depends on a given <var>library</var>
     * @param library the library to check.
     * @return true if the project depends on the library. This is not affected by whether the link
     * was done through {@link #needs(ProjectState)}.
     */
    public boolean dependsOn(ProjectState library) {
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                if (state != null && state.getProjectState() != null &&
                        library.getProject().equals(state.getProjectState().getProject())) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Updates a library with a new path.
     * <p/>This method acts both as a check and an action. If the project does not depend on the
     * given <var>oldRelativePath</var> then no action is done and <code>null</code> is returned.
     * <p/>If the project depends on the library, then the project is updated with the new path,
     * and the {@link LibraryState} for the library is returned.
     * <p/>Updating the project does two things:<ul>
     * <li>Update LibraryState with new relative path and new {@link IProject} object.</li>
     * <li>Update the main project's <code>project.properties</code> with the new relative path
     * for the changed library.</li>
     * </ul>
     *
     * @param oldRelativePath the old library path relative to this project
     * @param newRelativePath the new library path relative to this project
     * @param newLibraryState the new {@link ProjectState} object.
     * @return a non null object if the project depends on the library.
     *
     * @see LibraryState#getProjectState()
     */
    public LibraryState updateLibrary(String oldRelativePath, String newRelativePath,
            ProjectState newLibraryState) {
        // compute current location
        File projectFile = mProject.getLocation().toFile();

        // loop on all libraries and check if the path matches
        synchronized (mLibraries) {
            for (LibraryState state : mLibraries) {
                if (state.getProjectState() == null) {
                    try {
                        // oldRelativePath may not be the same exact string as the
                        // one in the project properties (trailing separator could be different
                        // for instance).
                        // Use java.io.File to deal with this and also do a platform-dependent
                        // path comparison
                        File library1 = new File(projectFile, oldRelativePath);
                        File library2 = new File(projectFile, state.getRelativePath());
                        if (library1.getCanonicalPath().equals(library2.getCanonicalPath())) {
                            // save the exact property string to replace.
                            String oldProperty = state.getRelativePath();

                            // then update the LibraryPath.
                            state.setRelativePath(newRelativePath);
                            state.setProject(newLibraryState);

                            // update the project.properties file
                            IStatus status = replaceLibraryProperty(oldProperty, newRelativePath);
                            if (status != null) {
                                if (status.getSeverity() != IStatus.OK) {
                                    // log the error somehow.
                                }
                            } else {
                                // This should not happen since the library wouldn't be here in the
                                // first place
                            }

                            // return the LibraryState object.
                            return state;
                        }
                    } catch (IOException e) {
                        // ignore this library
                    }
                }
            }
        }

        return null;
    }


    private void addParentProject(ProjectState parentState) {
        mParentProjects.add(parentState);
    }

    private void removeParentProject(ProjectState parentState) {
        mParentProjects.remove(parentState);
    }

    public List<ProjectState> getParentProjects() {
        return Collections.unmodifiableList(mParentProjects);
    }

    /**
     * Computes the transitive closure of projects referencing this project as a
     * library project
     *
     * @return a collection (in any order) of project states for projects that
     *         directly or indirectly include this project state's project as a
     *         library project
     */
    public Collection<ProjectState> getFullParentProjects() {
        Set<ProjectState> result = new HashSet<ProjectState>();
        addParentProjects(result, this);
        return result;
    }

    /** Adds all parent projects of the given project, transitively, into the given parent set */
    private static void addParentProjects(Set<ProjectState> parents, ProjectState state) {
        for (ProjectState s : state.mParentProjects) {
            if (!parents.contains(s)) {
                parents.add(s);
                addParentProjects(parents, s);
            }
        }
    }

    /**
     * Update the value of a library dependency.
     * <p/>This loops on all current dependency looking for the value to replace and then replaces
     * it.
     * <p/>This both updates the in-memory {@link #mProperties} values and on-disk
     * project.properties file.
     * @param oldValue the old value to replace
     * @param newValue the new value to set.
     * @return the status of the replacement. If null, no replacement was done (value not found).
     */
    private IStatus replaceLibraryProperty(String oldValue, String newValue) {
        int index = 1;
        while (true) {
            String propName = ProjectProperties.PROPERTY_LIB_REF + Integer.toString(index++);
            String rootPath = mProperties.getProperty(propName);

            if (rootPath == null) {
                break;
            }

            if (rootPath.equals(oldValue)) {
                // need to update the properties. Get a working copy to change it and save it on
                // disk since ProjectProperties is read-only.
                ProjectPropertiesWorkingCopy workingCopy = mProperties.makeWorkingCopy();
                workingCopy.setProperty(propName, newValue);
                try {
                    workingCopy.save();

                    // reload the properties with the new values from the disk.
                    mProperties.reload();
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, String.format(
                            "Failed to save %1$s for project %2$s",
                                    mProperties.getType() .getFilename(), mProject.getName()),
                            e);

                }
                return Status.OK_STATUS;
            }
        }

        return null;
    }

    /**
     * Update the full library list, including indirect dependencies. The result is returned by
     * {@link #getFullLibraryProjects()}.
     */
    void updateFullLibraryList() {
        ArrayList<IProject> list = new ArrayList<IProject>();
        synchronized (mLibraries) {
            buildFullLibraryDependencies(mLibraries, list);
        }

        mLibraryProjects = Collections.unmodifiableList(list);
    }

    /**
     * Resolves a given list of libraries, finds out if they depend on other libraries, and
     * returns a full list of all the direct and indirect dependencies in the proper order (first
     * is higher priority when calling aapt).
     * @param inLibraries the libraries to resolve
     * @param outLibraries where to store all the libraries.
     */
    private void buildFullLibraryDependencies(List<LibraryState> inLibraries,
            ArrayList<IProject> outLibraries) {
        // loop in the inverse order to resolve dependencies on the libraries, so that if a library
        // is required by two higher level libraries it can be inserted in the correct place
        for (int i = inLibraries.size() - 1  ; i >= 0 ; i--) {
            LibraryState library = inLibraries.get(i);

            // get its libraries if possible
            ProjectState libProjectState = library.getProjectState();
            if (libProjectState != null) {
                List<LibraryState> dependencies = libProjectState.getLibraries();

                // build the dependencies for those libraries
                buildFullLibraryDependencies(dependencies, outLibraries);

                // and add the current library (if needed) in front (higher priority)
                if (outLibraries.contains(libProjectState.getProject()) == false) {
                    outLibraries.add(0, libProjectState.getProject());
                }
            }
        }
    }


    /**
     * Converts a path containing only / by the proper platform separator.
     */
    private String convertPath(String path) {
        return path.replaceAll("/", Matcher.quoteReplacement(File.separator)); //$NON-NLS-1$
    }

    /**
     * Normalizes a relative path.
     */
    private String normalizePath(String path) {
        path = convertPath(path);
        if (path.endsWith("/")) { //$NON-NLS-1$
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProjectState) {
            return mProject.equals(((ProjectState) obj).mProject);
        } else if (obj instanceof IProject) {
            return mProject.equals(obj);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mProject.hashCode();
    }

    @Override
    public String toString() {
        return mProject.getName();
    }
}
