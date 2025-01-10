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
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (sort values - single strings)
 * Marcelo Marzola Bossoni (Eldorado) - Bug [326793] - Change from Table to Tree (display arrays as tree)
 * Paulo Faria (Eldorado) - Bug [326798] - According to meeting with sponsor, arrays should go in the end when the ordering is by value. 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * A simple comparator based on the viewer compare logic
 */
public class TreeComparator extends ViewerComparator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.
	 * viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int result = 0;
		TreeViewer tableViewer = (TreeViewer) viewer;
		TreeColumn sortColumn = tableViewer.getTree().getSortColumn();
		if (sortColumn != null && e1 instanceof RowInfo
				&& e2 instanceof RowInfo) {
			String sortColumnID = sortColumn.getText();
			RowInfo r1 = null;
			RowInfo r2 = null;
			if (tableViewer.getTree().getSortDirection() == SWT.UP) {
				r1 = (RowInfo) e1;
				r2 = (RowInfo) e2;
			} else {
				r2 = (RowInfo) e1;
				r1 = (RowInfo) e2;
			}

			String s1 = ""; //$NON-NLS-1$
			String s2 = ""; //$NON-NLS-1$
			if (sortColumnID
					.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
				// sorting key column
				s1 = r1.getKey();
				s2 = r2.getKey();

				result = s1.toLowerCase().compareTo(s2.toLowerCase());
			} else {
				// sorting one of the values columns
				CellInfo t1 = null;
				CellInfo t2 = null;

				if (r1.getChildren().size() == 0
						&& r2.getChildren().size() == 0) {
					// sorting single strings
					if (r1 instanceof RowInfoLeaf) {
						RowInfoLeaf l1 = (RowInfoLeaf) r1;
						if (l1.getParent() == null) {
							t1 = l1.getCells().get(sortColumnID);
						}
					}
					if (r2 instanceof RowInfoLeaf) {
						RowInfoLeaf l2 = (RowInfoLeaf) r2;
						if (l2.getParent() == null) {
							t2 = l2.getCells().get(sortColumnID);
						}
					}
					s1 = t1 != null ? t1.toString() : ""; //$NON-NLS-1$
					s2 = t2 != null ? t2.toString() : ""; //$NON-NLS-1$

					result = s1.toLowerCase().compareTo(s2.toLowerCase());

				} else if (r1.getChildren().size() > 0
						&& r2.getChildren().size() == 0) {
					// r1 is array but r2 is not, r1 goes in the end
					result = (tableViewer.getTree().getSortDirection() == SWT.UP) ? Integer.MAX_VALUE
							: Integer.MIN_VALUE;
				} else if (r1.getChildren().size() == 0
						&& r2.getChildren().size() > 0) {
					// r2 is array but r1 is not, r2 goes in the end
					result = (tableViewer.getTree().getSortDirection() == SWT.UP) ? Integer.MIN_VALUE
							: Integer.MAX_VALUE;
				} else if (r1.getChildren().size() > 0
						&& r2.getChildren().size() > 0) {
					// both r1 and r2 are arrays, order by array key
					result = 0;
				}
			}

		} else {
			result = super.compare(viewer, e1, e2);
		}

		return result;
	}
}
