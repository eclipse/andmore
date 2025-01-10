/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Gorri (Eldorado) - Bug 326793 -  Improvements on the String Arrays handling  
 * Matheus Lima (Eldorado) - Bug [326793] - Fixed array support for the String Localization Editor
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (Refactor visitDomXYZ and NodeManagers)
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.manager;

import static org.w3c.dom.Node.COMMENT_NODE;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.sequoyah.localization.android.IAndroidLocalizationSchemaConstants;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.NodeComment;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Encapsulates the knowledge about manipulating Single String items inside
 * String XML file for Android
 */
public class StringNodeManager extends NodeManager implements
		IAndroidLocalizationSchemaConstants {

	public StringNodeManager() {

	}

	@Override
	public void loadFile(LocalizationFileBean bean, LocalizationFile locFile) {
		bean.setStringNodes(((StringLocalizationFile) locFile).getStringNodes());
	}

	@Override
	public void updateLocalizationFileContent(Document document,
			ArrayList<StringNode> stringNodes) {

		/*
		 * Get string nodes
		 */
		NodeList stringNodeList = document.getElementsByTagName(XML_STRING_TAG);

		String key = null;
		String value = null;
		for (int i = 0; i < stringNodeList.getLength(); i++) {
			Element stringNode = (Element) stringNodeList.item(i);
			key = stringNode.getAttributeNode(XML_STRING_ATTR_NAME)
					.getNodeValue();
			String comment = null;
			if (stringNode.hasChildNodes()) {
				NodeList childs = stringNode.getChildNodes();
				for (int j = 0; j < childs.getLength(); j++) {
					Node childN = childs.item(j);
					if (childN.getNodeType() == COMMENT_NODE) {
						comment = childN.getNodeValue();
					}
				}

			}
			// get formatted text from single (non-array) item
			Node auxNode = stringNode.getFirstChild();
			StringBuffer valueText = new StringBuffer();
			getStringByNodes(valueText, auxNode);
			value = valueText.toString();

			stringNode.toString();
			StringNode stringNodeObj = new StringNode(key, value);
			if (comment != null) {
				NodeComment nodeComment = new NodeComment();
				nodeComment.setComment(comment);
				stringNodeObj.setNodeComment(nodeComment);
			}
			stringNodes.add(stringNodeObj);
		}
	}

	@Override
	public void createFile(Document document, Element resources,
			LocalizationFile localizationFile) {

		for (StringNode stringNode : ((StringLocalizationFile) localizationFile)
				.getStringNodes()) {
			addSingleEntry(document, resources, stringNode);
		}
	}

	/**
	 * Adds single entry into XML Android Localization file
	 * 
	 * @param document
	 * @param resources
	 * @param stringNode
	 */
	public void addSingleEntry(Document document, Element resources,
			StringNode stringNode) {
		Element string = document.createElement(XML_STRING_TAG);
		string.setAttribute(XML_STRING_ATTR_NAME, stringNode.getKey());
		string.appendChild(document.createTextNode(stringNode.getValue()));
		createOrUpdateComment(document, stringNode, string);
		resources.appendChild(string);
	}

	@Override
	public void updateFile(LocalizationFile locFile,
			Map<String, StringNode> singleStringsToUpdateOrAdd) {
		for (StringNode stringNode : ((StringLocalizationFile) locFile)
				.getStringNodes()) {
			singleStringsToUpdateOrAdd.put(stringNode.getKey(), stringNode);
		}
	}
}
