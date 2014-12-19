/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.exportgradle;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.ProjectState.LibraryState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to setup the project and its modules.
 */
public class ProjectSetupBuilder {

    private final static class InternalException extends Exception {
        private static final long serialVersionUID = 1L;

        InternalException(String message) {
            super(message);
        }
    }

    private boolean mCanFinish = false;
    private boolean mCanGenerate = false;
    private final List<GradleModule> mOriginalModules = Lists.newArrayList();
    private final Map<IJavaProject, GradleModule> mModules = Maps.newHashMap();
    private IPath mCommonRoot;
    private ExportStatus mStatus;

    public ProjectSetupBuilder() {

    }

    public void setCanGenerate(boolean generate) {
        mCanGenerate = generate;
    }

    public void setCanFinish(boolean canFinish) {
        mCanFinish = canFinish;
    }

    public boolean canFinish() {
        return mCanFinish;
    }

    public boolean canGenerate() {
        return mCanGenerate;
    }

    public void setStatus(ExportStatus status) {
        mStatus = status;
    }

    public ExportStatus getStatus() {
        return mStatus;
    }

    @NonNull
    public String setProject(@NonNull List<IJavaProject> selectedProjects)
            throws CoreException {
        mModules.clear();

        // build a list of all projects that must be included. This is in case
        // some dependencies have not been included in the selected projects. We also include
        // parent projects so that the full multi-project setup is correct.
        // Note that if two projects are selected that are not related, both will be added
        // in the same multi-project anyway.
        try {
            for (IJavaProject javaProject : selectedProjects) {
                GradleModule module;

                if (javaProject.getProject().hasNature(AdtConstants.NATURE_DEFAULT)) {
                    module = processAndroidProject(javaProject);
                } else {
                    module = processJavaProject(javaProject);
                }

                mOriginalModules.add(module);
            }

            Collection<GradleModule> modules = mModules.values();
            computeRootAndPaths(modules);

            return null;
        } catch (InternalException e) {
            return e.getMessage();
        }
    }

    @NonNull
    public Collection<GradleModule> getModules() {
        return mModules.values();
    }

    public int getModuleCount() {
        return mModules.size();
    }

    @Nullable
    public IPath getCommonRoot() {
        return mCommonRoot;
    }

    @Nullable
    public GradleModule getModule(IJavaProject javaProject) {
        return mModules.get(javaProject);
    }

    public boolean isOriginalProject(@NonNull IJavaProject javaProject) {
        GradleModule module = mModules.get(javaProject);
        return mOriginalModules.contains(module);
    }

    @NonNull
    public List<GradleModule> getOriginalModules() {
        return mOriginalModules;
    }

    @Nullable
    public List<GradleModule> getShortestDependencyTo(GradleModule module) {
        return findModule(module, mOriginalModules);
    }

    @Nullable
    public List<GradleModule> findModule(GradleModule toFind, GradleModule rootModule) {
        if (toFind == rootModule) {
            List<GradleModule> list = Lists.newArrayList();
            list.add(toFind);
            return list;
        }

        List<GradleModule> shortestChain = findModule(toFind, rootModule.getDependencies());

        if (shortestChain != null) {
            shortestChain.add(0, rootModule);
        }

        return shortestChain;
    }

    @Nullable
    public List<GradleModule> findModule(GradleModule toFind, List<GradleModule> modules) {
        List<GradleModule> currentChain = null;

        for (GradleModule child : modules) {
            List<GradleModule> newChain = findModule(toFind, child);
            if (currentChain == null) {
                currentChain = newChain;
            } else if (newChain != null) {
                if (currentChain.size() > newChain.size()) {
                    currentChain = newChain;
                }
            }
        }

        return currentChain;
    }

    @NonNull
    private GradleModule processAndroidProject(@NonNull IJavaProject javaProject)
            throws InternalException, CoreException {

        // get/create the module
        GradleModule module = createModuleOnDemand(javaProject);
        if (module.isConfigured()) {
            return module;
        }

        module.setType(GradleModule.Type.ANDROID);

        ProjectState projectState = Sdk.getProjectState(javaProject.getProject());
        assert projectState != null;

        // add library project dependencies
        List<LibraryState> libraryProjects = projectState.getLibraries();
        for (LibraryState libraryState : libraryProjects) {
            ProjectState libProjectState = libraryState.getProjectState();
            if (libProjectState != null) {
                IJavaProject javaLib = getJavaProject(libProjectState);
                if (javaLib != null) {
                    GradleModule libModule = processAndroidProject(javaLib);
                    module.addDependency(libModule);
                } else {
                    throw new InternalException(String.format(
                            "Project %1$s is missing. Needed by %2$s.\n" +
                            "Make sure all dependencies are opened.",
                            libraryState.getRelativePath(),
                            javaProject.getProject().getName()));
                }
            } else {
                throw new InternalException(String.format(
                        "Project %1$s is missing. Needed by %2$s.\n" +
                        "Make sure all dependencies are opened.",
                        libraryState.getRelativePath(),
                        javaProject.getProject().getName()));
            }
        }

        // add java project dependencies
        List<IJavaProject> javaDepProjects = getReferencedProjects(javaProject);
        for (IJavaProject javaDep : javaDepProjects) {
            GradleModule libModule = processJavaProject(javaDep);
            module.addDependency(libModule);
        }

        return module;
    }

