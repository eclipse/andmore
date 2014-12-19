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
package org.eclipse.andmore.common.layout.grid;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLUMN_COUNT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ATTR_ROW_COUNT;
import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.FQCN_SPACE;
import static com.android.SdkConstants.FQCN_SPACE_V7;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.SPACE;
import static com.android.SdkConstants.VALUE_BOTTOM;
import static com.android.SdkConstants.VALUE_CENTER_VERTICAL;
import static com.android.SdkConstants.VALUE_N_DP;
import static com.android.SdkConstants.VALUE_TOP;
import static com.android.SdkConstants.VALUE_VERTICAL;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_BOTTOM;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_HORIZ;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_VERT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_RIGHT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewMetadata;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.utils.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.common.layout.GravityHelper;
import org.eclipse.andmore.common.layout.GridLayoutRule;

/** Models a GridLayout */
public class GridModel {
    /** Marker value used to indicate values (rows, columns, etc) which have not been set */
    static final int UNDEFINED = Integer.MIN_VALUE;

    /** The size of spacers in the dimension that they are not defining */
    static final int SPACER_SIZE_DP = 1;

    /** Attribute value used for {@link #SPACER_SIZE_DP} */
    private static final String SPACER_SIZE = String.format(VALUE_N_DP, SPACER_SIZE_DP);

    /** Width assigned to a newly added column with the Add Column action */
    private static final int DEFAULT_CELL_WIDTH = 100;

    /** Height assigned to a newly added row with the Add Row action */
    private static final int DEFAULT_CELL_HEIGHT = 15;

    /** The GridLayout node, never null */
    public final INode layout;

    /** True if this is a vertical layout, and false if it is horizontal (the default) */
    public boolean vertical;

    /** The declared count of rows (which may be {@link #UNDEFINED} if not specified) */
    public int declaredRowCount;

    /** The declared count of columns (which may be {@link #UNDEFINED} if not specified) */
    public int declaredColumnCount;

    /** The actual count of rows found in the grid */
    public int actualRowCount;

    /** The actual count of columns found in the grid */
    public int actualColumnCount;

    /**
     * Array of positions (indexed by column) of the left edge of table cells; this
     * corresponds to the column positions in the grid
     */
    private int[] mLeft;

    /**
     * Array of positions (indexed by row) of the top edge of table cells; this
     * corresponds to the row positions in the grid
     */
    private int[] mTop;

    /**
     * Array of positions (indexed by column) of the maximum right hand side bounds of a
     * node in the given column; this represents the visual edge of a column even when the
     * actual column is wider
     */
    private int[] mMaxRight;

    /**
     * Array of positions (indexed by row) of the maximum bottom bounds of a node in the
     * given row; this represents the visual edge of a row even when the actual row is
     * taller
     */
    private int[] mMaxBottom;

    /**
     * Array of baselines computed for the rows. This array is populated lazily and should
     * not be accessed directly; call {@link #getBaseline(int)} instead.
     */
    private int[] mBaselines;

    /** List of all the view data for the children in this layout */
    private List<ViewData> mChildViews;

    /** The {@link IClientRulesEngine} */
    private final IClientRulesEngine mRulesEngine;

    /**
     * An actual instance of a GridLayout object that this grid model corresponds to.
     */
    private Object mViewObject;

    /** The namespace to use for attributes */
    private String mNamespace;

    /**
     * Constructs a {@link GridModel} for the given layout
     *
     * @param rulesEngine the associated rules engine
     * @param node the GridLayout node
     * @param viewObject an actual GridLayout instance, or null
     */
    private GridModel(IClientRulesEngine rulesEngine, INode node, Object viewObject) {
        mRulesEngine = rulesEngine;
        layout = node;
        mViewObject = viewObject;
        loadFromXml();
    }

    // Factory cache for most recent item (used primarily because during paints and drags
    // the grid model is called repeatedly for the same view object.)
    private static WeakReference<Object> sCachedViewObject = new WeakReference<Object>(null);
    private static WeakReference<GridModel> sCachedViewModel;

    /**
     * Factory which returns a grid model for the given node.
     *
     * @param rulesEngine the associated rules engine
     * @param node the GridLayout node
     * @param viewObject an actual GridLayout instance, or null
     * @return a new model
     */
    @NonNull
    public static GridModel get(
            @NonNull IClientRulesEngine rulesEngine,
            @NonNull INode node,
            @Nullable Object viewObject) {
        if (viewObject != null && viewObject == sCachedViewObject.get()) {
            GridModel model = sCachedViewModel.get();
            if (model != null) {
                return model;
            }
        }

        GridModel model = new GridModel(rulesEngine, node, viewObject);
        sCachedViewModel = new WeakReference<GridModel>(model);
        sCachedViewObject = new WeakReference<Object>(viewObject);
        return model;
    }

    /**
     * Returns the {@link ViewData} for the child at the given index
     *
     * @param index the position of the child node whose view we want to look up
     * @return the corresponding {@link ViewData}
     */
    public ViewData getView(int index) {
        return mChildViews.get(index);
    }

    /**
     * Returns the {@link ViewData} for the given child node.
     *
     * @param node the node for which we want the view info
     * @return the view info for the node, or null if not found
     */
    public ViewData getView(INode node) {
        for (ViewData view : mChildViews) {
            if (view.node == node) {
                return view;
            }
        }

        return null;
    }

    /**
     * Computes the index (among the children nodes) to insert a new node into which
     * should be positioned at the given row and column. This will skip over any nodes
     * that have implicit positions earlier than the given node, and will also ensure that
     * all nodes are placed before the spacer nodes.
     *
     * @param row the target row of the new node
     * @param column the target column of the new node
     * @return the insert position to use or -1 if no preference is found
     */
    public int getInsertIndex(int row, int column) {
        if (vertical) {
            for (ViewData view : mChildViews) {
                if (view.column > column || view.column == column && view.row >= row) {
                    return view.index;
                }
            }
        } else {
            for (ViewData view : mChildViews) {
                if (view.row > row || view.row == row && view.column >= column) {
                    return view.index;
                }
            }
        }

        // Place it before the first spacer
        for (ViewData view : mChildViews) {
            if (view.isSpacer()) {
                return view.index;
            }
        }

        return -1;
    }

    /**
     * Returns the baseline of the given row, or -1 if none is found. This looks for views
     * in the row which have baseline vertical alignment and also define their own
     * baseline, and returns the first such match.
     *
     * @param row the row to look up a baseline for
     * @return the baseline relative to the row position, or -1 if not defined
     */
    public int getBaseline(int row) {
        if (row < 0 || row >= mBaselines.length) {
            return -1;
        }

        int baseline = mBaselines[row];
        if (baseline == UNDEFINED) {
            baseline = -1;

            // TBD: Consider stringing together row information in the view data
            // so I can quickly identify the views in a given row instead of searching
            // among all?
            for (ViewData view : mChildViews) {
                // We only count baselines for views with rowSpan=1 because
                // baseline alignment doesn't work for cell spanning views
                if (view.row == row && view.rowSpan == 1) {
                    baseline = view.node.getBaseline();
                    if (baseline != -1) {
                        // Even views that do have baselines do not count towards a row
                        // baseline if they have a vertical gravity
                        String gravity = getGridAttribute(view.node, ATTR_LAYOUT_GRAVITY);
                        if (gravity == null
                                || !(gravity.contains(VALUE_TOP)
                                        || gravity.contains(VALUE_BOTTOM)
                                        || gravity.contains(VALUE_CENTER_VERTICAL))) {
                            // Compute baseline relative to the row, not the view itself
                            baseline += view.node.getBounds().y - getRowY(row);
                            break;
                        }
                    }
                }
            }
            mBaselines[row] = baseline;
        }

        return baseline;
    }

    /** Applies the row and column values into the XML */
    void applyPositionAttributes() {
        for (ViewData view : mChildViews) {
            view.applyPositionAttributes();
        }

        // Also fix the columnCount
        if (getGridAttribute(layout, ATTR_COLUMN_COUNT) != null &&
                declaredColumnCount > actualColumnCount) {
            setGridAttribute(layout, ATTR_COLUMN_COUNT, actualColumnCount);
        }
    }

    /**
     * Sets the given GridLayout attribute (rowCount, layout_row, etc) to the
     * given value. This automatically handles using the right XML namespace
     * based on whether the GridLayout is the android.widget.GridLayout, or the
     * support library GridLayout, and whether it's in a library project or not
     * etc.
     *
     * @param node the node to apply the attribute to
     * @param name the local name of the attribute
     * @param value the integer value to set the attribute to
     */
    public void setGridAttribute(INode node, String name, int value) {
        setGridAttribute(node, name, Integer.toString(value));
    }

    /**
     * Sets the given GridLayout attribute (rowCount, layout_row, etc) to the
     * given value. This automatically handles using the right XML namespace
     * based on whether the GridLayout is the android.widget.GridLayout, or the
     * support library GridLayout, and whether it's in a library project or not
     * etc.
     *
     * @param node the node to apply the attribute to
     * @param name the local name of the attribute
     * @param value the string value to set the attribute to, or null to clear
     *            it
     */
    public void setGridAttribute(INode node, String name, String value) {
        node.setAttribute(getNamespace(), name, value);
    }

    /**
     * Returns the namespace URI to use for GridLayout-specific attributes, such
     * as columnCount, layout_column, layout_column_span, layout_gravity etc.
     *
     * @return the namespace, never null
     */
    public String getNamespace() {
        if (mNamespace == null) {
            mNamespace = ANDROID_URI;

            String fqcn = layout.getFqcn();
            if (!fqcn.equals(GRID_LAYOUT) && !fqcn.equals(FQCN_GRID_LAYOUT)) {
                mNamespace = mRulesEngine.getAppNameSpace();
            }
        }

        return mNamespace;
    }

