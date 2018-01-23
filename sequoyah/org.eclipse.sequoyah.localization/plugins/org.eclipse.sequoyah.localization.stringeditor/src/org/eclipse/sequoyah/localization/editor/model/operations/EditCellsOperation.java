/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcel Gorri (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of editing a specific editor cell.
 */
public class EditCellsOperation extends EditorOperation {

	private List<EditCellOperation> editCellOperations = new ArrayList<EditCellOperation>();

	/**
	 * Creates a new EditCellOperation.
	 * 
	 * @param key
	 *            - the key related to the cell.
	 * @param column
	 *            - the column related to the cell.
	 * @param oldValue
	 *            - the cell old value.
	 * @param newValue
	 *            - the cell new value.
	 * @param editor
	 *            - the editor Object.
	 * @param infors
	 *            - rows associated to each cell
	 */
	public EditCellsOperation(String label, String[] key, String[] column,
			CellInfo[] oldValue, CellInfo[] newValue, StringEditorPart editor,
			Object[] infos) {
		super(label, editor);
		for (int i = 0; i < key.length; i++) {
			this.editCellOperations.add(new EditCellOperation(key[i],
					column[i], oldValue[i], newValue[i], editor,
					(RowInfo) infos[i]));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#execute(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
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
		for (EditCellOperation cellOperation : this.editCellOperations) {
			cellOperation.redo(monitor, info);
		}
		return Status.OK_STATUS;
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

		int size = this.editCellOperations.size();

		for (int i = size; i > 0; i--) {
			EditCellOperation cellOperation = this.editCellOperations
					.get(i - 1);
			cellOperation.undo(monitor, info);
		}
		return Status.OK_STATUS;

	}

}
