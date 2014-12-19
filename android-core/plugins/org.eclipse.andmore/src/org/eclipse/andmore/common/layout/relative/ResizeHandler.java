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
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.Segment;
import com.android.ide.common.api.SegmentType;

import java.util.Collections;
import java.util.Set;

import org.eclipse.andmore.common.layout.BaseLayoutRule;

/**
 * A {@link ResizeHandler} is a {@link GuidelineHandler} which handles resizing of individual
 * edges in a RelativeLayout.
 */
public class ResizeHandler extends GuidelineHandler {
    private final SegmentType mHorizontalEdgeType;
    private final SegmentType mVerticalEdgeType;

    /**
     * Creates a new {@link ResizeHandler}
     *
     * @param layout the layout containing the resized node
     * @param resized the node being resized
     * @param rulesEngine the applicable {@link IClientRulesEngine}
     * @param horizontalEdgeType the type of horizontal edge being resized, or null
     * @param verticalEdgeType the type of vertical edge being resized, or null
     */
    public ResizeHandler(INode layout, INode resized,
            IClientRulesEngine rulesEngine,
            SegmentType horizontalEdgeType, SegmentType verticalEdgeType) {
        super(layout, rulesEngine);

        assert horizontalEdgeType != null || verticalEdgeType != null;
        assert horizontalEdgeType != BASELINE && verticalEdgeType != BASELINE;
        assert horizontalEdgeType != CENTER_HORIZONTAL && verticalEdgeType != CENTER_HORIZONTAL;
        assert horizontalEdgeType != CENTER_VERTICAL && verticalEdgeType != CENTER_VERTICAL;

        mHorizontalEdgeType = horizontalEdgeType;
        mVerticalEdgeType = verticalEdgeType;

        Set<INode> nodes = Collections.singleton(resized);
        mDraggedNodes = nodes;

        mHorizontalDeps = mDependencyGraph.dependsOn(nodes, false /* vertical */);
        mVerticalDeps = mDependencyGraph.dependsOn(nodes, true /* vertical */);

        if (horizontalEdgeType != null) {
            if (horizontalEdgeType == TOP) {
                mMoveTop = true;
            } else if (horizontalEdgeType == BOTTOM) {
                mMoveBottom = true;
            }
        }
        if (verticalEdgeType != null) {
            if (verticalEdgeType == LEFT) {
                mMoveLeft = true;
            } else if (verticalEdgeType == RIGHT) {
                mMoveRight = true;
            }
        }

        for (INode child : layout.getChildren()) {
            if (child != resized) {
                String id = child.getStringAttr(ANDROID_URI, ATTR_ID);
                addBounds(child, id,
                        !mHorizontalDeps.contains(child),
                        !mVerticalDeps.contains(child));
            }
        }

        addBounds(layout, layout.getStringAttr(ANDROID_URI, ATTR_ID), true, true);
    }

    @Override
    protected void snapVertical(Segment vEdge, int x, Rect newBounds) {
        int maxDistance = BaseLayoutRule.getMaxMatchDistance();
        if (vEdge.edgeType == LEFT) {
            int margin = mSnap ? 0 : abs(newBounds.x - x);
            if (margin > maxDistance) {
                mLeftMargin = margin;
            } else {
                newBounds.w += newBounds.x - x;
                newBounds.x = x;
            }
        } else if (vEdge.edgeType == RIGHT) {
            int margin = mSnap ? 0 : abs(newBounds.x - (x - newBounds.w));
            if (margin > maxDistance) {
                mRightMargin = margin;
            } else {
                newBounds.w = x - newBounds.x;
            }
        } else {
            assert false : vEdge;
        }
    }

    @Override
    protected void snapHorizontal(Segment hEdge, int y, Rect newBounds) {
        int maxDistance = BaseLayoutRule.getMaxMatchDistance();
        if (hEdge.edgeType == TOP) {
            int margin = mSnap ? 0 : abs(newBounds.y - y);
            if (margin > maxDistance) {
                mTopMargin = margin;
            } else {
                newBounds.h += newBounds.y - y;
                newBounds.y = y;
            }
        } else if (hEdge.edgeType == BOTTOM) {
            int margin = mSnap ? 0 : abs(newBounds.y - (y - newBounds.h));
            if (margin > maxDistance) {
                mBottomMargin = margin;
            } else {
                newBounds.h = y - newBounds.y;
            }
        } else {
            assert false : hEdge;
        }
    }

