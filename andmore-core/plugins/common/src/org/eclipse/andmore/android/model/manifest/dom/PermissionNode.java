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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a <permission> node on AndroidManifest.xml file
 */
public class PermissionNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_DESCRIPTION);
		defaultProperties.add(PROP_PERMISSIONGROUP);
		defaultProperties.add(PROP_PROTECTIONLEVEL);
	}

	/**
	 * Enumeration for protectionLevel property
	 */
	public enum ProtectionLevel {
		normal, dangerous, signature, signatureOrSystem
	}

	/**
	 * Map to resolve the string<->enumeration association of protectionLevel
	 * property
	 */
	private static Map<String, ProtectionLevel> protectionLevelMap;

	static {
		// Loads the map for protectionLevel
		protectionLevelMap = new HashMap<String, ProtectionLevel>();
		protectionLevelMap.put(ProtectionLevel.normal.toString().toLowerCase(), ProtectionLevel.normal);
		protectionLevelMap.put(ProtectionLevel.dangerous.toString().toLowerCase(), ProtectionLevel.dangerous);
		protectionLevelMap.put(ProtectionLevel.signature.toString().toLowerCase(), ProtectionLevel.signature);
		protectionLevelMap.put(ProtectionLevel.signatureOrSystem.toString().toLowerCase(),
				ProtectionLevel.signatureOrSystem);
	}

	/**
	 * Gets the enumeration value from the ProtectionLevel enumeration from a
	 * given name
	 * 
	 * @param name
	 *            the protectionLevel name
	 * @return the enumeration value from ProtectionLevel enumeration
	 */
	public static ProtectionLevel getProtectionLevel(String name) {
		ProtectionLevel protectionLevel = null;

		if (name != null) {
			String pl = name.trim().toLowerCase();
			protectionLevel = protectionLevelMap.get(pl);
		}

		return protectionLevel;
	}

	/**
	 * Gets the protectionLevel parameter name from a given ProtectionLevel
	 * enumeration value
	 * 
	 * @param protectionLevel
	 *            the enumeration value
	 * @return the parameter name
	 */
	public static String getProtectionLevelName(ProtectionLevel protectionLevel) {
		String name = "";

		if (protectionLevel != null) {
			name = protectionLevel.toString();
		}

		return name;
	}

	/**
	 * The description property
	 */
	private String propDescription = null;

	/**
	 * The permissionGroup property
	 */
	private String propPermissionGroup = null;

	/**
	 * The protectionLevel property
	 */
	private ProtectionLevel propProtectionLevel = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property. It must not be set to null
	 */
	public PermissionNode(String name) {
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
		String protectionLevelName = getProtectionLevelName(propProtectionLevel);
		properties.put(PROP_PROTECTIONLEVEL, protectionLevelName);

		if ((propDescription != null) && (propDescription.length() > 0)) {
			properties.put(PROP_DESCRIPTION, propDescription);
		}

		if ((propPermissionGroup != null) && (propPermissionGroup.trim().length() > 0)) {
			properties.put(PROP_PERMISSIONGROUP, propPermissionGroup);
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
		return NodeType.Permission;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		boolean isProtectionLevelValid = propProtectionLevel != null;

		return super.isNodeValid() && isProtectionLevelValid;
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

	/**
	 * Gets the permissionGroup property value
	 * 
	 * @return the permissionGroup property value
	 */
	public String getPermissionGroup() {
		return propPermissionGroup;
	}

	/**
	 * Sets the permissionGroup property value. Set it to null to remove it.
	 * 
	 * @param % the permissionGroup property value
	 */
	public void setPermissionGroup(String permissionGroup) {
		this.propPermissionGroup = permissionGroup;
	}

	/**
	 * Gets the protectionLevel property value
	 * 
	 * @return the protectionLevel property value
	 */
	public ProtectionLevel getProtectionLevel() {
		return propProtectionLevel;
	}

	/**
	 * Sets the protectionLevel property value. Set it to null to remove it.
	 * 
	 * @param % the protectionLevel property value
	 */
	public void setProtectionLevel(ProtectionLevel protectionLevel) {
		this.propProtectionLevel = protectionLevel;
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
