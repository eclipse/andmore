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

import static com.android.ide.common.api.DrawingStyle.DEPENDENCY;
import static com.android.ide.common.api.DrawingStyle.GUIDELINE;
import static com.android.ide.common.api.DrawingStyle.GUIDELINE_DASHED;
import static com.android.ide.common.api.SegmentType.BASELINE;
import static com.android.ide.common.api.SegmentType.BOTTOM;
import static com.android.ide.common.api.SegmentType.CENTER_HORIZONTAL;
import static com.android.ide.common.api.SegmentType.CENTER_VERTICAL;
import static com.android.ide.common.api.SegmentType.LEFT;
import static com.android.ide.common.api.SegmentType.RIGHT;
import static com.android.ide.common.api.SegmentType.TOP;
import static com.android.ide.common.api.SegmentType.UNKNOWN;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.ALIGN_BASELINE;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.ALIGN_BOTTOM;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_ABOVE;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_BELOW;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_LEFT_OF;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_RIGHT_OF;

import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.SegmentType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.common.layout.relative.DependencyGraph.Constraint;
import org.eclipse.andmore.common.layout.relative.DependencyGraph.ViewData;

/**
 * The {@link ConstraintPainter} is responsible for painting relative layout constraints -
 * such as a source node having its top edge constrained to a target node with a given margin.
 * This painter is used both to show static constraints, as well as visualizing proposed
 * constraints during a move or resize operation.
 */
public class ConstraintPainter {
    /** The size of the arrow head */
    private static final int ARROW_SIZE = 5;
    /** Size (height for horizontal, and width for vertical) parent feedback rectangles */
    private static final int PARENT_RECT_SIZE = 12;

    /**
     * Paints a given match as a constraint.
     *
     * @param graphics the graphics context
     * @param sourceBounds the source bounds
     * @param match the match
     */
    static void paintConstraint(IGraphics graphics, Rect sourceBounds, Match match) {
        Rect targetBounds = match.edge.node.getBounds();
        ConstraintType type = match.type;
        assert type != null;
        paintConstraint(graphics, type, match.with.node, sourceBounds, match.edge.node,
                targetBounds, null /* allConstraints */, true /* highlightTargetEdge */);
    }

    /**
     * Paints a constraint.
     * <p>
     * TODO: when there are multiple links originating in the same direction from
     * center, maybe offset them slightly from each other?
     *
     * @param graphics the graphics context to draw into
     * @param constraint The constraint to be drawn
     */
    private static void paintConstraint(IGraphics graphics, Constraint constraint,
            Set<Constraint> allConstraints) {
        ViewData source = constraint.from;
        ViewData target = constraint.to;

        INode sourceNode = source.node;
        INode targetNode = target.node;
        if (sourceNode == targetNode) {
            // Self reference - don't visualize
            return;
        }

        Rect sourceBounds = sourceNode.getBounds();
        Rect targetBounds = targetNode.getBounds();
        paintConstraint(graphics, constraint.type, sourceNode, sourceBounds, targetNode,
                targetBounds, allConstraints, false /* highlightTargetEdge */);
    }

    /**
     * Paint selection feedback by painting constraints for the selected nodes
     *
     * @param graphics the graphics context
     * @param parentNode the parent relative layout
     * @param childNodes the nodes whose constraints should be painted
     * @param showDependents whether incoming constraints should be shown as well
     */
    public static void paintSelectionFeedback(IGraphics graphics, INode parentNode,
            List<? extends INode> childNodes, boolean showDependents) {

        DependencyGraph dependencyGraph = new DependencyGraph(parentNode);
        Set<INode> horizontalDeps = dependencyGraph.dependsOn(childNodes, false /* vertical */);
        Set<INode> verticalDeps = dependencyGraph.dependsOn(childNodes, true /* vertical */);
        Set<INode> deps = new HashSet<INode>(horizontalDeps.size() + verticalDeps.size());
        deps.addAll(horizontalDeps);
        deps.addAll(verticalDeps);
        if (deps.size() > 0) {
            graphics.useStyle(DEPENDENCY);
            for (INode node : deps) {
                // Don't highlight the selected nodes themselves
                if (childNodes.contains(node)) {
                    continue;
                }
                Rect bounds = node.getBounds();
                graphics.fillRect(bounds);
            }
        }

        graphics.useStyle(GUIDELINE);
        for (INode childNode : childNodes) {
            ViewData view = dependencyGraph.getView(childNode);
            if (view == null) {
                continue;
            }

            // Paint all incoming constraints
            if (showDependents) {
                paintConstraints(graphics, view.dependedOnBy);
            }

            // Paint all outgoing constraints
            paintConstraints(graphics, view.dependsOn);
        }
    }

