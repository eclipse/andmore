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
 * Paulo Faria (Eldorado) - Bug [326793] -  Fix issue to translate array item (update position from child being edited) 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.datatype;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Cell of the editor. It can include comments
 */
public class CellInfo {

	/*
	 * The cell value
	 */
	private String value;

	/*
	 * The cell comment (if any)
	 */
	private String comment;

	/*
	 * The dirt state of the cell
	 */
	private boolean dirty;

	private int position = -1;

	private final List<CellInfo> children;

	/**
	 * Get the value of this cell
	 * 
	 * @return the value of this cell
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set this cell value
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Get this cell comment
	 * 
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Set this cell comment
	 * 
	 * @param comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Create a new CellInfo object with a given name and comment
	 * 
	 * @param value
	 * @param comment
	 */
	public CellInfo(String value, String comment, int position) {
		this(false);
		this.value = value;
		this.comment = comment;
		this.dirty = false;
		this.position = position;
	}

	/**
	 * Create a new CellInfo object with a given name and comment
	 * 
	 * @param value
	 * @param comment
	 */
	public CellInfo(String value, String comment) {
		this(value, comment, -1);
	}

	public CellInfo(CellInfo baseCellInfo) {
		this(baseCellInfo.hasChildren());
		this.value = baseCellInfo.getValue();
		this.comment = baseCellInfo.getComment();
		this.dirty = false;

		if (baseCellInfo.hasChildren()) {
			for (CellInfo child : baseCellInfo.getChildren()) {
				this.addChild(child);
			}
		}
	}

	/**
	 * Create a new cellinfo without children. That means that this cell will
	 * not be used as array head
	 * 
	 * @param hasChildren
	 */
	public CellInfo(boolean hasChildren) {
		if (hasChildren) {
			children = new ArrayList<CellInfo>();
		} else {
			children = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getValue() != null ? getValue() : ""; //$NON-NLS-1$
	}

	/*
	 * Set the dirty state of this cell
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isDirty() {
		return dirty;
	}

	public boolean hasChildren() {
		return children != null;
	}

	public List<CellInfo> getChildren() {
		return new ArrayList<CellInfo>(children);
	}

	public void clearChildren() {
		if (children != null) {
			children.clear();
		}
	}

	/**
	 * Add a new child at the position indicated by child.getPosition();
	 * 
	 * @param child
	 */
	public void addChild(CellInfo child) {
		ensurePosition(child);
		if (children != null) {
			children.add(child);
			child.setPosition(children.indexOf(child));
		}
	}

	/**
	 * Add a new child in desired index position
	 * 
	 * @param child
	 * @param index
	 * @param overwrite
	 *            true to replace the cell at index position, false to add it
	 *            and move all other cells
	 */
	public void addChild(CellInfo child, int index, boolean overwrite) {
		ensurePosition(overwrite ? index + 1 : index);
		if (children != null) {
			if (child != null) {
				child.setPosition(index);
			}
			if (!overwrite) {

				children.add(index, child);

				for (int i = index + 1; i < children.size(); i++) {
					children.get(i).setPosition(
							children.get(i).getPosition() + 1);
				}
			} else {
				children.set(index, child);
			}
		}
	}

	/**
	 * Ensure right array capacity and cell position
	 * 
	 * @param child
	 */
	private void ensurePosition(CellInfo child) {
		ensurePosition(child.getPosition());
	}

	private void ensurePosition(int index) {
		for (int i = children.size(); i < index; i++) {
			addChild(new CellInfo(null, null, i), i, false);
		}
	}

	public void removeChild(int index) {
		if (children != null && children.size() > index) {
			children.remove(index);
			for (int i = index; i < children.size(); i++) {
				children.get(i).setPosition(children.get(i).getPosition() - 1);
			}
		}
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}
}
