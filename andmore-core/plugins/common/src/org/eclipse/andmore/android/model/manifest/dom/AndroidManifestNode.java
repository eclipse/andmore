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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class that represents a xml node on AndroidManifest.xml file.
 */
public abstract class AndroidManifestNode {
	/**
	 * List that contains the node properties
	 */
	protected static final List<String> defaultProperties = new LinkedList<String>();

	/**
	 * Array that contains all node properties
	 */
	private String[] ALL_PROPERTIES = null;

	/**
	 * Enumeration to identify all types of xml nodes
	 */
	public enum NodeType {
		Action, Activity, ActivityAlias, Application, Category, Data, GrantUriPermission, Instrumentation, IntentFilter, Manifest, MetaData, Permission, PermissionGroup, PermissionTree, Provider, Receiver, Service, UsesLibrary, UsesPermission, UsesSdk, Comment, Unknown, UsesFeature
	}

	/**
	 * Retrieves the node name from its type. This name is the same as shown on
	 * AndroidManifest.xml file
	 * 
	 * @param nodeType
	 *            The node type
	 * 
	 * @return the node name
	 */
	public static String getNodeName(NodeType nodeType) {
		String nodeName;

		switch (nodeType) {
		case Action:
			nodeName = "action";
			break;
		case Activity:
			nodeName = "activity";
			break;
		case ActivityAlias:
			nodeName = "activity-alias";
			break;
		case Application:
			nodeName = "application";
			break;
		case Category:
			nodeName = "category";
			break;
		case Data:
			nodeName = "data";
			break;
		case GrantUriPermission:
			nodeName = "grant-uri-permission";
			break;
		case Instrumentation:
			nodeName = "instrumentation";
			break;
		case IntentFilter:
			nodeName = "intent-filter";
			break;
		case Manifest:
			nodeName = "manifest";
			break;
		case MetaData:
			nodeName = "meta-data";
			break;
		case Permission:
			nodeName = "permission";
			break;
		case PermissionGroup:
			nodeName = "permission-group";
			break;
		case PermissionTree:
			nodeName = "permission-tree";
			break;
		case Provider:
			nodeName = "provider";
			break;
		case Receiver:
			nodeName = "receiver";
			break;
		case Service:
			nodeName = "service";
			break;
		case UsesLibrary:
			nodeName = "uses-library";
			break;
		case UsesPermission:
			nodeName = "uses-permission";
			break;
		case UsesSdk:
			nodeName = "uses-sdk";
			break;
		case Comment:
			nodeName = "comment";
			break;
		case UsesFeature:
			nodeName = "uses-feature";
			break;
		default:
			nodeName = "unknown";
		}

		return nodeName;
	}

	/**
	 * All valid children nodes
	 */
	protected final List<AndroidManifestNode> children = new LinkedList<AndroidManifestNode>();

	/**
	 * All valid parent nodes
	 */
	protected AndroidManifestNode parent = null;

	/**
	 * All valid node properties
	 */
	protected final Map<String, String> properties = new HashMap<String, String>();

	/**
	 * All invalid children nodes
	 */
	protected final List<AndroidManifestNode> unknownChildren = new LinkedList<AndroidManifestNode>();

	/**
	 * All invalid children nodes
	 */
	protected final Map<String, String> unknownProperties = new HashMap<String, String>();

	/**
	 * Checks if a node type can be a child of this node
	 * 
	 * @param nodeType
	 *            The node type
	 * @return true if the type is accept as child or false otherwise
	 */
	protected abstract boolean canContains(NodeType nodeType);

	/**
	 * Checks if the node is valid, i.e., contains all required information to
	 * be valid on AndroidManifest.xml file
	 * 
	 * @return true if the node is valid or false otherwise
	 */
	protected abstract boolean isNodeValid();

	/**
	 * Retrieves the node type
	 * 
	 * @return the node type
	 */
	public abstract NodeType getNodeType();

