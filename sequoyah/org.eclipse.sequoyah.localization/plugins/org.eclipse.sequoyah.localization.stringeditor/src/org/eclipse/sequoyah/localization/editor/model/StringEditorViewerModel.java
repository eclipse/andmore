/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Paulo Faria (Eldorado) -  Bug [326793] -  Improvements on the String Arrays handling 
 * Marcelo Marzola Bossoni (Eldorado) -  Bug [326793] -  Allow keys edition
 * Fabricio Violin (Eldorado) -  Bug [326793] -  Revert to saved
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (validate key)
 * Paulo Faria (Eldorado) - Bug [326793] - Fixing highlight problems 
 * Daniel Drigo Pastore (Eldorado) - Bug [326793] - Fixing array image according to array items status
 * Paulo Faria (Eldorado) - Bug [326793] - 0007372: Undo/Redo operations are not properly working.
 * Paulo Faria (Eldorado) - Bug [326793] - 0007419: Issue when doing undo after clone 
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.StringEditorPlugin;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.datatype.IModelChangedListener;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.providers.ICellValidator;

/**
 * 
 * @author rdbp36
 * 
 *         The editor internal model. Works with 3 basic items: Single, Arrays
 *         and Array Items (children)
 * 
 */
public class StringEditorViewerModel
{

    private static final String DEFAULT_COLUMN_NAME = "values";

    private static final int INDEX_NOT_REQUIRED = -1;

    /**
     * List with all columns
     */
    private final List<ColumnInfo> columns;

    /**
     * A Map to quick access the columns by ID
     */
    private final Map<String, ColumnInfo> columnsMap;

    /**
     * A Map to quick access rows by key
     */
    private final Map<String, RowInfo> rowsMap;

    /*
     * A cell validator
     */
    private final ICellValidator validator;

    /*
     * Listeners
     */
    private final List<IModelChangedListener> listeners;

    public StringEditorViewerModel(List<ColumnInfo> infos, ICellValidator cellValidator)
    {
        columns = infos;
        columnsMap = new HashMap<String, ColumnInfo>();
        rowsMap = new TreeMap<String, RowInfo>();
        listeners = new ArrayList<IModelChangedListener>();
        validator = cellValidator;
        initMaps();
        doInitialValidation();
    }

    private void doInitialValidation()
    {
        for (RowInfo row : rowsMap.values())
        {
            validateRow(row.getKey());
        }

    }

    /**
     * Init the maps to keep things on track
     */
    private void initMaps()
    {

        for (ColumnInfo column : columns)
        {
            columnsMap.put(column.getId(), column);
            for (String cellKey : column.getCells().keySet())
            {
                CellInfo cell = column.getCells().get(cellKey);

                if (cell.hasChildren())
                {
                    // array
                    if (rowsMap.get(cellKey) == null)
                    {
                        // first column
                        RowInfo arrayRow = new RowInfo(cellKey);
                        List<CellInfo> subCells = cell.getChildren();
                        for (CellInfo subCell : subCells)
                        {
                            Map<String, CellInfo> cells = new HashMap<String, CellInfo>();
                            cells.put(column.getId(), subCell);
                            // it does not need to keep object instance because
                            // it is inserted on parent (RowInfo arrayRow)
                            arrayRow.addChild(
                                    new RowInfoLeaf(cellKey, arrayRow, subCell.getPosition(), cells),
                                    subCell.getPosition());
                        }
                        rowsMap.put(cellKey, arrayRow);
                    }
                    else
                    {
                        // other columns
                        RowInfo arrayRow = rowsMap.get(cellKey);

                        List<CellInfo> subCells = cell.getChildren();
                        List<RowInfoLeaf> subRows = arrayRow.getChildren();

                        for (CellInfo subCell : subCells)
                        {
                            if (subCell.getPosition() >= 0
                                    && subCell.getPosition() < subRows.size()
                                    && subRows.get(subCell.getPosition()) != null)
                            {
                                RowInfoLeaf leaf = subRows.get(subCell.getPosition());
                                leaf.addCell(column.getId(), subCell);
                            }
                            else
                            {
                                // not found subcell at the given index, create
                                // row and add sub cell
                                Map<String, CellInfo> cells = new HashMap<String, CellInfo>();
                                cells.put(column.getId(), subCell);
                                arrayRow.addChild(
                                        new RowInfoLeaf(cellKey, arrayRow, subCell.getPosition(),
                                                cells), subCell.getPosition());

                            }
                        }
                    }
                }
                else
                {
                    // string
                    if (rowsMap.get(cellKey) == null)
                    {
                        // first column
                        Map<String, CellInfo> cells = new HashMap<String, CellInfo>();
                        cells.put(column.getId(), cell);
                        RowInfoLeaf row = new RowInfoLeaf(cellKey, null, -1, cells);
                        rowsMap.put(cellKey, row);
                    }
                    else
                    {
                        // other columns
                        RowInfo rowInfo = rowsMap.get(cellKey);
                        if (rowInfo instanceof RowInfoLeaf)
                        {
                            ((RowInfoLeaf) rowInfo).addCell(column.getId(), cell);
                        }
                    }
                }
            }

        }
        notifyListeners();

    }

