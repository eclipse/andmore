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
 *  * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

public class RevertColumnToSavedStateOperation extends EditorOperation {

	private final ColumnInfo actualState;

	private ColumnInfo savedState = null;

	boolean changedColumn = false;

	public RevertColumnToSavedStateOperation(String label,
			StringEditorPart editor, ColumnInfo actual) {
		super(label, editor);
		this.actualState = actual;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		try {
			getEditorInput().revert(actualState.getId());
		} catch (IOException e) {
			BasePlugin.logError("Error reverting column: " //$NON-NLS-1$
					+ actualState.getId(), e);
		}
		savedState = new ColumnInfo(actualState.getId(),
				actualState.getTooltip(), getEditorInput().getValues(
						actualState.getId()), actualState.canRemove());
		return redo(monitor, info);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		getModel().removeColumnForRevertion(actualState.getId());
		getModel().addColumn(savedState);
		changedColumn = getEditor().unmarkColumnAsChanged(actualState.getId());
		getEditor().getEditorViewer().refresh();
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		getModel().removeColumn(savedState.getId());
		getModel().addColumn(actualState);
		if (changedColumn) {
			getEditor().markColumnAsChanged(actualState.getId());
		}
		getEditor().getEditorViewer().refresh();
		return Status.OK_STATUS;
	}

}
