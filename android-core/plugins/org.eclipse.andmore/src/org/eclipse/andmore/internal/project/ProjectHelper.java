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

package org.eclipse.andmore.internal.project;

import static org.eclipse.andmore.AdtConstants.COMPILER_COMPLIANCE_PREFERRED;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.xml.ManifestData;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.builders.PostCompilerBuilder;
import org.eclipse.andmore.internal.build.builders.PreCompilerBuilder;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class to manipulate Project parameters/properties.
 */
public final class ProjectHelper {
    public final static int COMPILER_COMPLIANCE_OK = 0;
    public final static int COMPILER_COMPLIANCE_LEVEL = 1;
    public final static int COMPILER_COMPLIANCE_SOURCE = 2;
    public final static int COMPILER_COMPLIANCE_CODEGEN_TARGET = 3;

    /**
     * Adds the given ClasspathEntry object to the class path entries.
     * This method does not check whether the entry is already defined in the project.
     *
     * @param entries The class path entries to read. A copy will be returned.
     * @param newEntry The new class path entry to add.
     * @return A new class path entries array.
     */
    public static IClasspathEntry[] addEntryToClasspath(
            IClasspathEntry[] entries, IClasspathEntry newEntry) {
        int n = entries.length;
        IClasspathEntry[] newEntries = new IClasspathEntry[n + 1];
        System.arraycopy(entries, 0, newEntries, 0, n);
        newEntries[n] = newEntry;
        return newEntries;
    }

    /**
     * Replaces the given ClasspathEntry in the classpath entries.
     *
     * If the classpath does not yet exists (Check is based on entry path), then it is added.
     *
     * @param entries The class path entries to read. The same array (replace) or a copy (add)
     *                will be returned.
     * @param newEntry The new class path entry to add.
     * @return The same array (replace) or a copy (add) will be returned.
     *
     * @see IClasspathEntry#getPath()
     */
    public static IClasspathEntry[] replaceEntryInClasspath(
            IClasspathEntry[] entries, IClasspathEntry newEntry) {

        IPath path = newEntry.getPath();
        for (int i = 0, count = entries.length; i < count ; i++) {
            if (path.equals(entries[i].getPath())) {
                entries[i] = newEntry;
                return entries;
            }
        }

        return addEntryToClasspath(entries, newEntry);
    }

    /**
     * Adds the corresponding source folder to the project's class path entries.
     * This method does not check whether the entry is already defined in the project.
     *
     * @param javaProject The java project of which path entries to update.
     * @param newEntry The new class path entry to add.
     * @throws JavaModelException
     */
    public static void addEntryToClasspath(IJavaProject javaProject, IClasspathEntry newEntry)
            throws JavaModelException {

        IClasspathEntry[] entries = javaProject.getRawClasspath();
        entries = addEntryToClasspath(entries, newEntry);
        javaProject.setRawClasspath(entries, new NullProgressMonitor());
    }