    /**
     * Get the row with the following key
     * 
     * @param key
     * @return the row
     */
    public RowInfo getRow(String key)
    {
        return rowsMap.get(key);
    }

    /**
     * Get the column with the following ID
     * 
     * @param ID
     * @return the column
     */
    public ColumnInfo getColumn(String ID)
    {
        return columnsMap.get(ID);
    }

    /**
     * Get all rows
     * 
     * @return map with all rows and their keys
     */
    public Map<String, RowInfo> getRows()
    {
        return rowsMap;
    }

    /**
     * Get all column
     * 
     * @return list with all columns
     */
    public List<ColumnInfo> getColumns()
    {
        return columns;

    }

    /**
     * Add a new column
     * 
     * @param info
     */
    public void addColumn(ColumnInfo info)
    {

        columns.add(info);
        columnsMap.put(info.getId(), info);
        Map<String, CellInfo> cells = info.getCells();
        Map<String, CellInfo> cellsClone = new HashMap<String, CellInfo>(cells);
        Iterator<String> keysIterator = cellsClone.keySet().iterator();

        while (keysIterator.hasNext())
        {
            String key = keysIterator.next();
            CellInfo cell = cells.get(key);

            RowInfo row = rowsMap.get(key);

            if (cell != null && cell.hasChildren() && cell.getChildren().size() > 0)
            {
                // array
                List<CellInfo> subCells = cell.getChildren();

                if (row == null)
                {
                    row = new RowInfo(key);
                    rowsMap.put(key, row);
                    for (CellInfo subCell : subCells)
                    {
                        Map<String, CellInfo> cellsForRow = new HashMap<String, CellInfo>();
                        cellsForRow.put(info.getId(), subCell);
                        row.addChild(new RowInfoLeaf(key, row, subCell.getPosition(), cellsForRow),
                                subCell.getPosition());
                    }
                }
                else
                {
                    List<RowInfoLeaf> subRows = row.getChildren();

                    for (CellInfo subCell : subCells)
                    {
                        if (subCell != null)
                        {
                            if (subCell.getPosition() >= 0
                                    && subCell.getPosition() < subRows.size()
                                    && subRows.get(subCell.getPosition()) != null)
                            {
                                RowInfoLeaf leaf = subRows.get(subCell.getPosition());
                                addCellIfItIsNotOnDefaultColumn(info, leaf);
                                leaf.addCell(info.getId(), subCell);
                            }
                            else
                            {
                                Map<String, CellInfo> cellsForArray =
                                        new HashMap<String, CellInfo>();
                                cellsForArray.put(info.getId(), subCell);
                                addRow(new RowInfoLeaf(key, row, subCell.getPosition(),
                                        cellsForArray));
                            }
                        }
                    }
                }
            }
            else if (cell != null && !cell.hasChildren())
            {
                // string
                if (row == null)
                {
                    Map<String, CellInfo> cellsForArray = new HashMap<String, CellInfo>();
                    cellsForArray.put(info.getId(), cell);
                    row = new RowInfoLeaf(key, null, -1, cellsForArray);
                    rowsMap.put(key, row);
                }
                else
                {
                    if (row instanceof RowInfoLeaf)
                    {
                        RowInfoLeaf leaf = ((RowInfoLeaf) row);
                        addCellIfItIsNotOnDefaultColumn(info, leaf);
                        leaf.addCell(info.getId(), cell);
                    }
                }
            }
            if (row != null)
            {
                validateRow(row);
            }
        }
        notifyListeners();
    }

