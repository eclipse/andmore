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
import static com.android.ide.common.api.SegmentType.BASELINE;
import static com.android.ide.common.api.SegmentType.BOTTOM;
import static com.android.ide.common.api.SegmentType.CENTER_HORIZONTAL;
import static com.android.ide.common.api.SegmentType.CENTER_VERTICAL;
import static com.android.ide.common.api.SegmentType.LEFT;
import static com.android.ide.common.api.SegmentType.RIGHT;
import static com.android.ide.common.api.SegmentType.TOP;
import static com.android.SdkConstants.ATTR_ID;
import static java.lang.Math.abs;

import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.Segment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.relative.DependencyGraph.ViewData;

/**
 * A {@link MoveHandler} is a {@link GuidelineHandler} which handles move and drop
 * gestures, and offers guideline suggestions and snapping.
 * <p>
 * Unlike the {@link ResizeHandler}, the {@link MoveHandler} looks for matches for all
 * different segment types -- the left edge, the right edge, the baseline, the center
 * edges, and so on -- and picks the best among these.
 */
public class MoveHandler extends GuidelineHandler {
    private int mDraggedBaseline;

    /**
     * Creates a new {@link MoveHandler}.
     *
     * @param layout the layout element the handler is operating on
     * @param elements the elements being dragged in the move operation
     * @param rulesEngine the corresponding {@link IClientRulesEngine}
     */
    public MoveHandler(INode layout, IDragElement[] elements, IClientRulesEngine rulesEngine) {
        super(layout, rulesEngine);

        // Compute list of nodes being dragged within the layout, if any
        List<INode> nodes = new ArrayList<INode>();
        for (IDragElement element : elements) {
            ViewData view = mDependencyGraph.getView(element);
            if (view != null) {
                nodes.add(view.node);
            }
        }
        mDraggedNodes = nodes;

        mHorizontalDeps = mDependencyGraph.dependsOn(nodes, false /* verticalEdge */);
        mVerticalDeps = mDependencyGraph.dependsOn(nodes, true /* verticalEdge */);

        for (INode child : layout.getChildren()) {
            Rect bc = child.getBounds();
            if (bc.isValid()) {
                // First see if this node looks like it's the same as one of the
                // *dragged* bounds
                boolean isDragged = false;
                for (IDragElement element : elements) {
                    // This tries to determine if an INode corresponds to an
                    // IDragElement, by comparing their bounds.
                    if (bc.equals(element.getBounds())) {
                        isDragged = true;
                    }
                }

                if (!isDragged) {
                    String id = child.getStringAttr(ANDROID_URI, ATTR_ID);
                    // It's okay for id to be null; if you apply a constraint
                    // to a node with a missing id we will generate the id

                    boolean addHorizontal = !mHorizontalDeps.contains(child);
                    boolean addVertical = !mVerticalDeps.contains(child);

                    addBounds(child, id, addHorizontal, addVertical);
                    if (addHorizontal) {
                        addBaseLine(child, id);
                    }
                }
            }
        }

        String id = layout.getStringAttr(ANDROID_URI, ATTR_ID);
        addBounds(layout, id, true, true);
        addCenter(layout, id, true, true);
    }

    @Override
    protected void snapVertical(Segment vEdge, int x, Rect newBounds) {
        int maxDistance = BaseLayoutRule.getMaxMatchDistance();
        if (vEdge.edgeType == LEFT) {
            int margin = !mSnap ? 0 : abs(newBounds.x - x);
            if (margin > maxDistance) {
                mLeftMargin = margin;
            } else {
                newBounds.x = x;
            }
        } else if (vEdge.edgeType == RIGHT) {
            int margin = !mSnap ? 0 : abs(newBounds.x - (x - newBounds.w));
            if (margin > maxDistance) {
                mRightMargin = margin;
            } else {
                newBounds.x = x - newBounds.w;
            }
        } else if (vEdge.edgeType == CENTER_VERTICAL) {
            newBounds.x = x - newBounds.w / 2;
        } else {
            assert false : vEdge;
        }
    }

    // TODO: Consider unifying this with the snapping logic in ResizeHandler
    @Override
    protected void snapHorizontal(Segment hEdge, int y, Rect newBounds) {
        int maxDistance = BaseLayoutRule.getMaxMatchDistance();
        if (hEdge.edgeType == TOP) {
            int margin = !mSnap ? 0 : abs(newBounds.y - y);
            if (margin > maxDistance) {
                mTopMargin = margin;
            } else {
                newBounds.y = y;
            }
        } else if (hEdge.edgeType == BOTTOM) {
            int margin = !mSnap ? 0 : abs(newBounds.y - (y - newBounds.h));
            if (margin > maxDistance) {
                mBottomMargin = margin;
            } else {
                newBounds.y = y - newBounds.h;
            }
        } else if (hEdge.edgeType == CENTER_HORIZONTAL) {
            int margin = !mSnap ? 0 : abs(newBounds.y - (y - newBounds.h / 2));
            if (margin > maxDistance) {
                mTopMargin = margin;
                // or bottomMargin?
            } else {
                newBounds.y = y - newBounds.h / 2;
            }
        } else if (hEdge.edgeType == BASELINE) {
                newBounds.y = y - mDraggedBaseline;
        } else {
            assert false : hEdge;
        }
    }

