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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.db.core.ui.ITreeNode;

/**
 * Manager which notifies {@link IDatabaseModelListener} registered that a
 * {@link DatabaseModelEvent} occurred. It is a singleton that needs to be
 * called by the hierarchy of {@link ITreeNode} items when they modify the
 * database model.
 */
public class DatabaseModelEventManager {
	private static DatabaseModelEventManager _instance;

	private final List<IDatabaseModelListener> listeners = new ArrayList<IDatabaseModelListener>();

	private DatabaseModelEventManager() {
		// Singleton - avoid extensions and other means to construct an instance
	}

	/**
	 * @return single instance of {@link DatabaseModelEvent}
	 */
	public synchronized static DatabaseModelEventManager getInstance() {
		if (_instance == null) {
			_instance = new DatabaseModelEventManager();
		}
		return _instance;
	}

	/**
	 * Adds a new {@link IDatabaseModelListener}
	 * 
	 * @param listener
	 */
	public void addListener(IDatabaseModelListener listener) {
		synchronized (listener) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes the {@link IDatabaseModelListener}
	 * 
	 * @param listener
	 */
	public void removeListeners(IDatabaseModelListener listener) {
		synchronized (listener) {
			listeners.remove(listener);
		}
	}

	/**
	 * Fires/notifies/deliver the event to the listeners registered.
	 * 
	 * @param node
	 *            {@link ITreeNode} that needs to refresh the view based on the
	 *            model
	 * @param eventType
	 */
	public void fireEvent(ITreeNode node, DatabaseModelEvent.EVENT_TYPE eventType) {
		DatabaseModelEvent databaseModelEvent = new DatabaseModelEvent(node, eventType);
		synchronized (listeners) {
			if (listeners != null) {
				for (IDatabaseModelListener listener : listeners) {
					switch (eventType) {
					case ADD:
						listener.handleNodeAdditionEvent(databaseModelEvent);
						break;
					case REMOVE:
						listener.handleNodeRemovalEvent(databaseModelEvent);
						break;
					case UPDATE:
						listener.handleNodeUpdateEvent(databaseModelEvent);
						break;
					case CLEAR:
						listener.handleNodeClearEvent(databaseModelEvent);
						break;
					case EXPAND:
						listener.handleNodeExpandEvent(databaseModelEvent);
						break;
					case SELECT:
						listener.handleNodeSelectEvent(databaseModelEvent);
						break;
					default:
						break;
					}
				}
			}
		}
	}
}