    /** Removes the given flag from a flag attribute value and returns the result */
    static String removeFlag(String flag, String value) {
        if (value.equals(flag)) {
            return null;
        }
        // Handle spaces between pipes and flag are a prefix, suffix and interior occurrences
        int index = value.indexOf(flag);
        if (index != -1) {
            int pipe = value.lastIndexOf('|', index);
            int endIndex = index + flag.length();
            if (pipe != -1) {
                value = value.substring(0, pipe).trim() + value.substring(endIndex).trim();
            } else {
                pipe = value.indexOf('|', endIndex);
                if (pipe != -1) {
                    value = value.substring(0, index).trim() + value.substring(pipe + 1).trim();
                } else {
                    value = value.substring(0, index).trim() + value.substring(endIndex).trim();
                }
            }
        }

        return value;
    }

    /**
     * Loads a {@link GridModel} from the XML model.
     */
    private void loadFromXml() {
        INode[] children = layout.getChildren();

        declaredRowCount = getGridAttribute(layout, ATTR_ROW_COUNT, UNDEFINED);
        declaredColumnCount = getGridAttribute(layout, ATTR_COLUMN_COUNT, UNDEFINED);
        // Horizontal is the default, so if no value is specified it is horizontal.
        vertical = VALUE_VERTICAL.equals(getGridAttribute(layout, ATTR_ORIENTATION));

        mChildViews = new ArrayList<ViewData>(children.length);
        int index = 0;
        for (INode child : children) {
            ViewData view = new ViewData(child, index++);
            mChildViews.add(view);
        }

        // Assign row/column positions to all cells that do not explicitly define them
        if (!assignRowsAndColumnsFromViews(mChildViews)) {
            assignRowsAndColumnsFromXml(
                    declaredRowCount == UNDEFINED ? children.length : declaredRowCount,
                    declaredColumnCount == UNDEFINED ? children.length : declaredColumnCount);
        }

        assignCellBounds();

        for (int i = 0; i <= actualRowCount; i++) {
            mBaselines[i] = UNDEFINED;
        }
    }

    private Pair<Map<Integer, Integer>, Map<Integer, Integer>> findCellsOutsideDeclaredBounds() {
        // See if we have any (row,column) pairs that fall outside the declared
        // bounds; for these we identify the number of unique values and assign these
        // consecutive values
        Map<Integer, Integer> extraColumnsMap = null;
        Map<Integer, Integer> extraRowsMap = null;
        if (declaredRowCount != UNDEFINED) {
            Set<Integer> extraRows = null;
            for (ViewData view : mChildViews) {
                if (view.row >= declaredRowCount) {
                    if (extraRows == null) {
                        extraRows = new HashSet<Integer>();
                    }
                    extraRows.add(view.row);
                }
            }
            if (extraRows != null && declaredRowCount != UNDEFINED) {
                List<Integer> rows = new ArrayList<Integer>(extraRows);
                Collections.sort(rows);
                int row = declaredRowCount;
                extraRowsMap = new HashMap<Integer, Integer>();
                for (Integer declared : rows) {
                    extraRowsMap.put(declared, row++);
                }
            }
        }
        if (declaredColumnCount != UNDEFINED) {
            Set<Integer> extraColumns = null;
            for (ViewData view : mChildViews) {
                if (view.column >= declaredColumnCount) {
                    if (extraColumns == null) {
                        extraColumns = new HashSet<Integer>();
                    }
                    extraColumns.add(view.column);
                }
            }
            if (extraColumns != null && declaredColumnCount != UNDEFINED) {
                List<Integer> columns = new ArrayList<Integer>(extraColumns);
                Collections.sort(columns);
                int column = declaredColumnCount;
                extraColumnsMap = new HashMap<Integer, Integer>();
                for (Integer declared : columns) {
                    extraColumnsMap.put(declared, column++);
                }
            }
        }

        return Pair.of(extraRowsMap, extraColumnsMap);
    }

    /**
     * Figure out actual row and column numbers for views that do not specify explicit row
     * and/or column numbers
     * TODO: Consolidate with the algorithm in GridLayout to ensure we get the
     * exact same results!
     */
    private void assignRowsAndColumnsFromXml(int rowCount, int columnCount) {
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> p = findCellsOutsideDeclaredBounds();
        Map<Integer, Integer> extraRowsMap = p.getFirst();
        Map<Integer, Integer> extraColumnsMap = p.getSecond();

        if (!vertical) {
            // Horizontal GridLayout: this is the default. Row and column numbers
            // are assigned by assuming that the children are assigned successive
            // column numbers until we get to the column count of the grid, at which
            // point we jump to the next row. If any cell specifies either an explicit
            // row number of column number, we jump to the next available position.
            // Note also that if there are any rowspans on the current row, then the
            // next row we jump to is below the largest such rowspan - in other words,
            // the algorithm does not fill holes in the middle!

            // TODO: Ensure that we don't run into trouble if a later element specifies
            // an earlier number... find out what the layout does in that case!
            int row = 0;
            int column = 0;
            int nextRow = 1;
            for (ViewData view : mChildViews) {
                int declaredColumn = view.column;
                if (declaredColumn != UNDEFINED) {
                    if (declaredColumn >= columnCount) {
                        assert extraColumnsMap != null;
                        declaredColumn = extraColumnsMap.get(declaredColumn);
                        view.column = declaredColumn;
                    }
                    if (declaredColumn < column) {
                        // Must jump to the next row to accommodate the new row
                        assert nextRow > row;
                        //row++;
                        row = nextRow;
                    }
                    column = declaredColumn;
                } else {
                    view.column = column;
                }
                if (view.row != UNDEFINED) {
                    // TODO: Should this adjust the column number too? (If so must
                    // also update view.column since we've already processed the local
                    // column number)
                    row = view.row;
                } else {
                    view.row = row;
                }

                nextRow = Math.max(nextRow, view.row + view.rowSpan);

                // Advance
                column += view.columnSpan;
                if (column >= columnCount) {
                    column = 0;
                    assert nextRow > row;
                    //row++;
                    row = nextRow;
                }
            }
        } else {
            // Vertical layout: successive children are assigned to the same column in
            // successive rows.
            int row = 0;
            int column = 0;
            int nextColumn = 1;
            for (ViewData view : mChildViews) {
                int declaredRow = view.row;
                if (declaredRow != UNDEFINED) {
                    if (declaredRow >= rowCount) {
                        declaredRow = extraRowsMap.get(declaredRow);
                        view.row = declaredRow;
                    }
                    if (declaredRow < row) {
                        // Must jump to the next column to accommodate the new column
                        assert nextColumn > column;
                        column = nextColumn;
                    }
                    row = declaredRow;
                } else {
                    view.row = row;
                }
                if (view.column != UNDEFINED) {
                    // TODO: Should this adjust the row number too? (If so must
                    // also update view.row since we've already processed the local
                    // row number)
                    column = view.column;
                } else {
                    view.column = column;
                }

                nextColumn = Math.max(nextColumn, view.column + view.columnSpan);

                // Advance
                row += view.rowSpan;
                if (row >= rowCount) {
                    row = 0;
                    assert nextColumn > column;
                    //row++;
                    column = nextColumn;
                }
            }
        }
    }

    private static boolean sAttemptSpecReflection = true;

    private boolean assignRowsAndColumnsFromViews(List<ViewData> views) {
        if (!sAttemptSpecReflection) {
            return false;
        }

        try {
            // Lazily initialized reflection methods
            Field spanField = null;
            Field rowSpecField = null;
            Field colSpecField = null;
            Field minField = null;
            Field maxField = null;
            Method getLayoutParams = null;

            for (ViewData view : views) {
                // TODO: If the element *specifies* anything in XML, use that instead
                Object child = mRulesEngine.getViewObject(view.node);
                if (child == null) {
                    // Fallback to XML model
                    return false;
                }

                if (getLayoutParams == null) {
                    getLayoutParams = child.getClass().getMethod("getLayoutParams"); //$NON-NLS-1$
                }
                Object layoutParams = getLayoutParams.invoke(child);
                if (rowSpecField == null) {
                    Class<? extends Object> layoutParamsClass = layoutParams.getClass();
                    rowSpecField = layoutParamsClass.getDeclaredField("rowSpec");    //$NON-NLS-1$
                    colSpecField = layoutParamsClass.getDeclaredField("columnSpec"); //$NON-NLS-1$
                    rowSpecField.setAccessible(true);
                    colSpecField.setAccessible(true);
                }
                assert colSpecField != null;

                Object rowSpec = rowSpecField.get(layoutParams);
                Object colSpec = colSpecField.get(layoutParams);
                if (spanField == null) {
                    spanField = rowSpec.getClass().getDeclaredField("span"); //$NON-NLS-1$
                    spanField.setAccessible(true);
                }
                assert spanField != null;
                Object rowInterval = spanField.get(rowSpec);
                Object colInterval = spanField.get(colSpec);
                if (minField == null) {
                    Class<? extends Object> intervalClass = rowInterval.getClass();
                    minField = intervalClass.getDeclaredField("min"); //$NON-NLS-1$
                    maxField = intervalClass.getDeclaredField("max"); //$NON-NLS-1$
                    minField.setAccessible(true);
                    maxField.setAccessible(true);
                }
                assert maxField != null;

                int row = minField.getInt(rowInterval);
                int col = minField.getInt(colInterval);
                int rowEnd = maxField.getInt(rowInterval);
                int colEnd = maxField.getInt(colInterval);

                view.column = col;
                view.row = row;
                view.columnSpan = colEnd - col;
                view.rowSpan = rowEnd - row;
            }

            return true;

        } catch (Throwable e) {
            sAttemptSpecReflection = false;
            return false;
        }
    }

