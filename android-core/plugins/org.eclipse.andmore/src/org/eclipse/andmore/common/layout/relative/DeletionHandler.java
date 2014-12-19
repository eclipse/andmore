/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static org.eclipse.andmore.common.layout.BaseViewRule.stripIdPrefix;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_CENTER_HORIZONTAL;
import static org.eclipse.andmore.common.layout.relative.ConstraintType.LAYOUT_CENTER_VERTICAL;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INode.IAttribute;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles deletions in a relative layout, transferring constraints across
 * deleted nodes
 * <p>
 * TODO: Consider adding the
 * {@link SdkConstants#ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING} attribute to a
 * node if it's pointing to a node which is deleted and which has no transitive
 * reference to another node.
 */
public class DeletionHandler {
    private final INode mLayout;
    private final INode[] mChildren;
    private final List<INode> mDeleted;
    private final Set<String> mDeletedIds;
    private final Map<String, INode> mNodeMap;
    private final List<INode> mMoved;

    /**
     * Creates a new {@link DeletionHandler}
     *
     * @param deleted the deleted nodes
     * @param moved nodes that were moved (e.g. deleted, but also inserted elsewhere)
     * @param layout the parent layout of the deleted nodes
     */
    public DeletionHandler(@NonNull List<INode> deleted, @NonNull List<INode> moved,
            @NonNull INode layout) {
        mDeleted = deleted;
        mMoved = moved;
        mLayout = layout;

        mChildren = mLayout.getChildren();
        mNodeMap = Maps.newHashMapWithExpectedSize(mChildren.length);
        for (INode child : mChildren) {
            String id = child.getStringAttr(ANDROID_URI, ATTR_ID);
            if (id != null) {
                mNodeMap.put(stripIdPrefix(id), child);
            }
        }

        mDeletedIds = Sets.newHashSetWithExpectedSize(mDeleted.size());
        for (INode node : mDeleted) {
            String id = node.getStringAttr(ANDROID_URI, ATTR_ID);
            if (id != null) {
                mDeletedIds.add(stripIdPrefix(id));
            }
        }

        // Any widgets that remain (e.g. typically because they were moved) should
        // keep their incoming dependencies
        for (INode node : mMoved) {
            String id = node.getStringAttr(ANDROID_URI, ATTR_ID);
            if (id != null) {
                mDeletedIds.remove(stripIdPrefix(id));
            }
        }
    }

    @Nullable
    private static String getId(@NonNull IAttribute attribute) {
        if (attribute.getName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                && ANDROID_URI.equals(attribute.getUri())
                && !attribute.getName().startsWith(ATTR_LAYOUT_MARGIN)) {
            String id = attribute.getValue();
            // It might not be an id reference, so check manually rather than just
            // calling stripIdPrefix():
            if (id.startsWith(NEW_ID_PREFIX)) {
                return id.substring(NEW_ID_PREFIX.length());
            } else if (id.startsWith(ID_PREFIX)) {
                return id.substring(ID_PREFIX.length());
            }
        }

        return null;
    }

    /**
     * Updates the constraints in the layout to handle deletion of a set of
     * nodes. This ensures that any constraints pointing to one of the deleted
     * nodes are changed properly to point to a non-deleted node with similar
     * constraints.
     */
    public void updateConstraints() {
        if (mChildren.length == mDeleted.size()) {
            // Deleting everything: Nothing to be done
            return;
        }

        // Now remove incoming edges to any views that were deleted. If possible,
        // don't just delete them but replace them with a transitive constraint, e.g.
        // if we have "A <= B <= C" and "B" is removed, then we end up with "A <= C",

        for (INode child : mChildren) {
            if (mDeleted.contains(child)) {
                continue;
            }

            for (IAttribute attribute : child.getLiveAttributes()) {
                String id = getId(attribute);
                if (id != null) {
                    if (mDeletedIds.contains(id)) {
                        // Unset this reference to a deleted widget. It might be
                        // replaced if the pointed to node points to some other node
                        // on the same side, but it may use a different constraint name,
                        // or have none at all (e.g. parent).
                        String name = attribute.getName();
                        child.setAttribute(ANDROID_URI, name, null);

                        INode deleted = mNodeMap.get(id);
                        if (deleted != null) {
                            ConstraintType type = ConstraintType.fromAttribute(name);
                            if (type != null) {
                                transfer(deleted, child, type, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    private void transfer(INode deleted, INode target, ConstraintType targetType, int depth) {
        if (depth == 20) {
            // Prevent really deep flow or unbounded recursion in case there is a bug in
            // the cycle detection code
            return;
        }

        assert mDeleted.contains(deleted);

        for (IAttribute attribute : deleted.getLiveAttributes()) {
            String name = attribute.getName();
            ConstraintType type = ConstraintType.fromAttribute(name);
            if (type == null) {
                continue;
            }

            ConstraintType transfer = getCompatibleConstraint(type, targetType);
            if (transfer != null) {
                String id = getId(attribute);
                if (id != null) {
                    if (mDeletedIds.contains(id)) {
                        INode nextDeleted = mNodeMap.get(id);
                        if (nextDeleted != null) {
                            // Points to another deleted node: recurse
                            transfer(nextDeleted, target, targetType, depth + 1);
                        }
                    } else {
                        // Found an undeleted node destination: point to it directly.
                        // Note that we're using the
                        target.setAttribute(ANDROID_URI, transfer.name, attribute.getValue());
                    }
                } else {
                    // Pointing to parent or center etc (non-id ref): replicate this on the target
                    target.setAttribute(ANDROID_URI, name, attribute.getValue());
                }
            }
        }
    }

    /**
     * Determines if two constraints are in the same direction and if so returns
     * the constraint in the same direction. Rather than returning boolean true
     * or false, this returns the constraint which is sometimes modified. For
     * example, if you have a node which points left to a node which is centered
     * in parent, then the constraint is turned into center horizontal.
     */
    @Nullable
    private static ConstraintType getCompatibleConstraint(
            @NonNull ConstraintType first, @NonNull ConstraintType second) {
        if (first == second) {
            return first;
        }

        switch (second) {
            case ALIGN_LEFT:
            case LAYOUT_RIGHT_OF:
                switch (first) {
                    case LAYOUT_CENTER_HORIZONTAL:
                    case LAYOUT_LEFT_OF:
                    case ALIGN_LEFT:
                        return first;
                    case LAYOUT_CENTER_IN_PARENT:
                        return LAYOUT_CENTER_HORIZONTAL;
                }
                return null;

            case ALIGN_RIGHT:
            case LAYOUT_LEFT_OF:
                switch (first) {
                    case LAYOUT_CENTER_HORIZONTAL:
                    case ALIGN_RIGHT:
                    case LAYOUT_LEFT_OF:
                        return first;
                    case LAYOUT_CENTER_IN_PARENT:
                        return LAYOUT_CENTER_HORIZONTAL;
                }
                return null;

            case ALIGN_TOP:
            case LAYOUT_BELOW:
            case ALIGN_BASELINE:
                switch (first) {
                    case LAYOUT_CENTER_VERTICAL:
                    case ALIGN_TOP:
                    case LAYOUT_BELOW:
                    case ALIGN_BASELINE:
                        return first;
                    case LAYOUT_CENTER_IN_PARENT:
                        return LAYOUT_CENTER_VERTICAL;
                }
                return null;
            case ALIGN_BOTTOM:
            case LAYOUT_ABOVE:
                switch (first) {
                    case LAYOUT_CENTER_VERTICAL:
                    case ALIGN_BOTTOM:
                    case LAYOUT_ABOVE:
                    case ALIGN_BASELINE:
                        return first;
                    case LAYOUT_CENTER_IN_PARENT:
                        return LAYOUT_CENTER_VERTICAL;
                }
                return null;
        }

        return null;
    }
}
