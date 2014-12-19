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
 */package org.eclipse.andmore.android.model.manifest.dom;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a <uses-sdk> node on AndroidManifest.xml file
 */
public class UsesSDKNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_MINSDKVERSION);
		defaultProperties.add(PROP_MAXSDKVERSION);
		defaultProperties.add(PROP_TARGETSDKVERSION);
	}

	/**
	 * The minSdkVersion property
	 */
	private String propMinSdkVersion = null;

	/**
	 * The maxSdkVersion property
	 */
	private String propMaxSdkVersion = null;

	/**
	 * The targetSdkVersion property
	 */
	private String propTargetSdkVersion = null;

	protected static final String INVALID_VERSION_VALUE = "";

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
		if (propMinSdkVersion != null) {
			properties.put(PROP_MINSDKVERSION, propMinSdkVersion);
		}
		if (propMaxSdkVersion != null) {
			properties.put(PROP_MAXSDKVERSION, propMaxSdkVersion);
		}
		if (propTargetSdkVersion != null) {
			properties.put(PROP_TARGETSDKVERSION, propTargetSdkVersion);
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
		return NodeType.UsesSdk;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return (propMinSdkVersion != null) && (!INVALID_VERSION_VALUE.equals(propMinSdkVersion));
	}

	/**
	 * Gets the minSdkVersion property value
	 * 
	 * @return the minSdkVersion property value
	 */
	public String getMinSdkVersion() {
		return propMinSdkVersion != null ? propMinSdkVersion : INVALID_VERSION_VALUE;
	}

	/**
	 * Sets the minSdkVersion property value. Set it to null to remove it.
	 * 
	 * @param minSdkVersion
	 *            the minSdkVersion property value
	 */
	public void setMinSdkVersion(String minSdkVersion) {
		this.propMinSdkVersion = minSdkVersion;
	}

	/**
	 * Sets the maxSdkVersion property value. Set it to null to remove it.
	 * 
	 * @param maxSdkVersion
	 *            the maxSdkVersion property value
	 */
	public void setPropMaxSdkVersion(String propMaxSdkVersion) {
		this.propMaxSdkVersion = propMaxSdkVersion;
	}

	/**
	 * Sets the targetSdkVersion property value. Set it to null to remove it.
	 * 
	 * @param targetSdkVersion
	 *            the targetSdkVersion property value
	 */
	public void setPropTargetSdkVersion(String propTargetSdkVersion) {
		this.propTargetSdkVersion = propTargetSdkVersion;
	}

	public String getPropMinSdkVersion() {
		return propMinSdkVersion;
	}

	public String getPropMaxSdkVersion() {
		return propMaxSdkVersion;
	}

	public String getPropTargetSdkVersion() {
		return propTargetSdkVersion;
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
