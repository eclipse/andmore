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

import com.android.utils.Pair;

import org.eclipse.swt.events.KeyEvent;

import java.util.Collections;
import java.util.List;

/**
 * A gesture is a mouse or keyboard driven user operation, such as a
 * swipe-select or a resize. It can be thought of as a session, since it is
 * initiated, updated during user manipulation, and finally completed or
 * canceled. A gesture is associated with a single undo transaction (although
 * some gestures don't actually edit anything, such as a selection), and a
 * gesture can have a number of graphics {@link Overlay}s which are added and
 * cleaned up on behalf of the gesture by the system.
 * <p/>
 * Gestures are typically mouse oriented. If a mouse wishes to integrate
 * with the native drag &amp; drop support, it should also implement
 * the {@link DropGesture} interface, which is a sub interface of this
 * {@link Gesture} interface. There are pros and cons to using native drag
 * &amp; drop, so various gestures will differ in whether they use it.
 * In particular, you should use drag &amp; drop if your gesture should:
 * <ul>
 * <li> Show a native drag &amp; drop cursor
 * <li> Copy or move data, especially if this applies outside the canvas
 *    control window or even the application itself
 * </ul>
 * You might want to avoid using native drag &amp; drop if your gesture should:
 * <ul>
 * <li> Continue updating itself even when the mouse cursor leaves the
 *    canvas window (in a drag &amp; gesture, as soon as you leave the canvas
 *    the drag source is no longer informed of mouse updates, whereas a regular
 *    mouse listener is)
 * <li> Respond to modifier keys (for example, if toggling the Shift key
 *    should constrain motion as is common during resizing, and so on)
 * <li> Use no special cursor (for example, during a marquee selection gesture we
 *     don't want a native drag &amp; drop cursor)
 *  </ul>
 * <p/>
 * Examples of gestures:
 * <ul>
 * <li>Move (dragging to reorder or change hierarchy of views or change visual
 * layout attributes)
 * <li>Marquee (swiping out a rectangle to make a selection)
 * <li>Resize (dragging some edge or corner of a widget to change its size, for
 * example to some new fixed size, or to "attach" it to some other edge.)
 * <li>Inline Editing (editing the text of some text-oriented widget like a
 * label or a button)
 * <li>Link (associate two or more widgets in some way, such as an
 *   "is required" widget linked to a text field)
 * </ul>
 */
public abstract class Gesture {
    /** Start mouse coordinate, in control coordinates. */
    protected ControlPoint mStart;

    /** Initial SWT mask when the gesture started. */
    protected int mStartMask;

    /**
     * Returns a list of overlays, from bottom to top (where the later overlays
     * are painted on top of earlier ones if they overlap).
     *
     * @return A list of overlays to paint for this gesture, if applicable.
     *         Should not be null, but can be empty.
     */
    public List<Overlay> createOverlays() {
        return Collections.emptyList();
    }

    /**
     * Handles initialization of this gesture. Called when the gesture is
     * starting.
     *
     * @param pos The most recent mouse coordinate applicable to this
     *            gesture, relative to the canvas control.
     * @param startMask The initial SWT mask for the gesture, if known, or
     *            otherwise 0.
     */
    public void begin(ControlPoint pos, int startMask) {
        mStart = pos;
        mStartMask = startMask;
    }

    /**
     * Handles updating of the gesture state for a new mouse position.
     *
     * @param pos The most recent mouse coordinate applicable to this
     *            gesture, relative to the canvas control.
     */
    public void update(ControlPoint pos) {
    }

    /**
     * Handles termination of the gesture. This method is called when the
     * gesture has terminated (either through successful completion, or because
     * it was canceled).
     *
     * @param pos The most recent mouse coordinate applicable to this
     *            gesture, relative to the canvas control.
     * @param canceled True if the gesture was canceled, and false otherwise.
     */
    public void end(ControlPoint pos, boolean canceled) {
    }

    /**
     * Handles a key press during the gesture. May be called repeatedly when the
     * user is holding the key for several seconds.
     *
     * @param event The SWT event for the key press,
     * @return true if this gesture consumed the key press, otherwise return false
     */
    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    /**
     * Handles a key release during the gesture.
     *
     * @param event The SWT event for the key release,
     * @return true if this gesture consumed the key press, otherwise return false
     */
    public boolean keyReleased(KeyEvent event) {
        return false;
    }

    /**
     * Returns whether tooltips should be display below and to the right of the mouse
     * cursor.
     *
     * @return a pair of booleans, the first indicating whether the tooltip should be
     *         below and the second indicating whether the tooltip should be displayed to
     *         the right of the mouse cursor.
     */
    public Pair<Boolean, Boolean> getTooltipPosition() {
        return Pair.of(true, true);
    }
}
