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
 * Abstract class used to describe the behavior of resource entries on a
 * resource file. Every resource entry (string, drawable, dimen and color) have
 * the same format and an abstract class can describe their behavior:
 * 
 * <resourcenode name="resource name">resource value</resourcenode>
 */
public abstract class AbstractSimpleNameResourceNode extends AbstractResourceNode implements IResourceTypesAttributes {
	/**
	 * The node value
	 */
	protected String nodeValue;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The node name. It must not be null.
	 */
	public AbstractSimpleNameResourceNode(String name) {
		Assert.isLegal(name != null);
		attributes.put(ATTR_NAME, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.resources.types.AbstractResourceNode
	 * #canAddChildNode
	 * (com.motorola.studio.android.model.resources.types.AbstractResourceNode)
	 */
	@Override
	protected boolean canAddChildNode(AbstractResourceNode node) {
		// No child nodes are allowed
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.resources.types.AbstractResourceNode
	 * #isAttributeValid(java.lang.String)
	 */
	@Override
	protected boolean isAttributeValid(String attributeName) {
		return attributeName.equalsIgnoreCase(ATTR_NAME);
	}

	/**
	 * Retrieves the value of name property. <resourcenode
	 * name="resourceName">resource value</resourcenode>
	 * 
	 * @return the value of name property
	 */
	public String getName() {
		return attributes.get(ATTR_NAME);
	}

	/**
	 * Sets the value of name property. <resourcenode
	 * name="resourceName">resource value</resourcenode>
	 * 
	 * @param name
	 *            the value of name property
	 */
	public void setName(String name) {
		Assert.isLegal(name != null);

		attributes.put(ATTR_NAME, name);
	}

	/**
	 * Retrieves the resource value. <resourcenode name="resourceName">resource
	 * value</resourcenode>
	 * 
	 * @return the resource value
	 */
	public String getNodeValue() {
		return nodeValue;
	}

	/**
	 * Sets the resource value. <resourcenode name="resourceName">resource
	 * value</resourcenode>
	 * 
	 * @param nodeValue
	 *            the resource value
	 */
	public void setNodeValue(String nodeValue) {
		this.nodeValue = nodeValue;
	}
}
