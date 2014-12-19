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

import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.VALUE_TRUE;


import static com.android.SdkConstants.ANDROID_URI;
import com.android.ide.common.api.Segment;

/** A match is a potential pairing of two segments with a given {@link ConstraintType}. */
class Match {
    /** the edge of the dragged node that is matched */
    public final Segment with;

    /** the "other" edge that the dragged edge is matched with */
    public final Segment edge;

    /** the signed distance between the matched edges */
    public final int delta;

    /** the type of constraint this is a match for */
    public final ConstraintType type;

    /** whether this {@link Match} results in a cycle */
    public boolean cycle;

    /** The associated {@link GuidelineHander} which performed the match */
    private final GuidelineHandler mHandler;

    /**
     * Create a new match.
     *
     * @param handler the handler which performed the match
     * @param edge the "other" edge that the dragged edge is matched with
     * @param with the edge of the dragged node that is matched
     * @param type the type of constraint this is a match for
     * @param delta the signed distance between the matched edges
     */
    public Match(GuidelineHandler handler, Segment edge, Segment with,
            ConstraintType type, int delta) {
        mHandler = handler;

        this.edge = edge;
        this.with = with;
        this.type = type;
        this.delta = delta;
    }

    /**
     * Returns the XML constraint attribute value for this match
     *
     * @param generateId whether an id should be generated if one is missing
     * @return the XML constraint attribute value for this match
     */
    public String getConstraint(boolean generateId) {
        if (type.targetParent) {
            return type.name + '=' + VALUE_TRUE;
        } else {
            String id = edge.id;
            if (id == null || id.length() == -1) {
                if (!generateId) {
                    // Placeholder to display for the user during dragging
                    id = "<generated>";
                } else {
                    // Must generate an id on the fly!
                    // See if it's been set by a different constraint we've already applied
                    // to this same node
                    id = edge.node.getStringAttr(ANDROID_URI, ATTR_ID);
                    if (id == null || id.length() == 0) {
                        id = mHandler.getRulesEngine().getUniqueId(edge.node.getFqcn());
                        edge.node.setAttribute(ANDROID_URI, ATTR_ID, id);
                    }
                }
            }
            return type.name + '=' + id;
        }
    }

    @Override
    public String toString() {
        return "Match [type=" + type + ", delta=" + delta + ", edge=" + edge
                + "]";
    }
}
