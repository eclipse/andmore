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

package com.android.sdkuilib.internal.widgets;

import com.android.annotations.NonNull;
import com.android.sdklib.devices.Device;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.widgets.AvdCreationPresenter.IWidgetAdapter;
import com.android.utils.ILogger;

import org.eclipse.swt.widgets.Shell;

/**
 * Displays a dialog to create an AVD.
 * <p/>
 * Implementation: the code is split using a simplified MVP pattern. <br/>
 * {@link AvdCreationPresenter} contains all the logic of the dialog and performs all the
 * actions and event handling. <br/>
 * {@link AvdCreationSwtView} creates the SWT shell and maps the controls and forwards all
 * events to the presenter. The presenter uses the {@link IWidgetAdapter} exposed by the
 * SwtView class to update the display.
 * <p/>
 * To transform this dialog to Swing or a text-based unit-test, simply keep the presenter
 * as-is and re-implement the {@link IWidgetAdapter}.
 */
public class AvdCreationDialog extends AvdCreationSwtView {

    public AvdCreationDialog(Shell shell,
            AvdManager avdManager,
            ImageFactory imageFactory,
            ILogger log,
            AvdInfo editAvdInfo) {
        super(shell, imageFactory, new AvdCreationPresenter(avdManager, log, editAvdInfo));

    }

    /** Returns the AVD Created, if successful. */
    public AvdInfo getCreatedAvd() {
        return getPresenter().getCreatedAvd();
    }

    /**
     * Can be called after the constructor to set the default device for this AVD.
     * Useful especially for new AVDs.
     * @param device
     */
    public void selectInitialDevice(@NonNull Device device) {
        getPresenter().selectInitialDevice(device);
    }
}
