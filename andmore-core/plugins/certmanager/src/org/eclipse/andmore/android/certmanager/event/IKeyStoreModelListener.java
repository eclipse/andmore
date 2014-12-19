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

package org.eclipse.andmore.android.certmanager.event;

import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;

/**
 * This interface must be implemented by listeners to events occurred on the
 * {@link ITreeNode}.
 */
public interface IKeyStoreModelListener {
	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to add a node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be added.
	 */
	public void handleNodeAdditionEvent(KeyStoreModelEvent keyStoreModeEvent);

	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to remove a node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be added.
	 */
	public void handleNodeRemovalEvent(KeyStoreModelEvent keyStoreModeEvent);

	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to update a node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be added.
	 */
	public void handleNodeUpdateEvent(KeyStoreModelEvent keyStoreModeEvent);

	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to collapse a
	 * node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be collapsed.
	 */
	public void handleNodeCollapseEvent(KeyStoreModelEvent keyStoreModelEvent);

	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to refresh a node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be refreshed.
	 */
	public void handleNodeRefreshEvent(KeyStoreModelEvent keyStoreModelEvent);

	/**
	 * Handles the event {@link KeyStoreModelEvent#EventType} to clear a node.
	 * 
	 * @param keyStoreModelEvent
	 *            {@link KeyStoreModelEvent#getTreeNodeItem()} contains the node
	 *            to be cleared.
	 */
	public void handleNodeClearEvent(KeyStoreModelEvent keyStoreModelEvent);

}
