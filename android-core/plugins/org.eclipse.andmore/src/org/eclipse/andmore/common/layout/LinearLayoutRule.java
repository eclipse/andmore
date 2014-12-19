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
import static com.android.SdkConstants.ATTR_BASELINE_ALIGNED;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ATTR_WEIGHT_SUM;
import static com.android.SdkConstants.VALUE_1;
import static com.android.SdkConstants.VALUE_HORIZONTAL;
import static com.android.SdkConstants.VALUE_VERTICAL;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.VALUE_ZERO_DP;
import static org.eclipse.andmore.AdtUtils.formatFloatAttribute;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewMetadata;
import com.android.ide.common.api.IViewMetadata.FillPreference;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.RuleAction.Choices;
import com.android.ide.common.api.SegmentType;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.AdtPlugin;

/**
 * An {@link IViewRule} for android.widget.LinearLayout and all its derived
 * classes.
 */
public class LinearLayoutRule extends BaseLayoutRule {
    private static final String ACTION_ORIENTATION = "_orientation"; //$NON-NLS-1$
    private static final String ACTION_WEIGHT = "_weight"; //$NON-NLS-1$
    private static final String ACTION_DISTRIBUTE = "_distribute"; //$NON-NLS-1$
    private static final String ACTION_BASELINE = "_baseline"; //$NON-NLS-1$
    private static final String ACTION_CLEAR = "_clear"; //$NON-NLS-1$
    private static final String ACTION_DOMINATE = "_dominate"; //$NON-NLS-1$

    private static final URL ICON_HORIZONTAL =
        LinearLayoutRule.class.getResource("hlinear.png"); //$NON-NLS-1$
    private static final URL ICON_VERTICAL =
        LinearLayoutRule.class.getResource("vlinear.png"); //$NON-NLS-1$
    private static final URL ICON_WEIGHTS =
        LinearLayoutRule.class.getResource("weights.png"); //$NON-NLS-1$
    private static final URL ICON_DISTRIBUTE =
        LinearLayoutRule.class.getResource("distribute.png"); //$NON-NLS-1$
    private static final URL ICON_BASELINE =
        LinearLayoutRule.class.getResource("baseline.png"); //$NON-NLS-1$
    private static final URL ICON_CLEAR_WEIGHTS =
            LinearLayoutRule.class.getResource("clearweights.png"); //$NON-NLS-1$
    private static final URL ICON_DOMINATE =
            LinearLayoutRule.class.getResource("allweight.png"); //$NON-NLS-1$

    /**
     * Returns the current orientation, regardless of whether it has been defined in XML
     *
     * @param node The LinearLayout to look up the orientation for
     * @return "horizontal" or "vertical" depending on the current orientation of the
     *         linear layout
     */
    private String getCurrentOrientation(final INode node) {
        String orientation = node.getStringAttr(ANDROID_URI, ATTR_ORIENTATION);
        if (orientation == null || orientation.length() == 0) {
            orientation = VALUE_HORIZONTAL;
        }
        return orientation;
    }

    /**
     * Returns true if the given node represents a vertical linear layout.
     * @param node the node to check layout orientation for
     * @return true if the layout is in vertical mode, otherwise false
     */
    protected boolean isVertical(INode node) {
        // Horizontal is the default, so if no value is specified it is horizontal.
        return VALUE_VERTICAL.equals(node.getStringAttr(ANDROID_URI,
                ATTR_ORIENTATION));
    }

