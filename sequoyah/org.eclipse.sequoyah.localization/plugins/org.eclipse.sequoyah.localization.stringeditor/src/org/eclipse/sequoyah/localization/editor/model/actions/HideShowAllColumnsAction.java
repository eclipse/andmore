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
import org.eclipse.swt.widgets.Tree;

/**
 * An action to show/hide columns
 */
public class HideShowAllColumnsAction extends Action {
	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	private final Tree table;

	private final boolean visible;

	public HideShowAllColumnsAction(StringEditorPart stringEditorPart,
			String name, int style, Tree t, boolean visible) {
		super(name, style);
		this.stringEditorPart = stringEditorPart;
		this.table = t;
		this.visible = visible;
	}

	@Override
	public void run() {
		for (int i = 1; i < table.getColumnCount(); i++) {
			if (visible) {
				this.stringEditorPart.showColumn(table.getColumn(i));
			} else {
				this.stringEditorPart.hideColumn(table.getColumn(i));
			}
		}
	}
}