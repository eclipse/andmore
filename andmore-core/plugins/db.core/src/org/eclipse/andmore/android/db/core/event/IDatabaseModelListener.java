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
package org.eclipse.andmore.android.db.core.event;

public interface IDatabaseModelListener {
	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#ADD} of the
	 * addition of a new node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be added.
	 */
	public void handleNodeAdditionEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#REMOVE} of the
	 * removal of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be removed.
	 */
	public void handleNodeRemovalEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#UPDATE} of the
	 * update of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be updated.
	 */
	public void handleNodeUpdateEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#CLEAR} of the
	 * clear of the children of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the
	 *            parent node to clear its children.
	 */
	public void handleNodeClearEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#CLEAR} of the
	 * refresh of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be refreshed.
	 */
	public void handleNodeRefreshEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#EXPAND} of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be expanded.
	 */
	public void handleNodeExpandEvent(DatabaseModelEvent databaseModelEvent);

	/**
	 * Handles the event {@link DatabaseModelEvent#EVENT_TYPE#SELECT} of a node.
	 * 
	 * @param databaseModelEvent
	 *            {@link DatabaseModelEvent#getTreeNodeItem()} contains the node
	 *            to be selected.
	 */
	public void handleNodeSelectEvent(DatabaseModelEvent databaseModelEvent);
}
