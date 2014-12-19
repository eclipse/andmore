/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.ProjectUtils;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidStatus;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject.SourceTypes;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Project Creation Support.
 */
public class ProjectCreationSupport {
	/**
	 * Only static calls
	 */
	private ProjectCreationSupport() {
	}

	private static final String PACKAGE_NAME = "PACKAGE"; //$NON-NLS-1$

	private static final String APP_NAME = "app_name"; //$NON-NLS-1$

	private static final String APPLICATION_NAME = "APPLICATION_NAME"; //$NON-NLS-1$

	private static final String STRING_RSRC_PREFIX = "@string/"; //$NON-NLS-1$

	private static final String MIN_SDK_VERSION = "MIN_SDK_VERSION"; //$NON-NLS-1$

	private static final String BIN_DIR = IAndroidConstants.FD_OUTPUT + IPath.SEPARATOR;

	private static final String RES_DIR = IAndroidConstants.FD_RESOURCES + IPath.SEPARATOR;

	private static final String ASSETS_DIR = IAndroidConstants.FD_ASSETS + IPath.SEPARATOR;

	private static final String DRAWABLE_DIR = IAndroidConstants.FD_DRAWABLE;

	private static final String LAYOUT_DIR = IAndroidConstants.FD_LAYOUT + IPath.SEPARATOR;

	private static final String VALUES_DIR = IAndroidConstants.FD_VALUES + IPath.SEPARATOR;

	private static final String GEN_DIR = IAndroidConstants.FD_GEN_SOURCES + IPath.SEPARATOR;

	private static final String XML_DIR = "xml" + IPath.SEPARATOR;

	private static final String TEMPLATES_DIRECTORY = "templates/"; //$NON-NLS-1$

	private static final String MANIFEST_TEMPLATE = TEMPLATES_DIRECTORY + "AndroidManifest.template"; //$NON-NLS-1$

	private static final String ACTIVITY_NAME = "ACTIVITY_NAME"; //$NON-NLS-1$

	private static final String ACTIVITY_TEMPLATE = TEMPLATES_DIRECTORY + "activity.template"; //$NON-NLS-1$

	private static final String LAUNCHER_INTENT_TEMPLATE = TEMPLATES_DIRECTORY + "launcher_intent_filter.template"; //$NON-NLS-1$

	private static final String INTENT_FILTERS = "INTENT_FILTERS"; //$NON-NLS-1$

	private static final String ACTIVITIES = "ACTIVITIES"; //$NON-NLS-1$

	private static final String USES_SDK_TEMPLATE = TEMPLATES_DIRECTORY + "uses-sdk.template"; //$NON-NLS-1$

	private static final String USES_SDK = "USES-SDK"; //$NON-NLS-1$

	private static final String ICON = "ic_launcher.png"; //$NON-NLS-1$

	private static final String JAVA_ACTIVITY_TEMPLATE = "java_file.template"; //$NON-NLS-1$

	private static final String MAIN_LAYOUT_XML = "main.xml"; //$NON-NLS-1$

	private static final String LAYOUT_TEMPLATE = "layout.template"; //$NON-NLS-1$

	private static final String STRING_HELLO_WORLD = "hello"; //$NON-NLS-1$    

	private static final String TEST_USES_LIBRARY = "TEST-USES-LIBRARY"; //$NON-NLS-1$

	private static final String TEST_INSTRUMENTATION = "TEST-INSTRUMENTATION"; //$NON-NLS-1$

	private static final String[] DPIS = { "hdpi", "ldpi", "mdpi" };

	/*
	 * Widget Project manifest creation constants
	 */

	private static final String WIDGET_TEMPLATE_FOLDER = "templates/widget_project/";

	private static final String WIDGET_MANIFEST_TEMPLATE_PATH = "templates/widget_project/AndroidWidgetManifest.template"; //$NON-NLS-1$

	private static final String WIDGET_ACTIVITY_TEMPLATE_PATH = "templates/widget_project/activity.template"; //$NON-NLS-1$

	private static final String WIDGET_RECEIVER_TEMPLATE_PATH = "templates/widget_project/receiver.template"; //$NON-NLS-1$

	private static final String WIDGET_USES_SDK_TEMPLATE_PATH = "templates/widget_project/uses-sdk.template"; //$NON-NLS-1$

	private static final String RECEIVERS = "RECEIVERS"; //$NON-NLS-1$

	private static final String WIDGET_INITIAL_LAYOUT_XML = "widget_initial_layout.xml"; //$NON-NLS-1$

	private static final String WIDGET_INFO_XML = "widget_info.xml"; //$NON-NLS-1$

	private static final String WIDGET_PROVIDER_SAMPLE_NAME = "WidgetProvider"; //$NON-NLS-1$

	private static final String WIDGET_PROVIDER_SAMPLE_TEMPLATE = "WidgetProvider.template"; //$NON-NLS-1$

