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
import static com.android.SdkConstants.ATTR_COLUMN_COUNT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.FQCN_SPACE;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL_HORIZONTAL;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL_VERTICAL;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.RADIO_GROUP;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.SPACE;
import static com.android.SdkConstants.TABLE_LAYOUT;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_HORIZONTAL;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VALUE_VERTICAL;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_HORIZ_MASK;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_VERT_MASK;

import com.android.ide.common.api.IViewMetadata.FillPreference;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.common.layout.GravityHelper;
import org.eclipse.andmore.common.layout.GridLayoutRule;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.project.SupportLibraryHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class which performs the bulk of the layout conversion to grid layout
 * <p>
 * Future enhancements:
 * <ul>
 * <li>Render the layout at multiple screen sizes and analyze how the widget bounds
 *  change and use this to infer gravity
 *  <li> Use the layout_width and layout_height attributes on views to infer column and
 *  row flexibility (and as mentioned above, possibly layout_weight).
 * move and stretch and use that to add in additional constraints
 *  <li> Take into account existing margins and add/subtract those from the
 *  bounds computations and either clear or update them.
 * <li>Try to reorder elements into their natural order
 * <li> Try to preserve spacing? Right now everything gets converted into a compact
 *   grid with no spacing between the views; consider inserting {@code <Space>} views
 *   with dimensions based on existing distances.
 * </ul>
 */
@SuppressWarnings("restriction") // DOM model access
class GridLayoutConverter {
    private final MultiTextEdit mRootEdit;
    private final boolean mFlatten;
    private final Element mLayout;
    private final ChangeLayoutRefactoring mRefactoring;
    private final CanvasViewInfo mRootView;

    private List<View> mViews;
    private String mNamespace;
    private int mColumnCount;

    /** Creates a new {@link GridLayoutConverter} */
    GridLayoutConverter(ChangeLayoutRefactoring refactoring,
            Element layout, boolean flatten, MultiTextEdit rootEdit, CanvasViewInfo rootView) {
        mRefactoring = refactoring;
        mLayout = layout;
        mFlatten = flatten;
        mRootEdit = rootEdit;
        mRootView = rootView;
    }

    /** Performs conversion from any layout to a RelativeLayout */
    public void convertToGridLayout() {
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
        GridModel gridModel = new GridModel(layoutView, mLayout, mFlatten);
        mViews = gridModel.getViews();
        mColumnCount = gridModel.computeColumnCount();

        deleteRemovedElements(gridModel.getDeletedElements());
        mNamespace = mRefactoring.getAndroidNamespacePrefix();

        processGravities();

        // Insert space views if necessary
        insertStretchableSpans();

        // Create/update relative layout constraints
        assignGridAttributes();

        removeUndefinedAttrs();

        if (mColumnCount > 0) {
            mRefactoring.setAttribute(mRootEdit, mLayout, ANDROID_URI,
                mNamespace, ATTR_COLUMN_COUNT, Integer.toString(mColumnCount));
        }
    }

