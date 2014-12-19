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

package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.VALUE_N_DP;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.Segment;
import com.android.ide.common.api.SegmentType;

/** State held during resizing operations */
class ResizeState {
    /**
     * The associated rule
     */
    private final BaseLayoutRule mRule;

    /**
     * The node being resized
     */
    public final INode node;

     /**
      * The layout containing the resized node
      */
    public final INode layout;

    /** The proposed resized bounds of the node */
    public Rect bounds;

    /** The preferred wrap_content bounds of the node */
    public Rect wrapBounds;

    /** The suggested horizontal fill_parent guideline position */
    public Segment horizontalFillSegment;

    /** The suggested vertical fill_parent guideline position */
    public Segment verticalFillSegment;

    /** The type of horizontal edge being resized, or null */
    public SegmentType horizontalEdgeType;

    /** The type of vertical edge being resized, or null */
    public SegmentType verticalEdgeType;

    /** Whether the user has snapped to the wrap_content width */
    public boolean wrapWidth;

    /** Whether the user has snapped to the wrap_content height */
    public boolean wrapHeight;

    /** Whether the user has snapped to the match_parent width */
    public boolean fillWidth;

    /** Whether the user has snapped to the match_parent height */
    public boolean fillHeight;

    /** Custom field for use by subclasses */
    public Object clientData;

    /** Keyboard mask */
    public int modifierMask;

    /**
     * The actual view object for the layout containing the resizing operation,
     * or null if not known
     */
    public Object layoutView;

    /**
     * Constructs a new {@link ResizeState}
     *
     * @param rule the associated rule
     * @param layout the parent layout containing the resized node
     * @param layoutView the actual View instance for the layout, or null if not known
     * @param node the node being resized
     */
    ResizeState(BaseLayoutRule rule, INode layout, Object layoutView, INode node) {
        mRule = rule;

        this.layout = layout;
        this.node = node;
        this.layoutView = layoutView;
    }

    /**
     * Returns the width attribute to be set to match the new bounds
     *
     * @return the width string, never null
     */
    public String getWidthAttribute() {
        if (wrapWidth) {
            return VALUE_WRAP_CONTENT;
        } else if (fillWidth) {
            return mRule.getFillParentValueName();
        } else {
            return String.format(VALUE_N_DP, mRule.mRulesEngine.pxToDp(bounds.w));
        }
    }

    /**
     * Returns the height attribute to be set to match the new bounds
     *
     * @return the height string, never null
     */
    public String getHeightAttribute() {
        if (wrapHeight) {
            return VALUE_WRAP_CONTENT;
        } else if (fillHeight) {
            return mRule.getFillParentValueName();
        } else {
            return String.format(VALUE_N_DP, mRule.mRulesEngine.pxToDp(bounds.h));
        }
    }
}
