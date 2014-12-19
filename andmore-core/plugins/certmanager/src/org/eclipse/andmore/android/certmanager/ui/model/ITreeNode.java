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
package org.eclipse.andmore.android.certmanager.ui.model;

import java.security.KeyStoreException;
import java.util.List;

import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionFilter;

public interface ITreeNode extends IActionFilter {

	public static final String PROP_VALUE_NODE_STATUS_OK = "org.eclipse.andmore.android.certmanager.core.property.nodeStatusOk"; //$NON-NLS-1$

	public static final String PROP_VALUE_NODE_STATUS_KEYSTORE_TYPE_OK = "org.eclipse.andmore.android.certmanager.core.property.keystoreTypeOk"; //$NON-NLS-1$

	/**
	 * Property value used to check if the node has an error status.
	 */
	public static final String PROP_VALUE_NODE_STATUS_ERROR = "org.eclipse.andmore.android.certmanager.core.property.nodeStatusError"; //$NON-NLS-1$

	public static final String PROP_VALUE_NODE_STATUS_WARNING = "org.eclipse.andmore.android.certmanager.core.property.nodeStatusWarning"; //$NON-NLS-1$

	/**
	 * Property name used to test the status of the node.
	 */
	public static final String PROP_NAME_NODE_STATUS = "org.eclipse.andmore.android.certmanager.core.property.nodeStatus"; //$NON-NLS-1$

	public static final String PROP_NAMESPACE = "org.eclipse.andmore.android.certmanager.core.property";

	/**
	 * Method responsible to reload the node itself and its children
	 */
	void refresh() throws KeyStoreManagerException;

	/**
	 * @return the id
	 */
	String getId();

	/**
	 * @return the name
	 */
	String getName();

	/**
	 * @return the icon
	 */
	ImageDescriptor getIcon();

	/**
	 * @return true if it does not accept a child, false otherwise
	 */
	boolean isLeaf();

	/**
	 * Set the node Status, allowing the tree to decorate itself on errors. Is
	 * status is ERROR the icon will be decorated with a error image and tooltip
	 * will be replaced by status.getMessage() if available.
	 * 
	 * @param status
	 */
	void setNodeStatus(IStatus status);

	/**
	 * Retrieves the current node status.
	 * 
	 * @return
	 */
	IStatus getNodeStatus();

	/**
	 * Set the tooltip to be displayed for this node.
	 * 
	 * @param tooltip
	 */
	void setTooltip(String tooltip);

	/**
	 * @return this node tooltip text
	 */
	String getTooltip();

	/**
	 * Get parent of the tree node
	 * 
	 * @return null if it is the tree root, non-null if is a child node
	 */
	ITreeNode getParent();

	/**
	 * Retrieves list of children (without any filter)
	 * 
	 * @return collection of {@link ITreeNode} that are child of this abstract
	 *         tree node
	 * @throws KeyStoreException
	 * @throws KeyStoreManagerException
	 */
	List<ITreeNode> getChildren() throws KeyStoreManagerException;

	void addChild(ITreeNode newChild) throws KeyStoreManagerException;
}
