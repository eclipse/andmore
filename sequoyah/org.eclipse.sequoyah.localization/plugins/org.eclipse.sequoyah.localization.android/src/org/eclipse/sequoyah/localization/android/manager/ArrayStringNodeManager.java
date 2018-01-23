/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Gorri (Eldorado) - Bug 326793 -  Improvements on the String Arrays handling  
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
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Encapsulates the knowledge about manipulating Arrays inside String XML file
 * for Android
 */
public class ArrayStringNodeManager extends NodeManager implements
		IAndroidLocalizationSchemaConstants {

	public ArrayStringNodeManager() {

	}

	@Override
	public void loadFile(LocalizationFileBean bean, LocalizationFile locFile) {
		bean.setStringArrays(((StringLocalizationFile) locFile)
				.getStringArrays());
	}

	@Override
	public void updateLocalizationFileContent(Document document,
			ArrayList<StringNode> stringNodes) {
		/*
		 * Get array nodes
		 */
		NodeList arrayNodeList = document
				.getElementsByTagName(XML_STRING_ARRAY_TAG);
		String arrayKey = null;
		String arrayValue = null;
		for (int i = 0; i < arrayNodeList.getLength(); i++) {
			Element arrayNode = (Element) arrayNodeList.item(i);
			arrayKey = arrayNode.getAttributeNode(XML_STRING_ATTR_NAME)
					.getNodeValue();
			StringArrayNode stringArray = new StringArrayNode(arrayKey);
			if (arrayNode.hasChildNodes()) {
				NodeList arrayItems = arrayNode
						.getElementsByTagName(XML_STRING_ARRAY_ITEM_TAG);
				for (int j = 0; j < arrayItems.getLength(); j++) {
					Node childN = arrayItems.item(j);

					// get formatted text from array item
					Node auxNode = childN.getFirstChild();
					StringBuffer valueText = new StringBuffer();
					getStringByNodes(valueText, auxNode);
					arrayValue = valueText.toString();

					StringNode newNode = stringArray.addValue(arrayValue);

					// comments
					String comment = null;
					if (childN.hasChildNodes()) {
						NodeList childs = childN.getChildNodes();
						for (int k = 0; k < childs.getLength(); k++) {
							Node commentNode = childs.item(k);
							if (commentNode.getNodeType() == COMMENT_NODE) {
								comment = commentNode.getNodeValue();
							}
						}

					}

					if (comment != null) {
						NodeComment nodeComment = new NodeComment();
						nodeComment.setComment(comment);
						newNode.setNodeComment(nodeComment);
					}

				}

			}
			if (stringArray.getValues().size() > 0) {
				stringNodes.add(stringArray);
			}
		}
	}

	@Override
	public void createFile(Document document, Element resources,
			LocalizationFile localizationFile) {

		for (StringArrayNode stringArray : ((StringLocalizationFile) localizationFile)
				.getStringArrays()) {
			addArrayEntry(document, resources, stringArray);
		}

	}

	/**
	 * Adds array entry into XML Android Localization file
	 * 
	 * @param document
	 * @param resources
	 * @param stringArray
	 */
	public void addArrayEntry(Document document, Element resources,
			StringArrayNode stringArray) {
		Element array = document.createElement(XML_STRING_ARRAY_TAG);
		array.setAttribute(XML_STRING_ATTR_NAME, stringArray.getKey());
		for (StringNode stringNode : stringArray.getValues()) {
			createArrayItem(document, array, stringNode);
		}
		resources.appendChild(array);
	}

	/**
	 * Adds array item entry into XML Android Localization file
	 */
	public void createArrayItem(Document document, Element array,
			StringNode stringNode) {
		Element arrayItem = document.createElement(XML_STRING_ARRAY_ITEM_TAG);
		arrayItem
				.appendChild(document
						.createTextNode(stringNode.getValue() != null ? stringNode
								.getValue() : "")); //$NON-NLS-1$
		array.appendChild(arrayItem);

		createOrUpdateComment(document, stringNode, arrayItem);
	}

	@Override
	public void updateFile(LocalizationFile locFile,
			Map<String, StringNode> arrayStringsToUpdateOrAdd) {
		for (StringArrayNode stringArray : ((StringLocalizationFile) locFile)
				.getStringArrays()) {
			arrayStringsToUpdateOrAdd.put(stringArray.getKey(), stringArray);
		}
	}

}
