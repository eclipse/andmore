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
 * The {@link OutlineOverlay} paints an optional outline on top of the layout,
 * showing the structure of the individual Android View elements.
 */
public class OutlineOverlay extends Overlay {
    /** The {@link ViewHierarchy} this outline visualizes */
    private final ViewHierarchy mViewHierarchy;

    /** Outline color. Must be disposed, it's NOT a system color. */
    private Color mOutlineColor;

    /** Vertical scaling & scrollbar information. */
    private CanvasTransform mVScale;

    /** Horizontal scaling & scrollbar information. */
    private CanvasTransform mHScale;

    /**
     * Constructs a new {@link OutlineOverlay} linked to the given view
     * hierarchy.
     *
     * @param viewHierarchy The {@link ViewHierarchy} to render
     * @param hScale The {@link CanvasTransform} to use to transfer horizontal layout
     *            coordinates to screen coordinates
     * @param vScale The {@link CanvasTransform} to use to transfer vertical layout
     *            coordinates to screen coordinates
     */
    public OutlineOverlay(
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
        mOutlineColor = new Color(device, SwtDrawingStyle.OUTLINE.getStrokeColor());
    }

    @Override
    public void dispose() {
        if (mOutlineColor != null) {
            mOutlineColor.dispose();
            mOutlineColor = null;
        }
    }

    @Override
    public void paint(GC gc) {
        CanvasViewInfo lastRoot = mViewHierarchy.getRoot();
        if (lastRoot != null) {
            gc.setForeground(mOutlineColor);
            gc.setLineStyle(SwtDrawingStyle.OUTLINE.getLineStyle());
            int oldAlpha = gc.getAlpha();
            gc.setAlpha(SwtDrawingStyle.OUTLINE.getStrokeAlpha());
            drawOutline(gc, lastRoot);
            gc.setAlpha(oldAlpha);
        }
    }

    private void drawOutline(GC gc, CanvasViewInfo info) {
        Rectangle r = info.getAbsRect();

        int x = mHScale.translate(r.x);
        int y = mVScale.translate(r.y);
        int w = mHScale.scale(r.width);
        int h = mVScale.scale(r.height);

        // Add +1 to the width and +1 to the height such that when you have a
        // series of boxes (in say a LinearLayout), instead of the bottom of one
        // box and the top of the next box being -adjacent-, they -overlap-.
        // This makes the outline nicer visually since you don't get
        // "double thickness" lines for all adjacent boxes.
        gc.drawRectangle(x, y, w + 1, h + 1);

        for (CanvasViewInfo vi : info.getChildren()) {
            drawOutline(gc, vi);
        }
    }

}
