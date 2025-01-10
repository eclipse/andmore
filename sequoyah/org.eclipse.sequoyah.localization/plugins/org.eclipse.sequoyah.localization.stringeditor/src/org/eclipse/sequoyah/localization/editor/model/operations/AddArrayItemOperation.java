/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabricio Nallin Violin (Eldorado) - Bug [326793] - Initial Version
 * Matheus Lima (Eldorado) - Bug [326793] - Expanding the array in which the string is expanded
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

public class AddArrayItemOperation extends EditorOperation {
	private final RowInfo row;
	private int index = 0;
	private RowInfoLeaf item;
	private boolean wasExpanded = false;
	private boolean doRefresh = true;

	public AddArrayItemOperation(String label, StringEditorPart editor,
			RowInfo row) {
		super(label, editor);
		this.row = row;
		// this.index = row.getChildren().size();
	}
	
	/**
	 * 
	 * @param label
	 * @param editor
	 * @param row
	 * @param doRefresh set to false when adding multiple array items.
	 */
	public AddArrayItemOperation(String label, StringEditorPart editor,
            RowInfo row, boolean doRefresh) {
	    this(label, editor, row);
        this.doRefresh = doRefresh;
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
		RowInfo existingRowArray = getModel().getRow(row.getKey());
		index = existingRowArray.getChildren().size();
		item = new RowInfoLeaf(existingRowArray.getKey(), existingRowArray,
				index, null);

		wasExpanded = getEditor().getEditorViewer().getExpandedState(
				item.getParent());
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

		if (!wasExpanded) {
			getEditor().getEditorViewer().expandToLevel(item,
					AbstractTreeViewer.ALL_LEVELS);
		}
		getEditor().addRow(item);
		
		if(doRefresh)
		{
		    getEditor().refresh();
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

		getEditor().removeRow(row.getKey(), index);
		if (!wasExpanded) {
			getEditor().getEditorViewer().collapseToLevel(item.getParent(),
					AbstractTreeViewer.ALL_LEVELS);
		}
		
		if(doRefresh)
        {
            getEditor().refresh();
        }
		
		return Status.OK_STATUS;
	}
}
