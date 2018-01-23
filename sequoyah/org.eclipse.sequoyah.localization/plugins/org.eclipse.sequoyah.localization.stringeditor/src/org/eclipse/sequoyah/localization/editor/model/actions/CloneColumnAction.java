/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * <name> (<company>) - Bug [<bugid>] - <bugDescription>
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.CloneOperation;
import org.eclipse.swt.widgets.TreeColumn;

public class CloneColumnAction extends Action {
	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	public CloneColumnAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_CloneActionName);
		this.stringEditorPart = stringEditorPart;
		setEnabled(stringEditorPart.getActiveColumn() != 0);
	}

	@Override
	public void run() {
		TreeColumn column = stringEditorPart.getEditorViewer().getTree()
				.getColumn(stringEditorPart.getActiveColumn());
		ColumnInfo newColumnInfo = stringEditorPart.getContentProvider()
				.getOperationProvider().getNewColumn();
		if (newColumnInfo != null) {
			CloneOperation operation = new CloneOperation(stringEditorPart,
					column.getText(), newColumnInfo);
			operation.addContext(stringEditorPart.getUndoContext());
			stringEditorPart.executeOperation(operation);
		}
	}
}