    private void insertStretchableSpans() {
        // Look at the rows and columns and determine if we need to have a stretchable
        // row and/or a stretchable column in the layout.
        // In a GridLayout, a row or column is stretchable if it defines a gravity (regardless
        // of what the gravity is -- in other words, a column is not just stretchable if it
        // has gravity=fill but also if it has gravity=left). Furthermore, ALL the elements
        // in the row/column have to be stretchable for the overall row/column to be
        // considered stretchable.

        // Map from row index to boolean for "is the row fixed/inflexible?"
        Map<Integer, Boolean> rowFixed = new HashMap<Integer, Boolean>();
        Map<Integer, Boolean> columnFixed = new HashMap<Integer, Boolean>();
        for (View view : mViews) {
            if (view.mElement == mLayout) {
                continue;
            }

            int gravity = GravityHelper.getGravity(view.mGravity, 0);
            if ((gravity & GRAVITY_HORIZ_MASK) == 0) {
                columnFixed.put(view.mCol, true);
            } else if (!columnFixed.containsKey(view.mCol)) {
                columnFixed.put(view.mCol, false);
            }
            if ((gravity & GRAVITY_VERT_MASK) == 0) {
                rowFixed.put(view.mRow, true);
            } else if (!rowFixed.containsKey(view.mRow)) {
                rowFixed.put(view.mRow, false);
            }
        }

        boolean hasStretchableRow = false;
        boolean hasStretchableColumn = false;
        for (boolean fixed : rowFixed.values()) {
            if (!fixed) {
                hasStretchableRow = true;
            }
        }
        for (boolean fixed : columnFixed.values()) {
            if (!fixed) {
                hasStretchableColumn = true;
            }
        }

        if (!hasStretchableRow || !hasStretchableColumn) {
            // Insert <Space> to hold stretchable space
            // TODO: May also have to increment column count!
            int offset = 0; // WHERE?

            String gridLayout = mLayout.getTagName();
            if (mLayout instanceof IndexedRegion) {
                IndexedRegion region = (IndexedRegion) mLayout;
                int end = region.getEndOffset();
                // TODO: Look backwards for the "</"
                // (and can it ever be <foo/>) ?
                end -= (gridLayout.length() + 3); // 3: <, /, >
                offset = end;
            }

            int row = rowFixed.size();
            int column = columnFixed.size();
            StringBuilder sb = new StringBuilder(64);
            String spaceTag = SPACE;
            IFile file = mRefactoring.getFile();
            if (file != null) {
                spaceTag = SupportLibraryHelper.getTagFor(file.getProject(), FQCN_SPACE);
                if (spaceTag.equals(FQCN_SPACE)) {
                    spaceTag = SPACE;
                }
            }

            sb.append('<').append(spaceTag).append(' ');
            String gravity;
            if (!hasStretchableRow && !hasStretchableColumn) {
                gravity = GRAVITY_VALUE_FILL;
            } else if (!hasStretchableRow) {
                gravity = GRAVITY_VALUE_FILL_VERTICAL;
            } else {
                assert !hasStretchableColumn;
                gravity = GRAVITY_VALUE_FILL_HORIZONTAL;
            }

            sb.append(mNamespace).append(':');
            sb.append(ATTR_LAYOUT_GRAVITY).append('=').append('"').append(gravity);
            sb.append('"').append(' ');

            sb.append(mNamespace).append(':');
            sb.append(ATTR_LAYOUT_ROW).append('=').append('"').append(Integer.toString(row));
            sb.append('"').append(' ');

            sb.append(mNamespace).append(':');
            sb.append(ATTR_LAYOUT_COLUMN).append('=').append('"').append(Integer.toString(column));
            sb.append('"').append('/').append('>');

            String space = sb.toString();
            InsertEdit replace = new InsertEdit(offset, space);
            mRootEdit.addChild(replace);

            mColumnCount++;
        }
    }

    private void removeUndefinedAttrs() {
        ViewElementDescriptor descriptor = mRefactoring.getElementDescriptor(FQCN_GRID_LAYOUT);
        if (descriptor == null) {
            return;
        }

        Set<String> defined = new HashSet<String>();
        AttributeDescriptor[] layoutAttributes = descriptor.getLayoutAttributes();
        for (AttributeDescriptor attribute : layoutAttributes) {
            defined.add(attribute.getXmlLocalName());
        }

        for (View view : mViews) {
            Element child = view.mElement;

            List<Attr> attributes = mRefactoring.findLayoutAttributes(child);
            for (Attr attribute : attributes) {
                String name = attribute.getLocalName();
                if (!defined.contains(name)) {
                    // Remove it
                    try {
                        mRefactoring.removeAttribute(mRootEdit, child, attribute.getNamespaceURI(),
                                name);
                    } catch (MalformedTreeException mte) {
                        // Sometimes refactoring has modified attribute; not
                        // removing
                        // it is non-fatal so just warn instead of letting
                        // refactoring
                        // operation abort
                        AdtPlugin.log(IStatus.WARNING,
                                "Could not remove unsupported attribute %1$s; " + //$NON-NLS-1$
                                        "already modified during refactoring?", //$NON-NLS-1$
                                attribute.getLocalName());
                    }
                }
            }
        }
    }

    /** Removes any elements targeted for deletion */
    private void deleteRemovedElements(List<Element> delete) {
        if (mFlatten && delete.size() > 0) {
            for (Element element : delete) {
                mRefactoring.removeElementTags(mRootEdit, element, delete,
                        false /*changeIndentation*/);
            }
        }
    }

    /**
     * Creates refactoring edits which adds or updates the grid attributes
     */
    private void assignGridAttributes() {
        // We always convert to horizontal grid layouts for now
        mRefactoring.setAttribute(mRootEdit, mLayout, ANDROID_URI,
                mNamespace, ATTR_ORIENTATION, VALUE_HORIZONTAL);

        assignCellAttributes();
    }

