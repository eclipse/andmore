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
 * Marcel Gorri (Eldorado) - Bug [326793] - Improvements on the string arrays handling
 * Paulo Faria (Eldorado) - Bug [326793] -  Fixing undo/redo edit for array items
 * Matheus Lima (Eldorado) - Bug [326793] -  Fixed translation of strings
 * Daniel Drigo Pastore (Eldorado) - Bug [326793] - Fixing array image according to array items status
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of editing a specific editor cell.
 */
public class EditCellOperation extends EditorOperation {

	private final String key;

	private final String column;

	private final CellInfo oldValue;

	private final CellInfo newValue;

	private final RowInfo rowInfo;

	/**
	 * Creates a new EditCellOperation.
	 * 
	 * @param info
	 *            - the row related to the cell.
	 * @param column
	 *            - the column related to the cell.
	 * @param oldValue
	 *            - the cell old value.
	 * @param newValue
	 *            - the cell new value.
	 * @param editor
	 *            - the editor Object.
	 * @param rowInfo
	 *            - row info associated to the cell
	 * 
	 */
	public EditCellOperation(String key, String column, CellInfo oldValue,
			CellInfo newValue, StringEditorPart editor, RowInfo info) {
		super(Messages.EditCellOperation_0, editor);
		this.key = key;
		this.column = column;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.rowInfo = info;
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

		if (newValue != null && newValue.getPosition() >= 0) {
			getModel().addCell(newValue, key, column, newValue.getPosition(),
					true);
		} else {

			getModel().addCell(newValue, key, column);

		}

		if (newValue != null && !newValue.isDirty()) {
			newValue.setDirty(true);
		}
		try {
			if (newValue == null
					|| (newValue != null && newValue.getValue() == null)) {
				if (newValue.getPosition() >= 0) {
					getEditorInput().removeCell(key, column,
							newValue.getPosition());
				} else {
					getEditorInput().removeCell(key, column);
				}
			} else {
				if (rowInfo instanceof RowInfoLeaf) {
					RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
					if (leaf.getParent() == null) {
						// single string
						getEditorInput().setValue(column, key,
								newValue.getValue());
					} else {
						// array item
						// return value from cell to the original
						getEditorInput().setValue(column, key,
								newValue.getValue(), leaf.getPosition());
					}
				} else {
					// array
					getEditorInput().setValue(column, key, newValue.getValue());
				}
			}
		} catch (SequoyahException e) {
			BasePlugin.logError("Error editing cell value: (" + column //$NON-NLS-1$
					+ ", " + key //$NON-NLS-1$
					+ ") =" + newValue != null ? newValue //$NON-NLS-1$
					.getValue() : null, e);
		}
		getEditor().fireDirtyPropertyChanged();
		getEditor().getEditorViewer().update(this.rowInfo, null);
		if (rowInfo instanceof RowInfoLeaf
				&& ((RowInfoLeaf) rowInfo).getParent() != null) {
			getEditor().getEditorViewer().update(
					((RowInfoLeaf) rowInfo).getParent(), null);
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

		if (getModel().getRow(rowInfo.getKey()) == null) {
			if (rowInfo instanceof RowInfoLeaf) {
				getModel().addRow(((RowInfoLeaf) rowInfo).getParent());
			}
			getModel().addRow(rowInfo);
			getEditor().refresh();
		}
		if ((oldValue != null && oldValue.getPosition() >= 0)
				|| (newValue != null && newValue.getPosition() >= 0)) {
			getModel().addCell(
					oldValue,
					key,
					column, /*
							 * get position from oldValue first, than newValue.
							 * If none, -1
							 */
					oldValue != null ? oldValue.getPosition()
							: newValue != null ? newValue.getPosition() : -1,
					true);
		} else {

			getModel().addCell(oldValue, key, column);

		}
		
		if (oldValue != null && !oldValue.isDirty()) {
		    oldValue.setDirty(true);
        }
		try {
			if (oldValue != null && oldValue.getValue() != null) {
				// there is old value
				if (rowInfo instanceof RowInfoLeaf) {
					RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
					if (leaf.getParent() == null) {
						// string
						getEditorInput().setValue(column, key,
								oldValue.getValue());
					} else {
						// array item
						getEditorInput().setValue(column, key,
								oldValue.getValue(), leaf.getPosition());
					}

				} else {
					// array
					getEditorInput().setValue(column, key, oldValue.getValue());
				}
			} else {
				// there is no old value => remove cell
				if (rowInfo instanceof RowInfoLeaf) {
					RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
					if (leaf.getParent() == null) {
						// string
						getEditorInput().removeCell(key, column);
					} else {
						// array item
						getEditorInput().removeCell(key, column,
								leaf.getPosition());
					}
				} else {
					// array
					getEditorInput().removeCell(key, column);
				}
			}
		} catch (SequoyahException e) {
			BasePlugin.logError("Error undoing cell edition: (" + column //$NON-NLS-1$
					+ ", " + key //$NON-NLS-1$
					+ ") =" + oldValue != null ? oldValue //$NON-NLS-1$
					.getValue() : null, e);
		}
		getEditor().fireDirtyPropertyChanged();
		getEditor().getEditorViewer().update(this.rowInfo, null);
		// update parent
		if (rowInfo instanceof RowInfoLeaf && ((RowInfoLeaf)rowInfo).getParent() != null) {
			getEditor().getEditorViewer().update(((RowInfoLeaf)rowInfo).getParent(), null);
		}
		return Status.OK_STATUS;
	}

}
