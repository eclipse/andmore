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

import static com.android.ide.common.api.SegmentType.BASELINE;
import static com.android.ide.common.api.SegmentType.BOTTOM;
import static com.android.ide.common.api.SegmentType.CENTER_HORIZONTAL;
import static com.android.ide.common.api.SegmentType.CENTER_VERTICAL;
import static com.android.ide.common.api.SegmentType.LEFT;
import static com.android.ide.common.api.SegmentType.RIGHT;
import static com.android.ide.common.api.SegmentType.TOP;
import static com.android.ide.common.api.SegmentType.UNKNOWN;
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
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.SegmentType;

import java.util.HashMap;
import java.util.Map;

/**
 * Each constraint type corresponds to a type of constraint available for the
 * RelativeLayout; for example, {@link #LAYOUT_ABOVE} corresponds to the layout_above constraint.
 */
enum ConstraintType {
    LAYOUT_ABOVE(ATTR_LAYOUT_ABOVE,
            null /* sourceX */, BOTTOM, null /* targetX */, TOP,
            false /* targetParent */, true /* horizontalEdge */, false /* verticalEdge */,
            true /* relativeToMargin */),

    LAYOUT_BELOW(ATTR_LAYOUT_BELOW, null, TOP, null, BOTTOM, false, true, false, true),
    ALIGN_TOP(ATTR_LAYOUT_ALIGN_TOP, null, TOP, null, TOP, false, true, false, false),
    ALIGN_BOTTOM(ATTR_LAYOUT_ALIGN_BOTTOM, null, BOTTOM, null, BOTTOM, false, true, false, false),
    ALIGN_LEFT(ATTR_LAYOUT_ALIGN_LEFT, LEFT, null, LEFT, null, false, false, true, false),
    ALIGN_RIGHT(ATTR_LAYOUT_ALIGN_RIGHT, RIGHT, null, RIGHT, null, false, false, true, false),
    LAYOUT_LEFT_OF(ATTR_LAYOUT_TO_LEFT_OF, RIGHT, null, LEFT, null, false, false, true, true),
    LAYOUT_RIGHT_OF(ATTR_LAYOUT_TO_RIGHT_OF, LEFT, null, RIGHT, null, false, false, true, true),
    ALIGN_PARENT_TOP(ATTR_LAYOUT_ALIGN_PARENT_TOP, null, TOP, null, TOP, true, true, false, false),
    ALIGN_BASELINE(ATTR_LAYOUT_ALIGN_BASELINE, null, BASELINE, null, BASELINE, false, true, false,
            false),
    ALIGN_PARENT_LEFT(ATTR_LAYOUT_ALIGN_PARENT_LEFT, LEFT, null, LEFT, null, true, false, true,
            false),
    ALIGN_PARENT_RIGHT(ATTR_LAYOUT_ALIGN_PARENT_RIGHT, RIGHT, null, RIGHT, null, true, false, true,
            false),
    ALIGN_PARENT_BOTTOM(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM, null, BOTTOM, null, BOTTOM, true, true,
            false, false),
    LAYOUT_CENTER_HORIZONTAL(ATTR_LAYOUT_CENTER_HORIZONTAL, CENTER_VERTICAL, null, CENTER_VERTICAL,
            null, true, true, false, false),
    LAYOUT_CENTER_VERTICAL(ATTR_LAYOUT_CENTER_VERTICAL, null, CENTER_HORIZONTAL, null,
            CENTER_HORIZONTAL, true, false, true, false),
    LAYOUT_CENTER_IN_PARENT(ATTR_LAYOUT_CENTER_IN_PARENT, CENTER_VERTICAL, CENTER_HORIZONTAL,
            CENTER_VERTICAL, CENTER_HORIZONTAL, true, true, true, false);

    private ConstraintType(String name, SegmentType sourceSegmentTypeX,
            SegmentType sourceSegmentTypeY, SegmentType targetSegmentTypeX,
            SegmentType targetSegmentTypeY, boolean targetParent, boolean horizontalEdge,
            boolean verticalEdge, boolean relativeToMargin) {
        assert horizontalEdge || verticalEdge;

        this.name = name;
        this.sourceSegmentTypeX = sourceSegmentTypeX != null ? sourceSegmentTypeX : UNKNOWN;
        this.sourceSegmentTypeY = sourceSegmentTypeY != null ? sourceSegmentTypeY : UNKNOWN;
        this.targetSegmentTypeX = targetSegmentTypeX != null ? targetSegmentTypeX : UNKNOWN;
        this.targetSegmentTypeY = targetSegmentTypeY != null ? targetSegmentTypeY : UNKNOWN;
        this.targetParent = targetParent;
        this.horizontalEdge = horizontalEdge;
        this.verticalEdge = verticalEdge;
        this.relativeToMargin = relativeToMargin;
    }

