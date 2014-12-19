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
import java.util.List;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

public class LaunchConfigurationShortcut implements ILaunchShortcut {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers
	 * .ISelection, java.lang.String)
	 */
	@Override
	public void launch(ISelection selection, String mode) {
		ILaunchConfiguration launchConfiguration = getLaunchConfigurationForSelection(selection, true);
		handleLaunch(mode, launchConfiguration);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart,
	 * java.lang.String)
	 */
	@Override
	public void launch(IEditorPart editor, String mode) {
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {
			ILaunchConfiguration launchConfiguration = getLaunchConfigurationForResource(resource, true);
			handleLaunch(mode, launchConfiguration);
		}
	}

	private void handleLaunch(String mode, ILaunchConfiguration launchConfiguration) {
		if (launchConfiguration != null) {
			final ILaunchConfiguration config = launchConfiguration;
			final String launchMode = mode;

			Job job = new Job("Launch Job") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					IStatus status = Status.OK_STATUS;
					try {
						config.launch(launchMode, monitor);
					} catch (CoreException e) {
						status = new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID,
								LaunchNLS.ERR_LaunchConfigurationShortcut_CannotLaunchSelectedResourceMsg, e);
					}
					return status;
				}
			};

			job.schedule();
		} else {
			LaunchUtils.showErrorDialog(LaunchNLS.ERR_LaunchConfigurationShortcut_MsgTitle,
					LaunchNLS.ERR_LaunchConfigurationShortcut_CannotLaunchSelectedResourceMsg);
		}
	}

	/**
	 * Gets a launch configuration for a desired selection
	 * 
	 * @param selection
	 *            The selection
	 * @param create
	 *            If the launch configuration does not exist, does it must be
	 *            created?
	 * 
	 * @return The launch configuration for the selection
	 */

	private ILaunchConfiguration getLaunchConfigurationForSelection(ISelection selection, boolean create) {
		ILaunchConfiguration config = null;
		IStructuredSelection newSelection;
		Object selectedObject;
		IResource selectedResource = null;

		if (selection instanceof IStructuredSelection) {
			newSelection = (IStructuredSelection) selection;
			selectedObject = newSelection.getFirstElement();

			if (selectedObject instanceof IResource) {
				selectedResource = (IResource) selectedObject;
			} else if (selectedObject instanceof IJavaElement) {
				selectedResource = ((IJavaElement) selectedObject).getResource();
			}

			if (selectedResource != null) {
				config = getLaunchConfigurationForResource(selectedResource, create);
			}
		}

		return config;
	}

	/**
	 * Gets a launch configuration for a resource
	 * 
	 * @param resource
	 *            The resource
	 * @param create
	 *            If the launch configuration does not exist, does it must be
	 *            created?
	 * 
	 * @return The launch configuration for the resource
	 */
	private ILaunchConfiguration getLaunchConfigurationForResource(IResource resource, boolean create) {
		IResource app;
		IResource project;
		ILaunchConfiguration config = null;

		if (resource != null) {
			if (resource.getType() == IResource.PROJECT) {
				project = resource;
			} else {
				project = resource.getProject();
			}
			// Try to retrieve an existent launch configuration
			config = findLaunchConfiguration(project);

			if ((config == null) && create) {
				// No launch configuration could be found. Try to create a
				// launch configuration with the first runnable activity
				app = getFirstActivity((IProject) project);

				// If no application could be found, use the project
				// to create the launch configuration
				app = app == null ? resource : app;
				config = createLaunchConfiguration(app);
			}

		}

		return config;
	}

	/**
	 * Finds a launch configuration for a descriptor, a mpkg file or a project
	 * 
	 * @param resource
	 *            A descriptor, a mpkg file or a project
	 * 
	 * @return A launch configuration or null if it could not be found
	 */
	private ILaunchConfiguration findLaunchConfiguration(IResource resource) {
		ILaunchConfiguration launchConfig = null;

		if (resource != null) {
			try {
				List<ILaunchConfiguration> projectLC = getProjectLaunchConfigurations(resource.getProject());

				if ((resource.getType() == IResource.PROJECT) || (resource.getType() == IResource.FILE)) {
					// If the resource is a project, return the first launch
					// configuration found
					// for the project
					if (!projectLC.isEmpty()) {
						launchConfig = projectLC.iterator().next();
					}
				}
			} catch (CoreException e) {
				StudioLogger.error(LaunchConfigurationShortcut.class,
						"Error searching for launch configuration for resource: " + resource.getName(), e);
			}
		}

		return launchConfig;
	}

	/**
	 * Scan for all LaunchConfigurations associated with a project.
	 * 
	 * @param selectedResource
	 *            , the project itself or any file within the project to be
	 *            scanned
	 * @return List with all LaunchConfiguration associated with a project or an
	 *         empty List if none is found.
	 * @throws CoreException
	 */
	protected List<ILaunchConfiguration> getProjectLaunchConfigurations(IProject project) throws CoreException {
		List<ILaunchConfiguration> matches;

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType motodevLaunchType = launchManager
				.getLaunchConfigurationType(ILaunchConfigurationConstants.LAUNCH_CONFIGURATION_TYPE_EXTENSION_ID);

		ILaunchConfiguration[] motodevLaunchConfigurations = launchManager.getLaunchConfigurations(motodevLaunchType);
		matches = new ArrayList<ILaunchConfiguration>(motodevLaunchConfigurations.length);
		for (ILaunchConfiguration launchConfiguration : motodevLaunchConfigurations) {
			if (launchConfiguration
					.getAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(project.getName())) //$NON-NLS-1$
			{
				matches.add(launchConfiguration);
			}
		}

		return matches;
	}

	/**
	 * Gets the first runnable application/widget for a project. It can be a
	 * application/widget root folder or a mpkg file
	 * 
	 * @param project
	 *            The project
	 * 
	 * @return The first runnable application/widget or null if it does not
	 *         exist
	 */
	private IResource getFirstActivity(IProject project) {
		IResource app = null;

		String[] allActivities = LaunchUtils.getProjectActivities(project);

		if ((allActivities != null) && (allActivities.length >= 1)) {
			app = project.getFile(allActivities[0]);
		}

		return app;
	}

	/**
	 * Creates a launch configuration based on a resource
	 * 
	 * @param resource
	 *            The resource
	 * 
	 * @return A launch configuration
	 */
	private ILaunchConfiguration createLaunchConfiguration(IResource resource) {
		ILaunchConfiguration config = null;
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType motodevLaunchType = launchManager
				.getLaunchConfigurationType(ILaunchConfigurationConstants.LAUNCH_CONFIGURATION_TYPE_EXTENSION_ID);
		String projectName;

		String configBaseName = resource.getName();

		String launchConfigurationName = launchManager.generateLaunchConfigurationName(configBaseName);
		try {
			ILaunchConfigurationWorkingCopy workingCopy = motodevLaunchType.newInstance(null, launchConfigurationName);

			// Set Defaults
			workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
					ILaunchConfigurationConstants.DEFAULT_VALUE);
			workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY,
					ILaunchConfigurationConstants.DEFAULT_VALUE);
			// It is default not to exist Preferred AVD attribute, so we just
			// set the Studio's
			// device instance name attribute here
			workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, (String) null);
			LaunchUtils.setADTLaunchConfigurationDefaults(workingCopy);

			// Launch Settings
			IProject project = resource.getProject();
			projectName = project.getName();
			workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);

			if (resource.getType() != IResource.PROJECT) {
				workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY, resource.getName());
			}

			String deviceName = getSelectedInstanceName(project);
			workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, deviceName);
			// Preferred AVD name shall only exist in the launch configuration
			// if an AVD is selected
			Collection<String> validAvds = SdkUtils.getAllValidVmNames();
			if (validAvds.contains(deviceName)) {
				workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_ADT_DEVICE_INSTANCE_NAME, deviceName);
			}

			if (workingCopy.getAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY,
					ILaunchConfigurationConstants.DEFAULT_VALUE).equals("")) {
				workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
						ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT);
			}

			config = workingCopy.doSave();
		} catch (CoreException e) {
			StudioLogger.error(LaunchConfigurationShortcut.class, "Error creating launch configuration for resource: "
					+ resource.getName(), e);
		}

		return config;
	}

	/**
	 * Get a available and compatible instance name. This method seeks within
	 * all registered instances, following the criteria: Phone device with
	 * "full" compatibility (API version = project min. API) Phone device with
	 * "partial" compatibility (API version > project min. API) Emulator device
	 * with "full" compatibility (API version = project min. API) Emulator
	 * device with "partial" compatibility (API version = project min. API)
	 * 
	 * @param project
	 */
	protected String getSelectedInstanceName(IProject project) {
		String selectedDevice = "";

		// get all instances according ddms
		Collection<ISerialNumbered> instances = DevicesManager.getInstance().getAllDevicesSorted();
		String candidate = "";
		for (ISerialNumbered instance : instances) {

			IStatus compatible = LaunchUtils.isCompatible(project, instance);
			if (compatible.isOK()) {
				selectedDevice = instance.getDeviceName();
				break;
			} else if (compatible.getSeverity() == IStatus.WARNING) {
				candidate = instance.getDeviceName();
			}

		}
		if ((selectedDevice.equals("")) && !candidate.equals("")) {
			selectedDevice = candidate;
		}

		return selectedDevice;
	}

}
