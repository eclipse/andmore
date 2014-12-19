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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A {@link MarqueeGesture} is a gesture for swiping out a selection rectangle.
 * With a modifier key, items that intersect the rectangle can be toggled
 * instead of added to the new selection set.
 */
public class MarqueeGesture extends Gesture {
    /** The {@link Overlay} drawn for the marquee. */
    private MarqueeOverlay mOverlay;

    /** The canvas associated with this gesture. */
    private LayoutCanvas mCanvas;

    /** A copy of the initial selection, when we're toggling the marquee. */
    private Collection<CanvasViewInfo> mInitialSelection;

    /**
     * Creates a new marquee selection (selection swiping).
     *
     * @param canvas The canvas where selection is performed.
     * @param toggle If true, toggle the membership of contained elements
     *            instead of adding it.
     */
    public MarqueeGesture(LayoutCanvas canvas, boolean toggle) {
        mCanvas = canvas;

        if (toggle) {
            List<SelectionItem> selection = canvas.getSelectionManager().getSelections();
            mInitialSelection = new ArrayList<CanvasViewInfo>(selection.size());
            for (SelectionItem item : selection) {
                mInitialSelection.add(item.getViewInfo());
            }
        } else {
            mInitialSelection = Collections.emptySet();
        }
    }

    @Override
    public void update(ControlPoint pos) {
        if (mOverlay == null) {
            return;
        }

        int x = Math.min(pos.x, mStart.x);
        int y = Math.min(pos.y, mStart.y);
        int w = Math.abs(pos.x - mStart.x);
        int h = Math.abs(pos.y - mStart.y);

        mOverlay.updateSize(x, y, w, h);

        // Compute selection overlaps
        LayoutPoint topLeft = ControlPoint.create(mCanvas, x, y).toLayout();
        LayoutPoint bottomRight = ControlPoint.create(mCanvas, x + w, y + h).toLayout();
        mCanvas.getSelectionManager().selectWithin(topLeft, bottomRight, mInitialSelection);
    }

    @Override
    public List<Overlay> createOverlays() {
        mOverlay = new MarqueeOverlay();
        return Collections.<Overlay> singletonList(mOverlay);
    }

    /**
     * An {@link Overlay} for the {@link MarqueeGesture}; paints a selection
     * overlay rectangle matching the mouse coordinate delta between gesture
     * start and the current position.
     */
    private static class MarqueeOverlay extends Overlay {
        /** Rectangle border color. */
        private Color mStroke;

        /** Rectangle fill color. */
        private Color mFill;

        /** Current rectangle coordinates (in terms of control coordinates). */
        private Rectangle mRectangle = new Rectangle(0, 0, 0, 0);

        /** Alpha value of the fill. */
        private int mFillAlpha;

        /** Alpha value of the border. */
        private int mStrokeAlpha;

        /** Constructs a new {@link MarqueeOverlay}. */
        public MarqueeOverlay() {
        }

        /**
         * Updates the size of the marquee rectangle.
         *
         * @param x The top left corner of the rectangle, x coordinate.
         * @param y The top left corner of the rectangle, y coordinate.
         * @param w Rectangle width.
         * @param h Rectangle height.
         */
        public void updateSize(int x, int y, int w, int h) {
            mRectangle.x = x;
            mRectangle.y = y;
            mRectangle.width = w;
            mRectangle.height = h;
        }

        @Override
        public void create(Device device) {
            // TODO: Integrate DrawingStyles with this?
            mStroke = new Color(device, 255, 255, 255);
            mFill = new Color(device, 128, 128, 128);
            mFillAlpha = 64;
            mStrokeAlpha = 255;
        }

        @Override
        public void dispose() {
            mStroke.dispose();
            mFill.dispose();
        }

        @Override
        public void paint(GC gc) {
            if (mRectangle.width > 0 && mRectangle.height > 0) {
                gc.setLineStyle(SWT.LINE_SOLID);
                gc.setLineWidth(1);
                gc.setForeground(mStroke);
                gc.setBackground(mFill);
                gc.setAlpha(mStrokeAlpha);
                gc.drawRectangle(mRectangle);
                gc.setAlpha(mFillAlpha);
                gc.fillRectangle(mRectangle);
            }
        }
    }
}