    /** The attribute name of the constraint */
    public final String name;

    /** The horizontal position of the source of the constraint */
    public final SegmentType sourceSegmentTypeX;

    /** The vertical position of the source of the constraint */
    public final SegmentType sourceSegmentTypeY;

    /** The horizontal position of the target of the constraint */
    public final SegmentType targetSegmentTypeX;

    /** The vertical position of the target of the constraint */
    public final SegmentType targetSegmentTypeY;

    /**
     * If true, the constraint targets the parent layout, otherwise it targets another
     * view
     */
    public final boolean targetParent;

    /** If true, this constraint affects the horizontal dimension */
    public final boolean horizontalEdge;

    /** If true, this constraint affects the vertical dimension */
    public final boolean verticalEdge;

    /**
     * Whether this constraint is relative to the margin bounds of the node rather than
     * the node's actual bounds
     */
    public final boolean relativeToMargin;

    /** Map from attribute name to constraint type */
    private static Map<String, ConstraintType> sNameToType;

    /**
     * Returns the {@link ConstraintType} corresponding to the given attribute name, or
     * null if not found.
     *
     * @param attribute the name of the attribute to look up
     * @return the corresponding {@link ConstraintType}
     */
    @Nullable
    public static ConstraintType fromAttribute(@NonNull String attribute) {
        if (sNameToType == null) {
            ConstraintType[] types = ConstraintType.values();
            Map<String, ConstraintType> map = new HashMap<String, ConstraintType>(types.length);
            for (ConstraintType type : types) {
                map.put(type.name, type);
            }
            sNameToType = map;
        }
        return sNameToType.get(attribute);
    }

    /**
     * Returns true if this constraint type represents a constraint where the target edge
     * is one of the parent edges (actual edge, not center/baseline segments)
     *
     * @return true if the target segment is a parent edge
     */
    public boolean isRelativeToParentEdge() {
        return this == ALIGN_PARENT_LEFT || this == ALIGN_PARENT_RIGHT || this == ALIGN_PARENT_TOP
                || this == ALIGN_PARENT_BOTTOM;
    }

    /**
     * Returns a {@link ConstraintType} for a potential match of edges.
     *
     * @param withParent if true, the target is the parent
     * @param from the source edge
     * @param to the target edge
     * @return a {@link ConstraintType}, or null
     */
    @Nullable
    public static ConstraintType forMatch(boolean withParent, SegmentType from, SegmentType to) {
        // Attached to parent edge?
        if (withParent) {
            switch (from) {
                case TOP:
                    return ALIGN_PARENT_TOP;
                case BOTTOM:
                    return ALIGN_PARENT_BOTTOM;
                case LEFT:
                    return ALIGN_PARENT_LEFT;
                case RIGHT:
                    return ALIGN_PARENT_RIGHT;
                case CENTER_HORIZONTAL:
                    return LAYOUT_CENTER_VERTICAL;
                case CENTER_VERTICAL:
                    return LAYOUT_CENTER_HORIZONTAL;
            }

            return null;
        }

        // Attached to some other node.
        switch (from) {
            case TOP:
                switch (to) {
                    case TOP:
                        return ALIGN_TOP;
                    case BOTTOM:
                        return LAYOUT_BELOW;
                    case BASELINE:
                        return ALIGN_BASELINE;
                }
                break;
            case BOTTOM:
                switch (to) {
                    case TOP:
                        return LAYOUT_ABOVE;
                    case BOTTOM:
                        return ALIGN_BOTTOM;
                    case BASELINE:
                        return ALIGN_BASELINE;
                }
                break;
            case LEFT:
                switch (to) {
                    case LEFT:
                        return ALIGN_LEFT;
                    case RIGHT:
                        return LAYOUT_RIGHT_OF;
                }
                break;
            case RIGHT:
                switch (to) {
                    case LEFT:
                        return LAYOUT_LEFT_OF;
                    case RIGHT:
                        return ALIGN_RIGHT;
                }
                break;
            case BASELINE:
                return ALIGN_BASELINE;
        }

        return null;
    }
}
