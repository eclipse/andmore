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
package org.eclipse.andmore.android.model.manifest.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.model.manifest.dom.AndroidManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.AndroidManifestNode.NodeType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract class used to parse an AndroidManifest.xml file
 */
public abstract class AndroidManifestParser extends AndroidManifestNodeParser {
	/**
	 * The nodes present on the xml file root
	 */
	protected List<AndroidManifestNode> rootNodes = new LinkedList<AndroidManifestNode>();

	/**
	 * Parses an IDocument object containing the AndroidManifest.xml into a DOM
	 * 
	 * @param document
	 *            the IDocument object
	 * @throws SAXException
	 *             When a parsing error occurs
	 * @throws IOException
	 *             When a reading error occurs
	 */
	public void parseDocument(IDocument document) throws AndroidException {
		Node node;
		DOMParser domParser = new DOMParser();

		rootNodes.clear();

		StringReader stringReader = null;
		try {
			stringReader = new StringReader(document.get());
			domParser.parse(new InputSource(stringReader));
		} catch (SAXException e) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_AndroidManifestNodeParser_ErrorParsingTheXMLFile,
					e.getLocalizedMessage());
			StudioLogger.error(AndroidManifestParser.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (IOException e) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_AndroidManifestNodeParser_ErrorReadingTheXMLContent,
					e.getLocalizedMessage());
			StudioLogger.error(AndroidManifestParser.class, errMsg, e);
			throw new AndroidException(errMsg);
		} finally {
			if (stringReader != null) {
				stringReader.close();
			}
		}

		NodeList children = domParser.getDocument().getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			parseNode(node, null);
		}
	}

	/**
	 * Parses a XML Node
	 * 
	 * @param element
	 *            The XML Node
	 * @param rootNode
	 *            The XML Node parent (An AndroidManifestNode that has been
	 *            parsed)
	 */
	private void parseNode(Node node, AndroidManifestNode rootNode) {
		AndroidManifestNode amNode;
		NodeType nodeType = identifyNode(node);
		Node xmlNode;
		NodeList xmlChildNodes;
		NamedNodeMap attributes = node.getAttributes();

		switch (nodeType) {
		case Manifest:
			amNode = parseManifestNode(attributes);
			break;
		case UsesPermission:
			amNode = parseUsesPermissionNode(attributes);
			break;
		case Permission:
			amNode = parsePermissionNode(attributes);
			break;
		case PermissionTree:
			amNode = parsePermissionTreeNode(attributes);
			break;
		case PermissionGroup:
			amNode = parsePermissionGroupNode(attributes);
			break;
		case Instrumentation:
			amNode = parseInstrumentationNode(attributes);
			break;
		case UsesSdk:
			amNode = parseUsesSdkNode(attributes);
			break;
		case Application:
			amNode = parseApplicationNode(attributes);
			break;
		case Activity:
			amNode = parseActivityNode(attributes);
			break;
		case IntentFilter:
			amNode = parseIntentFilterNode(attributes);
			break;
		case Action:
			amNode = parseActionNode(attributes);
			break;
		case Category:
			amNode = parseCategoryNode(attributes);
			break;
		case Data:
			amNode = parseDataNode(attributes);
			break;
		case MetaData:
			amNode = parseMetadataNode(attributes);
			break;
		case ActivityAlias:
			amNode = parseActivityAliasNode(attributes);
			break;
		case Service:
			amNode = parseServiceNode(attributes);
			break;
		case Receiver:
			amNode = parseReceiverNode(attributes);
			break;
		case Provider:
			amNode = parseProviderNode(attributes);
			break;
		case GrantUriPermission:
			amNode = parseGrantUriPermissionNode(attributes);
			break;
		case UsesLibrary:
			amNode = parseUsesLibraryNode(attributes);
			break;
		case UsesFeature:
			amNode = parseUsesFeatureNode(attributes);
			break;
		case Comment:
			amNode = parseCommentNode((Comment) node);
			break;
		default:
			amNode = parseUnknownNode(node.getNodeName(), attributes);
		}

		xmlChildNodes = node.getChildNodes();

		for (int i = 0; i < xmlChildNodes.getLength(); i++) {
			xmlNode = xmlChildNodes.item(i);

			if ((xmlNode instanceof Element) || (xmlNode instanceof Comment)) {
				parseNode(xmlNode, amNode);
			}
		}

		if (rootNode == null) {
			rootNodes.add(amNode);
		} else {
			rootNode.addChild(amNode);
		}
	}

	/**
	 * Identifies a XML Node type as an AndroidManifestNode type.
	 * 
	 * @param xmlNode
	 *            The XML Node
	 * @return The corresponding AndroidManifestNode type to the XML Node
	 */
	private NodeType identifyNode(Node xmlNode) {
		NodeType identifiedType = NodeType.Unknown;
		String nodeName = xmlNode.getNodeName();
		String thisNodeName;

		if (xmlNode instanceof Comment) {
			identifiedType = NodeType.Comment;
		} else {
			for (NodeType nodeType : NodeType.values()) {
				thisNodeName = AndroidManifestNode.getNodeName(nodeType);

				if (thisNodeName.equalsIgnoreCase(nodeName)) {
					identifiedType = nodeType;
					break;
				}
			}
		}

		return identifiedType;
	}
}