    /**
     * Paints a set of constraints.
     */
    private static void paintConstraints(IGraphics graphics, List<Constraint> constraints) {
        Set<Constraint> mutableConstraintSet = new HashSet<Constraint>(constraints);

        // WORKAROUND! Hide alignBottom attachments if we also have a alignBaseline
        // constraint; this is because we also *add* alignBottom attachments when you add
        // alignBaseline constraints to work around a surprising behavior of baseline
        // constraints.
        for (Constraint constraint : constraints) {
            if (constraint.type == ALIGN_BASELINE) {
                // Remove any baseline
                for (Constraint c : constraints) {
                    if (c.type == ALIGN_BOTTOM && c.to.node == constraint.to.node) {
                        mutableConstraintSet.remove(c);
                    }
                }
            }
        }

        for (Constraint constraint : constraints) {
            // paintConstraint can digest more than one constraint, so we need to keep
            // checking to see if the given constraint is still relevant.
            if (mutableConstraintSet.contains(constraint)) {
                paintConstraint(graphics, constraint, mutableConstraintSet);
            }
        }
    }

    /**
     * Paints a constraint of the given type from the given source node, to the
     * given target node, with the specified bounds.
     */
    private static void paintConstraint(IGraphics graphics, ConstraintType type, INode sourceNode,
            Rect sourceBounds, INode targetNode, Rect targetBounds,
            Set<Constraint> allConstraints, boolean highlightTargetEdge) {

        SegmentType sourceSegmentTypeX = type.sourceSegmentTypeX;
        SegmentType sourceSegmentTypeY = type.sourceSegmentTypeY;
        SegmentType targetSegmentTypeX = type.targetSegmentTypeX;
        SegmentType targetSegmentTypeY = type.targetSegmentTypeY;

        // Horizontal center constraint?
        if (sourceSegmentTypeX == CENTER_VERTICAL && targetSegmentTypeX == CENTER_VERTICAL) {
            paintHorizontalCenterConstraint(graphics, sourceBounds, targetBounds);
            return;
        }

        // Vertical center constraint?
        if (sourceSegmentTypeY == CENTER_HORIZONTAL && targetSegmentTypeY == CENTER_HORIZONTAL) {
            paintVerticalCenterConstraint(graphics, sourceBounds, targetBounds);
            return;
        }

        // Corner constraint?
        if (allConstraints != null
                && (type == LAYOUT_ABOVE || type == LAYOUT_BELOW
                        || type == LAYOUT_LEFT_OF || type == LAYOUT_RIGHT_OF)) {
            if (paintCornerConstraint(graphics, type, sourceNode, sourceBounds, targetNode,
                    targetBounds, allConstraints)) {
                return;
            }
        }

        // Vertical constraint?
        if (sourceSegmentTypeX == UNKNOWN) {
            paintVerticalConstraint(graphics, type, sourceNode, sourceBounds, targetNode,
                    targetBounds, highlightTargetEdge);
            return;
        }

        // Horizontal constraint?
        if (sourceSegmentTypeY == UNKNOWN) {
            paintHorizontalConstraint(graphics, type, sourceNode, sourceBounds, targetNode,
                    targetBounds, highlightTargetEdge);
            return;
        }

        // This shouldn't happen - it means we have a constraint that defines all sides
        // and is not a centering constraint
        assert false;
    }