    /**
     * Add cell if it being insert on non-default column and it is not already on default column 
     * @param info
     * @param leaf
     */
    private void addCellIfItIsNotOnDefaultColumn(ColumnInfo info, RowInfoLeaf leaf)
    {
        if (!info.getId().equals(DEFAULT_COLUMN_NAME) && leaf.getCells() != null
                && leaf.getCells().get(DEFAULT_COLUMN_NAME) == null)
        {
            //it is trying to insert a new value on non-default column, but there is no cell in the default column
            //=> add cell into default column
            leaf.addCell(DEFAULT_COLUMN_NAME, new CellInfo("", ""));
        }
    }

    /**
     * Add a new Row.
     * 
     * @param info
     */
    public void addRow(RowInfo info)
    {
        Map<String, CellInfo> cells = null;
        if (info instanceof RowInfoLeaf)
        {
            RowInfoLeaf leaf = (RowInfoLeaf) info;
            if (leaf.getParent() == null)
            {
                // string
                cells = leaf.getCells();

                for (String column : columnsMap.keySet())
                {
                    ColumnInfo columnInfo = columnsMap.get(column);
                    if (columnInfo == null)
                    {
                        columnInfo = new ColumnInfo(column, column, null, true);
                        columnsMap.put(column, columnInfo);
                        columns.add(columnInfo);
                    }
                    CellInfo cell = cells.get(column);
                    if (cell == null)
                    {
                        cell = new CellInfo(false);
                    }
                    columnInfo.addCell(info.getKey(), cell);
                    rowsMap.put(info.getKey(), info);
                }
            }
            else
            {
                // array item
                RowInfo parentRowInfo = leaf.getParent();
                if (rowsMap.get(parentRowInfo.getKey()) == null)
                {
                    rowsMap.put(parentRowInfo.getKey(), parentRowInfo);
                }
                if (!parentRowInfo.getChildren().contains(leaf))
                {
                    parentRowInfo.addChild(leaf, leaf.getPosition());
                }

                for (String column : columnsMap.keySet())
                {
                    ColumnInfo columnInfo = columnsMap.get(column);
                    if (columnInfo == null)
                    {
                        columnInfo = new ColumnInfo(column, column, null, true);
                        columnsMap.put(column, columnInfo);
                        columns.add(columnInfo);
                    }
                    CellInfo parentCell = columnInfo.getCells().get(parentRowInfo.getKey());
                    if (parentCell == null)
                    {
                        parentCell = new CellInfo(true);
                        columnInfo.addCell(parentRowInfo.getKey(), parentCell);
                    }

                    CellInfo subcellInfo = leaf.getCells().get(column);
                    if (subcellInfo == null)
                    {
                        subcellInfo = new CellInfo(null, null, leaf.getPosition());
                    }
                    parentCell.addChild(subcellInfo, leaf.getPosition(), false);
                }
            }
        }
        else
        {
            // array
            List<RowInfoLeaf> subrows = info.getChildren();
            // columnName to parentCell
            Map<String, CellInfo> parentsMap = new LinkedHashMap<String, CellInfo>();

            for (RowInfoLeaf subrow : subrows)
            {
                cells = subrow.getCells();
                for (ColumnInfo cl : columns)
                {
                    String column = cl.getId();
                    CellInfo parentCell = parentsMap.get(column);
                    if (parentCell == null)
                    {
                        // not found => create
                        parentCell = new CellInfo(true);
                        parentsMap.put(column, parentCell);
                    }
                    CellInfo cell = cells.get(column);
                    if (cell == null)
                    {
                        cell = new CellInfo(false);
                    }

                    parentCell.addChild(cell);
                }
            }

            for (String column : parentsMap.keySet())
            {
                ColumnInfo columnInfo = columnsMap.get(column);
                if (columnInfo == null)
                {
                    columnInfo = new ColumnInfo(column, column, null, true);
                    columnsMap.put(column, columnInfo);
                    columns.add(columnInfo);
                }
                columnInfo.addCell(info.getKey(), parentsMap.get(column));
            }
            rowsMap.put(info.getKey(), info);
        }
        validateRow(info);
        notifyListeners();
    }

    /**
     * Add a new cell
     * 
     * @param info
     *            the cell
     * @param key
     *            the key
     * @param column
     *            the column
     */
    public void addCell(CellInfo info, String key, String column)
    {
        addCell(info, key, column, INDEX_NOT_REQUIRED, false);
    }

