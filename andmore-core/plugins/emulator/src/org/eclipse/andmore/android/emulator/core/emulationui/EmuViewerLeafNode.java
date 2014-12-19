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

/**
 * DESCRIPTION: This is a utility class used to represent a leaf node of the
 * emulation views tree viewer
 *
 * RESPONSIBILITY: Guarantee that no children is added to this leaf node
 *
 * COLABORATORS: None.
 *
 * USAGE: A class should construct an instance of this class whenever it wishes
 * to add a leaf node to an emulation view tree
 */
public abstract class EmuViewerLeafNode extends EmuViewerNode {
	/**
	 * Constructor
	 * 
	 * @see EmuViewerNode#EmuViewerNode(EmuViewerNode, String)
	 */
	public EmuViewerLeafNode(EmuViewerNode parent, String nodeId) {
		super(parent, nodeId);
	}

	/**
	 * @see EmuViewerNode#addChild(EmuViewerNode)
	 */
	@Override
	public void addChild(EmuViewerNode child) {
		// Do nothing
	}

	/**
	 * Retrieves the id that identifies the bean that provides data to this node
	 * 
	 * @return The bean identifier
	 */
	public abstract long getBeanId();

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// For leaf nodes, identify which is the concrete class name and append
		// to that name
		// the associated bean id
		return getClass().getSimpleName() + ":" + getBeanId();
	}
}
