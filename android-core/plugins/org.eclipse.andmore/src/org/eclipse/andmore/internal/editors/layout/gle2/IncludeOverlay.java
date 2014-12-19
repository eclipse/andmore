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

import com.android.annotations.VisibleForTesting;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The {@link IncludeOverlay} class renders masks to -partially- hide everything outside
 * an included file's own content. This overlay is in use when you are editing an included
 * file shown within a different file's context (e.g. "Show In > other").
 */
public class IncludeOverlay extends Overlay {
    /** Mask transparency - 0 is transparent, 255 is opaque */
    private static final int MASK_TRANSPARENCY = 160;

    /** The associated {@link LayoutCanvas}. */
    private LayoutCanvas mCanvas;

    /**
     * Constructs an {@link IncludeOverlay} tied to the given canvas.
     *
     * @param canvas The {@link LayoutCanvas} to paint the overlay over.
     */
    public IncludeOverlay(LayoutCanvas canvas) {
        mCanvas = canvas;
    }

    @Override
    public void paint(GC gc) {
        ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
        List<Rectangle> includedBounds = viewHierarchy.getIncludedBounds();
        if (includedBounds == null || includedBounds.size() == 0) {
            // We don't support multiple included children yet. When that works,
            // this code should use a BSP tree to figure out which regions to paint
            // to leave holes in the mask.
            return;
        }

        Image image = mCanvas.getImageOverlay().getImage();
        if (image == null) {
            return;
        }

        int oldAlpha = gc.getAlpha();
        gc.setAlpha(MASK_TRANSPARENCY);
        Color bg = gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        gc.setBackground(bg);

        CanvasViewInfo root = viewHierarchy.getRoot();
        Rectangle whole = root.getAbsRect();
        whole = new Rectangle(whole.x, whole.y, whole.width + 1, whole.height + 1);
        Collection<Rectangle> masks = subtractRectangles(whole, includedBounds);

        for (Rectangle mask : masks) {
            ControlPoint topLeft = LayoutPoint.create(mCanvas, mask.x, mask.y).toControl();
            ControlPoint bottomRight = LayoutPoint.create(mCanvas, mask.x + mask.width,
                    mask.y + mask.height).toControl();
            int x1 = topLeft.x;
            int y1 = topLeft.y;
            int x2 = bottomRight.x;
            int y2 = bottomRight.y;

            gc.fillRectangle(x1, y1, x2 - x1, y2 - y1);
        }

        gc.setAlpha(oldAlpha);
    }

    /**
     * Given a Rectangle, remove holes from it (specified as a collection of Rectangles) such
     * that the result is a list of rectangles that cover everything that is not a hole.
     *
     * @param rectangle the rectangle to subtract from
     * @param holes the holes to subtract from the rectangle
     * @return a list of sub rectangles that remain after subtracting out the given list of holes
     */
    @VisibleForTesting
    static Collection<Rectangle> subtractRectangles(
            Rectangle rectangle, Collection<Rectangle> holes) {
        List<Rectangle> result = new ArrayList<Rectangle>();
        result.add(rectangle);

        for (Rectangle hole : holes) {
            List<Rectangle> tempResult = new ArrayList<Rectangle>();
            for (Rectangle r : result) {
                if (hole.intersects(r)) {
                    // Clip the hole to fit the rectangle bounds
                    Rectangle h = hole.intersection(r);

                    // Split the rectangle

                    // Above (includes the NW and NE corners)
                    if (h.y > r.y) {
                        tempResult.add(new Rectangle(r.x, r.y, r.width, h.y - r.y));
                    }

                    // Left (not including corners)
                    if (h.x > r.x) {
                        tempResult.add(new Rectangle(r.x, h.y, h.x - r.x, h.height));
                    }

                    int hx2 = h.x + h.width;
                    int hy2 = h.y + h.height;
                    int rx2 = r.x + r.width;
                    int ry2 = r.y + r.height;

                    // Below (includes the SW and SE corners)
                    if (hy2 < ry2) {
                        tempResult.add(new Rectangle(r.x, hy2, r.width, ry2 - hy2));
                    }

                    // Right (not including corners)
                    if (hx2 < rx2) {
                        tempResult.add(new Rectangle(hx2, h.y, rx2 - hx2, h.height));
                    }
                } else {
                    tempResult.add(r);
                }
            }

            result = tempResult;
        }

        return result;
    }
}
