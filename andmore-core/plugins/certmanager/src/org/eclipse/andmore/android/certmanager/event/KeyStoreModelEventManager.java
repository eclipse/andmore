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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;

/**
 * Manager which notifies {@link IKeyStoreModelListener} registered that a
 * {@link KeyStoreModelEvent} occurred. It is a singleton that needs to be
 * called by the hierarchy of {@link ITreeNode} items when they modify the
 * {@link ITreeNode}.
 */
public class KeyStoreModelEventManager {
	private static KeyStoreModelEventManager _instance;

	private final List<IKeyStoreModelListener> listeners = new ArrayList<IKeyStoreModelListener>();

	private KeyStoreModelEventManager() {
		// Singleton - private default constructor prevents instantiations by
		// other classes.
	}

	/**
	 * Return the singleton instance of KeyStoreModelEventManager.
	 * */
	public synchronized static KeyStoreModelEventManager getInstance() {
		if (_instance == null) {
			_instance = new KeyStoreModelEventManager();
		}
		return _instance;
	}

	/**
	 * Add the parameter {@code listener} to the list of KeyStore event
	 * listeners.
	 * 
	 * @param listener
	 *            The listener to be added.
	 * */
	public void addListener(IKeyStoreModelListener listener) {
		synchronized (listener) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove the parameter {@code listener} to the list of KeyStore event
	 * listeners.
	 * 
	 * @param listener
	 *            The listener to be removed.
	 * */
	public void removeListener(IKeyStoreModelListener listener) {
		synchronized (listener) {
			listeners.remove(listener);
		}
	}

	/**
	 * Fire/notify/deliver the event to registered listeners.
	 * 
	 * @param node
	 *            {@link ITreeNode} that needs to refresh the view based on the
	 *            model
	 * @param eventType
	 *            Event that occurred.
	 */
	public void fireEvent(ITreeNode treeNodeItem, KeyStoreModelEvent.EventType eventType) {
		KeyStoreModelEvent keyStoreModelEvent = new KeyStoreModelEvent(treeNodeItem, eventType);
		synchronized (listeners) {
			if (listeners != null) {
				for (IKeyStoreModelListener listener : listeners) {
					switch (eventType) {
					case ADD:
						listener.handleNodeAdditionEvent(keyStoreModelEvent);
						break;
					case REMOVE:
						listener.handleNodeRemovalEvent(keyStoreModelEvent);
						break;
					case UPDATE:
						listener.handleNodeUpdateEvent(keyStoreModelEvent);
						break;
					case COLLAPSE:
						listener.handleNodeCollapseEvent(keyStoreModelEvent);
						break;
					case REFRESH:
						listener.handleNodeRefreshEvent(keyStoreModelEvent);
						break;
					case CLEAR:
						listener.handleNodeClearEvent(keyStoreModelEvent);
						break;
					default:
						break;
					}
				}
			}
		}
	}
}
