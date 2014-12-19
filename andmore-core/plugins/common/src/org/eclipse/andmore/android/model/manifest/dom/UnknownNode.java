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
package org.eclipse.andmore.android.model.manifest.dom;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a unknown nodes found on AndroidManifest.xml file
 */
public class UnknownNode extends AndroidManifestNode {
	/**
	 * The node name
	 */
	private String nodeName = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The node name. It must not be null.
	 */
	public UnknownNode(String name) {
		Assert.isLegal(name != null);
		this.nodeName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * canContains
	 * (com.motorola.studio.android.model.manifest.dom.AndroidManifestNode
	 * .NodeType)
	 */
	@Override
	protected boolean canContains(NodeType nodeType) {
		// If we don't know the node type, it is invalid and we
		// should keep all information
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Unknown;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeProperties()
	 */
	@Override
	public Map<String, String> getNodeProperties() {
		// An unknown node does not have formal properties
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		// An unknown node is always valid
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * canAddUnknownProperty(java.lang.String)
	 */
	@Override
	public boolean canAddUnknownProperty(String property) {
		// It always possible to add unknown properties
		return true;
	}

	/**
	 * Sets the node name
	 * 
	 * @param name
	 *            The node name
	 */
	public void setNodeName(String name) {
		Assert.isLegal(name != null);
		this.nodeName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeName()
	 */
	@Override
	public String getNodeName() {
		return nodeName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getSpecificNodeErrors()
	 */
	@Override
	protected List<IStatus> getSpecificNodeProblems() {
		return null;
	}
}
