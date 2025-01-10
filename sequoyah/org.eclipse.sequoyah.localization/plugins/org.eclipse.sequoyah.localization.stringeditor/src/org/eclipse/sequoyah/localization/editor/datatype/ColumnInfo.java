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
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.datatype;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a column of the editor
 */
public class ColumnInfo {

	/*
	 * This column id
	 */
	private String id;

	/*
	 * The column tooltip
	 */
	private String tooltip;

	/*
	 * The cells of this column where the key is the row key
	 */
	private final Map<String, CellInfo> cells;

	/*
	 * Can remove this column?
	 */
	private final boolean canRemove;

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param tooltip
	 * @param cells
	 *            initial cells or null
	 */
	public ColumnInfo(String id, String tooltip, Map<String, CellInfo> cells,
			boolean canRemove) {

		this.id = id;
		this.tooltip = tooltip;
		this.cells = cells != null ? cells : new HashMap<String, CellInfo>();
		this.canRemove = canRemove;
	}

	/**
	 * Get the column id
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * get this column tooltip
	 * 
	 * @return
	 */
	public String getTooltip() {
		return tooltip;
	}

	/**
	 * Get the cells
	 * 
	 * @return a Map with keys and the cellinfo object
	 */
	public Map<String, CellInfo> getCells() {
		return cells;
	}

	/**
	 * Set this column ID
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Set this column tooltip
	 * 
	 * @param tooltip
	 */
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * Add a new cell to this column
	 * 
	 * @param key
	 * @param value
	 */
	public void addCell(String key, CellInfo value) {
		cells.put(key, value);
	}

	/**
	 * remove the cell with the following key
	 * 
	 * @param key
	 */
	public void removeCell(String key) {
		cells.remove(key);
	}

	/**
	 * get the cell with the following key
	 * 
	 * @param key
	 */
	public CellInfo getCell(String key) {
		return cells.get(key);
	}

	/**
	 * Change a key name to another value
	 * 
	 * @param old
	 * @param newKey
	 */
	public void renameKey(String oldKey, String newKey) {
		CellInfo theCell = getCell(oldKey);
		removeCell(oldKey);
		addCell(newKey, theCell);
	}

	/**
	 * Check if we can remove this column
	 * 
	 * @return
	 */
	public boolean canRemove() {
		return canRemove;
	}

	@Override
	public String toString() {
		return "ColumnInfo [id=" + id + ", cells=" + cells + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
