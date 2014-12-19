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

import static com.android.ide.common.api.MarginType.NO_MARGIN;
import static com.android.ide.common.api.MarginType.WITHOUT_MARGIN;
import static com.android.ide.common.api.MarginType.WITH_MARGIN;
import static com.android.ide.common.api.SegmentType.BASELINE;
import static com.android.ide.common.api.SegmentType.BOTTOM;
import static com.android.ide.common.api.SegmentType.CENTER_HORIZONTAL;
import static com.android.ide.common.api.SegmentType.CENTER_VERTICAL;
import static com.android.ide.common.api.SegmentType.LEFT;
import static com.android.ide.common.api.SegmentType.RIGHT;
import static com.android.ide.common.api.SegmentType.TOP;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_ABOVE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_HORIZONTAL;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_IN_PARENT;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_VERTICAL;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.VALUE_N_DP;
import static com.android.SdkConstants.VALUE_TRUE;
import static java.lang.Math.abs;
import static org.eclipse.andmore.common.layout.BaseLayoutRule.getMaxMatchDistance;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.ALIGN_BASELINE;

import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.Segment;
import com.android.ide.common.api.SegmentType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.relative.DependencyGraph.Constraint;
import org.eclipse.andmore.common.layout.relative.DependencyGraph.ViewData;

/**
 * The {@link GuidelineHandler} class keeps track of state related to a guideline operation
 * like move and resize, and performs various constraint computations.
 */
public class GuidelineHandler {
    /**
     * A dependency graph for the relative layout recording constraint relationships
     */
    protected DependencyGraph mDependencyGraph;

    /** The RelativeLayout we are moving/resizing within */
    public INode layout;

    /** The set of nodes being dragged (may be null) */
    protected Collection<INode> mDraggedNodes;

    /** The bounds of the primary child node being dragged */
    protected Rect mBounds;

    /** Whether the left edge is being moved/resized */
    protected boolean mMoveLeft;

    /** Whether the right edge is being moved/resized */
    protected boolean mMoveRight;

    /** Whether the top edge is being moved/resized */
    protected boolean mMoveTop;

    /** Whether the bottom edge is being moved/resized */
    protected boolean mMoveBottom;

    /**
     * Whether the drop/move/resize position should be snapped (which can be turned off
     * with a modifier key during the operation)
     */
    protected boolean mSnap = true;

    /**
     * The set of nodes which depend on the currently selected nodes, including
     * transitively, through horizontal constraints (a "horizontal constraint"
     * is a constraint between two horizontal edges)
     */
    protected Set<INode> mHorizontalDeps;

    /**
     * The set of nodes which depend on the currently selected nodes, including
     * transitively, through vertical constraints (a "vertical constraint"
     * is a constraint between two vertical edges)
     */
    protected Set<INode> mVerticalDeps;

    /** The current list of constraints which result in a horizontal cycle (if applicable) */
    protected List<Constraint> mHorizontalCycle;

    /** The current list of constraints which result in a vertical cycle (if applicable) */
    protected List<Constraint> mVerticalCycle;

    /**
     * All horizontal segments in the relative layout - top and bottom edges, baseline
     * edges, and top and bottom edges offset by the applicable margins in each direction
     */
    protected List<Segment> mHorizontalEdges;

    /**
     * All vertical segments in the relative layout - left and right edges, and left and
     * right edges offset by the applicable margins in each direction
     */
    protected List<Segment> mVerticalEdges;

    /**
     * All center vertical segments in the relative layout. These are kept separate since
     * they only match other center edges.
     */
    protected List<Segment> mCenterVertEdges;

    /**
     * All center horizontal segments in the relative layout. These are kept separate
     * since they only match other center edges.
     */
    protected List<Segment> mCenterHorizEdges;

