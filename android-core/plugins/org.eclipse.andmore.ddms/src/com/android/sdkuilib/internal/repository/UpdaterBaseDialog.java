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

package com.android.sdkuilib.internal.repository;

import com.android.SdkConstants;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.repository.ui.SdkUpdaterWindowImpl2;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.sdkuilib.ui.SwtBaseDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;



/**
 * Base class for auxiliary dialogs shown in the updater (for example settings,
 * about box or add-on site.)
 */
public abstract class UpdaterBaseDialog extends SwtBaseDialog {

    private final SwtUpdaterData mSwtUpdaterData;

    protected UpdaterBaseDialog(Shell parentShell, SwtUpdaterData swtUpdaterData, String title) {
        super(parentShell,
              SWT.APPLICATION_MODAL,
              String.format("%1$s - %2$s", SdkUpdaterWindowImpl2.APP_NAME, title)); //$NON-NLS-1$
        mSwtUpdaterData = swtUpdaterData;
    }

    public SwtUpdaterData getSwtUpdaterData() {
        return mSwtUpdaterData;
    }

    /**
     * Initializes the shell with a 2-column Grid layout.
     * Caller should use {@link #createCloseButton()} to inject the
     * close button at the bottom of the dialog.
     */
    @Override
    protected void createContents() {
        Shell shell = getShell();
        setWindowImage(shell);

        GridLayoutBuilder.create(shell).columns(2);
    }

    protected void createCloseButton() {
        Button close = new Button(getShell(), SWT.PUSH);
        close.setText("Close");
        GridDataBuilder.create(close).hFill().vBottom();
        close.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }

    @Override
    protected void postCreate() {
        // pass
    }

    @Override
    protected void close() {
        super.close();
    }

    /**
     * Creates the icon of the window shell.
     *
     * @param shell The shell on which to put the icon
     */
    private void setWindowImage(Shell shell) {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png"; //$NON-NLS-1$
        }

        if (mSwtUpdaterData != null) {
            ImageFactory imgFactory = mSwtUpdaterData.getImageFactory();
            if (imgFactory != null) {
                shell.setImage(imgFactory.getImageByName(imageName));
            }
        }
    }
}