    /**
     * Computes the positions of the column and row boundaries
     */
    private void assignCellBounds() {
        if (!assignCellBoundsFromView()) {
            assignCellBoundsFromBounds();
        }
        initializeMaxBounds();
        mBaselines = new int[actualRowCount + 1];
    }

    /**
     * Computes the positions of the column and row boundaries, using actual
     * layout data from the associated GridLayout instance (stored in
     * {@link #mViewObject})
     */
    private boolean assignCellBoundsFromView() {
        if (mViewObject != null) {
            Pair<int[], int[]> cellBounds = GridModel.getAxisBounds(mViewObject);
            if (cellBounds != null) {
                int[] xs = cellBounds.getFirst();
                int[] ys = cellBounds.getSecond();
                Rect layoutBounds = layout.getBounds();

                // Handle "blank" grid layouts: insert a fake grid of CELL_COUNT^2 cells
                // where the user can do initial placement
                if (actualColumnCount <= 1 && actualRowCount <= 1 && mChildViews.isEmpty()) {
                    final int CELL_COUNT = 1;
                    xs = new int[CELL_COUNT + 1];
                    ys = new int[CELL_COUNT + 1];
                    int cellWidth = layoutBounds.w / CELL_COUNT;
                    int cellHeight = layoutBounds.h / CELL_COUNT;

                    for (int i = 0; i <= CELL_COUNT; i++) {
                        xs[i] = i * cellWidth;
                        ys[i] = i * cellHeight;
                    }
                }

                actualColumnCount = xs.length - 1;
                actualRowCount = ys.length - 1;

                int layoutBoundsX = layoutBounds.x;
                int layoutBoundsY = layoutBounds.y;
                mLeft = new int[xs.length];
                mTop = new int[ys.length];
                for (int i = 0; i < xs.length; i++) {
                    mLeft[i] = xs[i] + layoutBoundsX;
                }
                for (int i = 0; i < ys.length; i++) {
                    mTop[i] = ys[i] + layoutBoundsY;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Computes the boundaries of the rows and columns by considering the bounds of the
     * children.
     */
    private void assignCellBoundsFromBounds() {
        Rect layoutBounds = layout.getBounds();

        // Compute the actualColumnCount and actualRowCount. This -should- be
        // as easy as declaredColumnCount + extraColumnsMap.size(),
        // but the user doesn't *have* to declare a column count (or a row count)
        // and we need both, so go and find the actual row and column maximums.
        int maxColumn = 0;
        int maxRow = 0;
        for (ViewData view : mChildViews) {
            maxColumn = max(maxColumn, view.column);
            maxRow = max(maxRow, view.row);
        }
        actualColumnCount = maxColumn + 1;
        actualRowCount = maxRow + 1;

        mLeft = new int[actualColumnCount + 1];
        for (int i = 1; i < actualColumnCount; i++) {
            mLeft[i] = UNDEFINED;
        }
        mLeft[0] = layoutBounds.x;
        mLeft[actualColumnCount] = layoutBounds.x2();
        mTop = new int[actualRowCount + 1];
        for (int i = 1; i < actualRowCount; i++) {
            mTop[i] = UNDEFINED;
        }
        mTop[0] = layoutBounds.y;
        mTop[actualRowCount] = layoutBounds.y2();

        for (ViewData view : mChildViews) {
            Rect bounds = view.node.getBounds();
            if (!bounds.isValid()) {
                continue;
            }
            int column = view.column;
            int row = view.row;

            if (mLeft[column] == UNDEFINED) {
                mLeft[column] = bounds.x;
            } else {
                mLeft[column] = Math.min(bounds.x, mLeft[column]);
            }
            if (mTop[row] == UNDEFINED) {
                mTop[row] = bounds.y;
            } else {
                mTop[row] = Math.min(bounds.y, mTop[row]);
            }
        }

        // Ensure that any empty columns/rows have a valid boundary value; for now,
        for (int i = actualColumnCount - 1; i >= 0; i--) {
            if (mLeft[i] == UNDEFINED) {
                if (i == 0) {
                    mLeft[i] = layoutBounds.x;
                } else if (i < actualColumnCount - 1) {
                    mLeft[i] = mLeft[i + 1] - 1;
                    if (mLeft[i - 1] != UNDEFINED && mLeft[i] < mLeft[i - 1]) {
                        mLeft[i] = mLeft[i - 1];
                    }
                } else {
                    mLeft[i] = layoutBounds.x2();
                }
            }
        }
        for (int i = actualRowCount - 1; i >= 0; i--) {
            if (mTop[i] == UNDEFINED) {
                if (i == 0) {
                    mTop[i] = layoutBounds.y;
                } else if (i < actualRowCount - 1) {
                    mTop[i] = mTop[i + 1] - 1;
                    if (mTop[i - 1] != UNDEFINED && mTop[i] < mTop[i - 1]) {
                        mTop[i] = mTop[i - 1];
                    }
                } else {
                    mTop[i] = layoutBounds.y2();
                }
            }
        }

        // The bounds should be in ascending order now
        if (false && GridLayoutRule.sDebugGridLayout) {
            for (int i = 1; i < actualRowCount; i++) {
                assert mTop[i + 1] >= mTop[i];
            }
            for (int i = 0; i < actualColumnCount; i++) {
                assert mLeft[i + 1] >= mLeft[i];
            }
        }
    }

    /**
     * Determine, for each row and column, what the largest x and y edges are
     * within that row or column. This is used to find a natural split point to
     * suggest when adding something "to the right of" or "below" another view.
     */
    private void initializeMaxBounds() {
        mMaxRight = new int[actualColumnCount + 1];
        mMaxBottom = new int[actualRowCount + 1];

        for (ViewData view : mChildViews) {
            Rect bounds = view.node.getBounds();
            if (!bounds.isValid()) {
                continue;
            }

            if (!view.isSpacer()) {
                int x2 = bounds.x2();
                int y2 = bounds.y2();
                int column = view.column;
                int row = view.row;
                int targetColumn = min(actualColumnCount - 1,
                        column + view.columnSpan - 1);
                int targetRow = min(actualRowCount - 1, row + view.rowSpan - 1);
                IViewMetadata metadata = mRulesEngine.getMetadata(view.node.getFqcn());
                if (metadata != null) {
                    Margins insets = metadata.getInsets();
                    if (insets != null) {
                        x2 -= insets.right;
                        y2 -= insets.bottom;
                    }
                }
                if (mMaxRight[targetColumn] < x2
                        && ((view.gravity & (GRAVITY_CENTER_HORIZ | GRAVITY_RIGHT)) == 0)) {
                    mMaxRight[targetColumn] = x2;
                }
                if (mMaxBottom[targetRow] < y2
                        && ((view.gravity & (GRAVITY_CENTER_VERT | GRAVITY_BOTTOM)) == 0)) {
                    mMaxBottom[targetRow] = y2;
                }
            }
        }
    }

    /**
     * Looks up the x[] and y[] locations of the columns and rows in the given GridLayout
     * instance.
     *
     * @param view the GridLayout object, which should already have performed layout
     * @return a pair of x[] and y[] integer arrays, or null if it could not be found
     */
    public static Pair<int[], int[]> getAxisBounds(Object view) {
        try {
            Class<?> clz = view.getClass();
            String verticalAxisName = "verticalAxis";
            Field horizontalAxis;
            try {
                horizontalAxis = clz.getDeclaredField("horizontalAxis"); //$NON-NLS-1$
            } catch (NoSuchFieldException e) {
                // Field names changed in KitKat
                horizontalAxis = clz.getDeclaredField("mHorizontalAxis"); //$NON-NLS-1$
                verticalAxisName = "mVerticalAxis";
            }
            Field verticalAxis = clz.getDeclaredField(verticalAxisName);
            horizontalAxis.setAccessible(true);
            verticalAxis.setAccessible(true);
            Object horizontal = horizontalAxis.get(view);
            Object vertical = verticalAxis.get(view);
            Field locations = horizontal.getClass().getDeclaredField("locations"); //$NON-NLS-1$
            assert locations.getType().isArray() : locations.getType();
            locations.setAccessible(true);
            Object horizontalLocations = locations.get(horizontal);
            Object verticalLocations = locations.get(vertical);
            int[] xs = (int[]) horizontalLocations;
            int[] ys = (int[]) verticalLocations;
            return Pair.of(xs, ys);
        } catch (Throwable t) {
            // Probably trying to show a GridLayout on a platform that does not support it.
            // Return null to indicate that the grid bounds must be computed from view bounds.
            return null;
        }
    }

    /**
     * Add a new column.
     *
     * @param selectedChildren if null or empty, add the column at the end of the grid,
     *            and otherwise add it before the column of the first selected child
     * @return the newly added column spacer
     */
    public INode addColumn(List<? extends INode> selectedChildren) {
        // Determine insert index
        int newColumn = actualColumnCount;
        if (selectedChildren != null && selectedChildren.size() > 0) {
            INode first = selectedChildren.get(0);
            ViewData view = getView(first);
            newColumn = view.column;
        }

        INode newView = addColumn(newColumn, null, UNDEFINED, false, UNDEFINED, UNDEFINED);
        if (newView != null) {
            mRulesEngine.select(Collections.singletonList(newView));
        }

        return newView;
    }

    /**
     * Adds a new column.
     *
     * @param newColumn the column index to insert before
     * @param newView the {@link INode} to insert as the column spacer, which may be null
     *            (in which case a spacer is automatically created)
     * @param columnWidthDp the width, in device independent pixels, of the column to be
     *            added (which may be {@link #UNDEFINED}
     * @param split if true, split the existing column into two at the given x position
     * @param row the row to add the newView to
     * @param x the x position of the column we're inserting
     * @return the column spacer
     */
    public INode addColumn(int newColumn, INode newView, int columnWidthDp,
            boolean split, int row, int x) {
        // Insert a new column
        actualColumnCount++;
        if (declaredColumnCount != UNDEFINED) {
            declaredColumnCount++;
            setGridAttribute(layout, ATTR_COLUMN_COUNT, declaredColumnCount);
        }

        boolean isLastColumn = true;
        for (ViewData view : mChildViews) {
            if (view.column >= newColumn) {
                isLastColumn = false;
                break;
            }
        }

        for (ViewData view : mChildViews) {
            boolean columnSpanSet = false;

            int endColumn = view.column + view.columnSpan;
            if (view.column >= newColumn || endColumn == newColumn) {
                if (view.column == newColumn || endColumn == newColumn) {
                    //if (view.row == 0) {
                    if (newView == null && !isLastColumn) {
                        // Insert a new spacer
                        int index = getChildIndex(layout.getChildren(), view.node);
                        assert view.index == index; // TODO: Get rid of getter
                        if (endColumn == newColumn) {
                            // This cell -ends- at the desired position: insert it after
                            index++;
                        }

                        ViewData newViewData = addSpacer(layout, index,
                                split ? row : UNDEFINED,
                                split ? newColumn - 1 : UNDEFINED,
                                columnWidthDp != UNDEFINED ? columnWidthDp : DEFAULT_CELL_WIDTH,
                                DEFAULT_CELL_HEIGHT);
                        newViewData.column = newColumn - 1;
                        newViewData.row = row;
                        newView = newViewData.node;
                    }

                    // Set the actual row number on the first cell on the new row.
                    // This means we don't really need the spacer above to imply
                    // the new row number, but we use the spacer to assign the row
                    // some height.
                    if (view.column == newColumn) {
                        view.column++;
                        setGridAttribute(view.node, ATTR_LAYOUT_COLUMN, view.column);
                    } // else: endColumn == newColumn: handled below
                } else if (getGridAttribute(view.node, ATTR_LAYOUT_COLUMN) != null) {
                    view.column++;
                    setGridAttribute(view.node, ATTR_LAYOUT_COLUMN, view.column);
                }
            } else if (endColumn > newColumn) {
                view.columnSpan++;
                setColumnSpanAttribute(view.node, view.columnSpan);
                columnSpanSet = true;
            }

            if (split && !columnSpanSet && view.node.getBounds().x2() > x) {
                if (view.node.getBounds().x < x) {
                    view.columnSpan++;
                    setColumnSpanAttribute(view.node, view.columnSpan);
                }
            }
        }

        // Hardcode the row numbers if the last column is a new column such that
        // they don't jump back to backfill the previous row's new last cell
        if (isLastColumn) {
            for (ViewData view : mChildViews) {
                if (view.column == 0 && view.row > 0) {
                    setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);
                }
            }
            if (split) {
                assert newView == null;
                addSpacer(layout, -1, row, newColumn -1,
                        columnWidthDp != UNDEFINED ? columnWidthDp : DEFAULT_CELL_WIDTH,
                                SPACER_SIZE_DP);
            }
        }

        return newView;
    }

    /**
     * Removes the columns containing the given selection
     *
     * @param selectedChildren a list of nodes whose columns should be deleted
     */
    public void removeColumns(List<? extends INode> selectedChildren) {
        if (selectedChildren.size() == 0) {
            return;
        }

        // Figure out which columns should be removed
        Set<Integer> removeColumns = new HashSet<Integer>();
        Set<ViewData> removedViews = new HashSet<ViewData>();
        for (INode child : selectedChildren) {
            ViewData view = getView(child);
            removedViews.add(view);
            removeColumns.add(view.column);
        }
        // Sort them in descending order such that we can process each
        // deletion independently
        List<Integer> removed = new ArrayList<Integer>(removeColumns);
        Collections.sort(removed, Collections.reverseOrder());

        for (int removedColumn : removed) {
            // Remove column.
            // First, adjust column count.
            // TODO: Don't do this if the column being deleted is outside
            // the declared column range!
            // TODO: Do this under a write lock? / editXml lock?
            actualColumnCount--;
            if (declaredColumnCount != UNDEFINED) {
                declaredColumnCount--;
            }

            // Remove any elements that begin in the deleted columns...
            // If they have colspan > 1, then we must insert a spacer instead.
            // For any other elements that overlap, we need to subtract from the span.

            for (ViewData view : mChildViews) {
                if (view.column == removedColumn) {
                    int index = getChildIndex(layout.getChildren(), view.node);
                    assert view.index == index; // TODO: Get rid of getter
                    if (view.columnSpan > 1) {
                        // Make a new spacer which is the width of the following
                        // columns
                        int columnWidth = getColumnWidth(removedColumn, view.columnSpan) -
                                getColumnWidth(removedColumn, 1);
                        int columnWidthDip = mRulesEngine.pxToDp(columnWidth);
                        ViewData spacer = addSpacer(layout, index, UNDEFINED, UNDEFINED,
                                columnWidthDip, SPACER_SIZE_DP);
                        spacer.row = 0;
                        spacer.column = removedColumn;
                    }
                    layout.removeChild(view.node);
                } else if (view.column < removedColumn
                        && view.column + view.columnSpan > removedColumn) {
                    // Subtract column span to skip this item
                    view.columnSpan--;
                    setColumnSpanAttribute(view.node, view.columnSpan);
                } else if (view.column > removedColumn) {
                    view.column--;
                    if (getGridAttribute(view.node, ATTR_LAYOUT_COLUMN) != null) {
                        setGridAttribute(view.node, ATTR_LAYOUT_COLUMN, view.column);
                    }
                }
            }
        }

        // Remove children from child list!
        if (removedViews.size() <= 2) {
            mChildViews.removeAll(removedViews);
        } else {
            List<ViewData> remaining =
                    new ArrayList<ViewData>(mChildViews.size() - removedViews.size());
            for (ViewData view : mChildViews) {
                if (!removedViews.contains(view)) {
                    remaining.add(view);
                }
            }
            mChildViews = remaining;
        }

        //if (declaredColumnCount != UNDEFINED) {
            setGridAttribute(layout, ATTR_COLUMN_COUNT, actualColumnCount);
        //}

    }

    /**
     * Add a new row.
     *
     * @param selectedChildren if null or empty, add the row at the bottom of the grid,
     *            and otherwise add it before the row of the first selected child
     * @return the newly added row spacer
     */
    public INode addRow(List<? extends INode> selectedChildren) {
        // Determine insert index
        int newRow = actualRowCount;
        if (selectedChildren.size() > 0) {
            INode first = selectedChildren.get(0);
            ViewData view = getView(first);
            newRow = view.row;
        }

        INode newView = addRow(newRow, null, UNDEFINED, false, UNDEFINED, UNDEFINED);
        if (newView != null) {
            mRulesEngine.select(Collections.singletonList(newView));
        }

        return newView;
    }

    /**
     * Adds a new column.
     *
     * @param newRow the row index to insert before
     * @param newView the {@link INode} to insert as the row spacer, which may be null (in
     *            which case a spacer is automatically created)
     * @param rowHeightDp the height, in device independent pixels, of the row to be added
     *            (which may be {@link #UNDEFINED}
     * @param split if true, split the existing row into two at the given y position
     * @param column the column to add the newView to
     * @param y the y position of the row we're inserting
     * @return the row spacer
     */
    public INode addRow(int newRow, INode newView, int rowHeightDp, boolean split,
            int column, int y) {
        actualRowCount++;
        if (declaredRowCount != UNDEFINED) {
            declaredRowCount++;
            setGridAttribute(layout, ATTR_ROW_COUNT, declaredRowCount);
        }

        boolean added = false;
        for (ViewData view : mChildViews) {
            if (view.row >= newRow) {
                // Adjust the column count
                if (view.row == newRow && view.column == 0) {
                    // Insert a new spacer
                    if (newView == null) {
                        int index = getChildIndex(layout.getChildren(), view.node);
                        assert view.index == index; // TODO: Get rid of getter
                        if (declaredColumnCount != UNDEFINED && !split) {
                            setGridAttribute(layout, ATTR_COLUMN_COUNT, declaredColumnCount);
                        }
                        ViewData newViewData = addSpacer(layout, index,
                                    split ? newRow - 1 : UNDEFINED,
                                    split ? column : UNDEFINED,
                                    SPACER_SIZE_DP,
                                    rowHeightDp != UNDEFINED ? rowHeightDp : DEFAULT_CELL_HEIGHT);
                        newViewData.column = column;
                        newViewData.row = newRow - 1;
                        newView = newViewData.node;
                    }

                    // Set the actual row number on the first cell on the new row.
                    // This means we don't really need the spacer above to imply
                    // the new row number, but we use the spacer to assign the row
                    // some height.
                    view.row++;
                    setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);

                    added = true;
                } else if (getGridAttribute(view.node, ATTR_LAYOUT_ROW) != null) {
                    view.row++;
                    setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);
                }
            } else {
                int endRow = view.row + view.rowSpan;
                if (endRow > newRow) {
                    view.rowSpan++;
                    setRowSpanAttribute(view.node, view.rowSpan);
                } else if (split && view.node.getBounds().y2() > y) {
                    if (view.node.getBounds().y < y) {
                        view.rowSpan++;
                        setRowSpanAttribute(view.node, view.rowSpan);
                    }
                }
            }
        }

        if (!added) {
            // Append a row at the end
            if (newView == null) {
                ViewData newViewData = addSpacer(layout, -1, UNDEFINED, UNDEFINED,
                        SPACER_SIZE_DP,
                        rowHeightDp != UNDEFINED ? rowHeightDp : DEFAULT_CELL_HEIGHT);
                newViewData.column = column;
                // TODO: MAke sure this row number is right!
                newViewData.row = split ? newRow - 1 : newRow;
                newView = newViewData.node;
            }
            if (declaredColumnCount != UNDEFINED && !split) {
                setGridAttribute(layout, ATTR_COLUMN_COUNT, declaredColumnCount);
            }
            if (split) {
                setGridAttribute(newView, ATTR_LAYOUT_ROW, newRow - 1);
                setGridAttribute(newView, ATTR_LAYOUT_COLUMN, column);
            }
        }

        return newView;
    }

    /**
     * Removes the rows containing the given selection
     *
     * @param selectedChildren a list of nodes whose rows should be deleted
     */
    public void removeRows(List<? extends INode> selectedChildren) {
        if (selectedChildren.size() == 0) {
            return;
        }

        // Figure out which rows should be removed
        Set<ViewData> removedViews = new HashSet<ViewData>();
        Set<Integer> removedRows = new HashSet<Integer>();
        for (INode child : selectedChildren) {
            ViewData view = getView(child);
            removedViews.add(view);
            removedRows.add(view.row);
        }
        // Sort them in descending order such that we can process each
        // deletion independently
        List<Integer> removed = new ArrayList<Integer>(removedRows);
        Collections.sort(removed, Collections.reverseOrder());

        for (int removedRow : removed) {
            // Remove row.
            // First, adjust row count.
            // TODO: Don't do this if the row being deleted is outside
            // the declared row range!
            actualRowCount--;
            if (declaredRowCount != UNDEFINED) {
                declaredRowCount--;
                setGridAttribute(layout, ATTR_ROW_COUNT, declaredRowCount);
            }

            // Remove any elements that begin in the deleted rows...
            // If they have colspan > 1, then we must hardcode a new row number
            // instead.
            // For any other elements that overlap, we need to subtract from the span.

            for (ViewData view : mChildViews) {
                if (view.row == removedRow) {
                    // We don't have to worry about a rowSpan > 1 here, because even
                    // if it is, those rowspans are not used to assign default row/column
                    // positions for other cells
// TODO: Check this; it differs from the removeColumns logic!
                    layout.removeChild(view.node);
                } else if (view.row > removedRow) {
                    view.row--;
                    if (getGridAttribute(view.node, ATTR_LAYOUT_ROW) != null) {
                        setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);
                    }
                } else if (view.row < removedRow
                        && view.row + view.rowSpan > removedRow) {
                    // Subtract row span to skip this item
                    view.rowSpan--;
                    setRowSpanAttribute(view.node, view.rowSpan);
                }
            }
        }

        // Remove children from child list!
        if (removedViews.size() <= 2) {
            mChildViews.removeAll(removedViews);
        } else {
            List<ViewData> remaining =
                    new ArrayList<ViewData>(mChildViews.size() - removedViews.size());
            for (ViewData view : mChildViews) {
                if (!removedViews.contains(view)) {
                    remaining.add(view);
                }
            }
            mChildViews = remaining;
        }
    }