    @Override
    protected boolean isEdgeTypeCompatible(SegmentType edge, SegmentType dragged, int delta) {
        boolean compatible = super.isEdgeTypeCompatible(edge, dragged, delta);

        // When resizing and not snapping (e.g. using margins to pick a specific pixel
        // width) we cannot use -negative- margins to jump back to a closer edge; we
        // must always use positive margins, so mark closer edges that result in a negative
        // margin as not compatible.
        if (compatible && !mSnap) {
            switch (dragged) {
                case LEFT:
                case TOP:
                    return delta <= 0;
                default:
                    return delta >= 0;
            }
        }

        return compatible;
    }

    /**
     * Updates the handler for the given mouse resize
     *
     * @param feedback the feedback handler
     * @param child the node being resized
     * @param newBounds the new bounds of the resize rectangle
     * @param modifierMask the keyboard modifiers pressed during the drag
     */
    public void updateResize(DropFeedback feedback, INode child, Rect newBounds,
            int modifierMask) {
        mSnap = (modifierMask & DropFeedback.MODIFIER2) == 0;
        mBounds = newBounds;
        clearSuggestions();

        Rect b = newBounds;
        Segment hEdge = null;
        Segment vEdge = null;
        String childId = child.getStringAttr(ANDROID_URI, ATTR_ID);

        // TODO: MarginType=NO_MARGIN may not be right. Consider resizing a widget
        //   that has margins and how that should be handled.

        if (mHorizontalEdgeType == TOP) {
            hEdge = new Segment(b.y, b.x, b.x2(), child, childId, mHorizontalEdgeType, NO_MARGIN);
        } else if (mHorizontalEdgeType == BOTTOM) {
            hEdge = new Segment(b.y2(), b.x, b.x2(), child, childId, mHorizontalEdgeType,
                    NO_MARGIN);
        } else {
            assert mHorizontalEdgeType == null;
        }

        if (mVerticalEdgeType == LEFT) {
            vEdge = new Segment(b.x, b.y, b.y2(), child, childId, mVerticalEdgeType, NO_MARGIN);
        } else if (mVerticalEdgeType == RIGHT) {
            vEdge = new Segment(b.x2(), b.y, b.y2(), child, childId, mVerticalEdgeType, NO_MARGIN);
        } else {
            assert mVerticalEdgeType == null;
        }

        mTopMargin = mBottomMargin = mLeftMargin = mRightMargin = 0;

        if (hEdge != null && mHorizontalEdges.size() > 0) {
            // Compute horizontal matches
            mHorizontalSuggestions = findClosest(hEdge, mHorizontalEdges);

            Match match = pickBestMatch(mHorizontalSuggestions);
            if (match != null
                    && (!mSnap || Math.abs(match.delta) < BaseLayoutRule.getMaxMatchDistance())) {
                if (mHorizontalDeps.contains(match.edge.node)) {
                    match.cycle = true;
                }

                snapHorizontal(hEdge, match.edge.at, newBounds);

                if (hEdge.edgeType == TOP) {
                    mCurrentTopMatch = match;
                } else if (hEdge.edgeType == BOTTOM) {
                    mCurrentBottomMatch = match;
                } else {
                    assert hEdge.edgeType == CENTER_HORIZONTAL
                            || hEdge.edgeType == BASELINE : hEdge;
                    mCurrentTopMatch = match;
                }
            }
        }

        if (vEdge != null && mVerticalEdges.size() > 0) {
            mVerticalSuggestions = findClosest(vEdge, mVerticalEdges);

            Match match = pickBestMatch(mVerticalSuggestions);
            if (match != null
                    && (!mSnap || Math.abs(match.delta) < BaseLayoutRule.getMaxMatchDistance())) {
                if (mVerticalDeps.contains(match.edge.node)) {
                    match.cycle = true;
                }

                // Snap
                snapVertical(vEdge, match.edge.at, newBounds);

                if (vEdge.edgeType == LEFT) {
                    mCurrentLeftMatch = match;
                } else if (vEdge.edgeType == RIGHT) {
                    mCurrentRightMatch = match;
                } else {
                    assert vEdge.edgeType == CENTER_VERTICAL;
                    mCurrentLeftMatch = match;
                }
            }
        }

        checkCycles(feedback);
    }
}
