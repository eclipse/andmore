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
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * An action to show/hide columns
 */
public class HideShowColumnAction extends Action {
	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;
	private final TreeColumn column;

	public HideShowColumnAction(StringEditorPart stringEditorPart, String name,
			int style, TreeColumn c) {
		super(name, style);
		this.stringEditorPart = stringEditorPart;
		column = c;
		setChecked(c.getWidth() > 0);
	}

	@Override
	public void run() {
		if (!isChecked()) {
			this.stringEditorPart.hideColumn(column);
		} else {
			this.stringEditorPart.showColumn(column);
		}
	}
}