    /**
     * Suggestions for horizontal matches. There could be more than one, but all matches
     * will be equidistant from the current position (as well as in the same direction,
     * which means that you can't have one match 5 pixels to the left and one match 5
     * pixels to the right since it would be impossible to snap to fit with both; you can
     * however have multiple matches all 5 pixels to the left.)
     * <p
     * The best vertical match will be found in {@link #mCurrentTopMatch} or
     * {@link #mCurrentBottomMatch}.
     */
    protected List<Match> mHorizontalSuggestions;

    /**
     * Suggestions for vertical matches.
     * <p
     * The best vertical match will be found in {@link #mCurrentLeftMatch} or
     * {@link #mCurrentRightMatch}.
     */
    protected List<Match> mVerticalSuggestions;

    /**
     * The current match on the left edge, or null if no match or if the left edge is not
     * being moved or resized.
     */
    protected Match mCurrentLeftMatch;

    /**
     * The current match on the top edge, or null if no match or if the top edge is not
     * being moved or resized.
     */
    protected Match mCurrentTopMatch;

    /**
     * The current match on the right edge, or null if no match or if the right edge is
     * not being moved or resized.
     */
    protected Match mCurrentRightMatch;

    /**
     * The current match on the bottom edge, or null if no match or if the bottom edge is
     * not being moved or resized.
     */
    protected Match mCurrentBottomMatch;

    /**
     * The amount of margin to add to the top edge, or 0
     */
    protected int mTopMargin;

    /**
     * The amount of margin to add to the bottom edge, or 0
     */
    protected int mBottomMargin;

    /**
     * The amount of margin to add to the left edge, or 0
     */
    protected int mLeftMargin;

    /**
     * The amount of margin to add to the right edge, or 0
     */
    protected int mRightMargin;

    /**
     * The associated rules engine
     */
    protected IClientRulesEngine mRulesEngine;

    /**
     * Construct a new {@link GuidelineHandler} for the given relative layout.
     *
     * @param layout the RelativeLayout to handle
     */
    GuidelineHandler(INode layout, IClientRulesEngine rulesEngine) {
        this.layout = layout;
        mRulesEngine = rulesEngine;

        mHorizontalEdges = new ArrayList<Segment>();
        mVerticalEdges = new ArrayList<Segment>();
        mCenterVertEdges = new ArrayList<Segment>();
        mCenterHorizEdges = new ArrayList<Segment>();
        mDependencyGraph = new DependencyGraph(layout);
    }

    /**
     * Returns true if the handler has any suggestions to offer
     *
     * @return true if the handler has any suggestions to offer
     */
    public boolean haveSuggestions() {
        return mCurrentLeftMatch != null || mCurrentTopMatch != null
                || mCurrentRightMatch != null || mCurrentBottomMatch != null;
    }

    /**
     * Returns the closest match.
     *
     * @return the closest match, or null if nothing matched
     */
    protected Match pickBestMatch(List<Match> matches) {
        int alternatives = matches.size();
        if (alternatives == 0) {
            return null;
        } else if (alternatives == 1) {
            Match match = matches.get(0);
            return match;
        } else {
            assert alternatives > 1;
            Collections.sort(matches, new MatchComparator());
            return matches.get(0);
        }
    }

