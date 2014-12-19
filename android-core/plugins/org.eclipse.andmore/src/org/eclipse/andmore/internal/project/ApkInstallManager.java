/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDebugBridgeChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;

import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IProjectListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Registers which apk was installed on which device.
 * <p/>
 * The goal of this class is to remember the installation of APKs on devices, and provide
 * information about whether a new APK should be installed on a device prior to running the
 * application from a launch configuration.
 * <p/>
 * The manager uses {@link IProject} and {@link IDevice} to identify the target device and the
 * (project generating the) APK. This ensures that disconnected and reconnected devices will
 * always receive new APKs (since the version may not match).
 * <p/>
 * This is a singleton. To get the instance, use {@link #getInstance()}
 */
public final class ApkInstallManager {

    private final static ApkInstallManager sThis = new ApkInstallManager();

    /**
     * Internal struct to associate a project and a device.
     */
    private final static class ApkInstall {
        public ApkInstall(IProject project, String packageName, IDevice device) {
            this.project = project;
            this.packageName = packageName;
            this.device = device;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ApkInstall) {
                ApkInstall apkObj = (ApkInstall)obj;

                return (device == apkObj.device && project.equals(apkObj.project) &&
                        packageName.equals(apkObj.packageName));
            }

            return false;
        }

        @Override
        public int hashCode() {
            return (device.getSerialNumber() + project.getName() + packageName).hashCode();
        }

        final IProject project;
        final String packageName;
        final IDevice device;
    }

    /**
     * Receiver and parser for the "pm path package" command.
     */
    private final static class PmReceiver extends MultiLineReceiver {
        boolean foundPackage = false;
        @Override
        public void processNewLines(String[] lines) {
            // if the package if found, then pm will show a line starting with "package:/"
            if (foundPackage == false) { // just in case this is called several times for multilines
                for (String line : lines) {
                    if (line.startsWith("package:/")) {
                        foundPackage = true;
                        break;
                    }
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    /**
     * Hashset of the list of installed package. Hashset used to ensure we don't re-add new
     * objects for the same app.
     */
    private final HashSet<ApkInstall> mInstallList = new HashSet<ApkInstall>();

    public static ApkInstallManager getInstance() {
        return sThis;
    }

    /**
     * Registers an installation of <var>project</var> onto <var>device</var>
     * @param project The project that was installed.
     * @param packageName the package name of the apk
     * @param device The device that received the installation.
     */
    public void registerInstallation(IProject project, String packageName, IDevice device) {
        synchronized (mInstallList) {
            mInstallList.add(new ApkInstall(project, packageName, device));
        }
    }

    /**
     * Returns whether a <var>project</var> was installed on the <var>device</var>.
     * @param project the project that may have been installed.
     * @param device the device that may have received the installation.
     * @return
     */
    public boolean isApplicationInstalled(IProject project, String packageName, IDevice device) {
        synchronized (mInstallList) {
            ApkInstall found = null;
            for (ApkInstall install : mInstallList) {
                if (project.equals(install.project) && packageName.equals(install.packageName) &&
                        device == install.device) {
                    found = install;
                    break;
                }
            }

            // check the app is still installed.
            if (found != null) {
                try {
                    PmReceiver receiver = new PmReceiver();
                    found.device.executeShellCommand("pm path " + packageName, receiver);
                    if (receiver.foundPackage == false) {
                        mInstallList.remove(found);
                    }

                    return receiver.foundPackage;
                } catch (Exception e) {
                    // failed to query pm? force reinstall.
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Resets registered installations for a specific {@link IProject}.
     * <p/>This ensures that {@link #isApplicationInstalled(IProject, IDevice)} will always return
     * <code>null</code> for this specified project, for any device.
     * @param project the project for which to reset all installations.
     */
    public void resetInstallationFor(IProject project) {
        synchronized (mInstallList) {
            Iterator<ApkInstall> iterator = mInstallList.iterator();
            while (iterator.hasNext()) {
                ApkInstall install = iterator.next();
                if (install.project.equals(project)) {
                    iterator.remove();
                }
            }
        }
    }

    private ApkInstallManager() {
        AndroidDebugBridge.addDeviceChangeListener(mDeviceChangeListener);
        AndroidDebugBridge.addDebugBridgeChangeListener(mDebugBridgeListener);
        GlobalProjectMonitor.getMonitor().addProjectListener(mProjectListener);
    }

    private IDebugBridgeChangeListener mDebugBridgeListener = new IDebugBridgeChangeListener() {
        /**
         * Responds to a bridge change by clearing the full installation list.
         *
         * @see IDebugBridgeChangeListener#bridgeChanged(AndroidDebugBridge)
         */
        @Override
        public void bridgeChanged(AndroidDebugBridge bridge) {
            // the bridge changed, there is no way to know which IDevice will be which.
            // We reset everything
            synchronized (mInstallList) {
                mInstallList.clear();
            }
        }
    };

    private IDeviceChangeListener mDeviceChangeListener = new IDeviceChangeListener() {
        /**
         * Responds to a device being disconnected by removing all installations related
         * to this device.
         *
         * @see IDeviceChangeListener#deviceDisconnected(IDevice)
         */
        @Override
        public void deviceDisconnected(IDevice device) {
            synchronized (mInstallList) {
                Iterator<ApkInstall> iterator = mInstallList.iterator();
                while (iterator.hasNext()) {
                    ApkInstall install = iterator.next();
                    if (install.device == device) {
                        iterator.remove();
                    }
                }
            }
        }

        @Override
        public void deviceChanged(IDevice device, int changeMask) {
            // nothing to do.
        }

        @Override
        public void deviceConnected(IDevice device) {
            // nothing to do.
        }
    };

    private IProjectListener mProjectListener = new IProjectListener() {
        /**
         * Responds to a closed project by resetting all its installation.
         *
         * @see IProjectListener#projectClosed(IProject)
         */
        @Override
        public void projectClosed(IProject project) {
            resetInstallationFor(project);
        }

        /**
         * Responds to a deleted project by resetting all its installation.
         *
         * @see IProjectListener#projectDeleted(IProject)
         */
        @Override
        public void projectDeleted(IProject project) {
            resetInstallationFor(project);
        }

        @Override
        public void projectOpened(IProject project) {
            // nothing to do.
        }

        @Override
        public void projectOpenedWithWorkspace(IProject project) {
            // nothing to do.
        }

        @Override
        public void allProjectsOpenedWithWorkspace() {
            // nothing to do.
        }

        @Override
        public void projectRenamed(IProject project, IPath from) {
            // project renaming also triggers delete/open events so
            // there's nothing to do here (since delete will remove
            // whatever's linked to the project from the list).
        }
    };
}