    /**
     * Updates the handler for the given mouse move
     *
     * @param feedback the feedback handler
     * @param elements the elements being dragged
     * @param offsetX the new mouse X coordinate
     * @param offsetY the new mouse Y coordinate
     * @param modifierMask the keyboard modifiers pressed during the drag
     */
    public void updateMove(DropFeedback feedback, IDragElement[] elements,
            int offsetX, int offsetY, int modifierMask) {
        mSnap = (modifierMask & DropFeedback.MODIFIER2) == 0;

        Rect firstBounds = elements[0].getBounds();
        INode firstNode = null;
        if (mDraggedNodes != null && mDraggedNodes.size() > 0) {
            // TODO - this isn't quite right; this could be a different node than we have
            // bounds for!
            firstNode = mDraggedNodes.iterator().next();
            firstBounds = firstNode.getBounds();
        }

        mBounds = new Rect(offsetX, offsetY, firstBounds.w, firstBounds.h);
        Rect layoutBounds = layout.getBounds();
        if (mBounds.x2() > layoutBounds.x2()) {
            mBounds.x -= mBounds.x2() - layoutBounds.x2();
        }
        if (mBounds.y2() > layoutBounds.y2()) {
            mBounds.y -= mBounds.y2() - layoutBounds.y2();
        }
        if (mBounds.x < layoutBounds.x) {
            mBounds.x = layoutBounds.x;
        }
        if (mBounds.y < layoutBounds.y) {
            mBounds.y = layoutBounds.y;
        }

        clearSuggestions();

        Rect b = mBounds;
        Segment edge = new Segment(b.y, b.x, b.x2(), null, null, TOP, NO_MARGIN);
        List<Match> horizontalMatches = findClosest(edge, mHorizontalEdges);
        edge = new Segment(b.y2(), b.x, b.x2(), null, null, BOTTOM, NO_MARGIN);
        addClosest(edge, mHorizontalEdges, horizontalMatches);

        edge = new Segment(b.x, b.y, b.y2(), null, null, LEFT, NO_MARGIN);
        List<Match> verticalMatches = findClosest(edge, mVerticalEdges);
        edge = new Segment(b.x2(), b.y, b.y2(), null, null, RIGHT, NO_MARGIN);
        addClosest(edge, mVerticalEdges, verticalMatches);

        // Match center
        edge = new Segment(b.centerX(), b.y, b.y2(), null, null, CENTER_VERTICAL, NO_MARGIN);
        addClosest(edge, mCenterVertEdges, verticalMatches);
        edge = new Segment(b.centerY(), b.x, b.x2(), null, null, CENTER_HORIZONTAL, NO_MARGIN);
        addClosest(edge, mCenterHorizEdges, horizontalMatches);

        // Match baseline
        if (firstNode != null) {
            int baseline = firstNode.getBaseline();
            if (baseline != -1) {
                mDraggedBaseline = baseline;
                edge = new Segment(b.y + baseline, b.x, b.x2(), firstNode, null, BASELINE,
                        NO_MARGIN);
                addClosest(edge, mHorizontalEdges, horizontalMatches);
            }
        } else {
            int baseline = feedback.dragBaseline;
            if (baseline != -1) {
                mDraggedBaseline = baseline;
                edge = new Segment(offsetY + baseline, b.x, b.x2(), null, null, BASELINE,
                        NO_MARGIN);
                addClosest(edge, mHorizontalEdges, horizontalMatches);
            }
        }

        mHorizontalSuggestions = horizontalMatches;
        mVerticalSuggestions = verticalMatches;
        mTopMargin = mBottomMargin = mLeftMargin = mRightMargin = 0;

        Match match = pickBestMatch(mHorizontalSuggestions);
        if (match != null) {
            if (mHorizontalDeps.contains(match.edge.node)) {
                match.cycle = true;
            }

            // Reset top AND bottom bounds regardless of whether both are bound
            mMoveTop = true;
            mMoveBottom = true;

            // TODO: Consider doing the snap logic on all the possible matches
            // BEFORE sorting, in case this affects the best-pick algorithm (since some
            // edges snap and others don't).
            snapHorizontal(match.with, match.edge.at, mBounds);

            if (match.with.edgeType == TOP) {
                mCurrentTopMatch = match;
            } else if (match.with.edgeType == BOTTOM) {
                mCurrentBottomMatch = match;
            } else {
                assert match.with.edgeType == CENTER_HORIZONTAL
                        || match.with.edgeType == BASELINE : match.with.edgeType;
                mCurrentTopMatch = match;
            }
        }

        match = pickBestMatch(mVerticalSuggestions);
        if (match != null) {
            if (mVerticalDeps.contains(match.edge.node)) {
                match.cycle = true;
            }

            // Reset left AND right bounds regardless of whether both are bound
            mMoveLeft = true;
            mMoveRight = true;

            snapVertical(match.with, match.edge.at, mBounds);

            if (match.with.edgeType == LEFT) {
                mCurrentLeftMatch = match;
            } else if (match.with.edgeType == RIGHT) {
                mCurrentRightMatch = match;
            } else {
                assert match.with.edgeType == CENTER_VERTICAL;
                mCurrentLeftMatch = match;
            }
        }

        checkCycles(feedback);
    }
}
