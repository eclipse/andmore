/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Gorri (Eldorado) - Bug 326793 -  Improvements on the String Arrays handling  
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (Refactor visitDomXYZ and NodeManagers)
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory to create nodes managers specialized to manipulate DOM for single
 * string or array
 */
public class NodeManagerProvider {
	/**
	 * Private instance of this factory for singleton purposes.
	 */
	private volatile static NodeManagerProvider nodeManagerProvider;

	private static final String STRING_NODE_MANAGER = "StringNodeManager"; //$NON-NLS-1$
	private static final String ARRAY_NODE_MANAGER = "ArrayNodeManager"; //$NON-NLS-1$

	/**
	 * Private map for keeping Node Managers for strings or arrays for
	 * StringLocalizationFileManager
	 */
	private Map<String, NodeManager> map = new HashMap<String, NodeManager>();

	/**
	 * Store NodeManager classes for creation.
	 * 
	 */
	private void addNodeManagers() {
		NodeManager stringNodeManager = new StringNodeManager();
		map.put(STRING_NODE_MANAGER, stringNodeManager);
		NodeManager arrayStringNodeManager = new ArrayStringNodeManager();
		map.put(ARRAY_NODE_MANAGER, arrayStringNodeManager);
	}

	/**
	 * Default constructor (private since it is a singleton).
	 */
	private NodeManagerProvider() {
		addNodeManagers();
	}

	/**
	 * This method provides a single instance of this factory for whoever needs
	 * to use it.
	 * 
	 * @return unique instance of this factory
	 */
	public static NodeManagerProvider getInstance() {
		if (nodeManagerProvider == null) {
			synchronized (NodeManagerProvider.class) {
				if (nodeManagerProvider == null) {
					nodeManagerProvider = new NodeManagerProvider();
				}
			}
		}
		return nodeManagerProvider;
	}

	/**
	 * Method responsible for creating the different types of LocalizationFile
	 * based on the type attribute of the LocalizationFileBean received as
	 * parameter.
	 * 
	 * @param bean
	 *            Bean containing all information necessary for the creation of
	 *            a LocalizationFile.
	 * @return LocalizationFile created if the parameter received is not null.
	 */
	public Collection<NodeManager> getNodeManagers() {
		return map.values();
	}

	/**
	 * @return NodeManager to manipulate Single Strings into DOM
	 */
	public StringNodeManager getStringNodeManager() {
		return (StringNodeManager) map.get(STRING_NODE_MANAGER);
	}

	/**
	 * @return NodeManager to manipulate Arrays into DOM
	 */
	public ArrayStringNodeManager getArrayStringNodeManager() {
		return (ArrayStringNodeManager) map.get(ARRAY_NODE_MANAGER);
	}
}