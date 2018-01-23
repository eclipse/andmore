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

import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Listener;

class StringEditorCellHighlighter extends FocusCellOwnerDrawHighlighter {

	public StringEditorCellHighlighter(TreeViewer viewer) {
		super(viewer);
		// TODO: next release make things better than just avoid parent listener
		for (Listener l : viewer.getControl().getListeners(SWT.EraseItem)) {
			viewer.getControl().removeListener(SWT.EraseItem, l);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#focusCellChanged
	 * (org.eclipse.jface.viewers.ViewerCell,
	 * org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected void focusCellChanged(ViewerCell newCell, ViewerCell oldCell) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#
	 * getSelectedCellBackgroundColor(org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected Color getSelectedCellBackgroundColor(ViewerCell cell) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#
	 * getSelectedCellBackgroundColorNoFocus
	 * (org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected Color getSelectedCellBackgroundColorNoFocus(ViewerCell cell) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#
	 * getSelectedCellForegroundColor(org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected Color getSelectedCellForegroundColor(ViewerCell cell) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#
	 * getSelectedCellForegroundColorNoFocus
	 * (org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected Color getSelectedCellForegroundColorNoFocus(ViewerCell cell) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter#onlyTextHighlighting
	 * (org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	protected boolean onlyTextHighlighting(ViewerCell cell) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.FocusCellHighlighter#getFocusCell()
	 */
	@Override
	public ViewerCell getFocusCell() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.FocusCellHighlighter#init()
	 */
	@Override
	protected void init() {
		// do nothing
	}
}