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
 * Marcelo Marzola Bossoni (Eldorado) - Bug [326793] - Change from Table to Tree (display arrays as tree)
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationListener;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.swt.widgets.Tree;

public class StringEditorColumnViewerEditorActivationListener extends
		ColumnViewerEditorActivationListener {

	private final TreeViewer viewer;
	private boolean jumpToNextCell = true;

	public StringEditorColumnViewerEditorActivationListener(TreeViewer viewer) {
		this.viewer = viewer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.ColumnViewerEditorActivationListener#
	 * beforeEditorDeactivated
	 * (org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent)
	 */
	@Override
	public void beforeEditorDeactivated(
			ColumnViewerEditorDeactivationEvent event) {

		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.ColumnViewerEditorActivationListener#
	 * beforeEditorActivated
	 * (org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent)
	 */
	@Override
	public void beforeEditorActivated(ColumnViewerEditorActivationEvent event) {

		boolean next = true;
		if (event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC) {
			ViewerCell cell = (ViewerCell) event.getSource();
			if (cell != null && cell.getElement() instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) cell.getElement();
				if (leaf.getParent() != null) {
					next = false;
				}
			}
		}
		jumpToNextCell = next;

		// Workaround for the Mac OS X issue of cells with null text not being
		// double-clickable

		ViewerCell cell = (ViewerCell) event.getSource();
		if (cell.getText().equals("")) { //$NON-NLS-1$
			cell.setText(" "); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.ColumnViewerEditorActivationListener#
	 * afterEditorDeactivated
	 * (org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent)
	 */
	@Override
	public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
		ViewerCell cell = (ViewerCell) event.getSource();

		// traverse the viewer without any key press
		if (jumpToNextCell
				&& event.eventType == ColumnViewerEditorDeactivationEvent.EDITOR_SAVED) {
			// Workaround linux gtk problem returning 0 size for column
			if (Platform.getOS().equals(Platform.OS_LINUX)) {
				((Tree) cell.getControl()).getColumn(cell.getColumnIndex())
						.setWidth(cell.getBounds().width + 1);
			}
			// finish workaround
			ViewerCell nieghbor = cell.getNeighbor(ViewerCell.BELOW, false);
			if (nieghbor != null) {
				viewer.setSelection(new StructuredSelection(nieghbor
						.getElement()));
			}
		}
	}

	@Override
	public void afterEditorActivated(ColumnViewerEditorActivationEvent event) {

	}
}
