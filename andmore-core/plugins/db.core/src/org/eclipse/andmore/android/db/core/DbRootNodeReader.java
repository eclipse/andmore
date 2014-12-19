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
package org.eclipse.andmore.android.db.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public abstract class DbRootNodeReader {
	public static final String DB_ROOT_NODE_EXTENSION_POINT_ID = "org.eclipse.andmore.android.db.core.dbRootNode"; //$NON-NLS-1$

	public static final String DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	public static final String DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

	public static final String DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$

	public static final String DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Load existing root nodes, mapped by extension id
	 * 
	 * @param treeNodeList
	 *            The map of root nodes to be populated
	 * 
	 */
	public static void loadRootNode(HashMap<String, AbstractTreeNode> treeNodeList) throws PartInitException

	{
		List<String> failedNodeNames = new ArrayList<String>();
		treeNodeList.clear();

		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = extReg.getExtensionPoint(DB_ROOT_NODE_EXTENSION_POINT_ID);

		String id = null;
		String name = null;
		if (extPoint != null) {

			IExtension[] extensions = extPoint.getExtensions();

			for (IExtension aExtension : extensions) {
				IConfigurationElement[] configElements = aExtension.getConfigurationElements();
				for (IConfigurationElement aConfig : configElements) {

					id = aConfig.getAttribute(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_ID);
					name = aConfig.getAttribute(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_NAME);

					String nodeClassName = aConfig.getAttribute(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_CLASS);

					AbstractTreeNode treeNode = null;
					if (nodeClassName != null) {

						try {
							treeNode = (AbstractTreeNode) aConfig
									.createExecutableExtension(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_CLASS);
							treeNode.setId(id);
							treeNode.setName(name);
							if (aConfig.getAttribute(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_ICON) != null) {
								ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(
										DbCoreActivator.PLUGIN_ID,
										aConfig.getAttribute(DB_ROOT_NODE_EXTENSION_POINT_ATTRIBUTE_ICON));
								treeNode.setIcon(icon);
							}

							treeNodeList.put(id, treeNode);
						} catch (CoreException e) {
							StudioLogger
									.error(DbRootNodeReader.class,
											"Unexpected error with the root node extension point. Name:" + name + " ID:" + id, e); //$NON-NLS-1$ //$NON-NLS-2$
							failedNodeNames.add(name);
						}
					}
				}
			}

			if (!failedNodeNames.isEmpty()) {
				throw new PartInitException("The following nodes could not be loaded : " //$NON-NLS-1$
						+ failedNodeNames);
			}
		}
	}
}
