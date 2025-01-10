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

import java.util.List;

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
 * Action to add a new key (array)
 */
public class AddArrayAction extends Action {
	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;
	int quantity = 1;
	public static final String ID = "Array"; //$NON-NLS-1$

	public AddArrayAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_AddArrayActionName);
		this.stringEditorPart = stringEditorPart;
		setImageDescriptor(StringEditorPlugin.imageDescriptorFromPlugin(
				StringEditorPlugin.PLUGIN_ID, "icons/string_array.png")); //$NON-NLS-1$
		setId(ID);
		setDescription(Messages.AddArrayAction_DescriptionPrefix + ID);
	}

	public void setQuantity(int i) {
		quantity = i;
	}

	@Override
	public void run() {
		RowInfo[] rowInfo = new RowInfo[quantity];
		rowInfo = stringEditorPart.getContentProvider().getOperationProvider()
				.getNewArrayRow(quantity);

		// add new key only if the key isn't null and the new key does not
		// exists
		if (rowInfo != null) {
			if (rowInfo.length > 1) {
				AddKeysOperation operation = new AddKeysOperation(
						Messages.StringEditorPart_AddArrayOperationName,
						stringEditorPart, rowInfo);
				operation.addContext(stringEditorPart.getUndoContext());
				stringEditorPart.executeOperation(operation);
			} else {
				String arrayKey = rowInfo[0].getKey();
				if (rowInfo[0] instanceof RowInfo) {// array
					if (stringEditorPart.getModel().getRow(arrayKey) == null) {
						// new array
						List<RowInfoLeaf> children = rowInfo[0].getChildren();
						// if (children.isEmpty()) {//empty array
						for (int i = 0; i < children.size(); i++) {
							if (stringEditorPart.getModel().getRow(
									children.get(i).getCells().toString()) == null) {// child
																						// does
																						// not
																						// exist
								AddKeyOperation operation = new AddKeyOperation(
										Messages.StringEditorPart_AddArrayOperationName,
										stringEditorPart, rowInfo[0]);
								operation.addContext(stringEditorPart
										.getUndoContext());
								stringEditorPart.executeOperation(operation);
							} else { // key already exists
								stringEditorPart
										.setMessage(
												Messages.bind(
														Messages.StringEditorPart_KeyAlreadyExistsErrorMessage,
														arrayKey),
												IMessage.ERROR);

							}
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