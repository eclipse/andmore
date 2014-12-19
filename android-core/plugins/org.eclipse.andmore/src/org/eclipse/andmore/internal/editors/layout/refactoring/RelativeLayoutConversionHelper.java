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
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_BACKGROUND;
import static com.android.SdkConstants.ATTR_BASELINE_ALIGNED;
import static com.android.SdkConstants.ATTR_LAYOUT_ABOVE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_HORIZONTAL;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_VERTICAL;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_WEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_N_DP;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.SdkConstants.VALUE_VERTICAL;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_BOTTOM;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_HORIZ;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_VERT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_FILL_HORIZ;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_FILL_VERT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_LEFT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_RIGHT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_TOP;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_VERT_MASK;

import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.GravityHelper;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.text.edits.MultiTextEdit;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class which performs the bulk of the layout conversion to relative layout
 * <p>
 * Future enhancements:
 * <ul>
 * <li>Render the layout at multiple screen sizes and analyze how the widgets move and
 * stretch and use that to add in additional constraints
 * <li> Adapt the LinearLayout analysis code to work with TableLayouts and TableRows as well
 * (just need to tweak the "isVertical" interpretation to account for the different defaults,
 * and perhaps do something about column size properties.
 * <li> We need to take into account existing margins and clear/update them
 * </ul>
 */
class RelativeLayoutConversionHelper {
    private final MultiTextEdit mRootEdit;
    private final boolean mFlatten;
    private final Element mLayout;
    private final ChangeLayoutRefactoring mRefactoring;
    private final CanvasViewInfo mRootView;
    private List<Element> mDeletedElements;

    RelativeLayoutConversionHelper(ChangeLayoutRefactoring refactoring,
            Element layout, boolean flatten, MultiTextEdit rootEdit, CanvasViewInfo rootView) {
        mRefactoring = refactoring;
        mLayout = layout;
        mFlatten = flatten;
        mRootEdit = rootEdit;
        mRootView = rootView;
    }

    /** Performs conversion from any layout to a RelativeLayout */
    public void convertToRelative() {
        if (mRootView == null) {
            return;
        }

        // Locate the view for the layout
        CanvasViewInfo layoutView = findViewForElement(mRootView, mLayout);
        if (layoutView == null || layoutView.getChildren().size() == 0) {
            // No children. THAT was an easy conversion!
            return;
        }

        // Study the layout and get information about how to place individual elements
        List<View> views = analyzeLayout(layoutView);

        // Create/update relative layout constraints
        createAttachments(views);
    }

    /** Returns the elements that were deleted, or null */
    List<Element> getDeletedElements() {
        return mDeletedElements;
    }

    /**
     * Analyzes the given view hierarchy and produces a list of {@link View} objects which
     * contain placement information for each element
     */
    private List<View> analyzeLayout(CanvasViewInfo layoutView) {
        EdgeList edgeList = new EdgeList(layoutView);
        mDeletedElements = edgeList.getDeletedElements();
        deleteRemovedElements(mDeletedElements);

        List<Integer> columnOffsets = edgeList.getColumnOffsets();
        List<Integer> rowOffsets = edgeList.getRowOffsets();

        // Compute x/y offsets for each row/column index
        int[] left = new int[columnOffsets.size()];
        int[] top = new int[rowOffsets.size()];

        Map<Integer, Integer> xToCol = new HashMap<Integer, Integer>();
        int columnIndex = 0;
        for (Integer offset : columnOffsets) {
            left[columnIndex] = offset;
            xToCol.put(offset, columnIndex++);
        }
        Map<Integer, Integer> yToRow = new HashMap<Integer, Integer>();
        int rowIndex = 0;
        for (Integer offset : rowOffsets) {
            top[rowIndex] = offset;
            yToRow.put(offset, rowIndex++);
        }

        // Create a complete list of view objects
        List<View> views = createViews(edgeList, columnOffsets);
        initializeSpans(edgeList, columnOffsets, rowOffsets, xToCol, yToRow);

        // Sanity check
        for (View view : views) {
            assert view.getLeftEdge() == left[view.mCol];
            assert view.getTopEdge() == top[view.mRow];
            assert view.getRightEdge() == left[view.mCol+view.mColSpan];
            assert view.getBottomEdge() == top[view.mRow+view.mRowSpan];
        }

        // Ensure that every view has a proper id such that it can be referred to
        // with a constraint
        initializeIds(edgeList, views);

        // Attempt to lay the views out in a grid with constraints (though not that widgets
        // can overlap as well)
        Grid grid = new Grid(views, left, top);
        computeKnownConstraints(views, edgeList);
        computeHorizontalConstraints(grid);
        computeVerticalConstraints(grid);

        return views;
    }

    /** Produces a list of {@link View} objects from an {@link EdgeList} */
    private List<View> createViews(EdgeList edgeList, List<Integer> columnOffsets) {
        List<View> views = new ArrayList<View>();
        for (Integer offset : columnOffsets) {
            List<View> leftEdgeViews = edgeList.getLeftEdgeViews(offset);
            if (leftEdgeViews == null) {
                // must have been a right edge
                continue;
            }
            for (View view : leftEdgeViews) {
                views.add(view);
            }
        }
        return views;
    }

    /** Removes any elements targeted for deletion */
    private void deleteRemovedElements(List<Element> delete) {
        if (mFlatten && delete.size() > 0) {
            for (Element element : delete) {
                mRefactoring.removeElementTags(mRootEdit, element, delete,
                        !AdtPrefs.getPrefs().getFormatGuiXml() /*changeIndentation*/);
            }
        }
    }

    /** Ensures that every element has an id such that it can be referenced from a constraint */
    private void initializeIds(EdgeList edgeList, List<View> views) {
        // Ensure that all views have a valid id
        for (View view : views) {
            String id = mRefactoring.ensureHasId(mRootEdit, view.mElement, null);
            edgeList.setIdAttributeValue(view, id);
        }
    }

    /**
     * Initializes the column and row indices, as well as any column span and row span
     * values
     */
    private void initializeSpans(EdgeList edgeList, List<Integer> columnOffsets,
            List<Integer> rowOffsets, Map<Integer, Integer> xToCol, Map<Integer, Integer> yToRow) {
        // Now initialize table view row, column and spans
        for (Integer offset : columnOffsets) {
            List<View> leftEdgeViews = edgeList.getLeftEdgeViews(offset);
            if (leftEdgeViews == null) {
                // must have been a right edge
                continue;
            }
            for (View view : leftEdgeViews) {
                Integer col = xToCol.get(view.getLeftEdge());
                assert col != null;
                Integer end = xToCol.get(view.getRightEdge());
                assert end != null;

                view.mCol = col;
                view.mColSpan = end - col;
            }
        }

        for (Integer offset : rowOffsets) {
            List<View> topEdgeViews = edgeList.getTopEdgeViews(offset);
            if (topEdgeViews == null) {
                // must have been a bottom edge
                continue;
            }
            for (View view : topEdgeViews) {
                Integer row = yToRow.get(view.getTopEdge());
                assert row != null;
                Integer end = yToRow.get(view.getBottomEdge());
                assert end != null;

                view.mRow = row;
                view.mRowSpan = end - row;
            }
        }
    }

    /**
     * Creates refactoring edits which adds or updates constraints for the given list of
     * views
     */
    private void createAttachments(List<View> views) {
        // Make the attachments
        String namespace = mRefactoring.getAndroidNamespacePrefix();
        for (View view : views) {
            for (Pair<String, String> constraint : view.getHorizConstraints()) {
                mRefactoring.setAttribute(mRootEdit, view.mElement, ANDROID_URI,
                        namespace, constraint.getFirst(), constraint.getSecond());
            }
            for (Pair<String, String> constraint : view.getVerticalConstraints()) {
                mRefactoring.setAttribute(mRootEdit, view.mElement, ANDROID_URI,
                        namespace, constraint.getFirst(), constraint.getSecond());
            }
        }
    }

    /**
     * Analyzes the existing layouts and layout parameter objects in the document to infer
     * constraints for layout types that we know about - such as LinearLayout baseline
     * alignment, weights, gravity, etc.
     */
    private void computeKnownConstraints(List<View> views, EdgeList edgeList) {
        // List of parent layout elements we've already processed. We iterate through all
        // the -children-, and we ask each for its element parent (which won't have a view)
        // and we look at the parent's layout attributes and its children layout constraints,
        // and then we stash away constraints that we can infer. This means that we will
        // encounter the same parent for every sibling, so that's why there's a map to
        // prevent duplicate work.
        Set<Node> seen = new HashSet<Node>();

        for (View view : views) {
            Element element = view.getElement();
            Node parent = element.getParentNode();
            if (seen.contains(parent)) {
                continue;
            }
            seen.add(parent);

            if (parent.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element layout = (Element) parent;
            String layoutName = layout.getTagName();

            if (LINEAR_LAYOUT.equals(layoutName)) {
                analyzeLinearLayout(edgeList, layout);
            } else if (RELATIVE_LAYOUT.equals(layoutName)) {
                analyzeRelativeLayout(edgeList, layout);
            } else {
                // Some other layout -- add more conditional handling here
                // for framelayout, tables, etc.
            }
        }
    }

    /**
     * Returns the layout weight of of the given child of a LinearLayout, or 0.0 if it
     * does not define a weight
     */
    private float getWeight(Element linearLayoutChild) {
        String weight = linearLayoutChild.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_WEIGHT);
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
    private float getWeightSum(Element linearLayout) {
        float sum = 0;
        for (Element child : DomUtilities.getChildren(linearLayout)) {
            sum += getWeight(child);
        }

        return sum;
    }

    /**
     * Analyzes the given LinearLayout and updates the constraints to reflect
     * relationships it can infer - based on baseline alignment, gravity, order and
     * weights. This method also removes "0dip" as a special width/height used in
     * LinearLayouts with weight distribution.
     */
    private void analyzeLinearLayout(EdgeList edgeList, Element layout) {
        boolean isVertical = VALUE_VERTICAL.equals(layout.getAttributeNS(ANDROID_URI,
                ATTR_ORIENTATION));
        View baselineRef = null;
        if (!isVertical &&
            !VALUE_FALSE.equals(layout.getAttributeNS(ANDROID_URI, ATTR_BASELINE_ALIGNED))) {
            // Baseline alignment. Find the tallest child and set it as the baseline reference.
            int tallestHeight = 0;
            View tallest = null;
            for (Element child : DomUtilities.getChildren(layout)) {
                View view = edgeList.getView(child);
                if (view != null && view.getHeight() > tallestHeight) {
                    tallestHeight = view.getHeight();
                    tallest = view;
                }
            }
            if (tallest != null) {
                baselineRef = tallest;
            }
        }

        float weightSum = getWeightSum(layout);
        float cumulativeWeight = 0;

        List<Element> children = DomUtilities.getChildren(layout);
        String prevId = null;
        boolean isFirstChild = true;
        boolean linkBackwards = true;
        boolean linkForwards = false;

        for (int index = 0, childCount = children.size(); index < childCount; index++) {
            Element child = children.get(index);

            View childView = edgeList.getView(child);
            if (childView == null) {
                // Could be a nested layout that is being removed etc
                prevId = null;
                isFirstChild = false;
                continue;
            }

            // Look at the layout_weight attributes and determine whether we should be
            // attached on the bottom/right or on the top/left
            if (weightSum > 0.0f) {
                float weight = getWeight(child);

                // We can't emulate a LinearLayout where multiple children have positive
                // weights. However, we CAN support the common scenario where a single
                // child has a non-zero weight, and all children after it are pushed
                // to the end and the weighted child fills the remaining space.
                if (cumulativeWeight == 0 && weight > 0) {
                    // See if we have a bottom/right edge to attach the forwards link to
                    // (at the end of the forwards chains). Only if so can we link forwards.
                    View referenced;
                    if (isVertical) {
                        referenced = edgeList.getSharedBottomEdge(layout);
                    } else {
                        referenced = edgeList.getSharedRightEdge(layout);
                    }
                    if (referenced != null) {
                        linkForwards = true;
                    }
                } else if (cumulativeWeight > 0) {
                    linkBackwards = false;
                }

                cumulativeWeight += weight;
            }

            analyzeGravity(edgeList, layout, isVertical, child, childView);
            convert0dipToWrapContent(child);

            // Chain elements together in the flow direction of the linear layout
            if (prevId != null) { // No constraint for first child
                if (linkBackwards) {
                    if (isVertical) {
                        childView.addVerticalConstraint(ATTR_LAYOUT_BELOW, prevId);
                    } else {
                        childView.addHorizConstraint(ATTR_LAYOUT_TO_RIGHT_OF, prevId);
                    }
                }
            } else if (isFirstChild) {
                assert linkBackwards;

                // First element; attach it to the parent if we can
                if (isVertical) {
                    View referenced = edgeList.getSharedTopEdge(layout);
                    if (referenced != null) {
                        if (isAncestor(referenced.getElement(), child)) {
                            childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_TOP,
                                VALUE_TRUE);
                        } else {
                            childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_TOP,
                                    referenced.getId());
                        }
                    }
                } else {
                    View referenced = edgeList.getSharedLeftEdge(layout);
                    if (referenced != null) {
                        if (isAncestor(referenced.getElement(), child)) {
                            childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_LEFT,
                                    VALUE_TRUE);
                        } else {
                            childView.addHorizConstraint(
                                    ATTR_LAYOUT_ALIGN_LEFT, referenced.getId());
                        }
                    }
                }
            }

            if (linkForwards) {
                if (index < (childCount - 1)) {
                    Element nextChild = children.get(index + 1);
                    String nextId = mRefactoring.ensureHasId(mRootEdit, nextChild, null);
                    if (nextId != null) {
                        if (isVertical) {
                            childView.addVerticalConstraint(ATTR_LAYOUT_ABOVE, nextId);
                        } else {
                            childView.addHorizConstraint(ATTR_LAYOUT_TO_LEFT_OF, nextId);
                        }
                    }
                } else {
                    // Attach to right/bottom edge of the layout
                    if (isVertical) {
                        View referenced = edgeList.getSharedBottomEdge(layout);
                        if (referenced != null) {
                            if (isAncestor(referenced.getElement(), child)) {
                                childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM,
                                    VALUE_TRUE);
                            } else {
                                childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_BOTTOM,
                                        referenced.getId());
                            }
                        }
                    } else {
                        View referenced = edgeList.getSharedRightEdge(layout);
                        if (referenced != null) {
                            if (isAncestor(referenced.getElement(), child)) {
                                childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_RIGHT,
                                        VALUE_TRUE);
                            } else {
                                childView.addHorizConstraint(
                                        ATTR_LAYOUT_ALIGN_RIGHT, referenced.getId());
                            }
                        }
                    }
                }
            }

            if (baselineRef != null && baselineRef.getId() != null
                    && !baselineRef.getId().equals(childView.getId())) {
                assert !isVertical;
                // Only align if they share the same gravity
                if ((childView.getGravity() & GRAVITY_VERT_MASK) ==
                        (baselineRef.getGravity() & GRAVITY_VERT_MASK)) {
                    childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_BASELINE, baselineRef.getId());
                }
            }

            prevId = mRefactoring.ensureHasId(mRootEdit, child, null);
            isFirstChild = false;
        }
    }

    /**
     * Checks the layout "gravity" value for the given child and updates the constraints
     * to account for the gravity
     */
    private int analyzeGravity(EdgeList edgeList, Element layout, boolean isVertical,
            Element child, View childView) {
        // Use gravity to constrain elements in the axis orthogonal to the
        // direction of the layout
        int gravity = childView.getGravity();
        if (isVertical) {
            if ((gravity & GRAVITY_RIGHT) != 0) {
                View referenced = edgeList.getSharedRightEdge(layout);
                if (referenced != null) {
                    if (isAncestor(referenced.getElement(), child)) {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_RIGHT,
                                VALUE_TRUE);
                    } else {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_RIGHT,
                                referenced.getId());
                    }
                }
            } else if ((gravity & GRAVITY_CENTER_HORIZ) != 0) {
                View referenced1 = edgeList.getSharedLeftEdge(layout);
                View referenced2 = edgeList.getSharedRightEdge(layout);
                if (referenced1 != null && referenced2 == referenced1) {
                    if (isAncestor(referenced1.getElement(), child)) {
                        childView.addHorizConstraint(ATTR_LAYOUT_CENTER_HORIZONTAL,
                                VALUE_TRUE);
                    }
                }
            } else if ((gravity & GRAVITY_FILL_HORIZ) != 0) {
                View referenced1 = edgeList.getSharedLeftEdge(layout);
                View referenced2 = edgeList.getSharedRightEdge(layout);
                if (referenced1 != null && referenced2 == referenced1) {
                    if (isAncestor(referenced1.getElement(), child)) {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_LEFT,
                                VALUE_TRUE);
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_RIGHT,
                                VALUE_TRUE);
                    } else {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_LEFT,
                                referenced1.getId());
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_RIGHT,
                                referenced2.getId());
                    }
                }
            } else if ((gravity & GRAVITY_LEFT) != 0) {
                View referenced = edgeList.getSharedLeftEdge(layout);
                if (referenced != null) {
                    if (isAncestor(referenced.getElement(), child)) {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_PARENT_LEFT,
                                VALUE_TRUE);
                    } else {
                        childView.addHorizConstraint(ATTR_LAYOUT_ALIGN_LEFT,
                                referenced.getId());
                    }
                }
            }
        } else {
            // Handle horizontal layout: perform vertical gravity attachments
            if ((gravity & GRAVITY_BOTTOM) != 0) {
                View referenced = edgeList.getSharedBottomEdge(layout);
                if (referenced != null) {
                    if (isAncestor(referenced.getElement(), child)) {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM,
                                VALUE_TRUE);
                    } else {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_BOTTOM,
                                referenced.getId());
                    }
                }
            } else if ((gravity & GRAVITY_CENTER_VERT) != 0) {
                View referenced1 = edgeList.getSharedTopEdge(layout);
                View referenced2 = edgeList.getSharedBottomEdge(layout);
                if (referenced1 != null && referenced2 == referenced1) {
                    if (isAncestor(referenced1.getElement(), child)) {
                        childView.addVerticalConstraint(ATTR_LAYOUT_CENTER_VERTICAL,
                                VALUE_TRUE);
                    }
                }
            } else if ((gravity & GRAVITY_FILL_VERT) != 0) {
                View referenced1 = edgeList.getSharedTopEdge(layout);
                View referenced2 = edgeList.getSharedBottomEdge(layout);
                if (referenced1 != null && referenced2 == referenced1) {
                    if (isAncestor(referenced1.getElement(), child)) {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_TOP,
                                VALUE_TRUE);
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM,
                                VALUE_TRUE);
                    } else {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_TOP,
                                referenced1.getId());
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_BOTTOM,
                                referenced2.getId());
                    }
                }
            } else if ((gravity & GRAVITY_TOP) != 0) {
                View referenced = edgeList.getSharedTopEdge(layout);
                if (referenced != null) {
                    if (isAncestor(referenced.getElement(), child)) {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_PARENT_TOP,
                                VALUE_TRUE);
                    } else {
                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_TOP,
                                referenced.getId());
                    }
                }
            }
        }
        return gravity;
    }

    /** Converts 0dip values in layout_width and layout_height to wrap_content instead */
    private void convert0dipToWrapContent(Element child) {
        // Must convert layout_height="0dip" to layout_height="wrap_content".
        // 0dip is a special trick used in linear layouts in the presence of
        // weights where 0dip ensures that the height of the view is not taken
        // into account when distributing the weights. However, when converted
        // to RelativeLayout this will instead cause the view to actually be assigned
        // 0 height.
        String height = child.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
        // 0dip, 0dp, 0px, etc
        if (height != null && height.startsWith("0")) { //$NON-NLS-1$
            mRefactoring.setAttribute(mRootEdit, child, ANDROID_URI,
                    mRefactoring.getAndroidNamespacePrefix(), ATTR_LAYOUT_HEIGHT,
                    VALUE_WRAP_CONTENT);
        }
        String width = child.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
        if (width != null && width.startsWith("0")) { //$NON-NLS-1$
            mRefactoring.setAttribute(mRootEdit, child, ANDROID_URI,
                    mRefactoring.getAndroidNamespacePrefix(), ATTR_LAYOUT_WIDTH,
                    VALUE_WRAP_CONTENT);
        }
    }

    /**
     * Analyzes an embedded RelativeLayout within a layout hierarchy and updates the
     * constraints in the EdgeList with those relationships which can continue in the
     * outer single RelativeLayout.
     */
    private void analyzeRelativeLayout(EdgeList edgeList, Element layout) {
        NodeList children = layout.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                View childView = edgeList.getView(child);
                if (childView == null) {
                    // Could be a nested layout that is being removed etc
                    continue;
                }

                NamedNodeMap attributes = child.getAttributes();
                for (int j = 0, m = attributes.getLength(); j < m; j++) {
                    Attr attribute = (Attr) attributes.item(j);
                    String name = attribute.getLocalName();
                    String value = attribute.getValue();
                    if (name.equals(ATTR_LAYOUT_WIDTH)
                            || name.equals(ATTR_LAYOUT_HEIGHT)) {
                        // Ignore these for now
                    } else if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                            && ANDROID_URI.equals(attribute.getNamespaceURI())) {
                        // Determine if the reference is to a known edge
                        String id = getIdBasename(value);
                        if (id != null) {
                            View referenced = edgeList.getView(id);
                            if (referenced != null) {
                                // This is a valid reference, so preserve
                                // the attribute
                                if (name.equals(ATTR_LAYOUT_BELOW) ||
                                        name.equals(ATTR_LAYOUT_ABOVE) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_TOP) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_BOTTOM) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_BASELINE)) {
                                    // Vertical constraint
                                    childView.addVerticalConstraint(name, value);
                                } else if (name.equals(ATTR_LAYOUT_ALIGN_LEFT) ||
                                        name.equals(ATTR_LAYOUT_TO_LEFT_OF) ||
                                        name.equals(ATTR_LAYOUT_TO_RIGHT_OF) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_RIGHT)) {
                                    // Horizontal constraint
                                    childView.addHorizConstraint(name, value);
                                } else {
                                    // We don't expect this
                                    assert false : name;
                                }
                            } else {
                                // Reference to some layout that is not included here.
                                // TODO: See if the given layout has an edge
                                // that corresponds to one of our known views
                                // so we can adjust the constraints and keep it after all.
                            }
                        } else {
                            // It's a parent-relative constraint (such
                            // as aligning with a parent edge, or centering
                            // in the parent view)
                            boolean remove = true;
                            if (name.equals(ATTR_LAYOUT_ALIGN_PARENT_LEFT)) {
                                View referenced = edgeList.getSharedLeftEdge(layout);
                                if (referenced != null) {
                                    if (isAncestor(referenced.getElement(), child)) {
                                        childView.addHorizConstraint(name, VALUE_TRUE);
                                    } else {
                                        childView.addHorizConstraint(
                                                ATTR_LAYOUT_ALIGN_LEFT, referenced.getId());
                                    }
                                    remove = false;
                                }
                            } else if (name.equals(ATTR_LAYOUT_ALIGN_PARENT_RIGHT)) {
                                View referenced = edgeList.getSharedRightEdge(layout);
                                if (referenced != null) {
                                    if (isAncestor(referenced.getElement(), child)) {
                                        childView.addHorizConstraint(name, VALUE_TRUE);
                                    } else {
                                        childView.addHorizConstraint(
                                            ATTR_LAYOUT_ALIGN_RIGHT, referenced.getId());
                                    }
                                    remove = false;
                                }
                            } else if (name.equals(ATTR_LAYOUT_ALIGN_PARENT_TOP)) {
                                View referenced = edgeList.getSharedTopEdge(layout);
                                if (referenced != null) {
                                    if (isAncestor(referenced.getElement(), child)) {
                                        childView.addVerticalConstraint(name, VALUE_TRUE);
                                    } else {
                                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_TOP,
                                                referenced.getId());
                                    }
                                    remove = false;
                                }
                            } else if (name.equals(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM)) {
                                View referenced = edgeList.getSharedBottomEdge(layout);
                                if (referenced != null) {
                                    if (isAncestor(referenced.getElement(), child)) {
                                        childView.addVerticalConstraint(name, VALUE_TRUE);
                                    } else {
                                        childView.addVerticalConstraint(ATTR_LAYOUT_ALIGN_BOTTOM,
                                                referenced.getId());
                                    }
                                    remove = false;
                                }
                            }

                            boolean alignWithParent =
                                    name.equals(ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING);
                            if (remove && alignWithParent) {
                                // TODO - look for this one AFTER we have processed
                                // everything else, and then set constraints as necessary
                                // IF there are no other conflicting constraints!
                            }

                            // Otherwise it's some kind of centering which we don't support
                            // yet.

                            // TODO: Find a way to determine whether we have
                            // a corresponding edge for the parent (e.g. if
                            // the ViewInfo bounds match our outer parent or
                            // some other edge) and if so, substitute for that
                            // id.
                            // For example, if this element was centered
                            // horizontally in a RelativeLayout that actually
                            // occupies the entire width of our outer layout,
                            // then it can be preserved after all!

                            if (remove) {
                                if (name.startsWith("layout_margin")) { //$NON-NLS-1$
                                    continue;
                                }

                                // Remove unknown attributes?
                                // It's too early to do this, because we may later want
                                // to *set* this value and it would result in an overlapping edits
                                // exception. Therefore, we need to RECORD which attributes should
                                // be removed, which lines should have its indentation adjusted
                                // etc and finally process it all at the end!
                                //mRefactoring.removeAttribute(mRootEdit, child,
                                //        attribute.getNamespaceURI(), name);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Given {@code @id/foo} or {@code @+id/foo}, returns foo. Note that given foo it will
     * return null.
     */
    private static String getIdBasename(String id) {
        if (id.startsWith(NEW_ID_PREFIX)) {
            return id.substring(NEW_ID_PREFIX.length());
        } else if (id.startsWith(ID_PREFIX)) {
            return id.substring(ID_PREFIX.length());
        }

        return null;
    }

    /** Returns true if the given second argument is a descendant of the first argument */
    private static boolean isAncestor(Node ancestor, Node node) {
        while (node != null) {
            if (node == ancestor) {
                return true;
            }
            node = node.getParentNode();
        }
        return false;
    }

    /**
     * Computes horizontal constraints for the views in the grid for any remaining views
     * that do not have constraints (as the result of the analysis of known layouts). This
     * will look at the rendered layout coordinates and attempt to connect elements based
     * on a spatial layout in the grid.
     */
    private void computeHorizontalConstraints(Grid grid) {
        int columns = grid.getColumns();

        String attachLeftProperty = ATTR_LAYOUT_ALIGN_PARENT_LEFT;
        String attachLeftValue = VALUE_TRUE;
        int marginLeft = 0;
        for (int col = 0; col < columns; col++) {
            if (!grid.colContainsTopLeftCorner(col)) {
                // Just accumulate margins for the next column
                marginLeft += grid.getColumnWidth(col);
            } else {
                // Add horizontal attachments
                String firstId = null;
                for (View view : grid.viewsStartingInCol(col, true)) {
                    assert view.getId() != null;
                    if (firstId == null) {
                        firstId = view.getId();
                        if (view.isConstrainedHorizontally()) {
                            // Nothing to do -- we already have an accurate position for
                            // this view
                        } else if (attachLeftProperty != null) {
                            view.addHorizConstraint(attachLeftProperty, attachLeftValue);
                            if (marginLeft > 0) {
                                view.addHorizConstraint(ATTR_LAYOUT_MARGIN_LEFT,
                                        String.format(VALUE_N_DP, marginLeft));
                                marginLeft = 0;
                            }
                        } else {
                            assert false;
                        }
                    } else if (!view.isConstrainedHorizontally()) {
                        view.addHorizConstraint(ATTR_LAYOUT_ALIGN_LEFT, firstId);
                    }
                }
            }

            // Figure out edge for the next column
            View view = grid.findRightEdgeView(col);
            if (view != null) {
                assert view.getId() != null;
                attachLeftProperty = ATTR_LAYOUT_TO_RIGHT_OF;
                attachLeftValue = view.getId();

                marginLeft = 0;
            } else if (marginLeft == 0) {
                marginLeft = grid.getColumnWidth(col);
            }
        }
    }

    /**
     * Performs vertical layout just like the {@link #computeHorizontalConstraints} method
     * did horizontally
     */
    private void computeVerticalConstraints(Grid grid) {
        int rows = grid.getRows();

        String attachTopProperty = ATTR_LAYOUT_ALIGN_PARENT_TOP;
        String attachTopValue = VALUE_TRUE;
        int marginTop = 0;
        for (int row = 0; row < rows; row++) {
            if (!grid.rowContainsTopLeftCorner(row)) {
                // Just accumulate margins for the next column
                marginTop += grid.getRowHeight(row);
            } else {
                // Add horizontal attachments
                String firstId = null;
                for (View view : grid.viewsStartingInRow(row, true)) {
                    assert view.getId() != null;
                    if (firstId == null) {
                        firstId = view.getId();
                        if (view.isConstrainedVertically()) {
                            // Nothing to do -- we already have an accurate position for
                            // this view
                        } else if (attachTopProperty != null) {
                            view.addVerticalConstraint(attachTopProperty, attachTopValue);
                            if (marginTop > 0) {
                                view.addVerticalConstraint(ATTR_LAYOUT_MARGIN_TOP,
                                        String.format(VALUE_N_DP, marginTop));
                                marginTop = 0;
                            }
                        } else {
                            assert false;
                        }
                    } else if (!view.isConstrainedVertically()) {
                        view.addVerticalConstraint(ATTR_LAYOUT_ALIGN_TOP, firstId);
                    }
                }
            }

            // Figure out edge for the next row
            View view = grid.findBottomEdgeView(row);
            if (view != null) {
                assert view.getId() != null;
                attachTopProperty = ATTR_LAYOUT_BELOW;
                attachTopValue = view.getId();
                marginTop = 0;
            } else if (marginTop == 0) {
                marginTop = grid.getRowHeight(row);
            }
        }
    }

    /**
     * Searches a view hierarchy and locates the {@link CanvasViewInfo} for the given
     * {@link Element}
     *
     * @param info the root {@link CanvasViewInfo} to search below
     * @param element the target element
     * @return the {@link CanvasViewInfo} which corresponds to the given element
     */
    private CanvasViewInfo findViewForElement(CanvasViewInfo info, Element element) {
        if (getElement(info) == element) {
            return info;
        }

        for (CanvasViewInfo child : info.getChildren()) {
            CanvasViewInfo result = findViewForElement(child, element);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /** Returns the {@link Element} for the given {@link CanvasViewInfo} */
    private static Element getElement(CanvasViewInfo info) {
        Node node = info.getUiViewNode().getXmlNode();
        if (node instanceof Element) {
            return (Element) node;
        }

        return null;
    }

    /**
     * A grid of cells which can contain views, used to infer spatial relationships when
     * computing constraints. Note that a view can appear in than one cell; they will
     * appear in all cells that their bounds overlap with!
     */
    private class Grid {
        private final int[] mLeft;
        private final int[] mTop;
        // A list from row to column to cell, where a cell is a list of views
        private final List<List<List<View>>> mRowList;
        private int mRowCount;
        private int mColCount;

        Grid(List<View> views, int[] left, int[] top) {
            mLeft = left;
            mTop = top;

            // The left/top arrays should include the ending point too
            mColCount = left.length - 1;
            mRowCount = top.length - 1;

            // Using nested lists rather than arrays to avoid lack of typed arrays
            // (can't create List<View>[row][column] arrays)
            mRowList = new ArrayList<List<List<View>>>(top.length);
            for (int row = 0; row < top.length; row++) {
                List<List<View>> columnList = new ArrayList<List<View>>(left.length);
                for (int col = 0; col < left.length; col++) {
                    columnList.add(new ArrayList<View>(4));
                }
                mRowList.add(columnList);
            }

            for (View view : views) {
                // Get rid of the root view; we don't want that in the attachments logic;
                // it was there originally such that it would contribute the outermost
                // edges.
                if (view.mElement == mLayout) {
                    continue;
                }

                for (int i = 0; i < view.mRowSpan; i++) {
                    for (int j = 0; j < view.mColSpan; j++) {
                        mRowList.get(view.mRow + i).get(view.mCol + j).add(view);
                    }
                }
            }
        }

        /**
         * Returns the number of rows in the grid
         *
         * @return the row count
         */
        public int getRows() {
            return mRowCount;
        }

        /**
         * Returns the number of columns in the grid
         *
         * @return the column count
         */
        public int getColumns() {
            return mColCount;
        }

        /**
         * Returns the list of views overlapping the given cell
         *
         * @param row the row of the target cell
         * @param col the column of the target cell
         * @return a list of views overlapping the given column
         */
        public List<View> get(int row, int col) {
            return mRowList.get(row).get(col);
        }

        /**
         * Returns true if the given column contains a top left corner of a view
         *
         * @param column the column to check
         * @return true if one or more views have their top left corner in this column
         */
        public boolean colContainsTopLeftCorner(int column) {
            for (int row = 0; row < mRowCount; row++) {
                View view = getTopLeftCorner(row, column);
                if (view != null) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Returns true if the given row contains a top left corner of a view
         *
         * @param row the row to check
         * @return true if one or more views have their top left corner in this row
         */
        public boolean rowContainsTopLeftCorner(int row) {
            for (int col = 0; col < mColCount; col++) {
                View view = getTopLeftCorner(row, col);
                if (view != null) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Returns a list of views (optionally sorted by increasing row index) that have
         * their left edge starting in the given column
         *
         * @param col the column to look up views for
         * @param sort whether to sort the result in increasing row order
         * @return a list of views starting in the given column
         */
        public List<View> viewsStartingInCol(int col, boolean sort) {
            List<View> views = new ArrayList<View>();
            for (int row = 0; row < mRowCount; row++) {
                View view = getTopLeftCorner(row, col);
                if (view != null) {
                    views.add(view);
                }
            }

            if (sort) {
                View.sortByRow(views);
            }

            return views;
        }

        /**
         * Returns a list of views (optionally sorted by increasing column index) that have
         * their top edge starting in the given row
         *
         * @param row the row to look up views for
         * @param sort whether to sort the result in increasing column order
         * @return a list of views starting in the given row
         */
        public List<View> viewsStartingInRow(int row, boolean sort) {
            List<View> views = new ArrayList<View>();
            for (int col = 0; col < mColCount; col++) {
                View view = getTopLeftCorner(row, col);
                if (view != null) {
                    views.add(view);
                }
            }

            if (sort) {
                View.sortByColumn(views);
            }

            return views;
        }

        /**
         * Returns the pixel width of the given column
         *
         * @param col the column to look up the width of
         * @return the width of the column
         */
        public int getColumnWidth(int col) {
            return mLeft[col + 1] - mLeft[col];
        }

        /**
         * Returns the pixel height of the given row
         *
         * @param row the row to look up the height of
         * @return the height of the row
         */
        public int getRowHeight(int row) {
            return mTop[row + 1] - mTop[row];
        }

        /**
         * Returns the first view found that has its top left corner in the cell given by
         * the row and column indexes, or null if not found.
         *
         * @param row the row of the target cell
         * @param col the column of the target cell
         * @return a view with its top left corner in the given cell, or null if not found
         */
        View getTopLeftCorner(int row, int col) {
            List<View> views = get(row, col);
            if (views.size() > 0) {
                for (View view : views) {
                    if (view.mRow == row && view.mCol == col) {
                        return view;
                    }
                }
            }

            return null;
        }

        public View findRightEdgeView(int col) {
            for (int row = 0; row < mRowCount; row++) {
                List<View> views = get(row, col);
                if (views.size() > 0) {
                    List<View> result = new ArrayList<View>();
                    for (View view : views) {
                        // Ends on the right edge of this column?
                        if (view.mCol + view.mColSpan == col + 1) {
                            result.add(view);
                        }
                    }
                    if (result.size() > 1) {
                        View.sortByColumn(result);
                    }
                    if (result.size() > 0) {
                        return result.get(0);
                    }
                }
            }

            return null;
        }

        public View findBottomEdgeView(int row) {
            for (int col = 0; col < mColCount; col++) {
                List<View> views = get(row, col);
                if (views.size() > 0) {
                    List<View> result = new ArrayList<View>();
                    for (View view : views) {
                        // Ends on the bottom edge of this column?
                        if (view.mRow + view.mRowSpan == row + 1) {
                            result.add(view);
                        }
                    }
                    if (result.size() > 1) {
                        View.sortByRow(result);
                    }
                    if (result.size() > 0) {
                        return result.get(0);
                    }

                }
            }

            return null;
        }

        /**
         * Produces a display of view contents along with the pixel positions of each row/column,
         * like the following (used for diagnostics only)
         * <pre>
         *          |0                  |49                 |143                |192           |240
         *        36|                   |                   |button2            |
         *        72|                   |radioButton1       |button2            |
         *        74|button1            |radioButton1       |button2            |
         *       108|button1            |                   |button2            |
         *       110|                   |                   |button2            |
         *       149|                   |                   |                   |
         *       320
         * </pre>
         */
        @Override
        public String toString() {
            // Dump out the view table
            int cellWidth = 20;

            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            out.printf("%" + cellWidth + "s", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            for (int col = 0; col < mColCount + 1; col++) {
                out.printf("|%-" + (cellWidth - 1) + "d", mLeft[col]); //$NON-NLS-1$ //$NON-NLS-2$
            }
            out.printf("\n"); //$NON-NLS-1$
            for (int row = 0; row < mRowCount + 1; row++) {
                out.printf("%" + cellWidth + "d", mTop[row]); //$NON-NLS-1$ //$NON-NLS-2$
                if (row == mRowCount) {
                    break;
                }
                for (int col = 0; col < mColCount; col++) {
                    List<View> views = get(row, col);
                    StringBuilder sb = new StringBuilder();
                    for (View view : views) {
                        String id = view != null ? view.getId() : ""; //$NON-NLS-1$
                        if (id.startsWith(NEW_ID_PREFIX)) {
                            id = id.substring(NEW_ID_PREFIX.length());
                        }
                        if (id.length() > cellWidth - 2) {
                            id = id.substring(0, cellWidth - 2);
                        }
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(id);
                    }
                    String cellString = sb.toString();
                    if (cellString.contains(",") && cellString.length() > cellWidth - 2) { //$NON-NLS-1$
                        cellString = cellString.substring(0, cellWidth - 6) + "...,"; //$NON-NLS-1$
                    }
                    out.printf("|%-" + (cellWidth - 2) + "s ", cellString); //$NON-NLS-1$ //$NON-NLS-2$
                }
                out.printf("\n"); //$NON-NLS-1$
            }

            out.flush();
            return stringWriter.toString();
        }
    }

    /** Holds layout information about an individual view. */
    private static class View {
        private final Element mElement;
        private int mRow = -1;
        private int mCol = -1;
        private int mRowSpan = -1;
        private int mColSpan = -1;
        private CanvasViewInfo mInfo;
        private String mId;
        private List<Pair<String, String>> mHorizConstraints =
            new ArrayList<Pair<String, String>>(4);
        private List<Pair<String, String>> mVerticalConstraints =
            new ArrayList<Pair<String, String>>(4);
        private int mGravity;

        public View(CanvasViewInfo view, Element element) {
            mInfo = view;
            mElement = element;
            mGravity = GravityHelper.getGravity(element);
        }

        public int getHeight() {
            return mInfo.getAbsRect().height;
        }

        public int getGravity() {
            return mGravity;
        }

        public String getId() {
            return mId;
        }

        public Element getElement() {
            return mElement;
        }

        public List<Pair<String, String>> getHorizConstraints() {
            return mHorizConstraints;
        }

        public List<Pair<String, String>> getVerticalConstraints() {
            return mVerticalConstraints;
        }

        public boolean isConstrainedHorizontally() {
            return mHorizConstraints.size() > 0;
        }

        public boolean isConstrainedVertically() {
            return mVerticalConstraints.size() > 0;
        }

        public void addHorizConstraint(String property, String value) {
            assert property != null && value != null;
            // TODO - look for duplicates?
            mHorizConstraints.add(Pair.of(property, value));
        }

        public void addVerticalConstraint(String property, String value) {
            assert property != null && value != null;
            mVerticalConstraints.add(Pair.of(property, value));
        }

        public int getLeftEdge() {
            return mInfo.getAbsRect().x;
        }

        public int getTopEdge() {
            return mInfo.getAbsRect().y;
        }

        public int getRightEdge() {
            Rectangle bounds = mInfo.getAbsRect();
            // +1: make the bounds overlap, so the right edge is the same as the
            // left edge of the neighbor etc. Otherwise we end up with lots of 1-pixel wide
            // columns between adjacent items.
            return bounds.x + bounds.width + 1;
        }

        public int getBottomEdge() {
            Rectangle bounds = mInfo.getAbsRect();
            return bounds.y + bounds.height + 1;
        }

        @Override
        public String toString() {
            return "View [mId=" + mId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        public static void sortByRow(List<View> views) {
            Collections.sort(views, new ViewComparator(true/*rowSort*/));
        }

        public static void sortByColumn(List<View> views) {
            Collections.sort(views, new ViewComparator(false/*rowSort*/));
        }

        /** Comparator to help sort views by row or column index */
        private static class ViewComparator implements Comparator<View> {
            boolean mRowSort;

            public ViewComparator(boolean rowSort) {
                mRowSort = rowSort;
            }

            @Override
            public int compare(View view1, View view2) {
                if (mRowSort) {
                    return view1.mRow - view2.mRow;
                } else {
                    return view1.mCol - view2.mCol;
                }
            }
        }
    }

    /**
     * An edge list takes a hierarchy of elements and records the bounds of each element
     * into various lists such that it can answer queries about shared edges, about which
     * particular pixels occur as a boundary edge, etc.
     */
    private class EdgeList {
        private final Map<Element, View> mElementToViewMap = new HashMap<Element, View>(100);
        private final Map<String, View> mIdToViewMap = new HashMap<String, View>(100);
        private final Map<Integer, List<View>> mLeft = new HashMap<Integer, List<View>>();
        private final Map<Integer, List<View>> mTop = new HashMap<Integer, List<View>>();
        private final Map<Integer, List<View>> mRight = new HashMap<Integer, List<View>>();
        private final Map<Integer, List<View>> mBottom = new HashMap<Integer, List<View>>();
        private final Map<Element, Element> mSharedLeftEdge = new HashMap<Element, Element>();
        private final Map<Element, Element> mSharedTopEdge = new HashMap<Element, Element>();
        private final Map<Element, Element> mSharedRightEdge = new HashMap<Element, Element>();
        private final Map<Element, Element> mSharedBottomEdge = new HashMap<Element, Element>();
        private final List<Element> mDelete = new ArrayList<Element>();

        EdgeList(CanvasViewInfo view) {
            analyze(view, true);
            mDelete.remove(getElement(view));
        }

        public void setIdAttributeValue(View view, String id) {
            assert id.startsWith(NEW_ID_PREFIX) || id.startsWith(ID_PREFIX);
            view.mId = id;
            mIdToViewMap.put(getIdBasename(id), view);
        }

        public View getView(Element element) {
            return mElementToViewMap.get(element);
        }

        public View getView(String id) {
            return mIdToViewMap.get(id);
        }

        public List<View> getTopEdgeViews(Integer topOffset) {
            return mTop.get(topOffset);
        }

        public List<View> getLeftEdgeViews(Integer leftOffset) {
            return mLeft.get(leftOffset);
        }

        void record(Map<Integer, List<View>> map, Integer edge, View info) {
            List<View> list = map.get(edge);
            if (list == null) {
                list = new ArrayList<View>();
                map.put(edge, list);
            }
            list.add(info);
        }

        private List<Integer> getOffsets(Set<Integer> first, Set<Integer> second) {
            Set<Integer> joined = new HashSet<Integer>(first.size() + second.size());
            joined.addAll(first);
            joined.addAll(second);
            List<Integer> unique = new ArrayList<Integer>(joined);
            Collections.sort(unique);

            return unique;
        }

        public List<Element> getDeletedElements() {
            return mDelete;
        }

        public List<Integer> getColumnOffsets() {
            return getOffsets(mLeft.keySet(), mRight.keySet());
        }
        public List<Integer> getRowOffsets() {
            return getOffsets(mTop.keySet(), mBottom.keySet());
        }

        private View analyze(CanvasViewInfo view, boolean isRoot) {
            View added = null;
            if (!mFlatten || !isRemovableLayout(view)) {
                added = add(view);
                if (!isRoot) {
                    return added;
                }
            } else {
                mDelete.add(getElement(view));
            }

            Element parentElement = getElement(view);
            Rectangle parentBounds = view.getAbsRect();

            // Build up a table model of the view
            for (CanvasViewInfo child : view.getChildren()) {
                Rectangle childBounds = child.getAbsRect();
                Element childElement = getElement(child);

                // See if this view shares the edge with the removed
                // parent layout, and if so, record that such that we can
                // later handle attachments to the removed parent edges
                if (parentBounds.x == childBounds.x) {
                    mSharedLeftEdge.put(childElement, parentElement);
                }
                if (parentBounds.y == childBounds.y) {
                    mSharedTopEdge.put(childElement, parentElement);
                }
                if (parentBounds.x + parentBounds.width == childBounds.x + childBounds.width) {
                    mSharedRightEdge.put(childElement, parentElement);
                }
                if (parentBounds.y + parentBounds.height == childBounds.y + childBounds.height) {
                    mSharedBottomEdge.put(childElement, parentElement);
                }

                if (mFlatten && isRemovableLayout(child)) {
                    // When flattening, we want to disregard all layouts and instead
                    // add their children!
                    for (CanvasViewInfo childView : child.getChildren()) {
                        analyze(childView, false);

                        Element childViewElement = getElement(childView);
                        Rectangle childViewBounds = childView.getAbsRect();

                        // See if this view shares the edge with the removed
                        // parent layout, and if so, record that such that we can
                        // later handle attachments to the removed parent edges
                        if (parentBounds.x == childViewBounds.x) {
                            mSharedLeftEdge.put(childViewElement, parentElement);
                        }
                        if (parentBounds.y == childViewBounds.y) {
                            mSharedTopEdge.put(childViewElement, parentElement);
                        }
                        if (parentBounds.x + parentBounds.width == childViewBounds.x
                                + childViewBounds.width) {
                            mSharedRightEdge.put(childViewElement, parentElement);
                        }
                        if (parentBounds.y + parentBounds.height == childViewBounds.y
                                + childViewBounds.height) {
                            mSharedBottomEdge.put(childViewElement, parentElement);
                        }
                    }
                    mDelete.add(childElement);
                } else {
                    analyze(child, false);
                }
            }

            return added;
        }

        public View getSharedLeftEdge(Element element) {
            return getSharedEdge(element, mSharedLeftEdge);
        }

        public View getSharedRightEdge(Element element) {
            return getSharedEdge(element, mSharedRightEdge);
        }

        public View getSharedTopEdge(Element element) {
            return getSharedEdge(element, mSharedTopEdge);
        }

        public View getSharedBottomEdge(Element element) {
            return getSharedEdge(element, mSharedBottomEdge);
        }

        private View getSharedEdge(Element element, Map<Element, Element> sharedEdgeMap) {
            Element original = element;

            while (element != null) {
                View view = getView(element);
                if (view != null) {
                    assert isAncestor(element, original);
                    return view;
                }
                element = sharedEdgeMap.get(element);
            }

            return null;
        }

        private View add(CanvasViewInfo info) {
            Rectangle bounds = info.getAbsRect();
            Element element = getElement(info);
            View view = new View(info, element);
            mElementToViewMap.put(element, view);
            record(mLeft, Integer.valueOf(bounds.x), view);
            record(mTop, Integer.valueOf(bounds.y), view);
            record(mRight, Integer.valueOf(view.getRightEdge()), view);
            record(mBottom, Integer.valueOf(view.getBottomEdge()), view);
            return view;
        }

        /**
         * Returns true if the given {@link CanvasViewInfo} represents an element we
         * should remove in a flattening conversion. We don't want to remove non-layout
         * views, or layout views that for example contain drawables on their own.
         */
        private boolean isRemovableLayout(CanvasViewInfo child) {
            // The element being converted is NOT removable!
            Element element = getElement(child);
            if (element == mLayout) {
                return false;
            }

            ElementDescriptor descriptor = child.getUiViewNode().getDescriptor();
            String name = descriptor.getXmlLocalName();
            if (name.equals(LINEAR_LAYOUT) || name.equals(RELATIVE_LAYOUT)) {
                // Don't delete layouts that provide a background image or gradient
                if (element.hasAttributeNS(ANDROID_URI, ATTR_BACKGROUND)) {
                    AdtPlugin.log(IStatus.WARNING,
                            "Did not flatten layout %1$s because it defines a '%2$s' attribute",
                            VisualRefactoring.getId(element), ATTR_BACKGROUND);
                    return false;
                }

                return true;
            }

            return false;
        }
    }
}
