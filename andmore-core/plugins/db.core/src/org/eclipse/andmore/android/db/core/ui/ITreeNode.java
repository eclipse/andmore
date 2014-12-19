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
package org.eclipse.andmore.android.db.core.ui;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionFilter;

/**
 * This interface represents a db tree node.
 */
public interface ITreeNode extends IActionFilter {

	/**
	 * Method responsible to reload the node itself and its children
	 */
	void refresh();

	/**
	 * Get parent of the tree node
	 * 
	 * @return null if it is the tree root, non-null if is a child node
	 */
	ITreeNode getParent();

	/**
	 * @param parent
	 *            null if it is the tree root, non-null if is a child node
	 */
	void setParent(ITreeNode parent);

	/**
	 * Retrieves list of children (without any filter)
	 * 
	 * @return collection of {@link AbstractTreeNode} that are child of this
	 *         abstract tree node
	 */
	List<ITreeNode> getChildren();

	/**
	 * Clear the children (e.g. before reloading again)
	 */
	void clear();

	/**
	 * Retrieve the child for the given node index
	 * 
	 * @param index
	 * @return {@link AbstractTreeNode} if child with the given index was found,
	 *         null if node not found as a child
	 */
	ITreeNode getChild(int index);

	/**
	 * Retrieve the child for the given node ID
	 * 
	 * @param id
	 *            node ID as specified in the extension point
	 *            org.eclipse.andmore.android.db.core.dbRootNode
	 * @return {@link AbstractTreeNode} if child with the given ID was found,
	 *         null if node not found as a child
	 */
	ITreeNode getChildById(String id);

	/**
	 * Get list of children nodes that matches a regular expression
	 * 
	 * @param regex
	 *            regular expression to filter nodes, see {@link Pattern} for
	 *            the constructs if null, returns all items
	 * @return list of {@link AbstractTreeNode} that matches the view filter
	 */
	List<ITreeNode> getFilteredChildren(String regex);

	void putChild(ITreeNode treeNode);

	void putChildren(List<ITreeNode> childrenList);

	/**
	 * Remove a child with the given node
	 * 
	 * @param node
	 */
	void removeChild(ITreeNode node);

	/**
	 * @return true if node is getting data to be added in the tree, false
	 *         otherwise
	 */
	boolean isLoading();

	/**
	 * @param isLoading
	 *            true if node is getting data to be added in the tree, false
	 *            otherwise
	 */
	void setLoading(boolean isLoading);

	/**
	 * @return the id
	 */
	String getId();

	/**
	 * @param id
	 *            the id to set
	 */
	void setId(String id);

	/**
	 * @return the name
	 */
	String getName();

	/**
	 * @param name
	 *            the name to set
	 */
	void setName(String name);

	/**
	 * @return the icon
	 */
	ImageDescriptor getIcon();

	/**
	 * @param icon
	 *            the icon to set
	 */
	void setIcon(ImageDescriptor icon);

	/**
	 * @return the canRefresh
	 */
	IStatus canRefresh();

	/**
	 * @return true if it does not accept a child, false otherwise
	 */
	boolean isLeaf();

	/**
	 * Refreshes this node in a background task
	 * 
	 * @param canRefreshInput
	 */
	void refreshAsync();

	/**
	 * Refreshes this node in a background task.
	 * 
	 * @param canRefreshInput
	 *            is an optional parameter, is intended to be set used by
	 *            refresh handler.
	 */
	void refreshAsync(boolean canRefreshYesResponse);

	/**
	 * Clean method is intended to be called right before removing this node
	 * from it's parent. All resource cleaning must be done in this method.
	 */
	void cleanUp();

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

}