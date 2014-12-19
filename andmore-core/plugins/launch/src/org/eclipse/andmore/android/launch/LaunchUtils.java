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

package org.eclipse.andmore.android.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.android.ide.common.xml.AndroidManifestParser;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;

/**
 * DESCRIPTION: Utilities for Studio for Android Launch use
 * 
 * RESPONSIBILITY: Provide common utility methods that can be used by any Studio
 * for Android Launch plugin.
 * 
 * COLABORATORS: None
 * 
 * USAGE: This class should not be instantiated and its methods should be called
 * statically.
 */
@SuppressWarnings("restriction")
public class LaunchUtils {
	/**
	 * Retrieve a instance by name
	 * 
	 * @param instanceName
	 * @return IInstance with the given name or null if none is found, or it's
	 *         not available.
	 */
	public static String getSerialNumberForInstance(String instanceName) {
		String serial = null;
		List<IInstance> list = InstanceRegistry.getInstance().getInstances();
		for (IInstance inst : list) {
			if ((inst.getName().equals(instanceName)) && (inst instanceof ISerialNumbered)) {
				serial = ((ISerialNumbered) inst).getSerialNumber();
			}
		}
		return serial;
	}

	/**
	 * Get a project in the current workspace based on its projectName
	 * 
	 * @param projectName
	 * @return the IProject representing the project, or null if none is found
	 */
	public static IProject getProject(String projectName) {
		IProject project = null;

		Path projectPath = new Path(projectName);
		if (projectPath.isValidSegment(projectName)) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}

