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

package org.eclipse.andmore.internal.launch;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDebugBridgeChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.CanceledException;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.ClientData.DebuggerStatus;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.Log;
import com.android.ddmlib.TimeoutException;
import com.android.ide.common.xml.ManifestData;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.utils.NullLogger;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.internal.actions.AvdManagerAction;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.launch.AndroidLaunchConfiguration.TargetMode;
import org.eclipse.andmore.internal.launch.DelayedLaunchInfo.InstallRetryMode;
import org.eclipse.andmore.internal.launch.DeviceChooserDialog.DeviceChooserResponse;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.project.ApkInstallManager;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controls the launch of Android application either on a device or on the
 * emulator. If an emulator is already running, this class will attempt to reuse
 * it.
 */
public final class AndroidLaunchController implements IDebugBridgeChangeListener,
        IDeviceChangeListener, IClientChangeListener, ILaunchController {

    private static final String FLAG_AVD = "-avd"; //$NON-NLS-1$
    private static final String FLAG_NETDELAY = "-netdelay"; //$NON-NLS-1$
    private static final String FLAG_NETSPEED = "-netspeed"; //$NON-NLS-1$
    private static final String FLAG_WIPE_DATA = "-wipe-data"; //$NON-NLS-1$
    private static final String FLAG_NO_BOOT_ANIM = "-no-boot-anim"; //$NON-NLS-1$

    /**
     * Map to store {@link ILaunchConfiguration} objects that must be launched as simple connection
     * to running application. The integer is the port on which to connect.
     * <b>ALL ACCESS MUST BE INSIDE A <code>synchronized (sListLock)</code> block!</b>
     */
    private static final HashMap<ILaunchConfiguration, Integer> sRunningAppMap =
        new HashMap<ILaunchConfiguration, Integer>();

    private static final Object sListLock = sRunningAppMap;

    /**
     * List of {@link DelayedLaunchInfo} waiting for an emulator to connect.
     * <p>Once an emulator has connected, {@link DelayedLaunchInfo#getDevice()} is set and the
     * DelayedLaunchInfo object is moved to
     * {@link AndroidLaunchController#mWaitingForReadyEmulatorList}.
     * <b>ALL ACCESS MUST BE INSIDE A <code>synchronized (sListLock)</code> block!</b>
     */
    private final ArrayList<DelayedLaunchInfo> mWaitingForEmulatorLaunches =
        new ArrayList<DelayedLaunchInfo>();

    /**
     * List of application waiting to be launched on a device/emulator.<br>
     * <b>ALL ACCESS MUST BE INSIDE A <code>synchronized (sListLock)</code> block!</b>
     * */
    private final ArrayList<DelayedLaunchInfo> mWaitingForReadyEmulatorList =
        new ArrayList<DelayedLaunchInfo>();

    /**
     * Application waiting to show up as waiting for debugger.
     * <b>ALL ACCESS MUST BE INSIDE A <code>synchronized (sListLock)</code> block!</b>
     */
    private final ArrayList<DelayedLaunchInfo> mWaitingForDebuggerApplications =
        new ArrayList<DelayedLaunchInfo>();

    /**
     * List of clients that have appeared as waiting for debugger before their name was available.
     * <b>ALL ACCESS MUST BE INSIDE A <code>synchronized (sListLock)</code> block!</b>
     */
    private final ArrayList<Client> mUnknownClientsWaitingForDebugger = new ArrayList<Client>();

    /** static instance for singleton */
    private static AndroidLaunchController sThis = new AndroidLaunchController();

    /** private constructor to enforce singleton */
    private AndroidLaunchController() {
        AndroidDebugBridge.addDebugBridgeChangeListener(this);
        AndroidDebugBridge.addDeviceChangeListener(this);
        AndroidDebugBridge.addClientChangeListener(this);
    }

    /**
     * Returns the singleton reference.
     */
    public static AndroidLaunchController getInstance() {
        return sThis;
    }


    /**
     * Launches a remote java debugging session on an already running application
     * @param project The project of the application to debug.
     * @param debugPort The port to connect the debugger to.
     */
    public static void debugRunningApp(IProject project, int debugPort) {
        // get an existing or new launch configuration
        ILaunchConfiguration config = AndroidLaunchController.getLaunchConfig(project,
                LaunchConfigDelegate.ANDROID_LAUNCH_TYPE_ID);

        if (config != null) {
            setPortLaunchConfigAssociation(config, debugPort);

            // and launch
            DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
        }
    }

    /**
     * Returns an {@link ILaunchConfiguration} for the specified {@link IProject}.
     * @param project the project
     * @param launchTypeId launch delegate type id
     * @return a new or already existing <code>ILaunchConfiguration</code> or null if there was
     * an error when creating a new one.
     */
    public static ILaunchConfiguration getLaunchConfig(IProject project, String launchTypeId) {
        // get the launch manager
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        // now get the config type for our particular android type.
        ILaunchConfigurationType configType = manager.getLaunchConfigurationType(launchTypeId);

        String name = project.getName();

        // search for an existing launch configuration
        ILaunchConfiguration config = findConfig(manager, configType, name);

        // test if we found one or not
        if (config == null) {
            // Didn't find a matching config, so we make one.
            // It'll be made in the "working copy" object first.
            ILaunchConfigurationWorkingCopy wc = null;

            try {
                // make the working copy object
                wc = configType.newInstance(null,
                        manager.generateLaunchConfigurationName(name));

                // set the project name
                wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);

                // set the launch mode to default.
                wc.setAttribute(LaunchConfigDelegate.ATTR_LAUNCH_ACTION,
                        LaunchConfigDelegate.DEFAULT_LAUNCH_ACTION);

                // set default target mode
                wc.setAttribute(LaunchConfigDelegate.ATTR_TARGET_MODE,
                        LaunchConfigDelegate.DEFAULT_TARGET_MODE.toString());

                // default AVD: None
                wc.setAttribute(LaunchConfigDelegate.ATTR_AVD_NAME, (String) null);

                // set the default network speed
                wc.setAttribute(LaunchConfigDelegate.ATTR_SPEED,
                        LaunchConfigDelegate.DEFAULT_SPEED);

                // and delay
                wc.setAttribute(LaunchConfigDelegate.ATTR_DELAY,
                        LaunchConfigDelegate.DEFAULT_DELAY);

                // default wipe data mode
                wc.setAttribute(LaunchConfigDelegate.ATTR_WIPE_DATA,
                        LaunchConfigDelegate.DEFAULT_WIPE_DATA);

                // default disable boot animation option
                wc.setAttribute(LaunchConfigDelegate.ATTR_NO_BOOT_ANIM,
                        LaunchConfigDelegate.DEFAULT_NO_BOOT_ANIM);

                // set default emulator options
                IPreferenceStore store = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
                String emuOptions = store.getString(AdtPrefs.PREFS_EMU_OPTIONS);
                wc.setAttribute(LaunchConfigDelegate.ATTR_COMMANDLINE, emuOptions);

                // map the config and the project
                wc.setMappedResources(getResourcesToMap(project));

                // save the working copy to get the launch config object which we return.
                return wc.doSave();

            } catch (CoreException e) {
                String msg = String.format(
                        "Failed to create a Launch config for project '%1$s': %2$s",
                        project.getName(), e.getMessage());
                AndmoreAndroidPlugin.printErrorToConsole(project, msg);

                // no launch!
                return null;
            }
        }

        return config;
    }

    /**
     * Returns the list of resources to map to a Launch Configuration.
     * @param project the project associated to the launch configuration.
     */
    public static IResource[] getResourcesToMap(IProject project) {
        ArrayList<IResource> array = new ArrayList<IResource>(2);
        array.add(project);

        IFile manifest = ProjectHelper.getManifest(project);
        if (manifest != null) {
            array.add(manifest);
        }

        return array.toArray(new IResource[array.size()]);
    }

    /**
     * Launches an android app on the device or emulator
     *
     * @param project The project we're launching
     * @param mode the mode in which to launch, one of the mode constants
     *      defined by <code>ILaunchManager</code> - <code>RUN_MODE</code> or
     *      <code>DEBUG_MODE</code>.
     * @param apk the resource to the apk to launch.
     * @param packageName the Android package name of the app
     * @param debugPackageName the Android package name to debug
     * @param debuggable the debuggable value of the app's manifest, or null if not set.
     * @param requiredApiVersionNumber the api version required by the app, or null if none.
     * @param launchAction the action to perform after app sync
     * @param config the launch configuration
     * @param launch the launch object
     */
    public void launch(final IProject project, String mode, IFile apk,
            String packageName, String debugPackageName, Boolean debuggable,
            String requiredApiVersionNumber, final IAndroidLaunchAction launchAction,
            final AndroidLaunchConfiguration config, final AndroidLaunch launch,
            IProgressMonitor monitor) {

        String message = String.format("Performing %1$s", launchAction.getLaunchDescription());
        AndmoreAndroidPlugin.printToConsole(project, message);

        // create the launch info
        final DelayedLaunchInfo launchInfo = new DelayedLaunchInfo(project, packageName,
                debugPackageName, launchAction, apk, debuggable, requiredApiVersionNumber, launch,
                monitor);

        // set the debug mode
        launchInfo.setDebugMode(mode.equals(ILaunchManager.DEBUG_MODE));

        // get the SDK
        Sdk currentSdk = Sdk.getCurrent();
        AvdManager avdManager = currentSdk.getAvdManager();

        // reload the AVDs to make sure we are up to date
        try {
            avdManager.reloadAvds(NullLogger.getLogger());
        } catch (AndroidLocationException e1) {
            // this happens if the AVD Manager failed to find the folder in which the AVDs are
            // stored. This is unlikely to happen, but if it does, we should force to go manual
            // to allow using physical devices.
            config.mTargetMode = TargetMode.MANUAL;
        }

        // get the sdk against which the project is built
        IAndroidTarget projectTarget = currentSdk.getTarget(project);

        // get the min required android version
        ManifestInfo mi = ManifestInfo.get(project);
        final int minApiLevel = mi.getMinSdkVersion();
        final String minApiCodeName = mi.getMinSdkCodeName();
        final AndroidVersion minApiVersion = new AndroidVersion(minApiLevel, minApiCodeName);

        // FIXME: check errors on missing sdk, AVD manager, or project target.

        // device chooser response object.
        final DeviceChooserResponse response = new DeviceChooserResponse();

        /*
         * Launch logic:
         * - Use Last Launched Device/AVD set.
         *       If user requested to use same device for future launches, and the last launched
         *       device/avd is still present, then simply launch on the same device/avd.
         * - Manual Mode
         *       Always display a UI that lets a user see the current running emulators/devices.
         *       The UI must show which devices are compatibles, and allow launching new emulators
         *       with compatible (and not yet running) AVD.
         * - Automatic Way
         *     * Preferred AVD set.
         *           If Preferred AVD is not running: launch it.
         *           Launch the application on the preferred AVD.
         *     * No preferred AVD.
         *           Count the number of compatible emulators/devices.
         *           If != 1, display a UI similar to manual mode.
         *           If == 1, launch the application on this AVD/device.
         * - Launch on multiple devices:
         *     From the currently active devices & emulators, filter out those that cannot run
         *     the app (by api level), and launch on all the others.
         */
        IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
        if (config.mReuseLastUsedDevice) {
            // check to see if the last used device is still online
            IDevice lastUsedDevice = getDeviceIfOnline(config.mLastUsedDevice,
                    devices);
            if (lastUsedDevice != null) {
                response.setDeviceToUse(lastUsedDevice);
                continueLaunch(response, project, launch, launchInfo, config);
                return;
            }
        }

        if (config.mTargetMode == TargetMode.AUTO) {
            // first check if we have a preferred AVD name, and if it actually exists, and is valid
            // (ie able to run the project).
            // We need to check this in case the AVD was recreated with a different target that is
            // not compatible.
            AvdInfo preferredAvd = null;
            if (config.mAvdName != null) {
                preferredAvd = avdManager.getAvd(config.mAvdName, true /*validAvdOnly*/);
            }

            if (preferredAvd != null) {
                IAndroidTarget preferredAvdTarget = currentSdk.getAndroidTargetFor(preferredAvd);
                if (preferredAvdTarget != null
                        && !preferredAvdTarget.getVersion().canRun(minApiVersion)) {
                    preferredAvd = null;

                    AndmoreAndroidPlugin.printErrorToConsole(project, String.format(
                            "Preferred AVD '%1$s' (API Level: %2$d) cannot run application with minApi %3$s. Looking for a compatible AVD...",
                            config.mAvdName,
                            preferredAvdTarget.getVersion().getApiLevel(),
                            minApiVersion));
                }
            }

            if (preferredAvd != null) {
                // We have a preferred avd that can actually run the application.
                // Now see if the AVD is running, and if so use it, otherwise launch it.

                for (IDevice d : devices) {
                    String deviceAvd = d.getAvdName();
                    if (deviceAvd != null && deviceAvd.equals(config.mAvdName)) {
                        response.setDeviceToUse(d);

                        AndmoreAndroidPlugin.printToConsole(project, String.format(
                                "Automatic Target Mode: Preferred AVD '%1$s' is available on emulator '%2$s'",
                                config.mAvdName, d));

                        continueLaunch(response, project, launch, launchInfo, config);
                        return;
                    }
                }

                // at this point we have a valid preferred AVD that is not running.
                // We need to start it.
                response.setAvdToLaunch(preferredAvd);

                AndmoreAndroidPlugin.printToConsole(project, String.format(
                        "Automatic Target Mode: Preferred AVD '%1$s' is not available. Launching new emulator.",
                        config.mAvdName));

                continueLaunch(response, project, launch, launchInfo, config);
                return;
            }

            // no (valid) preferred AVD? look for one.

            // If the API level requested in the manifest is lower than the current project
            // target, when we will iterate devices/avds later ideally we will want to find
            // a device/avd which target is as close to the manifest as possible (instead of
            // a device which target is the same as the project's target) and use it as the
            // new default.

            if (minApiCodeName != null && minApiLevel < projectTarget.getVersion().getApiLevel()) {
                int maxDist = projectTarget.getVersion().getApiLevel() - minApiLevel;
                IAndroidTarget candidate = null;

                for (IAndroidTarget target : currentSdk.getTargets()) {
                    if (target.canRunOn(projectTarget)) {
                        int currDist = target.getVersion().getApiLevel() - minApiLevel;
                        if (currDist >= 0 && currDist < maxDist) {
                            maxDist = currDist;
                            candidate = target;
                            if (maxDist == 0) {
                                // Found a perfect match
                                break;
                            }
                        }
                    }
                }

                if (candidate != null) {
                    // We found a better SDK target candidate, that is closer to the
                    // API level from minSdkVersion than the one currently used by the
                    // project. Below (in the for...devices loop) we'll try to find
                    // a device/AVD for it.
                    projectTarget = candidate;
                }
            }

            HashMap<IDevice, AvdInfo> compatibleRunningAvds = new HashMap<IDevice, AvdInfo>();
            boolean hasDevice = false; // if there's 1+ device running, we may force manual mode,
                                       // as we cannot always detect proper compatibility with
                                       // devices. This is the case if the project target is not
                                       // a standard platform
            for (IDevice d : devices) {
                String deviceAvd = d.getAvdName();
                if (deviceAvd != null) { // physical devices return null.
                    AvdInfo info = avdManager.getAvd(deviceAvd, true /*validAvdOnly*/);
                    if (AvdCompatibility.canRun(info, projectTarget, minApiVersion)
                            == AvdCompatibility.Compatibility.YES) {
                        compatibleRunningAvds.put(d, info);
                    }
                } else {
                    if (projectTarget.isPlatform()) { // means this can run on any device as long
                                                      // as api level is high enough
                        AndroidVersion deviceVersion = Sdk.getDeviceVersion(d);
                        // the deviceVersion may be null if it wasn't yet queried (device just
                        // plugged in or emulator just booting up.
                        if (deviceVersion != null &&
                                deviceVersion.canRun(projectTarget.getVersion())) {
                            // device is compatible with project
                            compatibleRunningAvds.put(d, null);
                            continue;
                        }
                    } else {
                        // for non project platform, we can't be sure if a device can
                        // run an application or not, since we don't query the device
                        // for the list of optional libraries that it supports.
                    }
                    hasDevice = true;
                }
            }

            // depending on the number of devices, we'll simulate an automatic choice
            // from the device chooser or simply show up the device chooser.
            if (hasDevice == false && compatibleRunningAvds.size() == 0) {
                // if zero emulators/devices, we launch an emulator.
                // We need to figure out which AVD first.

                // we are going to take the closest AVD. ie a compatible AVD that has the API level
                // closest to the project target.
                AvdInfo defaultAvd = findMatchingAvd(avdManager, projectTarget, minApiVersion);

                if (defaultAvd != null) {
                    response.setAvdToLaunch(defaultAvd);

                    AndmoreAndroidPlugin.printToConsole(project, String.format(
                            "Automatic Target Mode: launching new emulator with compatible AVD '%1$s'",
                            defaultAvd.getName()));

                    continueLaunch(response, project, launch, launchInfo, config);
                    return;
                } else {
                    AndmoreAndroidPlugin.printToConsole(project, String.format(
                            "Failed to find an AVD compatible with target '%1$s'.",
                            projectTarget.getName()));

                    final Display display = AndmoreAndroidPlugin.getDisplay();
                    final boolean[] searchAgain = new boolean[] { false };
                    // ask the user to create a new one.
                    display.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            Shell shell = display.getActiveShell();
                            if (MessageDialog.openQuestion(shell, "Android AVD Error",
                                    "No compatible targets were found. Do you wish to add a new Android Virtual Device?")) {
                                AvdManagerAction action = new AvdManagerAction();
                                action.run(null /*action*/);
                                searchAgain[0] = true;
                            }
                        }
                    });
                    if (searchAgain[0]) {
                        // attempt to reload the AVDs and find one compatible.
                        defaultAvd = findMatchingAvd(avdManager, projectTarget, minApiVersion);

                        if (defaultAvd == null) {
                            AndmoreAndroidPlugin.printErrorToConsole(project, String.format(
                                    "Still no compatible AVDs with target '%1$s': Aborting launch.",
                                    projectTarget.getName()));
                            stopLaunch(launchInfo);
                        } else {
                            response.setAvdToLaunch(defaultAvd);

                            AndmoreAndroidPlugin.printToConsole(project, String.format(
                                    "Launching new emulator with compatible AVD '%1$s'",
                                    defaultAvd.getName()));

                            continueLaunch(response, project, launch, launchInfo, config);
                            return;
                        }
                    }
                }
            } else if (hasDevice == false && compatibleRunningAvds.size() == 1) {
                Entry<IDevice, AvdInfo> e = compatibleRunningAvds.entrySet().iterator().next();
                response.setDeviceToUse(e.getKey());

                // get the AvdInfo, if null, the device is a physical device.
                AvdInfo avdInfo = e.getValue();
                if (avdInfo != null) {
                    message = String.format("Automatic Target Mode: using existing emulator '%1$s' running compatible AVD '%2$s'",
                            response.getDeviceToUse(), e.getValue().getName());
                } else {
                    message = String.format("Automatic Target Mode: using device '%1$s'",
                            response.getDeviceToUse());
                }
                AndmoreAndroidPlugin.printToConsole(project, message);

                continueLaunch(response, project, launch, launchInfo, config);
                return;
            }

            // if more than one device, we'll bring up the DeviceChooser dialog below.
            if (compatibleRunningAvds.size() >= 2) {
                message = "Automatic Target Mode: Several compatible targets. Please select a target device.";
            } else if (hasDevice) {
                message = "Automatic Target Mode: Unable to detect device compatibility. Please select a target device.";
            }

            AndmoreAndroidPlugin.printToConsole(project, message);
        } else if ((config.mTargetMode == TargetMode.ALL_DEVICES_AND_EMULATORS
                || config.mTargetMode == TargetMode.ALL_DEVICES
                || config.mTargetMode == TargetMode.ALL_EMULATORS)
                && ILaunchManager.RUN_MODE.equals(mode)) {
            // if running on multiple devices, identify all compatible devices
            boolean includeDevices = config.mTargetMode != TargetMode.ALL_EMULATORS;
            boolean includeAvds = config.mTargetMode != TargetMode.ALL_DEVICES;
            Collection<IDevice> compatibleDevices = findCompatibleDevices(devices,
                    minApiVersion, includeDevices, includeAvds);
            if (compatibleDevices.size() == 0) {
                AndmoreAndroidPlugin.printErrorToConsole(project,
                      "No active compatible AVD's or devices found. "
                    + "Relaunch this configuration after connecting a device or starting an AVD.");
                stopLaunch(launchInfo);
            } else {
                multiLaunch(launchInfo, compatibleDevices);
            }
            return;
        }

        // bring up the device chooser.
        final IAndroidTarget desiredProjectTarget = projectTarget;
        final AtomicBoolean continueLaunch = new AtomicBoolean(false);
        AndmoreAndroidPlugin.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    // open the chooser dialog. It'll fill 'response' with the device to use
                    // or the AVD to launch.
                    DeviceChooserDialog dialog = new DeviceChooserDialog(
                            AndmoreAndroidPlugin.getShell(),
                            response, launchInfo.getPackageName(),
                            desiredProjectTarget, minApiVersion,
                            config.mReuseLastUsedDevice);
                    if (dialog.open() == Window.OK) {
                        updateLaunchConfigWithLastUsedDevice(launch.getLaunchConfiguration(),
                                response);
                        continueLaunch.set(true);
                    } else {
                        AndmoreAndroidPlugin.printErrorToConsole(project, "Launch canceled!");
                        stopLaunch(launchInfo);
                        return;
                    }
                } catch (Exception e) {
                    // there seems to be some case where the shell will be null. (might be
                    // an OS X bug). Because of this the creation of the dialog will throw
                    // and IllegalArg exception interrupting the launch with no user feedback.
                    // So we trap all the exception and display something.
                    String msg = e.getMessage();
                    if (msg == null) {
                        msg = e.getClass().getCanonicalName();
                    }
                    AndmoreAndroidPlugin.printErrorToConsole(project,
                            String.format("Error during launch: %s", msg));
                    stopLaunch(launchInfo);
                }
            }
        });

        if (continueLaunch.get()) {
            continueLaunch(response, project, launch, launchInfo, config);
        }
    }

    /**
     * Returns devices that can run a app of provided API level.
     * @param devices list of devices to filter from
     * @param requiredVersion minimum required API that should be supported
     * @param includeDevices include physical devices in the filtered list
     * @param includeAvds include emulators in the filtered list
     * @return set of compatible devices, may be an empty set
     */
    private Collection<IDevice> findCompatibleDevices(IDevice[] devices,
            AndroidVersion requiredVersion, boolean includeDevices, boolean includeAvds) {
        Set<IDevice> compatibleDevices = new HashSet<IDevice>(devices.length);
        Sdk currentSdk = Sdk.getCurrent();
        AvdManager avdManager = currentSdk.getAvdManager();
        for (IDevice d: devices) {
            boolean isEmulator = d.isEmulator();
            boolean canRun = false;

            if (isEmulator) {
                if (!includeAvds) {
                    continue;
                }

                AvdInfo avdInfo = avdManager.getAvd(d.getAvdName(), true);
                IAndroidTarget target = currentSdk.getAndroidTargetFor(avdInfo);
                if (avdInfo != null && target != null) {
                    canRun = target.getVersion().canRun(requiredVersion);
                }
            } else {
                if (!includeDevices) {
                    continue;
                }

                AndroidVersion deviceVersion = Sdk.getDeviceVersion(d);
                if (deviceVersion != null) {
                    canRun = deviceVersion.canRun(requiredVersion);
                }
            }

            if (canRun) {
                compatibleDevices.add(d);
            }
        }

        return compatibleDevices;
    }

    /**
     * Find a matching AVD.
     * @param minApiVersion
     */
    private AvdInfo findMatchingAvd(AvdManager avdManager, final IAndroidTarget projectTarget,
            AndroidVersion minApiVersion) {
        AvdInfo[] avds = avdManager.getValidAvds();
        AvdInfo bestAvd = null;
        Sdk currentSdk = Sdk.getCurrent();
        for (AvdInfo avd : avds) {
            if (AvdCompatibility.canRun(avd, projectTarget, minApiVersion)
                    == AvdCompatibility.Compatibility.YES) {
                // at this point we can ignore the code name issue since
                // AvdCompatibility.canRun() will already have filtered out the non compatible AVDs.
                if (bestAvd == null ||
                        currentSdk.getAndroidTargetFor(avd).getVersion().getApiLevel() <
                        currentSdk.getAndroidTargetFor(bestAvd).getVersion().getApiLevel()) {
                    bestAvd = avd;
                }
            }
        }
        return bestAvd;
    }

    /**
     * Continues the launch based on the DeviceChooser response.
     * @param response the device chooser response
     * @param project The project being launched
     * @param launch The eclipse launch info
     * @param launchInfo The {@link DelayedLaunchInfo}
     * @param config The config needed to start a new emulator.
     */
    private void continueLaunch(final DeviceChooserResponse response, final IProject project,
            final AndroidLaunch launch, final DelayedLaunchInfo launchInfo,
            final AndroidLaunchConfiguration config) {
        if (response.getAvdToLaunch() != null) {
            // there was no selected device, we start a new emulator.
            synchronized (sListLock) {
                AvdInfo info = response.getAvdToLaunch();
                mWaitingForEmulatorLaunches.add(launchInfo);
                AndmoreAndroidPlugin.printToConsole(project, String.format(
                        "Launching a new emulator with Virtual Device '%1$s'",
                        info.getName()));
                boolean status = launchEmulator(config, info);

                if (status == false) {
                    // launching the emulator failed!
                    AndmoreAndroidPlugin.displayError("Emulator Launch",
                            "Couldn't launch the emulator! Make sure the SDK directory is properly setup and the emulator is not missing.");

                    // stop the launch and return
                    mWaitingForEmulatorLaunches.remove(launchInfo);
                    AndmoreAndroidPlugin.printErrorToConsole(project, "Launch canceled!");
                    stopLaunch(launchInfo);
                    return;
                }

                return;
            }
        } else if (response.getDeviceToUse() != null) {
            launchInfo.setDevice(response.getDeviceToUse());
            simpleLaunch(launchInfo, launchInfo.getDevice());
        }
    }

    /**
     * Queries for a debugger port for a specific {@link ILaunchConfiguration}.
     * <p/>
     * If the configuration and a debugger port where added through
     * {@link #setPortLaunchConfigAssociation(ILaunchConfiguration, int)}, then this method
     * will return the debugger port, and remove the configuration from the list.
     * @param launchConfig the {@link ILaunchConfiguration}
     * @return the debugger port or {@link LaunchConfigDelegate#INVALID_DEBUG_PORT} if the
     * configuration was not setup.
     */
    static int getPortForConfig(ILaunchConfiguration launchConfig) {
        synchronized (sListLock) {
            Integer port = sRunningAppMap.get(launchConfig);
            if (port != null) {
                sRunningAppMap.remove(launchConfig);
                return port;
            }
        }

        return LaunchConfigDelegate.INVALID_DEBUG_PORT;
    }

    /**
     * Set a {@link ILaunchConfiguration} and its associated debug port, in the list of
     * launch config to connect directly to a running app instead of doing full launch (sync,
     * launch, and connect to).
     * @param launchConfig the {@link ILaunchConfiguration} object.
     * @param port The debugger port to connect to.
     */
    private static void setPortLaunchConfigAssociation(ILaunchConfiguration launchConfig,
            int port) {
        synchronized (sListLock) {
            sRunningAppMap.put(launchConfig, port);
        }
    }

    /**
     * Checks the build information, and returns whether the launch should continue.
     * <p/>The value tested are:
     * <ul>
     * <li>Minimum API version requested by the application. If the target device does not match,
     * the launch is canceled.</li>
     * <li>Debuggable attribute of the application and whether or not the device requires it. If
     * the device requires it and it is not set in the manifest, the launch will be forced to
     * "release" mode instead of "debug"</li>
     * <ul>
     */
    private boolean checkBuildInfo(DelayedLaunchInfo launchInfo, IDevice device) {
        if (device != null) {
            // check the app required API level versus the target device API level

            String deviceVersion = device.getProperty(IDevice.PROP_BUILD_VERSION);
            String deviceApiLevelString = device.getProperty(IDevice.PROP_BUILD_API_LEVEL);
            String deviceCodeName = device.getProperty(IDevice.PROP_BUILD_CODENAME);

            int deviceApiLevel = -1;
            try {
                deviceApiLevel = Integer.parseInt(deviceApiLevelString);
            } catch (NumberFormatException e) {
                // pass, we'll keep the apiLevel value at -1.
            }

            String requiredApiString = launchInfo.getRequiredApiVersionNumber();
            if (requiredApiString != null) {
                int requiredApi = -1;
                try {
                    requiredApi = Integer.parseInt(requiredApiString);
                } catch (NumberFormatException e) {
                    // pass, we'll keep requiredApi value at -1.
                }

                if (requiredApi == -1) {
                    // this means the manifest uses a codename for minSdkVersion
                    // check that the device is using the same codename
                    if (requiredApiString.equals(deviceCodeName) == false) {
                        AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), String.format(
                            "ERROR: Application requires a device running '%1$s'!",
                            requiredApiString));
                        return false;
                    }
                } else {
                    // app requires a specific API level
                    if (deviceApiLevel == -1) {
                        AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                                "WARNING: Unknown device API version!");
                    } else if (deviceApiLevel < requiredApi) {
                        String msg = String.format(
                                "ERROR: Application requires API version %1$d. Device API version is %2$d (Android %3$s).",
                                requiredApi, deviceApiLevel, deviceVersion);
                        AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg);

                        // abort the launch
                        return false;
                    }
                }
            } else {
                // warn the application API level requirement is not set.
                AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                        "WARNING: Application does not specify an API level requirement!");

                // and display the target device API level (if known)
                if (deviceApiLevel == -1) {
                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                            "WARNING: Unknown device API version!");
                } else {
                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), String.format(
                            "Device API version is %1$d (Android %2$s)", deviceApiLevel,
                            deviceVersion));
                }
            }

            // now checks that the device/app can be debugged (if needed)
            if (device.isEmulator() == false && launchInfo.isDebugMode()) {
                String debuggableDevice = device.getProperty(IDevice.PROP_DEBUGGABLE);
                if (debuggableDevice != null && debuggableDevice.equals("0")) { //$NON-NLS-1$
                    // the device is "secure" and requires apps to declare themselves as debuggable!
                    // launchInfo.getDebuggable() will return null if the manifest doesn't declare
                    // anything. In this case this is fine since the build system does insert
                    // debuggable=true. The only case to look for is if false is manually set
                    // in the manifest.
                    if (launchInfo.getDebuggable() == Boolean.FALSE) {
                        String message = String.format("Application '%1$s' has its 'debuggable' attribute set to FALSE and cannot be debugged.",
                                launchInfo.getPackageName());
                        AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), message);

                        // because am -D does not check for ro.debuggable and the
                        // 'debuggable' attribute, it is important we do not use the -D option
                        // in this case or the app will wait for a debugger forever and never
                        // really launch.
                        launchInfo.setDebugMode(false);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Do a simple launch on the specified device, attempting to sync the new
     * package, and then launching the application. Failed sync/launch will
     * stop the current AndroidLaunch and return false;
     * @param launchInfo
     * @param device
     * @return true if succeed
     */
    private boolean simpleLaunch(DelayedLaunchInfo launchInfo, IDevice device) {
        if (!doPreLaunchActions(launchInfo, device)) {
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), "Launch canceled!");
            stopLaunch(launchInfo);
            return false;
        }

        // launch the app
        launchApp(launchInfo, device);

        return true;
    }

    private boolean doPreLaunchActions(DelayedLaunchInfo launchInfo, IDevice device) {
        // API level check
        if (!checkBuildInfo(launchInfo, device)) {
            return false;
        }

        // sync app
        if (!syncApp(launchInfo, device)) {
            return false;
        }

        return true;
    }

    private void multiLaunch(DelayedLaunchInfo launchInfo, Collection<IDevice> devices) {
        for (IDevice d: devices) {
            boolean success = doPreLaunchActions(launchInfo, d);
            if (!success) {
                String deviceName = d.isEmulator() ? d.getAvdName() : d.getSerialNumber();
                AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                        "Launch failed on device: " + deviceName);
                continue;
            }
        }

        doLaunchAction(launchInfo, devices);

        // multiple launches are only supported for run configuration, so we can terminate
        // the launch itself
        stopLaunch(launchInfo);
    }

    /**
     * If needed, syncs the application and all its dependencies on the device/emulator.
     *
     * @param launchInfo The Launch information object.
     * @param device the device on which to sync the application
     * @return true if the install succeeded.
     */
    private boolean syncApp(DelayedLaunchInfo launchInfo, IDevice device) {
        boolean alreadyInstalled = ApkInstallManager.getInstance().isApplicationInstalled(
                launchInfo.getProject(), launchInfo.getPackageName(), device);

        if (alreadyInstalled) {
            AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
            "Application already deployed. No need to reinstall.");
        } else {
            if (doSyncApp(launchInfo, device) == false) {
                return false;
            }
        }

        // The app is now installed, now try the dependent projects
        for (DelayedLaunchInfo dependentLaunchInfo : getDependenciesLaunchInfo(launchInfo)) {
            String msg = String.format("Project dependency found, installing: %s",
                    dependentLaunchInfo.getProject().getName());
            AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(), msg);
            if (syncApp(dependentLaunchInfo, device) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Syncs the application on the device/emulator.
     *
     * @param launchInfo The Launch information object.
     * @param device the device on which to sync the application
     * @return true if the install succeeded.
     */
    private boolean doSyncApp(DelayedLaunchInfo launchInfo, IDevice device) {
        IPath path = launchInfo.getPackageFile().getLocation();
        String fileName = path.lastSegment();
        try {
            String message = String.format("Uploading %1$s onto device '%2$s'",
                    fileName, device.getSerialNumber());
            AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(), message);

            String remotePackagePath = device.syncPackageToDevice(path.toOSString());
            boolean installResult = installPackage(launchInfo, remotePackagePath, device);
            device.removeRemotePackage(remotePackagePath);

            // if the installation succeeded, we register it.
            if (installResult) {
               ApkInstallManager.getInstance().registerInstallation(
                       launchInfo.getProject(), launchInfo.getPackageName(), device);
            }
            return installResult;
        }
        catch (IOException e) {
            String msg = String.format("Failed to install %1$s on device '%2$s': %3$s", fileName,
                    device.getSerialNumber(), e.getMessage());
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg, e);
        } catch (TimeoutException e) {
            String msg = String.format("Failed to install %1$s on device '%2$s': timeout", fileName,
                    device.getSerialNumber());
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg);
        } catch (AdbCommandRejectedException e) {
            String msg = String.format(
                    "Failed to install %1$s on device '%2$s': adb rejected install command with: %3$s",
                    fileName, device.getSerialNumber(), e.getMessage());
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg, e);
        } catch (CanceledException e) {
            if (e.wasCanceled()) {
                AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                        String.format("Install of %1$s canceled", fileName));
            } else {
                String msg = String.format("Failed to install %1$s on device '%2$s': %3$s",
                        fileName, device.getSerialNumber(), e.getMessage());
                AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg, e);
            }
        }

        return false;
    }

    /**
     * For the current launchInfo, create additional DelayedLaunchInfo that should be used to
     * sync APKs that we are dependent on to the device.
     *
     * @param launchInfo the original launch info that we want to find the
     * @return a list of DelayedLaunchInfo (may be empty if no dependencies were found or error)
     */
    public List<DelayedLaunchInfo> getDependenciesLaunchInfo(DelayedLaunchInfo launchInfo) {
        List<DelayedLaunchInfo> dependencies = new ArrayList<DelayedLaunchInfo>();

        // Convert to equivalent JavaProject
        IJavaProject javaProject;
        try {
            //assuming this is an Android (and Java) project since it is attached to the launchInfo.
            javaProject = BaseProjectHelper.getJavaProject(launchInfo.getProject());
        } catch (CoreException e) {
            // return empty dependencies
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), e);
            return dependencies;
        }

        // Get all projects that this depends on
        List<IJavaProject> androidProjectList;
        try {
            androidProjectList = ProjectHelper.getAndroidProjectDependencies(javaProject);
        } catch (JavaModelException e) {
            // return empty dependencies
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), e);
            return dependencies;
        }

        // for each project, parse manifest and create launch information
        for (IJavaProject androidProject : androidProjectList) {
            // Parse the Manifest to get various required information
            // copied from LaunchConfigDelegate
            ManifestData manifestData = AndroidManifestHelper.parseForData(
                    androidProject.getProject());

            if (manifestData == null) {
                continue;
            }

            // Get the APK location (can return null)
            IFile apk = ProjectHelper.getApplicationPackage(androidProject.getProject());
            if (apk == null) {
                // getApplicationPackage will have logged an error message
                continue;
            }

            // Create new launchInfo as an hybrid between parent and dependency information
            DelayedLaunchInfo delayedLaunchInfo = new DelayedLaunchInfo(
                    androidProject.getProject(),
                    manifestData.getPackage(),
                    manifestData.getPackage(),
                    launchInfo.getLaunchAction(),
                    apk,
                    manifestData.getDebuggable(),
                    manifestData.getMinSdkVersionString(),
                    launchInfo.getLaunch(),
                    launchInfo.getMonitor());

            // Add to the list
            dependencies.add(delayedLaunchInfo);
        }

        return dependencies;
    }

    /**
     * Installs the application package on the device, and handles return result
     * @param launchInfo The launch information
     * @param remotePath The remote path of the package.
     * @param device The device on which the launch is done.
     */
    private boolean installPackage(DelayedLaunchInfo launchInfo, final String remotePath,
            final IDevice device) {
        String message = String.format("Installing %1$s...", launchInfo.getPackageFile().getName());
        AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(), message);
        try {
            // try a reinstall first, because the most common case is the app is already installed
            String result = doInstall(launchInfo, remotePath, device, true /* reinstall */);

            /* For now we force to retry the install (after uninstalling) because there's no
             * other way around it: adb install does not want to update a package w/o uninstalling
             * the old one first!
             */
            return checkInstallResult(result, device, launchInfo, remotePath,
                    InstallRetryMode.ALWAYS);
        } catch (Exception e) {
            String msg = String.format(
                    "Failed to install %1$s on device '%2$s!",
                    launchInfo.getPackageFile().getName(), device.getSerialNumber());
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg, e.getMessage());
        }

        return false;
    }

    /**
     * Checks the result of an installation, and takes optional actions based on it.
     * @param result the result string from the installation
     * @param device the device on which the installation occured.
     * @param launchInfo the {@link DelayedLaunchInfo}
     * @param remotePath the temporary path of the package on the device
     * @param retryMode indicates what to do in case, a package already exists.
     * @return <code>true<code> if success, <code>false</code> otherwise.
     * @throws InstallException
     */
    private boolean checkInstallResult(String result, IDevice device, DelayedLaunchInfo launchInfo,
            String remotePath, InstallRetryMode retryMode) throws InstallException {
        if (result == null) {
            AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(), "Success!");
            return true;
        }
        else if (result.equals("INSTALL_FAILED_ALREADY_EXISTS")) { //$NON-NLS-1$
            // this should never happen, since reinstall mode is used on the first attempt
            if (retryMode == InstallRetryMode.PROMPT) {
                boolean prompt = AndmoreAndroidPlugin.displayPrompt("Application Install",
                        "A previous installation needs to be uninstalled before the new package can be installed.\nDo you want to uninstall?");
                if (prompt) {
                    retryMode = InstallRetryMode.ALWAYS;
                } else {
                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                        "Installation error! The package already exists.");
                    return false;
                }
            }

            if (retryMode == InstallRetryMode.ALWAYS) {
                /*
                 * TODO: create a UI that gives the dev the choice to:
                 * - clean uninstall on launch
                 * - full uninstall if application exists.
                 * - soft uninstall if application exists (keeps the app data around).
                 * - always ask (choice of soft-reinstall, full reinstall)
                AdtPlugin.printErrorToConsole(launchInfo.mProject,
                        "Application already exists, uninstalling...");
                String res = doUninstall(device, launchInfo);
                if (res == null) {
                    AdtPlugin.printToConsole(launchInfo.mProject, "Success!");
                } else {
                    AdtPlugin.printErrorToConsole(launchInfo.mProject,
                            String.format("Failed to uninstall: %1$s", res));
                    return false;
                }
                */

                AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                        "Application already exists. Attempting to re-install instead...");
                String res = doInstall(launchInfo, remotePath, device, true /* reinstall */ );
                return checkInstallResult(res, device, launchInfo, remotePath,
                        InstallRetryMode.NEVER);
            }
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                    "Installation error! The package already exists.");
        } else if (result.equals("INSTALL_FAILED_INVALID_APK")) { //$NON-NLS-1$
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                "Installation failed due to invalid APK file!",
                "Please check logcat output for more details.");
        } else if (result.equals("INSTALL_FAILED_INVALID_URI")) { //$NON-NLS-1$
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                "Installation failed due to invalid URI!",
                "Please check logcat output for more details.");
        } else if (result.equals("INSTALL_FAILED_COULDNT_COPY")) { //$NON-NLS-1$
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                String.format("Installation failed: Could not copy %1$s to its final location!",
                        launchInfo.getPackageFile().getName()),
                "Please check logcat output for more details.");
        } else if (result.equals("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES")) { //$NON-NLS-1$
            if (retryMode != InstallRetryMode.NEVER) {
                boolean prompt = AndmoreAndroidPlugin.displayPrompt("Application Install",
                                "Re-installation failed due to different application signatures. You must perform a full uninstall of the application. WARNING: This will remove the application data!\nDo you want to uninstall?");
                if (prompt) {
                    doUninstall(device, launchInfo);
                    String res = doInstall(launchInfo, remotePath, device, false);
                    return checkInstallResult(res, device, launchInfo, remotePath,
                            InstallRetryMode.NEVER);
                }
            }
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                    "Re-installation failed due to different application signatures.",
                    "You must perform a full uninstall of the application. WARNING: This will remove the application data!",
                    String.format("Please execute 'adb uninstall %1$s' in a shell.", launchInfo.getPackageName()));
        } else {
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                String.format("Installation error: %1$s", result),
                "Please check logcat output for more details.");
        }

        return false;
    }

    /**
     * Performs the uninstallation of an application.
     * @param device the device on which to install the application.
     * @param launchInfo the {@link DelayedLaunchInfo}.
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws InstallException if the installation failed.
     */
    private String doUninstall(IDevice device, DelayedLaunchInfo launchInfo)
            throws InstallException {
        try {
            return device.uninstallPackage(launchInfo.getPackageName());
        } catch (InstallException e) {
            String msg = String.format(
                    "Failed to uninstall %1$s: %2$s", launchInfo.getPackageName(), e.getMessage());
            AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(), msg);
            throw e;
        }
    }

    /**
     * Performs the installation of an application whose package has been uploaded on the device.
     *
     * @param launchInfo the {@link DelayedLaunchInfo}.
     * @param remotePath the path of the application package in the device tmp folder.
     * @param device the device on which to install the application.
     * @param reinstall
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws InstallException if the uninstallation failed.
     */
    private String doInstall(DelayedLaunchInfo launchInfo, final String remotePath,
            final IDevice device, boolean reinstall) throws InstallException {
        try {
            device.installRemotePackage(remotePath, reinstall);
            return null;
        }
        catch (InstallException e) {
            return e.getMessage();
        }
    }

    /**
     * launches an application on a device or emulator
     *
     * @param info the {@link DelayedLaunchInfo} that indicates the launch action
     * @param device the device or emulator to launch the application on
     */
    @Override
    public void launchApp(final DelayedLaunchInfo info, IDevice device) {
        if (info.isDebugMode()) {
            synchronized (sListLock) {
                if (mWaitingForDebuggerApplications.contains(info) == false) {
                    mWaitingForDebuggerApplications.add(info);
                }
            }
        }
        if (doLaunchAction(info, device)) {
            // if the app is not a debug app, we need to do some clean up, as
            // the process is done!
            if (info.isDebugMode() == false) {
                // stop the launch object, since there's no debug, and it can't
                // provide any control over the app
                stopLaunch(info);
            }
        } else {
            // something went wrong or no further launch action needed
            // lets stop the Launch
            stopLaunch(info);
        }
    }

    private boolean doLaunchAction(final DelayedLaunchInfo info, Collection<IDevice> devices) {
        boolean result = info.getLaunchAction().doLaunchAction(info, devices);

        // Monitor the logcat output on the launched device to notify
        // the user if any significant error occurs that is visible from logcat
        for (IDevice d : devices) {
            DdmsPlugin.getDefault().startLogCatMonitor(d);
        }

        return result;
    }

    private boolean doLaunchAction(final DelayedLaunchInfo info, IDevice device) {
        return doLaunchAction(info, Collections.singletonList(device));
    }

    private boolean launchEmulator(AndroidLaunchConfiguration config, AvdInfo avdToLaunch) {

        // split the custom command line in segments
        ArrayList<String> customArgs = new ArrayList<String>();
        boolean hasWipeData = false;
        if (config.mEmulatorCommandLine != null && config.mEmulatorCommandLine.length() > 0) {
            String[] segments = config.mEmulatorCommandLine.split("\\s+"); //$NON-NLS-1$

            // we need to remove the empty strings
            for (String s : segments) {
                if (s.length() > 0) {
                    customArgs.add(s);
                    if (!hasWipeData && s.equals(FLAG_WIPE_DATA)) {
                        hasWipeData = true;
                    }
                }
            }
        }

        boolean needsWipeData = config.mWipeData && !hasWipeData;
        if (needsWipeData) {
            if (!AndmoreAndroidPlugin.displayPrompt("Android Launch", "Are you sure you want to wipe all user data when starting this emulator?")) {
                needsWipeData = false;
            }
        }

        // build the command line based on the available parameters.
        ArrayList<String> list = new ArrayList<String>();

        String path = AndmoreAndroidPlugin.getOsAbsoluteEmulator();

        list.add(path);

        list.add(FLAG_AVD);
        list.add(avdToLaunch.getName());

        if (config.mNetworkSpeed != null) {
            list.add(FLAG_NETSPEED);
            list.add(config.mNetworkSpeed);
        }

        if (config.mNetworkDelay != null) {
            list.add(FLAG_NETDELAY);
            list.add(config.mNetworkDelay);
        }

        if (needsWipeData) {
            list.add(FLAG_WIPE_DATA);
        }

        if (config.mNoBootAnim) {
            list.add(FLAG_NO_BOOT_ANIM);
        }

        list.addAll(customArgs);

        // convert the list into an array for the call to exec.
        String[] command = list.toArray(new String[list.size()]);

        // launch the emulator
        try {
            Process process = Runtime.getRuntime().exec(command);
            grabEmulatorOutput(process);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Looks for and returns an existing {@link ILaunchConfiguration} object for a
     * specified project.
     * @param manager The {@link ILaunchManager}.
     * @param type The {@link ILaunchConfigurationType}.
     * @param projectName The name of the project
     * @return an existing <code>ILaunchConfiguration</code> object matching the project, or
     *      <code>null</code>.
     */
    private static ILaunchConfiguration findConfig(ILaunchManager manager,
            ILaunchConfigurationType type, String projectName) {
        try {
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);

            for (ILaunchConfiguration config : configs) {
                if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        "").equals(projectName)) {  //$NON-NLS-1$
                    return config;
                }
            }
        } catch (CoreException e) {
            MessageDialog.openError(AndmoreAndroidPlugin.getShell(),
                    "Launch Error", e.getStatus().getMessage());
        }

        // didn't find anything that matches. Return null
        return null;
    }


    /**
     * Connects a remote debugger on the specified port.
     * @param debugPort The port to connect the debugger to
     * @param launch The associated AndroidLaunch object.
     * @param monitor A Progress monitor
     * @return false if cancelled by the monitor
     * @throws CoreException
     */
    @SuppressWarnings("deprecation")
    public static boolean connectRemoteDebugger(int debugPort,
            AndroidLaunch launch, IProgressMonitor monitor)
                throws CoreException {
        // get some default parameters.
        int connectTimeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);

        HashMap<String, String> newMap = new HashMap<String, String>();

        newMap.put("hostname", "localhost");  //$NON-NLS-1$ //$NON-NLS-2$

        newMap.put("port", Integer.toString(debugPort)); //$NON-NLS-1$

        newMap.put("timeout", Integer.toString(connectTimeout));

        // get the default VM connector
        IVMConnector connector = JavaRuntime.getDefaultVMConnector();

        // connect to remote VM
        connector.connect(newMap, monitor, launch);

        // check for cancellation
        if (monitor.isCanceled()) {
            IDebugTarget[] debugTargets = launch.getDebugTargets();
            for (IDebugTarget target : debugTargets) {
                if (target.canDisconnect()) {
                    target.disconnect();
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Launch a new thread that connects a remote debugger on the specified port.
     * @param debugPort The port to connect the debugger to
     * @param androidLaunch The associated AndroidLaunch object.
     * @param monitor A Progress monitor
     * @see #connectRemoteDebugger(int, AndroidLaunch, IProgressMonitor)
     */
    public static void launchRemoteDebugger(final int debugPort, final AndroidLaunch androidLaunch,
            final IProgressMonitor monitor) {
        new Thread("Debugger connection") { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    connectRemoteDebugger(debugPort, androidLaunch, monitor);
                } catch (CoreException e) {
                    androidLaunch.stopLaunch();
                }
                monitor.done();
            }
        }.start();
    }

    /**
     * Sent when a new {@link AndroidDebugBridge} is started.
     * <p/>
     * This is sent from a non UI thread.
     * @param bridge the new {@link AndroidDebugBridge} object.
     *
     * @see IDebugBridgeChangeListener#bridgeChanged(AndroidDebugBridge)
     */
    @Override
    public void bridgeChanged(AndroidDebugBridge bridge) {
        // The adb server has changed. We cancel any pending launches.
        String message = "adb server change: cancelling '%1$s'!";
        synchronized (sListLock) {
            for (DelayedLaunchInfo launchInfo : mWaitingForReadyEmulatorList) {
                AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                    String.format(message, launchInfo.getLaunchAction().getLaunchDescription()));
                stopLaunch(launchInfo);
            }
            for (DelayedLaunchInfo launchInfo : mWaitingForDebuggerApplications) {
                AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                        String.format(message,
                                launchInfo.getLaunchAction().getLaunchDescription()));
                stopLaunch(launchInfo);
            }

            mWaitingForReadyEmulatorList.clear();
            mWaitingForDebuggerApplications.clear();
        }
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceConnected(IDevice)
     */
    @Override
    public void deviceConnected(IDevice device) {
        synchronized (sListLock) {
            // look if there's an app waiting for a device
            if (mWaitingForEmulatorLaunches.size() > 0) {
                // get/remove first launch item from the list
                // FIXME: what if we have multiple launches waiting?
                DelayedLaunchInfo launchInfo = mWaitingForEmulatorLaunches.get(0);
                mWaitingForEmulatorLaunches.remove(0);

                // give the launch item its device for later use.
                launchInfo.setDevice(device);

                // and move it to the other list
                mWaitingForReadyEmulatorList.add(launchInfo);

                // and tell the user about it
                AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                        String.format("New emulator found: %1$s", device.getSerialNumber()));
                AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                        String.format("Waiting for HOME ('%1$s') to be launched...",
                            AndmoreAndroidPlugin.getDefault().getPreferenceStore().getString(
                                    AdtPrefs.PREFS_HOME_PACKAGE)));
            }
        }
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceDisconnected(IDevice)
     */
    @Override
    public void deviceDisconnected(IDevice device) {
        // any pending launch on this device must be canceled.
        String message = "%1$s disconnected! Cancelling '%2$s'!";
        synchronized (sListLock) {
            ArrayList<DelayedLaunchInfo> copyList =
                (ArrayList<DelayedLaunchInfo>) mWaitingForReadyEmulatorList.clone();
            for (DelayedLaunchInfo launchInfo : copyList) {
                if (launchInfo.getDevice() == device) {
                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                            String.format(message, device.getSerialNumber(),
                                    launchInfo.getLaunchAction().getLaunchDescription()));
                    stopLaunch(launchInfo);
                }
            }
            copyList = (ArrayList<DelayedLaunchInfo>) mWaitingForDebuggerApplications.clone();
            for (DelayedLaunchInfo launchInfo : copyList) {
                if (launchInfo.getDevice() == device) {
                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                            String.format(message, device.getSerialNumber(),
                                    launchInfo.getLaunchAction().getLaunchDescription()));
                    stopLaunch(launchInfo);
                }
            }
        }
    }

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask indicating what changed.
     *
     * @see IDeviceChangeListener#deviceChanged(IDevice, int)
     */
    @Override
    public void deviceChanged(IDevice device, int changeMask) {
        // We could check if any starting device we care about is now ready, but we can wait for
        // its home app to show up, so...
    }

    /**
     * Sent when an existing client information changed.
     * <p/>
     * This is sent from a non UI thread.
     * @param client the updated client.
     * @param changeMask the bit mask describing the changed properties. It can contain
     * any of the following values: {@link Client#CHANGE_INFO}, {@link Client#CHANGE_NAME}
     * {@link Client#CHANGE_DEBUGGER_STATUS}, {@link Client#CHANGE_THREAD_MODE},
     * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
     * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
     *
     * @see IClientChangeListener#clientChanged(Client, int)
     */
    @Override
    public void clientChanged(final Client client, int changeMask) {
        boolean connectDebugger = false;
        if ((changeMask & Client.CHANGE_NAME) == Client.CHANGE_NAME) {
            String applicationName = client.getClientData().getClientDescription();
            if (applicationName != null) {
                IPreferenceStore store = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
                String home = store.getString(AdtPrefs.PREFS_HOME_PACKAGE);

                if (home.equals(applicationName)) {

                    // looks like home is up, get its device
                    IDevice device = client.getDevice();

                    // look for application waiting for home
                    synchronized (sListLock) {
                        for (int i = 0; i < mWaitingForReadyEmulatorList.size(); ) {
                            DelayedLaunchInfo launchInfo = mWaitingForReadyEmulatorList.get(i);
                            if (launchInfo.getDevice() == device) {
                                // it's match, remove from the list
                                mWaitingForReadyEmulatorList.remove(i);

                                // We couldn't check earlier the API level of the device
                                // (it's asynchronous when the device boot, and usually
                                // deviceConnected is called before it's queried for its build info)
                                // so we check now
                                if (checkBuildInfo(launchInfo, device) == false) {
                                    // device is not the proper API!
                                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                                            "Launch canceled!");
                                    stopLaunch(launchInfo);
                                    return;
                                }

                                AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(),
                                        String.format("HOME is up on device '%1$s'",
                                                device.getSerialNumber()));

                                // attempt to sync the new package onto the device.
                                if (syncApp(launchInfo, device)) {
                                    // application package is sync'ed, lets attempt to launch it.
                                    launchApp(launchInfo, device);
                                } else {
                                    // failure! Cancel and return
                                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                                    "Launch canceled!");
                                    stopLaunch(launchInfo);
                                }

                                break;
                            } else {
                                i++;
                            }
                        }
                    }
                }

                // check if it's already waiting for a debugger, and if so we connect to it.
                if (client.getClientData().getDebuggerConnectionStatus() == DebuggerStatus.WAITING) {
                    // search for this client in the list;
                    synchronized (sListLock) {
                        int index = mUnknownClientsWaitingForDebugger.indexOf(client);
                        if (index != -1) {
                            connectDebugger = true;
                            mUnknownClientsWaitingForDebugger.remove(client);
                        }
                    }
                }
            }
        }

        // if it's not home, it could be an app that is now in debugger mode that we're waiting for
        // lets check it

        if ((changeMask & Client.CHANGE_DEBUGGER_STATUS) == Client.CHANGE_DEBUGGER_STATUS) {
            ClientData clientData = client.getClientData();
            String applicationName = client.getClientData().getClientDescription();
            if (clientData.getDebuggerConnectionStatus() == DebuggerStatus.WAITING) {
                // Get the application name, and make sure its valid.
                if (applicationName == null) {
                    // looks like we don't have the client yet, so we keep it around for when its
                    // name becomes available.
                    synchronized (sListLock) {
                        mUnknownClientsWaitingForDebugger.add(client);
                    }
                    return;
                } else {
                    connectDebugger = true;
                }
            }
        }

        if (connectDebugger) {
            Log.d("adt", "Debugging " + client);
            // now check it against the apps waiting for a debugger
            String applicationName = client.getClientData().getClientDescription();
            Log.d("adt", "App Name: " + applicationName);
            synchronized (sListLock) {
                for (int i = 0; i < mWaitingForDebuggerApplications.size(); ) {
                    final DelayedLaunchInfo launchInfo = mWaitingForDebuggerApplications.get(i);
                    if (client.getDevice() == launchInfo.getDevice() &&
                            applicationName.equals(launchInfo.getDebugPackageName())) {
                        // this is a match. We remove the launch info from the list
                        mWaitingForDebuggerApplications.remove(i);

                        // and connect the debugger.
                        String msg = String.format(
                                "Attempting to connect debugger to '%1$s' on port %2$d",
                                launchInfo.getDebugPackageName(), client.getDebuggerListenPort());
                        AndmoreAndroidPlugin.printToConsole(launchInfo.getProject(), msg);

                        new Thread("Debugger Connection") { //$NON-NLS-1$
                            @Override
                            public void run() {
                                try {
                                    if (connectRemoteDebugger(
                                            client.getDebuggerListenPort(),
                                            launchInfo.getLaunch(),
                                            launchInfo.getMonitor()) == false) {
                                        return;
                                    }
                                } catch (CoreException e) {
                                    // well something went wrong.
                                    AndmoreAndroidPlugin.printErrorToConsole(launchInfo.getProject(),
                                            String.format("Launch error: %s", e.getMessage()));
                                    // stop the launch
                                    stopLaunch(launchInfo);
                                }

                                launchInfo.getMonitor().done();
                            }
                        }.start();

                        // we're done processing this client.
                        return;

                    } else {
                        i++;
                    }
                }
            }

            // if we get here, we haven't found an app that we were launching, so we look
            // for opened android projects that contains the app asking for a debugger.
            // If we find one, we automatically connect to it.
            IProject project = ProjectHelper.findAndroidProjectByAppName(applicationName);

            if (project != null) {
                debugRunningApp(project, client.getDebuggerListenPort());
            }
        }
    }

    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the output from
     */
    private void grabEmulatorOutput(final Process process) {
        // read the lines as they come. if null is returned, it's
        // because the process finished
        new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            AndmoreAndroidPlugin.printErrorToConsole("Emulator", line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }.start();

        new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            AndmoreAndroidPlugin.printToConsole("Emulator", line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }.start();
    }

    @Override
    public void stopLaunch(DelayedLaunchInfo launchInfo) {
        launchInfo.getLaunch().stopLaunch();
        synchronized (sListLock) {
            mWaitingForReadyEmulatorList.remove(launchInfo);
            mWaitingForDebuggerApplications.remove(launchInfo);
        }
    }

    public static void updateLaunchConfigWithLastUsedDevice(
            ILaunchConfiguration launchConfiguration, DeviceChooserResponse response) {
        try {
            boolean configModified = false;
            boolean reuse = launchConfiguration.getAttribute(
                    LaunchConfigDelegate.ATTR_REUSE_LAST_USED_DEVICE, false);
            String serial = launchConfiguration.getAttribute(
                    LaunchConfigDelegate.ATTR_LAST_USED_DEVICE, (String)null);

            ILaunchConfigurationWorkingCopy wc = launchConfiguration.getWorkingCopy();
            if (reuse != response.useDeviceForFutureLaunches()) {
                reuse = response.useDeviceForFutureLaunches();
                wc.setAttribute(LaunchConfigDelegate.ATTR_REUSE_LAST_USED_DEVICE, reuse);
                configModified = true;
            }

            if (reuse) {
                String selected = getSerial(response);
                if (selected != null && !selected.equalsIgnoreCase(serial)) {
                    wc.setAttribute(LaunchConfigDelegate.ATTR_LAST_USED_DEVICE, selected);
                    configModified = true;
                }
            }

            if (configModified) {
                wc.doSave();
            }
        } catch (CoreException e) {
            // in such a case, users just won't see this setting take effect
            return;
        }
    }

    private static String getSerial(DeviceChooserResponse response) {
        AvdInfo avd = response.getAvdToLaunch();
        return (avd != null) ? avd.getName() : response.getDeviceToUse().getSerialNumber();
    }

    @Nullable
    public static IDevice getDeviceIfOnline(@Nullable String serial,
            @NonNull IDevice[] onlineDevices) {
        if (serial == null) {
            return null;
        }

        for (IDevice device : onlineDevices) {
            if (serial.equals(device.getAvdName()) ||
                    serial.equals(device.getSerialNumber())) {
                return device;
            }
        }

        return null;
    }
}
