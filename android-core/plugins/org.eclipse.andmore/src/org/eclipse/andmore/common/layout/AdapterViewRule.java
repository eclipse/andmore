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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

/** Rule for AdapterView subclasses that don't have more specific rules */
public class AdapterViewRule extends BaseLayoutRule {
    @Override
    public DropFeedback onDropEnter(@NonNull INode targetNode, @Nullable Object targetView,
            @Nullable IDragElement[] elements) {
        // You are not allowed to insert children into AdapterViews; you must
        // use the dedicated addView methods etc dynamically
        DropFeedback dropFeedback = new DropFeedback(null,  new IFeedbackPainter() {
            @Override
            public void paint(@NonNull IGraphics gc, @NonNull INode node,
                    @NonNull DropFeedback feedback) {
                Rect b = node.getBounds();
                if (b.isValid()) {
                    gc.useStyle(DrawingStyle.DROP_RECIPIENT);
                    gc.drawRect(b);
                }
            }
        });
        String fqcn = targetNode.getFqcn();
        String name = fqcn.substring(fqcn.lastIndexOf('.') +1);
        dropFeedback.errorMessage = String.format(
                "%s cannot be configured via XML; add content to the AdapterView using Java code",
                name);
        dropFeedback.invalidTarget = true;
        return dropFeedback;
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        feedback.invalidTarget = true;
        return feedback;
    }
}
