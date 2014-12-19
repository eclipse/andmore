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
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

public class DeleteDatabaseHandler extends AbstractHandler implements IHandler {

	private ITreeNode node;

	public DeleteDatabaseHandler() {
	}

	public DeleteDatabaseHandler(ITreeNode node) {
		this.node = node;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (node instanceof IDbNode) {
			IDbCreatorNode dbCreatorNode = (IDbCreatorNode) node.getParent();

			boolean shouldProceed = EclipseUtils.showQuestionDialog(
					DbCoreNLS.DeleteDatabaseHandler_ConfirmationQuestionDialog_Title,
					NLS.bind(DbCoreNLS.DeleteDatabaseHandler_ConfirmationQuestionDialog_Description, node.getName()));

			if (shouldProceed) {
				IStatus status = dbCreatorNode.deleteDb((IDbNode) node);
				if ((status != null) && !status.isOK()) {
					EclipseUtils.showErrorDialog(DbCoreNLS.DeleteDatabaseHandler_CouldNotDeleteDatabase,
							status.getMessage());
				}
			}

		}

		return null;
	}
}
