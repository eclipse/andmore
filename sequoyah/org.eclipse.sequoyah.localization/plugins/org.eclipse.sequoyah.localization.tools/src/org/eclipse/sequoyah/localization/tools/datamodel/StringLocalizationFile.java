/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Fabricio Violin (Eldorado) - Bug [317065] - Localization file initialization bug 
 * Daniel Pastore (Eldorado) - Bug [323036] - Add support to other localizable resources
 * Matheus Lima (Eldorado) - Bug [326793] - Fixed array support for the String Localization Editor
 * Paulo Faria (Eldorado) - Bug 326793 - Improvements on the String Arrays handling 
 * Paulo Faria (Eldorado) - Bug [326793] -  Improvements on the String Arrays handling
 * Thiago Junqueira (Eldorado) - Bug [326793] - Added method renameNodeKey. 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.tools.datamodel.node.NodeComment;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayItemNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;

/**
 * This class represents a real localization file for strings
 */
public class StringLocalizationFile extends LocalizationFile {

	/*
	 * String nodes (single strings and array nodes) indexed by key
	 */
	private final Map<String, StringNode> stringNodesMap = new HashMap<String, StringNode>();

	/*
	 * Static code to add file type to factory's hashmap
	 */
	static {
		LocalizationFileFactory.getInstance().addFileType(
				StringLocalizationFile.class.getName(),
				StringLocalizationFile.class);
	}

	/**
	 * Default constructor.
	 */
	public StringLocalizationFile() {
	}

	/**
	 * Constructor method
	 * 
	 * @param file
	 *            a reference to the file being represented
	 * @param localeInfo
	 *            the locale represented by the localization file
	 * @param stringNodes
	 *            the list of StringNodes which are part of the file
	 */
	public StringLocalizationFile(LocalizationFileBean bean) {
		super(bean);
		clearStringNodes();
		setStringNodes(bean.getStringNodes());
		setStringArrayNodes(bean.getStringArrays());
	}

	/**
	 * StringLocalizationFile knows how to create itself.
	 * 
	 * @param bean
	 *            Bean containing all information necessary to create any type
	 *            of LocalizationFile.
	 * @return LocalizationFile created (if the parameter received is not null).
	 */
	public static LocalizationFile create(LocalizationFileBean bean) {
		LocalizationFile locFile;
		if (bean != null) {
			locFile = new StringLocalizationFile(bean);
		} else {
			locFile = null;
		}
		return locFile;
	}

	/**
	 * Get the list of StringNodes (ONLY single strings, not arrays items nor
	 * arrays) which are part of the file
	 * 
	 * @return the list of StringNodes which are part of the file
	 */
	public List<StringNode> getStringNodes() {
		List<StringNode> stringNodes = new ArrayList<StringNode>();
		for (StringNode stringNode : stringNodesMap.values()) {
			if (!(stringNode instanceof StringArrayNode)) {
				stringNodes.add(stringNode);
			}
		}
		return stringNodes;
	}

	/**
	 * Get the list of StringNodes (ONLY arrays, not arrays items nor single
	 * strings) which are part of the file
	 * 
	 * @return the list of ArrayStringNodes which are part of the file
	 */
	public List<StringArrayNode> getStringArrays() {
		List<StringArrayNode> stringArrayNodes = new ArrayList<StringArrayNode>();
		for (StringNode stringNode : stringNodesMap.values()) {
			if (stringNode instanceof StringArrayNode) {
				StringArrayNode arrayNode = (StringArrayNode) stringNode;
				stringArrayNodes.add(arrayNode);
			}
		}
		return stringArrayNodes;
	}

	/**
	 * Get the list of StringNodes (single strings, and array items, NOT arrays)
	 * which are part of the file
	 * 
	 * @return the list of StringNodes which are part of the file
	 */
	public List<StringNode> getNodesWithTextContent() {
		List<StringNode> stringNodes = new ArrayList<StringNode>();
		for (StringNode stringNode : stringNodesMap.values()) {
			if (!(stringNode instanceof StringArrayNode)) {
				stringNodes.add(stringNode);
			} else {
				StringArrayNode array = (StringArrayNode) stringNode;
				for (StringNode child : array.getValues()) {
					stringNodes.add(child);
				}
			}
		}
		return stringNodes;
	}