	/**
	 * Retrieves all node properties, ready to be written to the
	 * AndroidManifest.xml file
	 * 
	 * @return all node properties, ready to be written to the
	 *         AndroidManifest.xml file
	 */
	public abstract Map<String, String> getNodeProperties();

	/**
	 * Retrieves the specific node errors. These errors are related to the
	 * manifest model, excluding those related to unknown child nodes and
	 * unknown attributes. For example, this method can return an error related
	 * to the lack of a required child node.
	 * 
	 * @return the specific node errors.
	 */
	protected abstract List<IStatus> getSpecificNodeProblems();

	/**
	 * Default constructor
	 */
	protected AndroidManifestNode() {
		// Do nothing
	}

	/**
	 * Retrieves the node name from its type. This name is the same as shown on
	 * AndroidManifest.xml file
	 * 
	 * @return the node name
	 */
	public String getNodeName() {
		return getNodeName(getNodeType());
	}

	/**
	 * Adds a child to the node. If the node is accepted as valid child, it will
	 * be treated this way. Otherwise, the node is treated as unknown.
	 * 
	 * @param child
	 *            The child node to be added
	 */
	public void addChild(AndroidManifestNode child) {
		Assert.isLegal(child != null);

		if (canContains(child.getNodeType())) {
			children.add(child);

		} else {
			unknownChildren.add(child);

		}

		// Set the parent
		child.setParent(this);
	}

	/**
	 * Adds a parent to the node.
	 * 
	 * @param parent
	 *            The parent node to be added
	 */
	public void setParent(AndroidManifestNode parent) {
		Assert.isLegal(parent != null);

		this.parent = parent;
	}

	/**
	 * Gets the parent of the node.
	 */
	public AndroidManifestNode getParent() {
		return parent;
	}

	/**
	 * Retrieves all valid children nodes
	 * 
	 * @return all valid children nodes
	 */
	public AndroidManifestNode[] getChildren() {
		AndroidManifestNode[] childrenArray = new AndroidManifestNode[children.size()];
		childrenArray = children.toArray(childrenArray);
		return childrenArray;
	}

	/**
	 * Retrieves all unknown children nodes
	 * 
	 * @return all unknown children nodes
	 */
	public AndroidManifestNode[] getUnkownChildren() {
		AndroidManifestNode[] unknownChildrenArray = new AndroidManifestNode[unknownChildren.size()];
		unknownChildrenArray = unknownChildren.toArray(unknownChildrenArray);
		return unknownChildrenArray;
	}

	/**
	 * Adds an unknown property to the node
	 * 
	 * @param property
	 *            The property name
	 * @param value
	 *            The property value
	 * @return true if the property has been added or false otherwise
	 */
	public boolean addUnknownProperty(String property, String value) {
		boolean added = false;

		if ((property != null) && (property.trim().length() > 0) && (value != null) && canAddUnknownProperty(property)) {
			unknownProperties.put(property, value);
		}

		return added;
	}

	/**
	 * Checks if an unknown property can be added, based on the valid properties
	 * 
	 * @param allProperties
	 *            the array containing all valid property names
	 * @param property
	 *            the property to be checked
	 * @return true if the unknown property can be added or false otherwise
	 */
	protected boolean canAddUnknownProperty(String property) {
		boolean canAdd = true;

		if (ALL_PROPERTIES == null) {
			ALL_PROPERTIES = new String[defaultProperties.size()];
			ALL_PROPERTIES = defaultProperties.toArray(ALL_PROPERTIES);
		}

		for (String prop : ALL_PROPERTIES) {
			if (prop.trim().equalsIgnoreCase(property)) {
				canAdd = false;
				break;
			}
		}

		return canAdd;
	}

	/**
	 * Retrieves all unknown node properties, ready to be written to the
	 * AndroidManifest.xml file
	 * 
	 * @return all unknown node properties, ready to be written to the
	 *         AndroidManifest.xml file
	 */
	public Map<String, String> getNodeUnknownProperties() {
		return unknownProperties;
	}

