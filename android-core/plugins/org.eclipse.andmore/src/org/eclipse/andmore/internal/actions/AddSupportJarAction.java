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

package org.eclipse.andmore.internal.actions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.sdk.AdtConsoleSdkLog;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.repository.api.LocalPackage;
import com.android.repository.io.FileOp;
import com.android.repository.io.FileOpUtils;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;
import com.android.sdkuilib.internal.repository.ui.AdtUpdateDialog;
import com.android.utils.Pair;

/**
 * An action to add the android-support-v4.jar support library
 * to the selected project.
 * <p/>
 * This should be used by the GLE. The action itself is currently more
 * like an example of how to invoke the new {@link AdtUpdateDialog}.
 * <p/>
 * TODO: make this more configurable.
 */
public class AddSupportJarAction implements IObjectActionDelegate {

    /** The vendor ID of the support library. */
    private static final String VENDOR_ID = "android";                             //$NON-NLS-1$
    /** The path ID of the support library. */
    private static final String SUPPORT_ID = "support";                            //$NON-NLS-1$
    /** The path ID of the compatibility library (which was its id for releases 1-3). */
    private static final String COMPATIBILITY_ID = "compatibility";                //$NON-NLS-1$
    private static final String FD_GRIDLAYOUT = "gridlayout";                      //$NON-NLS-1$
    private static final String FD_V7 = "v7";                                      //$NON-NLS-1$
    private static final String FD_V4 = "v4";                                      //$NON-NLS-1$
    private static final String FD_V13 = "v13";                                    //$NON-NLS-1$
    private static final String FD_APPCOMPAT = "appcompat";                        //$NON-NLS-1$
    private static final String FD_LIBS = "libs";                                  //$NON-NLS-1$
    private static final String ANDROID_SUPPORT_V4_JAR = "android-support-v4.jar"; //$NON-NLS-1$
    private static final String ANDROID_SUPPORT_V13_JAR = "android-support-v13.jar";//$NON-NLS-1$
    private static final String APPCOMPAT_V7_JAR = "android-support-v7-appcompat.jar";//$NON-NLS-1$
    private static final String APP_COMPAT_LIB_NAME = "appcompat_v7";               //$NON-NLS-1$
    private ISelection mSelection;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    @Override
    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {

            for (Iterator<?> it = ((IStructuredSelection) mSelection).iterator();
                    it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element)
                            .getAdapter(IProject.class);
                }
                if (project != null) {
                    install(project);
                }
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    /**
     * Install the support jar into the given project.
     *
     * @param project The Android project to install the support jar into
     * @return true if the installation was successful
     */
    public static boolean install(final IProject project) {
        File jarPath = installSupport(-1);
        if (jarPath != null) {
            try {
                return copyJarIntoProject(project, jarPath) != null;
            } catch (Exception e) {
                AndmoreAndroidPlugin.log(e, null);
            }
        }

        return false;
    }

    /**
     * Installs the Android Support library into the SDK extras/ folder. If a minimum
     * revision number is specified, this method will check whether the package is already
     * installed, and if the installed revision is at least as high as the requested revision,
     * this method will exit without performing an update.
     *
     * @param minimumRevision a minimum revision, or -1 to upgrade
     *            unconditionally. Note that this does <b>NOT</b> specify which
     *            revision to install; the latest version will always be
     *            installed.
     * @return the location of the support jar file, or null if something went
     *            wrong
     */
    @Nullable
    public static File installSupport(int minimumRevision) {

        final Sdk sdk = Sdk.getCurrent();
        if (sdk == null) {
            AndmoreAndroidPlugin.printErrorToConsole(
                    AddSupportJarAction.class.getSimpleName(),   // tag
                    "Error: Android SDK is not loaded yet."); //$NON-NLS-1$
            return null;
        }

        String sdkLocation = sdk.getSdkOsLocation();
        if (minimumRevision > 0) {
            File path = getSupportJarFile();
            if (path != null) {
                assert path.exists(); // guaranteed by the getSupportJarFile call
                int installedRevision = getInstalledRevision();
                if (installedRevision != -1 && minimumRevision <= installedRevision) {
                    return path;
                }
            }
        }

        // TODO: For the generic action, check the library isn't in the project already.

        // First call the package manager to make sure the package is installed
        // and get the installation path of the library.

        AdtUpdateDialog window = new AdtUpdateDialog(
                AndmoreAndroidPlugin.getShell(),
                new AdtConsoleSdkLog(),
                sdkLocation);

        Pair<Boolean, File> result = window.installExtraPackage(VENDOR_ID, SUPPORT_ID);

        // TODO: Make sure the version is at the required level; we know we need at least one
        // containing the v7 support

        if (!result.getFirst().booleanValue()) {
            AndmoreAndroidPlugin.printErrorToConsole("Failed to install Android Support library");
            return null;
        }

        // TODO these "v4" values needs to be dynamic, e.g. we could try to match
        // vN/android-support-vN.jar. Eventually we'll want to rely on info from the
        // package manifest anyway so this is irrelevant.

        File path = new File(result.getSecond(), FD_V4);
        final File jarPath = new File(path, ANDROID_SUPPORT_V4_JAR);

        if (!jarPath.isFile()) {
            AndmoreAndroidPlugin.printErrorToConsole("Android Support Jar not found:",
                    jarPath.getAbsolutePath());
            return null;
        }

        return jarPath;
    }

