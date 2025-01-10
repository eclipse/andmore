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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.AddColumnOperation;

/**
 * Action to add a new column
 */
public class AddColumnAction extends Action {

	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	public AddColumnAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_AddColumnActionName);
		this.stringEditorPart = stringEditorPart;

	}

	@Override
	public void run() {
		ColumnInfo info = this.stringEditorPart.getContentProvider()
				.getOperationProvider().getNewColumn();

		if (info != null) {
			if (this.stringEditorPart.getColumnByID(info.getId()) == null) {
				AddColumnOperation operation = new AddColumnOperation(
						Messages.StringEditorPart_AddColumnOperationName,
						this.stringEditorPart, info);
				operation.addContext(this.stringEditorPart.getUndoContext());
				this.stringEditorPart.executeOperation(operation);

			} else {
				MessageDialog
						.openInformation(
								this.stringEditorPart.getEditorSite()
										.getShell(),
								Messages.StringEditorPart_ColumnAlreadyExistTitle,
								NLS.bind(
										Messages.StringEditorPart_ColumnAlreadyExistMessage,
										info.getId()));
			}
		}
	}
}