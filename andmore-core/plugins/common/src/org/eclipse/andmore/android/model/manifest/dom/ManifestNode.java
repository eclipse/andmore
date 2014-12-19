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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents a <manifest> node on AndroidManifest.xml file
 */
public class ManifestNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_XMLNS);
		defaultProperties.add(PROP_PACKAGE);
		defaultProperties.add(PROP_SHAREDUSERID);
		defaultProperties.add(PROP_VERSIONCODE);
		defaultProperties.add(PROP_VERSIONNAME);
	}

	/**
	 * Default value for xmlns:android property
	 */
	private static final String PROP_XMLNS_DEFAULTVALUE = "http://schemas.android.com/apk/res/android";

	/**
	 * The package property
	 */
	private String propPackage = null;

	/**
	 * The sharedUserId property
	 */
	private String propSharedUserId = null;

	/**
	 * The versionCode property
	 */
	private Integer propVersionCode = null;

	/**
	 * The versionName property
	 */
	private String propVersionName = null;

	public ManifestNode(String packageName) {
		setPackage(packageName);
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
		return (nodeType == NodeType.Application) || (nodeType == NodeType.Instrumentation)
				|| (nodeType == NodeType.Permission) || (nodeType == NodeType.PermissionGroup)
				|| (nodeType == NodeType.PermissionTree) || (nodeType == NodeType.UsesPermission)
				|| (nodeType == NodeType.UsesFeature) || (nodeType == NodeType.UsesSdk)
				|| (nodeType == NodeType.Comment) || (nodeType == NodeType.MetaData) || (nodeType == NodeType.Unknown);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Manifest;
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

		properties.put(PROP_XMLNS, PROP_XMLNS_DEFAULTVALUE);
		properties.put(PROP_PACKAGE, propPackage);

		if (propSharedUserId != null) {
			properties.put(PROP_SHAREDUSERID, propSharedUserId);
		}

		if (propVersionCode != null) {
			properties.put(PROP_VERSIONCODE, propVersionCode.toString());
		}

		if (propVersionName != null) {
			properties.put(PROP_VERSIONNAME, propVersionName);
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
		int applicationCount = 0;
		boolean allAcceptable = true;

		for (AndroidManifestNode child : children) {
			if (child.getNodeType() == NodeType.Application) {
				applicationCount++;
			} else if (!canContains(child.getNodeType())) {
				allAcceptable = false;
				break;
			}
		}

		return allAcceptable && (applicationCount == 1) && isValidPackageName(propPackage);
	}

	/**
	 * Checks if a package name is valid
	 * 
	 * @param packageName
	 *            The package name to be verified
	 * 
	 * @return true if the package name is valid or false otherwise
	 */
	private boolean isValidPackageName(String packageName) {
		boolean isValid = false;

		String[] parts = packageName.split("\\.");

		if (parts.length > 1) {
			isValid = true;

			for (String part : parts) {
				isValid &= part.length() > 0;
			}
		}

		return isValid;
	}

	/**
	 * Sets the package property value.
	 * 
	 * @param packageName
	 *            the package property value
	 */
	public void setPackage(String packageName) {
		Assert.isLegal(packageName != null);
		propPackage = packageName;
	}

	/**
	 * Gets the package property value.
	 * 
	 * @return the package property value
	 */
	public String getPackageName() {
		return propPackage;
	}

	/**
	 * Sets the android:sharedUserId property value. Set it to null if you do
	 * not want to use it.
	 * 
	 * @param sharedUserId
	 *            the sharedUserId property value
	 */
	public void setSharedUserId(String sharedUserId) {
		propSharedUserId = sharedUserId;
	}

	/**
	 * Gets the android:sharedUserId property value.
	 * 
	 * @return the android:sharedUserId property value
	 */
	public String getSharedUserId() {
		return propSharedUserId;
	}

	/**
	 * Sets the android:versionCode property value. Set it to null if you do not
	 * want to use it.
	 * 
	 * @param versionCode
	 *            the versionCode property value
	 */
	public void setVersionCode(Integer versionCode) {
		propVersionCode = versionCode;
	}

	/**
	 * Gets the android:versionCode property value.
	 * 
	 * @return the android:versionCode property value.
	 */
	public Integer getVersionCode() {
		return propVersionCode;
	}

	/**
	 * Sets the android:versionName property value. Set it to null if you do not
	 * want to use it.
	 * 
	 * @param versionName
	 *            the versionName property value
	 */
	public void setVersionName(String versionName) {
		propVersionName = versionName;
	}

	/**
	 * Gets the android:versionName property value.
	 * 
	 * @return the android:versionName property value.
	 */
	public String getVersionName() {
		return propVersionName;
	}

	/**
	 * Retrieves the application node. Creates a new if it does not exist.
	 * 
	 * @return the application node
	 */
	public ApplicationNode getApplicationNode() {
		ApplicationNode applicationNode = null;

		for (AndroidManifestNode appNode : getAllChildrenFromType(NodeType.Application)) {
			applicationNode = (ApplicationNode) appNode;
			break;
		}

		if (applicationNode == null) {
			applicationNode = new ApplicationNode("");
			addChild(applicationNode);
		}

		return applicationNode;
	}

	/**
	 * Adds an Instrumentation Node to the Manifest Node
	 * 
	 * @param instrumentation
	 *            The Instrumentation Node
	 */
	public void addInstrumentationNode(InstrumentationNode instrumentation) {
		if (instrumentation != null) {
			if (!children.contains(instrumentation)) {
				children.add(instrumentation);
			}
		}
	}

	/**
	 * Retrieves all Instrumentation Nodes from the Manifest Node
	 * 
	 * @return all Instrumentation Nodes from the Manifest Node
	 */
	public List<InstrumentationNode> getInstrumentationNodes() {
		List<InstrumentationNode> instrumentations = new LinkedList<InstrumentationNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Instrumentation)) {
			instrumentations.add((InstrumentationNode) node);
		}

		return instrumentations;
	}

	/**
	 * Removes an Instrumentation Node from the Manifest Node
	 * 
	 * @param instrumentation
	 *            the Instrumentation Node to be removed
	 */
	public void removeInstrumentationNode(InstrumentationNode instrumentation) {
		if (instrumentation != null) {
			children.remove(instrumentation);
		}
	}

	/**
	 * Adds a Permission Node to the Manifest Node
	 * 
	 * @param permission
	 *            The Permission Node
	 */
	public void addPermissionNode(PermissionNode permission) {
		if (permission != null) {
			if (!children.contains(permission)) {
				children.add(permission);
			}
		}
	}

	/**
	 * Retrieves all Permission Nodes from the Manifest Node
	 * 
	 * @return all Permission Nodes from the Manifest Node
	 */
	public List<PermissionNode> getPermissionNodes() {
		List<PermissionNode> permissions = new LinkedList<PermissionNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Permission)) {
			permissions.add((PermissionNode) node);
		}

		return permissions;
	}

	/**
	 * Removes a Permission Node from the Manifest Node
	 * 
	 * @param permission
	 *            the Permission Node to be removed
	 */
	public void removePermissionNode(PermissionNode permission) {
		if (permission != null) {
			children.remove(permission);
		}
	}

	/**
	 * Adds a Permission Group Node to the Manifest Node
	 * 
	 * @param permissionGroup
	 *            The Permission Group Node
	 */
	public void addPermissionGroupNode(PermissionGroupNode permissionGroup) {
		if (permissionGroup != null) {
			if (!children.contains(permissionGroup)) {
				children.add(permissionGroup);
			}
		}
	}

	/**
	 * Retrieves all Permission Group Nodes from the Manifest Node
	 * 
	 * @return all Permission Group Nodes from the Manifest Node
	 */
	public List<PermissionGroupNode> getPermissionGroupNodes() {
		List<PermissionGroupNode> permissionGroups = new LinkedList<PermissionGroupNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.PermissionGroup)) {
			permissionGroups.add((PermissionGroupNode) node);
		}

		return permissionGroups;
	}

	/**
	 * Removes a Permission Group Node from the Manifest Node
	 * 
	 * @param permissionGroup
	 *            the Permission Group Node to be removed
	 */
	public void removePermissionGroupNode(PermissionGroupNode permissionGroup) {
		if (permissionGroup != null) {
			children.remove(permissionGroup);
		}
	}

	/**
	 * Adds a Permission Tree Node to the Manifest Node
	 * 
	 * @param permissionTree
	 *            The Permission Tree Node
	 */
	public void addPermissionTreeNode(PermissionTreeNode permissionTree) {
		if (permissionTree != null) {
			if (!children.contains(permissionTree)) {
				children.add(permissionTree);
			}
		}
	}

	/**
	 * Retrieves all Permission Tree Nodes from the Manifest Node
	 * 
	 * @return all Permission Tree Nodes from the Manifest Node
	 */
	public List<PermissionTreeNode> getPermissionTreeNodes() {
		List<PermissionTreeNode> permissionTrees = new LinkedList<PermissionTreeNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.PermissionTree)) {
			permissionTrees.add((PermissionTreeNode) node);
		}

		return permissionTrees;
	}

	/**
	 * Removes a Permission Tree Node from the Manifest Node
	 * 
	 * @param permissionTree
	 *            the Permission Tree Node to be removed
	 */
	public void removePermissionTreeNode(PermissionTreeNode permissionTree) {
		if (permissionTree != null) {
			children.remove(permissionTree);
		}
	}

	/**
	 * Adds an Uses Permission Node to the Manifest Node
	 * 
	 * @param usesPermission
	 *            The Uses Permission Node
	 */
	public void addUsesPermissionNode(UsesPermissionNode usesPermission) {
		if (usesPermission != null) {
			if (!children.contains(usesPermission)) {
				addBeforeApplicationNode(usesPermission);
			}
		}
	}

	/**
	 * Retrieves all Uses Permission Nodes from the Manifest Node
	 * 
	 * @return all Uses Permission Nodes from the Manifest Node
	 */
	public List<UsesPermissionNode> getUsesPermissionNodes() {
		List<UsesPermissionNode> usesPermissions = new LinkedList<UsesPermissionNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.UsesPermission)) {
			usesPermissions.add((UsesPermissionNode) node);
		}

		return usesPermissions;
	}

	/**
	 * Removes an Uses Permission Node from the Manifest Node
	 * 
	 * @param usesPermission
	 *            the Uses Permission Node to be removed
	 */
	public void removeUsesPermissionNode(UsesPermissionNode usesPermission) {
		if (usesPermission != null) {
			children.remove(usesPermission);
		}
	}

	/**
	 * Removes an Used Permission from the Manifest Node. Convenience method to
	 * remove an used permission using its name. Note: all uses-permission nodes
	 * of the given permission are removed.
	 * 
	 * @param permission
	 *            the used permission name to be removed
	 * @return the number of uses-permission nodes removed
	 */
	public int removeUsesPermissionNode(String permission) {
		int nodesRemoved = 0;
		for (UsesPermissionNode node : getUsesPermissionNodes()) {
			if (node.getName().equals(permission)) {
				removeUsesPermissionNode(node);
				nodesRemoved++;
			}
		}
		return nodesRemoved;
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

	/**
	 * Adds an Uses Feature Node to the Manifest Node
	 * 
	 * @param usesFeature
	 *            The Uses Feature Node
	 */
	public void addUsesFeatureNode(UsesFeatureNode usesFeature) {
		if (usesFeature != null) {
			if (!children.contains(usesFeature)) {
				addBeforeApplicationNode(usesFeature);
			}
		}
	}

	/**
	 * Retrieves Uses Feature Node from the Manifest Node
	 * 
	 * @return null if no node if the featureName is found, or the refence to
	 *         the node if it exists
	 */
	public UsesFeatureNode getUsesFeatureNode(String featureName) {
		UsesFeatureNode usesFeature = null;

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.UsesFeature)) {
			if ((node.getNodeProperties() != null) && node.getNodeProperties().containsKey("android:name")
					&& node.getNodeProperties().get("android:name").equals(featureName)) {
				usesFeature = (UsesFeatureNode) node;
				break;
			}
		}

		return usesFeature;
	}

	/**
	 * Retrieves Uses SDK Node from the Manifest Node
	 * 
	 * @return null if no uses-sdk node is found, or the refence to the node if
	 *         it exists
	 */
	public UsesSDKNode getUsesSdkNode() {
		UsesSDKNode usesSDKNode = null;

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.UsesSdk)) {
			if (node instanceof UsesSDKNode) {
				// get the first node - it should NOT have more than once
				usesSDKNode = (UsesSDKNode) node;
				break;
			}
		}

		return usesSDKNode;
	}

	/**
	 * Adds an Uses SDK Node to the Manifest Node
	 * 
	 * @param usesSDK
	 *            The Uses SDK Node
	 */
	public void addUsesSdkNode(UsesSDKNode usesSdk) {
		if (usesSdk != null) {
			if (!children.contains(usesSdk)) {
				addBeforeApplicationNode(usesSdk);
			}
		}
	}

	/**
	 * Adds before application node (if it exists), otherwise adds after
	 * 
	 * @param node
	 */
	private void addBeforeApplicationNode(AndroidManifestNode node) {
		ApplicationNode applicationNode = getApplicationNode();
		if (applicationNode != null) {
			int appNodeIdx = children.indexOf(applicationNode);
			children.add(appNodeIdx, node);
		} else {
			children.add(node);
		}
	}
}