    /**
     * Returns the installed revision number of the Android Support
     * library, or -1 if the package is not installed.
     *
     * @return the installed revision number, or -1
     */
    public static int getInstalledRevision() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            String sdkLocation = sdk.getSdkOsLocation();
//            SdkManager manager = SdkManager.createManager(sdkLocation, NullLogger.getLogger());
            Map<String, Integer> versions = Collections.emptyMap(); //manager.getExtrasVersions();
            Integer version = versions.get(VENDOR_ID + '/' + SUPPORT_ID);
            if (version == null) {
                // Check the old compatibility library. When the library is updated in-place
                // the manager doesn't change its folder name (since that is a source of
                // endless issues on Windows.)
                version = versions.get(VENDOR_ID + '/' + COMPATIBILITY_ID);
            }
            if (version != null) {
                return version.intValue();
            }
        }

        return -1;
    }

    /**
     * Similar to {@link #install}, but rather than copy a jar into the given
     * project, it creates a new library project in the workspace for the
     * support library, and adds a library dependency on the newly
     * installed library from the given project.
     *
     * @param project the project to add a dependency on the library to
     * @param waitForFinish If true, block until the task has finished
     * @return true if the installation was successful (or if
     *         <code>waitForFinish</code> is false, if the installation is
     *         likely to be successful - e.g. the user has at least agreed to
     *         all installation prompts.)
     */
    public static boolean installGridLayoutLibrary(final IProject project, boolean waitForFinish) {
        final IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null) {

            File supportPath = getSupportPackageDir();
            if (!supportPath.isDirectory()) {
                File path = installSupport(8); // GridLayout arrived in rev 7 and fixed in rev 8
                if (path == null) {
                    return false;
                }
                assert path.equals(supportPath);
            }
            File libraryPath = new File(supportPath, FD_V7 + File.separator + FD_GRIDLAYOUT);
            if (!libraryPath.isDirectory()) {
                // Upgrade support package: it's out of date. The SDK manager will
                // perform an upgrade to the latest version if the package is already installed.
                File path = installSupport(-1);
                if (path == null) {
                    return false;
                }
                assert path.equals(libraryPath) : path;
            }

            // Create workspace copy of the project and add library dependency
            IProject libraryProject = createLibraryProject(libraryPath, project,
                    "gridlayout_v7", waitForFinish); //$NON-NLS-1$
            if (libraryProject != null) {
                return addLibraryDependency(libraryProject, project, waitForFinish);
            }
        }

        return false;
    }

    /**
     * Similar to {@link #install}, but rather than copy a jar into the given
     * project, it creates a new library project in the workspace for the
     * support library, and adds a library dependency on the newly
     * installed library from the given project.
     *
     * @param project the project to add a dependency on the library to
     * @param waitForFinish If true, block until the task has finished
     * @return true if the installation was successful (or if
     *         <code>waitForFinish</code> is false, if the installation is
     *         likely to be successful - e.g. the user has at least agreed to
     *         all installation prompts.)
     */
    public static boolean installAppCompatLibrary(final IProject project, boolean waitForFinish) {
        final IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null) {

            // Don't add in the library if it already exists
            ProjectState state = Sdk.getProjectState(project);
            ProjectPropertiesWorkingCopy copy = state.getProperties().makeWorkingCopy();
            for (String property : copy.keySet()) {
                if (property.startsWith(ProjectProperties.PROPERTY_LIB_REF)) {
                    String libraryReference = copy.getProperty(property);
                    if (libraryReference != null && libraryReference.contains(APP_COMPAT_LIB_NAME)) {
                        return true;
                    }
                }
            }

            File supportPath = getSupportPackageDir();
            if (!supportPath.isDirectory()) {
                File path = installSupport(7);
                if (path == null) {
                    return false;
                }
                assert path.equals(supportPath);
            }
            File libraryPath = new File(supportPath, FD_V7 + File.separator + FD_APPCOMPAT);
            if (!libraryPath.isDirectory()) {
                // Upgrade support package: it's out of date. The SDK manager will
                // perform an upgrade to the latest version if the package is already installed.
                File path = installSupport(-1);
                if (path == null) {
                    return false;
                }
                assert path.equals(libraryPath) : path;
            }

            // Check to see if there's already a version of the library available
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            IProject libraryProject = root.getProject(APP_COMPAT_LIB_NAME);
            if (!libraryProject.exists()) {
            	// Create workspace copy of the project and add library dependency
            	libraryProject = createLibraryProject(libraryPath, project,
            			APP_COMPAT_LIB_NAME, waitForFinish);
            }
            if (libraryProject != null) {
                return addLibraryDependency(libraryProject, project, waitForFinish);
            }
        }

        return false;
    }

    /**
     * Returns the directory containing the support libraries (v4, v7, v13,
     * ...), which may or may not exist
     *
     * @return a path to the support library or null
     */
    private static File getSupportPackageDir() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            LocalPackage pkg = sdk.getAndroidSdkHandler().getLatestLocalPackageForPrefix("extras;android;support", false, new FakeProgressIndicator());
            if (pkg != null) {
                File supportPath = pkg.getLocation();
                return supportPath;
            }

            // Check the old compatibility library. When the library is updated in-place
            // the manager doesn't change its folder name (since that is a source of
            // endless issues on Windows.)
            pkg = sdk.getAndroidSdkHandler().getLatestLocalPackageForPrefix("extras;android;compatibility", false, new FakeProgressIndicator());
            if (pkg != null) {
                File supportPath = pkg.getLocation();
                return supportPath;
            }
        }
        return null;
    }

    /**
     * Returns a path to the installed jar file for the support library,
     * or null if it does not exist
     *
     * @return a path to the v4.jar or null
     */
    @Nullable
    public static File getSupportJarFile() {
        File supportDir = getSupportPackageDir();
        if (supportDir != null) {
            File path = new File(supportDir, FD_V4 + File.separator + ANDROID_SUPPORT_V4_JAR);
            if (path.exists()) {
                return path;
            }
        }

        return null;
    }

    /**
     * Returns a path to the installed jar file for the support library,
     * or null if it does not exist
     *
     * @return a path to the v13.jar or null
     */
    @Nullable
    public static File getSupport13JarFile() {
        File supportDir = getSupportPackageDir();
        if (supportDir != null) {
            File path = new File(supportDir, FD_V13 + File.separator + ANDROID_SUPPORT_V13_JAR);
            if (path.exists()) {
                return path;
            }
        }

        return null;
    }

    /**
     * Creates a library project in the Eclipse workspace out of the grid layout project
     * in the SDK tree.
     *
     * @param libraryPath the path to the directory tree containing the project contents
     * @param project the project to copy the SDK target out of
     * @param waitForFinish whether the operation should finish before this method returns
     * @return a library project, or null if it fails for some reason
     */
    private static IProject createLibraryProject(
            final File libraryPath,
            final IProject project,
            final String libraryName,
            boolean waitForFinish) {

        // Install a new library into the workspace. This is a copy rather than
        // a reference to the support library version such that modifications
        // do not modify the pristine copy in the SDK install area.

        final IProject newProject;
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();

            String name = AdtUtils.getUniqueProjectName(
                    libraryName, "_"); //$NON-NLS-1$
            newProject = root.getProject(name);
            IProjectDescription description = workspace.newProjectDescription(name);
            String[] natures = new String[] { AndmoreAndroidConstants.NATURE_DEFAULT, JavaCore.NATURE_ID };
            description.setNatureIds(natures);
            newProject.create(description, monitor);

            // Copy in the files recursively
            IFileSystem fileSystem = EFS.getLocalFileSystem();
            IFileStore sourceDir = fileSystem.getStore(libraryPath.toURI());
            IFileStore destDir = fileSystem.getStore(newProject.getLocationURI());
            sourceDir.copy(destDir, EFS.OVERWRITE, null);

            // Make sure the src folder exists
            destDir.getChild(SdkConstants.SRC_FOLDER).mkdir(0, null /*monitor*/);

            // Set the android platform to the same level as the calling project
            ProjectState state = Sdk.getProjectState(project);
            String target = state.getProperties().getProperty(ProjectProperties.PROPERTY_TARGET);
            if (target != null && target.length() > 0) {
                ProjectProperties properties = ProjectProperties.load(
                        destDir.toLocalFile(EFS.NONE, new NullProgressMonitor()).getPath(),
                        PropertyType.PROJECT);
                ProjectPropertiesWorkingCopy copy = properties.makeWorkingCopy();
                copy.setProperty(ProjectProperties.PROPERTY_TARGET, target);
                try {
                    copy.save();
                } catch (Exception e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }

			// Resources copied from the SDK can have old namespace references
			updateClasspath(newProject.getFile(".project"), monitor);
			updateClasspath(newProject.getFile(".classpath"), monitor);

            newProject.open(monitor);

            return newProject;
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
            return null;
        }
    }

	private static void updateClasspath(IFile file, IProgressMonitor monitor) {
		BufferedReader reader = null;
		StringBuilder builder = new StringBuilder();
		try {
			File osFile = new File(file.getLocationURI());
			reader = new BufferedReader(new FileReader(osFile));
			int read = -1;
			char[] bytes = new char[1024];
			while ((read = reader.read(bytes)) != -1) {
				builder.append(bytes, 0, read);
			}
		} catch (Exception e) {
			// do nothing
			return;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					return;
				}
			}
		}
		try {
			String content = builder.toString().replaceAll("com.android.ide.eclipse.adt", "org.eclipse.andmore");
			file.setContents(new ByteArrayInputStream(content.getBytes()), true, false, monitor);
		} catch (CoreException e) {
		}
	}

    /**
     * Adds a library dependency on the given library into the given project.
     *
     * @param libraryProject the library project to depend on
     * @param dependentProject the project to write the dependency into
     * @param waitForFinish whether this method should wait for the job to
     *            finish
     * @return true if the operation succeeded
     */
    public static boolean addLibraryDependency(
            final IProject libraryProject,
            final IProject dependentProject,
            boolean waitForFinish) {

        // Now add library dependency

        // Run an Eclipse asynchronous job to update the project
        Job job = new Job("Add Support Library Dependency to Project") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Add library dependency to project build path", 3);
                    monitor.worked(1);

                    // TODO: Add library project to the project.properties file!
                    ProjectState state = Sdk.getProjectState(dependentProject);
                    ProjectPropertiesWorkingCopy mPropertiesWorkingCopy =
                            state.getProperties().makeWorkingCopy();

                    // Get the highest version number of the libraries; there cannot be any
                    // gaps so we will assign the next library the next number
                    int nextVersion = 1;
                    for (String property : mPropertiesWorkingCopy.keySet()) {
                        if (property.startsWith(ProjectProperties.PROPERTY_LIB_REF)) {
                            String s = property.substring(
                                    ProjectProperties.PROPERTY_LIB_REF.length());
                            int version = Integer.parseInt(s);
                            if (version >= nextVersion) {
                                nextVersion = version + 1;
                            }
                        }
                    }

                    IPath relativePath = libraryProject.getLocation().makeRelativeTo(
                            dependentProject.getLocation());

                    mPropertiesWorkingCopy.setProperty(
                            ProjectProperties.PROPERTY_LIB_REF + nextVersion,
                            relativePath.toString());
                    try {
                        mPropertiesWorkingCopy.save();
                        IResource projectProp = dependentProject.findMember(
                                SdkConstants.FN_PROJECT_PROPERTIES);
                        projectProp.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
                    } catch (Exception e) {
                        String msg = String.format(
                                "Failed to save %1$s for project %2$s",
                                SdkConstants.FN_PROJECT_PROPERTIES, dependentProject.getName());
                        AndmoreAndroidPlugin.log(e, msg);
                    }

                    // Project fix-ups
                    Job fix = FixProjectAction.createFixProjectJob(libraryProject);
                    fix.schedule();
                    fix.join();

                    monitor.worked(1);

                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, IStatus.ERROR,
                            "Failed", e); //$NON-NLS-1$
                } finally {
                    if (monitor != null) {
                        monitor.done();
                    }
                }
            }
        };
        job.schedule();

        if (waitForFinish) {
            try {
                job.join();
                return job.getState() == IStatus.OK;
            } catch (InterruptedException e) {
                AndmoreAndroidPlugin.log(e, null);
            }
        }

        return true;
    }

    private static IResource copyJarIntoProject(
            IProject project,
            File jarPath) throws IOException, CoreException {
        IFolder resFolder = project.getFolder(SdkConstants.FD_NATIVE_LIBS);
        if (!resFolder.exists()) {
            resFolder.create(IResource.FORCE, true /*local*/, null);
        }

        IFile destFile = resFolder.getFile(jarPath.getName());
        IPath loc = destFile.getLocation();
        File destPath = loc.toFile();

        // Only modify the file if necessary so that we don't trigger unnecessary recompilations
        FileOp f = FileOpUtils.create();
        if (!f.isFile(destPath) || !f.isSameFile(jarPath, destPath)) {
            f.copyFile(jarPath, destPath);
            // Make sure Eclipse discovers java.io file changes
            resFolder.refreshLocal(1, new NullProgressMonitor());
        }

        return destFile;
    }

    /**
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        // pass
    }

}
