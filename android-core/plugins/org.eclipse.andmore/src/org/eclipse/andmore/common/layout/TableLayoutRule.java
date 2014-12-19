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

import static com.android.SdkConstants.FQCN_TABLE_ROW;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.SegmentType;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link IViewRule} for android.widget.TableLayout.
 */
public class TableLayoutRule extends LinearLayoutRule {
    // A table is a linear layout, but with a few differences:
    // the default is vertical, not horizontal
    // The fill of all children should be wrap_content

    private static final String ACTION_ADD_ROW = "_addrow"; //$NON-NLS-1$
    private static final String ACTION_REMOVE_ROW = "_removerow"; //$NON-NLS-1$
    private static final URL ICON_ADD_ROW =
        TableLayoutRule.class.getResource("addrow.png"); //$NON-NLS-1$
    private static final URL ICON_REMOVE_ROW =
        TableLayoutRule.class.getResource("removerow.png"); //$NON-NLS-1$

    @Override
    protected boolean isVertical(INode node) {
        // Tables are always vertical
        return true;
    }

    @Override
    protected boolean supportsOrientation() {
        return false;
    }

    @Override
    public void onChildInserted(@NonNull INode child, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // Overridden to inhibit the setting of layout_width/layout_height since
        // it should always be match_parent
    }

    /**
     * Add an explicit "Add Row" action to the context menu
     */
    @Override
    public void addContextMenuActions(@NonNull List<RuleAction> actions,
            final @NonNull INode selectedNode) {
        super.addContextMenuActions(actions, selectedNode);

        IMenuCallback addTab = new IMenuCallback() {
            @Override
            public void action(
                    @NonNull RuleAction action,
                    @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId,
                    @Nullable Boolean newValue) {
                final INode node = selectedNode;
                INode newRow = node.appendChild(FQCN_TABLE_ROW);
                mRulesEngine.select(Collections.singletonList(newRow));
            }
        };
        actions.add(RuleAction.createAction("_addrow", "Add Row", addTab, null, 5, false)); //$NON-NLS-1$
    }

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);
        addTableLayoutActions(mRulesEngine, actions, parentNode, children);
    }

    /**
     * Adds layout actions to add and remove toolbar items
     */
    static void addTableLayoutActions(final IClientRulesEngine rulesEngine,
            List<RuleAction> actions, final INode parentNode,
            final List<? extends INode> children) {
        IMenuCallback actionCallback = new IMenuCallback() {
            @Override
            public void action(
                    final @NonNull RuleAction action,
                    @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId,
                    final @Nullable Boolean newValue) {
                parentNode.editXml("Add/Remove Table Row", new INodeHandler() {
                    @Override
                    public void handle(@NonNull INode n) {
                        if (action.getId().equals(ACTION_ADD_ROW)) {
                            // Determine the index of the selection, if any; if there is
                            // a selection, insert the row before the current row, otherwise
                            // append it to the table.
                            int index = -1;
                            INode[] rows = parentNode.getChildren();
                            if (children != null) {
                                findTableIndex:
                                for (INode child : children) {
                                    // Find direct child of table layout
                                    while (child != null && child.getParent() != parentNode) {
                                        child = child.getParent();
                                    }
                                    if (child != null) {
                                        // Compute index of direct child of table layout
                                        for (int i = 0; i < rows.length; i++) {
                                            if (rows[i] == child) {
                                                index = i;
                                                break findTableIndex;
                                            }
                                        }
                                    }
                                }
                            }
                            INode newRow;
                            if (index == -1) {
                                newRow = parentNode.appendChild(FQCN_TABLE_ROW);
                            } else {
                                newRow = parentNode.insertChildAt(FQCN_TABLE_ROW, index);
                            }
                            rulesEngine.select(Collections.singletonList(newRow));
                        } else if (action.getId().equals(ACTION_REMOVE_ROW)) {
                            // Find the direct children of the TableLayout to delete;
                            // this is necessary since TableRow might also use
                            // this implementation, so the parentNode is the true
                            // TableLayout but the children might be grand children.
                            Set<INode> targets = new HashSet<INode>();
                            for (INode child : children) {
                                while (child != null && child.getParent() != parentNode) {
                                    child = child.getParent();
                                }
                                if (child != null) {
                                    targets.add(child);
                                }
                            }
                            for (INode target : targets) {
                                parentNode.removeChild(target);
                            }
                        }
                    }
                });
            }
        };

        // Add Row
        actions.add(RuleAction.createSeparator(150));
        actions.add(RuleAction.createAction(ACTION_ADD_ROW, "Add Table Row", actionCallback,
                ICON_ADD_ROW, 160, false));

        // Remove Row (if something is selected)
        if (children != null && children.size() > 0) {
            actions.add(RuleAction.createAction(ACTION_REMOVE_ROW, "Remove Table Row",
                    actionCallback, ICON_REMOVE_ROW, 170, false));
        }
    }

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (insertType.isCreate()) {
            // Start the table with 4 rows
            for (int i = 0; i < 4; i++) {
                node.appendChild(FQCN_TABLE_ROW);
            }
        }
    }

    @Override
    public DropFeedback onResizeBegin(@NonNull INode child, @NonNull INode parent,
            @Nullable SegmentType horizontalEdge, @Nullable SegmentType verticalEdge,
            @Nullable Object childView, @Nullable Object parentView) {
        // Children of a table layout cannot set their widths (it is controlled by column
        // settings on the table). They can set their heights (though for TableRow, the
        // height is always wrap_content).
        if (horizontalEdge == null) { // Widths are edited by vertical edges.
            // The user is not editing a vertical height so don't allow resizing at all
            return null;
        }
        if (child.getFqcn().equals(FQCN_TABLE_ROW)) {
            // TableRows are always WRAP_CONTENT
            return null;
        }

        // Allow resizing heights only
        return super.onResizeBegin(child, parent, horizontalEdge, null /*verticalEdge*/,
                childView, parentView);
    }
}
