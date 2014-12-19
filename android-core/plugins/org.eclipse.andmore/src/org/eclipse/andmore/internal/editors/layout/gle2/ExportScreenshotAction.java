/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.DOT_PNG;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Saves the current layout editor's rendered image to disk */
class ExportScreenshotAction extends Action {
    private final LayoutCanvas mCanvas;

    ExportScreenshotAction(LayoutCanvas canvas) {
        super("Export Screenshot...");
        mCanvas = canvas;
    }

    @Override
    public void run() {
        Shell shell = AdtPlugin.getShell();

        ImageOverlay imageOverlay = mCanvas.getImageOverlay();
        BufferedImage image = imageOverlay.getAwtImage();
        if (image != null) {
            FileDialog dialog = new FileDialog(shell, SWT.SAVE);
            dialog.setFilterExtensions(new String[] { "*.png" }); //$NON-NLS-1$
            String path = dialog.open();
            if (path != null) {
                if (!path.endsWith(DOT_PNG)) {
                    path = path + DOT_PNG;
                }
                File file = new File(path);
                if (file.exists()) {
                    MessageDialog d = new MessageDialog(null, "File Already Exists", null,
                            String.format(
                                    "%1$s already exists.\nWould you like to replace it?",
                                    path),
                            MessageDialog.QUESTION, new String[] {
                                    // Yes will be moved to the end because it's the default
                                    "Yes", "No"
                            }, 0);
                    int result = d.open();
                    if (result != 0) {
                        return;
                    }
                }
                try {
                    ImageIO.write(image, "PNG", file); //$NON-NLS-1$
                } catch (IOException e) {
                    AdtPlugin.log(e, null);
                }
            }
        } else {
            MessageDialog.openError(shell, "Error", "Image not available");
        }
    }
}
