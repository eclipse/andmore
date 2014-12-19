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
package org.eclipse.andmore.common.layout.relative;

import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;

import com.android.annotations.NonNull;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.SegmentType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.common.layout.relative.DependencyGraph.Constraint;

/**
 * The {@link GuidelinePainter} is responsible for painting guidelines during an operation
 * which uses a {@link GuidelineHandler} such as a resize operation.
 */
public final class GuidelinePainter implements IFeedbackPainter {
    // ---- Implements IFeedbackPainter ----
    @Override
    public void paint(@NonNull IGraphics gc, @NonNull INode node, @NonNull DropFeedback feedback) {
        GuidelineHandler state = (GuidelineHandler) feedback.userData;

        for (INode dragged : state.mDraggedNodes) {
            gc.useStyle(DrawingStyle.DRAGGED);
            Rect bounds = dragged.getBounds();
            if (bounds.isValid()) {
                gc.fillRect(bounds);
            }
        }

        Set<INode> horizontalDeps = state.mHorizontalDeps;
        Set<INode> verticalDeps = state.mVerticalDeps;
        Set<INode> deps = new HashSet<INode>(horizontalDeps.size() + verticalDeps.size());
        deps.addAll(horizontalDeps);
        deps.addAll(verticalDeps);
        if (deps.size() > 0) {
            gc.useStyle(DrawingStyle.DEPENDENCY);
            for (INode n : deps) {
                // Don't highlight the selected nodes themselves
                if (state.mDraggedNodes.contains(n)) {
                    continue;
                }
                Rect bounds = n.getBounds();
                gc.fillRect(bounds);
            }
        }

        if (state.mBounds != null) {
            if (state instanceof MoveHandler) {
                gc.useStyle(DrawingStyle.DROP_PREVIEW);
            } else {
                // Resizing
                if (state.haveSuggestions()) {
                    gc.useStyle(DrawingStyle.RESIZE_PREVIEW);
                } else {
                    gc.useStyle(DrawingStyle.RESIZE_FAIL);
                }
            }
            gc.drawRect(state.mBounds);

            // Draw baseline preview too
            if (feedback.dragBaseline != -1) {
                int y = state.mBounds.y + feedback.dragBaseline;
                gc.drawLine(state.mBounds.x, y, state.mBounds.x2(), y);
            }
        }

        List<String> strings = new ArrayList<String>();

        showMatch(gc, state.mCurrentLeftMatch, state, strings,
                state.mLeftMargin, ATTR_LAYOUT_MARGIN_LEFT);
        showMatch(gc, state.mCurrentRightMatch, state, strings,
                state.mRightMargin, ATTR_LAYOUT_MARGIN_RIGHT);
        showMatch(gc, state.mCurrentTopMatch, state, strings,
                state.mTopMargin, ATTR_LAYOUT_MARGIN_TOP);
        showMatch(gc, state.mCurrentBottomMatch, state, strings,
                state.mBottomMargin, ATTR_LAYOUT_MARGIN_BOTTOM);

        if (strings.size() > 0) {
            // Update the drag tooltip
            StringBuilder sb = new StringBuilder(200);
            for (String s : strings) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(s);
            }
            feedback.tooltip = sb.toString();

            // Set the tooltip orientation to ensure that it does not interfere with
            // the constraint arrows
            if (state.mCurrentLeftMatch != null) {
                feedback.tooltipX = SegmentType.RIGHT;
            } else if (state.mCurrentRightMatch != null) {
                feedback.tooltipX = SegmentType.LEFT;
            }
            if (state.mCurrentTopMatch != null) {
                feedback.tooltipY = SegmentType.BOTTOM;
            } else if (state.mCurrentBottomMatch != null) {
                feedback.tooltipY = SegmentType.TOP;
            }
        } else {
            feedback.tooltip = null;
        }

        if (state.mHorizontalCycle != null) {
            paintCycle(gc, state, state.mHorizontalCycle);
        }
        if (state.mVerticalCycle != null) {
            paintCycle(gc, state, state.mVerticalCycle);
        }
    }

    /** Paints a particular match constraint */
    private void showMatch(IGraphics gc, Match m, GuidelineHandler state, List<String> strings,
            int margin, String marginAttribute) {
        if (m == null) {
            return;
        }
        ConstraintPainter.paintConstraint(gc, state.mBounds, m);

        // Display the constraint. Remove the @id/ and @+id/ prefixes to make the text
        // shorter and easier to read. This doesn't use stripPrefix() because the id is
        // usually not a prefix of the value (for example, 'layout_alignBottom=@+id/foo').
        String constraint = m.getConstraint(false /* generateId */);
        String description = constraint.replace(NEW_ID_PREFIX, "").replace(ID_PREFIX, "");
        if (description.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
            description = description.substring(ATTR_LAYOUT_RESOURCE_PREFIX.length());
        }
        if (margin > 0) {
            int dp = state.getRulesEngine().pxToDp(margin);
            description = String.format("%1$s, margin=%2$d dp", description, dp);
        }
        strings.add(description);
    }

    /** Paints a constraint cycle */
    void paintCycle(IGraphics gc, GuidelineHandler state, List<Constraint> cycle) {
        gc.useStyle(DrawingStyle.CYCLE);
        assert cycle.size() > 0;

        INode from = cycle.get(0).from.node;
        Rect fromBounds = from.getBounds();
        if (state.mDraggedNodes.contains(from)) {
            fromBounds = state.mBounds;
        }
        Point fromCenter = fromBounds.center();
        INode to = null;

        List<Point> points = new ArrayList<Point>();
        points.add(fromCenter);

        for (Constraint constraint : cycle) {
            assert constraint.from.node == from;
            to = constraint.to.node;
            assert from != null && to != null;

            Point toCenter = to.getBounds().center();
            points.add(toCenter);

            // Also go through the dragged node bounds
            boolean isDragged = state.mDraggedNodes.contains(to);
            if (isDragged) {
                toCenter = state.mBounds.center();
                points.add(toCenter);
            }

            from = to;
            fromCenter = toCenter;
        }

        points.add(fromCenter);
        points.add(points.get(0));

        for (int i = 1, n = points.size(); i < n; i++) {
            gc.drawLine(points.get(i-1), points.get(i));
        }
    }
}
