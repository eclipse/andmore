/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Lima (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of cloning entire column.
 */
public class CloneOperation extends EditorOperation {

	private final String sourceColumnID;

	private final ColumnInfo destinationColumn;

	public CloneOperation(StringEditorPart editor, String sourceColumnID,
			ColumnInfo newColumnInfo) {
		super(Messages.CloneOperation_0, editor);
		this.sourceColumnID = sourceColumnID;
		this.destinationColumn = newColumnInfo;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		ColumnInfo source = getModel().getColumn(sourceColumnID);
		for (String cellKey : source.getCells().keySet()) {
			CellInfo cell = source.getCells().get(cellKey);
			if (cell != null) {
				destinationColumn.addCell(cellKey, new CellInfo(cell));
			}
		}
		return redo(monitor, info);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		getEditor().addColumn(destinationColumn, -1);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		getEditor().removeColumn(destinationColumn.getId());

		return Status.OK_STATUS;
	}

}