	/**
	 * Retrieves all children nodes from a specific type
	 * 
	 * @param type
	 *            The children type
	 * 
	 * @return all children nodes from the specific type
	 */
	protected AndroidManifestNode[] getAllChildrenFromType(NodeType type) {
		List<AndroidManifestNode> nodes = new LinkedList<AndroidManifestNode>();

		for (AndroidManifestNode node : children) {
			if (node.getNodeType() == type) {
				nodes.add(node);
			}
		}

		AndroidManifestNode[] arrayNodes = new AndroidManifestNode[nodes.size()];
		arrayNodes = nodes.toArray(arrayNodes);
		return arrayNodes;
	}

	/**
	 * Retrieves all node errors
	 * 
	 * @return an IStatus array containing all node errors
	 */
	public IStatus[] getNodeErrors() {
		List<IStatus> nodeErrors = new LinkedList<IStatus>();

		if ((getNodeType() != NodeType.Unknown) && (getNodeType() != NodeType.Comment)) {

			// Adds specific node errors
			List<IStatus> specificErrors = getSpecificNodeProblems();
			if ((specificErrors != null) && !specificErrors.isEmpty()) {
				nodeErrors.addAll(specificErrors);
			}

		}

		return nodeErrors.toArray(new IStatus[0]);
	}

	/**
	 * Retrieves all node warnings
	 * 
	 * @return an IStatus array containing all node warnings
	 */
	public IStatus[] getNodeWarnings() {
		List<IStatus> nodeWarnings = new LinkedList<IStatus>();

		if ((getNodeType() != NodeType.Unknown) && (getNodeType() != NodeType.Comment)) {
			String thisNode = "<" + getNodeName() + ">";
			// Adds errors about unknown properties
			for (String attribute : getNodeUnknownProperties().keySet()) {
				String errMsg = NLS.bind(UtilitiesNLS.WARN_AndroidManifestNode_TheNodeContainsAnInvalidAttribute,
						thisNode, attribute);

				nodeWarnings.add(new Status(IStatus.WARNING, CommonPlugin.PLUGIN_ID, errMsg));
			}
		}

		return nodeWarnings.toArray(new IStatus[0]);
	}

	/**
	 * Retrieves the errors for this node and for its children
	 * 
	 * @return an IStatus array containing all node errors
	 */
	public IStatus[] getRecursiveNodeErrors() {
		List<IStatus> nodeErrors = new LinkedList<IStatus>();
		IStatus[] thisNodeErrors = getNodeErrors();

		if (thisNodeErrors != null) {
			for (IStatus status : thisNodeErrors) {
				nodeErrors.add(status);
			}
		}

		for (AndroidManifestNode node : getChildren()) {
			IStatus[] childrenErrors = node.getNodeErrors();

			if (childrenErrors != null) {
				for (IStatus status : childrenErrors) {
					nodeErrors.add(status);
				}
			}
		}

		return nodeErrors.toArray(new IStatus[0]);
	}

	/**
	 * Checks if a know property can be added or updated, based on the valid
	 * properties.
	 * 
	 * @param property
	 *            the property to be checked
	 * @return true if the property can be added/updated or false otherwise
	 */
	protected boolean canAddOrUpdateProperty(String property) {
		boolean canAdd = false;

		if (ALL_PROPERTIES == null) {
			ALL_PROPERTIES = new String[defaultProperties.size()];
			ALL_PROPERTIES = defaultProperties.toArray(ALL_PROPERTIES);
		}

		for (String prop : ALL_PROPERTIES) {
			if (prop.trim().equalsIgnoreCase(property)) {
				canAdd = true;
				break;
			}
		}

		return canAdd;
	}

	/**
	 * Adds a known, valid property to the node
	 * 
	 * @param property
	 *            The property name
	 * @param value
	 *            The property value
	 * @return true if the property has been added or false otherwise
	 */
	public boolean addOrUpdateKnownProperty(String property, String value) {
		boolean added = false;

		if ((property != null) && (property.trim().length() > 0) && (value != null) && canAddOrUpdateProperty(property)) {
			properties.put(property, value);
		}

		return added;
	}
}
