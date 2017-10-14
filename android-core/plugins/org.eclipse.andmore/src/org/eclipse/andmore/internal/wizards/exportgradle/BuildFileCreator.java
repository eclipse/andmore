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

import static com.android.SdkConstants.GRADLE_LATEST_VERSION;
import static com.android.SdkConstants.GRADLE_PLUGIN_LATEST_VERSION;
import static com.android.SdkConstants.GRADLE_PLUGIN_NAME;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.io.IAbstractFile;
import com.android.repository.io.FileOpUtils;
import com.android.xml.AndroidManifest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Creates build.gradle and settings.gradle files for a set of projects.
 * <p>
 * Based on {@link org.eclipse.ant.internal.ui.datatransfer.BuildFileCreator}
 */
public class BuildFileCreator {
    static final String BUILD_FILE = "build.gradle"; //$NON-NLS-1$
    static final String SETTINGS_FILE = "settings.gradle"; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String GRADLE_WRAPPER_LOCATION =
            "tools/templates/gradle/wrapper"; //$NON-NLS-1$
    static final String PLUGIN_CLASSPATH =
            "classpath '" + GRADLE_PLUGIN_NAME + GRADLE_PLUGIN_LATEST_VERSION + "'"; //$NON-NLS-1$
    static final String MAVEN_REPOSITORY = "mavenCentral()"; //$NON-NLS-1$

    private static final String[] GRADLE_WRAPPER_FILES = new String[] {
        "gradlew", //$NON-NLS-1$
        "gradlew.bat", //$NON-NLS-1$
        "gradle/wrapper/gradle-wrapper.jar", //$NON-NLS-1$
        "gradle/wrapper/gradle-wrapper.properties" //$NON-NLS-1$
    };

