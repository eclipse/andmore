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
package org.eclipse.andmore.android.db.core.ui.view;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Manager class responsible to handle save state for tree nodes. Note: The
 * saveState method will be called only after workbench quits, quiting the view
 * will not fire the save event state. This class is a singleton. Users must
 * register the ISaveStateTreenode by using resiterSaveStateNode method. Don't
 * forget to unregister the node if node is removed.
 */
public class SaveStateManager {

	private static SaveStateManager instance;

	private final Set<ISaveStateTreeNode> registeredTreeNodes;

	private final IEclipsePreferences prefNode;

	/*
	 * Private constructor, this is a singleton.
	 */
	private SaveStateManager() {
		registeredTreeNodes = Collections.synchronizedSet(new HashSet<ISaveStateTreeNode>());
		prefNode = InstanceScope.INSTANCE.getNode(DbCoreActivator.PLUGIN_ID);
	}

	/**
	 * @return the single instance of {@link SaveStateManager}
	 */
	public static SaveStateManager getInstance() {
		if (instance == null) {
			instance = new SaveStateManager();
		}

		return instance;
	}

	/**
	 * Register a node to be saved.
	 * 
	 * @param treeNode
	 *            to be asked for save
	 */
	public void registerSaveStateNode(ISaveStateTreeNode treeNode) {
		synchronized (registeredTreeNodes) {
			registeredTreeNodes.add(treeNode);
		}
	}

	/**
	 * Calls the saveState method for all registered nodes.
	 * 
	 * @param memento
	 */
	public void saveState() {
		synchronized (registeredTreeNodes) {
			for (ISaveStateTreeNode registeredTreeNode : registeredTreeNodes) {
				registeredTreeNode.saveState(prefNode);
			}
		}
	}

	/**
	 * Unregister the given node from this manager.
	 * 
	 * @param node
	 */
	public void unregisterSaveStateNode(ISaveStateTreeNode node) {
		synchronized (registeredTreeNodes) {
			registeredTreeNodes.remove(node);
		}
	}

	/**
	 * Retrieves the {@link IEclipsePreferences} prefNode.
	 * 
	 * @return The {@link IEclipsePreferences} node used bu this manager to save
	 *         the states.
	 */
	public IEclipsePreferences getPrefNode() {
		return prefNode;
	}
}