    /**
     * Returns the row containing the given y line
     *
     * @param y the vertical position
     * @return the row containing the given line
     */
    public int getRow(int y) {
        int row = Arrays.binarySearch(mTop, y);
        if (row == -1) {
            // Smaller than the first element; just use the first row
            return 0;
        } else if (row < 0) {
            row = -(row + 2);
        }

        return row;
    }

    /**
     * Returns the column containing the given x line
     *
     * @param x the horizontal position
     * @return the column containing the given line
     */
    public int getColumn(int x) {
        int column = Arrays.binarySearch(mLeft, x);
        if (column == -1) {
            // Smaller than the first element; just use the first column
            return 0;
        } else if (column < 0) {
            column = -(column + 2);
        }

        return column;
    }

    /**
     * Returns the closest row to the given y line. This is
     * either the row containing the line, or the row below it.
     *
     * @param y the vertical position
     * @return the closest row
     */
    public int getClosestRow(int y) {
        int row = Arrays.binarySearch(mTop, y);
        if (row == -1) {
            // Smaller than the first element; just use the first column
            return 0;
        } else if (row < 0) {
            row = -(row + 2);
        }

        if (getRowDistance(row, y) < getRowDistance(row + 1, y)) {
            return row;
        } else {
            return row + 1;
        }
    }

