/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ide.eclipse.monitor;

import com.android.ide.eclipse.monitor.SdkToolsLocator.SdkInstallStatus;
import com.android.prefs.AndroidLocation;
import com.android.sdklib.SdkManager;
import com.android.sdkstats.SdkStatsService;
import com.android.sdkuilib.internal.repository.ui.AdtUpdateDialog;
import com.android.utils.ILogger;
import com.android.utils.NullLogger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import java.io.File;

public class MonitorApplication implements IApplication {
    private static final String SDK_PATH_ENVVAR = "com.android.sdk.path";
    private static final String MONITOR_WORKSPACE_PATH = "monitor-workspace";

    @Override
    public Object start(IApplicationContext context) throws Exception {
        Display display = PlatformUI.createDisplay();

        // set workspace location
        Location instanceLoc = Platform.getInstanceLocation();
        IPath workspacePath = new Path(AndroidLocation.getFolder()).append(MONITOR_WORKSPACE_PATH);
        instanceLoc.set(workspacePath.toFile().toURI().toURL(), true);

        // figure out path to SDK
        String sdkPath = findSdkPath(display);
        if (!isValidSdkLocation(sdkPath)) {
            // exit with return code -1
            return Integer.valueOf(-1);
        }
        MonitorPlugin.getDefault().setSdkFolder(new File(sdkPath));

        // install platform tools if necessary
        ILogger sdkLog = NullLogger.getLogger();
        SdkManager manager = SdkManager.createManager(sdkPath, sdkLog);
        if (manager.getPlatformToolsVersion() == null) {
            boolean install = MessageDialog.openQuestion(new Shell(display),
                    "Monitor",
                    "The platform tools package that provides adb is missing from your SDK installation. "
                    + "Monitor requires this package to work properly. Would you like to install that package now?");
            if (!install) {
                return Integer.valueOf(-1);
            }
            AdtUpdateDialog window = new AdtUpdateDialog(new Shell(display), sdkLog, sdkPath);
            window.installPlatformTools();
        }

        // If this is the first time using ddms or adt, open up the stats service
        // opt out dialog, and request user for permissions.
        // Note that the actual ping is performed in MonitorStartup
        SdkStatsService stats = new SdkStatsService();
        stats.checkUserPermissionForPing(new Shell(display));

        // open up RCP
        try {
            int returnCode = PlatformUI.createAndRunWorkbench(display,
                    new MonitorWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } finally {
            display.dispose();
        }
    }

    @Override
    public void stop() {
        if (!PlatformUI.isWorkbenchRunning())
            return;
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                if (!display.isDisposed())
                    workbench.close();
            }
        });
    }

    private String findSdkPath(Display display) {
        // see if there is a system property set (passed in via a command line arg)
        String sdkLocation = System.getProperty(SDK_PATH_ENVVAR);
        if (isValidSdkLocation(sdkLocation)) {
            return sdkLocation;
        }

        // see if there is an environment variable set
        sdkLocation = System.getenv(SDK_PATH_ENVVAR);
        if (isValidSdkLocation(sdkLocation)) {
            return sdkLocation;
        }

        // The monitor app should be located in "<sdk>/tools/lib/monitor-platform/"
        // So see if the folder one level up from the install location is a valid SDK.
        Location install = Platform.getInstallLocation();
        if (install != null && install.getURL() != null) {
            File libFolder = new File(install.getURL().getFile()).getParentFile();
            if (libFolder != null) {
                String toolsFolder = libFolder.getParent();
                if (toolsFolder != null) {
                    sdkLocation = new File(toolsFolder).getParent();
                    if (isValidSdkLocation(sdkLocation)) {
                        MonitorPlugin.getDdmsPreferenceStore().setLastSdkPath(sdkLocation);
                        return sdkLocation;
                    }
                }

            }
        }

        // check for the last used SDK
        sdkLocation = MonitorPlugin.getDdmsPreferenceStore().getLastSdkPath();
        if (isValidSdkLocation(sdkLocation)) {
            return sdkLocation;
        }

        // if nothing else works, prompt the user
        sdkLocation = getSdkLocationFromUser(new Shell(display));
        if (isValidSdkLocation(sdkLocation)) {
            MonitorPlugin.getDdmsPreferenceStore().setLastSdkPath(sdkLocation);
        }

        return sdkLocation;
    }

    private boolean isValidSdkLocation(String sdkLocation) {
        if (sdkLocation == null) {
            return false;
        }

        if (sdkLocation.trim().length() == 0) {
            return false;
        }

        SdkToolsLocator locator = new SdkToolsLocator(new File(sdkLocation));
        return locator.isValidInstallation() == SdkInstallStatus.VALID;
    }

    private String getSdkLocationFromUser(Shell shell) {
        SdkLocationChooserDialog dlg = new SdkLocationChooserDialog(shell);
        if (dlg.open() == Window.OK) {
            return dlg.getPath();
        } else {
            return null;
        }
    }
}
