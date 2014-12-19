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

import static org.eclipse.andmore.common.layout.GridLayoutRule.GRID_SIZE;
import static org.eclipse.andmore.common.layout.GridLayoutRule.MARGIN_SIZE;
import static org.eclipse.andmore.common.layout.grid.GridModel.UNDEFINED;

import org.eclipse.andmore.common.layout.GridLayoutRule;

import com.android.annotations.NonNull;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

/**
 * Painter which paints feedback during drag, drop and resizing operations, as well as
 * static selection feedback
 */
public class GridLayoutPainter {

    /**
     * Creates a painter for drop feedback
     *
     * @param rule the corresponding {@link GridLayoutRule}
     * @param elements the dragged elements
     * @return a {@link IFeedbackPainter} which can paint the drop feedback
     */
    public static IFeedbackPainter createDropFeedbackPainter(GridLayoutRule rule,
            IDragElement[] elements) {
        return new DropFeedbackPainter(rule, elements);
    }

    /**
     * Paints the structure (the grid model) of the given GridLayout.
     *
     * @param style the drawing style to use to paint the structure lines
     * @param layout the grid layout node
     * @param gc the graphics context to paint into
     * @param grid the grid model to be visualized
     */
    public static void paintStructure(DrawingStyle style, INode layout, IGraphics gc,
            GridModel grid) {
        Rect b = layout.getBounds();

        gc.useStyle(style);
        for (int row = 0; row < grid.actualRowCount; row++) {
            int y = grid.getRowY(row);
            gc.drawLine(b.x, y, b.x2(), y);
        }
        for (int column = 0; column < grid.actualColumnCount; column++) {
            int x = grid.getColumnX(column);
            gc.drawLine(x, b.y, x, b.y2());
        }
    }

    /**
     * Paints a regular grid according to the {@link GridLayoutRule#GRID_SIZE} and
     * {@link GridLayoutRule#MARGIN_SIZE} dimensions. These are the same lines that
     * snap-to-grid will align with.
     *
     * @param layout the GridLayout node
     * @param gc the graphics context to paint the grid into
     */
    public static void paintGrid(INode layout, IGraphics gc) {
        Rect b = layout.getBounds();

        int oldAlpha = gc.getAlpha();
        gc.useStyle(DrawingStyle.GUIDELINE);
        gc.setAlpha(128);

        int y1 = b.y + MARGIN_SIZE;
        int y2 = b.y2() - MARGIN_SIZE;
        for (int y = y1; y < y2; y += GRID_SIZE) {
            int x1 = b.x + MARGIN_SIZE;
            int x2 = b.x2() - MARGIN_SIZE;
            for (int x = x1; x < x2; x += GRID_SIZE) {
                gc.drawPoint(x, y);
            }
        }
        gc.setAlpha(oldAlpha);
    }

    /**
     * Paint resizing feedback (which currently paints the grid model faintly.)
     *
     * @param gc the graphics context
     * @param layout the GridLayout
     * @param grid the grid model
     */
    public static void paintResizeFeedback(IGraphics gc, INode layout, GridModel grid) {
        paintStructure(DrawingStyle.GRID, layout, gc, grid);
    }

    /**
     * A painter which can paint the drop feedback for elements being dragged into or
     * within a GridLayout.
     */
    private static class DropFeedbackPainter implements IFeedbackPainter {
        private final GridLayoutRule mRule;
        private final IDragElement[] mElements;

        /** Constructs a new {@link GridLayoutPainter} bound to the given {@link GridLayoutRule}
         * @param rule the corresponding rule
         * @param elements the elements to draw */
        public DropFeedbackPainter(GridLayoutRule rule, IDragElement[] elements) {
            mRule = rule;
            mElements = elements;
        }

        // Implements IFeedbackPainter
        @Override
        public void paint(@NonNull IGraphics gc, @NonNull INode node,
                @NonNull DropFeedback feedback) {
            Rect b = node.getBounds();
            if (!b.isValid()) {
                return;
            }

            // Highlight the receiver
            gc.useStyle(DrawingStyle.DROP_RECIPIENT);
            gc.drawRect(b);
            GridDropHandler data = (GridDropHandler) feedback.userData;

            if (!GridLayoutRule.sGridMode) {
                paintFreeFormDropFeedback(gc, node, feedback, b, data);
            } else {
                paintGridModeDropFeedback(gc, b, data);
            }
        }

