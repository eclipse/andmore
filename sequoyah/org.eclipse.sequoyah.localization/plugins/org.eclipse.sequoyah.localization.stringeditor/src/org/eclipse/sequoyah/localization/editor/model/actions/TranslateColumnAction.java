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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.sequoyah.localization.editor.datatype.TranslationInfo;
import org.eclipse.sequoyah.localization.editor.i18n.Messages;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.operations.AddColumnOperation;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;

public class TranslateColumnAction extends Action {
	/**
     * 
     */
	private final StringEditorPart stringEditorPart;

	public TranslateColumnAction(StringEditorPart stringEditorPart) {
		super(Messages.StringEditorPart_TranslateActionName);
		this.stringEditorPart = stringEditorPart;
		this.setEnabled(this.stringEditorPart.getActiveColumn() != 0);
	}

	@Override
	public void run() {

		TreeColumn originalColumn = stringEditorPart.getEditorViewer()
				.getTree().getColumn(stringEditorPart.getActiveColumn());

		final TranslationInfo newColumnInfo = stringEditorPart
				.getContentProvider().getOperationProvider()
				.getTranslatedColumnInfo(originalColumn.getText());

		if (newColumnInfo != null) {
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

								if ((newColumnInfo != null)
										&& (stringEditorPart
												.getColumnByID(newColumnInfo
														.getId()) == null)) {

									IStatus translateStatus = stringEditorPart
											.getEditorInput().translateColumn(
													originalColumn.getText(),
													newColumnInfo, monitor);
									if (translateStatus.isOK()) {

										stringEditorPart.getEditorInput()
												.getValues(
														newColumnInfo.getId());
										AddColumnOperation operation = new AddColumnOperation(
												Messages.StringEditorPart_TranslateActionName,
												stringEditorPart, newColumnInfo);

										operation.addContext(stringEditorPart
												.getUndoContext());
										stringEditorPart
												.executeOperation(operation);
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

								} else {
									monitor.setCanceled(true);
									MessageDialog
											.openError(
													stringEditorPart
															.getEditorSite()
															.getShell(),
													Messages.StringEditorPart_TranslationError,
													Messages.StringEditorPart_TranslationErrorTargetExists);
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