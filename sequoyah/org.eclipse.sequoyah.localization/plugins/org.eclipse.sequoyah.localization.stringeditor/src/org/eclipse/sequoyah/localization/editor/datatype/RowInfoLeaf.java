/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Julia Martinez Perdigueiro (Eldorado)
 * 
 * Contributors:
 * Name (Company) - [Bug] - Description
 ********************************************************************************/

package org.eclipse.sequoyah.localization.editor.datatype;

import java.util.HashMap;
import java.util.Map;

public class RowInfoLeaf extends RowInfo {

	/*
	 * This row cells (key is columnId)
	 */
	private final Map<String, CellInfo> cells;

	private final RowInfo parent;

	private int position;

	public RowInfoLeaf(String key, RowInfo parent, int index,
			Map<String, CellInfo> cells) {
		super(key);
		this.parent = parent;
		this.cells = cells != null ? cells : new HashMap<String, CellInfo>();
		this.position = parent != null ? index : -1;
	}

	/**
	 * Add a cell to this row
	 * 
	 * @param columnID
	 * @param value
	 */
	public void addCell(String columnID, CellInfo value) {
		cells.put(columnID, value);
	}

	/**
	 * Remove a cell of this row.
	 * 
	 * @param columnID
	 */
	public void removeCell(String columnID) {
		cells.remove(columnID);

	}

	/**
	 * get this row cells
	 * 
	 * @return the cells
	 */
	public Map<String, CellInfo> getCells() {
		return cells;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public RowInfo getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "parent=" + (parent == null ? null : parent.getKey()) //$NON-NLS-1$
				+ "; position=" + position; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
