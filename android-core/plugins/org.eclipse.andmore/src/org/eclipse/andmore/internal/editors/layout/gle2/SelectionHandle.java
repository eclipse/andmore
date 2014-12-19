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
package org.eclipse.andmore.internal.editors.layout.gle2;

import org.eclipse.swt.SWT;

/**
 * A selection handle is a small rectangle on the border of a selected view which lets you
 * change the size of the view by dragging it.
 */
public class SelectionHandle {
    /**
     * Size of the selection handle radius, in control coordinates. Note that this isn't
     * necessarily a <b>circular</b> radius; in the case of a rectangular handle, the
     * width and the height are both equal to this radius.
     * Note also that this radius is in <b>control</b> coordinates, whereas the rest
     * of the class operates in layout coordinates. This is because we do not want the
     * selection handles to grow or shrink along with the screen zoom; they are always
     * at the given pixel size in the control.
     */
    public final static int PIXEL_RADIUS = 3;

    /**
     * Extra number of pixels to look beyond the actual radius of the selection handle
     * when matching mouse positions to handles
     */
    public final static int PIXEL_MARGIN = 2;

    /** The position of the handle in the selection rectangle */
    enum Position {
        TOP_MIDDLE(SWT.CURSOR_SIZEN),
        TOP_RIGHT(SWT.CURSOR_SIZENE),
        RIGHT_MIDDLE(SWT.CURSOR_SIZEE),
        BOTTOM_RIGHT(SWT.CURSOR_SIZESE),
        BOTTOM_MIDDLE(SWT.CURSOR_SIZES),
        BOTTOM_LEFT(SWT.CURSOR_SIZESW),
        LEFT_MIDDLE(SWT.CURSOR_SIZEW),
        TOP_LEFT(SWT.CURSOR_SIZENW);

        /** Corresponding SWT cursor value */
        private int mSwtCursor;

        private Position(int swtCursor) {
            mSwtCursor = swtCursor;
        }

        private int getCursorType() {
            return mSwtCursor;
        }

        /** Is the {@link SelectionHandle} somewhere on the left edge? */
        boolean isLeft() {
            return this == TOP_LEFT || this == LEFT_MIDDLE || this == BOTTOM_LEFT;
        }

        /** Is the {@link SelectionHandle} somewhere on the right edge? */
        boolean isRight() {
            return this == TOP_RIGHT || this == RIGHT_MIDDLE || this == BOTTOM_RIGHT;
        }

        /** Is the {@link SelectionHandle} somewhere on the top edge? */
        boolean isTop() {
            return this == TOP_LEFT || this == TOP_MIDDLE || this == TOP_RIGHT;
        }

        /** Is the {@link SelectionHandle} somewhere on the bottom edge? */
        boolean isBottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_MIDDLE || this == BOTTOM_RIGHT;
        }
    };

    /** The x coordinate of the center of the selection handle */
    public final int centerX;
    /** The y coordinate of the center of the selection handle */
    public final int centerY;
    /** The position of the handle in the selection rectangle */
    private final Position mPosition;

    /**
     * Constructs a new {@link SelectionHandle} at the given layout coordinate
     * corresponding to a handle at the given {@link Position}.
     *
     * @param centerX the x coordinate of the center of the selection handle
     * @param centerY y coordinate of the center of the selection handle
     * @param position the position of the handle in the selection rectangle
     */
    public SelectionHandle(int centerX, int centerY, Position position) {
        mPosition = position;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    /**
     * Determines whether the given {@link LayoutPoint} is within the given distance in
     * layout coordinates. The distance should incorporate at least the equivalent
     * distance to the control coordinate space {@link #PIXEL_RADIUS}, but usually with a
     * few extra pixels added in to make the corners easier to target.
     *
     * @param point the mouse position in layout coordinates
     * @param distance the distance from the center of the handle to check whether the
     *            point fits within
     * @return true if the given point is within the given distance of this handle
     */
    public boolean contains(LayoutPoint point, int distance) {
        return (point.x >= centerX - distance
              && point.x <= centerX + distance
              && point.y >= centerY - distance
              && point.y <= centerY + distance);
    }

    /**
     * Returns the position of the handle in the selection rectangle
     *
     * @return the position of the handle in the selection rectangle
     */
    public Position getPosition() {
        return mPosition;
    }

    /**
     * Returns the SWT cursor type to use for this selection handle
     *
     * @return the position of the handle in the selection rectangle
     */
    public int getSwtCursorType() {
        return mPosition.getCursorType();
    }
}
