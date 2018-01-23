/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Drigo Pastore (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [326793] - Fix execute/redo to avoid recreate row objects
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of adding a new key (row) to the editor.
 */
public class AddArrayItemsOperation extends EditorOperation {
	private List<AddArrayItemOperation> addArrayItemsOperation = new ArrayList<AddArrayItemOperation>();

	public AddArrayItemsOperation(String label, StringEditorPart editor,
			RowInfo[] rows, int quantity) {
		super(label, editor);
		for (int i = 0; i < rows.length; i++) {
			this.addArrayItemsOperation.add(i, new AddArrayItemOperation(label,
					editor, rows[i], false));
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
		for (int i = 0; i < this.addArrayItemsOperation.size(); i++) {
			this.addArrayItemsOperation.get(i).execute(monitor, info);
		}
		getEditor().refresh();
		return Status.OK_STATUS;
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

		for (int i = 0; i < this.addArrayItemsOperation.size(); i++) {
			this.addArrayItemsOperation.get(i).redo(monitor, info);
		}
		getEditor().refresh();
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
		for (int i = this.addArrayItemsOperation.size(); i > 0; i--) {
			this.addArrayItemsOperation.get(i - 1).undo(monitor, info);
		}
		getEditor().refresh();
		return Status.OK_STATUS;
	}

}
