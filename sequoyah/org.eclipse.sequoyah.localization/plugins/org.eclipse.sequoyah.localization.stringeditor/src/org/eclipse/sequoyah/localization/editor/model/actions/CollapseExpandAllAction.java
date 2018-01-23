/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * <name> (<company>) - Bug [<bugid>] - <description>
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.sequoyah.localization.editor.StringEditorPlugin;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CollapseExpandAllAction extends Action {

	private final boolean expanded;

	private final StringEditorPart stringEditorPart;

	public CollapseExpandAllAction(StringEditorPart stringEditorPart,
			boolean expanded, String text, String description) {
		super(text);
		this.setDescription(description);
		this.expanded = expanded;
		this.stringEditorPart = stringEditorPart;
		setImageDescriptor(expanded ? StringEditorPlugin
				.imageDescriptorFromPlugin(StringEditorPlugin.PLUGIN_ID,
						"icons/expand_all.png") : PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
	}

	@Override
	public void run() {
		for (RowInfo row : stringEditorPart.getModel().getRows().values()) {
			if (row.getChildren().size() > 0) {
				stringEditorPart.getEditorViewer().setExpandedState(row,
						expanded);
			}
		}
	}
}
