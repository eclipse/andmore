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
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [353518] - Show translator errors messages
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.datatype.TranslationInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.EditCellsOperation;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;

public class TranslateCellAction extends Action {

	/**
     * 
     */
	private final StringEditorPart stringEditorPart;

	public TranslateCellAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_TranslateCellActionName);
		this.stringEditorPart = stringEditorPart;
		setEnabled((stringEditorPart.getActiveColumn() != 0)
				&& (stringEditorPart.getActiveRow() != null));
	}

	@Override
	@SuppressWarnings("unused")
	public void run() {
		TreeColumn originalColumn = stringEditorPart.getEditorViewer()
				.getTree().getColumn(stringEditorPart.getActiveColumn());

		StructuredSelection selection = (StructuredSelection) stringEditorPart
				.getEditorViewer().getSelection();

		ArrayList<String> selectedKeysText = new ArrayList<String>();
		ArrayList<String> selectedCellsText = new ArrayList<String>();
		ArrayList<Integer> selectedIndexes = new ArrayList<Integer>();

		final Object[] selectedRows = selection.toArray();
		int itemsCounter = 0;
		for (int i = 0; i < selectedRows.length; i++) {
			RowInfo rowInfo = (RowInfo) selectedRows[i];
			if (rowInfo instanceof RowInfoLeaf) {
				RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
				CellInfo cellInfo = leaf.getCells().get(
						originalColumn.getText());
				selectedKeysText.add(itemsCounter, rowInfo.getKey());

				if (cellInfo != null) {
					selectedCellsText.add(itemsCounter, cellInfo.getValue());
				} else {
					selectedCellsText.add(itemsCounter, ""); //$NON-NLS-1$
				}

				if (leaf.getParent() != null) {
					selectedIndexes.add(itemsCounter, leaf.getPosition());
				} else {
					selectedIndexes.add(itemsCounter, -1);
				}
				itemsCounter++;
			} else {
				for (RowInfoLeaf leaf : rowInfo.getChildren()) {
					CellInfo cellInfo = leaf.getCells().get(
							originalColumn.getText());

					selectedKeysText.add(itemsCounter, rowInfo.getKey());
					if (cellInfo != null) {
						selectedCellsText
								.add(itemsCounter, cellInfo.getValue());
					} else {
						selectedCellsText.add(itemsCounter, ""); //$NON-NLS-1$
					}

					if (leaf.getParent() != null) {
						selectedIndexes.add(itemsCounter, leaf.getPosition());
					} else {
						selectedIndexes.add(itemsCounter, -1);
					}
					itemsCounter++;

				}
			}
		}

		String selectedCellText = stringEditorPart.getActiveRow()
				.getViewerRow().getCell(stringEditorPart.getActiveColumn())
				.getText();
		TreeColumn[] TreeColumns = stringEditorPart.getEditorViewer().getTree()
				.getColumns();

		String[] a = new String[1];
		String[] b = new String[1];
		Integer[] c = new Integer[1];
		final TranslationInfo[] newColumnsInfo = stringEditorPart
				.getContentProvider()
				.getOperationProvider()
				.getTranslatedColumnsInfo(originalColumn.getText(),
						selectedKeysText.toArray(a),
						selectedCellsText.toArray(b),
						selectedIndexes.toArray(c), TreeColumns);

		if (newColumnsInfo != null) {
			try {
				PlatformUI.getWorkbench().getProgressService()
						.run(false, false, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {

								monitor.setTaskName(Messages.TranslationProgress_Connecting);

								TreeColumn originalColumn = stringEditorPart
										.getEditorViewer()
										.getTree()
										.getColumn(
												stringEditorPart
														.getActiveColumn());
								IStatus translateStatus = stringEditorPart
										.getEditorInput().translateCells(
												originalColumn.getText(),
												newColumnsInfo, monitor);
								if (translateStatus.isOK()) {

									int size = newColumnsInfo.length;
									List<String> keysList = new LinkedList<String>();
									List<String> columnsList = new LinkedList<String>();
									List<CellInfo> oldValuesList = new LinkedList<CellInfo>();
									List<CellInfo> newValuesList = new LinkedList<CellInfo>();
									List<RowInfo> newRowsList = new LinkedList<RowInfo>();

									for (TranslationInfo translationInfo : newColumnsInfo) {
										String key = translationInfo
												.getFromKey();
										String column = translationInfo
												.getToColumn();
										CellInfo oldValue = stringEditorPart
												.getModel().getCellInfo(
														column,
														key,
														translationInfo
																.getIndexKey());
										CellInfo newValue = new CellInfo(
												translationInfo.getToWord(),
												oldValue != null ? oldValue
														.getComment() : null,
												translationInfo.getIndexKey());
										RowInfo newRow = null;

										RowInfo info = stringEditorPart
												.getModel().getRow(
														translationInfo
																.getFromKey());
										if (!(info instanceof RowInfoLeaf)) {
											RowInfoLeaf leaf = info
													.getChildren()
													.get(translationInfo
															.getIndexKey());
											newRow = leaf;
										} else {
											newRow = info;
										}
										// only add the translation if the new
										// value is different from the old one
										if ((newValue != null)
												&& ((oldValue == null)
														|| ((oldValue
																.getValue() == null) && (newValue
																.getValue() != null)) || ((oldValue != null) && (newValue
														.getValue()
														.compareTo(
																oldValue.getValue()) != 0)))) {
											keysList.add(key);
											columnsList.add(column);
											oldValuesList.add(oldValue);
											newValuesList.add(newValue);
											newRowsList.add(newRow);
										}

									}

									if (keysList.size() > 0) {

										EditCellsOperation operation = new EditCellsOperation(
												Messages.StringEditorPart_TranslateCellActionName,
												keysList.toArray(new String[0]),
												columnsList
														.toArray(new String[0]),
												oldValuesList
														.toArray(new CellInfo[0]),
												newValuesList
														.toArray(new CellInfo[0]),
												stringEditorPart,
												newRowsList
														.toArray(new RowInfo[0]));

										stringEditorPart
												.executeOperation(operation);

									} else {
										monitor.setCanceled(true);
										MessageDialog
												.openInformation(
														stringEditorPart
																.getEditorSite()
																.getShell(),
														Messages.TranslateCellAction_NoResultsTitle,
														Messages.TranslateCellAction_NoResultsMessage);
									}

								} else {
									monitor.setCanceled(true);
									ErrorDialog
											.openError(
													stringEditorPart
															.getEditorSite()
															.getShell(),
													Messages.StringEditorPart_TranslationError,
													Messages.StringEditorPart_TranslationError
															+ "\n"
															+ Messages.StringEditorPart_TranslationErrorCheckConnetion,
													translateStatus);
								}

							}
						});
			} catch (InvocationTargetException e) {
				// Do nothing
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
	}
}