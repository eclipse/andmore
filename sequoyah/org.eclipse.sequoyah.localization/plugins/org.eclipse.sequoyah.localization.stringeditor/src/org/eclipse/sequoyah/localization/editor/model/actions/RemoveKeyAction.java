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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.RemoveKeyOperation;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Action to remove a key
 */
public class RemoveKeyAction extends Action {

	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	public RemoveKeyAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_RemoveKeyActionName);
		this.stringEditorPart = stringEditorPart;
		this.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		this.setEnabled(false);
	}

	@Override
	public boolean isEnabled() {
		boolean atLeastOneLineSelected = false;
		if ((this.stringEditorPart.getEditorViewer() != null)
				&& (this.stringEditorPart.getEditorViewer().getSelection() != null)) {
			StructuredSelection selection = (StructuredSelection) this.stringEditorPart
					.getEditorViewer().getSelection();
			final Object[] selectedRows = selection.toArray();
			atLeastOneLineSelected = (selectedRows != null && selectedRows.length > 0);
		}
		return atLeastOneLineSelected;
	}

	@Override
	public void run() {
		ISelection sel = this.stringEditorPart.getEditorViewer().getSelection();
		List<RowInfo> toBeDeleted = new ArrayList<RowInfo>();
		if ((sel != null) && (sel instanceof IStructuredSelection)) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			for (Object o : selection.toArray()) {
				if (o instanceof RowInfo) {
					toBeDeleted.add((RowInfo) o);
				}
			}
		}
		if (toBeDeleted.size() > 0) {
			RemoveKeyOperation operation = new RemoveKeyOperation(
					Messages.StringEditorPart_RemoveKeyOperationName,
					this.stringEditorPart, toBeDeleted);
			operation.addContext(this.stringEditorPart.getUndoContext());
			this.stringEditorPart.executeOperation(operation);
			this.setEnabled(false);
		}
		this.stringEditorPart.refreshButtonsEnabled();
	}
}