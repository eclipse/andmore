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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * Adds a multiplicity menu to a given action
 * 
 * @author kpqb38
 * 
 */
public class MenuDropDownAction extends Action implements IMenuCreator {
	private Menu fMenu;
	Action action = null;

	public MenuDropDownAction(Action action, ImageDescriptor imgDesc) {
		this.setImageDescriptor(imgDesc);
		setMenuCreator(this);
		this.action = action;
		if (action instanceof AddArrayItemAction)
			this.setEnabled(isEnabled());
		else
			this.setEnabled(true);
		this.setToolTipText(action.getDescription());
	}

	@Override
	public boolean isEnabled() {
		return action.isEnabled();
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		// First menu item: single action
		ActionContributionItem singleEntry = new ActionContributionItem(action);
		singleEntry.fill(fMenu, -1);
		// Second menu item: multiple action
		ActionContributionItem multipleEntries = new ActionContributionItem(
				new AddMultipleItemsAction(parent, action));
		multipleEntries.fill(fMenu, -1);

		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	// This is called when the button is clicked
	public void run() {
		if (action instanceof AddSingleStringAction)
			((AddSingleStringAction) action).setQuantity(1);
		else if (action instanceof AddArrayAction)
			((AddArrayAction) action).setQuantity(1);
		else
			((AddArrayItemAction) action).setQuantity(1);
		action.run();
	}
}