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
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewMetadata;
import com.android.ide.common.api.IViewMetadata.FillPreference;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.utils.Pair;

import java.util.List;
import java.util.Map;

/**
 * An {@link IViewRule} for android.widget.FrameLayout and all its derived
 * classes.
 */
public class FrameLayoutRule extends BaseLayoutRule {

    // ==== Drag'n'drop support ====
    // The FrameLayout accepts any drag'n'drop anywhere on its surface.

    @Override
    public DropFeedback onDropEnter(@NonNull INode targetNode, @Nullable Object targetView,
            final @Nullable IDragElement[] elements) {
        if (elements.length == 0) {
            return null;
        }

        return new DropFeedback(null, new IFeedbackPainter() {
            @Override
            public void paint(@NonNull IGraphics gc, @NonNull INode node,
                    @NonNull DropFeedback feedback) {
                drawFeedback(gc, node, elements, feedback);
            }
        });
    }

    protected void drawFeedback(
            IGraphics gc,
            INode targetNode,
            IDragElement[] elements,
            DropFeedback feedback) {
        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        gc.useStyle(DrawingStyle.DROP_RECIPIENT);
        gc.drawRect(b);

        // Get the drop point
        Point p = (Point) feedback.userData;

        if (p == null) {
            return;
        }

        Rect be = elements[0].getBounds();

        gc.useStyle(DrawingStyle.DROP_PREVIEW);
        if (be.isValid()) {
            // At least the first element has a bound. Draw rectangles
            // for all dropped elements with valid bounds, offset at
            // (0,0)
            for (IDragElement it : elements) {
                Rect currBounds = it.getBounds();
                if (currBounds.isValid()) {
                    int offsetX = b.x - currBounds.x;
                    int offsetY = b.y - currBounds.y;
                    drawElement(gc, it, offsetX, offsetY);
                }
            }
        } else {
            // We don't have bounds for new elements. In this case
            // just draw insert lines indicating the top left corner where
            // the item will be placed

            // +1: Place lines fully within the view (the stroke width is 2) to
            // make
            // it even more visually obvious
            gc.drawLine(b.x + 1, b.y, b.x + 1, b.y + b.h);
            gc.drawLine(b.x, b.y + 1, b.x + b.w, b.y + 1);
        }
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        feedback.userData = p;
        feedback.requestPaint = true;
        return feedback;
    }

    @Override
    public void onDropLeave(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback) {
        // ignore
    }

    @Override
    public void onDropped(final @NonNull INode targetNode, final @NonNull IDragElement[] elements,
            final @Nullable DropFeedback feedback, final @NonNull Point p) {
        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        // Collect IDs from dropped elements and remap them to new IDs
        // if this is a copy or from a different canvas.
        final Map<String, Pair<String, String>> idMap = getDropIdMap(targetNode, elements,
                feedback.isCopy || !feedback.sameCanvas);

        targetNode.editXml("Add elements to FrameLayout", new INodeHandler() {

            @Override
            public void handle(@NonNull INode node) {

                // Now write the new elements.
                for (IDragElement element : elements) {
                    String fqcn = element.getFqcn();

                    INode newChild = targetNode.appendChild(fqcn);

                    // Copy all the attributes, modifying them as needed.
                    addAttributes(newChild, element, idMap, DEFAULT_ATTR_FILTER);

                    addInnerElements(newChild, element, idMap);
                }
            }
        });
    }

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);
        actions.add(RuleAction.createSeparator(25));
        actions.add(createMarginAction(parentNode, children));
        if (children != null && children.size() > 0) {
            actions.add(createGravityAction(children, ATTR_LAYOUT_GRAVITY));
        }
    }

    @Override
    public void onChildInserted(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // Look at the fill preferences and fill embedded layouts etc
        String fqcn = node.getFqcn();
        IViewMetadata metadata = mRulesEngine.getMetadata(fqcn);
        if (metadata != null) {
            FillPreference fill = metadata.getFillPreference();
            String fillParent = getFillParentValueName();
            if (fill.fillHorizontally(true)) {
                node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);
            }
            if (fill.fillVertically(false)) {
                node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, fillParent);
            }
        }
    }
}