    private boolean checkCycle(DropFeedback feedback, Match match, boolean vertical) {
        if (match != null && match.cycle) {
            for (INode node : mDraggedNodes) {
                INode from = match.edge.node;
                assert match.with.node == null || match.with.node == node;
                INode to = node;
                List<Constraint> path = mDependencyGraph.getPathTo(from, to, vertical);
                if (path != null) {
                    if (vertical) {
                        mVerticalCycle = path;
                    } else {
                        mHorizontalCycle = path;
                    }
                    String desc = Constraint.describePath(path,
                            match.type.name, match.edge.id);

                    feedback.errorMessage = "Constraint creates a cycle: " + desc;
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks for any cycles in the dependencies
     *
     * @param feedback the drop feedback state
     */
    public void checkCycles(DropFeedback feedback) {
        // Deliberate short circuit evaluation -- only list the first cycle
        feedback.errorMessage = null;
        mHorizontalCycle = null;
        mVerticalCycle = null;

        if (checkCycle(feedback, mCurrentTopMatch, true /* vertical */)
                || checkCycle(feedback, mCurrentBottomMatch, true)) {
        }

        if (checkCycle(feedback, mCurrentLeftMatch, false)
                || checkCycle(feedback, mCurrentRightMatch, false)) {
        }
    }

    /** Records the matchable outside edges for the given node to the potential match list */
    protected void addBounds(INode node, String id,
            boolean addHorizontal, boolean addVertical) {
        Rect b = node.getBounds();
        Margins margins = node.getMargins();
        if (addHorizontal) {
            if (margins.top != 0) {
                mHorizontalEdges.add(new Segment(b.y, b.x, b.x2(), node, id, TOP, WITHOUT_MARGIN));
                mHorizontalEdges.add(new Segment(b.y - margins.top, b.x, b.x2(), node, id,
                        TOP, WITH_MARGIN));
            } else {
                mHorizontalEdges.add(new Segment(b.y, b.x, b.x2(), node, id, TOP, NO_MARGIN));
            }
            if (margins.bottom != 0) {
                mHorizontalEdges.add(new Segment(b.y2(), b.x, b.x2(), node, id, BOTTOM,
                        WITHOUT_MARGIN));
                mHorizontalEdges.add(new Segment(b.y2() + margins.bottom, b.x, b.x2(), node,
                        id, BOTTOM, WITH_MARGIN));
            } else {
                mHorizontalEdges.add(new Segment(b.y2(), b.x, b.x2(), node, id,
                        BOTTOM, NO_MARGIN));
            }
        }
        if (addVertical) {
            if (margins.left != 0) {
                mVerticalEdges.add(new Segment(b.x, b.y, b.y2(), node, id, LEFT, WITHOUT_MARGIN));
                mVerticalEdges.add(new Segment(b.x - margins.left, b.y, b.y2(), node, id, LEFT,
                        WITH_MARGIN));
            } else {
                mVerticalEdges.add(new Segment(b.x, b.y, b.y2(), node, id, LEFT, NO_MARGIN));
            }

            if (margins.right != 0) {
                mVerticalEdges.add(new Segment(b.x2(), b.y, b.y2(), node, id,
                        RIGHT, WITHOUT_MARGIN));
                mVerticalEdges.add(new Segment(b.x2() + margins.right, b.y, b.y2(), node, id,
                        RIGHT, WITH_MARGIN));
            } else {
                mVerticalEdges.add(new Segment(b.x2(), b.y, b.y2(), node, id,
                        RIGHT, NO_MARGIN));
            }
        }
    }

    /** Records the center edges for the given node to the potential match list */
    protected void addCenter(INode node, String id,
            boolean addHorizontal, boolean addVertical) {
        Rect b = node.getBounds();

        if (addHorizontal) {
            mCenterHorizEdges.add(new Segment(b.centerY(), b.x, b.x2(),
                node, id, CENTER_HORIZONTAL, NO_MARGIN));
        }
        if (addVertical) {
            mCenterVertEdges.add(new Segment(b.centerX(), b.y, b.y2(),
                node, id, CENTER_VERTICAL, NO_MARGIN));
        }
    }

    /** Records the baseline edge for the given node to the potential match list */
    protected int addBaseLine(INode node, String id) {
        int baselineY = node.getBaseline();
        if (baselineY != -1) {
            Rect b = node.getBounds();
            mHorizontalEdges.add(new Segment(b.y + baselineY, b.x, b.x2(), node, id, BASELINE,
                    NO_MARGIN));
        }

        return baselineY;
    }

    protected void snapVertical(Segment vEdge, int x, Rect newBounds) {
        newBounds.x = x;
    }

    protected void snapHorizontal(Segment hEdge, int y, Rect newBounds) {
        newBounds.y = y;
    }

    /**
     * Returns whether two edge types are compatible. For example, we only match the
     * center of one object with the center of another.
     *
     * @param edge the first edge type to compare
     * @param dragged the second edge type to compare the first one with
     * @param delta the delta between the two edge locations
     * @return true if the two edge types can be compatibly matched
     */
    protected boolean isEdgeTypeCompatible(SegmentType edge, SegmentType dragged, int delta) {

        if (Math.abs(delta) > BaseLayoutRule.getMaxMatchDistance()) {
            if (dragged == LEFT || dragged == TOP) {
                if (delta > 0) {
                    return false;
                }
            } else {
                if (delta < 0) {
                    return false;
                }
            }
        }

        switch (edge) {
            case BOTTOM:
            case TOP:
                return dragged == TOP || dragged == BOTTOM;
            case LEFT:
            case RIGHT:
                return dragged == LEFT || dragged == RIGHT;

            // Center horizontal, center vertical and Baseline only matches the same
            // type, and only within the matching distance -- no margins!
            case BASELINE:
            case CENTER_HORIZONTAL:
            case CENTER_VERTICAL:
                return dragged == edge && Math.abs(delta) < getMaxMatchDistance();
            default: assert false : edge;
        }
        return false;
    }

    /**
     * Finds the closest matching segments among the given list of edges for the given
     * dragged edge, and returns these as a list of matches
     */
    protected List<Match> findClosest(Segment draggedEdge, List<Segment> edges) {
        List<Match> closest = new ArrayList<Match>();
        addClosest(draggedEdge, edges, closest);
        return closest;
    }

    protected void addClosest(Segment draggedEdge, List<Segment> edges,
            List<Match> closest) {
        int at = draggedEdge.at;
        int closestDelta = closest.size() > 0 ? closest.get(0).delta : Integer.MAX_VALUE;
        int closestDistance = abs(closestDelta);
        for (Segment edge : edges) {
            assert draggedEdge.edgeType.isHorizontal() == edge.edgeType.isHorizontal();

            int delta = edge.at - at;
            int distance = abs(delta);
            if (distance > closestDistance) {
                continue;
            }

            if (!isEdgeTypeCompatible(edge.edgeType, draggedEdge.edgeType, delta)) {
                continue;
            }

            boolean withParent = edge.node == layout;
            ConstraintType type = ConstraintType.forMatch(withParent,
                    draggedEdge.edgeType, edge.edgeType);
            if (type == null) {
                continue;
            }

            // Ensure that the edge match is compatible; for example, a "below"
            // constraint can only apply to the margin bounds and a "bottom"
            // constraint can only apply to the non-margin bounds.
            if (type.relativeToMargin && edge.marginType == WITHOUT_MARGIN) {
                continue;
            } else if (!type.relativeToMargin && edge.marginType == WITH_MARGIN) {
                continue;
            }

            Match match = new Match(this, edge, draggedEdge, type, delta);

            if (distance < closestDistance) {
                closest.clear();
                closestDistance = distance;
                closestDelta = delta;
            } else if (delta * closestDelta < 0) {
                // They have different signs, e.g. the matches are equal but
                // on opposite sides; can't accept them both
                continue;
            }
            closest.add(match);
        }
    }

    protected void clearSuggestions() {
        mHorizontalSuggestions = mVerticalSuggestions = null;
        mCurrentLeftMatch = mCurrentRightMatch = null;
        mCurrentTopMatch = mCurrentBottomMatch = null;
    }

    /**
     * Given a node, apply the suggestions by expressing them as relative layout param
     * values
     *
     * @param n the node to apply constraints to
     */
    public void applyConstraints(INode n) {
        // Process each edge separately
        String centerBoth = n.getStringAttr(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT);
        if (centerBoth != null && centerBoth.equals(VALUE_TRUE)) {
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT, null);

            // If you had a center-in-both-directions attribute, and you're
            // only resizing in one dimension, then leave the other dimension
            // centered, e.g. if you have centerInParent and apply alignLeft,
            // then you should end up with alignLeft and centerVertically
            if (mCurrentTopMatch == null && mCurrentBottomMatch == null) {
                n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, VALUE_TRUE);
            }
            if (mCurrentLeftMatch == null && mCurrentRightMatch == null) {
                n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, VALUE_TRUE);
            }
        }

        if (mMoveTop) {
            // Remove top attachments
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_TOP, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_TOP, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_BELOW, null);

            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_BASELINE, null);

        }