    /**
     * Add a new cell to a certain column and with the given key
     * 
     * @param info
     *            the cell
     * @param key
     *            the key
     * @param column
     *            the column
     * @param index
     *            the index of this cell within the array or -1 for non array
     *            cells
     * @param overwrite
     *            true to overwrite the cell in the position index or false to
     *            add this cell inside the position, moving the other cells
     */
    public void addCell(CellInfo info, String key, String column, Integer index, boolean overwrite)
    {
        RowInfo rowInfo = rowsMap.get(key);
        if (rowInfo instanceof RowInfoLeaf)
        {
            // string or item array
            RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
            if (leaf.getParent() == null)
            {
                // string
                columnsMap.get(column).addCell(key, info);
            }
            leaf.addCell(column, info);
        }
        else
        {
            // array-item
            if (index >= 0)
            {
                List<RowInfoLeaf> rowChildren = rowInfo.getChildren();

                Map<String, CellInfo> cells = rowChildren.get(index).getCells();
                cells.put(column, info);

                ColumnInfo columnInfo = columnsMap.get(column);
                CellInfo parentCell = columnInfo.getCells().get(key);
                if (parentCell == null)
                {
                    // array does not exist
                    parentCell = new CellInfo(true);
                    columnInfo.addCell(key, parentCell);
                }
                parentCell.addChild(info, index, overwrite);
            }
        }
        validateRow(key);
        notifyListeners();
    }

    /**
     * Remove column from the UI model
     * 
     * @param column
     *            the columnID to remove
     */
    public void removeColumn(String column)
    {
        // remove from columns map
        columnsMap.remove(column);
        // remove from columns list
        List<ColumnInfo> orig = new ArrayList<ColumnInfo>(columns);
        for (ColumnInfo info : orig)
        {
            if (info.getId().equals(column))
            {
                columns.remove(info);
            }
        }
        // remove from rows map
        Map<String, RowInfo> rowsMapClone = new HashMap<String, RowInfo>(rowsMap);
        Iterator<RowInfo> it = rowsMapClone.values().iterator();
        while (it.hasNext())
        {
            RowInfo row = it.next();
            // string item
            if (row instanceof RowInfoLeaf)
            {
                ((RowInfoLeaf) row).removeCell(column);
                if (isEmptyRow(row))
                {
                    removeRow(row.getKey());
                }
            }
            else
            {
                for (RowInfoLeaf leaf : new ArrayList<RowInfoLeaf>(row.getChildren()))
                {
                    leaf.removeCell(column);
                    if (isEmptyRow(leaf))
                    {
                        removeRow(leaf.getKey(), leaf.getPosition());
                    }
                }
            }
        }

        notifyListeners();
    }

    /**
     * Remove column from the UI model. Special treatment to empty array items,
     * which will not be removed under certain circumstances.
     * 
     * @param column
     *            the columnID to remove
     */
    public void removeColumnForRevertion(String column)
    {
        ArrayList<Integer> itemsToRemove = new ArrayList<Integer>();
        // remove from columns map
        columnsMap.remove(column);
        // remove from columns list
        List<ColumnInfo> orig = new ArrayList<ColumnInfo>(columns);
        for (ColumnInfo info : orig)
        {
            if (info.getId().equals(column))
            {
                columns.remove(info);
            }
        }
        // remove from rows map
        Map<String, RowInfo> rowsMapClone = new HashMap<String, RowInfo>(rowsMap);
        Iterator<RowInfo> it = rowsMapClone.values().iterator();
        while (it.hasNext())
        {
            RowInfo row = it.next();
            if (row instanceof RowInfoLeaf)
            {
                RowInfoLeaf leaf = (RowInfoLeaf) row;
                leaf.removeCell(column);
                if (isEmptyRow(row))
                {
                    removeRow(row.getKey());
                }
            }
            else
            {
                List<RowInfoLeaf> arrMap = row.getChildren();
                List<RowInfoLeaf> arrMapClone = new ArrayList<RowInfoLeaf>(arrMap);

                itemsToRemove.clear();
                Iterator<RowInfoLeaf> iterator = arrMapClone.iterator();

                int counter = 0;
                int lastIndex = -2;
                while (iterator.hasNext())
                {
                    RowInfoLeaf tempLeaf = iterator.next();

                    tempLeaf.removeCell(column);
                    if (tempLeaf.getCells().isEmpty())
                    {
                        // the array empty rows are not removed if there is a
                        // valid and already saved row
                        // after it because it will change its position value
                        // that would become its later
                        // addition to the editor (on RevertToSavedOperation)
                        // very complex
                        if (!(itemsToRemove.isEmpty() || counter - lastIndex == 1))
                        {
                            itemsToRemove.clear();
                        }
                        // here we mark the array empty rows for a possible
                        // removal
                        itemsToRemove.add(counter);
                        lastIndex = counter;
                    }
                    counter++;
                }

                if (lastIndex < counter - 1)
                {
                    itemsToRemove.clear();
                }

                counter = 0;
                iterator = arrMapClone.iterator();

                while (iterator.hasNext())
                {
                    // perform the removal
                    RowInfoLeaf tempLeaf = iterator.next();
                    if (itemsToRemove.contains(counter))
                    {
                        removeRow(row.getKey(), tempLeaf.getPosition());
                        arrMap.remove(tempLeaf.getPosition());
                    }
                    counter++;
                }

                if (arrMap.isEmpty())
                {
                    removeRow(row.getKey());
                }
            }
        }

        notifyListeners();
    }

