/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.ndk.internal.launch;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceUnixSocketNamespace;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.google.common.base.Joiner;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.launch.AndroidLaunchController;
import org.eclipse.andmore.internal.launch.DeviceChooserDialog;
import org.eclipse.andmore.internal.launch.LaunchConfigDelegate;
import org.eclipse.andmore.internal.launch.DeviceChooserDialog.DeviceChooserResponse;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.ndk.internal.NativeAbi;
import org.eclipse.andmore.ndk.internal.NdkHelper;
import org.eclipse.andmore.ndk.internal.NdkVariables;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("restriction")
public class NdkGdbLaunchDelegate extends GdbLaunchDelegate {
	public static final String LAUNCH_TYPE_ID = "org.eclipse.andmore.ndk.debug.LaunchConfigType"; //$NON-NLS-1$

	private static final Joiner JOINER = Joiner.on(", ").skipNulls();

	private static final String DEBUG_SOCKET = "debugsock"; //$NON-NLS-1$

	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		boolean launched = doLaunch(config, mode, launch, monitor);
		if (!launched) {
			if (launch.canTerminate()) {
				launch.terminate();
			}
			DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
		}
	}

	public boolean doLaunch(final ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		IProject project = null;
		ICProject cProject = CDebugUtils.getCProject(config);
		if (cProject != null) {
			project = cProject.getProject();
		}

		if (project == null) {
			AdtPlugin.printErrorToConsole(Messages.NdkGdbLaunchDelegate_LaunchError_CouldNotGetProject);
			return false;
		}

		// make sure the project and its dependencies are built and
		// PostCompilerBuilder runs.
		// This is a synchronous call which returns when the build is done.
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_PerformIncrementalBuild);
		ProjectHelper.doFullIncrementalDebugBuild(project, monitor);

		// check if the project has errors, and abort in this case.
		if (ProjectHelper.hasError(project, true)) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_ProjectHasErrors);
			return false;
		}

		final ManifestData manifestData = AndroidManifestHelper.parseForData(project);
		final ManifestInfo manifestInfo = ManifestInfo.get(project);
		final AndroidVersion minSdkVersion = new AndroidVersion(manifestInfo.getMinSdkVersion(),
				manifestInfo.getMinSdkCodeName());

		// Get the activity name to launch
		String activityName = getActivityToLaunch(getActivityNameInLaunchConfig(config),
				manifestData.getLauncherActivity(), manifestData.getActivities(), project);

		// Get ABI's supported by the application
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_ObtainAppAbis);
		Collection<NativeAbi> appAbis = NdkHelper.getApplicationAbis(project, monitor);
		if (appAbis.size() == 0) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_UnableToDetectAppAbi);
			return false;
		}

		// Obtain device to use:
		// - if there is only 1 device, just use that
		// - if we have previously launched this config, and the device used is
		// present, use that
		// - otherwise show the DeviceChooserDialog
		final String configName = config.getName();
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_ObtainDevice);
		IDevice device = null;
		IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
		if (devices.length == 1) {
			device = devices[0];
		} else if ((device = getLastUsedDevice(config, devices)) == null) {
			final IAndroidTarget projectTarget = Sdk.getCurrent().getTarget(project);
			final DeviceChooserResponse response = new DeviceChooserResponse();
			final boolean continueLaunch[] = new boolean[] { false };
			AdtPlugin.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					DeviceChooserDialog dialog = new DeviceChooserDialog(AdtPlugin.getDisplay().getActiveShell(),
							response, manifestData.getPackage(), projectTarget, minSdkVersion, false /***
					 * 
					 * FIXME!
					 **/
					);
					if (dialog.open() == Window.OK) {
						AndroidLaunchController.updateLaunchConfigWithLastUsedDevice(config, response);
						continueLaunch[0] = true;
					}
				};
			});

			if (!continueLaunch[0]) {
				return false;
			}

			device = response.getDeviceToUse();
		}

		// ndk-gdb requires device > Froyo
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_CheckAndroidDeviceVersion);
		AndroidVersion deviceVersion = Sdk.getDeviceVersion(device);
		if (deviceVersion == null) {
			AdtPlugin.printErrorToConsole(project,
					Messages.NdkGdbLaunchDelegate_LaunchError_UnknownAndroidDeviceVersion);
			return false;
		} else if (!deviceVersion.isGreaterOrEqualThan(8)) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_Api8Needed);
			return false;
		}

		// get Device ABI
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_ObtainDeviceABI);
		String deviceAbi1 = device.getProperty("ro.product.cpu.abi"); //$NON-NLS-1$
		String deviceAbi2 = device.getProperty("ro.product.cpu.abi2"); //$NON-NLS-1$

		// get the abi that is supported by both the device and the application
		NativeAbi compatAbi = getCompatibleAbi(deviceAbi1, deviceAbi2, appAbis);
		if (compatAbi == null) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_NoCompatibleAbi);
			AdtPlugin.printErrorToConsole(project,
					String.format("ABI's supported by the application: %s", JOINER.join(appAbis)));
			AdtPlugin.printErrorToConsole(project, String.format("ABI's supported by the device: %s, %s", //$NON-NLS-1$
					deviceAbi1, deviceAbi2));
			return false;
		}

		// sync app
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_SyncAppToDevice);
		IFile apk = ProjectHelper.getApplicationPackage(project);
		if (apk == null) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_NullApk);
			return false;
		}
		try {
			device.installPackage(apk.getLocation().toOSString(), true);
		} catch (InstallException e1) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_InstallError, e1);
			return false;
		}

		// launch activity
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_ActivityLaunch + activityName);
		String command = String.format("am start -n %s/%s", manifestData.getPackage(), //$NON-NLS-1$
				activityName);
		try {
			CountDownLatch launchedLatch = new CountDownLatch(1);
			CollectingOutputReceiver receiver = new CollectingOutputReceiver(launchedLatch);
			device.executeShellCommand(command, receiver);
			launchedLatch.await(5, TimeUnit.SECONDS);
			String shellOutput = receiver.getOutput();
			if (shellOutput.contains("Error type")) { //$NON-NLS-1$
				throw new RuntimeException(receiver.getOutput());
			}
		} catch (Exception e) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_ActivityLaunchError, e);
			return false;
		}

		// kill existing gdbserver
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_KillExistingGdbServer);
		for (Client c : device.getClients()) {
			String description = c.getClientData().getClientDescription();
			if (description != null && description.contains("gdbserver")) { //$NON-NLS-1$
				c.kill();
			}
		}

		// pull app_process & libc from the device
		IPath solibFolder = project.getLocation().append("obj/local").append(compatAbi.getAbi());
		try {
			pull(device, "/system/bin/app_process", solibFolder); //$NON-NLS-1$
			pull(device, "/system/lib/libc.so", solibFolder); //$NON-NLS-1$
		} catch (Exception e) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_PullFileError, e);
			return false;
		}

		// wait for a couple of seconds for activity to be launched
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_WaitingForActivity);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// uninterrupted
		}

		// get pid of activity
		Client app = device.getClient(manifestData.getPackage());
		int pid = app.getClientData().getPid();

		// launch gdbserver
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_LaunchingGdbServer);
		CountDownLatch attachLatch = new CountDownLatch(1);
		GdbServerTask gdbServer = new GdbServerTask(device, manifestData.getPackage(), DEBUG_SOCKET, pid, attachLatch);
		new Thread(gdbServer, String.format("gdbserver for %s", manifestData.getPackage())).start(); //$NON-NLS-1$

		// wait for gdbserver to attach
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_WaitGdbServerAttach);
		boolean attached = false;
		try {
			attached = attachLatch.await(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			AdtPlugin.printErrorToConsole(project,
					Messages.NdkGdbLaunchDelegate_LaunchError_InterruptedWaitingForGdbserver);
			return false;
		}

		// if gdbserver failed to attach, we report any errors that may have
		// occurred
		if (!attached) {
			if (gdbServer.getLaunchException() != null) {
				AdtPlugin.printErrorToConsole(project,
						Messages.NdkGdbLaunchDelegate_LaunchError_gdbserverLaunchException,
						gdbServer.getLaunchException());
			} else {
				AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_gdbserverOutput,
						gdbServer.getShellOutput());
			}
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_VerifyIfDebugBuild);

			// shut down the gdbserver thread
			gdbServer.setCancelled();
			return false;
		}

		// Obtain application working directory
		String appDir = null;
		try {
			appDir = getAppDirectory(device, manifestData.getPackage(), 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_ObtainingAppFolder, e);
			return false;
		}

		// setup port forwarding between local port & remote (device) unix
		// domain socket
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_SettingUpPortForward);
		String localport = config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT,
				NdkLaunchConstants.DEFAULT_GDB_PORT);
		try {
			device.createForward(Integer.parseInt(localport), String.format("%s/%s", appDir, DEBUG_SOCKET), //$NON-NLS-1$
					DeviceUnixSocketNamespace.FILESYSTEM);
		} catch (Exception e) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_PortForwarding, e);
			return false;
		}

		// update launch attributes based on device
		ILaunchConfiguration config2 = performVariableSubstitutions(config, project, compatAbi, monitor);

		// launch gdb
		monitor.setTaskName(Messages.NdkGdbLaunchDelegate_Action_LaunchHostGdb);
		super.launch(config2, mode, launch, monitor);
		return true;
	}

	@Nullable
	private IDevice getLastUsedDevice(ILaunchConfiguration config, @NonNull IDevice[] devices) {
		try {
			boolean reuse = config.getAttribute(LaunchConfigDelegate.ATTR_REUSE_LAST_USED_DEVICE, false);
			if (!reuse) {
				return null;
			}

			String serial = config.getAttribute(LaunchConfigDelegate.ATTR_LAST_USED_DEVICE, (String) null);
			return AndroidLaunchController.getDeviceIfOnline(serial, devices);
		} catch (CoreException e) {
			return null;
		}
	}

	private void pull(IDevice device, String remote, IPath solibFolder) throws SyncException, IOException,
			AdbCommandRejectedException, TimeoutException {
		String remoteFileName = new Path(remote).toFile().getName();
		String targetFile = solibFolder.append(remoteFileName).toString();
		device.pullFile(remote, targetFile);
	}

	private ILaunchConfiguration performVariableSubstitutions(ILaunchConfiguration config, IProject project,
			NativeAbi compatAbi, IProgressMonitor monitor) throws CoreException {
		ILaunchConfigurationWorkingCopy wcopy = config.getWorkingCopy();

		String toolchainPrefix = NdkHelper.getToolchainPrefix(project, compatAbi, monitor);
		String gdb = toolchainPrefix + "gdb"; //$NON-NLS-1$

		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		IValueVariable ndkGdb = manager.newValueVariable(NdkVariables.NDK_GDB, NdkVariables.NDK_GDB, true, gdb);
		IValueVariable ndkProject = manager.newValueVariable(NdkVariables.NDK_PROJECT, NdkVariables.NDK_PROJECT, true,
				project.getLocation().toOSString());
		IValueVariable ndkCompatAbi = manager.newValueVariable(NdkVariables.NDK_COMPAT_ABI,
				NdkVariables.NDK_COMPAT_ABI, true, compatAbi.getAbi());

		IValueVariable[] ndkVars = new IValueVariable[] { ndkGdb, ndkProject, ndkCompatAbi };
		manager.addVariables(ndkVars);

		// fix path to gdb
		String userGdbPath = wcopy.getAttribute(NdkLaunchConstants.ATTR_NDK_GDB, NdkLaunchConstants.DEFAULT_GDB);
		wcopy.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, elaborateExpression(manager, userGdbPath));

		// setup program name
		wcopy.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME,
				elaborateExpression(manager, NdkLaunchConstants.DEFAULT_PROGRAM));

		// fix solib paths
		List<String> solibPaths = wcopy.getAttribute(NdkLaunchConstants.ATTR_NDK_SOLIB,
				Collections.singletonList(NdkLaunchConstants.DEFAULT_SOLIB_PATH));
		List<String> fixedSolibPaths = new ArrayList<String>(solibPaths.size());
		for (String u : solibPaths) {
			fixedSolibPaths.add(elaborateExpression(manager, u));
		}
		wcopy.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_SOLIB_PATH, fixedSolibPaths);

		manager.removeVariables(ndkVars);

		return wcopy.doSave();
	}

	private String elaborateExpression(IStringVariableManager manager, String expr) throws CoreException {
		boolean DEBUG = true;

		String eval = manager.performStringSubstitution(expr);
		if (DEBUG) {
			AdtPlugin.printToConsole("Substitute: ", expr, " --> ", eval);
		}

		return eval;
	}

	/**
	 * Returns the activity name to launch. If the user has requested a
	 * particular activity to be launched, then this method will confirm that
	 * the requested activity is defined in the manifest. If the user has not
	 * specified any activities, then it returns the default launcher activity.
	 * 
	 * @param activityNameInLaunchConfig
	 *            activity to launch as requested by the user.
	 * @param activities
	 *            list of activities as defined in the application's manifest
	 * @param project
	 *            android project
	 * @return activity name that should be launched, or null if no launchable
	 *         activity.
	 */
	private String getActivityToLaunch(String activityNameInLaunchConfig, Activity launcherActivity,
			Activity[] activities, IProject project) {
		if (activities.length == 0) {
			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_NoActivityInManifest);
			return null;
		} else if (activityNameInLaunchConfig == null && launcherActivity != null) {
			return launcherActivity.getName();
		} else {
			for (Activity a : activities) {
				if (a != null && a.getName().equals(activityNameInLaunchConfig)) {
					return activityNameInLaunchConfig;
				}
			}

			AdtPlugin.printErrorToConsole(project, Messages.NdkGdbLaunchDelegate_LaunchError_NoSuchActivity);
			if (launcherActivity != null) {
				return launcherActivity.getName();
			} else {
				AdtPlugin.printErrorToConsole(Messages.NdkGdbLaunchDelegate_LaunchError_NoLauncherActivity);
				return null;
			}
		}
	}

	private NativeAbi getCompatibleAbi(String deviceAbi1, String deviceAbi2, Collection<NativeAbi> appAbis) {
		for (NativeAbi abi : appAbis) {
			if (abi.getAbi().equals(deviceAbi1) || abi.getAbi().equals(deviceAbi2)) {
				return abi;
			}
		}

		return null;
	}

	/** Returns the name of the activity as defined in the launch configuration. */
	private String getActivityNameInLaunchConfig(ILaunchConfiguration configuration) {
		String empty = ""; //$NON-NLS-1$
		String activityName;
		try {
			activityName = configuration.getAttribute(LaunchConfigDelegate.ATTR_ACTIVITY, empty);
		} catch (CoreException e) {
			return null;
		}

		return (activityName != empty) ? activityName : null;
	}

	private String getAppDirectory(IDevice device, String app, long timeout, TimeUnit timeoutUnit)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException,
			InterruptedException {
		String command = String.format("run-as %s /system/bin/sh -c pwd", app); //$NON-NLS-1$

		CountDownLatch commandCompleteLatch = new CountDownLatch(1);
		CollectingOutputReceiver receiver = new CollectingOutputReceiver(commandCompleteLatch);
		device.executeShellCommand(command, receiver);
		commandCompleteLatch.await(timeout, timeoutUnit);
		return receiver.getOutput().trim();
	}
}
