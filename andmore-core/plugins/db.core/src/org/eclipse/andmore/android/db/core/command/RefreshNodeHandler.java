/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.db.core.command;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;

public class RefreshNodeHandler extends AbstractHandler implements IHandler {

	private ITreeNode node;

	public RefreshNodeHandler() {
	}

	public RefreshNodeHandler(ITreeNode node) {
		this.node = node;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (node == null) {
			node = getSelectedItem();
		}

		Runnable asyncRefresh = new Runnable() {

			@Override
			public void run() {
				if (node != null) {
					boolean canRefresh = false;
					boolean canRefreshInput = true;

					IStatus status = node.canRefresh();
					if (status.isOK()) {
						canRefresh = true;
					} else {
						if ((status instanceof CanRefreshStatus) && status.matches(CanRefreshStatus.ASK_USER)) {
							if (status.matches(CanRefreshStatus.ASK_USER | CanRefreshStatus.CANCELABLE)) {
								int dialogResults = EclipseUtils
										.showQuestionWithCancelDialog(
												NLS.bind(DbCoreNLS.RefreshNodeHandler_RefreshingNode_Msg_Title,
														node.getName()), status.getMessage());
								if (dialogResults != SWT.CANCEL) {
									canRefreshInput = dialogResults == SWT.YES;
									canRefresh = true;
								}
							} else {
								canRefresh = EclipseUtils
										.showQuestionDialog(
												NLS.bind(DbCoreNLS.RefreshNodeHandler_RefreshingNode_Msg_Title,
														node.getName()), status.getMessage());
							}
						} else {
							EclipseUtils.showErrorDialog(
									NLS.bind(DbCoreNLS.RefreshNodeHandler_RefreshingNode_Error_Msg, node.getName()),
									status.getMessage(), status);
						}
					}

					if (canRefresh) {
						node.refreshAsync(canRefreshInput);
						node = null; // clear selected node to force
										// getSelectedItem to be called when
										// calling via toolbar
					}

				}

			}
		};
		Thread refreshThread = new Thread(asyncRefresh);
		refreshThread.start();

		return null;
	}

	private ITreeNode getSelectedItem() {
		ITreeNode selectedNode = DbCoreActivator.getMOTODEVDatabaseExplorerView().getSelectedItemOnTree();

		return selectedNode;
	}

}