	/**
	 * Get the list of StringNodes top levels (arrays or single strings, NOT
	 * array items) which are part of the file
	 * 
	 * @return the list of ArrayStringNodes which are part of the file
	 */
	public List<StringNode> getTopLevelNodes() {
		List<StringNode> topLevelStringNodes = new ArrayList<StringNode>();
		topLevelStringNodes.addAll(stringNodesMap.values());
		return topLevelStringNodes;
	}

	/**
	 * Creates a new node based on the type of the row (single or array)
	 * 
	 * @param rowInfo
	 * @param value
	 *            to add in the StringNode
	 * @param comment
	 *            to add in the StringNode
	 * @return
	 */
	public StringNode createNode(RowInfo rowInfo, String value,
			String nodeComment) {
		StringNode newNode = null;
		if (rowInfo instanceof RowInfoLeaf) {
			// string or array item
			RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
			if (leaf.getParent() == null) {
				// string
				newNode = this.addStringNode(new StringNode(rowInfo.getKey(),
						value));
			} else {
				// array item - add parent to add value
				StringArrayNode arrayNode = (stringNodesMap.containsKey(rowInfo
						.getKey()) ? (StringArrayNode) stringNodesMap
						.get(rowInfo.getKey()) : new StringArrayNode(
						rowInfo.getKey()));
				// do not use addStringNode because it is not a top level item
				// (array or string)
				stringNodesMap.put(arrayNode.getKey(), arrayNode);
				StringArrayItemNode childNode = new StringArrayItemNode(value,
						arrayNode, leaf.getPosition());
				newNode = addStringArrayItemNode(childNode);
			}
		} else if (rowInfo instanceof RowInfo) {
			// array
			List<RowInfoLeaf> children = rowInfo.getChildren();
			StringArrayNode arrayNode = new StringArrayNode(rowInfo.getKey());
			for (int pos = 0; pos < children.size(); pos++) {
				arrayNode.addValue(value, pos);
			}
			newNode = this.addStringNode(arrayNode);
		}
		if (newNode != null) {
			newNode.setLocalizationFile(this);
			newNode.setDirty(true);
			NodeComment commentNode = new NodeComment();
			commentNode.setComment(nodeComment);
			newNode.setNodeComment(commentNode);
		}
		return newNode;
	}

	/**
	 * Get the StringNodes (top level nodes, that is single string or array, NOT
	 * array items) which represents a specific key.
	 * 
	 * 
	 * @param key
	 *            the StringNode key attribute
	 * @return the StringNode which represents the key passed as a parameter,
	 *         null if not found
	 */
	public StringNode getStringNodeByKey(String key) {
		StringNode result = stringNodesMap.get(key);
		return result;
	}

	public void clearStringNodes() {
		stringNodesMap.clear();
	}

	public boolean containsKey(String key) {
		return stringNodesMap.containsKey(key);
	}

	/**
	 * Set the list of StringNodes which are part of the file.
	 * 
	 * @param stringNodes
	 *            the list of StringNodes which are part of the file
	 */
	public void setStringNodes(List<StringNode> stringNodes) {
		if (stringNodes != null) {
			for (StringNode stringNode : stringNodes) {
				this.stringNodesMap.put(stringNode.getKey(), stringNode);
				stringNode.setLocalizationFile(this);
			}
		}
	}

	/**
	 * Set the list of StringArrays which are part of the file
	 * 
	 * @param stringArrayNodes
	 *            the list of StringArrays which are part of the file
	 */
	public void setStringArrayNodes(List<StringArrayNode> stringArrayNodes) {
		if (stringArrayNodes != null) {
			for (StringArrayNode stringArrayNode : stringArrayNodes) {
				List<StringArrayItemNode> stringNodes = stringArrayNode
						.getValues();
				for (StringNode stringNode : stringNodes) {
					stringNode.setLocalizationFile(this);
				}
				stringArrayNode.setLocalizationFile(this);
				this.stringNodesMap.put(stringArrayNode.getKey(),
						stringArrayNode);
			}
		}
	}

	public void removeNode(String key) {
		this.stringNodesMap.remove(key);
	}

