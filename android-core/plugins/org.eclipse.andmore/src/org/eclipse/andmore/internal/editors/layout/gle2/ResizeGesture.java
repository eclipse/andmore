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

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.ResizePolicy;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.layout.gle2.SelectionHandle.Position;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;

import java.util.Collections;
import java.util.List;

/**
 * A {@link ResizeGesture} is a gesture for resizing a selected widget. It is initiated
 * by a drag of a {@link SelectionHandle}.
 */
public class ResizeGesture extends Gesture {
    /** The {@link Overlay} drawn for the gesture feedback. */
    private ResizeOverlay mOverlay;

    /** The canvas associated with this gesture. */
    private LayoutCanvas mCanvas;

    /** The selection handle we're dragging to perform this resize */
    private SelectionHandle mHandle;

    private NodeProxy mParentNode;
    private NodeProxy mChildNode;
    private DropFeedback mFeedback;
    private ResizePolicy mResizePolicy;
    private SegmentType mHorizontalEdge;
    private SegmentType mVerticalEdge;

    /**
     * Creates a new marquee selection (selection swiping).
     *
     * @param canvas The canvas where selection is performed.
     * @param item The selected item the handle corresponds to
     * @param handle The handle being dragged to perform the resize
     */
    public ResizeGesture(LayoutCanvas canvas, SelectionItem item, SelectionHandle handle) {
        mCanvas = canvas;
        mHandle = handle;

        mChildNode = item.getNode();
        mParentNode = (NodeProxy) mChildNode.getParent();
        mResizePolicy = item.getResizePolicy();
        mHorizontalEdge = getHorizontalEdgeType(mHandle);
        mVerticalEdge = getVerticalEdgeType(mHandle);
    }

    @Override
    public void begin(ControlPoint pos, int startMask) {
        super.begin(pos, startMask);

        mCanvas.getSelectionOverlay().setHidden(true);

        RulesEngine rulesEngine = mCanvas.getRulesEngine();
        Rect newBounds = getNewBounds(pos);
        ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
        CanvasViewInfo childInfo = viewHierarchy.findViewInfoFor(mChildNode);
        CanvasViewInfo parentInfo = viewHierarchy.findViewInfoFor(mParentNode);
        Object childView = childInfo != null ? childInfo.getViewObject() : null;
        Object parentView = parentInfo != null ? parentInfo.getViewObject() : null;
        mFeedback = rulesEngine.callOnResizeBegin(mChildNode, mParentNode, newBounds,
                mHorizontalEdge, mVerticalEdge, childView, parentView);
        update(pos);
        mCanvas.getGestureManager().updateMessage(mFeedback);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        update(mCanvas.getGestureManager().getCurrentControlPoint());
        mCanvas.redraw();
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        update(mCanvas.getGestureManager().getCurrentControlPoint());
        mCanvas.redraw();
        return true;
    }

    @Override
    public void update(ControlPoint pos) {
        super.update(pos);
        RulesEngine rulesEngine = mCanvas.getRulesEngine();
        Rect newBounds = getNewBounds(pos);
        int modifierMask = mCanvas.getGestureManager().getRuleModifierMask();
        rulesEngine.callOnResizeUpdate(mFeedback, mChildNode, mParentNode, newBounds,
                modifierMask);
        mCanvas.getGestureManager().updateMessage(mFeedback);
    }

    @Override
    public void end(ControlPoint pos, boolean canceled) {
        super.end(pos, canceled);

        if (!canceled) {
            RulesEngine rulesEngine = mCanvas.getRulesEngine();
            Rect newBounds = getNewBounds(pos);
            rulesEngine.callOnResizeEnd(mFeedback, mChildNode, mParentNode, newBounds);
        }

        mCanvas.getSelectionOverlay().setHidden(false);
    }

    @Override
    public Pair<Boolean, Boolean> getTooltipPosition() {
        return Pair.of(mHorizontalEdge != SegmentType.TOP, mVerticalEdge != SegmentType.LEFT);
    }