    /**
     * Paints a corner constraint, or returns false if this constraint is not a corner.
     * A corner is one where there are two constraints from this source node to the
     * same target node, one horizontal and one vertical, to the closest edges on
     * the target node.
     * <p>
     * Corners are a common occurrence. If we treat the horizontal and vertical
     * constraints separately (below & toRightOf), then we end up with a lot of
     * extra lines and arrows -- e.g. two shared edges and arrows pointing to these
     * shared edges:
     *
     * <pre>
     *  +--------+ |
     *  | Target -->
     *  +----|---+ |
     *       v
     *  - - - - - -|- - - - - -
     *                   ^
     *             | +---|----+
     *             <-- Source |
     *             | +--------+
     *
     * Instead, we can simply draw a diagonal arrow here to represent BOTH constraints and
     * reduce clutter:
     *
     *  +---------+
     *  | Target _|
     *  +-------|\+
     *            \
     *             \--------+
     *             | Source |
     *             +--------+
     * </pre>
     *
     * @param graphics the graphics context to draw
     * @param type the constraint to be drawn
     * @param sourceNode the source node
     * @param sourceBounds the bounds of the source node
     * @param targetNode the target node
     * @param targetBounds the bounds of the target node
     * @param allConstraints the set of all constraints; if a corner is found and painted the
     *    matching corner constraint is removed from the set
     * @return true if the constraint was handled and painted as a corner, false otherwise
     */
    private static boolean paintCornerConstraint(IGraphics graphics, ConstraintType type,
            INode sourceNode, Rect sourceBounds, INode targetNode, Rect targetBounds,
            Set<Constraint> allConstraints) {

        SegmentType sourceSegmentTypeX = type.sourceSegmentTypeX;
        SegmentType sourceSegmentTypeY = type.sourceSegmentTypeY;
        SegmentType targetSegmentTypeX = type.targetSegmentTypeX;
        SegmentType targetSegmentTypeY = type.targetSegmentTypeY;

        ConstraintType opposite1 = null, opposite2 = null;
        switch (type) {
            case LAYOUT_BELOW:
            case LAYOUT_ABOVE:
                opposite1 = LAYOUT_LEFT_OF;
                opposite2 = LAYOUT_RIGHT_OF;
                break;
            case LAYOUT_LEFT_OF:
            case LAYOUT_RIGHT_OF:
                opposite1 = LAYOUT_ABOVE;
                opposite2 = LAYOUT_BELOW;
                break;
            default:
                return false;
        }
        Constraint pair = null;
        for (Constraint constraint : allConstraints) {
            if ((constraint.type == opposite1 || constraint.type == opposite2) &&
                    constraint.to.node == targetNode && constraint.from.node == sourceNode) {
                pair = constraint;
                break;
            }
        }

        // TODO -- ensure that the nodes are adjacent! In other words, that
        // their bounds are within N pixels.

        if (pair != null) {
            // Visualize the corner constraint
            if (sourceSegmentTypeX == UNKNOWN) {
                sourceSegmentTypeX = pair.type.sourceSegmentTypeX;
            }
            if (sourceSegmentTypeY == UNKNOWN) {
                sourceSegmentTypeY = pair.type.sourceSegmentTypeY;
            }
            if (targetSegmentTypeX == UNKNOWN) {
                targetSegmentTypeX = pair.type.targetSegmentTypeX;
            }
            if (targetSegmentTypeY == UNKNOWN) {
                targetSegmentTypeY = pair.type.targetSegmentTypeY;
            }

            int x1, y1, x2, y2;
            if (sourceSegmentTypeX == LEFT) {
                x1 = sourceBounds.x + 1 * sourceBounds.w / 4;
            } else {
                x1 = sourceBounds.x + 3 * sourceBounds.w / 4;
            }
            if (sourceSegmentTypeY == TOP) {
                y1 = sourceBounds.y + 1 * sourceBounds.h / 4;
            } else {
                y1 = sourceBounds.y + 3 * sourceBounds.h / 4;
            }
            if (targetSegmentTypeX == LEFT) {
                x2 = targetBounds.x + 1 * targetBounds.w / 4;
            } else {
                x2 = targetBounds.x + 3 * targetBounds.w / 4;
            }
            if (targetSegmentTypeY == TOP) {
                y2 = targetBounds.y + 1 * targetBounds.h / 4;
            } else {
                y2 = targetBounds.y + 3 * targetBounds.h / 4;
            }

            graphics.useStyle(GUIDELINE);
            graphics.drawArrow(x1, y1, x2, y2, ARROW_SIZE);

            // Don't process this constraint on its own later.
            allConstraints.remove(pair);

            return true;
        }

        return false;
    }

