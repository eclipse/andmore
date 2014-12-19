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

import com.android.ide.common.api.Point;

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

/**
 * A {@link LayoutPoint} is a coordinate in the Android canvas (in other words,
 * it may differ from the canvas control mouse coordinate because the canvas may
 * be zoomed and scrolled.)
 */
public final class LayoutPoint {
    /** Containing canvas which the point is relative to. */
    private final LayoutCanvas mCanvas;

    /** The X coordinate of the canvas coordinate. */
    public final int x;

    /** The Y coordinate of the canvas coordinate. */
    public final int y;

    /**
     * Constructs a new {@link LayoutPoint} from the given event. The event
     * must be from a {@link MouseListener} associated with the
     * {@link LayoutCanvas} such that the {@link MouseEvent#x} and
     * {@link MouseEvent#y} fields are relative to the canvas.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The mouse event to construct the {@link LayoutPoint}
     *            from.
     * @return A {@link LayoutPoint} which corresponds to the given
     *         {@link MouseEvent}.
     */
    public static LayoutPoint create(LayoutCanvas canvas, MouseEvent event) {
        // The mouse event coordinates should already be relative to the canvas
        // widget.
        assert event.widget == canvas : event.widget;
        return ControlPoint.create(canvas, event).toLayout();
    }

    /**
     * Constructs a new {@link LayoutPoint} from the given event. The event
     * must be from a {@link DragSourceListener} associated with the
     * {@link LayoutCanvas} such that the {@link DragSourceEvent#x} and
     * {@link DragSourceEvent#y} fields are relative to the canvas.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The mouse event to construct the {@link LayoutPoint}
     *            from.
     * @return A {@link LayoutPoint} which corresponds to the given
     *         {@link DragSourceEvent}.
     */
    public static LayoutPoint create(LayoutCanvas canvas, DragSourceEvent event) {
        // The drag source event coordinates should already be relative to the
        // canvas widget.
        return ControlPoint.create(canvas, event).toLayout();
    }

    /**
     * Constructs a new {@link LayoutPoint} from the given x,y coordinates.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param x The mouse event x coordinate relative to the canvas
     * @param y The mouse event x coordinate relative to the canvas
     * @return A {@link LayoutPoint} which corresponds to the given
     *         layout coordinates.
     */
    public static LayoutPoint create(LayoutCanvas canvas, int x, int y) {
        return new LayoutPoint(canvas, x, y);
    }

    /**
     * Constructs a new {@link LayoutPoint} with the given X and Y coordinates.
     *
     * @param canvas The canvas which contains this coordinate
     * @param x The canvas X coordinate
     * @param y The canvas Y coordinate
     */
    private LayoutPoint(LayoutCanvas canvas, int x, int y) {
        mCanvas = canvas;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the equivalent {@link ControlPoint} to this
     * {@link LayoutPoint}.
     *
     * @return The equivalent {@link ControlPoint} to this
     *         {@link LayoutPoint}
     */
    public ControlPoint toControl() {
        int cx = mCanvas.getHorizontalTransform().translate(x);
        int cy = mCanvas.getVerticalTransform().translate(y);

        return ControlPoint.create(mCanvas, cx, cy);
    }

    /**
     * Returns this {@link LayoutPoint} as a {@link Point}, in the same coordinate space.
     *
     * @return a new {@link Point} in the same coordinate space
     */
    public Point toPoint() {
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return "LayoutPoint [x=" + x + ", y=" + y + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LayoutPoint other = (LayoutPoint) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }
}