    /**
     * For the new mouse position, compute the resized bounds (the bounding rectangle that
     * the view should be resized to). This is not just a width or height, since in some
     * cases resizing will change the x/y position of the view as well (for example, in
     * RelativeLayout or in AbsoluteLayout).
     */
    private Rect getNewBounds(ControlPoint pos) {
        LayoutPoint p = pos.toLayout();
        LayoutPoint start = mStart.toLayout();
        Rect b = mChildNode.getBounds();
        Position direction = mHandle.getPosition();

        int x = b.x;
        int y = b.y;
        int w = b.w;
        int h = b.h;
        int deltaX = p.x - start.x;
        int deltaY = p.y - start.y;

        if (deltaX == 0 && deltaY == 0) {
            // No move - just use the existing bounds
            return b;
        }

        if (mResizePolicy.isAspectPreserving() && w != 0 && h != 0) {
            double aspectRatio = w / (double) h;
            int newW = Math.abs(b.w + (direction.isLeft() ? -deltaX : deltaX));
            int newH = Math.abs(b.h + (direction.isTop() ? -deltaY : deltaY));
            double newAspectRatio = newW / (double) newH;
            if (newH == 0 || newAspectRatio > aspectRatio) {
                deltaY = (int) (deltaX / aspectRatio);
            } else {
                deltaX = (int) (deltaY * aspectRatio);
            }
        }
        if (direction.isLeft()) {
            // The user is dragging the left edge, so the position is anchored on the
            // right.
            int x2 = b.x + b.w;
            int nx1 = b.x + deltaX;
            if (nx1 <= x2) {
                x = nx1;
                w = x2 - x;
            } else {
                w = 0;
                x = x2;
            }
        } else if (direction.isRight()) {
            // The user is dragging the right edge, so the position is anchored on the
            // left.
            int nx2 = b.x + b.w + deltaX;
            if (nx2 >= b.x) {
                w = nx2 - b.x;
            } else {
                w = 0;
            }
        } else {
            assert direction == Position.BOTTOM_MIDDLE || direction == Position.TOP_MIDDLE;
        }

        if (direction.isTop()) {
            // The user is dragging the top edge, so the position is anchored on the
            // bottom.
            int y2 = b.y + b.h;
            int ny1 = b.y + deltaY;
            if (ny1 < y2) {
                y = ny1;
                h = y2 - y;
            } else {
                h = 0;
                y = y2;
            }
        } else if (direction.isBottom()) {
            // The user is dragging the bottom edge, so the position is anchored on the
            // top.
            int ny2 = b.y + b.h + deltaY;
            if (ny2 >= b.y) {
                h = ny2 - b.y;
            } else {
                h = 0;
            }
        } else {
            assert direction == Position.LEFT_MIDDLE || direction == Position.RIGHT_MIDDLE;
        }

        return new Rect(x, y, w, h);
    }

    private static SegmentType getHorizontalEdgeType(SelectionHandle handle) {
        switch (handle.getPosition()) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
            case BOTTOM_MIDDLE:
                return SegmentType.BOTTOM;
            case LEFT_MIDDLE:
            case RIGHT_MIDDLE:
                return null;
            case TOP_LEFT:
            case TOP_MIDDLE:
            case TOP_RIGHT:
                return SegmentType.TOP;
            default: assert false : handle.getPosition();
        }
        return null;
    }

    private static SegmentType getVerticalEdgeType(SelectionHandle handle) {
        switch (handle.getPosition()) {
            case TOP_LEFT:
            case LEFT_MIDDLE:
            case BOTTOM_LEFT:
                return SegmentType.LEFT;
            case BOTTOM_MIDDLE:
            case TOP_MIDDLE:
                return null;
            case TOP_RIGHT:
            case RIGHT_MIDDLE:
            case BOTTOM_RIGHT:
                return SegmentType.RIGHT;
            default: assert false : handle.getPosition();
        }
        return null;
    }


    @Override
    public List<Overlay> createOverlays() {
        mOverlay = new ResizeOverlay();
        return Collections.<Overlay> singletonList(mOverlay);
    }

    /**
     * An {@link Overlay} to paint the resize feedback. This just delegates to the
     * layout rule for the parent which is handling the resizing.
     */
    private class ResizeOverlay extends Overlay {
        @Override
        public void paint(GC gc) {
            if (mChildNode != null && mFeedback != null) {
                RulesEngine rulesEngine = mCanvas.getRulesEngine();
                rulesEngine.callDropFeedbackPaint(mCanvas.getGcWrapper(), mChildNode, mFeedback);
            }
        }
    }
}
