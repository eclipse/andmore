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
 *  * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of removing a column from the editor.
 */
public class RemoveColumnOperation extends EditorOperation {

	private final ColumnInfo column;

	private final int columnIndex, columnWidth;

	boolean changedColumn = false;

	/**
	 * Creates a RemoveColumnOperation.
	 * 
	 * @param label
	 *            - The label for this column.
	 * @param editor
	 *            - The editor object.
	 * @param column
	 *            - The column information.
	 * @param tableColumn
	 *            - The column object.
	 */
	public RemoveColumnOperation(String label, StringEditorPart editor,
			ColumnInfo column, int columnIndex, int columnWidth) {
		super(label, editor);
		this.column = column;
		this.columnIndex = columnIndex;
		this.columnWidth = columnWidth;
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

		getEditor().removeColumn(column.getId());
		changedColumn = getEditor().unmarkColumnAsChanged(column.getId());
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

		getEditor().addColumn(column, columnIndex).setWidth(columnWidth);
		if (changedColumn) {
			getEditor().markColumnAsChanged(column.getId());
		}
		return Status.OK_STATUS;
	}

}
