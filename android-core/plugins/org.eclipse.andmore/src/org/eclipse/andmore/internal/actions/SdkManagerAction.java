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

package org.eclipse.andmore.internal.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AdtConsoleSdkLog;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.io.FileOpUtils;
import com.android.sdkuilib.repository.SdkUpdaterWindow;
import com.android.sdkuilib.repository.SdkUpdaterWindow.SdkInvocationContext;
import com.android.utils.GrabProcessOutput;
import com.android.utils.GrabProcessOutput.IProcessOutput;
import com.android.utils.GrabProcessOutput.Wait;

/**
 * Delegate for the toolbar/menu action "Android SDK Manager".
 * It displays the Android SDK Manager.
 */
public class SdkManagerAction implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

    @Override
    public void dispose() {
        // nothing to dispose.
    }

    @Override
    public void init(IWorkbenchWindow window) {
        // no init
    }

    @Override
    public void run(IAction action) {
        // Although orthogonal to the sdk manager action, this is a good time
        // to check whether the SDK has changed on disk.
        AndmoreAndroidPlugin.getDefault().refreshSdk();

        if (!openExternalSdkManager()) {
            // If we failed to execute the sdk manager, check the SDK location.
            // If it's not properly set, the check will display a dialog to state
            // so to the user and a link to the prefs.
            // Here's it's ok to call checkSdkLocationAndId() since it will not try
            // to run the SdkManagerAction (it might run openExternalSdkManager though.)
            // If checkSdkLocationAndId tries to open the SDK Manager, it end up using
            // the internal one.
            if (AndmoreAndroidPlugin.getDefault().checkSdkLocationAndId()) {
                // The SDK check was successful, yet the sdk manager fail to launch anyway.
                AndmoreAndroidPlugin.displayError(
                        "Android SDK",
                        "Failed to run the Android SDK Manager. Check the Android Console View for details.");
            }
        }
    }

    /**
     * A custom implementation of {@link ProgressMonitorDialog} that allows us
     * to rename the "Cancel" button to "Close" from the internal task.
     */
    private static class CloseableProgressMonitorDialog extends ProgressMonitorDialog {

        public CloseableProgressMonitorDialog(Shell parent) {
            super(parent);
        }

        public void changeCancelToClose() {
            if (cancel != null && !cancel.isDisposed()) {
                Display display = getShell() == null ? null : getShell().getDisplay();
                if (display != null) {
                    display.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (cancel != null && !cancel.isDisposed()) {
                                cancel.setText(IDialogConstants.CLOSE_LABEL);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Opens the SDK Manager as an external application.
     * This call is asynchronous, it doesn't wait for the manager to be closed.
     * <p/>
     * Important: this method must NOT invoke {@link AndmoreAndroidPlugin#checkSdkLocationAndId}
     * (in any of its variations) since the dialog uses this method to invoke the sdk
     * manager if needed.
     *
     * @return True if the application was found and executed. False if it could not
     *   be located or could not be launched.
     */
    public static boolean openExternalSdkManager() {

        // On windows this takes a couple seconds and it's not clear the launch action
        // has been invoked. To prevent the user from randomly clicking the "open sdk manager"
        // button multiple times, show a progress window that will automatically close
        // after a couple seconds.

        // By default openExternalSdkManager will return false.
        final AtomicBoolean returnValue = new AtomicBoolean(false);

        final CloseableProgressMonitorDialog p =
            new CloseableProgressMonitorDialog(AndmoreAndroidPlugin.getShell());
        p.setOpenOnRun(true);
        try {
            p.run(true /*fork*/, true /*cancelable*/, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {

                    // Get the SDK locatiom from the current SDK or as fallback
                    // directly from the ADT preferences.
                    Sdk sdk = Sdk.getCurrent();
                    String osSdkLocation = sdk == null ? null : sdk.getSdkOsLocation();
                    if (osSdkLocation == null || !new File(osSdkLocation).isDirectory()) {
                        osSdkLocation = AdtPrefs.getPrefs().getOsSdkFolder();
                    }

                    // If there's no SDK location or it's not a valid directory,
                    // there's nothing we can do. When this is invoked from run()
                    // the checkSdkLocationAndId method call should display a dialog
                    // telling the user to configure the preferences.
                    if (osSdkLocation == null || !new File(osSdkLocation).isDirectory()) {
                        return;
                    }

                    final int numIter = 30;  //30*100=3s to wait for window
                    final int sleepMs = 100;
                    monitor.beginTask("Starting Android SDK Manager", numIter);

                    File androidBat = FileOpUtils.append(
                            osSdkLocation,
                            SdkConstants.FD_TOOLS,
                            SdkConstants.androidCmdName());

                    if (!androidBat.exists()) {
                        AndmoreAndroidPlugin.printErrorToConsole("SDK Manager",
                                "Missing %s file in Android SDK.", SdkConstants.androidCmdName());
                        return;
                    }

                    if (monitor.isCanceled()) {
                        // Canceled by user; return true as if it succeeded.
                        returnValue.set(true);
                        return;
                    }

                    p.changeCancelToClose();

                    try {
                        final AdtConsoleSdkLog logger = new AdtConsoleSdkLog();

                        String command[] = new String[] {
                                androidBat.getAbsolutePath(),
                                "sdk"   //$NON-NLS-1$
                        };
                        Process process = Runtime.getRuntime().exec(command);
                        GrabProcessOutput.grabProcessOutput(
                                process,
                                Wait.ASYNC,
                                new IProcessOutput() {
                                    @Override
                                    public void out(@Nullable String line) {
                                        // Ignore stdout
                                    }

                                    @Override
                                    public void err(@Nullable String line) {
                                        if (line != null) {
                                            logger.info("[SDK Manager] %s", line);
                                        }
                                    }
                                });

                        // Set openExternalSdkManager to return true.
                        returnValue.set(true);
                    } catch (Exception ignore) {
                    }

                    // This small wait prevents the progress dialog from closing too fast.
                    for (int i = 0; i < numIter; i++) {
                        if (monitor.isCanceled()) {
                            // Canceled by user; return true as if it succeeded.
                            returnValue.set(true);
                            return;
                        }
                        if (i == 10) {
                            monitor.subTask("Initializing... SDK Manager will show up shortly.");
                        }
                        try {
                            Thread.sleep(sleepMs);
                            monitor.worked(1);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }

                    monitor.done();
                }
            });
        } catch (Exception e) {
            AndmoreAndroidPlugin.log(e, "SDK Manager exec failed");    //$NON-NLS-1#
            return false;
        }

        return returnValue.get();
    }

    /**
     * Opens the SDK Manager bundled within ADT.
     * The call is blocking and does not return till the SD Manager window is closed.
     *
     * @return True if the SDK location is known and the SDK Manager was started.
     *   False if the SDK location is not set and we can't open a SDK Manager to
     *   manage files in an unknown location.
     */
    public static boolean openAdtSdkManager() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk == null) {
            return false;
        }

        // Runs the updater window, directing only warning/errors logs to the ADT console
        // (normal log is just dropped, which is fine since the SDK Manager has its own
        // log window now.)

        SdkUpdaterWindow window = new SdkUpdaterWindow(
                AndmoreAndroidPlugin.getShell(),
                new AdtConsoleSdkLog() {
                    @Override
                    public void info(@NonNull String msgFormat, Object... args) {
                        // Do not show non-error/warning log in Eclipse.
                    };
                    @Override
                    public void verbose(@NonNull String msgFormat, Object... args) {
                        // Do not show non-error/warning log in Eclipse.
                    };
                },
                sdk.getSdkOsLocation(),
                SdkInvocationContext.IDE);

//        ISdkChangeListener listener = new ISdkChangeListener() {
//            @Override
//            public void onSdkLoaded() {
//                // Ignore initial load of the SDK.
//            }
//
//            /**
//             * Unload all we can from the SDK before new packages are installed.
//             * Typically we need to get rid of references to dx from platform-tools
//             * and to any platform resource data.
//             * <p/>
//             * {@inheritDoc}
//             */
//            @Override
//            public void preInstallHook() {
//
//                // TODO we need to unload as much of as SDK as possible. Otherwise
//                // on Windows we end up with Eclipse locking some files and we can't
//                // replace them.
//                //
//                // At this point, we know what the user wants to install so it would be
//                // possible to pass in flags to know what needs to be unloaded. Typically
//                // we need to:
//                // - unload dex if platform-tools is going to be updated. There's a vague
//                //   attempt below at removing any references to dex and GCing. Seems
//                //   to do the trick.
//                // - unload any target that is going to be updated since it may have
//                //   resource data used by a current layout editor (e.g. data/*.ttf
//                //   and various data/res/*.xml).
//                //
//                // Most important we need to make sure there isn't a build going on
//                // and if there is one, either abort it or wait for it to complete and
//                // then we want to make sure we don't get any attempt to use the SDK
//                // before the postInstallHook is called.
//
//                if (sdk != null) {
//                    sdk.unloadTargetData(true /*preventReload*/);
//                    sdk.unloadDexWrappers();
//                }
//            }
//
//            /**
//             * Nothing to do. We'll reparse the SDK later in onSdkReload.
//             * <p/>
//             * {@inheritDoc}
//             */
//            @Override
//            public void postInstallHook() {
//            }
//
//            /**
//             * Reparse the SDK in case anything was add/removed.
//             * <p/>
//             * {@inheritDoc}
//             */
//            @Override
//            public void onSdkReload() {
//                AndmoreAndroidPlugin.getDefault().reparseSdk();
//            }
//        };
//
//        window.addListener(listener);
        window.open();

        return true;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // nothing related to the current selection.
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // nothing to do.
    }
}
