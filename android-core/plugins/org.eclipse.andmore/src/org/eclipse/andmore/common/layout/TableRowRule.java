/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static com.android.SdkConstants.FQCN_TABLE_LAYOUT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.SegmentType;

import java.util.List;

/**
 * An {@link IViewRule} for android.widget.TableRow.
 */
public class TableRowRule extends LinearLayoutRule {
    @Override
    protected boolean isVertical(INode node) {
        return false;
    }

    @Override
    protected boolean supportsOrientation() {
        return false;
    }

    @Override
    public void onChildInserted(@NonNull INode child, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // Overridden to inhibit the setting of layout_width/layout_height since
        // the table row will enforce match_parent and wrap_content for width and height
        // respectively.
    }

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);

        // Also apply table-specific actions on the table row such that you can
        // select something in a table row and still get offered actions on the surrounding
        // table.
        if (children != null) {
            INode grandParent = parentNode.getParent();
            if (grandParent != null && grandParent.getFqcn().equals(FQCN_TABLE_LAYOUT)) {
                TableLayoutRule.addTableLayoutActions(mRulesEngine, actions, grandParent,
                        children);
            }
        }
    }

    @Override
    public DropFeedback onResizeBegin(@NonNull INode child, @NonNull INode parent,
            @Nullable SegmentType horizontalEdge, @Nullable SegmentType verticalEdge,
            @Nullable Object childView, @Nullable Object parentView) {
        // No resizing in TableRows; the width is *always* match_parent and the height is
        // *always* wrap_content.
        return null;
    }
}