    /**
     * Returns true if this LinearLayout supports switching orientation.
     *
     * @return true if this layout supports orientations
     */
    protected boolean supportsOrientation() {
        return true;
    }

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);
        if (supportsOrientation()) {
            Choices action = RuleAction.createChoices(
                    ACTION_ORIENTATION, "Orientation",  //$NON-NLS-1$
                    new PropertyCallback(Collections.singletonList(parentNode),
                            "Change LinearLayout Orientation",
                            ANDROID_URI, ATTR_ORIENTATION),
                    Arrays.<String>asList("Set Horizontal Orientation","Set Vertical Orientation"),
                    Arrays.<URL>asList(ICON_HORIZONTAL, ICON_VERTICAL),
                    Arrays.<String>asList("horizontal", "vertical"),
                    getCurrentOrientation(parentNode),
                    null /* icon */,
                    -10,
                    false /* supportsMultipleNodes */
            );
            action.setRadio(true);
            actions.add(action);
        }
        if (!isVertical(parentNode)) {
            String current = parentNode.getStringAttr(ANDROID_URI, ATTR_BASELINE_ALIGNED);
            boolean isAligned =  current == null || Boolean.valueOf(current);
            actions.add(RuleAction.createToggle(ACTION_BASELINE, "Toggle Baseline Alignment",
                    isAligned,
                    new PropertyCallback(Collections.singletonList(parentNode),
                            "Change Baseline Alignment",
                            ANDROID_URI, ATTR_BASELINE_ALIGNED), // TODO: Also set index?
                    ICON_BASELINE, 38, false));
        }

        // Gravity
        if (children != null && children.size() > 0) {
            actions.add(RuleAction.createSeparator(35));

            // Margins
            actions.add(createMarginAction(parentNode, children));

            // Gravity
            actions.add(createGravityAction(children, ATTR_LAYOUT_GRAVITY));

            // Weights
            IMenuCallback actionCallback = new IMenuCallback() {
                @Override
                public void action(
                        final @NonNull RuleAction action,
                        @NonNull List<? extends INode> selectedNodes,
                        final @Nullable String valueId,
                        final @Nullable Boolean newValue) {
                    parentNode.editXml("Change Weight", new INodeHandler() {
                        @Override
                        public void handle(@NonNull INode n) {
                            String id = action.getId();
                            if (id.equals(ACTION_WEIGHT)) {
                                String weight =
                                    children.get(0).getStringAttr(ANDROID_URI, ATTR_LAYOUT_WEIGHT);
                                if (weight == null || weight.length() == 0) {
                                    weight = "0.0"; //$NON-NLS-1$
                                }
                                weight = mRulesEngine.displayInput("Enter Weight Value:", weight,
                                        null);
                                if (weight != null) {
                                    if (weight.isEmpty()) {
                                        weight = null; // remove attribute
                                    }
                                    for (INode child : children) {
                                        child.setAttribute(ANDROID_URI,
                                                ATTR_LAYOUT_WEIGHT, weight);
                                    }
                                }
                            } else if (id.equals(ACTION_DISTRIBUTE)) {
                                distributeWeights(parentNode, parentNode.getChildren());
                            } else if (id.equals(ACTION_CLEAR)) {
                                clearWeights(parentNode);
                            } else if (id.equals(ACTION_CLEAR) || id.equals(ACTION_DOMINATE)) {
                                clearWeights(parentNode);
                                distributeWeights(parentNode,
                                        children.toArray(new INode[children.size()]));
                            } else {
                                assert id.equals(ACTION_BASELINE);
                            }
                        }
                    });
                }
            };
            actions.add(RuleAction.createSeparator(50));
            actions.add(RuleAction.createAction(ACTION_DISTRIBUTE, "Distribute Weights Evenly",
                    actionCallback, ICON_DISTRIBUTE, 60, false /*supportsMultipleNodes*/));
            actions.add(RuleAction.createAction(ACTION_DOMINATE, "Assign All Weight",
                    actionCallback, ICON_DOMINATE, 70, false));
            actions.add(RuleAction.createAction(ACTION_WEIGHT, "Change Layout Weight",
                    actionCallback, ICON_WEIGHTS, 80, false));
            actions.add(RuleAction.createAction(ACTION_CLEAR, "Clear All Weights",
                     actionCallback, ICON_CLEAR_WEIGHTS, 90, false));
        }
    }

    private void distributeWeights(INode parentNode, INode[] targets) {
        // Any XML to get weight sum?
        String weightSum = parentNode.getStringAttr(ANDROID_URI,
                ATTR_WEIGHT_SUM);
        double sum = -1.0;
        if (weightSum != null) {
            // Distribute
            try {
                sum = Double.parseDouble(weightSum);
            } catch (NumberFormatException nfe) {
                // Just keep using the default
            }
        }
        int numTargets = targets.length;
        double share;
        if (sum <= 0.0) {
            // The sum will be computed from the children, so just
            // use arbitrary amount
            share = 1.0;
        } else {
            share = sum / numTargets;
        }
        String value = formatFloatAttribute((float) share);
        String sizeAttribute = isVertical(parentNode) ?
                ATTR_LAYOUT_HEIGHT : ATTR_LAYOUT_WIDTH;
        for (INode target : targets) {
            target.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, value);
            // Also set the width/height to 0dp to ensure actual equal
            // size (without this, only the remaining space is
            // distributed)
            if (VALUE_WRAP_CONTENT.equals(target.getStringAttr(ANDROID_URI, sizeAttribute))) {
                target.setAttribute(ANDROID_URI, sizeAttribute, VALUE_ZERO_DP);
            }
        }
    }

    private void clearWeights(INode parentNode) {
        // Clear attributes
        String sizeAttribute = isVertical(parentNode)
                ? ATTR_LAYOUT_HEIGHT : ATTR_LAYOUT_WIDTH;
        for (INode target : parentNode.getChildren()) {
            target.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, null);
            String size = target.getStringAttr(ANDROID_URI, sizeAttribute);
            if (size != null && size.startsWith("0")) { //$NON-NLS-1$
                target.setAttribute(ANDROID_URI, sizeAttribute, VALUE_WRAP_CONTENT);
            }
        }
    }

    // ==== Drag'n'drop support ====

    @Override
    public DropFeedback onDropEnter(final @NonNull INode targetNode, @Nullable Object targetView,
            final @Nullable IDragElement[] elements) {

        if (elements.length == 0) {
            return null;
        }

        Rect bn = targetNode.getBounds();
        if (!bn.isValid()) {
            return null;
        }

        boolean isVertical = isVertical(targetNode);

        // Prepare a list of insertion points: X coords for horizontal, Y for
        // vertical.
        List<MatchPos> indexes = new ArrayList<MatchPos>();

        int last = isVertical ? bn.y : bn.x;
        int pos = 0;
        boolean lastDragged = false;
        int selfPos = -1;
        for (INode it : targetNode.getChildren()) {
            Rect bc = it.getBounds();
            if (bc.isValid()) {
                // First see if this node looks like it's the same as one of the
                // *dragged* bounds
                boolean isDragged = false;
                for (IDragElement element : elements) {
                    // This tries to determine if an INode corresponds to an
                    // IDragElement, by comparing their bounds.
                    if (element.isSame(it)) {
                        isDragged = true;
                        break;
                    }
                }

                // We don't want to insert drag positions before or after the
                // element that is itself being dragged. However, we -do- want
                // to insert a match position here, at the center, such that
                // when you drag near its current position we show a match right
                // where it's already positioned.
                if (isDragged) {
                    int v = isVertical ? bc.y + (bc.h / 2) : bc.x + (bc.w / 2);
                    selfPos = pos;
                    indexes.add(new MatchPos(v, pos++));
                } else if (lastDragged) {
                    // Even though we don't want to insert a match below, we
                    // need to increment the index counter such that subsequent
                    // lines know their correct index in the child list.
                    pos++;
                } else {
                    // Add an insertion point between the last point and the
                    // start of this child
                    int v = isVertical ? bc.y : bc.x;
                    v = (last + v) / 2;
                    indexes.add(new MatchPos(v, pos++));
                }

                last = isVertical ? (bc.y + bc.h) : (bc.x + bc.w);
                lastDragged = isDragged;
            } else {
                // We still have to count this position even if it has no bounds, or
                // subsequent children will be inserted at the wrong place
                pos++;
            }
        }

        // Finally add an insert position after all the children - unless of
        // course we happened to be dragging the last element
        if (!lastDragged) {
            int v = last + 1;
            indexes.add(new MatchPos(v, pos));
        }

        int posCount = targetNode.getChildren().length + 1;
        return new DropFeedback(new LinearDropData(indexes, posCount, isVertical, selfPos),
                new IFeedbackPainter() {

                    @Override
                    public void paint(@NonNull IGraphics gc, @NonNull INode node,
                            @NonNull DropFeedback feedback) {
                        // Paint callback for the LinearLayout. This is called
                        // by the canvas when a draw is needed.
                        drawFeedback(gc, node, elements, feedback);
                    }
                });
    }

    void drawFeedback(IGraphics gc, INode node, IDragElement[] elements, DropFeedback feedback) {
        Rect b = node.getBounds();
        if (!b.isValid()) {
            return;
        }

        // Highlight the receiver
        gc.useStyle(DrawingStyle.DROP_RECIPIENT);
        gc.drawRect(b);

        gc.useStyle(DrawingStyle.DROP_ZONE);

        LinearDropData data = (LinearDropData) feedback.userData;
        boolean isVertical = data.isVertical();
        int selfPos = data.getSelfPos();

        for (MatchPos it : data.getIndexes()) {
            int i = it.getDistance();
            int pos = it.getPosition();
            // Don't show insert drop zones for "self"-index since that one goes
            // right through the center of the widget rather than in a sibling
            // position
            if (pos != selfPos) {
                if (isVertical) {
                    // draw horizontal lines
                    gc.drawLine(b.x, i, b.x + b.w, i);
                } else {
                    // draw vertical lines
                    gc.drawLine(i, b.y, i, b.y + b.h);
                }
            }
        }

        Integer currX = data.getCurrX();
        Integer currY = data.getCurrY();

        if (currX != null && currY != null) {
            gc.useStyle(DrawingStyle.DROP_ZONE_ACTIVE);

            int x = currX;
            int y = currY;

            Rect be = elements[0].getBounds();

            // Draw a clear line at the closest drop zone (unless we're over the
            // dragged element itself)
            if (data.getInsertPos() != selfPos || selfPos == -1) {
                gc.useStyle(DrawingStyle.DROP_PREVIEW);
                if (data.getWidth() != null) {
                    int width = data.getWidth();
                    int fromX = x - width / 2;
                    int toX = x + width / 2;
                    gc.drawLine(fromX, y, toX, y);
                } else if (data.getHeight() != null) {
                    int height = data.getHeight();
                    int fromY = y - height / 2;
                    int toY = y + height / 2;
                    gc.drawLine(x, fromY, x, toY);
                }
            }

            if (be.isValid()) {
                boolean isLast = data.isLastPosition();

                // At least the first element has a bound. Draw rectangles for
                // all dropped elements with valid bounds, offset at the drop
                // point.
                int offsetX;
                int offsetY;
                if (isVertical) {
                    offsetX = b.x - be.x;
                    offsetY = currY - be.y - (isLast ? 0 : (be.h / 2));

                } else {
                    offsetX = currX - be.x - (isLast ? 0 : (be.w / 2));
                    offsetY = b.y - be.y;
                }

                gc.useStyle(DrawingStyle.DROP_PREVIEW);
                for (IDragElement element : elements) {
                    Rect bounds = element.getBounds();
                    if (bounds.isValid() && (bounds.w > b.w || bounds.h > b.h) &&
                            node.getChildren().length == 0) {
                        // The bounds of the child does not fully fit inside the target.
                        // Limit the bounds to the layout bounds (but only when there
                        // are no children, since otherwise positioning around the existing
                        // children gets difficult)
                        final int px, py, pw, ph;
                        if (bounds.w > b.w) {
                            px = b.x;
                            pw = b.w;
                        } else {
                            px = bounds.x + offsetX;
                            pw = bounds.w;
                        }
                        if (bounds.h > b.h) {
                            py = b.y;
                            ph = b.h;
                        } else {
                            py = bounds.y + offsetY;
                            ph = bounds.h;
                        }
                        Rect within = new Rect(px, py, pw, ph);
                        gc.drawRect(within);
                    } else {
                        drawElement(gc, element, offsetX, offsetY);
                    }
                }
            }
        }
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return feedback;
        }

        LinearDropData data = (LinearDropData) feedback.userData;
        boolean isVertical = data.isVertical();

        int bestDist = Integer.MAX_VALUE;
        int bestIndex = Integer.MIN_VALUE;
        Integer bestPos = null;

        for (MatchPos index : data.getIndexes()) {
            int i = index.getDistance();
            int pos = index.getPosition();
            int dist = (isVertical ? p.y : p.x) - i;
            if (dist < 0)
                dist = -dist;
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
                bestPos = pos;
                if (bestDist <= 0)
                    break;
            }
        }

        if (bestIndex != Integer.MIN_VALUE) {
            Integer oldX = data.getCurrX();
            Integer oldY = data.getCurrY();

            if (isVertical) {
                data.setCurrX(b.x + b.w / 2);
                data.setCurrY(bestIndex);
                data.setWidth(b.w);
                data.setHeight(null);
            } else {
                data.setCurrX(bestIndex);
                data.setCurrY(b.y + b.h / 2);
                data.setWidth(null);
                data.setHeight(b.h);
            }

            data.setInsertPos(bestPos);

            feedback.requestPaint = !equals(oldX, data.getCurrX())
                    || !equals(oldY, data.getCurrY());
        }

        return feedback;
    }

    private static boolean equals(Integer i1, Integer i2) {
        if (i1 == i2) {
            return true;
        } else if (i1 != null) {
            return i1.equals(i2);
        } else {
            // We know i2 != null
            return i2.equals(i1);
        }
    }

    @Override
    public void onDropLeave(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback) {
        // ignore
    }

    @Override
    public void onDropped(final @NonNull INode targetNode, final @NonNull IDragElement[] elements,
            final @Nullable DropFeedback feedback, final @NonNull Point p) {

        LinearDropData data = (LinearDropData) feedback.userData;
        final int initialInsertPos = data.getInsertPos();
        insertAt(targetNode, elements, feedback.isCopy || !feedback.sameCanvas, initialInsertPos);
    }

    @Override
    public void onChildInserted(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        if (insertType == InsertType.MOVE_WITHIN) {
            // Don't adjust widths/heights/weights when just moving within a single
            // LinearLayout
            return;
        }

        // Attempt to set fill-properties on newly added views such that for example,
        // in a vertical layout, a text field defaults to filling horizontally, but not
        // vertically.
        String fqcn = node.getFqcn();
        IViewMetadata metadata = mRulesEngine.getMetadata(fqcn);
        if (metadata != null) {
            boolean vertical = isVertical(parent);
            FillPreference fill = metadata.getFillPreference();
            String fillParent = getFillParentValueName();
            if (fill.fillHorizontally(vertical)) {
                node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);
            } else if (!vertical && fill == FillPreference.WIDTH_IN_VERTICAL) {
                // In a horizontal layout, make views that would fill horizontally in a
                // vertical layout have a non-zero weight instead. This will make the item
                // fill but only enough to allow other views to be shown as well.
                // (However, for drags within the same layout we do not touch
                // the weight, since it might already have been tweaked to a particular
                // value)
                node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, VALUE_1);
            }
            if (fill.fillVertically(vertical)) {
                node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, fillParent);
            }
        }

        // If you insert into a layout that already is using layout weights,
        // and all the layout weights are the same (nonzero) value, then use
        // the same weight for this new layout as well. Also duplicate the 0dip/0px/0dp
        // sizes, if used.
        boolean duplicateWeight = true;
        boolean duplicate0dip = true;
        String sameWeight = null;
        String sizeAttribute = isVertical(parent) ? ATTR_LAYOUT_HEIGHT : ATTR_LAYOUT_WIDTH;
        for (INode target : parent.getChildren()) {
            if (target == node) {
                continue;
            }
            String weight = target.getStringAttr(ANDROID_URI, ATTR_LAYOUT_WEIGHT);
            if (weight == null || weight.length() == 0) {
                duplicateWeight = false;
                break;
            } else if (sameWeight != null && !sameWeight.equals(weight)) {
                duplicateWeight = false;
            } else {
                sameWeight = weight;
            }
            String size = target.getStringAttr(ANDROID_URI, sizeAttribute);
            if (size != null && !size.startsWith("0")) { //$NON-NLS-1$
                duplicate0dip = false;
                break;
            }
        }
        if (duplicateWeight && sameWeight != null) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, sameWeight);
            if (duplicate0dip) {
                node.setAttribute(ANDROID_URI, sizeAttribute, VALUE_ZERO_DP);
            }
        }
    }

    /** A possible match position */
    private static class MatchPos {
        /** The pixel distance */
        private int mDistance;
        /** The position among siblings */
        private int mPosition;

        public MatchPos(int distance, int position) {
            mDistance = distance;
            mPosition = position;
        }

        @Override
        public String toString() {
            return "MatchPos [distance=" + mDistance //$NON-NLS-1$
                    + ", position=" + mPosition      //$NON-NLS-1$
                    + "]";                           //$NON-NLS-1$
        }

        private int getDistance() {
            return mDistance;
        }

        private int getPosition() {
            return mPosition;
        }
    }

    private static class LinearDropData {
        /** Vertical layout? */
        private final boolean mVertical;

        /** Insert points (pixels + index) */
        private final List<MatchPos> mIndexes;

        /** Number of insert positions in the target node */
        private final int mNumPositions;

        /** Current marker X position */
        private Integer mCurrX;

        /** Current marker Y position */
        private Integer mCurrY;

        /** Position of the dragged element in this layout (or
            -1 if the dragged element is from elsewhere) */
        private final int mSelfPos;

        /** Current drop insert index (-1 for "at the end") */
        private int mInsertPos = -1;

        /** width of match line if it's a horizontal one */
        private Integer mWidth;

        /** height of match line if it's a vertical one */
        private Integer mHeight;

        public LinearDropData(List<MatchPos> indexes, int numPositions,
                boolean isVertical, int selfPos) {
            mIndexes = indexes;
            mNumPositions = numPositions;
            mVertical = isVertical;
            mSelfPos = selfPos;
        }

        @Override
        public String toString() {
            return "LinearDropData [currX=" + mCurrX //$NON-NLS-1$
                    + ", currY=" + mCurrY //$NON-NLS-1$
                    + ", height=" + mHeight //$NON-NLS-1$
                    + ", indexes=" + mIndexes //$NON-NLS-1$
                    + ", insertPos=" + mInsertPos //$NON-NLS-1$
                    + ", isVertical=" + mVertical //$NON-NLS-1$
                    + ", selfPos=" + mSelfPos //$NON-NLS-1$
                    + ", width=" + mWidth //$NON-NLS-1$
                    + "]"; //$NON-NLS-1$
        }

        private boolean isVertical() {
            return mVertical;
        }

        private void setCurrX(Integer currX) {
            mCurrX = currX;
        }

        private Integer getCurrX() {
            return mCurrX;
        }

        private void setCurrY(Integer currY) {
            mCurrY = currY;
        }

        private Integer getCurrY() {
            return mCurrY;
        }

        private int getSelfPos() {
            return mSelfPos;
        }

        private void setInsertPos(int insertPos) {
            mInsertPos = insertPos;
        }

        private int getInsertPos() {
            return mInsertPos;
        }

        private List<MatchPos> getIndexes() {
            return mIndexes;
        }

        private void setWidth(Integer width) {
            mWidth = width;
        }

        private Integer getWidth() {
            return mWidth;
        }

        private void setHeight(Integer height) {
            mHeight = height;
        }

        private Integer getHeight() {
            return mHeight;
        }

        /**
         * Returns true if we are inserting into the last position
         *
         * @return true if we are inserting into the last position
         */
        public boolean isLastPosition() {
            return mInsertPos == mNumPositions - 1;
        }
    }

    /** Custom resize state used during linear layout resizing */
    private class LinearResizeState extends ResizeState {
        /** Whether the node should be assigned a new weight */
        public boolean useWeight;
        /** Weight sum to be applied to the parent */
        private float mNewWeightSum;
        /** The weight to be set on the node (provided {@link #useWeight} is true) */
        private float mWeight;
        /** Map from nodes to preferred bounds of nodes where the weights have been cleared */
        public final Map<INode, Rect> unweightedSizes;
        /** Total required size required by the siblings <b>without</b> weights */
        public int totalLength;
        /** List of nodes which should have their weights cleared */
        public List<INode> mClearWeights;

        private LinearResizeState(BaseLayoutRule rule, INode layout, Object layoutView,
                INode node) {
            super(rule, layout, layoutView, node);

            unweightedSizes = mRulesEngine.measureChildren(layout,
                    new IClientRulesEngine.AttributeFilter() {
                        @Override
                        public String getAttribute(@NonNull INode n, @Nullable String namespace,
                                @NonNull String localName) {
                            // Clear out layout weights; we need to measure the unweighted sizes
                            // of the children
                            if (ATTR_LAYOUT_WEIGHT.equals(localName)
                                    && SdkConstants.NS_RESOURCES.equals(namespace)) {
                                return ""; //$NON-NLS-1$
                            }

                            return null;
                        }
                    });

            // Compute total required size required by the siblings *without* weights
            totalLength = 0;
            final boolean isVertical = isVertical(layout);
            for (Map.Entry<INode, Rect> entry : unweightedSizes.entrySet()) {
                Rect preferredSize = entry.getValue();
                if (isVertical) {
                    totalLength += preferredSize.h;
                } else {
                    totalLength += preferredSize.w;
                }
            }
        }

        /** Resets the computed state */
        void reset() {
            mNewWeightSum = -1;
            useWeight = false;
            mClearWeights = null;
        }

        /** Sets a weight to be applied to the node */
        void setWeight(float weight) {
            useWeight = true;
            mWeight = weight;
        }

        /** Sets a weight sum to be applied to the parent layout */
        void setWeightSum(float weightSum) {
            mNewWeightSum = weightSum;
        }

        /** Marks that the given node should be cleared when applying the new size */
        void clearWeight(INode n) {
            if (mClearWeights == null) {
                mClearWeights = new ArrayList<INode>();
            }
            mClearWeights.add(n);
        }

        /** Applies the state to the nodes */
        public void apply() {
            assert useWeight;

            String value = mWeight > 0 ? formatFloatAttribute(mWeight) : null;
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, value);

            if (mClearWeights != null) {
                for (INode n : mClearWeights) {
                    if (getWeight(n) > 0.0f) {
                        n.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, null);
                    }
                }
            }

            if (mNewWeightSum > 0.0) {
                layout.setAttribute(ANDROID_URI, ATTR_WEIGHT_SUM,
                        formatFloatAttribute(mNewWeightSum));
            }
        }
    }

    @Override
    protected ResizeState createResizeState(INode layout, Object layoutView, INode node) {
        return new LinearResizeState(this, layout, layoutView, node);
    }

    protected void updateResizeState(LinearResizeState resizeState, final INode node, INode layout,
            Rect oldBounds, Rect newBounds, SegmentType horizontalEdge,
            SegmentType verticalEdge) {
        // Update the resize state.
        // This method attempts to compute a new layout weight to be used in the direction
        // of the linear layout. If the superclass has already determined that we can snap to
        // a wrap_content or match_parent boundary, we prefer that. Otherwise, we attempt to
        // compute a layout weight - which can fail if the size is too big (not enough room),
        // or if the size is too small (smaller than the natural width of the node), and so on.
        // In that case this method just aborts, which will leave the resize state object
        // in such a state that it will call the superclass to resize instead, which will fall
        // back to device independent pixel sizing.
        resizeState.reset();

        if (oldBounds.equals(newBounds)) {
            return;
        }

        // If we're setting the width/height to wrap_content/match_parent in the dimension of the
        // linear layout, then just apply wrap_content and clear weights.
        boolean isVertical = isVertical(layout);
        if (!isVertical && verticalEdge != null) {
            if (resizeState.wrapWidth || resizeState.fillWidth) {
                resizeState.clearWeight(node);
                return;
            }
            if (newBounds.w == oldBounds.w) {
                return;
            }
        }

        if (isVertical && horizontalEdge != null) {
            if (resizeState.wrapHeight || resizeState.fillHeight) {
                resizeState.clearWeight(node);
                return;
            }
            if (newBounds.h == oldBounds.h) {
                return;
            }
        }

        // Compute weight sum
        float sum = getWeightSum(layout);
        if (sum <= 0.0f) {
            sum = 1.0f;
            resizeState.setWeightSum(sum);
        }

        // If the new size of the node is smaller than its preferred/wrap_content size,
        // then we cannot use weights to size it; switch to pixel-based sizing instead
        Map<INode, Rect> sizes = resizeState.unweightedSizes;
        Rect nodePreferredSize = sizes.get(node);
        if (nodePreferredSize != null) {
            if (horizontalEdge != null && newBounds.h < nodePreferredSize.h ||
                    verticalEdge != null && newBounds.w < nodePreferredSize.w) {
                return;
            }
        }

        Rect layoutBounds = layout.getBounds();
        int remaining = (isVertical ? layoutBounds.h : layoutBounds.w) - resizeState.totalLength;
        Rect nodeBounds = sizes.get(node);
        if (nodeBounds == null) {
            return;
        }

        if (remaining > 0) {
            int missing = 0;
            if (isVertical) {
                if (newBounds.h > nodeBounds.h) {
                    missing = newBounds.h - nodeBounds.h;
                } else if (newBounds.h > resizeState.wrapBounds.h) {
                    // The weights concern how much space to ADD to the view.
                    // What if we have resized it to a size *smaller* than its current
                    // size without the weight delta? This can happen if you for example
                    // have set a hardcoded size, such as 500dp, and then size it to some
                    // smaller size.
                    missing = newBounds.h - resizeState.wrapBounds.h;
                    remaining += nodeBounds.h - resizeState.wrapBounds.h;
                    resizeState.wrapHeight = true;
                }
            } else {
                if (newBounds.w > nodeBounds.w) {
                    missing = newBounds.w - nodeBounds.w;
                } else if (newBounds.w > resizeState.wrapBounds.w) {
                    missing = newBounds.w - resizeState.wrapBounds.w;
                    remaining += nodeBounds.w - resizeState.wrapBounds.w;
                    resizeState.wrapWidth = true;
                }
            }
            if (missing > 0) {
                // (weight / weightSum) * remaining = missing, so
                // weight = missing * weightSum / remaining
                float weight = missing * sum / remaining;
                resizeState.setWeight(weight);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in this layout in order to make resizing affect the layout_weight
     * attribute instead of the layout_width (for horizontal LinearLayouts) or
     * layout_height (for vertical LinearLayouts).
     */
    @Override
    protected void setNewSizeBounds(ResizeState state, final INode node, INode layout,
            Rect oldBounds, Rect newBounds, SegmentType horizontalEdge,
            SegmentType verticalEdge) {
        LinearResizeState resizeState = (LinearResizeState) state;
        updateResizeState(resizeState, node, layout, oldBounds, newBounds,
                horizontalEdge, verticalEdge);

        if (resizeState.useWeight) {
            resizeState.apply();

            // Handle resizing in the opposite dimension of the layout
            final boolean isVertical = isVertical(layout);
            if (!isVertical && horizontalEdge != null) {
                if (newBounds.h != oldBounds.h || resizeState.wrapHeight
                        || resizeState.fillHeight) {
                    node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT,
                            resizeState.getHeightAttribute());
                }
            }
            if (isVertical && verticalEdge != null) {
                if (newBounds.w != oldBounds.w || resizeState.wrapWidth || resizeState.fillWidth) {
                    node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH,
                            resizeState.getWidthAttribute());
                }
            }
        } else {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WEIGHT, null);
            super.setNewSizeBounds(resizeState, node, layout, oldBounds, newBounds,
                    horizontalEdge, verticalEdge);
        }
    }

    @Override
    protected String getResizeUpdateMessage(ResizeState state, INode child, INode parent,
            Rect newBounds, SegmentType horizontalEdge, SegmentType verticalEdge) {
        LinearResizeState resizeState = (LinearResizeState) state;
        updateResizeState(resizeState, child, parent, child.getBounds(), newBounds,
                horizontalEdge, verticalEdge);

        if (resizeState.useWeight) {
            String weight = formatFloatAttribute(resizeState.mWeight);
            String dimension = String.format("weight %1$s", weight);

            String width;
            String height;
            if (isVertical(parent)) {
                width = resizeState.getWidthAttribute();
                height = dimension;
            } else {
                width = dimension;
                height = resizeState.getHeightAttribute();
            }

            if (horizontalEdge == null) {
                return width;
            } else if (verticalEdge == null) {
                return height;
            } else {
                // U+00D7: Unicode for multiplication sign
                return String.format("%s \u00D7 %s", width, height);
            }
        } else {
            return super.getResizeUpdateMessage(state, child, parent, newBounds,
                    horizontalEdge, verticalEdge);
        }
    }

    /**
     * Returns the layout weight of of the given child of a LinearLayout, or 0.0 if it
     * does not define a weight
     */
    private static float getWeight(INode linearLayoutChild) {
        String weight = linearLayoutChild.getStringAttr(ANDROID_URI, ATTR_LAYOUT_WEIGHT);
        if (weight != null && weight.length() > 0) {
            try {
                return Float.parseFloat(weight);
            } catch (NumberFormatException nfe) {
                AdtPlugin.log(nfe, "Invalid weight %1$s", weight);
            }
        }

        return 0.0f;
    }

    /**
     * Returns the sum of all the layout weights of the children in the given LinearLayout
     *
     * @param linearLayout the layout to compute the total sum for
     * @return the total sum of all the layout weights in the given layout
     */
    private static float getWeightSum(INode linearLayout) {
        String weightSum = linearLayout.getStringAttr(ANDROID_URI,
                ATTR_WEIGHT_SUM);
        float sum = -1.0f;
        if (weightSum != null) {
            // Distribute
            try {
                sum = Float.parseFloat(weightSum);
                return sum;
            } catch (NumberFormatException nfe) {
                // Just keep using the default
            }
        }

        return getSumOfWeights(linearLayout);
    }

    private static float getSumOfWeights(INode linearLayout) {
        float sum = 0.0f;
        for (INode child : linearLayout.getChildren()) {
            sum += getWeight(child);
        }

        return sum;
    }
}
