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
package org.eclipse.andmore.android.model.resources.types;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class used to describe a node of a resource file
 */
public abstract class AbstractResourceNode {
	/**
	 * Enumeration used to describe the resource node types
	 */
	public static enum NodeType {
		Resources, String, Color, Dimen, Drawable, Unknown
	}

	/**
	 * The known attributes of a resource node
	 */
	protected final Map<String, String> attributes;

	/**
	 * The unknown attributes of a resource node
	 */
	protected final Map<String, String> unknownAttributes;

	/**
	 * The known child nodes of a resource
	 */
	protected final List<AbstractResourceNode> children;

	/**
	 * The unknown child nodes of a resource
	 */
	protected final List<AbstractResourceNode> unknownChildren;

	/**
	 * Checks if an attribute name is valid for a resource node
	 * 
	 * @param attributeName
	 *            The attribute name
	 * 
	 * @return true if the attribute is acceptable for the node and false
	 *         otherwise
	 */
	protected abstract boolean isAttributeValid(String attributeName);

	/**
	 * Retrieves the resource node type
	 * 
	 * @return the resource node type
	 */
	public abstract NodeType getNodeType();

	/**
	 * Checks if a node can be accepted as child node
	 * 
	 * @param node
	 *            The node to be checked
	 * 
	 * @return true if the node can be a child node or false otherwise
	 */
	protected abstract boolean canAddChildNode(AbstractResourceNode node);

	/**
	 * Retrieves the node type based on a node name
	 * 
	 * @param nodeName
	 *            the node name
	 * 
	 * @return the node type related to the node name. If the name cannot be
	 *         associated to any node type, the unknown type will be returned.
	 */
	public static NodeType getNodeType(String nodeName) {
		NodeType nodeType = NodeType.Unknown;

		for (NodeType type : NodeType.values()) {
			if (nodeName.trim().equalsIgnoreCase(getNodeTypeName(type))) {
				nodeType = type;
				break;
			}
		}

		return nodeType;
	}

	/**
	 * Retrieves the node name based on the node type
	 * 
	 * @param nodeType
	 *            The node type
	 * 
	 * @return The node name
	 */
	public static String getNodeTypeName(NodeType nodeType) {
		String nodeName;

		switch (nodeType) {
		case Resources:
			nodeName = "resources";
			break;
		case String:
			nodeName = "string";
			break;
		case Color:
			nodeName = "color";
			break;
		case Dimen:
			nodeName = "dimen";
			break;
		case Drawable:
			nodeName = "drawable";
			break;
		default:
			nodeName = "unknown";
		}

		return nodeName;
	}

	/**
	 * Default constructor
	 */
	public AbstractResourceNode() {
		attributes = new HashMap<String, String>();
		unknownAttributes = new HashMap<String, String>();
		children = new LinkedList<AbstractResourceNode>();
		unknownChildren = new LinkedList<AbstractResourceNode>();
	}

	/**
	 * Retrieves an attribute value from a resource node. The attribute must be
	 * valid.
	 * 
	 * @param attributeName
	 *            The attribute name
	 * @return The attribute value
	 */
	public String getAttributeValue(String attributeName) {
		String attrValue = null;

		if (attributeName != null) {
			attrValue = attributes.get(attributeName);
		}

		return attrValue;
	}

	/**
	 * Retrieves an array containing all known attributes of a resource node
	 * 
	 * @return an array containing all known attributes of a resource node
	 */
	public String[] getAttributes() {
		String[] attrs = new String[attributes.size()];

		attrs = attributes.keySet().toArray(attrs);

		return attrs;
	}

