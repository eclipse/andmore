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

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;

/**
 * A {@link ControlPoint} is a coordinate in the canvas control which corresponds
 * exactly to (0,0) at the top left of the canvas. It is unaffected by canvas
 * zooming.
 */
public final class ControlPoint {
    /** Containing canvas which the point is relative to. */
    private final LayoutCanvas mCanvas;

    /** The X coordinate of the mouse coordinate. */
    public final int x;

    /** The Y coordinate of the mouse coordinate. */
    public final int y;

    /**
     * Constructs a new {@link ControlPoint} from the given event. The event
     * must be from a {@link MouseListener} associated with the
     * {@link LayoutCanvas} such that the {@link MouseEvent#x} and
     * {@link MouseEvent#y} fields are relative to the canvas.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The mouse event to construct the {@link ControlPoint}
     *            from.
     * @return A {@link ControlPoint} which corresponds to the given
     *         {@link MouseEvent}.
     */
    public static ControlPoint create(LayoutCanvas canvas, MouseEvent event) {
        // The mouse event coordinates should already be relative to the canvas
        // widget.
        assert event.widget == canvas : event.widget;
        return new ControlPoint(canvas, event.x, event.y);
    }

    /**
     * Constructs a new {@link ControlPoint} from the given menu detect event.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The menu detect event to construct the {@link ControlPoint} from.
     * @return A {@link ControlPoint} which corresponds to the given
     *         {@link MenuDetectEvent}.
     */
    public static ControlPoint create(LayoutCanvas canvas, MenuDetectEvent event) {
        // The menu detect events are always display-relative.
        org.eclipse.swt.graphics.Point p = canvas.toControl(event.x, event.y);
        return new ControlPoint(canvas, p.x, p.y);
    }

    /**
     * Constructs a new {@link ControlPoint} from the given event. The event
     * must be from a {@link DragSourceListener} associated with the
     * {@link LayoutCanvas} such that the {@link DragSourceEvent#x} and
     * {@link DragSourceEvent#y} fields are relative to the canvas.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The mouse event to construct the {@link ControlPoint}
     *            from.
     * @return A {@link ControlPoint} which corresponds to the given
     *         {@link DragSourceEvent}.
     */
    public static ControlPoint create(LayoutCanvas canvas, DragSourceEvent event) {
        // The drag source event coordinates should already be relative to the
        // canvas widget.
        return new ControlPoint(canvas, event.x, event.y);
    }

    /**
     * Constructs a new {@link ControlPoint} from the given event.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param event The mouse event to construct the {@link ControlPoint}
     *            from.
     * @return A {@link ControlPoint} which corresponds to the given
     *         {@link DropTargetEvent}.
     */
    public static ControlPoint create(LayoutCanvas canvas, DropTargetEvent event) {
        // The drop target events are always relative to the display, so we must
        // first convert them to be canvas relative.
        org.eclipse.swt.graphics.Point p = canvas.toControl(event.x, event.y);
        return new ControlPoint(canvas, p.x, p.y);
    }

    /**
     * Constructs a new {@link ControlPoint} from the given x,y coordinates,
     * which must be relative to the given {@link LayoutCanvas}.
     *
     * @param canvas The {@link LayoutCanvas} this point is within.
     * @param x The mouse event x coordinate relative to the canvas
     * @param y The mouse event x coordinate relative to the canvas
     * @return A {@link ControlPoint} which corresponds to the given
     *         coordinates.
     */
    public static ControlPoint create(LayoutCanvas canvas, int x, int y) {
        return new ControlPoint(canvas, x, y);
    }

    /**
     * Constructs a new canvas control coordinate with the given X and Y
     * coordinates. This is private; use one of the factory methods
     * {@link #create(LayoutCanvas, MouseEvent)},
     * {@link #create(LayoutCanvas, DragSourceEvent)} or
     * {@link #create(LayoutCanvas, DropTargetEvent)} instead.
     *
     * @param canvas The canvas which contains this coordinate
     * @param x The mouse x coordinate
     * @param y The mouse y coordinate
     */
    private ControlPoint(LayoutCanvas canvas, int x, int y) {
        mCanvas = canvas;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the equivalent {@link LayoutPoint} to this
     * {@link ControlPoint}.
     *
     * @return The equivalent {@link LayoutPoint} to this
     *         {@link ControlPoint}.
     */
    public LayoutPoint toLayout() {
        int lx = mCanvas.getHorizontalTransform().inverseTranslate(x);
        int ly = mCanvas.getVerticalTransform().inverseTranslate(y);

        return LayoutPoint.create(mCanvas, lx, ly);
    }

    @Override
    public String toString() {
        return "ControlPoint [x=" + x + ", y=" + y + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
        ControlPoint other = (ControlPoint) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (mCanvas != other.mCanvas) {
            return false;
        }
        return true;
    }

    /**
     * Returns this point as an SWT point in the display coordinate system
     *
     * @return this point as an SWT point in the display coordinate system
     */
    public Point toDisplayPoint() {
        return mCanvas.toDisplay(x, y);
    }
}
