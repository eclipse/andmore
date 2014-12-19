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
 * Class that represents a <meta-data> node on AndroidManifest.xml file
 */
public class MetadataNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_NAME);
		defaultProperties.add(PROP_RESOURCE);
		defaultProperties.add(PROP_VALUE);
	}

	/**
	 * The name property
	 */
	private String propName = null;

	/**
	 * The resource property
	 */
	private String propResource = null;

	/**
	 * The value property
	 */
	private String propValue = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property. It must not be null.
	 */
	public MetadataNode(String name) {
		Assert.isLegal(name != null);
		this.propName = name;
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
		// Always returns false. This node can not contain children.
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeProperties()
	 */
	@Override
	public Map<String, String> getNodeProperties() {
		properties.clear();

		properties.put(PROP_NAME, propName);

		if ((propResource != null) && (propResource.trim().length() > 0)) {
			properties.put(PROP_RESOURCE, propResource);
		}

		if ((propValue != null) && (propValue.length() > 0)) {
			properties.put(PROP_VALUE, propValue);
		}

		return properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.MetaData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		boolean containsOnlyResource = (propResource != null) && (propValue == null);
		boolean containsOnlyValue = (propResource == null) && (propValue != null);

		return (propName.trim().length() > 0) && (containsOnlyResource || containsOnlyValue);
	}

	/**
	 * Gets the name property value
	 * 
	 * @return the name property value
	 */
	public String getName() {
		return propName;
	}

	/**
	 * Sets the name property value. This property must not be null.
	 * 
	 * @param name
	 *            the name property value
	 */
	public void setName(String name) {
		Assert.isLegal(name != null);
		this.propName = name;
	}

	/**
	 * Gets the resource property value
	 * 
	 * @return the resource property value
	 */
	public String getResource() {
		return propResource;
	}

	/**
	 * Sets the resource property value. This property must not be null.
	 * 
	 * @param resource
	 *            the resource property value
	 */
	public void setResource(String resource) {
		this.propResource = resource;
	}

	/**
	 * Gets the value property value
	 * 
	 * @return the value property value
	 */
	public String getValue() {
		return propValue;
	}

	/**
	 * Sets the value property value. This property must not be null.
	 * 
	 * @param value
	 *            the value property value
	 */
	public void setValue(String value) {
		this.propValue = value;
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
