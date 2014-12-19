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

import java.util.Map;

import org.eclipse.core.runtime.Assert;

/**
 * Abstract class to be used to create nodes that contains the properties
 * "icon", "label" and "name"
 */
public abstract class AbstractIconLabelNameNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		// Adds the node properties to the list
		defaultProperties.add(PROP_ICON);
		defaultProperties.add(PROP_LABEL);
		defaultProperties.add(PROP_NAME);
	}

	/**
	 * The icon property
	 */
	protected String propIcon = null;

	/**
	 * The label property
	 */
	protected String propLabel = null;

	/**
	 * The name property
	 */
	protected String propName = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The name property. It must not be null.
	 * @param newProperties
	 *            The new properties that are accepted by the child class
	 */
	protected AbstractIconLabelNameNode(String name) {
		Assert.isLegal(name != null);

		this.propName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return propName.trim().length() > 0;
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

		if ((propName != null) && (propName.trim().length() > 0)) {
			properties.put(PROP_NAME, propName);
		}

		if ((propIcon != null) && (propIcon.trim().length() > 0)) {
			properties.put(PROP_ICON, propIcon);
		}

		if ((propLabel != null) && (propLabel.trim().length() > 0)) {
			properties.put(PROP_LABEL, propLabel);
		}

		addAdditionalProperties();

		return properties;
	}

	/**
	 * Adds the additional properties to the properties variable
	 */
	protected abstract void addAdditionalProperties();

	/**
	 * Gets the icon property value
	 * 
	 * @return the icon property value
	 */
	public String getIcon() {
		return propIcon;
	}

	/**
	 * Sets the icon property value. Set it to null to remove it.
	 * 
	 * @param icon
	 *            the icon property value
	 */
	public void setIcon(String icon) {
		this.propIcon = icon;
	}

	/**
	 * Gets the label property value
	 * 
	 * @return the label property value
	 */
	public String getLabel() {
		return propLabel;
	}

	/**
	 * Sets the label property value. Set it to null to remove it.
	 * 
	 * @param label
	 *            the label property value
	 */
	public void setLabel(String label) {
		this.propLabel = label;
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
	 * Sets the name property value. It must not be set to null.
	 * 
	 * @param name
	 *            the name property value
	 */
	public void setName(String name) {
		Assert.isLegal(name != null);
		this.propName = name;
	}
}