	/**
	 * Get only the modified StringNodes in this localization file
	 * 
	 * @return the modified StringNodes in this localization file
	 */
	public List<StringNode> getModifiedStringNodes() {
		List<StringNode> modifiedStringNodes = new ArrayList<StringNode>();
		for (StringNode stringNode : getNodesWithTextContent()) {
			if (stringNode.isDirty()) {
				modifiedStringNodes.add(stringNode);
			}
		}
		return modifiedStringNodes;
	}

	/**
	 * Add a new StringNode to the list
	 */
	public StringNode addStringNode(StringNode stringNode) {
		StringNode newStringNode = stringNode;

		newStringNode.setLocalizationFile(this);
		stringNodesMap.put(newStringNode.getKey(), newStringNode);

		this.setDirty(true);

		return newStringNode;
	}

	/**
	 * Remove a top level StringNode from the list (array or single string)
	 */
	public void removeStringNode(StringNode stringNode) {
		if (stringNodesMap.containsKey(stringNode.getKey())) {
			stringNodesMap.remove(stringNode.getKey());
			this.setDirty(true);
		}
	}

	/**
	 * Removes StringArrayItemNode
	 * 
	 * @param child
	 */
	public void removeStringArrayItemNode(StringArrayItemNode child) {
		if (child != null
				&& stringNodesMap.containsKey(child.getParent().getKey())) {
			child.getParent().removeValue(child);
			this.setDirty(true);
		}
	}

	/**
	 * Adds StringArrayItemNode
	 * 
	 * @param child
	 */
	public StringNode addStringArrayItemNode(StringArrayItemNode child) {
		StringNode node = child;
		if (child != null) {
			StringNode parent = stringNodesMap.get(child.getParent().getKey());
			if (parent instanceof StringArrayNode) {
				((StringArrayNode) parent).addValue(child.getValue(),
						child.getPosition(), false);
			}
			this.setDirty(true);
		}
		return node;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = true;

		// if (!this.getLocaleInfo().equals(
		// ((StringLocalizationFile) obj).getLocaleInfo())) {
		//
		// result = false;
		// }
		if (!super.equals(obj)) {
			result = false;
		} else {
			StringLocalizationFile locFile = (StringLocalizationFile) obj;
			List<StringNode> locFileStringNodes = locFile
					.getNodesWithTextContent();

			// skip blank array items
			List<StringNode> thisStringNodes = removeBlankArrayItems(getNodesWithTextContent());
			List<StringNode> otherStringNodes = removeBlankArrayItems(locFileStringNodes);

			Collections.sort(thisStringNodes);
			Collections.sort(otherStringNodes);

			if ((thisStringNodes.size() != otherStringNodes.size())) {
				result = false;
			} else {
				boolean keyEqual, valueEqual;
				for (int i = 0; i < thisStringNodes.size(); i++) {
					keyEqual = thisStringNodes.get(i).getKey()
							.equals(otherStringNodes.get(i).getKey());
					String EOL = System.getProperty("line.separator"); //$NON-NLS-1$
					String fromFile = thisStringNodes.get(i).getValue() != null ? thisStringNodes
							.get(i).getValue().replaceAll(EOL, "\n") : null; //$NON-NLS-1$
					String other = otherStringNodes.get(i).getValue();
					valueEqual = fromFile != null && other != null ? fromFile
							.equals(other) : false;
					if ((!keyEqual) || (!valueEqual)) {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Remove blank array items
	 * 
	 * @param nodes
	 *            all nodes
	 * @return only non blank array items
	 */
	private List<StringNode> removeBlankArrayItems(List<StringNode> nodes) {
		List<StringNode> noBlankArrayItems = new ArrayList<StringNode>();

		for (StringNode node : nodes) {
			if (node instanceof StringArrayNode) {
				if (!node.getValue().equals("")) { //$NON-NLS-1$
					noBlankArrayItems.add(node);
				}
			} else {
				noBlankArrayItems.add(node);
			}
		}
		return noBlankArrayItems;
	}

	/**
	 * Updates the key value of a string node
	 * 
	 * @param oldKey
	 *            - The old key to be updated
	 * @param newKey
	 *            - The new value to be used
	 */
	public void renameNodeKey(String oldKey, String newKey) {
		// Update both StringNode map
		StringNode node = getStringNodeByKey(oldKey);
		if (node != null) {
			removeStringNode(node);
			node.setKey(newKey);
			addStringNode(node);
		}
	}

}
