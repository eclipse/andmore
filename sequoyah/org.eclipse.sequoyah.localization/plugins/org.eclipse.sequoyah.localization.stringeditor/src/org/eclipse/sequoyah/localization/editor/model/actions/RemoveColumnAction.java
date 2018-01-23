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
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.StringEditorViewerModel;
import org.eclipse.sequoyah.localization.editor.model.operations.RemoveColumnOperation;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Action to remove a column
 */
public class RemoveColumnAction extends Action {

	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	public RemoveColumnAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_RemoveColumnActionName);
		this.stringEditorPart = stringEditorPart;
		String activeColumnID = stringEditorPart.getEditorViewer().getTree()
				.getColumn(stringEditorPart.getActiveColumn()).getText();
		setEnabled((stringEditorPart.getActiveColumn() != 0)
				&& ((stringEditorPart.getModel().getColumn(activeColumnID) != null) && stringEditorPart
						.getModel().getColumn(activeColumnID).canRemove()));
	}

	@Override
	public void run() {
		TreeColumn selectedColumn = stringEditorPart.getEditorViewer()
				.getTree().getColumn(stringEditorPart.getActiveColumn());
		if (!selectedColumn.getText()
				.equals(Messages.StringEditorPart_KeyLabel)) {
			if (MessageDialog.openQuestion(stringEditorPart.getEditorSite()
					.getShell(),
					Messages.StringEditorPart_RemoveColumnActionName,
					Messages.StringEditorPart_RemoveColumnQuestionMessage
							+ " \"" + selectedColumn.getText() + "\"?")) { //$NON-NLS-1$ //$NON-NLS-2$
				RemoveColumnOperation operation = new RemoveColumnOperation(
						Messages.StringEditorPart_RemoveColumnOperationName,
						stringEditorPart,
						((StringEditorViewerModel) stringEditorPart
								.getEditorViewer().getInput())
								.getColumn(selectedColumn.getText()),
						stringEditorPart.getEditorViewer().getTree()
								.indexOf(selectedColumn),
						selectedColumn.getWidth());
				operation.addContext(stringEditorPart.getUndoContext());
				stringEditorPart.executeOperation(operation);
			}
		}
	}
}