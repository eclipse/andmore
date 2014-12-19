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

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a <permission-group> node on AndroidManifest.xml file
 */
public class PermissionGroupNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_DESCRIPTION);
	}

	/**
	 * The description property
	 */
	private String propDescription = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property. It must not be null;
	 */
	public PermissionGroupNode(String name) {
		super(name);
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
	 * @see
	 * com.motorola.studio.android.model.manifest.dom.AbstractIconLabelNameNode
	 * #addAdditionalProperties()
	 */
	@Override
	protected void addAdditionalProperties() {
		if ((propDescription != null) && (propDescription.length() > 0)) {
			properties.put(PROP_DESCRIPTION, propDescription);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.PermissionGroup;
	}

	/**
	 * Gets the description property value
	 * 
	 * @return the description property value
	 */
	public String getDescription() {
		return propDescription;
	}

	/**
	 * Sets the description property value. Set it to null to remove it.
	 * 
	 * @param description
	 *            the description property value
	 */
	public void setDescription(String description) {
		this.propDescription = description;
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