    @NonNull
    private GradleModule processJavaProject(@NonNull IJavaProject javaProject)
            throws InternalException, CoreException {
        // get/create the module
        GradleModule module = createModuleOnDemand(javaProject);

        if (module.isConfigured()) {
            return module;
        }

        module.setType(GradleModule.Type.JAVA);

        // add java project dependencies
        List<IJavaProject> javaDepProjects = getReferencedProjects(javaProject);
        for (IJavaProject javaDep : javaDepProjects) {
            // Java project should not reference Android project!
            if (javaDep.getProject().hasNature(AdtConstants.NATURE_DEFAULT)) {
                throw new InternalException(String.format(
                        "Java project %1$s depends on Android project %2$s!\n" +
                        "This is not a valid dependency",
                        javaProject.getProject().getName(), javaDep.getProject().getName()));
            }
            GradleModule libModule = processJavaProject(javaDep);
            module.addDependency(libModule);
        }

        return module;
    }

    private void computeRootAndPaths(Collection<GradleModule> modules) throws InternalException {
        // compute the common root.
        mCommonRoot = determineCommonRoot(modules);

        // compute all the relative paths.
        for (GradleModule module : modules) {
            String path = getGradlePath(module.getJavaProject().getProject().getLocation(),
                    mCommonRoot);

            module.setPath(path);
        }
    }

    /**
     * Finds the common parent directory shared by this project and all its dependencies.
     * If there's only one project, returns the single project's folder.
     * @throws InternalException
     */
    @NonNull
    private static IPath determineCommonRoot(Collection<GradleModule> modules)
            throws InternalException {
        IPath commonRoot = null;
        for (GradleModule module : modules) {
            if (commonRoot == null) {
                commonRoot = module.getJavaProject().getProject().getLocation();
            } else {
                commonRoot = findCommonRoot(commonRoot,
                        module.getJavaProject().getProject().getLocation());
            }
        }

        return commonRoot;
    }

    /**
     * Converts the given path to be relative to the given root path, and converts it to
     * Gradle project notation, such as is used in the settings.gradle file.
     */
    @NonNull
    private static String getGradlePath(IPath path, IPath root) {
        IPath relativePath = path.makeRelativeTo(root);
        String relativeString = relativePath.toOSString();
        return ":" + relativeString.replaceAll(Pattern.quote(File.separator), ":"); //$NON-NLS-1$
    }

    /**
     * Given two IPaths, finds the parent directory of both of them.
     * @throws InternalException
     */
    @NonNull
    private static IPath findCommonRoot(@NonNull IPath path1, @NonNull IPath path2)
            throws InternalException {
        if (path1.getDevice() != null && !path1.getDevice().equals(path2.getDevice())) {
            throw new InternalException(
                    "Different modules have been detected on different drives.\n" +
                    "This prevents finding a common root to all modules.");
        }

        IPath result = path1.uptoSegment(0);

        final int count = Math.min(path1.segmentCount(), path2.segmentCount());
        for (int i = 0; i < count; i++) {
            if (path1.segment(i).equals(path2.segment(i))) {
                result = result.append(IPath.SEPARATOR + path2.segment(i));
            }
        }
        return result;
    }

    @Nullable
    private IJavaProject getJavaProject(ProjectState projectState) {
        try {
            return BaseProjectHelper.getJavaProject(projectState.getProject());
        } catch (CoreException e) {
            return null;
        }
    }

    @NonNull
    private GradleModule createModuleOnDemand(@NonNull IJavaProject javaProject) {
        GradleModule module = mModules.get(javaProject);
        if (module == null) {
            module = new GradleModule(javaProject);
            mModules.put(javaProject, module);
        }

        return module;
    }

    @NonNull
    private static List<IJavaProject> getReferencedProjects(IJavaProject javaProject)
            throws JavaModelException, InternalException {

        List<IJavaProject> projects = Lists.newArrayList();

        IClasspathEntry entries[] = javaProject.getRawClasspath();
        for (IClasspathEntry classpathEntry : entries) {
            if (classpathEntry.getContentKind() == IPackageFragmentRoot.K_SOURCE
                    && classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                // found required project on build path
                String subProjectRoot = classpathEntry.getPath().toString();
                IJavaProject subProject = getJavaProject(subProjectRoot);
                // is project available in workspace?
                if (subProject != null) {
                    projects.add(subProject);
                } else {
                    throw new InternalException(String.format(
                            "Project '%s' is missing project dependency '%s' in Eclipse workspace.\n" +
                            "Make sure all dependencies are opened.",
                            javaProject.getProject().getName(),
                            classpathEntry.getPath().toString()));
                }
            }
        }

        return projects;
    }

    /**
     * Get Java project for given root.
     */
    @Nullable
    private static IJavaProject getJavaProject(String root) {
        IPath path = new Path(root);
        if (path.segmentCount() == 1) {
            return getJavaProjectByName(root);
        }
        IResource resource = ResourcesPlugin.getWorkspace().getRoot()
                .findMember(path);
        if (resource != null && resource.getType() == IResource.PROJECT) {
            if (resource.exists()) {
                return (IJavaProject) JavaCore.create(resource);
            }
        }
        return null;
    }

    /**
     * Get Java project from resource.
     */
    private static IJavaProject getJavaProjectByName(String name) {
        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
            if (project.exists()) {
                return JavaCore.create(project);
            }
        } catch (IllegalArgumentException iae) {
        }
        return null;
    }
}
