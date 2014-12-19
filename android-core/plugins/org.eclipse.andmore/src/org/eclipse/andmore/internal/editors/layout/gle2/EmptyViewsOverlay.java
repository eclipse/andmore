/*
 * Copyright (C) 2010 The Android Open Source Project
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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * The {@link EmptyViewsOverlay} paints bounding rectangles for any of the empty and
 * invisible container views in the scene.
 */
public class EmptyViewsOverlay extends Overlay {
    /** The {@link ViewHierarchy} containing visible view information. */
    private final ViewHierarchy mViewHierarchy;

    /** Border color to paint the bounding boxes with. */
    private Color mBorderColor;

    /** Vertical scaling & scrollbar information. */
    private CanvasTransform mVScale;

    /** Horizontal scaling & scrollbar information. */
    private CanvasTransform mHScale;

    /**
     * Constructs a new {@link EmptyViewsOverlay} linked to the given view hierarchy.
     *
     * @param viewHierarchy The {@link ViewHierarchy} to render.
     * @param hScale The {@link CanvasTransform} to use to transfer horizontal layout
     *            coordinates to screen coordinates.
     * @param vScale The {@link CanvasTransform} to use to transfer vertical layout coordinates
     *            to screen coordinates.
     */
    public EmptyViewsOverlay(
            ViewHierarchy viewHierarchy,
            CanvasTransform hScale,
            CanvasTransform vScale) {
        super();
        mViewHierarchy = viewHierarchy;
        mHScale = hScale;
        mVScale = vScale;
    }

    @Override
    public void create(Device device) {
        mBorderColor = new Color(device, SwtDrawingStyle.EMPTY.getStrokeColor());
    }

    @Override
    public void dispose() {
        if (mBorderColor != null) {
            mBorderColor.dispose();
            mBorderColor = null;
        }
    }

    @Override
    public void paint(GC gc) {
        gc.setForeground(mBorderColor);
        gc.setLineDash(null);
        gc.setLineStyle(SwtDrawingStyle.EMPTY.getLineStyle());
        int oldAlpha = gc.getAlpha();
        gc.setAlpha(SwtDrawingStyle.EMPTY.getStrokeAlpha());
        gc.setLineWidth(SwtDrawingStyle.EMPTY.getLineWidth());

        for (CanvasViewInfo info : mViewHierarchy.getInvisibleViews()) {
            Rectangle r = info.getAbsRect();

            int x = mHScale.translate(r.x);
            int y = mVScale.translate(r.y);
            int w = mHScale.scale(r.width);
            int h = mVScale.scale(r.height);

            // +1: See explanation in equivalent code in {@link OutlineOverlay#paint}
            gc.drawRectangle(x, y, w + 1, h + 1);
        }

        gc.setAlpha(oldAlpha);
    }
}
