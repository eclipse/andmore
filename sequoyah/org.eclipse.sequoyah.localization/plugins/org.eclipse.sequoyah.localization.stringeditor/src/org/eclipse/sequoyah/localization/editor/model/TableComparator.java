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
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A simple comparator based on the viewer compare logic
 */
public class TableComparator extends ViewerComparator {

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
		TableViewer tableViewer = (TableViewer) viewer;
		TableColumn sortColumn = tableViewer.getTable().getSortColumn();
		if (sortColumn != null && e1 instanceof RowInfo
				&& e2 instanceof RowInfo) {
			String sortColumnID = sortColumn.getText();
			RowInfo r1 = null;
			RowInfo r2 = null;
			if (tableViewer.getTable().getSortDirection() == SWT.UP) {
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
				s1 = r1.getKey();
				s2 = r2.getKey();
			} else {
				// FIXME comparation is wrong
				CellInfo t1 = null;// r1.getCells().get(sortColumnID);
				CellInfo t2 = null;// r2.getCells().get(sortColumnID);
				s1 = t1 != null ? t1.toString() : ""; //$NON-NLS-1$
				s2 = t2 != null ? t2.toString() : ""; //$NON-NLS-1$
			}
			result = s1.toLowerCase().compareTo(s2.toLowerCase());
		} else {
			result = super.compare(viewer, e1, e2);
		}

		return result;
	}
}
