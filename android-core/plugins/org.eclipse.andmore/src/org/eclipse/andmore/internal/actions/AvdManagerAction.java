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

import com.android.sdkuilib.repository.AvdManagerWindow;
import com.android.sdkuilib.repository.AvdManagerWindow.AvdInvocationContext;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.sdk.AdtConsoleSdkLog;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Delegate for the toolbar/menu action "AVD Manager".
 * It displays the AVD Manager window.
 */
public class AvdManagerAction implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

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
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            // Although orthogonal to the avd manager action, this is a good time
            // to check whether the SDK has changed on disk.
            AdtPlugin.getDefault().refreshSdk();

            // Runs the updater window, directing all logs to the ADT console.
            AvdManagerWindow window = new AvdManagerWindow(
                    AdtPlugin.getShell(),
                    new AdtConsoleSdkLog(),
                    sdk.getSdkOsLocation(),
                    AvdInvocationContext.IDE);
            window.open();
        } else {
            AdtPlugin.displayError("Android SDK",
                    "Location of the Android SDK has not been setup in the preferences.");
        }
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
