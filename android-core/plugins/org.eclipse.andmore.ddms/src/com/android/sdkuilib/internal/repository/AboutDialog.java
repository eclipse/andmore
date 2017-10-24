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
import com.android.sdklib.io.LegacyFileOp;
import com.android.sdklib.repository.PkgProps;
import com.android.sdklib.repository.SdkAddonConstants;
import com.android.sdklib.repository.SdkRepoConstants;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AboutDialog extends UpdaterBaseDialog {

    public AboutDialog(Shell parentShell, SwtUpdaterData swtUpdaterData) {
        super(parentShell, swtUpdaterData, "About" /*title*/);
        assert swtUpdaterData != null;
    }

    @Override
    protected void createContents() {
        super.createContents();
        Shell shell = getShell();
        shell.setMinimumSize(new Point(450, 150));
        shell.setSize(450, 150);

        GridLayoutBuilder.create(shell).columns(3);

        Label logo = new Label(shell, SWT.NONE);
        ImageFactory imgf = getSwtUpdaterData() == null ? null
                                                        : getSwtUpdaterData().getImageFactory();
        Image image = imgf == null ? null : imgf.getImageByName("sdkman_logo_128.png");
        if (image != null) logo.setImage(image);

        Label label = new Label(shell, SWT.NONE);
        GridDataBuilder.create(label).hFill().hGrab().hSpan(2);;
        label.setText(String.format(
                "Android SDK Manager.\n" +
                "Revision %1$s\n" +
                "Add-on XML Schema #%2$d\n" +
                "Repository XML Schema #%3$d\n" +
                // TODO: update with new year date (search this to find other occurrences to update)
                "Copyright (C) 2009-2012 The Android Open Source Project.",
                getRevision(),
                SdkAddonConstants.NS_LATEST_VERSION,
                SdkRepoConstants.NS_LATEST_VERSION));

        Label filler = new Label(shell, SWT.NONE);
        GridDataBuilder.create(filler).fill().grab().hSpan(2);

        createCloseButton();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // End of hiding from SWT Designer
    //$hide<<$

    private String getRevision() {
        Properties p = new Properties();
        try{
            File sourceProp = LegacyFileOp.append(getSwtUpdaterData().getOsSdkRoot(),
                    SdkConstants.FD_TOOLS,
                    SdkConstants.FN_SOURCE_PROP);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sourceProp);
                p.load(fis);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            String revision = p.getProperty(PkgProps.PKG_REVISION);
            if (revision != null) {
                return revision;
            }
        } catch (IOException e) {
        }

        return "?";
    }
}