	/**
	 * Adds an unknown attribute to a resource node
	 * 
	 * @param attributeName
	 *            The attribute name
	 * @param attributeValue
	 *            The attribute value
	 * @return true if the attribute has been added or false otherwise. An
	 *         attribute is not added if it exists.
	 */
	public boolean addUnknownAttribute(String attributeName, String attributeValue) {
		boolean added = false;

		if (attributeName != null) {
			if (!isAttributeValid(attributeName)) {
				if (!unknownAttributes.containsKey(attributeName)) {
					unknownAttributes.put(attributeName, attributeValue);
					added = true;
				}
			}
		}

		return added;
	}

	/**
	 * Removes an unknown attribute value from a resource node
	 * 
	 * @param attributeName
	 *            The attribute name
	 * @return true if the attribute has been removed or false otherwise.
	 */
	public boolean removeUnknownAttribute(String attributeName) {
		boolean removed = false;

		if (attributeName != null) {
			if (unknownAttributes.containsKey(attributeName)) {
				unknownAttributes.remove(attributeName);
				removed = true;
			}
		}

		return removed;
	}

	/**
	 * Retrieves the value of an unknown attribute of a resource node
	 * 
	 * @param attributeName
	 *            The attribute name
	 * @return The attribute value
	 */
	public String getUnknownAttributeValue(String attributeName) {
		String value = null;

		if (attributeName != null) {
			if (unknownAttributes.containsKey(attributeName)) {
				value = unknownAttributes.get(attributeName);
			}
		}

		return value;
	}

	/**
	 * Retrieves an array containing all unknown attribute names
	 * 
	 * @return an array containing all unknown attribute names
	 */
	public String[] getUnknownAttributes() {
		String attributes[] = new String[this.unknownAttributes.size()];

		attributes = this.unknownAttributes.keySet().toArray(attributes);

		return attributes;
	}

	/**
	 * Removes all unknown attributes
	 */
	public void clearUnknownAttributes() {
		unknownAttributes.clear();
	}

	/**
	 * Adds a child resource node to this node
	 * 
	 * @param node
	 *            The node to be added
	 * @return true if the node has been added or false otherwise
	 */
	public boolean addChildNode(AbstractResourceNode node) {
		boolean added = false;

		if (node != null) {
			if (canAddChildNode(node)) {
				if (!children.contains(node)) {
					children.add(node);
					added = true;
				}
			} else {
				if (!unknownChildren.contains(node)) {
					unknownChildren.add(node);
					added = true;
				}
			}
		}

		return added;
	}

	/**
	 * Removes a child node from this node
	 * 
	 * @param node
	 *            the node to be removed
	 * @return true if the node has been removed or false otherwise
	 */
	public boolean removeChildNode(AbstractResourceNode node) {
		boolean removed = false;

		if (node != null) {
			if (children.contains(node)) {
				children.remove(node);
				removed = true;
			} else if (unknownChildren.contains(node)) {
				unknownChildren.remove(node);
				removed = true;
			}
		}

		return removed;
	}

	/**
	 * Retrieves an array containing all child nodes of this node
	 * 
	 * @return an array containing all child nodes of this node
	 */
	public AbstractResourceNode[] getChildNodes() {
		AbstractResourceNode[] childNodes = new AbstractResourceNode[children.size()];

		childNodes = children.toArray(childNodes);

		return childNodes;
	}

	/**
	 * Retrieves an array containing all unknown child nodes of this node
	 * 
	 * @return an array containing all unknown child nodes of this node
	 */
	public AbstractResourceNode[] getUnknownChildNodes() {
		AbstractResourceNode[] childNodes = new AbstractResourceNode[unknownChildren.size()];

		childNodes = unknownChildren.toArray(childNodes);

		return childNodes;
	}

	/**
	 * Removes all child nodes of this node
	 */
	public void clearChildNodes() {
		children.clear();
	}

	/**
	 * Removes all unknown child nodes of this node
	 */
	public void clearUnknownChildNodes() {
		unknownChildren.clear();
	}

	/**
	 * Retrieves the name of this node
	 * 
	 * @return the name of this node
	 */
	public String getNodeName() {
		return getNodeTypeName(getNodeType());
	}
}
