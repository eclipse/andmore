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

package org.eclipse.andmore.internal.editors.draw9patch.ui;

import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Image editor pane.
 */
public class ImageEditorPanel extends Composite implements ImageViewer.UpdateListener,
        StatusPanel.StatusChangedListener {

    private static final int WEIGHT_VIEWER = 3;
    private static final int WEIGHT_PREVIEW = 1;

    private final ImageViewer mImageViewer;
    private final StretchesViewer mStretchesViewer;

    public ImageViewer getImageViewer() {
        return mImageViewer;
    }

    public ImageEditorPanel(Composite parent, int style) {
        super(parent, style);

        setLayout(new FillLayout());
        SashForm sashForm = new SashForm(this, SWT.HORIZONTAL);

        mImageViewer = new ImageViewer(sashForm, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        mImageViewer.addUpdateListener(this);

        mStretchesViewer = new StretchesViewer(sashForm, SWT.BORDER);

        sashForm.setWeights(new int[] {
                WEIGHT_VIEWER, WEIGHT_PREVIEW
        });
    }

    @Override
    public void zoomChanged(int zoom) {
        mImageViewer.setZoom(zoom);
    }

    @Override
    public void scaleChanged(int scale) {
        mStretchesViewer.setScale(scale);
    }

    @Override
    public void lockVisibilityChanged(boolean visible) {
        mImageViewer.setShowLock(visible);
    }

    @Override
    public void patchesVisibilityChanged(boolean visible) {
        mImageViewer.setShowPatchesArea(visible);
    }

    @Override
    public void badPatchesVisibilityChanged(boolean visible) {
        mImageViewer.setShowBadPatchesArea(visible);
    }

    @Override
    public void contentAreaVisibilityChanged(boolean visible) {
        mStretchesViewer.setShowContentArea(visible);
    }

    @Override
    public void update(NinePatchedImage image) {
        mStretchesViewer.updatePreview(image);
    }
}