        /**
         * Paints the drag feedback for a free-form mode drag
         */
        private void paintFreeFormDropFeedback(IGraphics gc, INode node, DropFeedback feedback,
                Rect b, GridDropHandler data) {
            GridModel grid = data.getGrid();
            if (GridLayoutRule.sSnapToGrid) {
                GridLayoutPainter.paintGrid(node, gc);
            }
            GridLayoutPainter.paintStructure(DrawingStyle.GRID, node, gc, grid);

            GridMatch rowMatch = data.getRowMatch();
            GridMatch columnMatch = data.getColumnMatch();

            if (rowMatch == null || columnMatch == null) {
                return;
            }

            IDragElement first = mElements[0];
            Rect dragBounds = first.getBounds();
            int offsetX = 0;
            int offsetY = 0;
            if (rowMatch.type == SegmentType.BOTTOM) {
                offsetY -= dragBounds.h;
            } else if (rowMatch.type == SegmentType.BASELINE) {
                offsetY -= feedback.dragBaseline;
            }
            if (columnMatch.type == SegmentType.RIGHT) {
                offsetX -= dragBounds.w;
            } else if (columnMatch.type == SegmentType.CENTER_HORIZONTAL) {
                offsetX -= dragBounds.w / 2;
            }

            // Draw guidelines for matches
            int y = rowMatch.matchedLine;
            int x = columnMatch.matchedLine;
            Rect bounds = first.getBounds();

            // Draw margin
            if (rowMatch.margin != UNDEFINED && rowMatch.margin > 0) {
                gc.useStyle(DrawingStyle.DISTANCE);
                int centerX = bounds.w / 2 + offsetX + x;
                int y1;
                int y2;
                if (rowMatch.type == SegmentType.TOP) {
                    y1 = offsetY + y - 1;
                    y2 = rowMatch.matchedLine - rowMatch.margin;
                } else {
                    assert rowMatch.type == SegmentType.BOTTOM;
                    y1 = bounds.h + offsetY + y - 1;
                    y2 = rowMatch.matchedLine + rowMatch.margin;
                }
                gc.drawLine(b.x, y1, b.x2(), y1);
                gc.drawLine(b.x, y2, b.x2(), y2);
                gc.drawString(Integer.toString(rowMatch.margin),
                        centerX - 3, y1 + (y2 - y1 - 16) / 2);
            } else {
                gc.useStyle(rowMatch.margin == 0 ? DrawingStyle.DISTANCE
                        : rowMatch.createCell ? DrawingStyle.GUIDELINE_DASHED
                                : DrawingStyle.GUIDELINE);
                gc.drawLine(b.x, y, b.x2(), y );
            }

            if (columnMatch.margin != UNDEFINED && columnMatch.margin > 0) {
                gc.useStyle(DrawingStyle.DISTANCE);
                int centerY = bounds.h / 2 + offsetY + y;
                int x1;
                int x2;
                if (columnMatch.type == SegmentType.LEFT) {
                    x1 = offsetX + x - 1;
                    x2 = columnMatch.matchedLine - columnMatch.margin;
                } else {
                    assert columnMatch.type == SegmentType.RIGHT;
                    x1 = bounds.w + offsetX + x - 1;
                    x2 = columnMatch.matchedLine + columnMatch.margin;
                }
                gc.drawLine(x1, b.y, x1, b.y2());
                gc.drawLine(x2, b.y, x2, b.y2());
                gc.drawString(Integer.toString(columnMatch.margin),
                        x1 + (x2 - x1 - 16) / 2, centerY - 3);
            } else {
                gc.useStyle(columnMatch.margin == 0 ? DrawingStyle.DISTANCE
                        : columnMatch.createCell ? DrawingStyle.GUIDELINE_DASHED
                                : DrawingStyle.GUIDELINE);
                gc.drawLine(x, b.y, x, b.y2());
            }

            // Draw preview rectangles for all the dragged elements
            gc.useStyle(DrawingStyle.DROP_PREVIEW);
            offsetX += x - bounds.x;
            offsetY += y - bounds.y;

            for (IDragElement element : mElements) {
                if (element == first) {
                    mRule.drawElement(gc, first, offsetX, offsetY);
                    // Preview baseline as well
                    if (feedback.dragBaseline != -1) {
                        int x1 = dragBounds.x + offsetX;
                        int y1 = dragBounds.y + offsetY + feedback.dragBaseline;
                        gc.drawLine(x1, y1, x1 + dragBounds.w, y1);
                    }
                } else {
                    b = element.getBounds();
                    if (b.isValid()) {
                        gc.drawRect(b.x + offsetX, b.y + offsetY,
                                b.x + offsetX + b.w, b.y + offsetY + b.h);
                    }
                }
            }
        }

