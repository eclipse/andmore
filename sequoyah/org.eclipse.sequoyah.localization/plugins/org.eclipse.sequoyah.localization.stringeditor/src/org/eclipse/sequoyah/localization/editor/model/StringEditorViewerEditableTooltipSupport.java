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
 * Carlos Alberto Souto Junior (Eldorado) - Bug [326793] - Fixed the comment persistence issue
 * Carlos Alberto Souto Junior (Eldorado) - Bug [326793] - Added new tooltip support method for StringArrayItem
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public class StringEditorViewerEditableTooltipSupport extends
		ColumnViewerToolTipSupport {

	private String tooltipText;

	private final ColumnViewer viewer;

	private Event currentTooltipEvent;

	private final StringEditorPart editor;

	protected StringEditorViewerEditableTooltipSupport(ColumnViewer viewer,
			int style, boolean manualActivation, StringEditorPart editor) {
		super(viewer, style, manualActivation);
		setHideOnMouseDown(false);
		this.tooltipText = null;
		this.viewer = viewer;
		this.editor = editor;
	}

	public static void enableFor(ColumnViewer viewer, int style,
			StringEditorPart editor) {
		new StringEditorViewerEditableTooltipSupport(viewer, style, false,
				editor);
	}

	@Override
	protected Composite createViewerToolTipContentArea(Event event,
			ViewerCell cell, Composite parent) {

		Composite toReturn = null;

		if (cell.getColumnIndex() != 0) {
			final String text = getText(event);
			toReturn = new Composite(parent, SWT.FILL);
			GridLayout layout = new GridLayout();
			toReturn.setLayout(layout);
			final Text textComposite = new Text(toReturn, SWT.MULTI | SWT.WRAP);
			GridData layouData = new GridData(GridData.FILL_BOTH);
			layouData.minimumWidth = 200;
			layouData.minimumHeight = 50;
			layouData.grabExcessHorizontalSpace = true;
			layouData.grabExcessVerticalSpace = true;
			textComposite
					.setText(text.trim().length() > 0 ? text
							: Messages.StringEditorViewerEditableTooltipSupport_TypeYourComment);
			textComposite.setLayoutData(layouData);
			textComposite.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					tooltipText = ((Text) e.widget).getText();
				}
			});
			textComposite.addFocusListener(new FocusListener() {

				public void focusLost(FocusEvent e) {
					// do nothing
				}

				public void focusGained(FocusEvent e) {
					if (text.trim().length() == 0) {
						textComposite.setText(""); //$NON-NLS-1$
						tooltipText = null;
					}
				}
			});
		} else {
			toReturn = super
					.createViewerToolTipContentArea(event, cell, parent);
		}
		currentTooltipEvent = event;
		return toReturn;
	}

	@Override
	protected void afterHideToolTip(Event event) {
		if (currentTooltipEvent != null) {
			ViewerCell cell = viewer.getCell(new Point(currentTooltipEvent.x,
					currentTooltipEvent.y));
			if (cell.getColumnIndex() != 0) {
				if (cell.getViewerRow().getElement() instanceof RowInfoLeaf) {
					RowInfoLeaf row = ((RowInfoLeaf) cell.getViewerRow()
							.getElement());
					TreeColumn column = ((Tree) viewer.getControl())
							.getColumn(cell.getColumnIndex());
					if (tooltipText != null
							&& !tooltipText.trim().equals(
									row.getCells().get(column.getText())
											.getComment())) {
						row.getCells().get(column.getText())
								.setComment(tooltipText.trim());
						if (this.editor != null) {
							try {
								if (row.getParent() == null) {
									editor.getEditorInput().setCellTooltip(
											column.getText(), row.getKey(),
											tooltipText);
								} else {
									editor.getEditorInput().setCellTooltip(
											column.getText(), row.getKey(),
											tooltipText, row.getPosition());
								}
								editor.fireDirtyPropertyChanged();
							} catch (SequoyahException e) {
								BasePlugin.logError(
										"Error setting cell tooltip: (" //$NON-NLS-1$
												+ column.getText()
												+ ", " + row.getKey() //$NON-NLS-1$
												+ ") = " + tooltipText, e); //$NON-NLS-1$
							}
						}
					}
				}
				this.tooltipText = null;
				currentTooltipEvent = null;
			}
		}
		super.afterHideToolTip(event);
	}
}