        if (mMoveBottom) {
            // Remove bottom attachments
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_BOTTOM, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_BOTTOM, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ABOVE, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_BASELINE, null);
        }

        if (mMoveLeft) {
            // Remove left attachments
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_LEFT, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_LEFT, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_TO_RIGHT_OF, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, null);
        }

        if (mMoveRight) {
            // Remove right attachments
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_RIGHT, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_RIGHT, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_TO_LEFT_OF, null);
            n.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, null);
        }

        if (mMoveTop && mCurrentTopMatch != null) {
            applyConstraint(n, mCurrentTopMatch.getConstraint(true /* generateId */));
            if (mCurrentTopMatch.type == ALIGN_BASELINE) {
                // HACK! WORKAROUND! Baseline doesn't provide a new bottom edge for attachments
                String c = mCurrentTopMatch.getConstraint(true);
                c = c.replace(ATTR_LAYOUT_ALIGN_BASELINE, ATTR_LAYOUT_ALIGN_BOTTOM);
                applyConstraint(n, c);
            }
        }

        if (mMoveBottom && mCurrentBottomMatch != null) {
            applyConstraint(n, mCurrentBottomMatch.getConstraint(true));
        }

        if (mMoveLeft && mCurrentLeftMatch != null) {
            applyConstraint(n, mCurrentLeftMatch.getConstraint(true));
        }

        if (mMoveRight && mCurrentRightMatch != null) {
            applyConstraint(n, mCurrentRightMatch.getConstraint(true));
        }

        if (mMoveLeft) {
            applyMargin(n, ATTR_LAYOUT_MARGIN_LEFT, mLeftMargin);
        }
        if (mMoveRight) {
            applyMargin(n, ATTR_LAYOUT_MARGIN_RIGHT, mRightMargin);
        }
        if (mMoveTop) {
            applyMargin(n, ATTR_LAYOUT_MARGIN_TOP, mTopMargin);
        }
        if (mMoveBottom) {
            applyMargin(n, ATTR_LAYOUT_MARGIN_BOTTOM, mBottomMargin);
        }
    }

    private void applyConstraint(INode n, String constraint) {
        assert constraint.contains("=") : constraint;
        String name = constraint.substring(0, constraint.indexOf('='));
        String value = constraint.substring(constraint.indexOf('=') + 1);
        n.setAttribute(ANDROID_URI, name, value);
    }

    private void applyMargin(INode n, String marginAttribute, int margin) {
        if (margin > 0) {
            int dp = mRulesEngine.pxToDp(margin);
            n.setAttribute(ANDROID_URI, marginAttribute, String.format(VALUE_N_DP, dp));
        } else if (n.getStringAttr(ANDROID_URI, marginAttribute) != null) {
            // Clear out existing margin
            n.setAttribute(ANDROID_URI, marginAttribute, null);
        }
    }

    private void removeRelativeParams(INode node) {
        for (ConstraintType type : ConstraintType.values()) {
            node.setAttribute(ANDROID_URI, type.name, null);
        }
        node.setAttribute(ANDROID_URI,ATTR_LAYOUT_MARGIN_LEFT, null);
        node.setAttribute(ANDROID_URI,ATTR_LAYOUT_MARGIN_RIGHT, null);
        node.setAttribute(ANDROID_URI,ATTR_LAYOUT_MARGIN_TOP, null);
        node.setAttribute(ANDROID_URI,ATTR_LAYOUT_MARGIN_BOTTOM, null);
    }

    /**
     * Attach the new child to the previous node
     * @param previous the previous child
     * @param node the new child to attach it to
     */
    public void attachPrevious(INode previous, INode node) {
        removeRelativeParams(node);

        String id = previous.getStringAttr(ANDROID_URI, ATTR_ID);
        if (id == null) {
            return;
        }

        if (mCurrentTopMatch != null || mCurrentBottomMatch != null) {
            // Attaching the top: arrange below, and for bottom arrange above
            node.setAttribute(ANDROID_URI,
                    mCurrentTopMatch != null ? ATTR_LAYOUT_BELOW : ATTR_LAYOUT_ABOVE, id);
            // Apply same left/right constraints as the parent
            if (mCurrentLeftMatch != null) {
                applyConstraint(node, mCurrentLeftMatch.getConstraint(true));
                applyMargin(node, ATTR_LAYOUT_MARGIN_LEFT, mLeftMargin);
            } else if (mCurrentRightMatch != null) {
                applyConstraint(node, mCurrentRightMatch.getConstraint(true));
                applyMargin(node, ATTR_LAYOUT_MARGIN_RIGHT, mRightMargin);
            }
        } else if (mCurrentLeftMatch != null || mCurrentRightMatch != null) {
            node.setAttribute(ANDROID_URI,
                    mCurrentLeftMatch != null ? ATTR_LAYOUT_TO_RIGHT_OF : ATTR_LAYOUT_TO_LEFT_OF,
                            id);
            // Apply same top/bottom constraints as the parent
            if (mCurrentTopMatch != null) {
                applyConstraint(node, mCurrentTopMatch.getConstraint(true));
                applyMargin(node, ATTR_LAYOUT_MARGIN_TOP, mTopMargin);
            } else if (mCurrentBottomMatch != null) {
                applyConstraint(node, mCurrentBottomMatch.getConstraint(true));
                applyMargin(node, ATTR_LAYOUT_MARGIN_BOTTOM, mBottomMargin);
            }
        } else {
            return;
        }
    }

    /** Breaks any cycles detected by the handler */
    public void removeCycles() {
        if (mHorizontalCycle != null) {
            removeCycles(mHorizontalDeps);
        }
        if (mVerticalCycle != null) {
            removeCycles(mVerticalDeps);
        }
    }

    private void removeCycles(Set<INode> deps) {
        for (INode node : mDraggedNodes) {
            ViewData view = mDependencyGraph.getView(node);
            if (view != null) {
                for (Constraint constraint : view.dependedOnBy) {
                    // For now, remove ALL constraints pointing to this node in this orientation.
                    // Later refine this to be smarter. (We can't JUST remove the constraints
                    // identified in the cycle since there could be multiple.)
                    constraint.from.node.setAttribute(ANDROID_URI, constraint.type.name, null);
                }
            }
        }
    }

    /**
     * Comparator used to sort matches such that the first match is the most desirable
     * match (where we prefer attaching to parent bounds, we avoid matches that lead to a
     * cycle, we prefer constraints on closer widgets rather than ones further away, and
     * so on.)
     * <p>
     * There are a number of sorting criteria. One of them is the distance between the
     * matched edges. We may end up with multiple matches that are the same distance. In
     * that case we look at the orientation; on the left side, prefer left-oriented
     * attachments, and on the right-side prefer right-oriented attachments. For example,
     * consider the following scenario:
     *
     * <pre>
     *    +--------------------+-------------------------+
     *    | Attached on left   |                         |
     *    +--------------------+                         |
     *    |                                              |
     *    |                    +-----+                   |
     *    |                    |  A  |                   |
     *    |                    +-----+                   |
     *    |                                              |
     *    |                    +-------------------------+
     *    |                    |       Attached on right |
     *    +--------------------+-------------------------+
     * </pre>
     *
     * Here, dragging the left edge should attach to the top left attached view, whereas
     * in the following layout dragging the right edge would attach to the bottom view:
     *
     * <pre>
     *    +--------------------------+-------------------+
     *    | Attached on left         |                   |
     *    +--------------------------+                   |
     *    |                                              |
     *    |                    +-----+                   |
     *    |                    |  A  |                   |
     *    |                    +-----+                   |
     *    |                                              |
     *    |                          +-------------------+
     *    |                          | Attached on right |
     *    +--------------------------+-------------------+
     *
     * </pre>
     *
     * </ul>
     */
    private final class MatchComparator implements Comparator<Match> {
        @Override
        public int compare(Match m1, Match m2) {
            // Always prefer matching parent bounds
            int parent1 = m1.edge.node == layout ? -1 : 1;
            int parent2 = m2.edge.node == layout ? -1 : 1;
            // unless it's a center bound -- those should always get lowest priority since
            // they overlap with other usually more interesting edges near the center of
            // the layout.
            if (m1.edge.edgeType == CENTER_HORIZONTAL
                    || m1.edge.edgeType == CENTER_VERTICAL) {
                parent1 = 2;
            }
            if (m2.edge.edgeType == CENTER_HORIZONTAL
                    || m2.edge.edgeType == CENTER_VERTICAL) {
                parent2 = 2;
            }
            if (parent1 != parent2) {
                return parent1 - parent2;
            }

            // Avoid matching edges that would lead to a cycle
            if (m1.edge.edgeType.isHorizontal()) {
                int cycle1 = mHorizontalDeps.contains(m1.edge.node) ? 1 : -1;
                int cycle2 = mHorizontalDeps.contains(m2.edge.node) ? 1 : -1;
                if (cycle1 != cycle2) {
                    return cycle1 - cycle2;
                }
            } else {
                int cycle1 = mVerticalDeps.contains(m1.edge.node) ? 1 : -1;
                int cycle2 = mVerticalDeps.contains(m2.edge.node) ? 1 : -1;
                if (cycle1 != cycle2) {
                    return cycle1 - cycle2;
                }
            }

            // TODO: Sort by minimum depth -- do we have the depth anywhere?

            // Prefer nodes that are closer
            int distance1, distance2;
            if (m1.edge.to <= m1.with.from) {
                distance1 = m1.with.from - m1.edge.to;
            } else if (m1.edge.from >= m1.with.to) {
                distance1 = m1.edge.from - m1.with.to;
            } else {
                // Some kind of overlap - not sure how to prioritize these yet...
                distance1 = 0;
            }
            if (m2.edge.to <= m2.with.from) {
                distance2 = m2.with.from - m2.edge.to;
            } else if (m2.edge.from >= m2.with.to) {
                distance2 = m2.edge.from - m2.with.to;
            } else {
                // Some kind of overlap - not sure how to prioritize these yet...
                distance2 = 0;
            }

            if (distance1 != distance2) {
                return distance1 - distance2;
            }

            // Prefer matching on baseline
            int baseline1 = (m1.edge.edgeType == BASELINE) ? -1 : 1;
            int baseline2 = (m2.edge.edgeType == BASELINE) ? -1 : 1;
            if (baseline1 != baseline2) {
                return baseline1 - baseline2;
            }

            // Prefer matching top/left edges before matching bottom/right edges
            int orientation1 = (m1.with.edgeType == LEFT ||
                      m1.with.edgeType == TOP) ? -1 : 1;
            int orientation2 = (m2.with.edgeType == LEFT ||
                      m2.with.edgeType == TOP) ? -1 : 1;
            if (orientation1 != orientation2) {
                return orientation1 - orientation2;
            }

            // Prefer opposite-matching over same-matching.
            // In other words, if we have the choice of matching
            // our left edge with another element's left edge,
            // or matching our left edge with another element's right
            // edge, prefer the right edge since that
            // The two matches have identical distance; try to sort by
            // orientation
            int edgeType1 = (m1.edge.edgeType != m1.with.edgeType) ? -1 : 1;
            int edgeType2 = (m2.edge.edgeType != m2.with.edgeType) ? -1 : 1;
            if (edgeType1 != edgeType2) {
                return edgeType1 - edgeType2;
            }

            return 0;
        }
    }

    /**
     * Returns the {@link IClientRulesEngine} IDE callback
     *
     * @return the {@link IClientRulesEngine} IDE callback, never null
     */
    public IClientRulesEngine getRulesEngine() {
        return mRulesEngine;
    }
}