		return project;
	}

	/**
	 * Verify if a given project is supported by the Studio for Android
	 * Launcher, checking if the project is a Android project
	 * 
	 * @param project
	 *            to be verified
	 * @return true if project is a an Android project, false otherwise.
	 */
	public static boolean isProjectSupported(IProject project) {
		boolean hasNature = false;
		boolean isLibrary = true;

		if ((project != null) && project.isOpen()) {
			try {
				hasNature = project.hasNature(AndroidPlugin.Android_Nature);
				isLibrary = SdkUtils.isLibraryProject(project);
			} catch (CoreException e) {
				// Do nothing
			}
		}

		return hasNature && !isLibrary;
	}

	/**
	 * Get all Android Projects within the current workspace.
	 * 
	 * @return IProject array with all Android projects in the current
	 *         workspace, or an empty array if none is found
	 */
	public static IProject[] getSupportedProjects() {
		Collection<IProject> projectCollection = new ArrayList<IProject>();
		IProject[] projectsName = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		/* select only Android projects */
		for (IProject project : projectsName) {
			if (project.isAccessible()) {
				if (LaunchUtils.isProjectSupported(project)) {
					projectCollection.add(project);
				}
			}
		}

		return projectCollection.toArray(new IProject[projectCollection.size()]);
	}

	/**
	 * Retrieve the project activities from the MANIFEST.xml file
	 * 
	 * @param project
	 * @return An array of activities.
	 */
	public static String[] getProjectActivities(IProject project) {

		String[] activities = null;
		Activity[] adtActivities = null;

		// parse the manifest for the list of activities.
		try {
			ManifestData manifestParser = AndroidManifestParser.parse(new IFolderWrapper(project));

			if (manifestParser != null) {
				adtActivities = manifestParser.getActivities();
			}

			if ((adtActivities != null) && (adtActivities.length > 0)) {
				activities = new String[adtActivities.length];
				for (int i = 0; i < adtActivities.length; i++) {
					activities[i] = adtActivities[i].getName();
				}
			}

		} catch (Exception e) {
			StudioLogger.error(LaunchUtils.class, "An error occurred trying to parse AndroidManifest", e);
		}

		return activities;

	}

	/**
	 * Set the default launch configuration values
	 */
	public static void setADTLaunchConfigurationDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_ALLOW_TERMINATE,
				ILaunchConfigurationConstants.ATTR_ALLOW_TERMINATE_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
				ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_TARGET_MODE,
				ILaunchConfigurationConstants.ATTR_TARGET_MODE_DEFAULT.toString());
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_SPEED,
				ILaunchConfigurationConstants.ATTR_SPEED_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_DELAY,
				ILaunchConfigurationConstants.ATTR_DELAY_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_WIPE_DATA,
				ILaunchConfigurationConstants.ATTR_WIPE_DATA_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_NO_BOOT_ANIM,
				ILaunchConfigurationConstants.ATTR_NO_BOOT_ANIM_DEFAULT);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_COMMANDLINE,
				ILaunchConfigurationConstants.DEFAULT_VALUE);

	}

	/**
	 * Update the launch configuration values
	 */
	public static void updateLaunchConfigurationDefaults(ILaunchConfigurationWorkingCopy configuration) {
		try {
			String deviceName = configuration.getAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, "");

			if ((deviceName != null) && !deviceName.equals("")) {
				IAndroidEmulatorInstance deviceInstance = DeviceFrameworkManager.getInstance().getInstanceByName(
						deviceName);

				if (deviceInstance instanceof IAndroidLogicInstance) {
					String commandLine = ((IAndroidLogicInstance) deviceInstance).getCommandLineArguments();
					configuration.setAttribute(ILaunchConfigurationConstants.ATTR_COMMANDLINE, commandLine);
				}
			}
		} catch (CoreException e) {
			StudioLogger.error(LaunchUtils.class,
					"Error updating launch configuration values for : " + configuration.getName(), e);
		}
	}

	/**
	 * Get the shell of the active workbench or null if there is no active
	 * workbench.
	 * 
	 * @return the active workbench shell
	 */
	public static Shell getActiveWorkbenchShell() {
		class ActiveShellRunnable implements Runnable {
			private Shell shell = null;

			public Shell getShell() {
				return shell;
			}

			@Override
			public void run() {
				IWorkbenchWindow activeWorkbench = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				if (activeWorkbench != null) {
					shell = activeWorkbench.getShell();
				}
			}
		}
		;

		ActiveShellRunnable runnable = new ActiveShellRunnable();
		PlatformUI.getWorkbench().getDisplay().syncExec(runnable);

		return runnable.getShell();
	}

	/**
	 * Show the error message using the given title and message
	 * 
	 * @param title
	 *            of the error dialog
	 * @param message
	 *            to be displayed in the error dialog.
	 */
	public static void showErrorDialog(final String title, final String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				MessageDialog.openError(ww.getShell(), title, message);
			}
		});
	}

	public static ISerialNumbered resolveInstance(List<IInstance> instances) {
		ISerialNumbered theInstance = null;
		Iterator<IInstance> it = instances.iterator();
		while (it.hasNext() && (theInstance == null)) {
			IInstance anInstance = it.next();
			if (anInstance instanceof ISerialNumbered) {
				theInstance = (ISerialNumbered) anInstance;
			}
		}
		return theInstance;
	}

	/**
	 * Check if an instanceName is compatible with some project
	 * 
	 * @param project
	 * @param instanceName
	 * @return {@link IStatus#OK} if fully compatible, {@link IStatus#WARNING}
	 *         if can be compatible and {@link IStatus#ERROR} if not compatible.
	 *         Return <code>null</code> if the instance does not exists
	 */

	public static IStatus isCompatible(IProject project, String instanceName) {
		IStatus status = null;
		List<IInstance> instances = InstanceRegistry.getInstance().getInstances();
		for (IInstance instance : instances) {
			if (instanceName.equals(instance.getName())) {
				if (instance instanceof ISerialNumbered) {
					status = isCompatible(project, (ISerialNumbered) instance);
					break;
				}
			}
		}
		return status;
	}

	/**
	 * Check if the given instance name is compatible with the given project
	 * 
	 * @param project
	 * @param instance
	 * @return {@link IStatus#OK} if fully compatible, {@link IStatus#WARNING}
	 *         if can be compatible and {@link IStatus#ERROR} if not compatible.
	 *         Return <code>null</code> if the instance does not exists
	 */
	public static IStatus isCompatible(IProject project, ISerialNumbered instance) {
		IStatus compatible = null;
		int projectAPILevel = SdkUtils.getApiVersionNumberForProject(project);
		String minSdkVersionStr = SdkUtils.getMinSdkVersion(project);
		int minSdkVersion;
		boolean isProjectTargetAPlatform = SdkUtils.getTarget(project) != null ? SdkUtils.getTarget(project)
				.isPlatform() : true;

		try {
			minSdkVersion = Integer.parseInt(minSdkVersionStr);
		} catch (Exception e) {
			// the projectAPILevel will be used and minSdkVersion will be
			// ignored
			minSdkVersion = projectAPILevel;
		}
		String projectTarget = SdkUtils.getTargetNameForProject(project);

		// if the instance is an emulator add the instance only if they have the
		// same target and at least the same APILevel
		if (instance instanceof IAndroidEmulatorInstance) {
			IAndroidEmulatorInstance emulatorInstance = (IAndroidEmulatorInstance) instance;
			int emulatorApi = emulatorInstance.getAPILevel();
			String emulatorTarget = emulatorInstance.getTarget();

			if (emulatorApi >= minSdkVersion) {
				String emulatorInstanceName = emulatorInstance.getName();
				String emulatorInstanceBaseTarget = SdkUtils.getBaseTarget(emulatorInstanceName);
				boolean isEmulatorTargetAPlatform = SdkUtils.isPlatformTarget(emulatorInstanceName);

				// if they have same target its ok
				if (emulatorTarget.equals(projectTarget)) {
					compatible = Status.OK_STATUS;
				}
				// if the emulator isn't a platform, but the base target is the
				// same as the project, everything is ok
				else if (!isEmulatorTargetAPlatform && emulatorInstanceBaseTarget.equals(projectTarget)) {
					compatible = Status.OK_STATUS;
				} else {
					compatible = new Status(IStatus.WARNING, LaunchPlugin.PLUGIN_ID, NLS.bind(
							LaunchNLS.UI_LaunchConfigurationTab_WARN_DEVICE_INCOMPATIBLE, emulatorApi, projectAPILevel));
				}
			} else {
				compatible = new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, NLS.bind(
						LaunchNLS.UI_LaunchConfigurationTab_ERR_EMULATOR_INCOMPATIBLE, emulatorTarget, projectTarget));
			}
		} else {
			if (instance != null) {
				int deviceSdkVersion = -1;
				int tries = 0;

				// wait the device to be online
				while ((tries < 5) && (deviceSdkVersion <= 0)) {
					try {
						deviceSdkVersion = Integer.parseInt(DDMSFacade.getDeviceProperty(instance.getSerialNumber(),
								"ro.build.version.sdk"));
					} catch (NumberFormatException e) {
						deviceSdkVersion = 0;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							// do nothing
						}
					}
					tries++;
				}

				if (deviceSdkVersion < minSdkVersion) {
					compatible = new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, NLS.bind(
							LaunchNLS.UI_LaunchConfigurationTab_ERR_DEVICE_INCOMPATIBLE, deviceSdkVersion,
							projectAPILevel));
				} else if (deviceSdkVersion == projectAPILevel) {
					if (!isProjectTargetAPlatform) {
						compatible = new Status(IStatus.WARNING, LaunchPlugin.PLUGIN_ID,
								LaunchNLS.UI_LaunchConfigurationTab_WARN_DEVICE_TARGET_MISSING);
					} else {
						compatible = Status.OK_STATUS;
					}
				} else {
					compatible = new Status(IStatus.WARNING, LaunchPlugin.PLUGIN_ID, NLS.bind(
							LaunchNLS.UI_LaunchConfigurationTab_WARN_DEVICE_INCOMPATIBLE, deviceSdkVersion,
							projectAPILevel));
				}
			}

		}
		return compatible;
	}

	/**
	 * Filter instances the compatible with the given project
	 * 
	 * @param project
	 *            whose compatible instances need to be retrieved
	 * @return a new collection containing only the instances that are
	 *         compatible with the given project
	 **/
	public static Collection<ISerialNumbered> filterInstancesByProject(Collection<ISerialNumbered> allInstances,
			IProject project) {
		Collection<ISerialNumbered> filteredInstances = new LinkedList<ISerialNumbered>();

		for (ISerialNumbered instance : allInstances) {
			IStatus compatible = LaunchUtils.isCompatible(project, instance);

			if (compatible.getSeverity() != IStatus.ERROR) {
				filteredInstances.add(instance);
			}
		}

		return filteredInstances;
	}

}