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
package org.eclipse.andmore.android.db.core.ui.action;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.command.BrowseTableContentsHandler;
import org.eclipse.andmore.android.db.core.command.CreateDatabaseManagementClassesHandler;
import org.eclipse.andmore.android.db.core.command.DbConnectHandler;
import org.eclipse.andmore.android.db.core.command.DbCreateHandler;
import org.eclipse.andmore.android.db.core.command.DbDisconnectHandler;
import org.eclipse.andmore.android.db.core.command.DeleteDatabaseHandler;
import org.eclipse.andmore.android.db.core.command.DeleteTableHandler;
import org.eclipse.andmore.android.db.core.command.ExtractDataHandler;
import org.eclipse.andmore.android.db.core.command.LoadDataHandler;
import org.eclipse.andmore.android.db.core.command.MapDatabaseHandler;
import org.eclipse.andmore.android.db.core.command.RefreshNodeHandler;
import org.eclipse.andmore.android.db.core.command.SampleContentsHandler;
import org.eclipse.andmore.android.db.core.command.TableCreateHandler;
import org.eclipse.andmore.android.db.core.command.UnmapDatabaseHandler;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.IDataSampler;
import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITableNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.TableNode;
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
		CREATE_DB("org.eclipse.andmore.android.db.core.ui.action.createDbAction") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new DbCreateHandler((IDbCreatorNode) node);
			}
		},
		CREATE_TABLE("org.eclipse.andmore.android.db.core.ui.action.createTableAction") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new TableCreateHandler((ITableCreatorNode) node);
			}
		},
		CONNECT("org.eclipse.andmore.android.db.core.ui.action.connect") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new DbConnectHandler((IDbNode) node);
			}
		},
		DISCONNECT("org.eclipse.andmore.android.db.core.ui.action.disconnect") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new DbDisconnectHandler((IDbNode) node);
			}
		},
		REFRESH_PROJECT("org.eclipse.andmore.android.db.core.ui.action.refreshProjectNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		REFRESH_WORKSPACE("org.eclipse.andmore.android.db.core.ui.action.refreshWorkspaceNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		REFRESH_FILESYSTEM("org.eclipse.andmore.android.db.core.ui.action.refreshFileSystemNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		REFRESH_DB("org.eclipse.andmore.android.db.core.ui.action.refreshDbNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		REFRESH_TABLE("org.eclipse.andmore.android.db.core.ui.action.refreshTableNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		REFRESH_DB_MAPPER_NODE("org.eclipse.andmore.android.db.core.ui.action.refreshDbMapperNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new RefreshNodeHandler(node);
			}

		},
		DELETE_TABLE("org.eclipse.andmore.android.db.core.ui.action.deleteTable") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new DeleteTableHandler(node);
			}

		},
		DELETE_DATABASE("org.eclipse.andmore.android.db.core.ui.action.deleteDatabase") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new DeleteDatabaseHandler(node);
			}

		},
		BROWSE_TABLE_CONTENTS("org.eclipse.andmore.android.db.core.ui.action.browseTableContents") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new BrowseTableContentsHandler((TableNode) node);
			}
		},
		CREATE_DB_MANAGEMENT_CLASSES("org.eclipse.andmore.android.db.core.ui.action.createDatabaseManagementClasses") //$NON-NLS-1$
		{
			@Override
			public IHandler getHandler(ITreeNode node) {
				return new CreateDatabaseManagementClassesHandler((IDbNode) node);
			}
		},
		MAP_DATABASE("org.eclipse.andmore.android.db.core.ui.action.mapDbNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new MapDatabaseHandler((IDbMapperNode) node);
			}

		},
		UNMAP_DATABASE("org.eclipse.andmore.android.db.core.ui.action.unmapDbNode") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new UnmapDatabaseHandler((IDbMapperNode) node);
			}

		},
		SAMPLE_CONTENTS("org.eclipse.andmore.android.db.core.ui.action.sampleContents") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new SampleContentsHandler((IDataSampler) node);
			}

		},
		EXTRACT_DATA("org.eclipse.andmore.android.db.core.ui.action.extractData") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new ExtractDataHandler((ITableNode) node);
			}

		},
		LOAD_DATA("org.eclipse.andmore.android.db.core.ui.action.loadData") //$NON-NLS-1$
		{

			@Override
			public IHandler getHandler(ITreeNode node) {
				return new LoadDataHandler((ITableNode) node);
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
				StudioLogger.debug("Could not execute popupHandler");
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
		// Do nothing.
	}

}
