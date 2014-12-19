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
 * Abstract class to be used to create nodes that contains only the property
 * "name"
 */
public abstract class AbstractSimpleNameNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_NAME);
	}

	/**
	 * The name property
	 */
	private String propName = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property
	 */
	public AbstractSimpleNameNode(String name) {
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

		if ((propName != null) && (propName.trim().length() > 0)) {
			properties.put(PROP_NAME, propName);
		}

		return properties;
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

	/**
	 * Returns the name property
	 * 
	 * @return the name property
	 */
	public String getName() {
		return propName;
	}

	/**
	 * Sets the name property. This value must not be null.
	 * 
	 * @param name
	 *            the name property
	 */
	public void setName(String name) {
		Assert.isLegal(name != null);
		this.propName = name;
	}
}