    /**
     * Returns the closest column to the given x line. This is
     * either the column containing the line, or the column following it.
     *
     * @param x the horizontal position
     * @return the closest column
     */
    public int getClosestColumn(int x) {
        int column = Arrays.binarySearch(mLeft, x);
        if (column == -1) {
            // Smaller than the first element; just use the first column
            return 0;
        } else if (column < 0) {
            column = -(column + 2);
        }

        if (getColumnDistance(column, x) < getColumnDistance(column + 1, x)) {
            return column;
        } else {
            return column + 1;
        }
    }

    /**
     * Returns the distance between the given x position and the beginning of the given column
     *
     * @param column the column
     * @param x the x position
     * @return the distance between the two
     */
    public int getColumnDistance(int column, int x) {
        return abs(getColumnX(column) - x);
    }

    /**
     * Returns the actual width of the given column. This returns the difference between
     * the rightmost edge of the views (not including spacers) and the left edge of the
     * column.
     *
     * @param column the column
     * @return the actual width of the non-spacer views in the column
     */
    public int getColumnActualWidth(int column) {
        return getColumnMaxX(column) - getColumnX(column);
    }

    /**
     * Returns the distance between the given y position and the top of the given row
     *
     * @param row the row
     * @param y the y position
     * @return the distance between the two
     */
    public int getRowDistance(int row, int y) {
        return abs(getRowY(row) - y);
    }

    /**
     * Returns the y position of the top of the given row
     *
     * @param row the target row
     * @return the y position of its top edge
     */
    public int getRowY(int row) {
        return mTop[min(mTop.length - 1, max(0, row))];
    }

    /**
     * Returns the bottom-most edge of any of the non-spacer children in the given row
     *
     * @param row the target row
     * @return the bottom-most edge of any of the non-spacer children in the row
     */
    public int getRowMaxY(int row) {
        return mMaxBottom[min(mMaxBottom.length - 1, max(0, row))];
    }

    /**
     * Returns the actual height of the given row. This returns the difference between
     * the bottom-most edge of the views (not including spacers) and the top edge of the
     * row.
     *
     * @param row the row
     * @return the actual height of the non-spacer views in the row
     */
    public int getRowActualHeight(int row) {
        return getRowMaxY(row) - getRowY(row);
    }

    /**
     * Returns a list of all the nodes that intersects the rows in the range
     * {@code y1 <= y <= y2}.
     *
     * @param y1 the starting y, inclusive
     * @param y2 the ending y, inclusive
     * @return a list of nodes intersecting the given rows, never null but possibly empty
     */
    public Collection<INode> getIntersectsRow(int y1, int y2) {
        List<INode> nodes = new ArrayList<INode>();

        for (ViewData view : mChildViews) {
            if (!view.isSpacer()) {
                Rect bounds = view.node.getBounds();
                if (bounds.y2() >= y1 && bounds.y <= y2) {
                    nodes.add(view.node);
                }
            }
        }

        return nodes;
    }

    /**
     * Returns the height of the given row or rows (if the rowSpan is greater than 1)
     *
     * @param row the target row
     * @param rowSpan the row span
     * @return the height in pixels of the given rows
     */
    public int getRowHeight(int row, int rowSpan) {
        return getRowY(row + rowSpan) - getRowY(row);
    }

    /**
     * Returns the x position of the left edge of the given column
     *
     * @param column the target column
     * @return the x position of its left edge
     */
    public int getColumnX(int column) {
        return mLeft[min(mLeft.length - 1, max(0, column))];
    }

    /**
     * Returns the rightmost edge of any of the non-spacer children in the given row
     *
     * @param column the target column
     * @return the rightmost edge of any of the non-spacer children in the column
     */
    public int getColumnMaxX(int column) {
        return mMaxRight[min(mMaxRight.length - 1, max(0, column))];
    }

    /**
     * Returns the width of the given column or columns (if the columnSpan is greater than 1)
     *
     * @param column the target column
     * @param columnSpan the column span
     * @return the width in pixels of the given columns
     */
    public int getColumnWidth(int column, int columnSpan) {
        return getColumnX(column + columnSpan) - getColumnX(column);
    }

    /**
     * Returns the bounds of the cell at the given row and column position, with the given
     * row and column spans.
     *
     * @param row the target row
     * @param column the target column
     * @param rowSpan the row span
     * @param columnSpan the column span
     * @return the bounds, in pixels, of the given cell
     */
    public Rect getCellBounds(int row, int column, int rowSpan, int columnSpan) {
        return new Rect(getColumnX(column), getRowY(row),
                getColumnWidth(column, columnSpan),
                getRowHeight(row, rowSpan));
    }

