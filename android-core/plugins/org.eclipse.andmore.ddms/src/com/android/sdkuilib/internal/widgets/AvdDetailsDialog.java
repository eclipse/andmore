/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.widgets;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdInfo.AvdStatus;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.sdkuilib.ui.SwtBaseDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog displaying the details of an AVD.
 */
final class AvdDetailsDialog extends SwtBaseDialog {

    private final AvdInfo mAvdInfo;

    public AvdDetailsDialog(Shell shell, AvdInfo avdInfo) {
        super(shell, SWT.APPLICATION_MODAL, "AVD details");
        mAvdInfo = avdInfo;
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected void createContents() {
        Shell shell = getShell();
        GridLayoutBuilder.create(shell).columns(2);
        GridDataBuilder.create(shell).fill();

        GridLayout gl;

        Composite c = new Composite(shell, SWT.NONE);
        c.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (mAvdInfo != null) {
            displayValue(c, "Name:", mAvdInfo.getName());
            displayValue(c, "CPU/ABI:", AvdInfo.getPrettyAbiType(mAvdInfo));

            displayValue(c, "Path:", mAvdInfo.getDataFolderPath());

            if (mAvdInfo.getStatus() != AvdStatus.OK) {
                displayValue(c, "Error:", mAvdInfo.getErrorMessage());
            } else {
                IAndroidTarget target = mAvdInfo.getTarget();
                AndroidVersion version = target.getVersion();
                displayValue(c, "Target:", String.format("%s (API level %s)",
                        target.getName(), version.getApiString()));

                // display some extra values.
                Map<String, String> properties = mAvdInfo.getProperties();
                if (properties != null) {
                    String skin = properties.get(AvdManager.AVD_INI_SKIN_NAME);
                    if (skin != null) {
                        displayValue(c, "Skin:", skin);
                    }

                    String sdcard = properties.get(AvdManager.AVD_INI_SDCARD_SIZE);
                    if (sdcard == null) {
                        sdcard = properties.get(AvdManager.AVD_INI_SDCARD_PATH);
                    }
                    if (sdcard != null) {
                        displayValue(c, "SD Card:", sdcard);
                    }

                    String snapshot = properties.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
                    if (snapshot != null) {
                        displayValue(c, "Snapshot:", snapshot);
                    }

                    // display other hardware
                    HashMap<String, String> copy = new HashMap<String, String>(properties);
                    // remove stuff we already displayed (or that we don't want to display)
                    copy.remove(AvdManager.AVD_INI_ABI_TYPE);
                    copy.remove(AvdManager.AVD_INI_CPU_ARCH);
                    copy.remove(AvdManager.AVD_INI_SKIN_NAME);
                    copy.remove(AvdManager.AVD_INI_SKIN_PATH);
                    copy.remove(AvdManager.AVD_INI_SDCARD_SIZE);
                    copy.remove(AvdManager.AVD_INI_SDCARD_PATH);
                    copy.remove(AvdManager.AVD_INI_IMAGES_1);
                    copy.remove(AvdManager.AVD_INI_IMAGES_2);

                    if (copy.size() > 0) {
                        Label l = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
                        l.setLayoutData(new GridData(
                                GridData.FILL, GridData.CENTER, false, false, 2, 1));

                        c = new Composite(shell, SWT.NONE);
                        c.setLayout(gl = new GridLayout(2, false));
                        gl.marginHeight = gl.marginWidth = 0;
                        c.setLayoutData(new GridData(GridData.FILL_BOTH));

                        for (Map.Entry<String, String> entry : copy.entrySet()) {
                            displayValue(c, entry.getKey() + ":", entry.getValue());
                        }
                    }
                }
            }
        }
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$


    @Override
    protected void postCreate() {
        // pass
    }

    /**
     * Displays a value with a label.
     *
     * @param parent the parent Composite in which to display the value. This Composite must use a
     * {@link GridLayout} with 2 columns.
     * @param label the label of the value to display.
     * @param value the string value to display.
     */
    private void displayValue(Composite parent, String label, String value) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);
        l.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));

        l = new Label(parent, SWT.NONE);
        l.setText(value);
        l.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
