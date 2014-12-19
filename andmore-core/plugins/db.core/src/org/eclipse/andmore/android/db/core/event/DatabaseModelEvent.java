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

import org.eclipse.andmore.android.db.core.ui.ITreeNode;

public class DatabaseModelEvent {
	private final ITreeNode treeNodeItem;

	private final EVENT_TYPE eventType;

	/**
	 * Represents the change in the model. The
	 * {@link DatabaseModelEvent#treeNodeItem} in each event may vary as
	 * specified below:
	 * <ul>
	 * <li>{@link EVENT_TYPE#ADD} it is the item that needs to be added</li>
	 * <li>{@link EVENT_TYPE#REMOVE} it is the item that needs to be removed</li>
	 * <li>{@link EVENT_TYPE#UPDATE} it is the item that needs to be updated</li>
	 * <li>{@link EVENT_TYPE#CLEAR} it is the parent node that needs to clear
	 * its children</li>
	 * <li>{@link EVENT_TYPE#EXPAND} it is the item that needs to be expanded</li>
	 * <li>{@link EVENT_TYPE#SELECT} it is the item that needs to be selected</li>
	 * </ul>
	 */
	public enum EVENT_TYPE {
		ADD, REMOVE, UPDATE, CLEAR, EXPAND, SELECT
	}

	/**
	 * @return the eventType
	 */
	protected EVENT_TYPE getEventType() {
		return eventType;
	}

	/**
	 * @return the treeNodeItem
	 */
	public ITreeNode getTreeNodeItem() {
		return treeNodeItem;
	}

	/**
	 * Creates a new event from database model over
	 * 
	 * @param item
	 *            an object implementing {@link ITreeNode}
	 * @param eventType
	 *            {@link EVENT_TYPE}
	 */
	public DatabaseModelEvent(ITreeNode item, EVENT_TYPE eventType) {
		this.treeNodeItem = item;
		this.eventType = eventType;
	}
}