	private static final String IMPORT_RESOURCE_CLASS = "IMPORT_RESOURCE_CLASS";

	/**
	 * Create a new Android Project
	 * 
	 * @param androidProject
	 * @param container
	 * @return
	 * @throws AndroidException
	 */
	public static boolean createProject(final AndroidProject androidProject, IWizardContainer container)
			throws AndroidException {
		boolean created = true;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(androidProject.getName());

		if (!canCreateProject(workspace.getRoot(), androidProject.getName())) {
			throw new AndroidException(AndroidNLS.EXC_ProjectCreationSupport_CannotCreateProjectReadOnlyWorkspace);
		} else {

			final IProjectDescription description = workspace.newProjectDescription(project.getName());

			final Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(MIN_SDK_VERSION, androidProject.getMinSdkVersion());

			if ((androidProject.getSourceType() == SourceTypes.NEW)
					|| (androidProject.getSourceType() == SourceTypes.WIDGET)) {
				/*
				 * An activity name can be of the form ".package.Class" or
				 * ".Class". The initial dot is ignored, as it is always added
				 * later in the templates.
				 */
				String activityName = androidProject.getActivityName();
				if (activityName.startsWith(".")) { //$NON-NLS-1$
					activityName = activityName.substring(1);
				}
				parameters.put(ACTIVITY_NAME, androidProject.getActivityName());
				parameters.put(PACKAGE_NAME, androidProject.getPackageName());
				parameters.put(APPLICATION_NAME, STRING_RSRC_PREFIX + APP_NAME);
				parameters.put(IMPORT_RESOURCE_CLASS, "");
			}

			/*
			 * create a dictionary of string that will contain name+content.
			 * we'll put all the strings into values/strings.xml
			 */
			final HashMap<String, String> stringDictionary = new HashMap<String, String>();
			stringDictionary.put(APP_NAME, androidProject.getApplicationName());

			if (!androidProject.isUsingDefaultLocation() && androidProject.isNewProject()) {
				Path destination = new Path(androidProject.getLocation());
				description.setLocation(destination);

				if (!FileUtil.canWrite(destination.toFile())) {
					String errMsg = NLS.bind(
							AndroidNLS.EXC_ProjectCreationSupport_CannotCreateProjectReadOnlyDestination,
							destination.toOSString());
					throw new AndroidException(errMsg);
				}

				if (!validateNewProjectLocationIsEmpty(destination)) {
					throw new AndroidException(AndroidNLS.UI_ProjectCreationSupport_NonEmptyFolder);
				}
			}

			if (androidProject.getSourceType() == SourceTypes.EXISTING) {
				Path destination = new Path(androidProject.getLocation());
				description.setLocation(destination);
			}

			/*
			 * Create a monitored operation to create the actual project
			 */
			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				@Override
				protected void execute(IProgressMonitor monitor) throws InvocationTargetException {

					createProjectAsync(project, androidProject, description, monitor, parameters, stringDictionary);
				}
			};

			/*
			 * Run the operation in a different thread
			 */
			created = runAsyncOperation(op, container);
		}

