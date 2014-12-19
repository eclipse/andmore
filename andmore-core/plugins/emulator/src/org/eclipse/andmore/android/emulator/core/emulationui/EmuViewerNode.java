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
package org.eclipse.andmore.android.emulator.core.emulationui;

import java.util.HashSet;
import java.util.Set;

/**
 * DESCRIPTION: This class represents a node in the tree presented in a
 * emulation view
 *
 * RESPONSIBILITY: Guarantee the tree structure by maintaining the parent/child
 * relationship
 *
 * COLABORATORS: None.
 *
 * USAGE: A class should construct an instance of this class whenever it wishes
 * to add a node to an emulation view tree
 */
public class EmuViewerNode {
	/**
	 * The parent node of this node
	 */
	private final EmuViewerNode parent;

	/**
	 * An id that identifies the node type. The id meaning is defined by the
	 * user
	 */
	private final String nodeId;

	/**
	 * The error message to use as the node label If <code>null</code>, use
	 * regular label resolution
	 */
	private String errorMessage = null;

	/**
	 * A set containing all children of this node
	 */
	private final Set<EmuViewerNode> children = new HashSet<EmuViewerNode>();

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The parent node of this node
	 * @param nodeId
	 *            An id that identifies the node type
	 */
	public EmuViewerNode(EmuViewerNode parent, String nodeId) {
		this.parent = parent;
		this.nodeId = nodeId;
	}

	/**
	 * Retrieves the node's parent
	 * 
	 * @return The parent node
	 */
	public EmuViewerNode getParent() {
		return parent;
	}

	/**
	 * Retrieves the id of this node. The id meaning is defined by the class
	 * user
	 * 
	 * @return The id of this node
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Adds a new child to this node
	 * 
	 * @param child
	 *            The child to be added to the node
	 */
	public void addChild(EmuViewerNode child) {
		children.add(child);
	}

	/**
	 * Retrieves all this node's children
	 * 
	 * @return A set containing all children of this node
	 */
	public Set<EmuViewerNode> getChildren() {
		return children;
	}

	/**
	 * Sets an error message to display as the node label. If <code>null</code>,
	 * the regular label resolution is used
	 * 
	 * @param errorMessage
	 *            An error message to display or <code>null</code> if it is
	 *            desired to have regular label resolution for this node
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * Tests if this node has an error message assigned
	 * 
	 * @return True if an error message was assigned; false otherwise
	 */
	public boolean hasErrorMessage() {
		return errorMessage != null;
	}

	/**
	 * Retrieves the error message assigned to this node
	 * 
	 * @return The error message, or <code>null</code> if no error message was
	 *         assigned
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// For generic/intermediate nodes use the node id itself
		return getNodeId();
	}
}
