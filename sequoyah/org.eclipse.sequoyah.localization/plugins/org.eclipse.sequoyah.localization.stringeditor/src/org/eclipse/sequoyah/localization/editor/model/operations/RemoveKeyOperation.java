/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of removing a key from the editor.
 */
public class RemoveKeyOperation extends EditorOperation {

	private final List<RowInfo> rows;

	/**
	 * Creates a RemoveKeyOperation.
	 * 
	 * @param label
	 *            - the label for this operation.
	 * @param editor
	 *            - the editor object.
	 * @param row
	 *            - The RowInfo object that identifies the key to be removed.
	 */
	public RemoveKeyOperation(String label, StringEditorPart editor,
			List<RowInfo> rows) {
		super(label, editor);

		this.rows = new ArrayList<RowInfo>();

		// only array items and string nodes are added to the list of rows
		// if an array header is selected, all of its children will be added

		for (RowInfo info : rows) {
			if (info instanceof RowInfoLeaf) {
				if (!this.rows.contains(info)) {
					this.rows.add(info);
				}
			} else {
				for (RowInfoLeaf leaf : info.getChildren()) {
					if (!this.rows.contains(leaf)) {
						this.rows.add(leaf);
					}
				}
			}
		}
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		return redo(monitor, info);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#redo(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		/*
		 * Remove all selected items
		 * 
		 * Put all array items of some array together to remove in reverse order
		 * due to indexing problems.
		 */
		Map<RowInfo, TreeSet<RowInfoLeaf>> mapForRemovingArrayItems = new HashMap<RowInfo, TreeSet<RowInfoLeaf>>();

		for (RowInfo row : rows) {
			if (row instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) row;
				if (leaf.getParent() == null) {
					// string
					getEditor().removeRow(row.getKey());
				} else {
					// array item - accumulate and remove all at once to avoid
					// index problems
					TreeSet<RowInfoLeaf> childrenList = mapForRemovingArrayItems
							.get(leaf.getParent());
					if (childrenList == null) {
						childrenList = new TreeSet<RowInfoLeaf>(
								new LeavesInvertedPositionComparator());
						mapForRemovingArrayItems.put(leaf.getParent(),
								childrenList);
					}
					childrenList.add(leaf);
				}
			} else {
				// array
				getEditor().removeRow(row.getKey());
			}
		}

		if (!mapForRemovingArrayItems.isEmpty()) {
			// remove array items that were accumulated
			for (TreeSet<RowInfoLeaf> childrenList : mapForRemovingArrayItems
					.values()) {
				for (RowInfoLeaf leaf : childrenList) {
					getEditor().removeRow(leaf.getKey(), leaf.getPosition());
				}
			}
		}

		getEditor().refresh();
		return Status.OK_STATUS;
	}

	/**
	 * Compares two leaves by their position in decreasing order. This applies
	 * to leaves who have the same parent. No checking is made to make sure they
	 * have the same parent, the only checking is about the position not being
	 * null.
	 * 
	 * This is meant to be used by the redo() method of this class only.
	 */
	private class LeavesInvertedPositionComparator implements
			Comparator<RowInfoLeaf> {

		public int compare(RowInfoLeaf o1, RowInfoLeaf o2) {
			int compareValue = 0;
			// if either object return null for getPosition(), results are
			// unexpected anyway, so this test will only pass (test only to
			// prevent from crashing)
			if (o1.getPosition() != null && o2.getPosition() != null) {
				compareValue = o1.getPosition().compareTo(o2.getPosition())
						* -1; // multiply by -1 to invert order
			}
			return compareValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#undo(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		for (RowInfo row : rows) {
			getEditor().addRow(row);
		}
		getEditor().refresh();
		return Status.OK_STATUS;
	}

}
