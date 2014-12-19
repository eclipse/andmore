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
package org.eclipse.andmore.android.db.devices.ui.action;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.command.RefreshNodeHandler;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class PopupMenuActionDelegate implements IObjectActionDelegate {

	/**
	 * Enum type for ActionHandlers. If you need to add a new ActionHandler,
	 * just include a new type to this enum with the action id that you defined
	 * on your action extension point
	 */
	enum ActionHandlers {
		REFRESH_DEVICE("org.eclipse.andmore.android.db.devices.ui.action.refreshDeviceNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		FILTER_DB_APPNODE("org.eclipse.andmore.android.db.devices.ui.action.filterDbApplicationNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new FilterDbApplicationHandler(node);
			}

		},
		MAP_DEVICE_DB_APPNODE("org.eclipse.andmore.android.db.devices.ui.action.mapDeviceDbNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new MapDeviceDatabaseHandler(node);
			}

		},
		SAVE_TO_LOCAL_FILE("org.eclipse.andmore.android.db.devices.ui.action.saveToLocalFile") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new SaveDatabaseToFileHandler(node);
			}

		};

		private final String actionId;

		private ActionHandlers(String actionId) {
			this.actionId = actionId;
		}

		public abstract IHandler getHandler(ITreeNode node);

		public static ActionHandlers getActionHandlerbyId(String id) {

			Object ret = null;
			for (ActionHandlers h : ActionHandlers.values()) {
				if (h.actionId.equals(id)) {
					ret = h;
					break;
				}
			}

			return (ActionHandlers) ret;
		}
	}

	private ITreeNode currentNode;

	@Override
	public void run(IAction action) {

		ActionHandlers type = ActionHandlers.getActionHandlerbyId(action.getId());

		IHandler handler = null;

		if (type != null) {
			handler = type.getHandler(currentNode);
		}

		if (handler != null) {
			ExecutionEvent event = new ExecutionEvent();
			try {
				handler.execute(event);
			} catch (ExecutionException e) {
				StudioLogger.error("Could not execute command: ", e.getMessage()); //$NON-NLS-1$
			}
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object selectedObject = structuredSelection.getFirstElement();
			if (selectedObject instanceof AbstractTreeNode) {
				currentNode = (ITreeNode) selectedObject;
			}
		}

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// do nothing
	}

}
