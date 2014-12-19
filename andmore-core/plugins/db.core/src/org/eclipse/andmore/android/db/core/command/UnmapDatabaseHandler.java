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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class UnmapDatabaseHandler extends AbstractHandler implements IHandler {

	private IDbMapperNode dbMapperNode;

	public UnmapDatabaseHandler() {

	}

	public UnmapDatabaseHandler(IDbMapperNode node) {
		this.dbMapperNode = node;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<ITreeNode> mappedDbNodes = dbMapperNode.getChildren();
		List<ITreeNode> dbNodesToUnmap = null;
		if (!mappedDbNodes.isEmpty()) {
			dbNodesToUnmap = queryDbPath(mappedDbNodes);
		}
		IStatus status = dbMapperNode.unmap(dbNodesToUnmap);
		if ((status.getCode() != IStatus.CANCEL) && !status.isOK()) {
			EclipseUtils.showErrorDialog(DbCoreNLS.UnmapDatabaseHandler_Error_Title,
					DbCoreNLS.UnmapDatabaseHandler_Error_Description, status);
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<ITreeNode> queryDbPath(List<ITreeNode> mappedDbNodes) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		ElementListSelectionDialog listDialog = new ElementListSelectionDialog(shell, new LabelProvider() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
			 */
			@Override
			public String getText(Object element) {
				if (element instanceof IDbNode) {
					IDbNode dbNode = (IDbNode) element;
					return dbNode.getName();
				}
				return super.getText(element);
			}
		});
		listDialog.setElements(mappedDbNodes.toArray());
		listDialog.setTitle(DbCoreNLS.UI_UnmapDatabaseAction_Title);
		listDialog.setBlockOnOpen(true);
		listDialog.setMultipleSelection(true);
		listDialog.open();
		Object[] result = listDialog.getResult();
		List asList = Arrays.asList(result);
		List<ITreeNode> checkedList = Collections.checkedList(asList, ITreeNode.class);
		return checkedList;
	}

}
