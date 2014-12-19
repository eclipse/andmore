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
 * This class represents an event occurred on the {@link ITreeNode}.
 */
public class KeyStoreModelEvent {
	private final EventType eventType;

	private final ITreeNode treeNodeItem;

	/**
	 * Represents the change in the model. The
	 * {@link KeyStoreModelEvent#treeNodeItem} in each event may vary as
	 * specified below:
	 * <ul>
	 * <li>{@link EVENT_TYPE#ADD} it is the item that needs to be added</li>
	 * <li>{@link EVENT_TYPE#REMOVE} it is the item that needs to be deleted</li>
	 * <li>{@link EVENT_TYPE#UPDATE} it is the item that needs to be updated</li>
	 * </ul>
	 */
	public enum EventType {
		ADD, REMOVE, UPDATE, COLLAPSE, REFRESH, CLEAR
	}

	/**
	 * Returns the event type.
	 * */
	public EventType getEventType() {
		return eventType;
	}

	/**
	 * Returns the tree node item related to the event.
	 * */
	public ITreeNode getTreeNodeItem() {
		return treeNodeItem;
	}

	/**
	 * Constructs a new event given an {@link ITreeNode} and a
	 * {@link KeyStoreModelEvent#EventType}.
	 * */
	public KeyStoreModelEvent(ITreeNode treeNodeItem, EventType eventType) {
		this.eventType = eventType;
		this.treeNodeItem = treeNodeItem;
	}
}