    private boolean isEmptyRow(RowInfo row)
    {
        if (row instanceof RowInfoLeaf)
        {
            return ((RowInfoLeaf) row).getCells().size() == 0;
        }
        else
        {
            return false;
        }
    }

    /**
     * Remove a single row. If the array is empty, remove the array as well
     * 
     * @param key
     *            the row key
     * @param index
     *            if the row is an array item, the index within this array.
     */
    public void removeRow(String key, int index)
    {
        RowInfo row = rowsMap.get(key);
        boolean validate = true;
        if (row != null)
        {
            row.removeChild(index);
            if (row.getChildren().size() == 0)
            {
                rowsMap.remove(key);
                validate = false;
            }
        }
        Iterator<ColumnInfo> it = columnsMap.values().iterator();
        while (it.hasNext())
        {
            ColumnInfo col = it.next();
            CellInfo parent = col.getCells().get(key);
            if (parent != null)
            {
                parent.removeChild(index);
                if (parent.getChildren().size() == 0)
                {
                    col.removeCell(key);
                }
            }
        }
        if (validate)
        {
            validateRow(row);
        }
        notifyListeners();
    }

    /**
     * Remove entire row. If the row is an array, remove the entire array
     * 
     * @param key
     */
    public void removeRow(String key)
    {
        rowsMap.remove(key);

        Iterator<ColumnInfo> it = columnsMap.values().iterator();
        while (it.hasNext())
        {
            ColumnInfo col = it.next();
            col.removeCell(key);
        }

        notifyListeners();
    }

    /**
     * Rename a key to another name
     * 
     * @param oldKey
     *            old key name
     * @param newKey
     *            the new name
     */
    public void renameKey(String oldKey, String newKey)
    {
        RowInfo theRow = rowsMap.get(oldKey);
        if (theRow != null)
        {
            rowsMap.remove(oldKey);
            theRow.setKey(newKey);
            rowsMap.put(newKey, theRow);
            for (ColumnInfo column : columns)
            {
                column.renameKey(oldKey, newKey);
            }
            validateRow(theRow);

            // check if it is a rowinfo and change its children
            for (RowInfoLeaf leaf : theRow.getChildren())
            {
                leaf.setKey(newKey);
            }
        }

    }