    /**
     * Paints a vertical constraint, handling the various scenarios where there are
     * margins, or where the two nodes overlap horizontally and where they don't, etc.
     * <p>
     * Here's an example of what will be shown for a "below" constraint where the
     * nodes do not overlap horizontally and the target node has a bottom margin:
     * <pre>
     *  +--------+
     *  | Target |
     *  +--------+
     *       |
     *       v
     *   - - - - - - - - - - - - - -
     *                         ^
     *                         |
     *                    +--------+
     *                    | Source |
     *                    +--------+
     * </pre>
     */
    private static void paintVerticalConstraint(IGraphics graphics, ConstraintType type,
            INode sourceNode, Rect sourceBounds, INode targetNode, Rect targetBounds,
            boolean highlightTargetEdge) {
        SegmentType sourceSegmentTypeY = type.sourceSegmentTypeY;
        SegmentType targetSegmentTypeY = type.targetSegmentTypeY;
        Margins targetMargins = targetNode.getMargins();

        assert sourceSegmentTypeY != UNKNOWN;
        assert targetBounds != null;

        int sourceY = sourceSegmentTypeY.getY(sourceNode, sourceBounds);
        int targetY = targetSegmentTypeY ==
            UNKNOWN ? sourceY : targetSegmentTypeY.getY(targetNode, targetBounds);

        if (highlightTargetEdge && type.isRelativeToParentEdge()) {
            graphics.useStyle(DrawingStyle.DROP_ZONE_ACTIVE);
            graphics.fillRect(targetBounds.x, targetY - PARENT_RECT_SIZE / 2,
                    targetBounds.x2(), targetY + PARENT_RECT_SIZE / 2);
        }

        // First see if the two views overlap horizontally. If so, we can just draw a direct
        // arrow from the source up to (or down to) the target.
        //
        //  +--------+
        //  | Target |
        //  +--------+
        //         ^
        //         |
        //         |
        //       +--------+
        //       | Source |
        //       +--------+
        //
        int maxLeft = Math.max(sourceBounds.x, targetBounds.x);
        int minRight = Math.min(sourceBounds.x2(), targetBounds.x2());

        int center = (maxLeft + minRight) / 2;
        if (center > sourceBounds.x && center < sourceBounds.x2()) {
            // Yes, the lines overlap -- just draw a straight arrow
            //
            //
            // If however there is a margin on the target edge, it should be drawn like this:
            //
            //  +--------+
            //  | Target |
            //  +--------+
            //         |
            //         |
            //         v
            //   - - - - - - -
            //         ^
            //         |
            //         |
            //       +--------+
            //       | Source |
            //       +--------+
            //
            // Use a minimum threshold for this visualization since it doesn't look good
            // for small margins
            if (targetSegmentTypeY == BOTTOM && targetMargins.bottom > 5) {
                int sharedY = targetY + targetMargins.bottom;
                if (sourceY > sharedY + 2) { // Skip when source falls on the margin line
                    graphics.useStyle(GUIDELINE_DASHED);
                    graphics.drawLine(targetBounds.x, sharedY, targetBounds.x2(), sharedY);
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(center, sourceY, center, sharedY + 2, ARROW_SIZE);
                    graphics.drawArrow(center, targetY, center, sharedY - 3, ARROW_SIZE);
                } else {
                    graphics.useStyle(GUIDELINE);
                    // Draw reverse arrow to make it clear the node is as close
                    // at it can be
                    graphics.drawArrow(center, targetY, center, sourceY, ARROW_SIZE);
                }
                return;
            } else if (targetSegmentTypeY == TOP && targetMargins.top > 5) {
                int sharedY = targetY - targetMargins.top;
                if (sourceY < sharedY - 2) {
                    graphics.useStyle(GUIDELINE_DASHED);
                    graphics.drawLine(targetBounds.x, sharedY, targetBounds.x2(), sharedY);
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(center, sourceY, center, sharedY - 3, ARROW_SIZE);
                    graphics.drawArrow(center, targetY, center, sharedY + 3, ARROW_SIZE);
                } else {
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(center, targetY, center, sourceY, ARROW_SIZE);
                }
                return;
            }

            // TODO: If the center falls smack in the center of the sourceBounds,
            // AND the source node is part of the selection, then adjust the
            // center location such that it is off to the side, let's say 1/4 or 3/4 of
            // the overlap region, to ensure that it does not overlap the center selection
            // handle

            // When the constraint is for two immediately adjacent edges, we
            // need to make some adjustments to make sure the arrow points in the right
            // direction
            if (sourceY == targetY) {
                if (sourceSegmentTypeY == BOTTOM || sourceSegmentTypeY == BASELINE) {
                    sourceY -= 2 * ARROW_SIZE;
                } else if (sourceSegmentTypeY == TOP) {
                    sourceY += 2 * ARROW_SIZE;
                } else {
                    assert sourceSegmentTypeY == CENTER_HORIZONTAL : sourceSegmentTypeY;
                    sourceY += sourceBounds.h / 2 - 2 * ARROW_SIZE;
                }
            } else if (sourceSegmentTypeY == BASELINE) {
                sourceY = targetY - 2 * ARROW_SIZE;
            }

            // Center the vertical line in the overlap region
            graphics.useStyle(GUIDELINE);
            graphics.drawArrow(center, sourceY, center, targetY, ARROW_SIZE);

            return;
        }

        // If there is no horizontal overlap in the vertical constraints, then we
        // will show the attachment relative to a dashed line that extends beyond
        // the target bounds, like this:
        //
        //  +--------+
        //  | Target |
        //  +--------+ - - - - - - - - -
        //                         ^
        //                         |
        //                    +--------+
        //                    | Source |
        //                    +--------+
        //
        // However, if the target node has a vertical margin, we may need to offset
        // the line:
        //
        //  +--------+
        //  | Target |
        //  +--------+
        //       |
        //       v
        //   - - - - - - - - - - - - - -
        //                         ^
        //                         |
        //                    +--------+
        //                    | Source |
        //                    +--------+
        //
        // If not, we'll need to indicate a shared edge. This is the edge that separate
        // them (but this will require me to evaluate margins!)

        // Compute overlap region and pick the middle
        int sharedY = targetSegmentTypeY ==
            UNKNOWN ? sourceY : targetSegmentTypeY.getY(targetNode, targetBounds);
        if (type.relativeToMargin) {
            if (targetSegmentTypeY == TOP) {
                sharedY -= targetMargins.top;
            } else if (targetSegmentTypeY == BOTTOM) {
                sharedY += targetMargins.bottom;
            }
        }

        int startX;
        int endX;
        if (center <= sourceBounds.x) {
            startX = targetBounds.x + targetBounds.w / 4;
            endX = sourceBounds.x2();
        } else {
            assert (center >= sourceBounds.x2());
            startX = sourceBounds.x;
            endX = targetBounds.x + 3 * targetBounds.w / 4;
        }
        // Must draw segmented line instead
        // Place the arrow 1/4 instead of 1/2 in the source to avoid overlapping with the
        // selection handles
        graphics.useStyle(GUIDELINE_DASHED);
        graphics.drawLine(startX, sharedY, endX, sharedY);

        // Adjust position of source arrow such that it does not sit across edge; it
        // should point directly at the edge
        if (Math.abs(sharedY - sourceY) < 2 * ARROW_SIZE) {
            if (sourceSegmentTypeY == BASELINE) {
                sourceY = sharedY - 2 * ARROW_SIZE;
            } else if (sourceSegmentTypeY == TOP) {
                sharedY = sourceY;
                sourceY = sharedY + 2 * ARROW_SIZE;
            } else {
                sharedY = sourceY;
                sourceY = sharedY - 2 * ARROW_SIZE;
            }
        }

        graphics.useStyle(GUIDELINE);

        // Draw the line from the source anchor to the shared edge
        int x = sourceBounds.x + ((sourceSegmentTypeY == BASELINE) ?
                sourceBounds.w / 2 :  sourceBounds.w / 4);
        graphics.drawArrow(x, sourceY, x, sharedY, ARROW_SIZE);

        // Draw the line from the target to the horizontal shared edge
        int tx = targetBounds.centerX();
        if (targetSegmentTypeY == TOP) {
            int ty = targetBounds.y;
            int margin = targetMargins.top;
            if (margin == 0 || !type.relativeToMargin) {
                graphics.drawArrow(tx, ty + 2 * ARROW_SIZE, tx, ty, ARROW_SIZE);
            } else {
                graphics.drawArrow(tx, ty, tx, ty - margin, ARROW_SIZE);
            }
        } else if (targetSegmentTypeY == BOTTOM) {
            int ty = targetBounds.y2();
            int margin = targetMargins.bottom;
            if (margin == 0 || !type.relativeToMargin) {
                graphics.drawArrow(tx, ty - 2 * ARROW_SIZE, tx, ty, ARROW_SIZE);
            } else {
                graphics.drawArrow(tx, ty, tx, ty + margin, ARROW_SIZE);
            }
        } else {
            assert targetSegmentTypeY == BASELINE : targetSegmentTypeY;
            int ty = targetSegmentTypeY.getY(targetNode, targetBounds);
            graphics.drawArrow(tx, ty - 2 * ARROW_SIZE, tx, ty, ARROW_SIZE);
        }

        return;
    }

