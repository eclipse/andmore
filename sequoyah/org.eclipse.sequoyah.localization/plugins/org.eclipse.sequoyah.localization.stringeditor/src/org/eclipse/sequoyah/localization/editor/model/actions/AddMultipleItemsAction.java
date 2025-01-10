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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.swt.widgets.Control;

/**
 * Action that repeats a given action
 * 
 * @author kpqb38
 * 
 */
public class AddMultipleItemsAction extends Action {
	int quantity = 1;
	Control parent = null;
	Action action = null;
	private String title = Messages.StringEditorPart_2;

	public AddMultipleItemsAction(Control parent, Action action) {
		super(Messages.StringEditorPart_1 + action.getId());

		if (action.getId().equals(AddArrayAction.ID)) {
			title = Messages.StringEditorPart_1
					+ Messages.AddMultipleItemsAction_2; //$NON-NLS-1$			
			setText(title);
		} else if (action.getId().equals(AddArrayItemAction.ID)) {
			title = Messages.StringEditorPart_1
					+ Messages.AddMultipleItemsAction_4; //$NON-NLS-1$
			setText(title);
		} else if (action.getId().equals(AddSingleStringAction.ID)) {
			title = Messages.StringEditorPart_1
					+ Messages.AddMultipleItemsAction_6; //$NON-NLS-1$
			setText(title);
		}

		this.parent = parent;
		this.action = action;
	}

	@Override
	public void run() {
		// Creates a dialog to ask how many items to create
		String dialogTitle = title;
		String dialogMessage = Messages.StringEditorPart_3
				+ action.getId().toLowerCase()
				+ Messages.AddMultipleItemsAction_0
				+ Messages.StringEditorPart_5;
		String initialValue = Messages.StringEditorPart_4;
		InputDialog dialog = new InputDialog(parent.getShell(), dialogTitle,
				dialogMessage, initialValue, new IInputValidator() {
					// Validation: only positive integer greater than zero
					public String isValid(String newText) {
						String errorMessage = null;
						try {
							int items = Integer.parseInt(newText);
							if (items < 0)
								errorMessage = Messages.StringEditorPart_6;
						} catch (Exception e) {
							errorMessage = Messages.StringEditorPart_6;
						}
						return errorMessage;
					}
				});
		if (dialog.open() == Window.OK) {
			setQuantity(Integer.parseInt(dialog.getValue()));
			if (action instanceof AddSingleStringAction)
				((AddSingleStringAction) action).setQuantity(quantity);
			else if (action instanceof AddArrayAction)
				((AddArrayAction) action).setQuantity(quantity);
			else
				((AddArrayItemAction) action).setQuantity(quantity);
			action.run();
		}
	}

	public void setQuantity(int i) {
		quantity = i;
	}
}