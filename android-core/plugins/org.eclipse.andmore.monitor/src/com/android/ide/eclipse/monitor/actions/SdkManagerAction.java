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

package com.android.ide.eclipse.monitor.actions;

import com.android.SdkConstants;
import com.android.ide.eclipse.monitor.MonitorPlugin;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.IOException;

public class SdkManagerAction implements IWorkbenchWindowActionDelegate {
    @Override
    public void run(IAction action) {
        File sdk = MonitorPlugin.getDefault().getSdkFolder();
        if (sdk != null) {
            File tools = new File(sdk, SdkConstants.FD_TOOLS);
            File androidBat = new File(tools, SdkConstants.androidCmdName());
            if (androidBat.exists()) {
                String[] cmd = new String[] {
                        androidBat.getAbsolutePath(),
                        getAndroidBatArgument(),
                        };
                try {
                    Runtime.getRuntime().exec(cmd);
                } catch (IOException e) {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window != null) {
                        ErrorDialog.openError(
                            window.getShell(),
                            "Monitor",
                            "Error while launching SDK Manager",
                            new Status(Status.ERROR,
                                    MonitorPlugin.PLUGIN_ID,
                                    "Error while launching SDK Manager",
                                    e));
                    }
                }
            }
        }
    }

    protected String getAndroidBatArgument() {
        return "sdk";
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
    }
}
