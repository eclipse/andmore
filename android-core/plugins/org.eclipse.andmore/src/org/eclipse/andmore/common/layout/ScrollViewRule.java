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
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.FQCN_LINEAR_LAYOUT;
import static com.android.SdkConstants.VALUE_VERTICAL;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

/**
 * An {@link IViewRule} for android.widget.ScrollView.
 */
public class ScrollViewRule extends FrameLayoutRule {

    @Override
    public void onChildInserted(@NonNull INode child, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onChildInserted(child, parent, insertType);

        // The child of the ScrollView should fill in both directions
        String fillParent = getFillParentValueName();
        child.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);
        child.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, fillParent);
    }

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (insertType.isCreate()) {
            // Insert a default linear layout (which will in turn be registered as
            // a child of this node and the create child method above will set its
            // fill parent attributes, its id, etc.
            INode linear = node.appendChild(FQCN_LINEAR_LAYOUT);
            linear.setAttribute(ANDROID_URI, ATTR_ORIENTATION, VALUE_VERTICAL);
        }
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        DropFeedback f = super.onDropMove(targetNode, elements, feedback, p);

        // ScrollViews only allow a single child
        if (targetNode.getChildren().length > 0) {
            f.invalidTarget = true;
        }
        return f;
    }

    @Override
    protected void drawFeedback(
            IGraphics gc,
            INode targetNode,
            IDragElement[] elements,
            DropFeedback feedback) {
        if (targetNode.getChildren().length > 0) {
            Rect b = targetNode.getBounds();
            if (b.isValid()) {
                gc.useStyle(DrawingStyle.DROP_RECIPIENT);
                gc.drawRect(b);
            }
        } else {
            super.drawFeedback(gc, targetNode, elements, feedback);
        }
    }
}
