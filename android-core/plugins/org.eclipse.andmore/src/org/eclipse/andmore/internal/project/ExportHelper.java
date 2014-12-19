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

package org.eclipse.andmore.internal.project;

import static com.android.sdklib.internal.project.ProjectProperties.PROPERTY_SDK;

import com.android.SdkConstants;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AndroidPrintStream;
import org.eclipse.andmore.internal.build.BuildHelper;
import org.eclipse.andmore.internal.build.DexException;
import org.eclipse.andmore.internal.build.NativeLibInJarException;
import org.eclipse.andmore.internal.build.ProguardExecException;
import org.eclipse.andmore.internal.build.ProguardResultException;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Export helper to export release version of APKs.
 */
public final class ExportHelper {
    private static final String HOME_PROPERTY = "user.home";                    //$NON-NLS-1$
    private static final String HOME_PROPERTY_REF = "${" + HOME_PROPERTY + '}'; //$NON-NLS-1$
    private static final String SDK_PROPERTY_REF = "${" + PROPERTY_SDK + '}';   //$NON-NLS-1$
    private final static String TEMP_PREFIX = "android_";                       //$NON-NLS-1$

    /**
     * Exports a release version of the application created by the given project.
     * @param project the project to export
     * @param outputFile the file to write
     * @param key the key to used for signing. Can be null.
     * @param certificate the certificate used for signing. Can be null.
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public static void exportReleaseApk(IProject project, File outputFile, PrivateKey key,
            X509Certificate certificate, IProgressMonitor monitor) throws CoreException {

        // the export, takes the output of the precompiler & Java builders so it's
        // important to call build in case the auto-build option of the workspace is disabled.
        // Also enable dependency building to make sure everything is up to date.
        // However do not package the APK since we're going to do it manually here, using a
        // different output location.
        ProjectHelper.compileInReleaseMode(project, monitor);

        // if either key or certificate is null, ensure the other is null.
        if (key == null) {
            certificate = null;
        } else if (certificate == null) {
            key = null;
        }

        try {
            // check if the manifest declares debuggable as true. While this is a release build,
            // debuggable in the manifest will override this and generate a debug build
            IResource manifestResource = project.findMember(SdkConstants.FN_ANDROID_MANIFEST_XML);
            if (manifestResource.getType() != IResource.FILE) {
                throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format("%1$s missing.", SdkConstants.FN_ANDROID_MANIFEST_XML)));
            }

            IFileWrapper manifestFile = new IFileWrapper((IFile) manifestResource);
            boolean debugMode = AndroidManifest.getDebuggable(manifestFile);

            AndroidPrintStream fakeStream = new AndroidPrintStream(null, null, new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // do nothing
                }
            });

            ProjectState projectState = Sdk.getProjectState(project);

            // get the jumbo mode option
            String forceJumboStr = projectState.getProperty(AdtConstants.DEX_OPTIONS_FORCEJUMBO);
            Boolean jumbo = Boolean.valueOf(forceJumboStr);

            String dexMergerStr = projectState.getProperty(AdtConstants.DEX_OPTIONS_DISABLE_MERGER);
            Boolean dexMerger = Boolean.valueOf(dexMergerStr);

            BuildToolInfo buildToolInfo = getBuildTools(projectState);

            BuildHelper helper = new BuildHelper(
                    projectState,
                    buildToolInfo,
                    fakeStream, fakeStream,
                    jumbo.booleanValue(),
                    dexMerger.booleanValue(),
                    debugMode, false /*verbose*/,
                    null /*resourceMarker*/);

            // get the list of library projects
            List<IProject> libProjects = projectState.getFullLibraryProjects();

            // Step 1. Package the resources.

            // tmp file for the packaged resource file. To not disturb the incremental builders
            // output, all intermediary files are created in tmp files.
            File resourceFile = File.createTempFile(TEMP_PREFIX, SdkConstants.DOT_RES);
            resourceFile.deleteOnExit();

            // Make sure the PNG crunch cache is up to date
            helper.updateCrunchCache();

            // get the merged manifest
            IFolder androidOutputFolder = BaseProjectHelper.getAndroidOutputFolder(project);
            IFile mergedManifestFile = androidOutputFolder.getFile(
                    SdkConstants.FN_ANDROID_MANIFEST_XML);


            // package the resources.
            helper.packageResources(
                    mergedManifestFile,
                    libProjects,
                    null,   // res filter
                    0,      // versionCode
                    resourceFile.getParent(),
                    resourceFile.getName());

            // Step 2. Convert the byte code to Dalvik bytecode

            // tmp file for the packaged resource file.
            File dexFile = File.createTempFile(TEMP_PREFIX, SdkConstants.DOT_DEX);
            dexFile.deleteOnExit();

            ProjectState state = Sdk.getProjectState(project);
            String proguardConfig = state.getProperties().getProperty(
                    ProjectProperties.PROPERTY_PROGUARD_CONFIG);

            boolean runProguard = false;
            List<File> proguardConfigFiles = null;
            if (proguardConfig != null && proguardConfig.length() > 0) {
                // Be tolerant with respect to file and path separators just like
                // Ant is. Allow "/" in the property file to mean whatever the file
                // separator character is:
                if (File.separatorChar != '/' && proguardConfig.indexOf('/') != -1) {
                    proguardConfig = proguardConfig.replace('/', File.separatorChar);
                }

                Iterable<String> paths = LintUtils.splitPath(proguardConfig);
                for (String path : paths) {
                    if (path.startsWith(SDK_PROPERTY_REF)) {
                        path = AdtPrefs.getPrefs().getOsSdkFolder() +
                                path.substring(SDK_PROPERTY_REF.length());
                    } else if (path.startsWith(HOME_PROPERTY_REF)) {
                        path = System.getProperty(HOME_PROPERTY) +
                                path.substring(HOME_PROPERTY_REF.length());
                    }
                    File proguardConfigFile = new File(path);
                    if (proguardConfigFile.isAbsolute() == false) {
                        proguardConfigFile = new File(project.getLocation().toFile(), path);
                    }
                    if (proguardConfigFile.isFile()) {
                        if (proguardConfigFiles == null) {
                            proguardConfigFiles = new ArrayList<File>();
                        }
                        proguardConfigFiles.add(proguardConfigFile);
                        runProguard = true;
                    } else {
                        throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                                "Invalid proguard configuration file path " + proguardConfigFile
                                + " does not exist or is not a regular file", null));
                    }
                }

                // get the proguard file output by aapt
                if (proguardConfigFiles != null) {
                    IFile proguardFile = androidOutputFolder.getFile(AdtConstants.FN_AAPT_PROGUARD);
                    proguardConfigFiles.add(proguardFile.getLocation().toFile());
                }
            }

            Collection<String> dxInput;

            if (runProguard) {
                // get all the compiled code paths. This will contain both project output
                // folder and jar files.
                Collection<String> paths = helper.getCompiledCodePaths();

                // create a jar file containing all the project output (as proguard cannot
                // process folders of .class files).
                File inputJar = File.createTempFile(TEMP_PREFIX, SdkConstants.DOT_JAR);
                inputJar.deleteOnExit();
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(inputJar));

                // a list of the other paths (jar files.)
                List<String> jars = new ArrayList<String>();

                for (String path : paths) {
                    File root = new File(path);
                    if (root.isDirectory()) {
                        addFileToJar(jos, root, root);
                    } else if (root.isFile()) {
                        jars.add(path);
                    }
                }
                jos.close();

                // destination file for proguard
                File obfuscatedJar = File.createTempFile(TEMP_PREFIX, SdkConstants.DOT_JAR);
                obfuscatedJar.deleteOnExit();

                // run proguard
                helper.runProguard(proguardConfigFiles, inputJar, jars, obfuscatedJar,
                        new File(project.getLocation().toFile(), SdkConstants.FD_PROGUARD));

                helper.setProguardOutput(obfuscatedJar.getAbsolutePath());

                // dx input is proguard's output
                dxInput = Collections.singletonList(obfuscatedJar.getAbsolutePath());
            } else {
                // no proguard, simply get all the compiled code path: project output(s) +
                // jar file(s)
                dxInput = helper.getCompiledCodePaths();
            }

            IJavaProject javaProject = JavaCore.create(project);

            helper.executeDx(javaProject, dxInput, dexFile.getAbsolutePath());

            // Step 3. Final package

            helper.finalPackage(
                    resourceFile.getAbsolutePath(),
                    dexFile.getAbsolutePath(),
                    outputFile.getAbsolutePath(),
                    libProjects,
                    key,
                    certificate,
                    null); //resourceMarker

            // success!
        } catch (CoreException e) {
            throw e;
        } catch (ProguardResultException e) {
            String msg = String.format("Proguard returned with error code %d. See console",
                    e.getErrorCode());
            AdtPlugin.printErrorToConsole(project, msg);
            AdtPlugin.printErrorToConsole(project, (Object[]) e.getOutput());
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    msg, e));
        } catch (ProguardExecException e) {
            String msg = String.format("Failed to run proguard: %s", e.getMessage());
            AdtPlugin.printErrorToConsole(project, msg);
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    msg, e));
        } catch (DuplicateFileException e) {
            String msg = String.format(
                    "Found duplicate file for APK: %1$s\nOrigin 1: %2$s\nOrigin 2: %3$s",
                    e.getArchivePath(), e.getFile1(), e.getFile2());
            AdtPlugin.printErrorToConsole(project, msg);
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    e.getMessage(), e));
        } catch (NativeLibInJarException e) {
            String msg = e.getMessage();

            AdtPlugin.printErrorToConsole(project, msg);
            AdtPlugin.printErrorToConsole(project, (Object[]) e.getAdditionalInfo());
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    e.getMessage(), e));
        } catch (DexException e) {
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    e.getMessage(), e));
        } catch (ApkCreationException e) {
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    e.getMessage(), e));
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Failed to export application", e));
        } finally {
            // move back to a debug build.
            // By using a normal build, we'll simply rebuild the debug version, and let the
            // builder decide whether to build the full package or not.
            ProjectHelper.buildWithDeps(project, IncrementalProjectBuilder.FULL_BUILD, monitor);
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    public static BuildToolInfo getBuildTools(ProjectState projectState)
            throws CoreException {
        BuildToolInfo buildToolInfo = projectState.getBuildToolInfo();
        if (buildToolInfo == null) {
            buildToolInfo = Sdk.getCurrent().getLatestBuildTool();
        }

        if (buildToolInfo == null) {
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "No Build Tools installed in the SDK."));
        }
        return buildToolInfo;
    }

    /**
     * Exports an unsigned release APK after prompting the user for a location.
     *
     * <strong>Must be called from the UI thread.</strong>
     *
     * @param project the project to export
     */
    public static void exportUnsignedReleaseApk(final IProject project) {
        Shell shell = Display.getCurrent().getActiveShell();

        // create a default file name for the apk.
        String fileName = project.getName() + SdkConstants.DOT_ANDROID_PACKAGE;

        // Pop up the file save window to get the file location
        FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);

        fileDialog.setText("Export Project");
        fileDialog.setFileName(fileName);

        final String saveLocation = fileDialog.open();
        if (saveLocation != null) {
            new Job("Android Release Export") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        exportReleaseApk(project,
                                new File(saveLocation),
                                null, //key
                                null, //certificate
                                monitor);

                        // this is unsigned export. Let's tell the developers to run zip align
                        AdtPlugin.displayWarning("Android IDE Plug-in", String.format(
                                "An unsigned package of the application was saved at\n%1$s\n\n" +
                                "Before publishing the application you will need to:\n" +
                                "- Sign the application with your release key,\n" +
                                "- run zipalign on the signed package. ZipAlign is located in <SDK>/tools/\n\n" +
                                "Aligning applications allows Android to use application resources\n" +
                                "more efficiently.", saveLocation));

                        return Status.OK_STATUS;
                    } catch (CoreException e) {
                        AdtPlugin.displayError("Android IDE Plug-in", String.format(
                                "Error exporting application:\n\n%1$s", e.getMessage()));
                        return e.getStatus();
                    }
                }
            }.schedule();
        }
    }

    /**
     * Adds a file to a jar file.
     * The <var>rootDirectory</var> dictates the path of the file inside the jar file. It must be
     * a parent of <var>file</var>.
     * @param jar the jar to add the file to
     * @param file the file to add
     * @param rootDirectory the rootDirectory.
     * @throws IOException
     */
    private static void addFileToJar(JarOutputStream jar, File file, File rootDirectory)
            throws IOException {
        if (file.isDirectory()) {
            if (file.getName().equals("META-INF") == false) {
                for (File child: file.listFiles()) {
                    addFileToJar(jar, child, rootDirectory);
                }
            }
        } else if (file.isFile()) {
            String rootPath = rootDirectory.getAbsolutePath();
            String path = file.getAbsolutePath();
            path = path.substring(rootPath.length()).replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }

            JarEntry entry = new JarEntry(path);
            entry.setTime(file.lastModified());
            jar.putNextEntry(entry);

            // put the content of the file.
            byte[] buffer = new byte[1024];
            int count;
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                while ((count = bis.read(buffer)) != -1) {
                    jar.write(buffer, 0, count);
                }
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            jar.closeEntry();
        }
    }
}