		return created;

	}

	/**
	 * Create android project.
	 * 
	 * @param project
	 * @param androidProject
	 * @param description
	 * @param monitor
	 * @param parameters
	 * @param stringDictionary
	 * @throws InvocationTargetException
	 */
	protected static void createProjectAsync(IProject project, AndroidProject androidProject,
			IProjectDescription description, IProgressMonitor monitor, Map<String, Object> parameters,
			Map<String, String> stringDictionary) throws InvocationTargetException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_CopyingSamplesMonitorTaskTitle, 1000);
		try {
			// Create project and open it
			project.create(description, new SubProgressMonitor(monitor, 100));
			if (monitor.isCanceled()) {
				undoProjectCreation(project);
				throw new OperationCanceledException();
			}
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 100));

			ProjectUtils.setupAndroidNatures(project, monitor);

			// Create folders in the project if they don't already exist
			createDefaultDir(project, IAndroidConstants.WS_ROOT, BIN_DIR, new SubProgressMonitor(monitor, 40));
			createDefaultDir(project, IAndroidConstants.WS_ROOT, RES_DIR, new SubProgressMonitor(monitor, 40));
			createDefaultDir(project, IAndroidConstants.WS_ROOT, ASSETS_DIR, new SubProgressMonitor(monitor, 40));
			createDefaultDir(project, IAndroidConstants.WS_ROOT, GEN_DIR, new SubProgressMonitor(monitor, 40));

			switch (androidProject.getSourceType()) {
			case NEW:
				// Create the source folders in the project if they don't
				// already exist
				List<String> sourceFolders = androidProject.getSourceFolders();
				for (String sourceFolder : sourceFolders) {
					createDefaultDir(project, IAndroidConstants.WS_ROOT, sourceFolder, new SubProgressMonitor(monitor,
							40));
				}

				// Create the resource folders in the project if they don't
				// already exist.
				int apiLevel = androidProject.getSdkTarget().getVersion().getApiLevel();
				if (apiLevel < 4) {
					createDefaultDir(project, RES_DIR, DRAWABLE_DIR + File.separator, new SubProgressMonitor(monitor,
							40));
				} else {
					for (String dpi : DPIS) {
						createDefaultDir(project, RES_DIR, DRAWABLE_DIR + "-" + dpi + File.separator,
								new SubProgressMonitor(monitor, 40));
					}
				}
				createDefaultDir(project, RES_DIR, LAYOUT_DIR, new SubProgressMonitor(monitor, 40));
				createDefaultDir(project, RES_DIR, VALUES_DIR, new SubProgressMonitor(monitor, 40));

				// Create files in the project if they don't already exist
				createManifest(project, parameters, stringDictionary, new SubProgressMonitor(monitor, 80));
				// add the default app icon
				addIcon(project, apiLevel, new SubProgressMonitor(monitor, 100));
				// Create the default package components
				String primarySrcFolder = IAndroidConstants.FD_SOURCES;
				if (!sourceFolders.contains(IAndroidConstants.FD_SOURCES)) {
					primarySrcFolder = sourceFolders.get(0);
				}
				addInitialCode(project, primarySrcFolder, parameters, stringDictionary, new SubProgressMonitor(monitor,
						200));
				// add the string definition file if needed
				if (stringDictionary.size() > 0) {
					EclipseUtils.createOrUpdateDictionaryFile(project, stringDictionary, null, new SubProgressMonitor(
							monitor, 100));
				}

				break;
			case EXISTING:
				createDefaultDir(project, IAndroidConstants.WS_ROOT, GEN_DIR, new SubProgressMonitor(monitor, 650));
				break;
			case SAMPLE:
				monitor.setTaskName(AndroidNLS.UI_ProjectCreationSupport_CopyingSamplesMonitorMessage);
				FileUtil.copyDir(androidProject.getSample().getFolder(), project.getLocation().toFile());
				project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 650));
				break;
			case WIDGET:
				// Create the source folders in the project if they don't
				// already exist
				List<String> widgetSourceFolders = androidProject.getSourceFolders();
				for (String sourceFolder : widgetSourceFolders) {
					createDefaultDir(project, IAndroidConstants.WS_ROOT, sourceFolder, new SubProgressMonitor(monitor,
							40));
				}

				// Create the resource folders in the project if they don't
				// already exist.
				int widgetApiLevel = androidProject.getSdkTarget().getVersion().getApiLevel();
				if (widgetApiLevel < 4) {
					createDefaultDir(project, RES_DIR, DRAWABLE_DIR + File.separator, new SubProgressMonitor(monitor,
							40));
				} else {
					for (String dpi : DPIS) {
						createDefaultDir(project, RES_DIR, DRAWABLE_DIR + "-" + dpi + File.separator,
								new SubProgressMonitor(monitor, 40));
					}
				}
				createDefaultDir(project, RES_DIR, LAYOUT_DIR, new SubProgressMonitor(monitor, 40));
				createDefaultDir(project, RES_DIR, VALUES_DIR, new SubProgressMonitor(monitor, 40));
				createDefaultDir(project, RES_DIR, XML_DIR, new SubProgressMonitor(monitor, 40));

				// Create files in the project if they don't already exist
				createWidgetManifest(project, parameters, stringDictionary, new SubProgressMonitor(monitor, 80));
				// add the default app icon
				addIcon(project, widgetApiLevel, new SubProgressMonitor(monitor, 100));
				// Create the default package components
				String widgetPrimarySrcFolder = IAndroidConstants.FD_SOURCES;
				if (!widgetSourceFolders.contains(IAndroidConstants.FD_SOURCES)) {
					primarySrcFolder = widgetSourceFolders.get(0);
				}
				addInitialWidgetCode(project, widgetPrimarySrcFolder, parameters, stringDictionary,
						new SubProgressMonitor(monitor, 200));
				// add the string definition file if needed
				if (stringDictionary.size() > 0) {
					EclipseUtils.createOrUpdateDictionaryFile(project, stringDictionary, null, new SubProgressMonitor(
							monitor, 100));
				}

				break;
			}

			// Setup class path
			IJavaProject javaProject = JavaCore.create(project);
			setupSourceFolders(javaProject, androidProject.getSourceFolders(), new SubProgressMonitor(monitor, 40));

			// Set output location
			javaProject
					.setOutputLocation(project.getFolder(BIN_DIR).getFullPath(), new SubProgressMonitor(monitor, 40));
			SdkUtils.associate(project, androidProject.getSdkTarget());
			ProjectUtils.fixProject(project);
		} catch (CoreException e) {
			undoProjectCreation(project);
			throw new InvocationTargetException(e);
		} catch (IOException e) {
			undoProjectCreation(project);
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Add initial code
	 * 
	 * @param project
	 * @param sourceFolder
	 * @param parameters
	 * @param stringDictionary
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void addInitialCode(IProject project, String sourceFolder, Map<String, Object> parameters,
			Map<String, String> stringDictionary, IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Configuring_Sample_Source_Task, 700);

		try {
			IFolder pkgFolder = project.getFolder(sourceFolder);

			Map<String, Object> processed_parameters = processSampleActivity(parameters);
			String activityName = (String) processed_parameters.get(ACTIVITY_NAME);
			String packageName = (String) processed_parameters.get(PACKAGE_NAME);

			pkgFolder = createPackageFolders(new SubProgressMonitor(monitor, 300), pkgFolder, packageName);

			if (activityName != null) {
				createSampleActivity(new SubProgressMonitor(monitor, 200), pkgFolder, processed_parameters,
						activityName);
			}

			IFolder layoutfolder = project.getFolder(RES_DIR + LAYOUT_DIR);
			IFile file = layoutfolder.getFile(MAIN_LAYOUT_XML);
			if (!file.exists()) {
				copyTemplateFile(LAYOUT_TEMPLATE, file, parameters, new SubProgressMonitor(monitor, 100));
				if (activityName != null) {
					stringDictionary.put(STRING_HELLO_WORLD,
							NLS.bind(AndroidNLS.GEN_ProjectCreationSupport_HelloWorldWithName, activityName));
				} else {
					stringDictionary.put(STRING_HELLO_WORLD, AndroidNLS.GEN_ProjectCreationSupport_HelloWorldSimple);
				}
				monitor.worked(100);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Add initial widget code
	 * 
	 * @param project
	 * @param sourceFolder
	 * @param parameters
	 * @param stringDictionary
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void addInitialWidgetCode(IProject project, String sourceFolder, Map<String, Object> parameters,
			Map<String, String> stringDictionary, IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Configuring_Sample_Source_Task, 800);

		try {
			IFolder pkgFolder = project.getFolder(sourceFolder);

			Map<String, Object> processed_parameters = processSampleActivity(parameters);
			String activityName = (String) processed_parameters.get(ACTIVITY_NAME);
			String packageName = (String) processed_parameters.get(PACKAGE_NAME);

			pkgFolder = createPackageFolders(new SubProgressMonitor(monitor, 200), pkgFolder, packageName);

			// Create sample activity
			if (activityName != null) {
				createSampleActivity(new SubProgressMonitor(monitor, 100), pkgFolder, processed_parameters,
						activityName);
			}

			// Create sample widget provider
			createSampleWidgetProvider(new SubProgressMonitor(monitor, 100), pkgFolder, processed_parameters);

			// Layout xml file
			IFolder layoutfolder = project.getFolder(RES_DIR + LAYOUT_DIR);

			IFile file = layoutfolder.getFile(MAIN_LAYOUT_XML);
			if (!file.exists()) {
				copyTemplateFile(LAYOUT_TEMPLATE, file, parameters, new SubProgressMonitor(monitor, 100));
				if (activityName != null) {
					stringDictionary.put(STRING_HELLO_WORLD,
							NLS.bind(AndroidNLS.GEN_ProjectCreationSupport_HelloWorldWithName, activityName));
				} else {
					stringDictionary.put(STRING_HELLO_WORLD, AndroidNLS.GEN_ProjectCreationSupport_HelloWorldSimple);
				}
				monitor.worked(100);
			}

			// Widget initial layout xml file
			IFile initial_layout_file = layoutfolder.getFile(WIDGET_INITIAL_LAYOUT_XML);
			if (!initial_layout_file.exists()) {
				copyWidgetTemplateFile(WIDGET_INITIAL_LAYOUT_XML, initial_layout_file, processed_parameters,
						new SubProgressMonitor(monitor, 100));
				monitor.worked(100);
			}

			// Widget info xml file
			IFolder xmlFolder = project.getFolder(RES_DIR + XML_DIR);

			IFile widget_info_file = xmlFolder.getFile(WIDGET_INFO_XML);
			if (!widget_info_file.exists()) {
				copyWidgetTemplateFile(WIDGET_INFO_XML, widget_info_file, processed_parameters, new SubProgressMonitor(
						monitor, 100));
				monitor.worked(100);
			}

		} finally {
			monitor.done();
		}
	}

	private static void createSampleActivity(IProgressMonitor monitor, IFolder pkgFolder,
			Map<String, Object> processed_parameters, String activityName) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Configuring_Sample_Activity_Task, 100);
		try {
			IFile file = pkgFolder.getFile(activityName + IAndroidConstants.DOT_JAVA);
			if (!file.exists()) {
				monitor.worked(10);
				copyTemplateFile(JAVA_ACTIVITY_TEMPLATE, file, processed_parameters,
						new SubProgressMonitor(monitor, 90));
			}
		} finally {
			monitor.done();
		}
	}

	private static void createSampleWidgetProvider(IProgressMonitor monitor, IFolder pkgFolder,
			Map<String, Object> processed_parameters) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Configuring_Sample_Widget_Provider, 100);
		try {
			IFile file = pkgFolder.getFile(WIDGET_PROVIDER_SAMPLE_NAME + IAndroidConstants.DOT_JAVA);
			if (!file.exists()) {
				monitor.worked(10);
				copyWidgetTemplateFile(WIDGET_PROVIDER_SAMPLE_TEMPLATE, file, processed_parameters,
						new SubProgressMonitor(monitor, 90));
			}
		} finally {
			monitor.done();
		}
	}

	private static IFolder createPackageFolders(IProgressMonitor monitor, IFolder pkgFolder, String packageName)
			throws CoreException {
		String[] components = packageName.split(IAndroidConstants.RE_DOT);
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Preparing_Java_Packages_Task, components.length * 100);
		try {
			for (String component : components) {
				pkgFolder = pkgFolder.getFolder(component);
				if (!pkgFolder.exists()) {
					pkgFolder.create(true, true, new SubProgressMonitor(monitor, 100));
				}
			}
		} finally {
			monitor.done();
		}

		return pkgFolder;
	}

	private static Map<String, Object> processSampleActivity(Map<String, Object> parameters) {
		String activityName = (String) parameters.get(ACTIVITY_NAME);

		Map<String, Object> processed_parameters = new HashMap<String, Object>(parameters);
		if ((activityName != null) && activityName.contains(".")) //$NON-NLS-1$
		{
			String packageName = (String) parameters.get(PACKAGE_NAME);
			packageName += "." + activityName.substring(0, activityName.lastIndexOf('.')); //$NON-NLS-1$
			activityName = activityName.substring(activityName.lastIndexOf('.'));

			processed_parameters.put(PACKAGE_NAME, packageName);
			processed_parameters.put(ACTIVITY_NAME, activityName);
		}

		return processed_parameters;
	}

	/**
	 * Copy template files
	 * 
	 * @param resourceFilename
	 * @param destFile
	 * @param parameters
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void copyTemplateFile(String resourceFilename, IFile destFile, Map<String, Object> parameters,
			IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Preparing_Template_File_Task, 150);
		InputStream stream = null;
		try {
			String template = readEmbeddedTextFileADT(TEMPLATES_DIRECTORY + resourceFilename, parameters);
			monitor.worked(50);
			stream = new ByteArrayInputStream(template.getBytes("UTF-8")); //$NON-NLS-1$
			destFile.create(stream, false, new SubProgressMonitor(monitor, 100));

		} finally {
			if (stream != null) {
				stream.close();
			}
			monitor.done();
		}
	}

	/**
	 * Copy widget template files
	 * 
	 * @param resourceFilename
	 * @param destFile
	 * @param parameters
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void copyWidgetTemplateFile(String resourceFilename, IFile destFile, Map<String, Object> parameters,
			IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Preparing_Template_File_Task, 150);
		InputStream stream = null;
		try {
			String template = readEmbeddedTextFileStudio(WIDGET_TEMPLATE_FOLDER + resourceFilename, parameters);
			monitor.worked(50);
			stream = new ByteArrayInputStream(template.getBytes("UTF-8")); //$NON-NLS-1$
			destFile.create(stream, false, new SubProgressMonitor(monitor, 100));

		} finally {
			if (stream != null) {
				stream.close();
			}
			monitor.done();
		}
	}

	/**
	 * Add Icon to the project
	 * 
	 * @param project
	 * @param apiLevel
	 * @param monitor
	 * @throws CoreException
	 */
	private static void addIcon(IProject project, int apiLevel, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Configuring_Project_Icon_Task, 1000);
		try {
			if (apiLevel < 4) {
				IFile imageFile = project.getFile(RES_DIR + IPath.SEPARATOR + DRAWABLE_DIR + IPath.SEPARATOR + ICON);
				if (!imageFile.exists()) {
					String fileName = ICON.substring(0, ICON.length() - 4) + "_" + DPIS[2]
							+ ICON.substring(ICON.length() - 4);
					createImageFromTemplate(monitor, imageFile, fileName);
				}
			} else {
				for (String dpi : DPIS) {
					IFile imageFile = project.getFile(RES_DIR + IPath.SEPARATOR + DRAWABLE_DIR + "-" + dpi
							+ IPath.SEPARATOR + ICON);
					if (!imageFile.exists()) {
						String fileName = ICON.substring(0, ICON.length() - 4) + "_" + dpi
								+ ICON.substring(ICON.length() - 4);
						createImageFromTemplate(monitor, imageFile, fileName);
					}
				}
			}
		} finally {
			monitor.done();
		}

	}

	private static void createImageFromTemplate(IProgressMonitor monitor, IFile imageFile, String fileName)
			throws CoreException {
		byte[] buffer = AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + fileName);

		if (buffer != null) {
			InputStream stream = null;
			try {
				stream = new ByteArrayInputStream(buffer);
				imageFile.create(stream, IResource.NONE, new SubProgressMonitor(monitor, 1000));
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					StudioLogger.info("Create image from template could not close stream. " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Adds the manifest to the project.
	 * 
	 * @param project
	 * @param parameters
	 * @param stringDictionary
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void createManifest(IProject project, Map<String, Object> parameters,
			Map<String, String> stringDictionary, IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Creating_Manifest_File_Task, 300);
		try {
			IFile manifestFile = project.getFile(IAndroidConstants.FN_ANDROID_MANIFEST);
			if (!manifestFile.exists()) {
				monitor.setTaskName(AndroidNLS.UI_ProjectCreationSupport_Reading_Template_File_Task);
				String manifestTemplate = readEmbeddedTextFileADT(MANIFEST_TEMPLATE, parameters);
				monitor.worked(10);
				if (parameters.containsKey(ACTIVITY_NAME)) {
					String activities = readEmbeddedTextFileADT(ACTIVITY_TEMPLATE, parameters);
					String intent = AdtPlugin.readEmbeddedTextFile(LAUNCHER_INTENT_TEMPLATE);
					activities = activities.replaceAll(INTENT_FILTERS, intent);
					manifestTemplate = manifestTemplate.replaceAll(ACTIVITIES, activities);
					monitor.worked(90);
				} else {
					manifestTemplate = manifestTemplate.replaceAll(ACTIVITIES, ""); //$NON-NLS-1$
					monitor.worked(90);
				}

				// We don't currently supports the TEST parameters. So let's
				// just remove the unused tags.
				manifestTemplate = manifestTemplate.replaceAll(TEST_USES_LIBRARY, ""); //$NON-NLS-1$
				manifestTemplate = manifestTemplate.replaceAll(TEST_INSTRUMENTATION, ""); //$NON-NLS-1$

				String minSdkVersion = (String) parameters.get(MIN_SDK_VERSION);
				if ((minSdkVersion != null) && (minSdkVersion.length() > 0)) {
					String usesSdk = readEmbeddedTextFileADT(USES_SDK_TEMPLATE, parameters);
					manifestTemplate = manifestTemplate.replaceAll(USES_SDK, usesSdk);
					monitor.worked(50);
				} else {
					manifestTemplate = manifestTemplate.replaceAll(USES_SDK, ""); //$NON-NLS-1$
					monitor.worked(50);
				}

				InputStream stream = null;
				try {
					stream = new ByteArrayInputStream(manifestTemplate.getBytes("UTF-8")); //$NON-NLS-1$
					manifestFile.create(stream, IResource.NONE, new SubProgressMonitor(monitor, 150));
				} finally {
					try {
						if (stream != null) {
							stream.close();
						}
					} catch (IOException e) {
						StudioLogger.info("Could not close stream while creating manifest", e.getMessage());
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adds the widget manifest to the project.
	 * 
	 * @param project
	 * @param parameters
	 * @param stringDictionary
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	private static void createWidgetManifest(IProject project, Map<String, Object> parameters,
			Map<String, String> stringDictionary, IProgressMonitor monitor) throws CoreException, IOException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Creating_Manifest_File_Task, 300);
		try {
			IFile manifestFile = project.getFile(IAndroidConstants.FN_ANDROID_MANIFEST);
			if (!manifestFile.exists()) {
				monitor.setTaskName(AndroidNLS.UI_ProjectCreationSupport_Reading_Template_File_Task);

				// Manifest skeleton
				String manifestTemplate = readEmbeddedTextFileStudio(WIDGET_MANIFEST_TEMPLATE_PATH, parameters);
				monitor.worked(10);
				// Activity information
				if (parameters.containsKey(ACTIVITY_NAME)) {
					String activities = readEmbeddedTextFileStudio(WIDGET_ACTIVITY_TEMPLATE_PATH, parameters);
					manifestTemplate = manifestTemplate.replaceAll(ACTIVITIES, activities);
					monitor.worked(70);
				} else {
					manifestTemplate = manifestTemplate.replaceAll(ACTIVITIES, ""); //$NON-NLS-1$
					monitor.worked(70);
				}

				// Receiver information
				String receivers = readEmbeddedTextFileStudio(WIDGET_RECEIVER_TEMPLATE_PATH, parameters);
				manifestTemplate = manifestTemplate.replaceAll(RECEIVERS, receivers);
				monitor.worked(70);

				// Min Sdk information
				String minSdkVersion = (String) parameters.get(MIN_SDK_VERSION);
				if ((minSdkVersion != null) && (minSdkVersion.length() > 0)) {
					String usesSdk = readEmbeddedTextFileStudio(WIDGET_USES_SDK_TEMPLATE_PATH, parameters);
					manifestTemplate = manifestTemplate.replaceAll(USES_SDK, usesSdk);
					monitor.worked(50);
				} else {
					manifestTemplate = manifestTemplate.replaceAll(USES_SDK, ""); //$NON-NLS-1$
					monitor.worked(50);
				}

				InputStream stream = null;

				try {
					stream = new ByteArrayInputStream(manifestTemplate.getBytes("UTF-8")); //$NON-NLS-1$
					manifestFile.create(stream, IResource.NONE, new SubProgressMonitor(monitor, 100));
				} finally {
					try {
						if (stream != null) {
							stream.close();
						}
					} catch (IOException e) {
						StudioLogger.info("Could not close stream while creating manifest for widget", e.getMessage());
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	private static String readEmbeddedTextFileADT(String template, Map<String, Object> parameters) {
		String loadedTemplate = AdtPlugin.readEmbeddedTextFile(template);

		for (String key : parameters.keySet()) {
			if (parameters.get(key) instanceof String) {
				loadedTemplate = loadedTemplate.replaceAll(key, (String) parameters.get(key));
			}
		}

		return loadedTemplate;
	}

	private static String readEmbeddedTextFileStudio(String template, Map<String, Object> parameters) {
		String loadedTemplate = EclipseUtils.readEmbeddedResource(AndroidPlugin.getDefault().getBundle(), template);

		for (String key : parameters.keySet()) {
			if (parameters.get(key) instanceof String) {
				loadedTemplate = loadedTemplate.replaceAll(key, (String) parameters.get(key));
			}
		}

		return loadedTemplate;
	}

	/**
	 * Setup src folders
	 * 
	 * @param javaProject
	 * @param sourceFolder
	 * @param monitor
	 * @throws JavaModelException
	 */
	private static void setupSourceFolders(IJavaProject javaProject, List<String> sourceFolders,
			IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Preparing_Source_Folders_Task,
				(sourceFolders.size() * 100) + 100);
		try {
			IProject project = javaProject.getProject();
			IClasspathEntry[] entries = javaProject.getRawClasspath();

			for (String sourceFolder : sourceFolders) {
				IFolder srcFolder = project.getFolder(sourceFolder);
				entries = removeClasspathEntry(entries, srcFolder);
				entries = removeClasspathEntry(entries, srcFolder.getParent());
				entries = ProjectUtils.addEntryToClasspath(entries, JavaCore.newSourceEntry(srcFolder.getFullPath()));
				monitor.worked(100);
			}

			javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 100));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Remove source folder from classpath
	 * 
	 * @param entries
	 * @param folder
	 * @return
	 */
	private static IClasspathEntry[] removeClasspathEntry(IClasspathEntry[] entries, IContainer folder) {

		IClasspathEntry[] newClassPath = null;

		if (folder != null) {
			IClasspathEntry removeEntry = JavaCore.newSourceEntry(folder.getFullPath());
			List<IClasspathEntry> entriesList = Arrays.asList(entries);

			if (entriesList.contains(removeEntry)) {
				newClassPath = new IClasspathEntry[entries.length - 1];
				int i = 0;
				for (IClasspathEntry entry : entriesList) {
					if (!entry.equals(removeEntry)) {
						newClassPath[i] = entry;
						i++;
					}
				}
			} else {
				newClassPath = entries;
			}
		} else {
			newClassPath = entries;
		}
		return newClassPath;
	}

	/**
	 * Add default directory to Project
	 * 
	 * @param project
	 * @param parentFolder
	 * @param folderName
	 * @param monitor
	 * @throws CoreException
	 */
	private static void createDefaultDir(IProject project, String parentFolder, String folderName,
			IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(AndroidNLS.UI_ProjectCreationSupport_Creating_Directory_Task + folderName, 100);

		try {
			monitor.setTaskName(AndroidNLS.UI_ProjectCreationSupport_Verifying_Directory_Task);
			if (folderName.length() > 0) {
				monitor.worked(10);
				IFolder folder = project.getFolder(parentFolder + folderName);
				monitor.worked(10);
				if (!folder.exists()) {
					monitor.worked(10);
					if (FileUtil.canWrite(folder.getLocation().toFile())) {
						monitor.worked(10);
						monitor.setTaskName(AndroidNLS.UI_ProjectCreationSupport_Creating_Directory_Task);
						folder.create(true, true, new SubProgressMonitor(monitor, 60));
					} else {
						String errMsg = NLS.bind(
								AndroidNLS.EXC_ProjectCreationSupport_CannotCreateFolderReadOnlyWorkspace, folder
										.getLocation().toFile().toString());
						IStatus status = new AndroidStatus(IStatus.ERROR, errMsg);
						throw new CoreException(status);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Validate new Project Location.
	 * 
	 * @param destination
	 * @param display
	 * @return
	 */
	public static boolean validateNewProjectLocationIsEmpty(IPath destination) {
		File f = new File(destination.toOSString());
		if (f.isDirectory() && (f.list().length > 0)) {
			// EclipseUtils.showErrorDialog(
			// AndroidNLS.UI_ProjectCreationSupport_NonEmptyFolderQuestionDialogTitle,
			// AndroidNLS.UI_ProjectCreationSupport_NonEmptyFolderQuestion);
			return false;
		}
		return true;
	}

	/**
	 * Run the operation in async thread.
	 * 
	 * @param op
	 * @param container
	 * 
	 * @return true if no errors occur during the operation or false otherwise
	 */
	private static boolean runAsyncOperation(WorkspaceModifyOperation op, IWizardContainer container) {
		boolean created = false;

		try {
			container.run(true, true, op);
			created = true;
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getTargetException();
			if (t instanceof CoreException) {
				CoreException core = (CoreException) t;
				if (core.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
					MessageDialog.openError(container.getShell(), AndroidNLS.UI_GenericErrorDialogTitle,
							AndroidNLS.ERR_ProjectCreationSupport_CaseVariantExistsError);
				} else {
					ErrorDialog.openError(container.getShell(), AndroidNLS.UI_GenericErrorDialogTitle, null,
							core.getStatus());
				}
			} else {
				MessageDialog.openError(container.getShell(), AndroidNLS.UI_GenericErrorDialogTitle, t.getMessage());
			}
		} catch (InterruptedException e) {
			StudioLogger.error(ProjectCreationSupport.class, "Error creating project.", e); //$NON-NLS-1$
		}

		return created;
	}

	/**
	 * Checks if a project can be created on workspace
	 * 
	 * @param root
	 *            The workspace root
	 * @param projectName
	 *            The project name
	 * 
	 * @return true if the project can be created or false otherwise
	 */
	private static boolean canCreateProject(IWorkspaceRoot root, String projectName) {
		File rootFolder = root.getLocation().toFile();
		File projectFolder = new File(rootFolder, projectName);

		return FileUtil.canWrite(projectFolder);
	}

	/**
	 * Undoes a project creation. Removes all files created by the project
	 * creation process and keeps any other previous files
	 * 
	 * @param project
	 *            The failed project
	 * @param existingResources
	 *            A set containing the path of pre-existing resources before
	 *            creating the project
	 */
	private static void undoProjectCreation(IProject project) {
		File projectPath = new File(project.getWorkspace().getRoot().getLocation().toFile(), project.getName());
		Set<String> existingResources = getExistingResources(projectPath);

		try {
			project.delete(false, true, new NullProgressMonitor());
		} catch (CoreException e1) {
			// Do nothing
			StudioLogger.error(ProjectCreationSupport.class, e1.getLocalizedMessage(), e1);
		}

		if (existingResources.isEmpty()) {
			try {
				FileUtil.deleteDirRecursively(project.getLocation().toFile());
			} catch (IOException e) {
				// Do nothing
				StudioLogger.error(ProjectCreationSupport.class, e.getLocalizedMessage(), e);
			}
		} else {
			File root = new File(project.getWorkspace().getRoot().getLocation().toFile(), project.getName());
			removeCreatedResources(root, existingResources);
		}
	}

	/**
	 * Retrieves a list of existing sub-resources from a folder
	 * 
	 * @param folder
	 *            the File object representing the folder
	 * 
	 * @return a list of existing sub-resources from the folder
	 */
	private static Set<String> getExistingResources(File folder) {
		Set<String> existing = new HashSet<String>();

		if ((folder != null) && folder.exists() && folder.isDirectory()) {
			existing.add(folder.toString());

			File[] children = folder.listFiles();

			if (children != null) {
				for (File child : children) {
					if (child.isDirectory()) {
						existing.addAll(getExistingResources(child));
					} else {
						existing.add(child.toString());
					}
				}
			}
		}

		return existing;
	}

	/**
	 * Removes the created resources by a failed project creation process
	 * 
	 * @param startingPoint
	 *            The project root folder (File object)
	 * @param existingResources
	 *            The set containing the previous existing resources in the
	 *            project root folder
	 */
	private static void removeCreatedResources(File startingPoint, Set<String> existingResources) {
		File[] members = startingPoint.listFiles();

		if (members != null) {
			for (File child : members) {
				if (child.isFile()) {
					if (!existingResources.contains(child.toString())) {
						child.delete();
					}
				} else {
					removeCreatedResources(child, existingResources);
				}
			}
		}

		if (!existingResources.contains(startingPoint.toString())) {
			startingPoint.delete();
		}
	}
}
