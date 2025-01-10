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
 * Matheus Lima (Eldorado) - Bug [326793] -  Fixed action description
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.sequoyah.localization.editor.StringEditorPlugin;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.AddKeyOperation;
import org.eclipse.sequoyah.localization.editor.model.operations.AddKeysOperation;
import org.eclipse.ui.forms.IMessage;

/**
 * Action to add a new key (single string)
 */
public class AddSingleStringAction extends Action {
	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;
	int quantity = 1;
	public static final String ID = "String"; //$NON-NLS-1$

	public AddSingleStringAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_AddSingleStringActionName);
		this.stringEditorPart = stringEditorPart;
		setImageDescriptor(StringEditorPlugin.imageDescriptorFromPlugin(
				StringEditorPlugin.PLUGIN_ID, "icons/string.png")); //$NON-NLS-1$
		setId(ID);
		setDescription(Messages.AddSingleStringAction_DescriptionPrefix + ID);
	}

	public void setQuantity(int i) {
		quantity = i;
	}

	@Override
	public void run() {
		RowInfo[] rowInfo = new RowInfo[quantity];
		rowInfo = stringEditorPart.getContentProvider().getOperationProvider()
				.getNewSingleRow(quantity);

		// add new key only if the key isn't null and the new key does not
		// exists
		if (rowInfo != null) {
			if (rowInfo.length > 1) {
				AddKeysOperation operation = new AddKeysOperation(
						Messages.StringEditorPart_AddSingleStringOperationName,
						stringEditorPart, rowInfo);
				operation.addContext(stringEditorPart.getUndoContext());
				stringEditorPart.executeOperation(operation);
			} else {
				String singleKey = rowInfo[0].getKey();
				if (rowInfo[0] instanceof RowInfoLeaf) {// string or array
														// item?
					if (((RowInfoLeaf) rowInfo[0]).getParent() == null) {// string
						if (stringEditorPart.getModel().getRow(singleKey) == null) {// key
							// does
							// not
							// exist
							AddKeyOperation operation = new AddKeyOperation(
									Messages.StringEditorPart_AddSingleStringOperationName,
									stringEditorPart, rowInfo[0]);
							operation.addContext(stringEditorPart
									.getUndoContext());
							stringEditorPart.executeOperation(operation);
						} else { // key already exists
							stringEditorPart
									.setMessage(
											Messages.bind(
													Messages.StringEditorPart_KeyAlreadyExistsErrorMessage,
													singleKey), IMessage.ERROR);
						}
					}
				}
			}
		}
		// LFE new workflow: Start editing row key
		int columnIndex = 0; // key index
		stringEditorPart.getEditorViewer().editElement(rowInfo[0], columnIndex);
		stringEditorPart.refreshButtonsEnabled();
		quantity = 1;
	}
}