/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

import com.android.annotations.NonNull;
import com.android.sdklib.internal.repository.AdbWrapper;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.NullTaskMonitor;
import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.updater.ArchiveInfo;
import com.android.sdklib.internal.repository.updater.SdkUpdaterLogic;
import com.android.sdklib.internal.repository.updater.UpdaterData;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.repository.ui.SdkUpdaterWindowImpl2;
import com.android.utils.ILogger;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Data shared between {@link SdkUpdaterWindowImpl2} and its pages.
 */
public class SwtUpdaterData extends UpdaterData {

    private Shell mWindowShell;

    /**
     * The current {@link ImageFactory}.
     * Set via {@link #setImageFactory(ImageFactory)} by the window implementation.
     * It is null when invoked using the command-line interface.
     */
    private ImageFactory mImageFactory;

    /**
     * Creates a new updater data.
     *
     * @param sdkLog Logger. Cannot be null.
     * @param osSdkRoot The OS path to the SDK root.
     */
    public SwtUpdaterData(String osSdkRoot, ILogger sdkLog) {
        super(osSdkRoot, sdkLog);
    }

    // ----- getters, setters ----

    public void setImageFactory(ImageFactory imageFactory) {
        mImageFactory = imageFactory;
    }

    public ImageFactory getImageFactory() {
        return mImageFactory;
    }

    public void setWindowShell(Shell windowShell) {
        mWindowShell = windowShell;
    }

    public Shell getWindowShell() {
        return mWindowShell;
    }

    @Override
    protected void displayInitError(String error) {
        // We may not have any UI. Only display a dialog if there's a window shell available.
        if (mWindowShell != null && !mWindowShell.isDisposed()) {
            MessageDialog.openError(mWindowShell,
                "Android Virtual Devices Manager",
                error);
        } else {
            super.displayInitError(error);
        }
    }

    // -----

    /**
     * Runs the runnable on the UI thread using {@link Display#syncExec(Runnable)}.
     *
     * @param r Non-null runnable.
     */
    @Override
    protected void runOnUiThread(@NonNull Runnable r) {
        if (mWindowShell != null && !mWindowShell.isDisposed()) {
            mWindowShell.getDisplay().syncExec(r);
        }
    }

    /**
     * Attempts to restart ADB.
     * <p/>
     * If the "ask before restart" setting is set (the default), prompt the user whether
     * now is a good time to restart ADB.
     */
    @Override
    protected void askForAdbRestart(ITaskMonitor monitor) {
        final boolean[] canRestart = new boolean[] { true };

        if (getWindowShell() != null &&
                getSettingsController().getSettings().getAskBeforeAdbRestart()) {
            // need to ask for permission first
            final Shell shell = getWindowShell();
            if (shell != null && !shell.isDisposed()) {
                shell.getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!shell.isDisposed()) {
                            canRestart[0] = MessageDialog.openQuestion(shell,
                                    "ADB Restart",
                                    "A package that depends on ADB has been updated. \n" +
                                    "Do you want to restart ADB now?");
                        }
                    }
                });
            }
        }

        if (canRestart[0]) {
            AdbWrapper adb = new AdbWrapper(getOsSdkRoot(), monitor);
            adb.stopAdb();
            adb.startAdb();
        }
    }

    @Override
    protected void notifyToolsNeedsToBeRestarted(int flags) {
        String msg = null;
        if ((flags & TOOLS_MSG_UPDATED_FROM_ADT) != 0) {
            msg =
            "The Android SDK and AVD Manager that you are currently using has been updated. " +
            "Please also run Eclipse > Help > Check for Updates to see if the Android " +
            "plug-in needs to be updated.";

        } else if ((flags & TOOLS_MSG_UPDATED_FROM_SDKMAN) != 0) {
            msg =
            "The Android SDK and AVD Manager that you are currently using has been updated. " +
            "It is recommended that you now close the manager window and re-open it. " +
            "If you use Eclipse, please run Help > Check for Updates to see if the Android " +
            "plug-in needs to be updated.";
        }

        final String msg2 = msg;

        final Shell shell = getWindowShell();
        if (msg2 != null && shell != null && !shell.isDisposed()) {
            shell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!shell.isDisposed()) {
                        MessageDialog.openInformation(shell,
                                "Android Tools Updated",
                                msg2);
                    }
                }
            });
        }
    }

    /**
     * Tries to update all the *existing* local packages.
     * This version *requires* to be run with a GUI.
     * <p/>
     * There are two modes of operation:
     * <ul>
     * <li>If selectedArchives is null, refreshes all sources, compares the available remote
     * packages with the current local ones and suggest updates to be done to the user (including
     * new platforms that the users doesn't have yet).
     * <li>If selectedArchives is not null, this represents a list of archives/packages that
     * the user wants to install or update, so just process these.
     * </ul>
     *
     * @param selectedArchives The list of remote archives to consider for the update.
     *  This can be null, in which case a list of remote archive is fetched from all
     *  available sources.
     * @param includeObsoletes True if obsolete packages should be used when resolving what
     *  to update.
     * @param flags Optional flags for the installer, such as {@link #NO_TOOLS_MSG}.
     * @return A list of archives that have been installed. Can be null if nothing was done.
     */
    @Override
    public List<Archive> updateOrInstallAll_WithGUI(
            Collection<Archive> selectedArchives,
            boolean includeObsoletes,
            int flags) {

        // Note: we no longer call refreshSources(true) here. This will be done
        // automatically by computeUpdates() iif it needs to access sources to
        // resolve missing dependencies.

        SdkUpdaterLogic ul = new SdkUpdaterLogic(this);
        List<ArchiveInfo> archives = ul.computeUpdates(
                selectedArchives,
                getSources(),
                getLocalSdkParser().getPackages(),
                includeObsoletes);

        if (selectedArchives == null) {
            getPackageLoader().loadRemoteAddonsList(new NullTaskMonitor(getSdkLog()));
            ul.addNewPlatforms(
                    archives,
                    getSources(),
                    getLocalSdkParser().getPackages(),
                    includeObsoletes);
        }

        // TODO if selectedArchives is null and archives.len==0, find if there are
        // any new platform we can suggest to install instead.

        Collections.sort(archives);

        SdkUpdaterChooserDialog dialog =
            new SdkUpdaterChooserDialog(getWindowShell(), this, archives);
        dialog.open();

        ArrayList<ArchiveInfo> result = dialog.getResult();
        if (result != null && result.size() > 0) {
            return installArchives(result, flags);
        }
        return null;
    }
}