    /**
     * Checks whether the given class path entry is already defined in the project.
     *
     * @param javaProject The java project of which path entries to check.
     * @param newEntry The parent source folder to remove.
     * @return True if the class path entry is already defined.
     * @throws JavaModelException
     */
    public static boolean isEntryInClasspath(IJavaProject javaProject, IClasspathEntry newEntry)
            throws JavaModelException {

        IClasspathEntry[] entries = javaProject.getRawClasspath();
        for (IClasspathEntry entry : entries) {
            if (entry.equals(newEntry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a classpath entry from the array.
     * @param entries The class path entries to read. A copy will be returned
     * @param index The index to remove.
     * @return A new class path entries array.
     */
    public static IClasspathEntry[] removeEntryFromClasspath(
            IClasspathEntry[] entries, int index) {
        int n = entries.length;
        IClasspathEntry[] newEntries = new IClasspathEntry[n-1];

        // copy the entries before index
        System.arraycopy(entries, 0, newEntries, 0, index);

        // copy the entries after index
        System.arraycopy(entries, index + 1, newEntries, index,
                entries.length - index - 1);

        return newEntries;
    }

    /**
     * Converts a OS specific path into a path valid for the java doc location
     * attributes of a project.
     * @param javaDocOSLocation The OS specific path.
     * @return a valid path for the java doc location.
     */
    public static String getJavaDocPath(String javaDocOSLocation) {
        // first thing we do is convert the \ into /
        String javaDoc = javaDocOSLocation.replaceAll("\\\\", //$NON-NLS-1$
                AdtConstants.WS_SEP);

        // then we add file: at the beginning for unix path, and file:/ for non
        // unix path
        if (javaDoc.startsWith(AdtConstants.WS_SEP)) {
            return "file:" + javaDoc; //$NON-NLS-1$
        }

        return "file:/" + javaDoc; //$NON-NLS-1$
    }

    /**
     * Look for a specific classpath entry by full path and return its index.
     * @param entries The entry array to search in.
     * @param entryPath The OS specific path of the entry.
     * @param entryKind The kind of the entry. Accepted values are 0
     * (no filter), IClasspathEntry.CPE_LIBRARY, IClasspathEntry.CPE_PROJECT,
     * IClasspathEntry.CPE_SOURCE, IClasspathEntry.CPE_VARIABLE,
     * and IClasspathEntry.CPE_CONTAINER
     * @return the index of the found classpath entry or -1.
     */
    public static int findClasspathEntryByPath(IClasspathEntry[] entries,
            String entryPath, int entryKind) {
        for (int i = 0 ; i < entries.length ; i++) {
            IClasspathEntry entry = entries[i];

            int kind = entry.getEntryKind();

            if (kind == entryKind || entryKind == 0) {
                // get the path
                IPath path = entry.getPath();

                String osPathString = path.toOSString();
                if (osPathString.equals(entryPath)) {
                    return i;
                }
            }
        }

        // not found, return bad index.
        return -1;
    }

    /**
     * Look for a specific classpath entry for file name only and return its
     *  index.
     * @param entries The entry array to search in.
     * @param entryName The filename of the entry.
     * @param entryKind The kind of the entry. Accepted values are 0
     * (no filter), IClasspathEntry.CPE_LIBRARY, IClasspathEntry.CPE_PROJECT,
     * IClasspathEntry.CPE_SOURCE, IClasspathEntry.CPE_VARIABLE,
     * and IClasspathEntry.CPE_CONTAINER
     * @param startIndex Index where to start the search
     * @return the index of the found classpath entry or -1.
     */
    public static int findClasspathEntryByName(IClasspathEntry[] entries,
            String entryName, int entryKind, int startIndex) {
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex ; i < entries.length ; i++) {
            IClasspathEntry entry = entries[i];

            int kind = entry.getEntryKind();

            if (kind == entryKind || entryKind == 0) {
                // get the path
                IPath path = entry.getPath();
                String name = path.segment(path.segmentCount()-1);

                if (name.equals(entryName)) {
                    return i;
                }
            }
        }

        // not found, return bad index.
        return -1;
    }

    public static boolean updateProject(IJavaProject project) {
        return updateProjects(new IJavaProject[] { project});
    }

    /**
     * Update the android-specific projects's classpath containers.
     * @param projects the projects to update
     * @return
     */
    public static boolean updateProjects(IJavaProject[] projects) {
        boolean r = AndroidClasspathContainerInitializer.updateProjects(projects);
        if (r) {
            return LibraryClasspathContainerInitializer.updateProjects(projects);
        }
        return false;
    }

    /**
     * Fix the project. This checks the SDK location.
     * @param project The project to fix.
     * @throws JavaModelException
     */
    public static void fixProject(IProject project) throws JavaModelException {
        if (AdtPlugin.getOsSdkFolder().length() == 0) {
            AdtPlugin.printToConsole(project, "Unknown SDK Location, project not fixed.");
            return;
        }

        // get a java project
        IJavaProject javaProject = JavaCore.create(project);
        fixProjectClasspathEntries(javaProject);
    }

    /**
     * Fix the project classpath entries. The method ensures that:
     * <ul>
     * <li>The project does not reference any old android.zip/android.jar archive.</li>
     * <li>The project does not use its output folder as a sourc folder.</li>
     * <li>The project does not reference a desktop JRE</li>
     * <li>The project references the AndroidClasspathContainer.
     * </ul>
     * @param javaProject The project to fix.
     * @throws JavaModelException
     */
    public static void fixProjectClasspathEntries(IJavaProject javaProject)
            throws JavaModelException {

        // get the project classpath
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        IClasspathEntry[] oldEntries = entries;
        boolean forceRewriteOfCPE = false;

        // check if the JRE is set as library
        int jreIndex = ProjectHelper.findClasspathEntryByPath(entries, JavaRuntime.JRE_CONTAINER,
                IClasspathEntry.CPE_CONTAINER);
        if (jreIndex != -1) {
            // the project has a JRE included, we remove it
            entries = ProjectHelper.removeEntryFromClasspath(entries, jreIndex);
        }

        // get the output folder
        IPath outputFolder = javaProject.getOutputLocation();

        boolean foundFrameworkContainer = false;
        IClasspathEntry foundLibrariesContainer = null;
        IClasspathEntry foundDependenciesContainer = null;

        for (int i = 0 ; i < entries.length ;) {
            // get the entry and kind
            IClasspathEntry entry = entries[i];
            int kind = entry.getEntryKind();

            if (kind == IClasspathEntry.CPE_SOURCE) {
                IPath path = entry.getPath();

                if (path.equals(outputFolder)) {
                    entries = ProjectHelper.removeEntryFromClasspath(entries, i);

                    // continue, to skip the i++;
                    continue;
                }
            } else if (kind == IClasspathEntry.CPE_CONTAINER) {
                String path = entry.getPath().toString();
                if (AdtConstants.CONTAINER_FRAMEWORK.equals(path)) {
                    foundFrameworkContainer = true;
                } else if (AdtConstants.CONTAINER_PRIVATE_LIBRARIES.equals(path)) {
                    foundLibrariesContainer = entry;
                } else if (AdtConstants.CONTAINER_DEPENDENCIES.equals(path)) {
                    foundDependenciesContainer = entry;
                }
            }

            i++;
        }

        // look to see if we have the m2eclipse nature
        boolean m2eNature = false;
        try {
            m2eNature = javaProject.getProject().hasNature("org.eclipse.m2e.core.maven2Nature");
        } catch (CoreException e) {
            AdtPlugin.log(e, "Failed to query project %s for m2e nature",
                    javaProject.getProject().getName());
        }


        // if the framework container is not there, we add it
        if (!foundFrameworkContainer) {
            // add the android container to the array
            entries = ProjectHelper.addEntryToClasspath(entries,
                    JavaCore.newContainerEntry(new Path(AdtConstants.CONTAINER_FRAMEWORK)));
        }

        // same thing for the library container
        if (foundLibrariesContainer == null) {
            // add the exported libraries android container to the array
            entries = ProjectHelper.addEntryToClasspath(entries,
                    JavaCore.newContainerEntry(
                            new Path(AdtConstants.CONTAINER_PRIVATE_LIBRARIES), true));
        } else if (!m2eNature && !foundLibrariesContainer.isExported()) {
            // the container is present but it's not exported and since there's no m2e nature
            // we do want it to be exported.
            // keep all the other parameters the same.
            entries = ProjectHelper.replaceEntryInClasspath(entries,
                    JavaCore.newContainerEntry(
                            new Path(AdtConstants.CONTAINER_PRIVATE_LIBRARIES),
                            foundLibrariesContainer.getAccessRules(),
                            foundLibrariesContainer.getExtraAttributes(),
                            true));
            forceRewriteOfCPE = true;
        }

        // same thing for the dependencies container
        if (foundDependenciesContainer == null) {
            // add the android dependencies container to the array
            entries = ProjectHelper.addEntryToClasspath(entries,
                    JavaCore.newContainerEntry(
                            new Path(AdtConstants.CONTAINER_DEPENDENCIES), true));
        } else if (!m2eNature && !foundDependenciesContainer.isExported()) {
            // the container is present but it's not exported and since there's no m2e nature
            // we do want it to be exported.
            // keep all the other parameters the same.
            entries = ProjectHelper.replaceEntryInClasspath(entries,
                    JavaCore.newContainerEntry(
                            new Path(AdtConstants.CONTAINER_DEPENDENCIES),
                            foundDependenciesContainer.getAccessRules(),
                            foundDependenciesContainer.getExtraAttributes(),
                            true));
            forceRewriteOfCPE = true;
        }

        // set the new list of entries to the project
        if (entries != oldEntries || forceRewriteOfCPE) {
            javaProject.setRawClasspath(entries, new NullProgressMonitor());
        }

        // If needed, check and fix compiler compliance and source compatibility
        ProjectHelper.checkAndFixCompilerCompliance(javaProject);
    }


    /**
     * Checks the project compiler compliance level is supported.
     * @param javaProject The project to check
     * @return A pair with the first integer being an error code, and the second value
     *   being the invalid value found or null. The error code can be: <ul>
     * <li><code>COMPILER_COMPLIANCE_OK</code> if the project is properly configured</li>
     * <li><code>COMPILER_COMPLIANCE_LEVEL</code> for unsupported compiler level</li>
     * <li><code>COMPILER_COMPLIANCE_SOURCE</code> for unsupported source compatibility</li>
     * <li><code>COMPILER_COMPLIANCE_CODEGEN_TARGET</code> for unsupported .class format</li>
     * </ul>
     */
    public static final Pair<Integer, String> checkCompilerCompliance(IJavaProject javaProject) {
        // get the project compliance level option
        String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);

        // check it against a list of valid compliance level strings.
        if (!checkCompliance(javaProject, compliance)) {
            // if we didn't find the proper compliance level, we return an error
            return Pair.of(COMPILER_COMPLIANCE_LEVEL, compliance);
        }

        // otherwise we check source compatibility
        String source = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);

        // check it against a list of valid compliance level strings.
        if (!checkCompliance(javaProject, source)) {
            // if we didn't find the proper compliance level, we return an error
            return Pair.of(COMPILER_COMPLIANCE_SOURCE, source);
        }

        // otherwise check codegen level
        String codeGen = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

        // check it against a list of valid compliance level strings.
        if (!checkCompliance(javaProject, codeGen)) {
            // if we didn't find the proper compliance level, we return an error
            return Pair.of(COMPILER_COMPLIANCE_CODEGEN_TARGET, codeGen);
        }

        return Pair.of(COMPILER_COMPLIANCE_OK, null);
    }

    /**
     * Checks the project compiler compliance level is supported.
     * @param project The project to check
     * @return A pair with the first integer being an error code, and the second value
     *   being the invalid value found or null. The error code can be: <ul>
     * <li><code>COMPILER_COMPLIANCE_OK</code> if the project is properly configured</li>
     * <li><code>COMPILER_COMPLIANCE_LEVEL</code> for unsupported compiler level</li>
     * <li><code>COMPILER_COMPLIANCE_SOURCE</code> for unsupported source compatibility</li>
     * <li><code>COMPILER_COMPLIANCE_CODEGEN_TARGET</code> for unsupported .class format</li>
     * </ul>
     */
    public static final Pair<Integer, String> checkCompilerCompliance(IProject project) {
        // get the java project from the IProject resource object
        IJavaProject javaProject = JavaCore.create(project);

        // check and return the result.
        return checkCompilerCompliance(javaProject);
    }


    /**
     * Checks, and fixes if needed, the compiler compliance level, and the source compatibility
     * level
     * @param project The project to check and fix.
     */
    public static final void checkAndFixCompilerCompliance(IProject project) {
        // FIXME This method is never used. Shall we just removed it?
        // {@link #checkAndFixCompilerCompliance(IJavaProject)} is used instead.

        // get the java project from the IProject resource object
        IJavaProject javaProject = JavaCore.create(project);

        // Now we check the compiler compliance level and make sure it is valid
        checkAndFixCompilerCompliance(javaProject);
    }

    /**
     * Checks, and fixes if needed, the compiler compliance level, and the source compatibility
     * level
     * @param javaProject The Java project to check and fix.
     */
    public static final void checkAndFixCompilerCompliance(IJavaProject javaProject) {
        Pair<Integer, String> result = checkCompilerCompliance(javaProject);
        if (result.getFirst().intValue() != COMPILER_COMPLIANCE_OK) {
            // setup the preferred compiler compliance level.
            javaProject.setOption(JavaCore.COMPILER_COMPLIANCE,
                    AdtConstants.COMPILER_COMPLIANCE_PREFERRED);
            javaProject.setOption(JavaCore.COMPILER_SOURCE,
                    AdtConstants.COMPILER_COMPLIANCE_PREFERRED);
            javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
                    AdtConstants.COMPILER_COMPLIANCE_PREFERRED);

            // clean the project to make sure we recompile
            try {
                javaProject.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD,
                        new NullProgressMonitor());
            } catch (CoreException e) {
                AdtPlugin.printErrorToConsole(javaProject.getProject(),
                        "Project compiler settings changed. Clean your project.");
            }
        }
    }

    /**
     * Makes the given project use JDK 6 (or more specifically,
     * {@link AdtConstants#COMPILER_COMPLIANCE_PREFERRED} as the compilation
     * target, regardless of what the default IDE JDK level is, provided a JRE
     * of the given level is installed.
     *
     * @param javaProject the Java project
     * @throws CoreException if the IDE throws an exception setting the compiler
     *             level
     */
    @SuppressWarnings("restriction") // JDT API for setting compliance options
    public static void enforcePreferredCompilerCompliance(@NonNull IJavaProject javaProject)
            throws CoreException {
        String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
        if (compliance == null ||
                JavaModelUtil.isVersionLessThan(compliance, COMPILER_COMPLIANCE_PREFERRED)) {
            IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
            for (int i = 0; i < types.length; i++) {
                IVMInstallType type = types[i];
                IVMInstall[] installs = type.getVMInstalls();
                for (int j = 0; j < installs.length; j++) {
                    IVMInstall install = installs[j];
                    if (install instanceof IVMInstall2) {
                        IVMInstall2 install2 = (IVMInstall2) install;
                        // Java version can be 1.6.0, and preferred is 1.6
                        if (install2.getJavaVersion().startsWith(COMPILER_COMPLIANCE_PREFERRED)) {
                            Map<String, String> options = javaProject.getOptions(false);
                            JavaCore.setComplianceOptions(COMPILER_COMPLIANCE_PREFERRED, options);
                            JavaModelUtil.setDefaultClassfileOptions(options,
                                    COMPILER_COMPLIANCE_PREFERRED);
                            javaProject.setOptions(options);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a {@link IProject} by its running application name, as it returned by the AVD.
     * <p/>
     * <var>applicationName</var> will in most case be the package declared in the manifest, but
     * can, in some cases, be a custom process name declared in the manifest, in the
     * <code>application</code>, <code>activity</code>, <code>receiver</code>, or
     * <code>service</code> nodes.
     * @param applicationName The application name.
     * @return a project or <code>null</code> if no matching project were found.
     */
    public static IProject findAndroidProjectByAppName(String applicationName) {
        // Get the list of project for the current workspace
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();

        // look for a project that matches the packageName of the app
        // we're trying to debug
        for (IProject p : projects) {
            if (p.isOpen()) {
                try {
                    if (p.hasNature(AdtConstants.NATURE_DEFAULT) == false) {
                        // ignore non android projects
                        continue;
                    }
                } catch (CoreException e) {
                    // failed to get the nature? skip project.
                    continue;
                }

                // check that there is indeed a manifest file.
                IFile manifestFile = getManifest(p);
                if (manifestFile == null) {
                    // no file? skip this project.
                    continue;
                }

                ManifestData data = AndroidManifestHelper.parseForData(manifestFile);
                if (data == null) {
                    // skip this project.
                    continue;
                }

                String manifestPackage = data.getPackage();

                if (manifestPackage != null && manifestPackage.equals(applicationName)) {
                    // this is the project we were looking for!
                    return p;
                } else {
                    // if the package and application name don't match,
                    // we look for other possible process names declared in the manifest.
                    String[] processes = data.getProcesses();
                    for (String process : processes) {
                        if (process.equals(applicationName)) {
                            return p;
                        }
                    }
                }
            }
        }

        return null;

    }

    public static void fixProjectNatureOrder(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();

        // if the android nature is not the first one, we reorder them
        if (AdtConstants.NATURE_DEFAULT.equals(natures[0]) == false) {
            // look for the index
            for (int i = 0 ; i < natures.length ; i++) {
                if (AdtConstants.NATURE_DEFAULT.equals(natures[i])) {
                    // if we try to just reorder the array in one pass, this doesn't do
                    // anything. I guess JDT check that we are actually adding/removing nature.
                    // So, first we'll remove the android nature, and then add it back.

                    // remove the android nature
                    removeNature(project, AdtConstants.NATURE_DEFAULT);

                    // now add it back at the first index.
                    description = project.getDescription();
                    natures = description.getNatureIds();

                    String[] newNatures = new String[natures.length + 1];

                    // first one is android
                    newNatures[0] = AdtConstants.NATURE_DEFAULT;

                    // next the rest that was before the android nature
                    System.arraycopy(natures, 0, newNatures, 1, natures.length);

                    // set the new natures
                    description.setNatureIds(newNatures);
                    project.setDescription(description, null);

                    // and stop
                    break;
                }
            }
        }
    }


    /**
     * Removes a specific nature from a project.
     * @param project The project to remove the nature from.
     * @param nature The nature id to remove.
     * @throws CoreException
     */
    public static void removeNature(IProject project, String nature) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();

        // check if the project already has the android nature.
        for (int i = 0; i < natures.length; ++i) {
            if (nature.equals(natures[i])) {
                String[] newNatures = new String[natures.length - 1];
                if (i > 0) {
                    System.arraycopy(natures, 0, newNatures, 0, i);
                }
                System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                description.setNatureIds(newNatures);
                project.setDescription(description, null);

                return;
            }
        }

    }

    /**
     * Returns if the project has error level markers.
     * @param includeReferencedProjects flag to also test the referenced projects.
     * @throws CoreException
     */
    public static boolean hasError(IProject project, boolean includeReferencedProjects)
    throws CoreException {
        IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        if (markers != null && markers.length > 0) {
            // the project has marker(s). even though they are "problem" we
            // don't know their severity. so we loop on them and figure if they
            // are warnings or errors
            for (IMarker m : markers) {
                int s = m.getAttribute(IMarker.SEVERITY, -1);
                if (s == IMarker.SEVERITY_ERROR) {
                    return true;
                }
            }
        }

        // test the referenced projects if needed.
        if (includeReferencedProjects) {
            List<IProject> projects = getReferencedProjects(project);

            for (IProject p : projects) {
                if (hasError(p, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Saves a String property into the persistent storage of a resource.
     * @param resource The resource into which the string value is saved.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    public static boolean saveStringProperty(IResource resource, String propertyName,
            String value) {
        QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID, propertyName);

        try {
            resource.setPersistentProperty(qname, value);
        } catch (CoreException e) {
            return false;
        }

        return true;
    }

    /**
     * Loads a String property from the persistent storage of a resource.
     * @param resource The resource from which the string value is loaded.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @return the property value or null if it was not found.
     */
    public static String loadStringProperty(IResource resource, String propertyName) {
        QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID, propertyName);

        try {
            String value = resource.getPersistentProperty(qname);
            return value;
        } catch (CoreException e) {
            return null;
        }
    }

    /**
     * Saves a property into the persistent storage of a resource.
     * @param resource The resource into which the boolean value is saved.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    public static boolean saveBooleanProperty(IResource resource, String propertyName,
            boolean value) {
        return saveStringProperty(resource, propertyName, Boolean.toString(value));
    }

    /**
     * Loads a boolean property from the persistent storage of a resource.
     * @param resource The resource from which the boolean value is loaded.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @param defaultValue The default value to return if the property was not found.
     * @return the property value or the default value if the property was not found.
     */
    public static boolean loadBooleanProperty(IResource resource, String propertyName,
            boolean defaultValue) {
        String value = loadStringProperty(resource, propertyName);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    public static Boolean loadBooleanProperty(IResource resource, String propertyName) {
        String value = loadStringProperty(resource, propertyName);
        if (value != null) {
            return Boolean.valueOf(value);
        }

        return null;
    }

    /**
     * Saves the path of a resource into the persistent storage of a resource.
     * @param resource The resource into which the resource path is saved.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @param value The resource to save. It's its path that is actually stored. If null, an
     *      empty string is stored.
     * @return true if the save succeeded
     */
    public static boolean saveResourceProperty(IResource resource, String propertyName,
            IResource value) {
        if (value != null) {
            IPath iPath = value.getFullPath();
            return saveStringProperty(resource, propertyName, iPath.toString());
        }

        return saveStringProperty(resource, propertyName, ""); //$NON-NLS-1$
    }

    /**
     * Loads the path of a resource from the persistent storage of a resource, and returns the
     * corresponding IResource object.
     * @param resource The resource from which the resource path is loaded.
     * @param propertyName the name of the property. The id of the plug-in is added to this string.
     * @return The corresponding IResource object (or children interface) or null
     */
    public static IResource loadResourceProperty(IResource resource, String propertyName) {
        String value = loadStringProperty(resource, propertyName);

        if (value != null && value.length() > 0) {
            return ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(value));
        }

        return null;
    }

    /**
     * Returns the list of referenced project that are opened and Java projects.
     * @param project
     * @return a new list object containing the opened referenced java project.
     * @throws CoreException
     */
    public static List<IProject> getReferencedProjects(IProject project) throws CoreException {
        IProject[] projects = project.getReferencedProjects();

        ArrayList<IProject> list = new ArrayList<IProject>();

        for (IProject p : projects) {
            if (p.isOpen() && p.hasNature(JavaCore.NATURE_ID)) {
                list.add(p);
            }
        }

        return list;
    }


    /**
     * Checks a Java project compiler level option against a list of supported versions.
     * @param optionValue the Compiler level option.
     * @return true if the option value is supported.
     */
    private static boolean checkCompliance(@NonNull IJavaProject project, String optionValue) {
        for (String s : AdtConstants.COMPILER_COMPLIANCE) {
            if (s != null && s.equals(optionValue)) {
                return true;
            }
        }

        if (JavaCore.VERSION_1_7.equals(optionValue)) {
            // Requires API 19 and buildTools 19
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IProject p = project.getProject();
                IAndroidTarget target = currentSdk.getTarget(p);
                if (target == null || target.getVersion().getApiLevel() < 19) {
                    return false;
                }

                ProjectState projectState = Sdk.getProjectState(p);
                if (projectState != null) {
                    BuildToolInfo buildToolInfo = projectState.getBuildToolInfo();
                    if (buildToolInfo == null) {
                        buildToolInfo = currentSdk.getLatestBuildTool();
                    }
                    if (buildToolInfo == null || buildToolInfo.getRevision().getMajor() < 19) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Returns the apk filename for the given project
     * @param project The project.
     * @param config An optional config name. Can be null.
     */
    public static String getApkFilename(IProject project, String config) {
        if (config != null) {
            return project.getName() + "-" + config + SdkConstants.DOT_ANDROID_PACKAGE; //$NON-NLS-1$
        }

        return project.getName() + SdkConstants.DOT_ANDROID_PACKAGE;
    }

    /**
     * Find the list of projects on which this JavaProject is dependent on at the compilation level.
     *
     * @param javaProject Java project that we are looking for the dependencies.
     * @return A list of Java projects for which javaProject depend on.
     * @throws JavaModelException
     */
    public static List<IJavaProject> getAndroidProjectDependencies(IJavaProject javaProject)
        throws JavaModelException {
        String[] requiredProjectNames = javaProject.getRequiredProjectNames();

        // Go from java project name to JavaProject name
        IJavaModel javaModel = javaProject.getJavaModel();

        // loop through all dependent projects and keep only those that are Android projects
        List<IJavaProject> projectList = new ArrayList<IJavaProject>(requiredProjectNames.length);
        for (String javaProjectName : requiredProjectNames) {
            IJavaProject androidJavaProject = javaModel.getJavaProject(javaProjectName);

            //Verify that the project has also the Android Nature
            try {
                if (!androidJavaProject.getProject().hasNature(AdtConstants.NATURE_DEFAULT)) {
                    continue;
                }
            } catch (CoreException e) {
                continue;
            }

            projectList.add(androidJavaProject);
        }

        return projectList;
    }

    /**
     * Returns the android package file as an IFile object for the specified
     * project.
     * @param project The project
     * @return The android package as an IFile object or null if not found.
     */
    public static IFile getApplicationPackage(IProject project) {
        // get the output folder
        IFolder outputLocation = BaseProjectHelper.getAndroidOutputFolder(project);

        if (outputLocation == null) {
            AdtPlugin.printErrorToConsole(project,
                    "Failed to get the output location of the project. Check build path properties"
                    );
            return null;
        }


        // get the package path
        String packageName = project.getName() + SdkConstants.DOT_ANDROID_PACKAGE;
        IResource r = outputLocation.findMember(packageName);

        // check the package is present
        if (r instanceof IFile && r.exists()) {
            return (IFile)r;
        }

        String msg = String.format("Could not find %1$s!", packageName);
        AdtPlugin.printErrorToConsole(project, msg);

        return null;
    }

    /**
     * Returns an {@link IFile} object representing the manifest for the given project.
     *
     * @param project The project containing the manifest file.
     * @return An IFile object pointing to the manifest or null if the manifest
     *         is missing.
     */
    public static IFile getManifest(IProject project) {
        IResource r = project.findMember(AdtConstants.WS_SEP
                + SdkConstants.FN_ANDROID_MANIFEST_XML);

        if (r == null || r.exists() == false || (r instanceof IFile) == false) {
            return null;
        }
        return (IFile) r;
    }

    /**
     * Does a full release build of the application, including the libraries. Do not build the
     * package.
     *
     * @param project The project to be built.
     * @param monitor A eclipse runtime progress monitor to be updated by the builders.
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    public static void compileInReleaseMode(IProject project, IProgressMonitor monitor)
            throws CoreException {
        compileInReleaseMode(project, true /*includeDependencies*/, monitor);
    }

    /**
     * Does a full release build of the application, including the libraries. Do not build the
     * package.
     *
     * @param project The project to be built.
     * @param monitor A eclipse runtime progress monitor to be updated by the builders.
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    private static void compileInReleaseMode(IProject project, boolean includeDependencies,
            IProgressMonitor monitor)
            throws CoreException {

        if (includeDependencies) {
            ProjectState projectState = Sdk.getProjectState(project);

            // this gives us all the library projects, direct and indirect dependencies,
            // so no need to run this method recursively.
            List<IProject> libraries = projectState.getFullLibraryProjects();

            // build dependencies in reverse order to prevent libraries being rebuilt
            // due to refresh of other libraries (they would be compiled in the wrong mode).
            for (int i = libraries.size() - 1 ; i >= 0 ; i--) {
                IProject lib = libraries.get(i);
                compileInReleaseMode(lib, false /*includeDependencies*/, monitor);

                // force refresh of the dependency.
                lib.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        }

        // do a full build on all the builders to guarantee that the builders are called.
        // (Eclipse does an optimization where builders are not called if there aren't any
        // deltas).

        ICommand[] commands = project.getDescription().getBuildSpec();
        for (ICommand command : commands) {
            String name = command.getBuilderName();
            if (PreCompilerBuilder.ID.equals(name)) {
                Map newArgs = new HashMap();
                newArgs.put(PreCompilerBuilder.RELEASE_REQUESTED, "");
                if (command.getArguments() != null) {
                    newArgs.putAll(command.getArguments());
                }

                project.build(IncrementalProjectBuilder.FULL_BUILD,
                        PreCompilerBuilder.ID, newArgs, monitor);
            } else if (PostCompilerBuilder.ID.equals(name)) {
                if (includeDependencies == false) {
                    // this is a library, we need to build it!
                    project.build(IncrementalProjectBuilder.FULL_BUILD, name,
                            command.getArguments(), monitor);
                }
            } else {

                project.build(IncrementalProjectBuilder.FULL_BUILD, name,
                        command.getArguments(), monitor);
            }
        }
    }

    /**
     * Force building the project and all its dependencies.
     *
     * @param project the project to build
     * @param kind the build kind
     * @param monitor
     * @throws CoreException
     */
    public static void buildWithDeps(IProject project, int kind, IProgressMonitor monitor)
            throws CoreException {
        // Get list of projects that we depend on
        ProjectState projectState = Sdk.getProjectState(project);

        // this gives us all the library projects, direct and indirect dependencies,
        // so no need to run this method recursively.
        List<IProject> libraries = projectState.getFullLibraryProjects();

        // build dependencies in reverse order to prevent libraries being rebuilt
        // due to refresh of other libraries (they would be compiled in the wrong mode).
        for (int i = libraries.size() - 1 ; i >= 0 ; i--) {
            IProject lib = libraries.get(i);
            lib.build(kind, monitor);
            lib.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }

        project.build(kind, monitor);
    }


    /**
     * Build project incrementally, including making the final packaging even if it is disabled
     * by default.
     *
     * @param project The project to be built.
     * @param monitor A eclipse runtime progress monitor to be updated by the builders.
     * @throws CoreException
     */
    public static void doFullIncrementalDebugBuild(IProject project, IProgressMonitor monitor)
            throws CoreException {
        // Get list of projects that we depend on
        List<IJavaProject> androidProjectList = new ArrayList<IJavaProject>();
        try {
            androidProjectList = getAndroidProjectDependencies(
                                    BaseProjectHelper.getJavaProject(project));
        } catch (JavaModelException e) {
            AdtPlugin.printErrorToConsole(project, e);
        }
        // Recursively build dependencies
        for (IJavaProject dependency : androidProjectList) {
            doFullIncrementalDebugBuild(dependency.getProject(), monitor);
        }

        // Do an incremental build to pick up all the deltas
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

        // If the preferences indicate not to use post compiler optimization
        // then the incremental build will have done everything necessary, otherwise,
        // we have to run the final builder manually (if requested).
        if (AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
            // Create the map to pass to the PostC builder
            Map<String, String> args = new TreeMap<String, String>();
            args.put(PostCompilerBuilder.POST_C_REQUESTED, ""); //$NON-NLS-1$

            // call the post compiler manually, forcing FULL_BUILD otherwise Eclipse won't
            // call the builder since the delta is empty.
            project.build(IncrementalProjectBuilder.FULL_BUILD,
                          PostCompilerBuilder.ID, args, monitor);
        }

        // because the post compiler builder does a delayed refresh due to
        // library not picking the refresh up if it's done during the build,
        // we want to force a refresh here as this call is generally asking for
        // a build to use the apk right after the call.
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }
}