    /**
     * Produces a display of view contents along with the pixel positions of each
     * row/column, like the following (used for diagnostics only)
     *
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
        int cellWidth = 25;

        List<List<List<ViewData>>> rowList = new ArrayList<List<List<ViewData>>>(mTop.length);
        for (int row = 0; row < mTop.length; row++) {
            List<List<ViewData>> columnList = new ArrayList<List<ViewData>>(mLeft.length);
            for (int col = 0; col < mLeft.length; col++) {
                columnList.add(new ArrayList<ViewData>(4));
            }
            rowList.add(columnList);
        }
        for (ViewData view : mChildViews) {
            for (int i = 0; i < view.rowSpan; i++) {
                if (view.row + i > mTop.length) { // Guard against bogus span values
                    break;
                }
                if (rowList.size() <= view.row + i) {
                    break;
                }
                for (int j = 0; j < view.columnSpan; j++) {
                    List<List<ViewData>> columnList = rowList.get(view.row + i);
                    if (columnList.size() <= view.column + j) {
                        break;
                    }
                    columnList.get(view.column + j).add(view);
                }
            }
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        out.printf("%" + cellWidth + "s", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (int col = 0; col < actualColumnCount + 1; col++) {
            out.printf("|%-" + (cellWidth - 1) + "d", mLeft[col]); //$NON-NLS-1$ //$NON-NLS-2$
        }
        out.printf("\n"); //$NON-NLS-1$
        for (int row = 0; row < actualRowCount + 1; row++) {
            out.printf("%" + cellWidth + "d", mTop[row]); //$NON-NLS-1$ //$NON-NLS-2$
            if (row == actualRowCount) {
                break;
            }
            for (int col = 0; col < actualColumnCount; col++) {
                List<ViewData> views = rowList.get(row).get(col);

                StringBuilder sb = new StringBuilder();
                for (ViewData view : views) {
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

    /**
     * Split a cell into two or three columns.
     *
     * @param newColumn The column number to insert before
     * @param insertMarginColumn If false, then the cell at newColumn -1 is split with the
     *            left part taking up exactly columnWidthDp dips. If true, then the column
     *            is split twice; the left part is the implicit width of the column, the
     *            new middle (margin) column is exactly the columnWidthDp size and the
     *            right column is the remaining space of the old cell.
     * @param columnWidthDp The width of the column inserted before the new column (or if
     *            insertMarginColumn is false, then the width of the margin column)
     * @param x the x coordinate of the new column
     */
    public void splitColumn(int newColumn, boolean insertMarginColumn, int columnWidthDp, int x) {
        actualColumnCount++;

        // Insert a new column
        if (declaredColumnCount != UNDEFINED) {
            declaredColumnCount++;
            if (insertMarginColumn) {
                declaredColumnCount++;
            }
            setGridAttribute(layout, ATTR_COLUMN_COUNT, declaredColumnCount);
        }

        // Are we inserting a new last column in the grid? That requires some special handling...
        boolean isLastColumn = true;
        for (ViewData view : mChildViews) {
            if (view.column >= newColumn) {
                isLastColumn = false;
                break;
            }
        }

        // Hardcode the row numbers if the last column is a new column such that
        // they don't jump back to backfill the previous row's new last cell:
        // TODO: Only do this for horizontal layouts!
        if (isLastColumn) {
            for (ViewData view : mChildViews) {
                if (view.column == 0 && view.row > 0) {
                    if (getGridAttribute(view.node, ATTR_LAYOUT_ROW) == null) {
                        setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);
                    }
                }
            }
        }

        // Find the spacer which marks this column, and if found, mark it as a split
        ViewData prevColumnSpacer = null;
        for (ViewData view : mChildViews) {
            if (view.column == newColumn - 1 && view.isColumnSpacer()) {
                prevColumnSpacer = view;
                break;
            }
        }

        // Process all existing grid elements:
        //  * Increase column numbers for all columns that have a hardcoded column number
        //     greater than the new column
        //  * Set an explicit column=0 where needed (TODO: Implement this)
        //  * Increase the columnSpan for all columns that overlap the newly inserted column edge
        //  * Split the spacer which defined the size of this column into two
        //    (and if not found, create a new spacer)
        //
        for (ViewData view : mChildViews) {
            if (view == prevColumnSpacer) {
                continue;
            }

            INode node = view.node;
            int column = view.column;
            if (column > newColumn || (column == newColumn && view.node.getBounds().x2() > x)) {
                // ALWAYS set the column, because
                //    (1) if it has been set, it needs to be corrected
                //    (2) if it has not been set, it needs to be set to cause this column
                //        to skip over the new column (there may be no views for the new
                //        column on this row).
                //   TODO: Enhance this such that we only set the column to a skip number
                //   where necessary, e.g. only on the FIRST view on this row following the
                //   skipped column!

                //if (getGridAttribute(node, ATTR_LAYOUT_COLUMN) != null) {
                view.column += insertMarginColumn ? 2 : 1;
                setGridAttribute(node, ATTR_LAYOUT_COLUMN, view.column);
                //}
            } else if (!view.isSpacer()) {
                // Adjust the column span? We must increase it if
                //  (1) the new column is inside the range [column, column + columnSpan]
                //  (2) the new column is within the last cell in the column span,
                //      and the exact X location of the split is within the horizontal
                //      *bounds* of this node (provided it has gravity=left)
                //  (3) the new column is within the last cell and the cell has gravity
                //      right or gravity center
                int endColumn = column + view.columnSpan;
                if (endColumn > newColumn
                        || endColumn == newColumn && (view.node.getBounds().x2() > x
                                || GravityHelper.isConstrainedHorizontally(view.gravity)
                                    &&  !GravityHelper.isLeftAligned(view.gravity))) {
                    // This cell spans the new insert position, so increment the column span
                    view.columnSpan += insertMarginColumn ? 2 : 1;
                    setColumnSpanAttribute(node, view.columnSpan);
                }
            }
        }

