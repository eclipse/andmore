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

import static org.eclipse.andmore.common.layout.grid.GridModel.UNDEFINED;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.SegmentType;

/**
 * A match for a drag within a GridLayout, corresponding to an alignment with another
 * edge, or a margin, or centering, or a gap distance from another edge and so on.
 */
class GridMatch implements Comparable<GridMatch> {
    /** The distance to the matched edge - used to pick best matches */
    public final int distance;

    /** Type of edge that was matched (this refers to the edge on the dragged node,
     * not on the matched node/row/cell etc) */
    public final SegmentType type;

    /** Row or column for the match */
    public int cellIndex;

    /** If true, create a new row/column */
    public boolean createCell;

    /** The actual x or y position of the matched segment */
    public int matchedLine;

    /** Amount of margin between the matched edges */
    public int margin;

    /**
     * Constructs a match.
     *
     * @param type the edge of the dragged element that was matched
     * @param distance the absolute distance from the ideal match - used to find the best
     *            match
     * @param matchedLine the actual X or Y location of the ideal match
     * @param cellIndex the index of the row or column we matched with
     * @param createCell if true, create a new cell by splitting the existing cell at the
     *            matchedLine position
     * @param margin a margin distance to add to the actual location from the matched line
     */
    GridMatch(SegmentType type, int distance, int matchedLine, int cellIndex,
            boolean createCell, int margin) {
        super();
        this.type = type;
        this.distance = distance;
        this.matchedLine = matchedLine;
        this.cellIndex = cellIndex;
        this.createCell = createCell;
        this.margin = margin;
    }

    // Implements Comparable<GridMatch>
    @Override
    public int compareTo(GridMatch o) {
        // Pick closest matches first
        if (distance != o.distance) {
            return distance - o.distance;
        }

        // Prefer some types of matches over other matches
        return getPriority() - o.getPriority();
    }

    /**
     * Describes the match for the user
     *
     * @param layout the GridLayout containing the match
     * @return a short description for the user of the match
     */
    public String getDisplayName(INode layout) {
        switch (type) {
            case BASELINE:
                return String.format("Align baseline in row %1$d", cellIndex + 1);
            case CENTER_HORIZONTAL:
                return "Center horizontally";
            case LEFT:
                if (!createCell) {
                    return String.format("Insert into column %1$d", cellIndex + 1);
                }
                if (margin != UNDEFINED) {
                    if (cellIndex == 0 && margin != 0) {
                        return "Add one margin distance from the left";
                    }
                    return String.format("Add next to column %1$d", cellIndex + 1);
                }
                return String.format("Align left at x=%1$d", matchedLine - layout.getBounds().x);
            case RIGHT:
                if (!createCell) {
                    return String.format("Insert right-aligned into column %1$d", cellIndex + 1);
                }
                return String.format("Align right at x=%1$d", matchedLine - layout.getBounds().x);
            case TOP:
                if (!createCell) {
                    return String.format("Insert into row %1$d", cellIndex + 1);
                }
                if (margin != UNDEFINED) {
                    if (cellIndex == 0 && margin != 0) {
                        return "Add one margin distance from the top";
                    }
                    return String.format("Add below row %1$d", cellIndex + 1);
                }
                return String.format("Align top at y=%1d", matchedLine - layout.getBounds().y);
            case BOTTOM:
                if (!createCell) {
                    return String.format("Insert into bottom of row %1$d", cellIndex + 1);
                }
                return String.format("Align bottom at y=%1d", matchedLine - layout.getBounds().y);
            case CENTER_VERTICAL:
                return "Center vertically";
            case UNKNOWN:
            default:
                return null;
        }
    }

    /**
     * Computes the sorting priority of this match, giving baseline matches higher
     * precedence than centering which in turn is ordered before external edge matches
     */
    private int getPriority() {
        switch (type) {
            case BASELINE:
                return 0;
            case CENTER_HORIZONTAL:
            case CENTER_VERTICAL:
                return 1;
            case BOTTOM:
            case LEFT:
            case RIGHT:
            case TOP:
                return 2;
        }

        return 3;
    }
}