    /**
     * Assign cell attributes to the table, skipping those that will be implied
     * by the grid model
     */
    private void assignCellAttributes() {
        int implicitRow = 0;
        int implicitColumn = 0;
        int nextRow = 0;
        for (View view : mViews) {
            Element element = view.getElement();
            if (element == mLayout) {
                continue;
            }

            int row = view.getRow();
            int column = view.getColumn();

            if (column != implicitColumn && (implicitColumn > 0 || implicitRow > 0)) {
                mRefactoring.setAttribute(mRootEdit, element, ANDROID_URI,
                        mNamespace, ATTR_LAYOUT_COLUMN, Integer.toString(column));
                if (column < implicitColumn) {
                    implicitRow++;
                }
                implicitColumn = column;
            }
            if (row != implicitRow) {
                mRefactoring.setAttribute(mRootEdit, element, ANDROID_URI,
                        mNamespace, ATTR_LAYOUT_ROW, Integer.toString(row));
                implicitRow = row;
            }

            int rowSpan = view.getRowSpan();
            int columnSpan = view.getColumnSpan();
            assert columnSpan >= 1;

            if (rowSpan > 1) {
                mRefactoring.setAttribute(mRootEdit, element, ANDROID_URI,
                        mNamespace, ATTR_LAYOUT_ROW_SPAN, Integer.toString(rowSpan));
            }
            if (columnSpan > 1) {
                mRefactoring.setAttribute(mRootEdit, element, ANDROID_URI,
                        mNamespace, ATTR_LAYOUT_COLUMN_SPAN,
                        Integer.toString(columnSpan));
            }
            nextRow = Math.max(nextRow, row + rowSpan);

            // wrap_content is redundant in GridLayouts
            Attr width = element.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
            if (width != null && VALUE_WRAP_CONTENT.equals(width.getValue())) {
                mRefactoring.removeAttribute(mRootEdit, width);
            }
            Attr height = element.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
            if (height != null && VALUE_WRAP_CONTENT.equals(height.getValue())) {
                mRefactoring.removeAttribute(mRootEdit, height);
            }

            // Fix up children moved from LinearLayouts that have "invalid" sizes that
            // was intended for layout weight handling in their old parent
            if (LINEAR_LAYOUT.equals(element.getParentNode().getNodeName())) {
                convert0dipToWrapContent(element);
            }

            implicitColumn += columnSpan;
            if (implicitColumn >= mColumnCount) {
                implicitColumn = 0;
                assert nextRow > implicitRow;
                implicitRow = nextRow;
            }
        }
    }

