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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_EMS;
import static com.android.SdkConstants.REQUEST_FOCUS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.RuleAction;

import java.util.List;

/**
 * An {@link IViewRule} for android.widget.EditText.
 */
public class EditTextRule extends BaseViewRule {

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (parent != null) {
            INode focus = findFocus(findRoot(parent));
            if (focus == null) {
                // Add <requestFocus>
                node.appendChild(REQUEST_FOCUS);
            }

            if (parent.getBounds().w >= 320) {
                node.setAttribute(ANDROID_URI, ATTR_EMS, "10"); //$NON-NLS-1$
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds a "Request Focus" menu item.
     */
    @Override
    public void addContextMenuActions(@NonNull List<RuleAction> actions,
            final @NonNull INode selectedNode) {
        super.addContextMenuActions(actions, selectedNode);

        final boolean hasFocus = hasFocus(selectedNode);
        final String label = hasFocus ? "Clear Focus" : "Request Focus";

        IMenuCallback onChange = new IMenuCallback() {
            @Override
            public void action(
                    @NonNull RuleAction menuAction,
                    @NonNull List<? extends INode> selectedNodes,
                    @Nullable String valueId,
                    @Nullable Boolean newValue) {
                selectedNode.editXml(label, new INodeHandler() {
                    @Override
                    public void handle(@NonNull INode node) {
                        INode focus = findFocus(findRoot(node));
                        if (focus != null && focus.getParent() != null) {
                            focus.getParent().removeChild(focus);
                        }
                        if (!hasFocus) {
                            node.appendChild(REQUEST_FOCUS);
                        }
                    }
                });
            }
        };

        actions.add(RuleAction.createAction("_setfocus", label, onChange, //$NON-NLS-1$
                null, 5, false /*supportsMultipleNodes*/));
        actions.add(RuleAction.createSeparator(7));
    }

    /** Returns true if the given node currently has focus */
    private static boolean hasFocus(INode node) {
        INode focus = findFocus(node);
        if (focus != null) {
            return focus.getParent() == node;
        }

        return false;
    }

    /** Returns the root/top level node in the view hierarchy that contains the given node */
    private static INode findRoot(INode node) {
        // First find the parent
        INode root = node;
        while (root != null) {
            INode parent = root.getParent();
            if (parent == null) {
                break;
            } else {
                root = parent;
            }
        }

        return root;
    }

    /** Finds the focus node (not the node containing focus, but the actual request focus node
     * under a given node */
    private static INode findFocus(INode node) {
        if (node.getFqcn().equals(REQUEST_FOCUS)) {
            return node;
        }

        for (INode child : node.getChildren()) {
            INode focus = findFocus(child);
            if (focus != null) {
                return focus;
            }
        }
        return null;
    }

}
