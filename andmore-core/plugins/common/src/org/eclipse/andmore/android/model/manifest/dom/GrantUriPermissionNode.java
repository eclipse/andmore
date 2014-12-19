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

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a <grant-uri-permission> node on AndroidManifest.xml
 * file
 */
public class GrantUriPermissionNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_PATH);
		defaultProperties.add(PROP_PATHPREFIX);
		defaultProperties.add(PROP_PATHPATTERN);
	}

	/**
	 * The path property
	 */
	private String propPath = null;

	/**
	 * The pathPrefix property
	 */
	private String propPathPrefix = null;

	/**
	 * The pathPattern property
	 */
	private String propPathPattern = null;

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

		if ((propPath != null) && (propPath.trim().length() > 0)) {
			properties.put(PROP_PATH, propPath);
		}

		if ((propPathPattern != null) && (propPathPattern.trim().length() > 0)) {
			properties.put(PROP_PATHPATTERN, propPathPattern);
		}

		if ((propPathPrefix != null) && (propPathPrefix.trim().length() > 0)) {
			properties.put(PROP_PATHPREFIX, propPathPrefix);
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
		return NodeType.GrantUriPermission;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return ((propPath != null) && (propPath.trim().length() > 0))
				|| ((propPathPattern != null) && (propPathPattern.trim().length() > 0))
				|| ((propPathPrefix != null) && (propPathPrefix.trim().length() > 0));
	}

	/**
	 * Gets the path property value
	 * 
	 * @return the path property value
	 */
	public String getPath() {
		return propPath;
	}

	/**
	 * Sets the path property value. Set it to null to remove it.
	 * 
	 * @param path
	 *            the path property value
	 */
	public void setPath(String path) {
		this.propPath = path;
	}

	/**
	 * Gets the pathPrefix property value
	 * 
	 * @return the pathPrefix property value
	 */
	public String getPathPrefix() {
		return propPathPrefix;
	}

	/**
	 * Sets the pathPrefix property value. Set it to null to remove it.
	 * 
	 * @param pathPrefix
	 *            the pathPrefix property value
	 */
	public void setPathPrefix(String pathPrefix) {
		this.propPathPrefix = pathPrefix;
	}

	/**
	 * Gets the pathPattern property value
	 * 
	 * @return the pathPattern property value
	 */
	public String getPathPattern() {
		return propPathPattern;
	}

	/**
	 * Sets the pathPattern property value. Set it to null to remove it.
	 * 
	 * @param pathPattern
	 *            the pathPattern property value
	 */
	public void setPathPattern(String pathPattern) {
		this.propPathPattern = pathPattern;
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