    private void processGravities() {
        for (View view : mViews) {
            Element element = view.getElement();
            if (element == mLayout) {
                continue;
            }

            Attr width = element.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
            Attr height = element.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
            String gravity = element.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_GRAVITY);
            String newGravity = null;
            if (width != null && (VALUE_MATCH_PARENT.equals(width.getValue()) ||
                    VALUE_FILL_PARENT.equals(width.getValue()))) {
                mRefactoring.removeAttribute(mRootEdit, width);
                newGravity = gravity = GRAVITY_VALUE_FILL_HORIZONTAL;
            }
            if (height != null && (VALUE_MATCH_PARENT.equals(height.getValue()) ||
                    VALUE_FILL_PARENT.equals(height.getValue()))) {
                mRefactoring.removeAttribute(mRootEdit, height);
                if (newGravity == GRAVITY_VALUE_FILL_HORIZONTAL) {
                    newGravity = GRAVITY_VALUE_FILL;
                } else {
                    newGravity = GRAVITY_VALUE_FILL_VERTICAL;
                }
                gravity = newGravity;
            }

            if (gravity == null || gravity.length() == 0) {
                ElementDescriptor descriptor = view.mInfo.getUiViewNode().getDescriptor();
                if (descriptor instanceof ViewElementDescriptor) {
                    ViewElementDescriptor viewDescriptor = (ViewElementDescriptor) descriptor;
                    String fqcn = viewDescriptor.getFullClassName();
                    FillPreference fill = ViewMetadataRepository.get().getFillPreference(fqcn);
                    gravity = GridLayoutRule.computeDefaultGravity(fill);
                    if (gravity != null) {
                        newGravity = gravity;
                    }
                }
            }

            if (newGravity != null) {
                mRefactoring.setAttribute(mRootEdit, element, ANDROID_URI,
                        mNamespace, ATTR_LAYOUT_GRAVITY, newGravity);
            }

            view.mGravity = newGravity != null ? newGravity : gravity;
        }
    }


    /** Converts 0dip values in layout_width and layout_height to wrap_content instead */
    private void convert0dipToWrapContent(Element child) {
        // Must convert layout_height="0dip" to layout_height="wrap_content".
        // (And since wrap_content is the default, what we really do is remove
        // the attribute completely.)
        // 0dip is a special trick used in linear layouts in the presence of
        // weights where 0dip ensures that the height of the view is not taken
        // into account when distributing the weights. However, when converted
        // to RelativeLayout this will instead cause the view to actually be assigned
        // 0 height.
        Attr height = child.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
        // 0dip, 0dp, 0px, etc
        if (height != null && height.getValue().startsWith("0")) { //$NON-NLS-1$
            mRefactoring.removeAttribute(mRootEdit, height);
        }
        Attr width = child.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
        if (width != null && width.getValue().startsWith("0")) { //$NON-NLS-1$
            mRefactoring.removeAttribute(mRootEdit, width);
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


    /** Holds layout information about an individual view */
    private static class View {
        private final Element mElement;
        private int mRow = -1;
        private int mCol = -1;
        private int mRowSpan = -1;
        private int mColSpan = -1;
        private int mX1;
        private int mY1;
        private int mX2;
        private int mY2;
        private CanvasViewInfo mInfo;
        private String mGravity;

        public View(CanvasViewInfo view, Element element) {
            mInfo = view;
            mElement = element;

            Rectangle b = mInfo.getAbsRect();
            mX1 = b.x;
            mX2 = b.x + b.width;
            mY1 = b.y;
            mY2 = b.y + b.height;
        }

        /**
         * Returns the element for this view
         *
         * @return the element for the view
         */
        public Element getElement() {
            return mElement;
        }

        /**
         * The assigned row for this view
         *
         * @return the assigned row
         */
        public int getRow() {
            return mRow;
        }

        /**
         * The assigned column for this view
         *
         * @return the assigned column
         */
        public int getColumn() {
            return mCol;
        }

        /**
         * The assigned row span for this view
         *
         * @return the assigned row span
         */
        public int getRowSpan() {
            return mRowSpan;
        }

        /**
         * The assigned column span for this view
         *
         * @return the assigned column span
         */
        public int getColumnSpan() {
            return mColSpan;
        }

        /**
         * The left edge of the view to be used for placement
         *
         * @return the left edge x coordinate
         */
        public int getLeftEdge() {
            return mX1;
        }

        /**
         * The top edge of the view to be used for placement
         *
         * @return the top edge y coordinate
         */
        public int getTopEdge() {
            return mY1;
        }

        /**
         * The right edge of the view to be used for placement
         *
         * @return the right edge x coordinate
         */
        public int getRightEdge() {
            return mX2;
        }

        /**
         * The bottom edge of the view to be used for placement
         *
         * @return the bottom edge y coordinate
         */
        public int getBottomEdge() {
            return mY2;
        }

        @Override
        public String toString() {
            return "View(" + VisualRefactoring.getId(mElement) + ": " + mX1 + "," + mY1 + ")";
        }
    }

    /** Grid model for the views found in the view hierarchy, partitioned into rows and columns */
    private static class GridModel {
        private final List<View> mViews = new ArrayList<View>();
        private final List<Element> mDelete = new ArrayList<Element>();
        private final Map<Element, View> mElementToView = new HashMap<Element, View>();
        private Element mLayout;
        private boolean mFlatten;

        GridModel(CanvasViewInfo view, Element layout, boolean flatten) {
            mLayout = layout;
            mFlatten = flatten;

            scan(view, true);
            analyzeKnownLayouts();
            initializeColumns();
            initializeRows();
            mDelete.remove(getElement(view));
        }

        /**
         * Returns the {@link View} objects to be placed in the grid
         *
         * @return list of {@link View} objects, never null but possibly empty
         */
        public List<View> getViews() {
            return mViews;
        }

        /**
         * Returns the list of elements that are scheduled for deletion in the
         * flattening operation
         *
         * @return elements to be deleted, never null but possibly empty
         */
        public List<Element> getDeletedElements() {
            return mDelete;
        }

        /**
         * Compute and return column count
         *
         * @return the column count
         */
        public int computeColumnCount() {
            int columnCount = 0;
            for (View view : mViews) {
                if (view.getElement() == mLayout) {
                    continue;
                }

                int column = view.getColumn();
                int columnSpan = view.getColumnSpan();
                if (column + columnSpan > columnCount) {
                    columnCount = column + columnSpan;
                }
            }
            return columnCount;
        }

        /**
         * Initializes the column and columnSpan attributes of the views
         */
        private void initializeColumns() {
            // Now initialize table view row, column and spans
            Map<Integer, List<View>> mColumnViews = new HashMap<Integer, List<View>>();
            for (View view : mViews) {
                if (view.mElement == mLayout) {
                    continue;
                }
                int x = view.getLeftEdge();
                List<View> list = mColumnViews.get(x);
                if (list == null) {
                    list = new ArrayList<View>();
                    mColumnViews.put(x, list);
                }
                list.add(view);
            }

            List<Integer> columnOffsets = new ArrayList<Integer>(mColumnViews.keySet());
            Collections.sort(columnOffsets);

            int columnIndex = 0;
            for (Integer column : columnOffsets) {
                List<View> views = mColumnViews.get(column);
                if (views != null) {
                    for (View view : views) {
                        view.mCol = columnIndex;
                    }
                }
                columnIndex++;
            }
            // Initialize column spans
            for (View view : mViews) {
                if (view.mElement == mLayout) {
                    continue;
                }
                int index = Collections.binarySearch(columnOffsets, view.getRightEdge());
                int column;
                if (index == -1) {
                    // Smaller than the first element; just use the first column
                    column = 0;
                } else if (index < 0) {
                    column = -(index + 2);
                } else {
                    column = index;
                }

                if (column < view.mCol) {
                    column = view.mCol;
                }

                view.mColSpan = column - view.mCol + 1;
            }
        }

        /**
         * Initializes the row and rowSpan attributes of the views
         */
        private void initializeRows() {
            Map<Integer, List<View>> mRowViews = new HashMap<Integer, List<View>>();
            for (View view : mViews) {
                if (view.mElement == mLayout) {
                    continue;
                }
                int y = view.getTopEdge();
                List<View> list = mRowViews.get(y);
                if (list == null) {
                    list = new ArrayList<View>();
                    mRowViews.put(y, list);
                }
                list.add(view);
            }

            List<Integer> rowOffsets = new ArrayList<Integer>(mRowViews.keySet());
            Collections.sort(rowOffsets);

            int rowIndex = 0;
            for (Integer row : rowOffsets) {
                List<View> views = mRowViews.get(row);
                if (views != null) {
                    for (View view : views) {
                        view.mRow = rowIndex;
                    }
                }
                rowIndex++;
            }

            // Initialize row spans
            for (View view : mViews) {
                if (view.mElement == mLayout) {
                    continue;
                }
                int index = Collections.binarySearch(rowOffsets, view.getBottomEdge());
                int row;
                if (index == -1) {
                    // Smaller than the first element; just use the first row
                    row = 0;
                } else if (index < 0) {
                    row = -(index + 2);
                } else {
                    row = index;
                }

                if (row < view.mRow) {
                    row = view.mRow;
                }

                view.mRowSpan = row - view.mRow + 1;
            }
        }

        /**
         * Walks over a given view hierarchy and locates views to be placed in
         * the grid layout (or deleted if we are flattening the hierarchy)
         *
         * @param view the view to analyze
         * @param isRoot whether this view is the root (which cannot be removed)
         * @return the {@link View} object for the {@link CanvasViewInfo}
         *         hierarchy we just analyzed, or null
         */
        private View scan(CanvasViewInfo view, boolean isRoot) {
            View added = null;
            if (!mFlatten || !isRemovableLayout(view)) {
                added = add(view);
                if (!isRoot) {
                    return added;
                }
            } else {
                mDelete.add(getElement(view));
            }

            // Build up a table model of the view
            for (CanvasViewInfo child : view.getChildren()) {
                Element childElement = getElement(child);

                // See if this view shares the edge with the removed
                // parent layout, and if so, record that such that we can
                // later handle attachments to the removed parent edges

                if (mFlatten && isRemovableLayout(child)) {
                    // When flattening, we want to disregard all layouts and instead
                    // add their children!
                    for (CanvasViewInfo childView : child.getChildren()) {
                        scan(childView, false);
                    }
                    mDelete.add(childElement);
                } else {
                    scan(child, false);
                }
            }

            return added;
        }

        /** Adds the given {@link CanvasViewInfo} into our internal view list */
        private View add(CanvasViewInfo info) {
            Element element = getElement(info);
            View view = new View(info, element);
            mViews.add(view);
            mElementToView.put(element, view);
            return view;
        }

        private void analyzeKnownLayouts() {
            Set<Element> parents = new HashSet<Element>();
            for (View view : mViews) {
                Node parent = view.getElement().getParentNode();
                if (parent instanceof Element) {
                    parents.add((Element) parent);
                }
            }

            List<Collection<View>> rowGroups = new ArrayList<Collection<View>>();
            List<Collection<View>> columnGroups = new ArrayList<Collection<View>>();
            for (Element parent : parents) {
                String tagName = parent.getTagName();
                if (tagName.equals(LINEAR_LAYOUT) || tagName.equals(TABLE_LAYOUT) ||
                        tagName.equals(TABLE_ROW) || tagName.equals(RADIO_GROUP)) {
                    Set<View> group = new HashSet<View>();
                    for (Element child : DomUtilities.getChildren(parent)) {
                        View view = mElementToView.get(child);
                        if (view != null) {
                            group.add(view);
                        }
                    }
                    if (group.size() > 1) {
                        boolean isVertical = VALUE_VERTICAL.equals(parent.getAttributeNS(
                                ANDROID_URI, ATTR_ORIENTATION));
                        if (tagName.equals(TABLE_LAYOUT)) {
                            isVertical = true;
                        } else if (tagName.equals(TABLE_ROW)) {
                            isVertical = false;
                        }
                        if (isVertical) {
                            columnGroups.add(group);
                        } else {
                            rowGroups.add(group);
                        }
                    }
                } else if (tagName.equals(RELATIVE_LAYOUT)) {
                    List<Element> children = DomUtilities.getChildren(parent);
                    for (Element child : children) {
                        View view = mElementToView.get(child);
                        if (view == null) {
                            continue;
                        }
                        NamedNodeMap attributes = child.getAttributes();
                        for (int i = 0, n = attributes.getLength(); i < n; i++) {
                            Attr attr = (Attr) attributes.item(i);
                            String name = attr.getLocalName();
                            if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                                boolean alignVertical =
                                        name.equals(ATTR_LAYOUT_ALIGN_TOP) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_BOTTOM) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_BASELINE);
                                boolean alignHorizontal =
                                        name.equals(ATTR_LAYOUT_ALIGN_LEFT) ||
                                        name.equals(ATTR_LAYOUT_ALIGN_RIGHT);
                                if (!alignVertical && !alignHorizontal) {
                                    continue;
                                }
                                String value = attr.getValue();
                                if (value.startsWith(ID_PREFIX)
                                        || value.startsWith(NEW_ID_PREFIX)) {
                                    String targetName = BaseViewRule.stripIdPrefix(value);
                                    Element target = null;
                                    for (Element c : children) {
                                        String id = VisualRefactoring.getId(c);
                                        if (targetName.equals(BaseViewRule.stripIdPrefix(id))) {
                                            target = c;
                                            break;
                                        }
                                    }
                                    View targetView = mElementToView.get(target);
                                    if (targetView != null) {
                                        List<View> group = new ArrayList<View>(2);
                                        group.add(view);
                                        group.add(targetView);
                                        if (alignHorizontal) {
                                            columnGroups.add(group);
                                        } else {
                                            assert alignVertical;
                                            rowGroups.add(group);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TODO: Consider looking for interesting metadata from other layouts
                }
            }

            // Assign the same top or left coordinates to the groups to ensure that they
            // all get positioned in the same row or column
            for (Collection<View> rowGroup : rowGroups) {
                // Find the smallest one
                Iterator<View> iterator = rowGroup.iterator();
                int smallest = iterator.next().mY1;
                while (iterator.hasNext()) {
                    smallest = Math.min(smallest, iterator.next().mY1);
                }
                for (View view : rowGroup) {
                   view.mY2 -= (view.mY1 - smallest);
                   view.mY1 = smallest;
                }
            }
            for (Collection<View> columnGroup : columnGroups) {
                Iterator<View> iterator = columnGroup.iterator();
                int smallest = iterator.next().mX1;
                while (iterator.hasNext()) {
                    smallest = Math.min(smallest, iterator.next().mX1);
                }
                for (View view : columnGroup) {
                   view.mX2 -= (view.mX1 - smallest);
                   view.mX1 = smallest;
                }
            }
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
            if (name.equals(LINEAR_LAYOUT) || name.equals(RELATIVE_LAYOUT)
                    || name.equals(TABLE_LAYOUT) || name.equals(TABLE_ROW)) {
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