        /**
         * Paints the drag feedback for a grid-mode drag
         */
        private void paintGridModeDropFeedback(IGraphics gc, Rect b, GridDropHandler data) {
            int radius = mRule.getNewCellSize();
            GridModel grid = data.getGrid();

            gc.useStyle(DrawingStyle.GUIDELINE);
            // Paint grid
            for (int row = 1; row < grid.actualRowCount; row++) {
                int y = grid.getRowY(row);
                gc.drawLine(b.x, y - radius, b.x2(), y - radius);
                gc.drawLine(b.x, y + radius, b.x2(), y + radius);

            }
            for (int column = 1; column < grid.actualColumnCount; column++) {
                int x = grid.getColumnX(column);
                gc.drawLine(x - radius, b.y, x - radius, b.y2());
                gc.drawLine(x + radius, b.y, x + radius, b.y2());
            }
            gc.drawRect(b.x, b.y, b.x2(), b.y2());
            gc.drawRect(b.x + 2 * radius, b.y + 2 * radius,
                    b.x2() - 2 * radius, b.y2() - 2 * radius);

            GridMatch columnMatch = data.getColumnMatch();
            GridMatch rowMatch = data.getRowMatch();
            int column = columnMatch.cellIndex;
            int row = rowMatch.cellIndex;
            boolean createColumn = columnMatch.createCell;
            boolean createRow = rowMatch.createCell;

            Rect cellBounds = grid.getCellBounds(row, column, 1, 1);

            IDragElement first = mElements[0];
            Rect dragBounds = first.getBounds();
            int offsetX = cellBounds.x - dragBounds.x;
            int offsetY = cellBounds.y - dragBounds.y;

            gc.useStyle(DrawingStyle.DROP_ZONE_ACTIVE);
            if (createColumn) {
                gc.fillRect(new Rect(cellBounds.x - radius,
                        cellBounds.y + (createRow ? -radius : radius),
                        2 * radius + 1, cellBounds.h - (createRow ? 0 : 2 * radius)));
                offsetX -= radius + dragBounds.w / 2;
            }
            if (createRow) {
                gc.fillRect(new Rect(cellBounds.x + radius, cellBounds.y - radius,
                        cellBounds.w - 2 * radius, 2 * radius + 1));
                offsetY -= radius + dragBounds.h / 2;
            } else if (!createColumn) {
                // Choose this cell
                gc.fillRect(new Rect(cellBounds.x + radius, cellBounds.y + radius,
                        cellBounds.w - 2 * radius, cellBounds.h - 2 * radius));
            }

            gc.useStyle(DrawingStyle.DROP_PREVIEW);

            Rect bounds = first.getBounds();
            int x = offsetX;
            int y = offsetY;
            if (columnMatch.type == SegmentType.RIGHT) {
                x += cellBounds.w - bounds.w;
            } else if (columnMatch.type == SegmentType.CENTER_HORIZONTAL) {
                x += cellBounds.w / 2 - bounds.w / 2;
            }
            if (rowMatch.type == SegmentType.BOTTOM) {
                y += cellBounds.h - bounds.h;
            } else if (rowMatch.type == SegmentType.CENTER_VERTICAL) {
                y += cellBounds.h / 2 - bounds.h / 2;
            }

            mRule.drawElement(gc, first, x, y);
        }
    }

    /**
     * Paints the structure (the row and column boundaries) of the given
     * GridLayout
     *
     * @param view the instance of the GridLayout whose structure should be
     *            painted
     * @param style the drawing style to use for the cell boundaries
     * @param layout the layout element
     * @param gc the graphics context
     * @return true if the structure was successfully inferred from the view and
     *         painted
     */
    public static boolean paintStructure(Object view, DrawingStyle style, INode layout,
            IGraphics gc) {
        Pair<int[],int[]> cellBounds = GridModel.getAxisBounds(view);
        if (cellBounds != null) {
            int[] xs = cellBounds.getFirst();
            int[] ys = cellBounds.getSecond();
            Rect b = layout.getBounds();
            gc.useStyle(style);
            for (int row = 0; row < ys.length; row++) {
                int y = ys[row] + b.y;
                gc.drawLine(b.x, y, b.x2(), y);
            }
            for (int column = 0; column < xs.length; column++) {
                int x = xs[column] + b.x;
                gc.drawLine(x, b.y, x, b.y2());
            }

            return true;
        } else {
            return false;
        }
    }
}
