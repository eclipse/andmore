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
 * Daniel Drigo Pastore (Eldorado) - Bug [326793] - Changes on image and tooltip
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import java.text.NumberFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * This class provides a label for each column
 */
public class StringEditorColumnLabelProvider extends ColumnLabelProvider {

	private final String column;

	private final StringEditorPart editor;

	private final Point tooltipShift;

	private final Color searchColor = new Color(Display.getDefault(), 255, 200,
			200);

	/**
	 * Creates a new label provider
	 * 
	 * @param column
	 *            the columnID to get the info
	 */
	public StringEditorColumnLabelProvider(String column,
			StringEditorPart editor) {
		this.column = column;
		this.editor = editor;
		// Workaround for tooltip on Ubuntu.. show tooltip under mouse cursor
		this.tooltipShift = Platform.getOS().equals(Platform.OS_LINUX) ? new Point(
				-6, -10) : new Point(5, -5);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (column.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			if (element instanceof RowInfoLeaf) {
				RowInfoLeaf row = (RowInfoLeaf) element;
				if (row.getParent() != null) {
					// array item
					NumberFormat f = NumberFormat.getInstance();
					f.setMinimumIntegerDigits(3);
					// did not work :( char levelMarker = '\u2514';
					return super.getText(f.format(row.getPosition()));
				}
			}
			return super.getText(((RowInfo) element).getKey());
		}
		if (element instanceof RowInfoLeaf) {
			return super
					.getText(((RowInfoLeaf) element).getCells().get(column));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.
	 * Object)
	 */
	@Override
	public String getToolTipText(Object element) {
		String comment = null;
		RowInfo row = (RowInfo) element;
		if (column.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			if (!row.getStatus().isOK()) {
				StringBuilder builder = new StringBuilder();
				builder.append(Messages.StringEditorColumnLabelProvider_0);
				for (IStatus child : row.getStatus().getChildren()) {
					builder.append("\n - "); //$NON-NLS-1$
					builder.append(child.getMessage());
				}
				comment = builder.toString();
			}
		} else if (editor.getShowCellComments()) {
			if (row instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) row;
				CellInfo cell = leaf.getCells().get(column);
				if (cell != null && cell.getValue() != null
						&& cell.getValue().trim().length() > 0) {
					comment = cell.getComment() != null ? cell.getComment()
							: ""; //$NON-NLS-1$
				}
			}
		}
		return comment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getBackground(java.lang
	 * .Object)
	 */
	@Override
	public Color getBackground(Object element) {
		Color c = null;
		if (column.equals(Messages.StringEditorPart_KeyLabel)) {
			c = Display.getDefault().getSystemColor(
					SWT.COLOR_WIDGET_LIGHT_SHADOW);
		} else {
			if (element instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) element;
				CellInfo cell = leaf.getCells().get(column);
				String searchText = editor.getSearchText();
				if (cell != null) {
					if (searchText.length() > 0
							&& cell.toString().toLowerCase()
									.contains(searchText.toLowerCase())) {
						c = searchColor;
					} else if (editor.getHighlightChanges() && cell.isDirty()) {
						c = Display.getDefault().getSystemColor(
								SWT.COLOR_INFO_BACKGROUND);
					}
				}
			} else {
				c = Display.getDefault().getSystemColor(
						SWT.COLOR_WIDGET_LIGHT_SHADOW);
			}
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image statusImage = null;
		if (column.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			RowInfo row = ((RowInfo) element);
			switch (row.getStatus().getSeverity()) {
			case IStatus.ERROR:
				statusImage = editor.getErrorImage();
				break;
			case IStatus.WARNING:
				statusImage = editor.getWarningImage();
				break;
			default:
				statusImage = editor.getOKImage();
				break;
			}
		}

		return statusImage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	public Font getFont(Object element) {
		if (column.equals(Messages.StringEditorPart_KeyLabel)) {
			if (!(element instanceof RowInfoLeaf)) {
				Font font = Display.getDefault().getSystemFont();
				FontData[] data = font.getFontData();
				Font newFont = new Font(font.getDevice(), data[0].getName(),
						data[0].getHeight(), SWT.BOLD);
				return newFont;
			} else if (element instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) element;
				if (leaf.getParent() != null) {
					Font font = Display.getDefault().getSystemFont();
					FontData[] data = font.getFontData();
					Font newFont = new Font(font.getDevice(),
							data[0].getName(), data[0].getHeight(), SWT.ITALIC);
					return newFont;
				}
			}
		}
		return null;
	}

	@Override
	public Point getToolTipShift(Object object) {
		return tooltipShift;
	}

	@Override
	public int getToolTipDisplayDelayTime(Object object) {
		return 250;
	}

}
