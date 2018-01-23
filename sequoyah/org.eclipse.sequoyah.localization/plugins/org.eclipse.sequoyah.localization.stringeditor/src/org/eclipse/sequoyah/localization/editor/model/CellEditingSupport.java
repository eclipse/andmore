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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.operations.EditCellOperation;
import org.eclipse.sequoyah.localization.editor.model.operations.EditKeyOperation;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IMessage;

/**
 * A editing support for this editor cells. Only the non keys cells are editable
 */
class CellEditingSupport extends EditingSupport {

	/**
	 * 
	 */
	private final StringEditorPart stringEditorPart;

	private final String columnID;

	private final CellEditor editor;

	public CellEditingSupport(final StringEditorPart stringEditorPart,
			TreeViewer viewer, String columnID) {
		super(viewer);
		this.stringEditorPart = stringEditorPart;
		this.columnID = columnID;
		editor = columnID.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel) ? new TextCellEditor(
				viewer.getTree(), SWT.SINGLE) : new TextCellEditor(
				viewer.getTree(), SWT.MULTI | SWT.V_SCROLL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
	 */
	@Override
	protected boolean canEdit(Object element) {
		// only let to edit keys from string and array
		// DO NOT let to edit keys from array items
		boolean canEdit = true;
		if (columnID.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			// key cell
			if (element instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) element;
				if (leaf.getParent() != null) {
					// array item
					canEdit = false;
				} else {
					// single string item
					canEdit = true;
				}
			} else if (element instanceof RowInfo) {
				// array
				canEdit = true;
			}
		} else {
			// non-key cell
			canEdit = (element instanceof RowInfoLeaf) ? true : false;
		}
		return canEdit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.
	 * Object)
	 */
	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
	 */
	@Override
	protected Object getValue(Object element) {
		RowInfo theRow = null;
		String value = null;
		if (element instanceof RowInfo) {
			theRow = (RowInfo) element;
		}
		// assuming that we can only edit arrays and items and not array
		// items
		if (columnID.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			value = theRow.getKey();
		} else {
			if (element instanceof RowInfoLeaf) {
				CellInfo info = ((RowInfoLeaf) element).getCells()
						.get(columnID);
				value = ((info != null) && (info.getValue() != null)) ? info
						.getValue() : ""; //$NON-NLS-1$
			}
		}
		return value; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	protected void setValue(Object element, final Object value) {

		if (columnID.equalsIgnoreCase(Messages.StringEditorPart_KeyLabel)) {
			final RowInfo[] theRow = new RowInfo[1];
			if (element instanceof RowInfo) {
				theRow[0] = (RowInfo) element;
			}
			if (!value.equals(theRow[0].getKey())
					&& stringEditorPart.getModel().getRows().containsKey(value)) {
				// key already exists at another row => notify user
				getViewer().getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(
								getViewer().getControl().getShell(),
								Messages.CellEditingSupport_0,
								Messages.CellEditingSupport_1 + "'" //$NON-NLS-1$
										+ value.toString() + "'" //$NON-NLS-1$
										+ Messages.CellEditingSupport_2);
						stringEditorPart.getEditorViewer().editElement(
								theRow[0], 0);
					}
				});

			} else {
				// key does not exist

				// only execute an operation if the new value is different
				// from
				// the old one AND the value isn't blank
				if (value != null && value.toString().trim().length() > 0
						&& value.toString().compareTo(theRow[0].getKey()) != 0) {
					EditKeyOperation operation = new EditKeyOperation(
							theRow[0].getKey(), value.toString(),
							stringEditorPart);
					stringEditorPart.executeOperation(operation);
				}
			}
		} else {

			if (element instanceof RowInfoLeaf) {
				RowInfoLeaf theRow = (RowInfoLeaf) element;
				CellInfo oldCell = theRow.getCells().get(columnID);
				CellInfo newCell = null;

				String EOL = System.getProperty("line.separator"); //$NON-NLS-1$
				String oldValue = ((oldCell != null) ? oldCell.getValue() : ""); //$NON-NLS-1$
				String newValue = ((String) value).replaceAll(EOL, "\n"); //$NON-NLS-1$
				if (newValue.equals(oldValue)) {
					return;
				}

				/*
				 * If our new value is a valid one, we create a new cell
				 */
				if (value.toString().length() > 0) {
					/*
					 * if our old cell isn't a null one check if the values
					 * aren't the same
					 */
					if (oldCell != null) {
						/*
						 * Our old cell is different from our new one
						 */
						if (((oldCell.getValue() != null) && !oldCell
								.getValue().equals(value.toString()))
								|| (oldCell.getValue() == null)) {
							newCell = new CellInfo(value.toString(),
									oldCell.getComment(), oldCell.getPosition());
						}
					} else {
						newCell = new CellInfo(value.toString(), null,
								theRow.getPosition());
					}
				} else {
					if (oldCell != null) {
						newCell = new CellInfo(null, null,
								oldCell.getPosition());
					}
				}
				if (newCell != null) {
					newCell.setDirty(true);
					EditCellOperation operation = new EditCellOperation(
							theRow.getKey(), columnID, oldCell, newCell,
							stringEditorPart, theRow);
					stringEditorPart.executeOperation(operation);
				}
			}
		}
	}
}