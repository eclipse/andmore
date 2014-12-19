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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.RuleAction;

import java.util.List;

/**
 * Convenience implementation of {@link IMenuCallback} which can be used to set a
 * particular property to the new valueId or newValue passed from the {@link IMenuCallback}
 */
public class PropertyCallback implements IMenuCallback {
    private final List<? extends INode> mTargetNodes;
    private final String mUndoLabel;
    private final String mUri;
    private final String mAttribute;

    /**
     * Creates a new property callback.
     *
     * @param targetNodes the nodes to apply the property to, or null to use the
     *            nodes pass into the
     *            {@link #action(RuleAction, List, String, Boolean)} method.
     * @param undoLabel the label to use for the undo action
     * @param uri the attribute URI to apply
     * @param attribute the attribute name to apply
     */
    public PropertyCallback(List<? extends INode> targetNodes, String undoLabel,
            String uri, String attribute) {
        super();
        mTargetNodes = targetNodes;
        mUndoLabel = undoLabel;
        mUri = uri;
        mAttribute = attribute;
    }

    // ---- Implements IMenuCallback ----
    @Override
    public void action(@NonNull RuleAction action, @NonNull List<? extends INode> selectedNodes,
            final @Nullable String valueId, final @Nullable Boolean newValue) {
        if (mTargetNodes != null && mTargetNodes.size() > 0) {
            selectedNodes = mTargetNodes;
        }
        if (selectedNodes == null || selectedNodes.size() == 0) {
            return;
        }
        final List<? extends INode> nodes = selectedNodes;
        selectedNodes.get(0).editXml(mUndoLabel, new INodeHandler() {
            @Override
            public void handle(@NonNull INode n) {
                for (INode targetNode : nodes) {
                    if (valueId != null) {
                        targetNode.setAttribute(mUri, mAttribute, valueId);
                    } else {
                        assert newValue != null;
                        targetNode.setAttribute(mUri, mAttribute, Boolean.toString(newValue));
                    }
                }
            }
        });
    }
}