    /**
     * Paints a horizontal constraint, handling the various scenarios where there are margins,
     * or where the two nodes overlap horizontally and where they don't, etc.
     */
    private static void paintHorizontalConstraint(IGraphics graphics, ConstraintType type,
            INode sourceNode, Rect sourceBounds, INode targetNode, Rect targetBounds,
            boolean highlightTargetEdge) {
        SegmentType sourceSegmentTypeX = type.sourceSegmentTypeX;
        SegmentType targetSegmentTypeX = type.targetSegmentTypeX;
        Margins targetMargins = targetNode.getMargins();

        assert sourceSegmentTypeX != UNKNOWN;
        assert targetBounds != null;

        // See paintVerticalConstraint for explanations of the various cases.

        int sourceX = sourceSegmentTypeX.getX(sourceNode, sourceBounds);
        int targetX = targetSegmentTypeX == UNKNOWN ?
                sourceX : targetSegmentTypeX.getX(targetNode, targetBounds);

        if (highlightTargetEdge && type.isRelativeToParentEdge()) {
            graphics.useStyle(DrawingStyle.DROP_ZONE_ACTIVE);
            graphics.fillRect(targetX - PARENT_RECT_SIZE / 2, targetBounds.y,
                    targetX + PARENT_RECT_SIZE / 2, targetBounds.y2());
        }

        int maxTop = Math.max(sourceBounds.y, targetBounds.y);
        int minBottom = Math.min(sourceBounds.y2(), targetBounds.y2());

        // First see if the two views overlap vertically. If so, we can just draw a direct
        // arrow from the source over to the target.
        int center = (maxTop + minBottom) / 2;
        if (center > sourceBounds.y && center < sourceBounds.y2()) {
            // See if we should draw a margin line
            if (targetSegmentTypeX == RIGHT && targetMargins.right > 5) {
                int sharedX = targetX + targetMargins.right;
                if (sourceX > sharedX + 2) { // Skip when source falls on the margin line
                    graphics.useStyle(GUIDELINE_DASHED);
                    graphics.drawLine(sharedX, targetBounds.y, sharedX, targetBounds.y2());
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(sourceX, center, sharedX + 2, center, ARROW_SIZE);
                    graphics.drawArrow(targetX, center, sharedX - 3, center, ARROW_SIZE);
                } else {
                    graphics.useStyle(GUIDELINE);
                    // Draw reverse arrow to make it clear the node is as close
                    // at it can be
                    graphics.drawArrow(targetX, center, sourceX, center, ARROW_SIZE);
                }
                return;
            } else if (targetSegmentTypeX == LEFT && targetMargins.left > 5) {
                int sharedX = targetX - targetMargins.left;
                if (sourceX < sharedX - 2) {
                    graphics.useStyle(GUIDELINE_DASHED);
                    graphics.drawLine(sharedX, targetBounds.y, sharedX, targetBounds.y2());
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(sourceX, center, sharedX - 3, center, ARROW_SIZE);
                    graphics.drawArrow(targetX, center, sharedX + 3, center, ARROW_SIZE);
                } else {
                    graphics.useStyle(GUIDELINE);
                    graphics.drawArrow(targetX, center, sourceX, center, ARROW_SIZE);
                }
                return;
            }

            if (sourceX == targetX) {
                if (sourceSegmentTypeX == RIGHT) {
                    sourceX -= 2 * ARROW_SIZE;
                } else if (sourceSegmentTypeX == LEFT ) {
                    sourceX += 2 * ARROW_SIZE;
                } else {
                    assert sourceSegmentTypeX == CENTER_VERTICAL : sourceSegmentTypeX;
                    sourceX += sourceBounds.w / 2 - 2 * ARROW_SIZE;
                }
            }

            graphics.useStyle(GUIDELINE);
            graphics.drawArrow(sourceX, center, targetX, center, ARROW_SIZE);
            return;
        }

        // Segment line

        // Compute overlap region and pick the middle
        int sharedX = targetSegmentTypeX == UNKNOWN ?
                sourceX : targetSegmentTypeX.getX(targetNode, targetBounds);
        if (type.relativeToMargin) {
            if (targetSegmentTypeX == LEFT) {
                sharedX -= targetMargins.left;
            } else if (targetSegmentTypeX == RIGHT) {
                sharedX += targetMargins.right;
            }
        }

        int startY, endY;
        if (center <= sourceBounds.y) {
            startY = targetBounds.y + targetBounds.h / 4;
            endY = sourceBounds.y2();
        } else {
            assert (center >= sourceBounds.y2());
            startY = sourceBounds.y;
            endY = targetBounds.y + 3 * targetBounds.h / 2;
        }

        // Must draw segmented line instead
        // Place at 1/4 instead of 1/2 to avoid overlapping with selection handles
        int y = sourceBounds.y + sourceBounds.h / 4;
        graphics.useStyle(GUIDELINE_DASHED);
        graphics.drawLine(sharedX, startY, sharedX, endY);

        // Adjust position of source arrow such that it does not sit across edge; it
        // should point directly at the edge
        if (Math.abs(sharedX - sourceX) < 2 * ARROW_SIZE) {
            if (sourceSegmentTypeX == LEFT) {
                sharedX = sourceX;
                sourceX = sharedX + 2 * ARROW_SIZE;
            } else {
                sharedX = sourceX;
                sourceX = sharedX - 2 * ARROW_SIZE;
            }
        }

        graphics.useStyle(GUIDELINE);

        // Draw the line from the source anchor to the shared edge
        graphics.drawArrow(sourceX, y, sharedX, y, ARROW_SIZE);

        // Draw the line from the target to the horizontal shared edge
        int ty = targetBounds.centerY();
        if (targetSegmentTypeX == LEFT) {
            int tx = targetBounds.x;
            int margin = targetMargins.left;
            if (margin == 0 || !type.relativeToMargin) {
                graphics.drawArrow(tx + 2 * ARROW_SIZE, ty, tx, ty, ARROW_SIZE);
            } else {
                graphics.drawArrow(tx, ty, tx - margin, ty, ARROW_SIZE);
            }
        } else {
            assert targetSegmentTypeX == RIGHT;
            int tx = targetBounds.x2();
            int margin = targetMargins.right;
            if (margin == 0 || !type.relativeToMargin) {
                graphics.drawArrow(tx - 2 * ARROW_SIZE, ty, tx, ty, ARROW_SIZE);
            } else {
                graphics.drawArrow(tx, ty, tx + margin, ty, ARROW_SIZE);
            }
        }

        return;
    }

