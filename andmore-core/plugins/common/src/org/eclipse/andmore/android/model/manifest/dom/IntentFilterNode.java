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

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents an <intent-filter> node on AndroidManifest.xml file
 */
public class IntentFilterNode extends AndroidManifestNode implements IAndroidManifestProperties {
	static {
		defaultProperties.add(PROP_ICON);
		defaultProperties.add(PROP_LABEL);
		defaultProperties.add(PROP_PRIORITY);
	}

	/**
	 * The icon property
	 */
	private String propIcon = null;

	/**
	 * The label property
	 */
	private String propLabel = null;

	/**
	 * The priority property
	 */
	private Integer propPriority = null;

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
		return (nodeType == NodeType.Action) || (nodeType == NodeType.Category) || (nodeType == NodeType.Data)
				|| (nodeType == NodeType.Comment);
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

		if ((propIcon != null) && (propIcon.trim().length() > 0)) {
			properties.put(PROP_ICON, propIcon);
		}

		if ((propLabel != null) && (propLabel.length() > 0)) {
			properties.put(PROP_LABEL, propLabel);
		}

		if (propPriority != null) {
			properties.put(PROP_PRIORITY, propPriority.toString());
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
		return NodeType.IntentFilter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		AndroidManifestNode[] actions = getAllChildrenFromType(NodeType.Action);

		return actions.length > 0;
	}

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
	 * Gets the priority property value
	 * 
	 * @return the priority property value
	 */
	public Integer getPriority() {
		return propPriority;
	}

	/**
	 * Sets the priority property value. Set it to null to remove it.
	 * 
	 * @param priority
	 *            the priority property value
	 */
	public void setPriority(Integer priority) {
		this.propPriority = priority;
	}

	/**
	 * Adds an Action Node to the Intent Filter Node
	 * 
	 * @param action
	 *            The Action Node
	 */
	public void addActionNode(ActionNode action) {
		if (action != null) {
			if (!children.contains(action)) {
				children.add(action);
			}
		}
	}

	/**
	 * Retrieves all Action Nodes from the Intent Filter Node
	 * 
	 * @return all Action Nodes from the Intent Filter Node
	 */
	public List<ActionNode> getActionNodes() {
		List<ActionNode> actions = new LinkedList<ActionNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Action)) {
			actions.add((ActionNode) node);
		}

		return actions;
	}

	/**
	 * Removes an Action Node from the Intent Filter Node
	 * 
	 * @param action
	 *            the Action Node to be removed
	 */
	public void removeActionNode(ActionNode action) {
		if (action != null) {
			children.remove(action);
		}
	}

	/**
	 * Adds a Category Node to the Intent Filter Node
	 * 
	 * @param category
	 *            The Category Node
	 */
	public void addCategoryNode(CategoryNode category) {
		if (category != null) {
			if (!children.contains(category)) {
				children.add(category);
			}
		}
	}

	/**
	 * Adds a Category Node to the Intent Filter Node
	 * 
	 * @param category
	 *            The Category Node
	 */
	public void addUsesPermissionNode(UsesPermissionNode permission) {
		if (permission != null) {
			if (!children.contains(permission)) {
				children.add(permission);
			}
		}
	}

	/**
	 * Retrieves all Category Nodes from the Intent Filter Node
	 * 
	 * @return all Category Nodes from the Intent Filter Node
	 */
	public List<CategoryNode> getCategoryNodes() {
		List<CategoryNode> categories = new LinkedList<CategoryNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Category)) {
			categories.add((CategoryNode) node);
		}

		return categories;
	}

	/**
	 * Retrieves all Uses permission Nodes from the Intent Filter Node
	 * 
	 * @return all Category Nodes from the Intent Filter Node
	 */
	public List<UsesPermissionNode> getUsesPermissionNodes() {
		List<UsesPermissionNode> permissions = new LinkedList<UsesPermissionNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.UsesPermission)) {
			permissions.add((UsesPermissionNode) node);
		}

		return permissions;
	}

	/**
	 * Removes a Category Node from the Intent Filter Node
	 * 
	 * @param category
	 *            the Category Node to be removed
	 */
	public void removeCategoryNode(CategoryNode category) {
		if (category != null) {
			children.remove(category);
		}
	}

	/**
	 * Removes a UsesPermission Node from the Intent Filter Node
	 * 
	 * @param permission
	 *            the Category Node to be removed
	 */
	public void removeUsesPermissionNode(UsesPermissionNode permission) {
		if (permission != null) {
			children.remove(permission);
		}
	}

	/**
	 * Adds a Data Node to the Intent Filter Node
	 * 
	 * @param data
	 *            The Data Node
	 */
	public void addDataNode(DataNode data) {
		if (data != null) {
			if (!children.contains(data)) {
				children.add(data);
			}
		}
	}

	/**
	 * Retrieves all Data Nodes from the Intent Filter Node
	 * 
	 * @return all Data Nodes from the Intent Filter Node
	 */
	public List<DataNode> getDataNodes() {
		List<DataNode> datas = new LinkedList<DataNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Data)) {
			datas.add((DataNode) node);
		}

		return datas;
	}

	/**
	 * Removes a Data Node from the Intent Filter Node
	 * 
	 * @param data
	 *            the Data Node to be removed
	 */
	public void removeDataNode(DataNode data) {
		if (data != null) {
			children.remove(data);
		}
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
	 * Check if this intent-filter node has any information or if it is empty.
	 * 
	 * @return True if this node has no information, false otherwise.
	 * */
	public boolean isEmpty() {
		// an intent-filter node may have action nodes, category nodes, data
		// nodes or attributes (properties).
		return getActionNodes().isEmpty() && getCategoryNodes().isEmpty() && getDataNodes().isEmpty()
				&& getNodeProperties().isEmpty();
	}

	public ActionNode getActionNode(String name) {
		ActionNode result = null;

		// iterate over action nodes
		for (ActionNode actionNode : getActionNodes()) {
			if (actionNode.getName().equals(name)) {
				result = actionNode;
			}
		}

		return result;
	}

	public CategoryNode getCategoryNode(String name) {
		CategoryNode result = null;

		// iterate over action nodes
		for (CategoryNode categoryNode : getCategoryNodes()) {
			if (categoryNode.getName().equals(name)) {
				result = categoryNode;
			}
		}

		return result;
	}

}
