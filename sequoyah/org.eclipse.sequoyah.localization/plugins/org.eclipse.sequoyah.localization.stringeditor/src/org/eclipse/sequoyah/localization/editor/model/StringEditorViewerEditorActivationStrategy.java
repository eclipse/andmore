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
package org.eclipse.sequoyah.localization.editor.model;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.swt.SWT;

class StringEditorViewerEditorActivationStrategy extends
		ColumnViewerEditorActivationStrategy {
	StringEditorViewerEditorActivationStrategy(ColumnViewer viewer) {
		super(viewer);
	}

	@Override
	protected boolean isEditorActivationEvent(
			ColumnViewerEditorActivationEvent event) {
		boolean activate = false;
		if ((event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED)
				&& (event.stateMask == 0)) {
			if ((event.character >= 32) && (event.character <= 127)) {
				activate = true;
			} else if ((event.keyCode == SWT.CR) || (event.keyCode == SWT.DEL)
					|| (event.keyCode == SWT.KEYPAD_CR)) {
				activate = true;
			} else {
				activate = super.isEditorActivationEvent(event);
			}
		} else if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) {
			activate = true;
		} else if (event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC) {
			// LFE new workflow: when button to add string, array or
			// array item were clicked
			activate = true;
		}
		return activate;
	}
}