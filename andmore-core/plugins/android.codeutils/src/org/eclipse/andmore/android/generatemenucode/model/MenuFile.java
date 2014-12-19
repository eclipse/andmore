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
package org.eclipse.andmore.android.generatemenucode.model;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatecode.AndroidXMLFileConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Represents a single menu.xml file. Based on Android documentation:
 * {@link http
 * ://developer.android.com/guide/topics/resources/menu-resource.html}.
 */
public class MenuFile {
	private final String name;

	private final File file;

	private MenuNode rootMenuNode;

	/**
	 * @param menuFileName
	 *            name that may appear into the dialog to create code based on
	 *            menu
	 * @param menuFilePath
	 *            absolute file path to menu.xml
	 * @throws AndroidException
	 *             fail to parse menu.xml
	 */
	public MenuFile(String menuFileName, File menuFilePath) throws AndroidException {
		this.name = menuFileName;
		this.file = menuFilePath;
		rootMenuNode = parseDocument(file);
	}

	/**
	 * @return the rootMenuNode the in-memory representation from menu.xml
	 */
	public MenuNode getRootMenuNode() {
		return rootMenuNode;
	}

	/**
	 * The path to file (relative to project)
	 */
	public String getName() {
		return name;
	}

	/**
	 * The path to file (relative to project)
	 */
	public String getNameWithoutExtension() {
		String result;
		if ((name != null) && name.contains(".")) {
			result = name.substring(0, name.lastIndexOf('.'));
		} else {
			result = name;
		}
		return result;
	}

	public File getFile() {
		return file;
	}

	/**
	 * Parses an IDocument object containing the menu.xml into a DOM
	 * 
	 * @param document
	 *            the IDocument object
	 * @throws SAXException
	 *             When a parsing error occurs
	 * @throws IOException
	 *             When a reading error occurs
	 */
	private static final MenuNode parseDocument(File f) throws AndroidException {
		MenuNode mainMenuNode = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(f);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			StudioLogger.error(MenuFile.class, "Error parsing menu: " + e.getMessage());
			throw new AndroidException(e);
		}

		Node node = doc.getFirstChild();
		mainMenuNode = (MenuNode) readAttributes(node, null);

		populateNodeForRootMenuNode(mainMenuNode, node);

		return mainMenuNode;
	}

	/**
	 * Populates information about menus initiating recursion (start on menu
	 * node that is in the root of the file)
	 * 
	 * @param mainMenuNode
	 * @param node
	 */
	public static void populateNodeForRootMenuNode(MenuNode mainMenuNode, Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
				AbstractMenuNode menuNode = readAttributes(node, mainMenuNode);
				if (node.hasChildNodes()) {
					// navigate in deep in the tree, using current menuNode as
					// parent node
					populateNodes(node.getChildNodes(), menuNode);
				}
			}
		}
	}

	/**
	 * Populates information about menus on non-root nodes
	 * 
	 * @param children
	 * @param parentNode
	 */
	private static final void populateNodes(NodeList children, AbstractMenuNode parentNode) {
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
				AbstractMenuNode menuNode = readAttributes(node, parentNode);
				if (node.hasChildNodes()) {
					// navigate in deep in the tree, using current menuNode as
					// parent node
					populateNodes(node.getChildNodes(), menuNode);
				}
			}
		}
	}

	/**
	 * Reads attributes that are relevant to generate code based on menu
	 * 
	 * @param node
	 * @param parentNode
	 *            null if the root node, non-null if internal node
	 * @return current node being navigated
	 */
	private static AbstractMenuNode readAttributes(Node node, AbstractMenuNode parentNode) {
		AbstractMenuNode currentMenuNode = getMenuNode(node.getNodeName());
		Node id = node.getAttributes().getNamedItem(AndroidXMLFileConstants.ANDROID_ID);
		if ((id != null)) {
			String idText = id.getNodeValue();
			idText = idText.replace(AndroidXMLFileConstants.IDENTIFIER, "");
			if (currentMenuNode instanceof MenuItemNode) {
				MenuItemNode menuItemNode = (MenuItemNode) currentMenuNode;
				menuItemNode.setId(idText);

				Node onClick = node.getAttributes().getNamedItem(AndroidXMLFileConstants.ANDROID_ON_CLICK);
				if (onClick != null) {
					menuItemNode.setOnClickMethod(onClick.getNodeValue());
				}
			} else if (currentMenuNode instanceof GroupNode) {
				GroupNode groupNode = (GroupNode) currentMenuNode;
				groupNode.setId(idText);
			}
		}

		if (parentNode != null) {
			// if internal node => set its parent
			appendItemNodeStructure(parentNode, currentMenuNode);
		}
		return currentMenuNode;
	}

	/**
	 * Appends current node into tree representation
	 * 
	 * @param parentNode
	 * @param currentMenuNode
	 */
	public static void appendItemNodeStructure(AbstractMenuNode parentNode, AbstractMenuNode currentMenuNode) {
		if (parentNode instanceof MenuNode) {
			MenuNode menuNode = (MenuNode) parentNode;
			menuNode.add(currentMenuNode);
		} else if ((parentNode instanceof GroupNode) && (currentMenuNode instanceof MenuItemNode)) {
			GroupNode groupNode = (GroupNode) parentNode;
			groupNode.add((MenuItemNode) currentMenuNode);
		} else if ((parentNode instanceof MenuItemNode) && (currentMenuNode instanceof MenuNode)) {
			MenuItemNode menuItemNode = (MenuItemNode) parentNode;
			menuItemNode.setSubMenu((MenuNode) currentMenuNode);
		}
	}

	/**
	 * Gets the type of the node, which can be menu, item or group
	 * 
	 * @param nodeName
	 * @return
	 */
	private static AbstractMenuNode getMenuNode(String nodeName) {
		AbstractMenuNode node = null;
		if (nodeName.equals(AbstractMenuNode.MenuNodeType.menu.name())) {
			node = new MenuNode();
		} else if (nodeName.equals(AbstractMenuNode.MenuNodeType.item.name())) {
			node = new MenuItemNode();
		} else if (nodeName.equals(AbstractMenuNode.MenuNodeType.group.name())) {
			node = new GroupNode();
		}
		return node;
	}
}