    private static final Comparator<IFile> FILE_COMPARATOR = new Comparator<IFile>() {
        @Override
        public int compare(IFile o1, IFile o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };

    private final GradleModule mModule;
    private final StringBuilder mBuildFile = new StringBuilder();

    /**
     * Create buildfile for the projects.
     *
     * @param shell parent instance for dialogs
     * @return project names for which buildfiles were created
     * @throws InterruptedException thrown when user cancels task
     */
    public static void createBuildFiles(
            @NonNull ProjectSetupBuilder builder,
            @NonNull Shell shell,
            @NonNull IProgressMonitor pm) {

        File gradleLocation = new File(Sdk.getCurrent().getSdkOsLocation(), GRADLE_WRAPPER_LOCATION);
        SubMonitor localmonitor = null;

        try {
            // See if we have a Gradle wrapper in the SDK templates directory. If so, we can copy
            // it over.
            boolean hasGradleWrapper = true;
            for (File wrapperFile : getGradleWrapperFiles(gradleLocation)) {
                if (!wrapperFile.exists()) {
                    hasGradleWrapper = false;
                }
            }

            Collection<GradleModule> modules = builder.getModules();
            boolean multiModules = modules.size() > 1;

            // determine files to create/change
            List<IFile> files = new ArrayList<IFile>();

            // add the build.gradle file for all modules.
            for (GradleModule module : modules) {
                // build.gradle file
                IFile file = module.getProject().getFile(BuildFileCreator.BUILD_FILE);
                files.add(file);
            }

            // get the commonRoot for all modules. If only one module, this returns the path
            // of the project.
            IPath commonRoot = builder.getCommonRoot();

            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IPath workspaceLocation = workspaceRoot.getLocation();

            IPath relativePath = commonRoot.makeRelativeTo(workspaceLocation);
            // if makeRelativePath to returns the same path, then commonRoot is not in the
            // workspace.
            boolean rootInWorkspace = !relativePath.equals(commonRoot);
            // we only care if the root is a workspace project. if it's the workspace folder itself,
            // then the files won't be handled by the workspace.
            rootInWorkspace = rootInWorkspace && relativePath.segmentCount() > 0;

            File settingsFile = new File(commonRoot.toFile(), SETTINGS_FILE);

            // more than one modules -> generate settings.gradle
            if (multiModules && rootInWorkspace) {

                // Locate the settings.gradle file and add it to the changed files list
                IPath settingsGradle = Path.fromOSString(settingsFile.getAbsolutePath());

                // different path, means commonRoot is inside the workspace, which means we have
                // to add settings.gradle and wrapper files to the list of files to add.
                IFile iFile = workspaceRoot.getFile(settingsGradle);
                if (iFile != null) {
                    files.add(iFile);
                }
            }

            // Gradle wrapper files
            if (hasGradleWrapper && rootInWorkspace) {
                // See if there already wrapper files there and only mark nonexistent ones for
                // creation.
                for (File wrapperFile : getGradleWrapperFiles(commonRoot.toFile())) {
                    if (!wrapperFile.exists()) {
                        IPath path = Path.fromOSString(wrapperFile.getAbsolutePath());
                        IFile file = workspaceRoot.getFile(path);
                        files.add(file);
                    }
                }
            }

            ExportStatus status = new ExportStatus();
            builder.setStatus(status);

            // Trigger checkout of changed files
            Set<IFile> confirmedFiles = validateEdit(files, status, shell);

            if (status.hasError()) {
                return;
            }

            // Now iterate over all the modules and generate the build files.
            localmonitor = SubMonitor.convert(pm, ExportMessages.PageTitle,
                    confirmedFiles.size());
            List<String> projectSettingsPath = Lists.newArrayList();
            for (GradleModule currentModule : modules) {
                IProject moduleProject = currentModule.getProject();

                IFile file = moduleProject.getFile(BuildFileCreator.BUILD_FILE);
                if (!confirmedFiles.contains(file)) {
                    continue;
                }

                localmonitor.setTaskName(NLS.bind(ExportMessages.FileStatusMessage,
                        moduleProject.getName()));

                ProjectState projectState = Sdk.getProjectState(moduleProject);
                BuildFileCreator instance = new BuildFileCreator(currentModule, shell);
                if (projectState != null) {
                    // This is an Android project
                    if (!multiModules) {
                        instance.appendBuildScript();
                    }
                    instance.appendHeader(projectState.isLibrary());
                    instance.appendDependencies();
                    instance.startAndroidTask(projectState);
                    //instance.appendDefaultConfig();
                    instance.createAndroidSourceSets();
                    instance.finishAndroidTask();
                } else {
                    // This is a plain Java project
                    instance.appendJavaHeader();
                    instance.createJavaSourceSets();
                }

               try {
                    // Write the build file
                    String buildfile = instance.mBuildFile.toString();
                    InputStream is =
                            new ByteArrayInputStream(buildfile.getBytes("UTF-8")); //$NON-NLS-1$
                    if (file.exists()) {
                        file.setContents(is, true, true, null);
                    } else {
                        file.create(is, true, null);
                    }
               } catch (Exception e) {
                     status.addFileStatus(ExportStatus.FileStatus.IO_FAILURE,
                             file.getLocation().toFile());
                     status.setErrorMessage(e.getMessage());
                     return;
               }

               if (localmonitor.isCanceled()) {
                   return;
               }
               localmonitor.worked(1);

                // get the project path to add it to the settings.gradle.
                projectSettingsPath.add(currentModule.getPath());
            }

            // write the settings file.
            if (multiModules) {
                try {
                    writeGradleSettingsFile(settingsFile, projectSettingsPath);
                } catch (IOException e) {
                    status.addFileStatus(ExportStatus.FileStatus.IO_FAILURE, settingsFile);
                    status.setErrorMessage(e.getMessage());
                    return;
                }
                File mainBuildFile = new File(commonRoot.toFile(), BUILD_FILE);
                try {
                    writeRootBuildGradle(mainBuildFile);
                } catch (IOException e) {
                    status.addFileStatus(ExportStatus.FileStatus.IO_FAILURE, mainBuildFile);
                    status.setErrorMessage(e.getMessage());
                    return;
                }
            }

            // finally write the wrapper
            // TODO check we can based on where it is
            if (hasGradleWrapper) {
                copyGradleWrapper(gradleLocation, commonRoot.toFile(), status);
                if (status.hasError()) {
                    return;
                }
            }

        } finally {
            if (localmonitor != null && !localmonitor.isCanceled()) {
                localmonitor.done();
            }
            if (pm != null) {
                pm.done();
            }
        }
    }

    /**
     * @param GradleModule create buildfile for this project
     * @param shell parent instance for dialogs
     */
    private BuildFileCreator(GradleModule module, Shell shell) {
        mModule = module;
    }

    /**
     * Return the files that comprise the Gradle wrapper as a collection of {@link File} instances.
     * @param root
     * @return
     */
    private static List<File> getGradleWrapperFiles(File root) {
        List<File> files = new ArrayList<File>(GRADLE_WRAPPER_FILES.length);
        for (String file : GRADLE_WRAPPER_FILES) {
            files.add(new File(root, file));
        }
        return files;
    }

    /**
     * Copy the Gradle wrapper files from one directory to another.
     */
    private static void copyGradleWrapper(File from, File to, ExportStatus status) {
        for (String file : GRADLE_WRAPPER_FILES) {
            File dest = new File(to, file);
            try {
                File src = new File(from, file);
                dest.getParentFile().mkdirs();
                FileOpUtils.create().copyFile(src, dest);

                if (src.getName().equals(GRADLE_PROPERTIES)) {
                    updateGradleDistributionUrl(GRADLE_LATEST_VERSION, dest);
                }
                dest.setExecutable(src.canExecute());
                status.addFileStatus(ExportStatus.FileStatus.OK, dest);
            } catch (IOException e) {
                status.addFileStatus(ExportStatus.FileStatus.IO_FAILURE, dest);
                return;
            }
        }
    }

    /**
     * Outputs boilerplate buildscript information common to all Gradle build files.
     */
    private void appendBuildScript() {
        appendBuildScript(mBuildFile);
    }

    /**
     * Outputs boilerplate header information common to all Gradle build files.
     */
    private static void appendBuildScript(StringBuilder builder) {
        builder.append("buildscript {\n"); //$NON-NLS-1$
        builder.append("    repositories {\n"); //$NON-NLS-1$
        builder.append("        " + MAVEN_REPOSITORY + "\n"); //$NON-NLS-1$
        builder.append("    }\n"); //$NON-NLS-1$
        builder.append("    dependencies {\n"); //$NON-NLS-1$
        builder.append("        " + PLUGIN_CLASSPATH + "\n"); //$NON-NLS-1$
        builder.append("    }\n"); //$NON-NLS-1$
        builder.append("}\n"); //$NON-NLS-1$
    }

    /**
     * Outputs boilerplate header information common to all Gradle build files.
     */
    private void appendHeader(boolean isLibrary) {
        if (isLibrary) {
            mBuildFile.append("apply plugin: 'android-library'\n"); //$NON-NLS-1$
        } else {
            mBuildFile.append("apply plugin: 'android'\n"); //$NON-NLS-1$
        }
        mBuildFile.append("\n"); //$NON-NLS-1$
    }

    /**
     * Outputs a block which sets up library and project dependencies.
     */
    private void appendDependencies() {
        mBuildFile.append("dependencies {\n"); //$NON-NLS-1$

        // first the local jars.
        // TODO: Fix
        mBuildFile.append("    compile fileTree(dir: 'libs', include: '*.jar')\n"); //$NON-NLS-1$

        for (GradleModule dep : mModule.getDependencies()) {
            mBuildFile.append("    compile project('" + dep.getPath() + "')\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        mBuildFile.append("}\n"); //$NON-NLS-1$
        mBuildFile.append("\n"); //$NON-NLS-1$
    }

    /**
     * Outputs the beginning of an Android task in the build file.
     */
    private void startAndroidTask(ProjectState projectState) {
        int buildApi = projectState.getTarget().getVersion().getApiLevel();
        String toolsVersion = projectState.getTarget().getBuildToolInfo().getRevision().toString();
        mBuildFile.append("android {\n"); //$NON-NLS-1$
        mBuildFile.append("    compileSdkVersion " + buildApi + "\n"); //$NON-NLS-1$
        mBuildFile.append("    buildToolsVersion \"" + toolsVersion + "\"\n"); //$NON-NLS-1$
        mBuildFile.append("\n"); //$NON-NLS-1$

        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(projectState.getProject());
            // otherwise we check source compatibility
            String source = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
            if (JavaCore.VERSION_1_7.equals(source)) {
                mBuildFile.append(
                        "    compileOptions {\n" + //$NON-NLS-1$
                        "        sourceCompatibility JavaVersion.VERSION_1_7\n" + //$NON-NLS-1$
                        "        targetCompatibility JavaVersion.VERSION_1_7\n" + //$NON-NLS-1$
                        "    }\n" + //$NON-NLS-1$
                        "\n"); //$NON-NLS-1$
            }
        } catch (CoreException e) {
            // Ignore compliance level, go with default
        }
    }

    /**
     * Outputs a sourceSets block to the Android task that locates all of the various source
     * subdirectories in the project.
     */
    private void createAndroidSourceSets() {
        IFolderWrapper projectFolder = new IFolderWrapper(mModule.getProject());
        IAbstractFile mManifestFile = AndroidManifest.getManifest(projectFolder);
        if (mManifestFile == null) {
            return;
        }
        List<String> srcDirs = new ArrayList<String>();
        for (IClasspathEntry entry : mModule.getJavaProject().readRawClasspath()) {
            if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE ||
                    SdkConstants.FD_GEN_SOURCES.equals(entry.getPath().lastSegment())) {
                continue;
            }
            IPath path = entry.getPath().removeFirstSegments(1);
            srcDirs.add("'" + path.toOSString() + "'"); //$NON-NLS-1$
        }

        String srcPaths = Joiner.on(",").join(srcDirs);

        mBuildFile.append("    sourceSets {\n"); //$NON-NLS-1$
        mBuildFile.append("        main {\n"); //$NON-NLS-1$
        mBuildFile.append("            manifest.srcFile '" + SdkConstants.FN_ANDROID_MANIFEST_XML + "'\n"); //$NON-NLS-1$
        mBuildFile.append("            java.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("            resources.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("            aidl.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("            renderscript.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("            res.srcDirs = ['res']\n"); //$NON-NLS-1$
        mBuildFile.append("            assets.srcDirs = ['assets']\n"); //$NON-NLS-1$
        mBuildFile.append("        }\n"); //$NON-NLS-1$
        mBuildFile.append("\n"); //$NON-NLS-1$
        mBuildFile.append("        // Move the tests to tests/java, tests/res, etc...\n"); //$NON-NLS-1$
        mBuildFile.append("        instrumentTest.setRoot('tests')\n"); //$NON-NLS-1$
        if (srcDirs.contains("'src'")) {
            mBuildFile.append("\n"); //$NON-NLS-1$
            mBuildFile.append("        // Move the build types to build-types/<type>\n"); //$NON-NLS-1$
            mBuildFile.append("        // For instance, build-types/debug/java, build-types/debug/AndroidManifest.xml, ...\n"); //$NON-NLS-1$
            mBuildFile.append("        // This moves them out of them default location under src/<type>/... which would\n"); //$NON-NLS-1$
            mBuildFile.append("        // conflict with src/ being used by the main source set.\n"); //$NON-NLS-1$
            mBuildFile.append("        // Adding new build types or product flavors should be accompanied\n"); //$NON-NLS-1$
            mBuildFile.append("        // by a similar customization.\n"); //$NON-NLS-1$
            mBuildFile.append("        debug.setRoot('build-types/debug')\n"); //$NON-NLS-1$
            mBuildFile.append("        release.setRoot('build-types/release')\n"); //$NON-NLS-1$
        }
        mBuildFile.append("    }\n"); //$NON-NLS-1$
    }

    /**
     * Outputs the completion of the Android task in the build file.
     */
    private void finishAndroidTask() {
        mBuildFile.append("}\n"); //$NON-NLS-1$
    }

    /**
     * Outputs a boilerplate header for non-Android projects
     */
    private void appendJavaHeader() {
        mBuildFile.append("apply plugin: 'java'\n"); //$NON-NLS-1$
    }

    /**
     * Outputs a sourceSets block for non-Android projects to locate the source directories.
     */
    private void createJavaSourceSets() {
        List<String> dirs = new ArrayList<String>();
        for (IClasspathEntry entry : mModule.getJavaProject().readRawClasspath()) {
            if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                continue;
            }
            IPath path = entry.getPath().removeFirstSegments(1);
            dirs.add("'" + path.toOSString() + "'"); //$NON-NLS-1$
        }

        String srcPaths = Joiner.on(",").join(dirs);

        mBuildFile.append("sourceSets {\n"); //$NON-NLS-1$
        mBuildFile.append("    main.java.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("    main.resources.srcDirs = [" + srcPaths + "]\n"); //$NON-NLS-1$
        mBuildFile.append("    test.java.srcDirs = ['tests/java']\n"); //$NON-NLS-1$
        mBuildFile.append("    test.resources.srcDirs = ['tests/resources']\n"); //$NON-NLS-1$
        mBuildFile.append("}\n"); //$NON-NLS-1$
    }

    /**
     * Merges the new subproject dependencies into the settings.gradle file if it already exists,
     * and creates one if it does not.
     * @throws IOException
     */
    private static void writeGradleSettingsFile(File settingsFile, List<String> projectPaths)
            throws IOException {
        StringBuilder contents = new StringBuilder();
        for (String path : projectPaths) {
            contents.append("include '").append(path).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        Files.write(contents.toString(), settingsFile, Charsets.UTF_8);
    }

    private static void writeRootBuildGradle(File buildFile) throws IOException {
        StringBuilder sb = new StringBuilder(
                "// Top-level build file where you can add configuration options common to all sub-projects/modules.\n");

        appendBuildScript(sb);

        Files.write(sb.toString(), buildFile, Charsets.UTF_8);
    }

    /**
     * Request write access to given files. Depending on the version control
     * plug-in opens a confirm checkout dialog.
     *
     * @param shell
     *            parent instance for dialogs
     * @return <code>IFile</code> objects for which user confirmed checkout
     * @throws CoreException
     *             thrown if project is under version control, but not connected
     */
    static Set<IFile> validateEdit(
            @NonNull List<IFile> files,
            @NonNull ExportStatus exportStatus,
            @NonNull Shell shell) {
        Set<IFile> confirmedFiles = new TreeSet<IFile>(FILE_COMPARATOR);
        if (files.size() == 0) {
            return confirmedFiles;
        }
        IStatus status = (files.get(0)).getWorkspace().validateEdit(
                files.toArray(new IFile[files.size()]), shell);
        if (status.isMultiStatus() && status.getChildren().length > 0) {
            for (int i = 0; i < status.getChildren().length; i++) {
                IStatus statusChild = status.getChildren()[i];
                if (statusChild.isOK()) {
                    confirmedFiles.add(files.get(i));
                } else {
                    exportStatus.addFileStatus(
                            ExportStatus.FileStatus.VCS_FAILURE,
                            files.get(i).getLocation().toFile());
                }
            }
        } else if (status.isOK()) {
            confirmedFiles.addAll(files);
        }
        if (status.getSeverity() == IStatus.ERROR) {
            // not possible to checkout files: not connected to version
            // control plugin or hijacked files and made read-only, so
            // collect error messages provided by validator and re-throw
            StringBuffer message = new StringBuffer(status.getPlugin() + ": " //$NON-NLS-1$
                    + status.getMessage() + NEWLINE);
            if (status.isMultiStatus()) {
                for (int i = 0; i < status.getChildren().length; i++) {
                    IStatus statusChild = status.getChildren()[i];
                    message.append(statusChild.getMessage() + NEWLINE);
                }
            }
            String s = message.toString();
            exportStatus.setErrorMessage(s);
        }

        return confirmedFiles;
    }

    // -------------------------------------------------------------------------------
    // Fix gradle wrapper version. This code is from GradleUtil in the Studio plugin:
    // -------------------------------------------------------------------------------

    private static final String GRADLE_PROPERTIES = "gradle-wrapper.properties";
    private static final String GRADLEW_PROPERTIES_PATH =
            "gradle" + File.separator + "wrapper" + File.separator + GRADLE_PROPERTIES;
    private static final String GRADLEW_DISTRIBUTION_URL_PROPERTY_NAME = "distributionUrl";

    @NonNull
    private static File getGradleWrapperPropertiesFilePath(@NonNull File projectRootDir) {
        return new File(projectRootDir, GRADLEW_PROPERTIES_PATH);
    }

    @Nullable
    public static File findWrapperPropertiesFile(@NonNull File projectRootDir) {
      File wrapperPropertiesFile = getGradleWrapperPropertiesFilePath(projectRootDir);
      return wrapperPropertiesFile.isFile() ? wrapperPropertiesFile : null;
    }

    private static boolean updateGradleDistributionUrl(
            @NonNull String gradleVersion,
            @NonNull File propertiesFile) throws IOException {
        Properties properties = loadGradleWrapperProperties(propertiesFile);
        String gradleDistributionUrl = getGradleDistributionUrl(gradleVersion, false);
        String property = properties.getProperty(GRADLEW_DISTRIBUTION_URL_PROPERTY_NAME);
        if (property != null
                && (property.equals(gradleDistributionUrl) || property
                        .equals(getGradleDistributionUrl(gradleVersion, true)))) {
            return false;
        }
        properties.setProperty(GRADLEW_DISTRIBUTION_URL_PROPERTY_NAME, gradleDistributionUrl);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(propertiesFile);
            properties.store(out, null);
            return true;
        } finally {
            Closeables.close(out, true);
        }
    }

    @NonNull
    private static Properties loadGradleWrapperProperties(@NonNull File propertiesFile)
            throws IOException {
        Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(propertiesFile);
            properties.load(fileInputStream);
            return properties;
        } finally {
            Closeables.close(fileInputStream, true);
        }
    }

    @NonNull
    private static String getGradleDistributionUrl(@NonNull String gradleVersion,
            boolean binOnly) {
        String suffix = binOnly ? "bin" : "all";
        return String.format("http://services.gradle.org/distributions/gradle-%1$s-" + suffix
                + ".zip", gradleVersion);
    }
}