    public void addListener(IModelChangedListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(IModelChangedListener listener)
    {
        listeners.remove(listener);
    }

    private void notifyListeners()
    {
        for (IModelChangedListener listener : listeners)
        {
            listener.modelChanged(this);
        }
    }

    /**
     * Clean dirty state of cells
     * 
     * @return changed lines
     */
    public List<RowInfo> save()
    {
        List<RowInfo> changed = new ArrayList<RowInfo>();
        for (RowInfo info : rowsMap.values())
        {
            Collection<RowInfoLeaf> leaves;
            if (info instanceof RowInfoLeaf)
            {
                leaves = new ArrayList<RowInfoLeaf>(1);
                leaves.add((RowInfoLeaf) info);
            }
            else
            {
                leaves = info.getChildren();
            }
            for (RowInfoLeaf leaf : leaves)
            {
                for (CellInfo cell : leaf.getCells().values())
                {
                    if (cell != null && cell.isDirty())
                    {
                        cell.setDirty(false);
                        changed.add(info);
                    }
                }
            }
        }
        return changed;
    }

    public List<ColumnInfo> getColumnsChanged()
    {
        Set<ColumnInfo> changed = new HashSet<ColumnInfo>();
        for (RowInfo info : rowsMap.values())
        {
            Collection<RowInfoLeaf> leaves;
            if (info instanceof RowInfoLeaf)
            {
                leaves = new ArrayList<RowInfoLeaf>(1);
                leaves.add((RowInfoLeaf) info);
            }
            else
            {
                leaves = info.getChildren();
            }
            for (RowInfoLeaf leaf : leaves)
            {
                for (Map.Entry<String, CellInfo> cellEntry : leaf.getCells().entrySet())
                {
                    CellInfo cell = cellEntry.getValue();
                    if (cell != null && cell.isDirty())
                    {
                        changed.add(columnsMap.get(cellEntry.getKey()));
                    }
                }
            }
        }
        return (new ArrayList<ColumnInfo>(changed));
    }

    /**
     * Validate key
     */
    public IStatus validateRowKey(String key)
    {
        RowInfo row = rowsMap.get(key);
        IStatus rowKeyStatus = validator.isKeyValid(row.getKey());
        if (!rowKeyStatus.isOK())
        {
            row.addStatus(rowKeyStatus);
        }
        return rowKeyStatus;
    }

    /**
     * Validate all cells of this row, even if it has null value
     * 
     * @param key
     *            : the row key
     */
    public void validateRow(String key)
    {
        RowInfo row = rowsMap.get(key);
        validateRow(row);
    }

    public void validateRow(RowInfo row)
    {
        row.cleanStatus();
        if (row instanceof RowInfoLeaf)
        {
            // string or array item
            RowInfoLeaf leaf = (RowInfoLeaf) row;
            if (leaf.getParent() == null)
            {
                // string => validate key
                validateRowKey(row.getKey());
            }
            for (ColumnInfo column : columns)
            {
                CellInfo cell = leaf.getCells().get(column.getId());
                IStatus cellStatus =
                        validator.isCellValid(column.getId(), row.getKey(),
                                cell != null ? cell.getValue() : null);
                if (!cellStatus.isOK())
                {
                    row.addStatus(cellStatus);
                }
            }
            // array item => update cell status
            if (leaf.getParent() != null)
            {
                IStatus parentStatus = leaf.getParent().getStatus();
                // update parent status with child status
                if (leaf.getStatus().getSeverity() > parentStatus.getSeverity())
                {
                    parentStatus = leaf.getStatus();
                    leaf.getParent().addStatus(parentStatus);
                }
            }

        }
        else
        {
            // array => validate key
            validateRowKey(row.getKey());
            IStatus cellStatus = null;
            // array: validate each child
            List<RowInfoLeaf> children = row.getChildren();
            for (RowInfoLeaf child : children)
            {
                child.cleanStatus();
                for (ColumnInfo column : columns)
                {
                    CellInfo cell = child.getCells().get(column.getId());
                    cellStatus =
                            validator.isCellValid(column.getId(), row.getKey(),
                                    cell != null ? cell.getValue() : null);
                    if (!cellStatus.isOK())
                    {
                        child.addStatus(cellStatus);
                    }
                }
                // update parent status with child status
                IStatus parentStatus = row.getStatus();
                if (child.getStatus().getSeverity() > parentStatus.getSeverity())
                {
                    parentStatus = child.getStatus();
                    row.addStatus(parentStatus);
                }
            }
        }
    }

    public IStatus getStatus()
    {
        MultiStatus status = null;
        for (RowInfo row : rowsMap.values())
        {
            if (!row.getStatus().isOK())
            {
                if (status == null)
                {
                    status = new MultiStatus(StringEditorPlugin.PLUGIN_ID, 0, null, null);
                }
                status.merge(row.getStatus());
            }
        }
        return status != null ? status : Status.OK_STATUS;
    }

    public CellInfo getCellInfo(String columnID, String key, int indexKey)
    {
        CellInfo theCell = null;
        ColumnInfo theColumn = columnsMap.get(columnID);
        if (theColumn != null)
        {
            CellInfo candidateCell = theColumn.getCell(key);
            if (candidateCell != null)
            {
                if (indexKey >= 0)
                {
                    theCell = candidateCell.getChildren().get(indexKey);
                }
                else
                {
                    theCell = candidateCell;
                }
            }
        }
        return theCell;
    }

}
