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
package org.eclipse.andmore.android.generateviewbylayout.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * Represents a layout xml data under a Android project
 */
public class LayoutFile {

	/*
	 * Constants
	 */
	private static final String FRAGMENT = "fragment";

	private static final String LAYOUT = "Layout";

	/*
	 * Layout variables
	 */

	private final List<LayoutNode> nodes;

	private final String name;

	private final File file;

	public LayoutFile(String layoutName, File layout) throws AndroidException {
		this.name = layoutName;
		this.file = layout;
		nodes = parseDocument(file);
	}

	/**
	 * @return list of GUI items inside layout XML
	 */
	public List<LayoutNode> getNodes() {
		return nodes;
	}

	/**
	 * @return name of the layout xml (to appear in dialogs/wizards)
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return path to layout xml file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Parses an IDocument object containing the layout.xml into a DOM
	 * 
	 * @param document
	 *            the IDocument object
	 * @throws SAXException
	 *             When a parsing error occurs
	 * @throws IOException
	 *             When a reading error occurs
	 */
	private static final List<LayoutNode> parseDocument(File f) throws AndroidException {
		List<LayoutNode> lNodes = new ArrayList<LayoutNode>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(f);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			StudioLogger.error(LayoutFile.class, "Error parsing layout: " + e.getMessage());
			throw new AndroidException(e);
		}

		NodeList children = doc.getChildNodes();

		visitNodes(children, lNodes);
		return lNodes;
	}

	private static final void visitNodes(NodeList children, List<LayoutNode> lNodes) {
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
				LayoutNode layNode = readAttributes(node);
				if (layNode != null) {
					lNodes.add(layNode);
				}
				if (node.hasChildNodes()) {
					visitNodes(node.getChildNodes(), lNodes);
				}
			}
		}
	}

	private static final LayoutNode readAttributes(Node node) {
		LayoutNode layoutNode = null;
		Node id = node.getAttributes().getNamedItem(AndroidXMLFileConstants.ANDROID_ID);
		if ((id != null) && id.getNodeValue().contains(AndroidXMLFileConstants.IDENTIFIER)) {
			// only treats @+id , i.e, if the id is defined in the
			// user-application.
			// Using @id is intended for calling ids defined in android (or
			// another application)
			layoutNode = getLayoutNode(node.getNodeName());
			layoutNode.setNodeType(node.getNodeName());

			String idText = id.getNodeValue();
			idText = idText.replace(AndroidXMLFileConstants.IDENTIFIER, "");
			layoutNode.setNodeId(idText);

			Node onClick = node.getAttributes().getNamedItem(AndroidXMLFileConstants.ANDROID_ON_CLICK);
			if (onClick != null) {
				layoutNode.setOnClick(onClick.getNodeValue());
			}

			if (node.getNodeName().contains(LAYOUT)) {
				layoutNode.setLayout(true);
			} else {
				layoutNode.setGUIItem(true);
			}
			if (node.getNodeName().toLowerCase().contains(FRAGMENT)) {
				layoutNode.setNodeType("Fragment"); // to avoid problems with
													// mixed case
				layoutNode.setFragmentPlaceholder(true);
				Node clazzNameNode = node.getAttributes().getNamedItem(AndroidXMLFileConstants.ANDROID_NAME);
				if (clazzNameNode != null) {
					String clazzName = clazzNameNode.getNodeValue();
					int lastNameInd = clazzName.lastIndexOf(".");
					if (lastNameInd >= 0) {
						clazzName = clazzName.substring(lastNameInd + 1);
					}
					layoutNode.setClazzName(clazzName);
				}
			}
		} else if (id == null) {
			// no android:id set
			layoutNode = new LayoutNode();
			layoutNode.setNodeType(node.getNodeName());

			if (node.getNodeName().contains(LAYOUT)) {
				layoutNode.setLayout(true);
			} else {
				layoutNode.setGUIItem(true);
			}
		}
		return layoutNode;
	}

	private static LayoutNode getLayoutNode(String nodeName) {
		LayoutNode node = null;

		if (nodeName.equals(LayoutNode.LayoutNodeViewType.EditText.name())) {
			node = new EditTextNode();
		} else if (nodeName.equals(LayoutNode.LayoutNodeViewType.CheckBox.name())) {
			node = new CheckboxNode();
		} else if (nodeName.equals(LayoutNode.LayoutNodeViewType.RadioButton.name())) {
			node = new RadioButtonNode();
		} else if (nodeName.equals(LayoutNode.LayoutNodeViewType.Spinner.name())) {
			node = new SpinnerNode();
		} else if (nodeName.equals(LayoutNode.LayoutNodeViewType.SeekBar.name())) {
			node = new SeekBarNode();
		} else {
			node = new LayoutNode();
		}

		return node;
	}

}
