/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.ui;

import static com.android.SdkConstants.DOT_9PNG;
import static com.android.utils.SdkUtils.endsWithIgnoreCase;

import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceResolver;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageControl;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderService;
import org.eclipse.andmore.internal.editors.layout.gle2.SwtUtils;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * The {@link ResourcePreviewHelper} provides help to {@link TrayDialog} resource choosers
 * where some resources (such as drawables and colors) are previewed in the tray area.
 */
public class ResourcePreviewHelper {
    /**
     * The width of the preview rendering
     * <p>
     * TODO: Make the preview rendering resize if the tray area is resized
     */
    private static final int WIDTH = 100;
    /** The height of the preview rendering */
    private static final int HEIGHT = 100;

    private final GraphicalEditorPart mEditor;
    private final TrayDialog mTrayDialog;

    private boolean mShowingPreview;
    private DialogTray mPreviewTray;
    private ImageControl mPreviewImageControl;

    /**
     * Constructs a new {@link ResourcePreviewHelper}.
     * <p>
     * TODO: Add support for performing previews without an associated graphical editor,
     * such as previewing icons from the manifest form editor; just pick default
     * configuration settings in that case.
     *
     * @param trayDialog the associated tray-capable dialog
     * @param editor a graphical editor. This is currently needed in order to provide
     *            configuration data for the rendering.
     */
    public ResourcePreviewHelper(TrayDialog trayDialog, GraphicalEditorPart editor) {
        this.mTrayDialog = trayDialog;
        this.mEditor = editor;
    }

    /**
     * Updates the preview based on the current selection and resource type, possibly
     * hiding or opening the tray in the process.
     *
     * @param type the resource type for the selected resource
     * @param resource the full resource url
     */
    public void updatePreview(ResourceType type, String resource) {
        boolean showPreview = type == ResourceType.DRAWABLE || type == ResourceType.COLOR;
        if (showPreview) {
            if (mPreviewTray == null) {
                mPreviewTray = new DialogTray() {
                    @Override
                    protected Control createContents(Composite parent) {
                        // This creates a centered image control
                        Composite panel = new Composite(parent, SWT.NONE);
                        panel.setLayout(new GridLayout(3, false));
                        Label dummy1 = new Label(panel, SWT.NONE);
                        dummy1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
                        mPreviewImageControl = new ImageControl(panel, SWT.NONE, SwtUtils
                                .createEmptyImage(parent.getDisplay(), WIDTH, HEIGHT));
                        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
                        gd.widthHint = WIDTH;
                        gd.heightHint = HEIGHT;
                        mPreviewImageControl.setLayoutData(gd);
                        Label dummy2 = new Label(panel, SWT.NONE);
                        dummy2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));

                        return panel;
                    }

                };
            }

            if (!mShowingPreview) {
                mTrayDialog.openTray(mPreviewTray);
            }

            BufferedImage image = null;
            try {
                if (type == ResourceType.COLOR) {
                    ResourceResolver resources = mEditor.getResourceResolver();
                    ResourceValue value = resources.findResValue(resource, false);
                    if (value != null) {
                        RGB color = ResourceHelper.resolveColor(resources, value);
                        if (color != null) {
                            image = ImageUtils.createColoredImage(WIDTH, HEIGHT, color);
                        }
                    }
                } else {
                    assert type == ResourceType.DRAWABLE;

                    ResourceResolver resources = mEditor.getResourceResolver();
                    ResourceValue drawable = resources.findResValue(resource, false);
                    if (drawable != null) {
                        String path = drawable.getValue();

                        // Special-case image files (other than 9-patch files) and render these
                        // directly, in order to provide proper aspect ratio handling and
                        // to handle scaling to show the full contents:
                        if (ImageUtils.hasImageExtension(path)
                                && !endsWithIgnoreCase(path, DOT_9PNG)) {
                            File file = new File(path);
                            if (file.exists()) {
                                try {
                                    image = ImageIO.read(file);
                                    int width = image.getWidth();
                                    int height = image.getHeight();
                                    if (width > WIDTH || height > HEIGHT) {
                                        double xScale = WIDTH / (double) width;
                                        double yScale = HEIGHT / (double) height;
                                        double scale = Math.min(xScale, yScale);
                                        image = ImageUtils.scale(image, scale, scale);
                                    }
                                } catch (IOException e) {
                                    AdtPlugin.log(e, "Can't read preview image %1$s", path);
                                }
                            }
                        }

                        if (image == null) {
                            RenderService renderService = RenderService.create(mEditor);
                            renderService.setOverrideRenderSize(WIDTH, HEIGHT);
                            image = renderService.renderDrawable(drawable);
                        }
                    }
                }
            } catch (Throwable t) {
                // Some kind of rendering error occurred. However, we don't want to use
                //    AdtPlugin.log(t, "Can't generate preview for %1$s", resource);
                // because if it's a severe type of error (such as an InternalError shown
                // in issue #18623) then a dialog will pop up and interfere with the
                // preview, so just log a warning (unfortunately without the trace) instead.
                AdtPlugin.log(IStatus.WARNING, "Can't generate preview for %1$s", resource);
            }

            Display display = mEditor.getSite().getShell().getDisplay();
            if (image != null) {
                mPreviewImageControl.setImage(SwtUtils.convertToSwt(display, image, true, -1));
            } else {
                mPreviewImageControl.setImage(SwtUtils.createEmptyImage(display, WIDTH, HEIGHT));
            }
            mPreviewImageControl.redraw();
        } else if (mPreviewTray != null && mShowingPreview) {
            mTrayDialog.closeTray();
        }
        mShowingPreview = showPreview;
    }
}