        // Insert new spacer:
        if (prevColumnSpacer != null) {
            int px = getColumnWidth(newColumn - 1, 1);
            if (insertMarginColumn || columnWidthDp == 0) {
                px -= getColumnActualWidth(newColumn - 1);
            }
            int dp = mRulesEngine.pxToDp(px);
            int remaining = dp - columnWidthDp;
            if (remaining > 0) {
                prevColumnSpacer.node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH,
                        String.format(VALUE_N_DP, remaining));
                prevColumnSpacer.column = insertMarginColumn ? newColumn + 1 : newColumn;
                setGridAttribute(prevColumnSpacer.node, ATTR_LAYOUT_COLUMN,
                        prevColumnSpacer.column);
            }
        }

        if (columnWidthDp > 0) {
            int index = prevColumnSpacer != null ? prevColumnSpacer.index : -1;

            addSpacer(layout, index, 0, insertMarginColumn ? newColumn : newColumn - 1,
                columnWidthDp, SPACER_SIZE_DP);
        }
    }

    /**
     * Split a cell into two or three rows.
     *
     * @param newRow The row number to insert before
     * @param insertMarginRow If false, then the cell at newRow -1 is split with the above
     *            part taking up exactly rowHeightDp dips. If true, then the row is split
     *            twice; the top part is the implicit height of the row, the new middle
     *            (margin) row is exactly the rowHeightDp size and the bottom column is
     *            the remaining space of the old cell.
     * @param rowHeightDp The height of the row inserted before the new row (or if
     *            insertMarginRow is false, then the height of the margin row)
     * @param y the y coordinate of the new row
     */
    public void splitRow(int newRow, boolean insertMarginRow, int rowHeightDp, int y) {
        actualRowCount++;

        // Insert a new row
        if (declaredRowCount != UNDEFINED) {
            declaredRowCount++;
            if (insertMarginRow) {
                declaredRowCount++;
            }
            setGridAttribute(layout, ATTR_ROW_COUNT, declaredRowCount);
        }

        // Find the spacer which marks this row, and if found, mark it as a split
        ViewData prevRowSpacer = null;
        for (ViewData view : mChildViews) {
            if (view.row == newRow - 1 && view.isRowSpacer()) {
                prevRowSpacer = view;
                break;
            }
        }

        // Se splitColumn() for details
        for (ViewData view : mChildViews) {
            if (view == prevRowSpacer) {
                continue;
            }

            INode node = view.node;
            int row = view.row;
            if (row > newRow || (row == newRow && view.node.getBounds().y2() > y)) {
                //if (getGridAttribute(node, ATTR_LAYOUT_ROW) != null) {
                view.row += insertMarginRow ? 2 : 1;
                setGridAttribute(node, ATTR_LAYOUT_ROW, view.row);
                //}
            } else if (!view.isSpacer()) {
                int endRow = row + view.rowSpan;
                if (endRow > newRow
                        || endRow == newRow && (view.node.getBounds().y2() > y
                                || GravityHelper.isConstrainedVertically(view.gravity)
                                    && !GravityHelper.isTopAligned(view.gravity))) {
                    // This cell spans the new insert position, so increment the row span
                    view.rowSpan += insertMarginRow ? 2 : 1;
                    setRowSpanAttribute(node, view.rowSpan);
                }
            }
        }

        // Insert new spacer:
        if (prevRowSpacer != null) {
            int px = getRowHeight(newRow - 1, 1);
            if (insertMarginRow || rowHeightDp == 0) {
                px -= getRowActualHeight(newRow - 1);
            }
            int dp = mRulesEngine.pxToDp(px);
            int remaining = dp - rowHeightDp;
            if (remaining > 0) {
                prevRowSpacer.node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT,
                        String.format(VALUE_N_DP, remaining));
                prevRowSpacer.row = insertMarginRow ? newRow + 1 : newRow;
                setGridAttribute(prevRowSpacer.node, ATTR_LAYOUT_ROW, prevRowSpacer.row);
            }
        }

        if (rowHeightDp > 0) {
            int index = prevRowSpacer != null ? prevRowSpacer.index : -1;
            addSpacer(layout, index, insertMarginRow ? newRow : newRow - 1,
                    0, SPACER_SIZE_DP, rowHeightDp);
        }
    }

    /**
     * Data about a view in a table; this is not the same as a cell because multiple views
     * can share a single cell, and a view can span many cells.
     */
    public class ViewData {
        public final INode node;
        public final int index;
        public int row;
        public int column;
        public int rowSpan;
        public int columnSpan;
        public int gravity;

        ViewData(INode n, int index) {
            node = n;
            this.index = index;

            column = getGridAttribute(n, ATTR_LAYOUT_COLUMN, UNDEFINED);
            columnSpan = getGridAttribute(n, ATTR_LAYOUT_COLUMN_SPAN, 1);
            row = getGridAttribute(n, ATTR_LAYOUT_ROW, UNDEFINED);
            rowSpan = getGridAttribute(n, ATTR_LAYOUT_ROW_SPAN, 1);
            gravity = GravityHelper.getGravity(getGridAttribute(n, ATTR_LAYOUT_GRAVITY), 0);
        }

        /** Applies the column and row fields into the XML model */
        void applyPositionAttributes() {
            setGridAttribute(node, ATTR_LAYOUT_COLUMN, column);
            setGridAttribute(node, ATTR_LAYOUT_ROW, row);
        }

        /** Returns the id of this node, or makes one up for display purposes */
        String getId() {
            String id = node.getStringAttr(ANDROID_URI, ATTR_ID);
            if (id == null) {
                id = "<unknownid>"; //$NON-NLS-1$
                String fqn = node.getFqcn();
                fqn = fqn.substring(fqn.lastIndexOf('.') + 1);
                id = fqn + "-"
                        + Integer.toString(System.identityHashCode(node)).substring(0, 3);
            }

            return id;
        }

        /** Returns true if this {@link ViewData} represents a spacer */
        boolean isSpacer() {
            return isSpace(node.getFqcn());
        }

        /**
         * Returns true if this {@link ViewData} represents a column spacer
         */
        boolean isColumnSpacer() {
            return isSpacer() &&
                // Any spacer not found in column 0 is a column spacer since we
                // place all horizontal spacers in column 0
                ((column > 0)
                // TODO: Find a cleaner way. Maybe set ids on the elements in (0,0) and
                // for column distinguish by id. Or at least only do this for column 0!
                || !SPACER_SIZE.equals(node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_WIDTH)));
        }

        /**
         * Returns true if this {@link ViewData} represents a row spacer
         */
        boolean isRowSpacer() {
            return isSpacer() &&
                // Any spacer not found in row 0 is a row spacer since we
                // place all vertical spacers in row 0
                ((row > 0)
                // TODO: Find a cleaner way. Maybe set ids on the elements in (0,0) and
                // for column distinguish by id. Or at least only do this for column 0!
                || !SPACER_SIZE.equals(node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_HEIGHT)));
        }
    }

    /**
     * Sets the column span of the given node to the given value (or if the value is 1,
     * removes it)
     *
     * @param node the target node
     * @param span the new column span
     */
    public void setColumnSpanAttribute(INode node, int span) {
        setGridAttribute(node, ATTR_LAYOUT_COLUMN_SPAN, span > 1 ? Integer.toString(span) : null);
    }

    /**
     * Sets the row span of the given node to the given value (or if the value is 1,
     * removes it)
     *
     * @param node the target node
     * @param span the new row span
     */
    public void setRowSpanAttribute(INode node, int span) {
        setGridAttribute(node, ATTR_LAYOUT_ROW_SPAN, span > 1 ? Integer.toString(span) : null);
    }

    /** Returns the index of the given target node in the given child node array */
    static int getChildIndex(INode[] children, INode target) {
        int index = 0;
        for (INode child : children) {
            if (child == target) {
                return index;
            }
            index++;
        }

        return -1;
    }

    /**
     * Update the model to account for the given nodes getting deleted. The nodes
     * are not actually deleted by this method; that is assumed to be performed by the
     * caller. Instead this method performs whatever model updates are necessary to
     * preserve the grid structure.
     *
     * @param nodes the nodes to be deleted
     */
    public void onDeleted(@NonNull List<INode> nodes) {
        if (nodes.size() == 0) {
            return;
        }

        // Attempt to clean up spacer objects for any newly-empty rows or columns
        // as the result of this deletion

        Set<INode> deleted = new HashSet<INode>();

        for (INode child : nodes) {
            // We don't care about deletion of spacers
            String fqcn = child.getFqcn();
            if (fqcn.equals(FQCN_SPACE) || fqcn.equals(FQCN_SPACE_V7)) {
                continue;
            }
            deleted.add(child);
        }

        Set<Integer> usedColumns = new HashSet<Integer>(actualColumnCount);
        Set<Integer> usedRows = new HashSet<Integer>(actualRowCount);
        Multimap<Integer, ViewData> columnSpacers = ArrayListMultimap.create(actualColumnCount, 2);
        Multimap<Integer, ViewData> rowSpacers = ArrayListMultimap.create(actualRowCount, 2);
        Set<ViewData> removedViews = new HashSet<ViewData>();

        for (ViewData view : mChildViews) {
            if (deleted.contains(view.node)) {
                removedViews.add(view);
            } else if (view.isColumnSpacer()) {
                columnSpacers.put(view.column, view);
            } else if (view.isRowSpacer()) {
                rowSpacers.put(view.row, view);
            } else {
                usedColumns.add(Integer.valueOf(view.column));
                usedRows.add(Integer.valueOf(view.row));
            }
        }

        if (usedColumns.size() == 0 || usedRows.size() == 0) {
            // No more views - just remove all the spacers
            for (ViewData spacer : columnSpacers.values()) {
                layout.removeChild(spacer.node);
            }
            for (ViewData spacer : rowSpacers.values()) {
                layout.removeChild(spacer.node);
            }
            mChildViews.clear();
            actualColumnCount = 0;
            declaredColumnCount = 2;
            actualRowCount = 0;
            declaredRowCount = UNDEFINED;
            setGridAttribute(layout, ATTR_COLUMN_COUNT, 2);

            return;
        }

        // Determine columns to introduce spacers into:
        // This is tricky; I should NOT combine spacers if there are cells tied to
        // individual ones

        // TODO: Invalidate column sizes too! Otherwise repeated updates might get confused!
        // Similarly, inserts need to do the same!

        // Produce map of old column numbers to new column numbers
        // Collapse regions of consecutive space and non-space ranges together
        int[] columnMap = new int[actualColumnCount + 1]; // +1: Easily handle columnSpans as well
        int newColumn = 0;
        boolean prevUsed = usedColumns.contains(0);
        for (int column = 1; column < actualColumnCount; column++) {
            boolean used = usedColumns.contains(column);
            if (used || prevUsed != used) {
                newColumn++;
                prevUsed = used;
            }
            columnMap[column] = newColumn;
        }
        newColumn++;
        columnMap[actualColumnCount] = newColumn;
        assert columnMap[0] == 0;

        int[] rowMap = new int[actualRowCount + 1]; // +1: Easily handle rowSpans as well
        int newRow = 0;
        prevUsed = usedRows.contains(0);
        for (int row = 1; row < actualRowCount; row++) {
            boolean used = usedRows.contains(row);
            if (used || prevUsed != used) {
                newRow++;
                prevUsed = used;
            }
            rowMap[row] = newRow;
        }
        newRow++;
        rowMap[actualRowCount] = newRow;
        assert rowMap[0] == 0;


        // Adjust column and row numbers to account for deletions: for a given cell, if it
        // is to the right of a deleted column, reduce its column number, and if it only
        // spans across the deleted column, reduce its column span.
        for (ViewData view : mChildViews) {
            if (removedViews.contains(view)) {
                continue;
            }
            int newColumnStart = columnMap[Math.min(columnMap.length - 1, view.column)];
            // Gracefully handle rogue/invalid columnSpans in the XML
            int newColumnEnd = columnMap[Math.min(columnMap.length - 1,
                    view.column + view.columnSpan)];
            if (newColumnStart != view.column) {
                view.column = newColumnStart;
                setGridAttribute(view.node, ATTR_LAYOUT_COLUMN, view.column);
            }

            int columnSpan = newColumnEnd - newColumnStart;
            if (columnSpan != view.columnSpan) {
                if (columnSpan >= 1) {
                    view.columnSpan = columnSpan;
                    setColumnSpanAttribute(view.node, view.columnSpan);
                } // else: merging spacing columns together
            }


            int newRowStart = rowMap[Math.min(rowMap.length - 1, view.row)];
            int newRowEnd = rowMap[Math.min(rowMap.length - 1, view.row + view.rowSpan)];
            if (newRowStart != view.row) {
                view.row = newRowStart;
                setGridAttribute(view.node, ATTR_LAYOUT_ROW, view.row);
            }

            int rowSpan = newRowEnd - newRowStart;
            if (rowSpan != view.rowSpan) {
                if (rowSpan >= 1) {
                    view.rowSpan = rowSpan;
                    setRowSpanAttribute(view.node, view.rowSpan);
                } // else: merging spacing rows together
            }
        }

        // Merge spacers (and add spacers for newly empty columns)
        int start = 0;
        while (start < actualColumnCount) {
            // Find next unused span
            while (start < actualColumnCount && usedColumns.contains(start)) {
                start++;
            }
            if (start == actualColumnCount) {
                break;
            }
            assert !usedColumns.contains(start);
            // Find the next span of unused columns and produce a SINGLE
            // spacer for that range (unless it's a zero-sized columns)
            int end = start + 1;
            for (; end < actualColumnCount; end++) {
                if (usedColumns.contains(end)) {
                    break;
                }
            }

            // Add up column sizes
            int width = getColumnWidth(start, end - start);

            // Find all spacers: the first one found should be moved to the start column
            // and assigned to the full height of the columns, and
            // the column count reduced by the corresponding amount

            // TODO: if width = 0, fully remove

            boolean isFirstSpacer = true;
            for (int column = start; column < end; column++) {
                Collection<ViewData> spacers = columnSpacers.get(column);
                if (spacers != null && !spacers.isEmpty()) {
                    // Avoid ConcurrentModificationException since we're inserting into the
                    // map within this loop (always at a different index, but the map doesn't
                    // know that)
                    spacers = new ArrayList<ViewData>(spacers);
                    for (ViewData spacer : spacers) {
                        if (isFirstSpacer) {
                            isFirstSpacer = false;
                            spacer.column = columnMap[start];
                            setGridAttribute(spacer.node, ATTR_LAYOUT_COLUMN, spacer.column);
                            if (end - start > 1) {
                                // Compute a merged width for all the spacers (not needed if
                                // there's just one spacer; it should already have the correct width)
                                int columnWidthDp = mRulesEngine.pxToDp(width);
                                spacer.node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH,
                                        String.format(VALUE_N_DP, columnWidthDp));
                            }
                            columnSpacers.put(start, spacer);
                        } else {
                            removedViews.add(spacer); // Mark for model removal
                            layout.removeChild(spacer.node);
                        }
                    }
                }
            }

            if (isFirstSpacer) {
                // No spacer: create one
                int columnWidthDp = mRulesEngine.pxToDp(width);
                addSpacer(layout, -1, UNDEFINED, columnMap[start], columnWidthDp, DEFAULT_CELL_HEIGHT);
            }

            start = end;
        }
        actualColumnCount = newColumn;
