/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.internal.project;

import static org.eclipse.andmore.AdtConstants.CONTAINER_DEPENDENCIES;

import com.android.SdkConstants;
import com.android.ide.common.sdk.LoadStatus;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.build.JarListSanitizer;
import com.android.sdklib.build.JarListSanitizer.DifferentLibException;
import com.android.sdklib.build.JarListSanitizer.Sha1Exception;
import com.android.sdklib.build.RenderScriptProcessor;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AndroidPrintStream;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class LibraryClasspathContainerInitializer extends BaseClasspathContainerInitializer {

    private final static String ATTR_SRC = "src"; //$NON-NLS-1$
    private final static String ATTR_DOC = "doc"; //$NON-NLS-1$
    private final static String DOT_PROPERTIES = ".properties"; //$NON-NLS-1$

    public LibraryClasspathContainerInitializer() {
    }

    /**
     * Updates the {@link IJavaProject} objects with new library.
     * @param androidProjects the projects to update.
     * @return <code>true</code> if success, <code>false</code> otherwise.
     */
    public static boolean updateProjects(IJavaProject[] androidProjects) {
        try {
            // Allocate a new AndroidClasspathContainer, and associate it to the library
            // container id for each projects.
            int projectCount = androidProjects.length;

            IClasspathContainer[] libraryContainers = new IClasspathContainer[projectCount];
            IClasspathContainer[] dependencyContainers = new IClasspathContainer[projectCount];
            for (int i = 0 ; i < projectCount; i++) {
                libraryContainers[i] = allocateLibraryContainer(androidProjects[i]);
                dependencyContainers[i] = allocateDependencyContainer(androidProjects[i]);
            }

            // give each project their new container in one call.
            JavaCore.setClasspathContainer(
                    new Path(AdtConstants.CONTAINER_PRIVATE_LIBRARIES),
                    androidProjects, libraryContainers, new NullProgressMonitor());

            JavaCore.setClasspathContainer(
                    new Path(AdtConstants.CONTAINER_DEPENDENCIES),
                    androidProjects, dependencyContainers, new NullProgressMonitor());
            return true;
        } catch (JavaModelException e) {
            return false;
        }
    }

    /**
     * Updates the {@link IJavaProject} objects with new library.
     * @param androidProjects the projects to update.
     * @return <code>true</code> if success, <code>false</code> otherwise.
     */
    public static boolean updateProject(List<ProjectState> projects) {
        List<IJavaProject> javaProjectList = new ArrayList<IJavaProject>(projects.size());
        for (ProjectState p : projects) {
            IJavaProject javaProject = JavaCore.create(p.getProject());
            if (javaProject != null) {
                javaProjectList.add(javaProject);
            }
        }

        IJavaProject[] javaProjects = javaProjectList.toArray(
                new IJavaProject[javaProjectList.size()]);

        return updateProjects(javaProjects);
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (AdtConstants.CONTAINER_PRIVATE_LIBRARIES.equals(containerPath.toString())) {
            IClasspathContainer libraries = allocateLibraryContainer(project);
            if (libraries != null) {
                JavaCore.setClasspathContainer(new Path(AdtConstants.CONTAINER_PRIVATE_LIBRARIES),
                        new IJavaProject[] { project },
                        new IClasspathContainer[] { libraries },
                        new NullProgressMonitor());
            }

        } else if(AdtConstants.CONTAINER_DEPENDENCIES.equals(containerPath.toString())) {
            IClasspathContainer dependencies = allocateDependencyContainer(project);
            if (dependencies != null) {
                JavaCore.setClasspathContainer(new Path(AdtConstants.CONTAINER_DEPENDENCIES),
                        new IJavaProject[] { project },
                        new IClasspathContainer[] { dependencies },
                        new NullProgressMonitor());
            }
        }
    }

    private static IClasspathContainer allocateLibraryContainer(IJavaProject javaProject) {
        final IProject iProject = javaProject.getProject();

        // check if the project has a valid target.
        ProjectState state = Sdk.getProjectState(iProject);
        if (state == null) {
            // getProjectState should already have logged an error. Just bail out.
            return null;
        }

        /*
         * At this point we're going to gather a list of all that need to go in the
         * dependency container.
         * - Library project outputs (direct and indirect)
         * - Java project output (those can be indirectly referenced through library projects
         *   or other other Java projects)
         * - Jar files:
         *    + inside this project's libs/
         *    + inside the library projects' libs/
         *    + inside the referenced Java projects' classpath
         */
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

        // list of java project dependencies and jar files that will be built while
        // going through the library projects.
        Set<File> jarFiles = new HashSet<File>();
        Set<IProject> refProjects = new HashSet<IProject>();

        // process all the libraries

        List<IProject> libProjects = state.getFullLibraryProjects();
        for (IProject libProject : libProjects) {
            // process all of the library project's dependencies
            getDependencyListFromClasspath(libProject, refProjects, jarFiles, true);
        }

        // now process this projects' referenced projects only.
        processReferencedProjects(iProject, refProjects, jarFiles);

        // and the content of its libs folder
        getJarListFromLibsFolder(iProject, jarFiles);

        // now add a classpath entry for each Java project (this is a set so dups are already
        // removed)
        for (IProject p : refProjects) {
            entries.add(JavaCore.newProjectEntry(p.getFullPath(), true /*isExported*/));
        }

        entries.addAll(convertJarsToClasspathEntries(iProject, jarFiles));

        return allocateContainer(javaProject, entries, new Path(AdtConstants.CONTAINER_PRIVATE_LIBRARIES),
                "Android Private Libraries");
    }

    private static List<IClasspathEntry> convertJarsToClasspathEntries(final IProject iProject,
            Set<File> jarFiles) {
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(jarFiles.size());

        // and process the jar files list, but first sanitize it to remove dups.
        JarListSanitizer sanitizer = new JarListSanitizer(
                iProject.getFolder(SdkConstants.FD_OUTPUT).getLocation().toFile(),
                new AndroidPrintStream(iProject, null /*prefix*/,
                        AdtPlugin.getOutStream()));

        String errorMessage = null;

        try {
            List<File> sanitizedList = sanitizer.sanitize(jarFiles);

            for (File jarFile : sanitizedList) {
                if (jarFile instanceof CPEFile) {
                    CPEFile cpeFile = (CPEFile) jarFile;
                    IClasspathEntry e = cpeFile.getClasspathEntry();

                    entries.add(JavaCore.newLibraryEntry(
                            e.getPath(),
                            e.getSourceAttachmentPath(),
                            e.getSourceAttachmentRootPath(),
                            e.getAccessRules(),
                            e.getExtraAttributes(),
                            true /*isExported*/));
                } else {
                    String jarPath = jarFile.getAbsolutePath();

                    IPath sourceAttachmentPath = null;
                    IClasspathAttribute javaDocAttribute = null;

                    File jarProperties = new File(jarPath + DOT_PROPERTIES);
                    if (jarProperties.isFile()) {
                        Properties p = new Properties();
                        InputStream is = null;
                        try {
                            p.load(is = new FileInputStream(jarProperties));

                            String value = p.getProperty(ATTR_SRC);
                            if (value != null) {
                                File srcPath = getFile(jarFile, value);

                                if (srcPath.exists()) {
                                    sourceAttachmentPath = new Path(srcPath.getAbsolutePath());
                                }
                            }

                            value = p.getProperty(ATTR_DOC);
                            if (value != null) {
                                File docPath = getFile(jarFile, value);
                                if (docPath.exists()) {
                                    try {
                                        javaDocAttribute = JavaCore.newClasspathAttribute(
                                                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                                                docPath.toURI().toURL().toString());
                                    } catch (MalformedURLException e) {
                                        AdtPlugin.log(e, "Failed to process 'doc' attribute for %s",
                                                jarProperties.getAbsolutePath());
                                    }
                                }
                            }

                        } catch (FileNotFoundException e) {
                            // shouldn't happen since we check upfront
                        } catch (IOException e) {
                            AdtPlugin.log(e, "Failed to read %s", jarProperties.getAbsolutePath());
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        }
                    }

                    if (javaDocAttribute != null) {
                        entries.add(JavaCore.newLibraryEntry(new Path(jarPath),
                                sourceAttachmentPath, null /*sourceAttachmentRootPath*/,
                                new IAccessRule[0],
                                new IClasspathAttribute[] { javaDocAttribute },
                                true /*isExported*/));
                    } else {
                        entries.add(JavaCore.newLibraryEntry(new Path(jarPath),
                                sourceAttachmentPath, null /*sourceAttachmentRootPath*/,
                                true /*isExported*/));
                    }
                }
            }
        } catch (DifferentLibException e) {
            errorMessage = e.getMessage();
            AdtPlugin.printErrorToConsole(iProject, (Object[]) e.getDetails());
        } catch (Sha1Exception e) {
            errorMessage = e.getMessage();
        }

        processError(iProject, errorMessage, AdtConstants.MARKER_DEPENDENCY,
                true /*outputToConsole*/);

        return entries;
    }

    private static IClasspathContainer allocateDependencyContainer(IJavaProject javaProject) {
        final IProject iProject = javaProject.getProject();
        final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        final Set<File> jarFiles = new HashSet<File>();
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        AdtPlugin plugin = AdtPlugin.getDefault();
        if (plugin == null) { // This is totally weird, but I've seen it happen!
            return null;
        }

        synchronized (Sdk.getLock()) {
            boolean sdkIsLoaded = plugin.getSdkLoadStatus() == LoadStatus.LOADED;

            // check if the project has a valid target.
            final ProjectState state = Sdk.getProjectState(iProject);
            if (state == null) {
                // getProjectState should already have logged an error. Just bail out.
                return null;
            }

            // annotations support for older version of android
            if (state.getTarget() != null && state.getTarget().getVersion().getApiLevel() <= 15) {
                File annotationsJar = new File(Sdk.getCurrent().getSdkOsLocation(),
                        SdkConstants.FD_TOOLS + File.separator + SdkConstants.FD_SUPPORT +
                        File.separator + SdkConstants.FN_ANNOTATIONS_JAR);

                jarFiles.add(annotationsJar);
            }

            if (state.getRenderScriptSupportMode()) {
                if (!sdkIsLoaded) {
                    return null;
                }
                BuildToolInfo buildToolInfo = state.getBuildToolInfo();
                if (buildToolInfo == null) {
                    buildToolInfo = Sdk.getCurrent().getLatestBuildTool();

                    if (buildToolInfo == null) {
                        return null;
                    }
                }

                File renderScriptSupportJar = RenderScriptProcessor.getSupportJar(
                        buildToolInfo.getLocation().getAbsolutePath());

                jarFiles.add(renderScriptSupportJar);
            }

            // process all the libraries

            List<IProject> libProjects = state.getFullLibraryProjects();
            for (IProject libProject : libProjects) {
                // get the project output
                IFolder outputFolder = BaseProjectHelper.getAndroidOutputFolder(libProject);

                if (outputFolder != null) { // can happen when closing/deleting a library)
                    IFile jarIFile = outputFolder.getFile(libProject.getName().toLowerCase() +
                           SdkConstants.DOT_JAR);

                    // get the source folder for the library project
                    List<IPath> srcs = BaseProjectHelper.getSourceClasspaths(libProject);
                    // find the first non-derived source folder.
                    IPath sourceFolder = null;
                    for (IPath src : srcs) {
                        IFolder srcFolder = workspaceRoot.getFolder(src);
                        if (srcFolder.isDerived() == false) {
                            sourceFolder = src;
                            break;
                        }
                    }

                    // we can directly add a CPE for this jar as there's no risk of a duplicate.
                    IClasspathEntry entry = JavaCore.newLibraryEntry(
                            jarIFile.getLocation(),
                            sourceFolder, // source attachment path
                            null,         // default source attachment root path.
                            true /*isExported*/);

                    entries.add(entry);
                }
            }

            entries.addAll(convertJarsToClasspathEntries(iProject, jarFiles));

            return allocateContainer(javaProject, entries, new Path(CONTAINER_DEPENDENCIES),
                    "Android Dependencies");
        }
    }

    private static IClasspathContainer allocateContainer(IJavaProject javaProject,
            List<IClasspathEntry> entries, IPath id, String description) {

        if (AdtPlugin.getDefault() == null) { // This is totally weird, but I've seen it happen!
            return null;
        }

        // First check that the project has a library-type container.
        try {
            IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
            final IClasspathEntry[] oldRawClasspath = rawClasspath;

            boolean foundContainer = false;
            for (IClasspathEntry entry : rawClasspath) {
                // get the entry and kind
                final int kind = entry.getEntryKind();

                if (kind == IClasspathEntry.CPE_CONTAINER) {
                    String path = entry.getPath().toString();
                    String idString = id.toString();
                    if (idString.equals(path)) {
                        foundContainer = true;
                        break;
                    }
                }
            }

            // if there isn't any, add it.
            if (foundContainer == false) {
                // add the android container to the array
                rawClasspath = ProjectHelper.addEntryToClasspath(rawClasspath,
                        JavaCore.newContainerEntry(id, true /*isExported*/));
            }

            // set the new list of entries to the project
            if (rawClasspath != oldRawClasspath) {
                javaProject.setRawClasspath(rawClasspath, new NullProgressMonitor());
            }
        } catch (JavaModelException e) {
            // This really shouldn't happen, but if it does, simply return null (the calling
            // method will fails as well)
            return null;
        }

        return new AndroidClasspathContainer(
                entries.toArray(new IClasspathEntry[entries.size()]),
                id,
                description,
                IClasspathContainer.K_APPLICATION);
    }

    private static File getFile(File root, String value) {
        File file = new File(value);
        if (file.isAbsolute() == false) {
            file = new File(root.getParentFile(), value);
        }

        return file;
    }

    /**
     * Finds all the jar files inside a project's libs folder.
     * @param project
     * @param jarFiles
     */
    private static void getJarListFromLibsFolder(IProject project, Set<File> jarFiles) {
        IFolder libsFolder = project.getFolder(SdkConstants.FD_NATIVE_LIBS);
        if (libsFolder.exists()) {
            try {
                IResource[] members = libsFolder.members();
                for (IResource member : members) {
                    if (member.getType() == IResource.FILE &&
                            SdkConstants.EXT_JAR.equalsIgnoreCase(member.getFileExtension())) {
                        IPath location = member.getLocation();
                        if (location != null) {
                            jarFiles.add(location.toFile());
                        }
                    }
                }
            } catch (CoreException e) {
                // can't get the list? ignore this folder.
            }
        }
    }

    /**
     * Process reference projects from the main projects to add indirect dependencies coming
     * from Java project.
     * @param project the main project
     * @param projects the project list to add to
     * @param jarFiles the jar list to add to.
     */
    private static void processReferencedProjects(IProject project,
            Set<IProject> projects, Set<File> jarFiles) {
        try {
            IProject[] refs = project.getReferencedProjects();
            for (IProject p : refs) {
                // ignore if it's an Android project, or if it's not a Java
                // Project
                if (p.hasNature(JavaCore.NATURE_ID)
                        && p.hasNature(AdtConstants.NATURE_DEFAULT) == false) {

                    // process this project's dependencies
                    getDependencyListFromClasspath(p, projects, jarFiles, true /*includeJarFiles*/);
                }
            }
        } catch (CoreException e) {
            // can't get the referenced projects? ignore
        }
    }

    /**
     * Finds all the dependencies of a given project and add them to a project list and
     * a jar list.
     * Only classpath entries that are exported are added, and only Java project (not Android
     * project) are added.
     *
     * @param project the project to query
     * @param projects the referenced project list to add to
     * @param jarFiles the jar list to add to
     * @param includeJarFiles whether to include jar files or just projects. This is useful when
     *           calling on an Android project (value should be <code>false</code>)
     */
    private static void getDependencyListFromClasspath(IProject project, Set<IProject> projects,
            Set<File> jarFiles, boolean includeJarFiles) {
        IJavaProject javaProject = JavaCore.create(project);
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();

        // we could use IJavaProject.getResolvedClasspath directly, but we actually
        // want to see the containers themselves.
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                // ignore entries that are not exported
                if (!e.getPath().toString().equals(CONTAINER_DEPENDENCIES) && e.isExported()) {
                    processCPE(e, javaProject, wsRoot, projects, jarFiles, includeJarFiles);
                }
            }
        }
    }

    /**
     * Processes a {@link IClasspathEntry} and add it to one of the list if applicable.
     * @param entry the entry to process
     * @param javaProject the {@link IJavaProject} from which this entry came.
     * @param wsRoot the {@link IWorkspaceRoot}
     * @param projects the project list to add to
     * @param jarFiles the jar list to add to
     * @param includeJarFiles whether to include jar files or just projects. This is useful when
     *           calling on an Android project (value should be <code>false</code>)
     */
    private static void processCPE(IClasspathEntry entry, IJavaProject javaProject,
            IWorkspaceRoot wsRoot,
            Set<IProject> projects, Set<File> jarFiles, boolean includeJarFiles) {

        // if this is a classpath variable reference, we resolve it.
        if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
            entry = JavaCore.getResolvedClasspathEntry(entry);
        }

        if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
            IProject refProject = wsRoot.getProject(entry.getPath().lastSegment());
            try {
                // ignore if it's an Android project, or if it's not a Java Project
                if (refProject.hasNature(JavaCore.NATURE_ID) &&
                        refProject.hasNature(AdtConstants.NATURE_DEFAULT) == false) {
                    // add this project to the list
                    projects.add(refProject);

                    // also get the dependency from this project.
                    getDependencyListFromClasspath(refProject, projects, jarFiles,
                            true /*includeJarFiles*/);
                }
            } catch (CoreException exception) {
                // can't query the project nature? ignore
            }
        } else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
            if (includeJarFiles) {
                handleClasspathLibrary(entry, wsRoot, jarFiles);
            }
        } else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
            // get the container and its content
            try {
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        entry.getPath(), javaProject);
                // ignore the system and default_system types as they represent
                // libraries that are part of the runtime.
                if (container != null &&
                        container.getKind() == IClasspathContainer.K_APPLICATION) {
                    IClasspathEntry[] entries = container.getClasspathEntries();
                    for (IClasspathEntry cpe : entries) {
                        processCPE(cpe, javaProject, wsRoot, projects, jarFiles, includeJarFiles);
                    }
                }
            } catch (JavaModelException jme) {
                // can't resolve the container? ignore it.
                AdtPlugin.log(jme, "Failed to resolve ClasspathContainer: %s", entry.getPath());
            }
        }
    }

    private static final class CPEFile extends File {
        private static final long serialVersionUID = 1L;

        private final IClasspathEntry mClasspathEntry;

        public CPEFile(String pathname, IClasspathEntry classpathEntry) {
            super(pathname);
            mClasspathEntry = classpathEntry;
        }

        public CPEFile(File file, IClasspathEntry classpathEntry) {
            super(file.getAbsolutePath());
            mClasspathEntry = classpathEntry;
        }

        public IClasspathEntry getClasspathEntry() {
            return mClasspathEntry;
        }
    }

    private static void handleClasspathLibrary(IClasspathEntry e, IWorkspaceRoot wsRoot,
            Set<File> jarFiles) {
        // get the IPath
        IPath path = e.getPath();

        IResource resource = wsRoot.findMember(path);

        if (SdkConstants.EXT_JAR.equalsIgnoreCase(path.getFileExtension())) {
            // case of a jar file (which could be relative to the workspace or a full path)
            if (resource != null && resource.exists() &&
                    resource.getType() == IResource.FILE) {
                jarFiles.add(new CPEFile(resource.getLocation().toFile(), e));
            } else {
                // if the jar path doesn't match a workspace resource,
                // then we get an OSString and check if this links to a valid file.
                String osFullPath = path.toOSString();

                File f = new CPEFile(osFullPath, e);
                if (f.isFile()) {
                    jarFiles.add(f);
                }
            }
        }
    }
}