    /**
     * Paints a vertical center constraint. The constraint is shown as a dashed line
     * through the vertical view, and a solid line over the node bounds.
     */
    private static void paintVerticalCenterConstraint(IGraphics graphics, Rect sourceBounds,
            Rect targetBounds) {
        graphics.useStyle(GUIDELINE_DASHED);
        graphics.drawLine(targetBounds.x, targetBounds.centerY(),
                targetBounds.x2(), targetBounds.centerY());
        graphics.useStyle(GUIDELINE);
        graphics.drawLine(sourceBounds.x, sourceBounds.centerY(),
                sourceBounds.x2(), sourceBounds.centerY());
    }

    /**
     * Paints a horizontal center constraint. The constraint is shown as a dashed line
     * through the horizontal view, and a solid line over the node bounds.
     */
    private static void paintHorizontalCenterConstraint(IGraphics graphics, Rect sourceBounds,
            Rect targetBounds) {
        graphics.useStyle(GUIDELINE_DASHED);
        graphics.drawLine(targetBounds.centerX(), targetBounds.y,
                targetBounds.centerX(), targetBounds.y2());
        graphics.useStyle(GUIDELINE);
        graphics.drawLine(sourceBounds.centerX(), sourceBounds.y,
                sourceBounds.centerX(), sourceBounds.y2());
    }
}