//if (usedColumns.contains(newColumn)) {
//    // TODO: This may be totally wrong for right aligned content!
//    actualColumnCount++;
//}

        // Merge spacers for rows
        start = 0;
        while (start < actualRowCount) {
            // Find next unused span
            while (start < actualRowCount && usedRows.contains(start)) {
                start++;
            }
            if (start == actualRowCount) {
                break;
            }
            assert !usedRows.contains(start);
            // Find the next span of unused rows and produce a SINGLE
            // spacer for that range (unless it's a zero-sized rows)
            int end = start + 1;
            for (; end < actualRowCount; end++) {
                if (usedRows.contains(end)) {
                    break;
                }
            }

            // Add up row sizes
            int height = getRowHeight(start, end - start);

            // Find all spacers: the first one found should be moved to the start row
            // and assigned to the full height of the rows, and
            // the row count reduced by the corresponding amount

            // TODO: if width = 0, fully remove

            boolean isFirstSpacer = true;
            for (int row = start; row < end; row++) {
                Collection<ViewData> spacers = rowSpacers.get(row);
                if (spacers != null && !spacers.isEmpty()) {
                    // Avoid ConcurrentModificationException since we're inserting into the
                    // map within this loop (always at a different index, but the map doesn't
                    // know that)
                    spacers = new ArrayList<ViewData>(spacers);
                    for (ViewData spacer : spacers) {
                        if (isFirstSpacer) {
                            isFirstSpacer = false;
                            spacer.row = rowMap[start];
                            setGridAttribute(spacer.node, ATTR_LAYOUT_ROW, spacer.row);
                            if (end - start > 1) {
                                // Compute a merged width for all the spacers (not needed if
                                // there's just one spacer; it should already have the correct height)
                                int rowHeightDp = mRulesEngine.pxToDp(height);
                                spacer.node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT,
                                        String.format(VALUE_N_DP, rowHeightDp));
                            }
                            rowSpacers.put(start, spacer);
                        } else {
                            removedViews.add(spacer); // Mark for model removal
                            layout.removeChild(spacer.node);
                        }
                    }
                }
            }

            if (isFirstSpacer) {
                // No spacer: create one
                int rowWidthDp = mRulesEngine.pxToDp(height);
                addSpacer(layout, -1, rowMap[start], UNDEFINED, DEFAULT_CELL_WIDTH, rowWidthDp);
            }

            start = end;
        }
        actualRowCount = newRow;
//        if (usedRows.contains(newRow)) {
//            actualRowCount++;
//        }

        // Update the model: remove removed children from the view data list
        if (removedViews.size() <= 2) {
            mChildViews.removeAll(removedViews);
        } else {
            List<ViewData> remaining =
                    new ArrayList<ViewData>(mChildViews.size() - removedViews.size());
            for (ViewData view : mChildViews) {
                if (!removedViews.contains(view)) {
                    remaining.add(view);
                }
            }
            mChildViews = remaining;
        }

        // Update the final column and row declared attributes
        if (declaredColumnCount != UNDEFINED) {
            declaredColumnCount = actualColumnCount;
            setGridAttribute(layout, ATTR_COLUMN_COUNT, actualColumnCount);
        }
        if (declaredRowCount != UNDEFINED) {
            declaredRowCount = actualRowCount;
            setGridAttribute(layout, ATTR_ROW_COUNT, actualRowCount);
        }
    }

    /**
     * Adds a spacer to the given parent, at the given index.
     *
     * @param parent the GridLayout
     * @param index the index to insert the spacer at, or -1 to append
     * @param row the row to add the spacer to (or {@link #UNDEFINED} to not set a row yet
     * @param column the column to add the spacer to (or {@link #UNDEFINED} to not set a
     *            column yet
     * @param widthDp the width in device independent pixels to assign to the spacer
     * @param heightDp the height in device independent pixels to assign to the spacer
     * @return the newly added spacer
     */
    ViewData addSpacer(INode parent, int index, int row, int column,
            int widthDp, int heightDp) {
        INode spacer;

        String tag = FQCN_SPACE;
        String gridLayout = parent.getFqcn();
        if (!gridLayout.equals(GRID_LAYOUT) && gridLayout.length() > GRID_LAYOUT.length()) {
            String pkg = gridLayout.substring(0, gridLayout.length() - GRID_LAYOUT.length());
            tag = pkg + SPACE;
        }
        if (index != -1) {
            spacer = parent.insertChildAt(tag, index);
        } else {
            spacer = parent.appendChild(tag);
        }

        ViewData view = new ViewData(spacer, index != -1 ? index : mChildViews.size());
        mChildViews.add(view);

        if (row != UNDEFINED) {
            view.row = row;
            setGridAttribute(spacer, ATTR_LAYOUT_ROW, row);
        }
        if (column != UNDEFINED) {
            view.column = column;
            setGridAttribute(spacer, ATTR_LAYOUT_COLUMN, column);
        }
        if (widthDp > 0) {
            spacer.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH,
                    String.format(VALUE_N_DP, widthDp));
        }
        if (heightDp > 0) {
            spacer.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT,
                    String.format(VALUE_N_DP, heightDp));
        }

        // Temporary hack
        if (GridLayoutRule.sDebugGridLayout) {
            //String id = NEW_ID_PREFIX + "s";
            //if (row == 0) {
            //    id += "c";
            //}
            //if (column == 0) {
            //    id += "r";
            //}
            //if (row > 0) {
            //    id += Integer.toString(row);
            //}
            //if (column > 0) {
            //    id += Integer.toString(column);
            //}
            String id = NEW_ID_PREFIX + "spacer_" //$NON-NLS-1$
                    + Integer.toString(System.identityHashCode(spacer)).substring(0, 3);
            spacer.setAttribute(ANDROID_URI, ATTR_ID, id);
        }


        return view;
    }

    /**
     * Returns the string value of the given attribute, or null if it does not
     * exist. This only works for attributes that are GridLayout specific, such
     * as columnCount, layout_column, layout_row_span, etc.
     *
     * @param node the target node
     * @param name the attribute name (which must be in the android: namespace)
     * @return the attribute value or null
     */

    public String getGridAttribute(INode node, String name) {
        return node.getStringAttr(getNamespace(), name);
    }

    /**
     * Returns the integer value of the given attribute, or the given defaultValue if the
     * attribute was not set. This only works for attributes that are GridLayout specific,
     * such as columnCount, layout_column, layout_row_span, etc.
     *
     * @param node the target node
     * @param attribute the attribute name (which must be in the android: namespace)
     * @param defaultValue the default value to use if the value is not set
     * @return the attribute integer value
     */
    private int getGridAttribute(INode node, String attribute, int defaultValue) {
        String valueString = node.getStringAttr(getNamespace(), attribute);
        if (valueString != null) {
            try {
                return Integer.decode(valueString);
            } catch (NumberFormatException nufe) {
                // Ignore - error in user's XML
            }
        }

        return defaultValue;
    }

    /**
     * Returns the number of children views in the GridLayout
     *
     * @return the number of children views in the GridLayout
     */
    public int getViewCount() {
        return mChildViews.size();
    }

    /**
     * Returns true if the given class name represents a spacer
     *
     * @param fqcn the fully qualified class name
     * @return true if this is a spacer
     */
    public static boolean isSpace(String fqcn) {
        return FQCN_SPACE.equals(fqcn) || FQCN_SPACE_V7.equals(fqcn);
    }
}
