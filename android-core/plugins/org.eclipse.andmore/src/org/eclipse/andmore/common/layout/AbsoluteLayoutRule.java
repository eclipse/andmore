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
import static com.android.SdkConstants.ATTR_LAYOUT_X;
import static com.android.SdkConstants.ATTR_LAYOUT_Y;
import static com.android.SdkConstants.VALUE_N_DP;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An {@link IViewRule} for android.widget.AbsoluteLayout and all its derived
 * classes.
 */
public class AbsoluteLayoutRule extends BaseLayoutRule {

    @Override
    public List<String> getSelectionHint(@NonNull INode parentNode, @NonNull INode childNode) {
        List<String> infos = new ArrayList<String>(2);
        infos.add("AbsoluteLayout is deprecated.");
        infos.add("Use other layouts instead.");
        return infos;
    }

    // ==== Drag'n'drop support ====
    // The AbsoluteLayout accepts any drag'n'drop anywhere on its surface.

    @Override
    public DropFeedback onDropEnter(@NonNull INode targetNode, @Nullable Object targetView,
            final @Nullable IDragElement[] elements) {

        if (elements.length == 0) {
            return null;
        }

        DropFeedback df = new DropFeedback(null, new IFeedbackPainter() {
            @Override
            public void paint(@NonNull IGraphics gc, @NonNull INode node,
                    @NonNull DropFeedback feedback) {
                // Paint callback for the AbsoluteLayout.
                // This is called by the canvas when a draw is needed.
                drawFeedback(gc, node, elements, feedback);
            }
        });
        df.errorMessage = "AbsoluteLayout is deprecated.";
        return df;
    }

    void drawFeedback(
            IGraphics gc,
            INode targetNode,
            IDragElement[] elements,
            DropFeedback feedback) {
        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        // Highlight the receiver
        gc.useStyle(DrawingStyle.DROP_RECIPIENT);
        gc.drawRect(b);

        // Get the drop point
        Point p = (Point) feedback.userData;

        if (p == null) {
            return;
        }

        int x = p.x;
        int y = p.y;

        Rect be = elements[0].getBounds();

        if (be.isValid()) {
            // At least the first element has a bound. Draw rectangles
            // for all dropped elements with valid bounds, offset at
            // the drop point.
            int offsetX = x - be.x + (feedback.dragBounds != null ? feedback.dragBounds.x : 0);
            int offsetY = y - be.y + (feedback.dragBounds != null ? feedback.dragBounds.y : 0);
            gc.useStyle(DrawingStyle.DROP_PREVIEW);
            for (IDragElement element : elements) {
                drawElement(gc, element, offsetX, offsetY);
            }
        } else {
            // We don't have bounds for new elements. In this case
            // just draw cross hairs to the drop point.
            gc.useStyle(DrawingStyle.GUIDELINE);
            gc.drawLine(x, b.y, x, b.y + b.h);
            gc.drawLine(b.x, y, b.x + b.w, y);

            // Use preview lines to indicate the bottom quadrant as well (to
            // indicate that you are looking at the top left position of the
            // drop, not the center for example)
            gc.useStyle(DrawingStyle.DROP_PREVIEW);
            gc.drawLine(x, y, b.x + b.w, y);
            gc.drawLine(x, y, x, b.y + b.h);
        }
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        // Update the data used by the DropFeedback.paintCallback above.
        feedback.userData = p;
        feedback.requestPaint = true;

        return feedback;
    }

    @Override
    public void onDropLeave(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback) {
        // Nothing to do.
    }

    @Override
    public void onDropped(final @NonNull INode targetNode, final @NonNull IDragElement[] elements,
            final @Nullable DropFeedback feedback, final @NonNull Point p) {

        final Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        // Collect IDs from dropped elements and remap them to new IDs
        // if this is a copy or from a different canvas.
        final Map<String, Pair<String, String>> idMap = getDropIdMap(targetNode, elements,
                feedback.isCopy || !feedback.sameCanvas);

        targetNode.editXml("Add elements to AbsoluteLayout", new INodeHandler() {
            @Override
            public void handle(@NonNull INode node) {
                boolean first = true;
                Point offset = null;

                // Now write the new elements.
                for (IDragElement element : elements) {
                    String fqcn = element.getFqcn();
                    Rect be = element.getBounds();

                    INode newChild = targetNode.appendChild(fqcn);

                    // Copy all the attributes, modifying them as needed.
                    addAttributes(newChild, element, idMap, DEFAULT_ATTR_FILTER);

                    int deltaX = (feedback.dragBounds != null ? feedback.dragBounds.x : 0);
                    int deltaY = (feedback.dragBounds != null ? feedback.dragBounds.y : 0);

                    int x = p.x - b.x + deltaX;
                    int y = p.y - b.y + deltaY;

                    if (first) {
                        first = false;
                        if (be.isValid()) {
                            offset = new Point(x - be.x, y - be.y);
                        }
                    } else if (offset != null && be.isValid()) {
                        x = offset.x + be.x;
                        y = offset.y + be.y;
                    } else {
                        x += 10;
                        y += be.isValid() ? be.h : 10;
                    }

                    double scale = feedback.dipScale;
                    if (scale != 1.0) {
                        x *= scale;
                        y *= scale;
                    }

                    newChild.setAttribute(ANDROID_URI, ATTR_LAYOUT_X,
                            String.format(VALUE_N_DP, x));
                    newChild.setAttribute(ANDROID_URI, ATTR_LAYOUT_Y,
                            String.format(VALUE_N_DP, y));

                    addInnerElements(newChild, element, idMap);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in this layout in order to let the top left coordinate be affected by
     * the resize operation too. In other words, dragging the top left corner to resize a
     * widget will not only change the size of the widget, it will also move it (though in
     * this case, the bottom right corner will stay fixed).
     */
    @Override
    protected void setNewSizeBounds(ResizeState resizeState, INode node, INode layout,
            Rect previousBounds, Rect newBounds, SegmentType horizontalEdge,
            SegmentType verticalEdge) {
        super.setNewSizeBounds(resizeState, node, layout, previousBounds, newBounds,
                horizontalEdge, verticalEdge);
        if (verticalEdge != null && newBounds.x != previousBounds.x) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_X,
                    String.format(VALUE_N_DP,
                            mRulesEngine.pxToDp(newBounds.x - node.getParent().getBounds().x)));
        }
        if (horizontalEdge != null && newBounds.y != previousBounds.y) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_Y,
                    String.format(VALUE_N_DP,
                            mRulesEngine.pxToDp(newBounds.y - node.getParent().getBounds().y)));
        }
    }

    @Override
    protected String getResizeUpdateMessage(ResizeState resizeState, INode child, INode parent,
            Rect newBounds, SegmentType horizontalEdge, SegmentType verticalEdge) {
        Rect parentBounds = parent.getBounds();
        if (horizontalEdge == SegmentType.BOTTOM && verticalEdge == SegmentType.RIGHT) {
            return super.getResizeUpdateMessage(resizeState, child, parent, newBounds,
                    horizontalEdge, verticalEdge);
        }
        return String.format("x=%d, y=%d\nwidth=%s, height=%s",
                mRulesEngine.pxToDp(newBounds.x - parentBounds.x),
                mRulesEngine.pxToDp(newBounds.y - parentBounds.y),
                resizeState.getWidthAttribute(), resizeState.getHeightAttribute());
    }
}
