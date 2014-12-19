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
package org.eclipse.andmore.android.model.resources.types;

import org.eclipse.core.runtime.Assert;

/**
 * Class that represents an unknown node on a resource file
 */
public class UnknownNode extends AbstractResourceNode {
	/**
	 * The node name: <nodename attrName="attrValue">nodeValue</nodename>
	 */
	private String nodeName;

	/**
	 * The node value: <nodename attrName="attrValue">nodeValue</nodename>
	 */
	private String nodeValue;

	/**
	 * Default constructor
	 * 
	 * @param nodeName
	 *            The node name. It must not be null.
	 */
	public UnknownNode(String nodeName) {
		Assert.isLegal(nodeName != null);
		this.nodeName = nodeName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.resources.AbstractResourceNode#
	 * canAddChildNode
	 * (com.motorola.studio.android.model.resources.AbstractResourceNode)
	 */
	@Override
	protected boolean canAddChildNode(AbstractResourceNode node) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.resources.AbstractResourceNode#getNodeName
	 * ()
	 */
	@Override
	public String getNodeName() {
		return nodeName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.resources.AbstractResourceNode#getNodeType
	 * ()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Unknown;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.resources.AbstractResourceNode#
	 * isAttributeValid(java.lang.String)
	 */
	@Override
	protected boolean isAttributeValid(String attributeName) {
		return false;
	}

	/**
	 * Sets the node value: <nodename attrName="attrValue">nodeValue</nodename>
	 * 
	 * @param value
	 *            The node value
	 */
	public void setNodeValue(String value) {
		this.nodeValue = value;
	}

	/**
	 * Retrieves the node value: <nodename
	 * attrName="attrValue">nodeValue</nodename>
	 * 
	 * @return The node value
	 */
	public String getNodeValue() {
		return nodeValue;
	}
}
