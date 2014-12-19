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

package org.eclipse.andmore.android.model.resources.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode;
import org.eclipse.andmore.android.model.resources.types.AbstractSimpleNameResourceNode;
import org.eclipse.andmore.android.model.resources.types.ColorNode;
import org.eclipse.andmore.android.model.resources.types.DimenNode;
import org.eclipse.andmore.android.model.resources.types.DrawableNode;
import org.eclipse.andmore.android.model.resources.types.IResourceTypesAttributes;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.StringNode;
import org.eclipse.andmore.android.model.resources.types.UnknownNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract class that implements methods to parse a resource file
 */
public class AbstractResourceFileParser implements IResourceTypesAttributes {
	/**
	 * The root nodes of the resource file
	 */
	protected final List<AbstractResourceNode> rootNodes;

	/**
	 * Default constructor
	 */
	public AbstractResourceFileParser() {
		rootNodes = new LinkedList<AbstractResourceNode>();
	}

	/**
	 * Parses an IDocument object containing a resource file content
	 * 
	 * @param document
	 *            the IDocument object
	 * @param sourceFileName
	 *            The resource file that is being parsed
	 * 
	 * @throws SAXException
	 *             When a parsing error occurs
	 * @throws IOException
	 *             When a reading error occurs
	 */
	public void parseDocument(IDocument document, String sourceFileName) throws AndroidException {
		Element element;
		DOMParser domParser = new DOMParser();

		rootNodes.clear();

		StringReader stringReader = null;
		try {
			stringReader = new StringReader(document.get());
			domParser.parse(new InputSource(stringReader));
		} catch (SAXException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_AbstractResourceFileParser_ErrorParsingTheXMLFile,
					sourceFileName, e.getLocalizedMessage());
			StudioLogger.error(AbstractResourceFileParser.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (IOException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_AbstractResourceFileParser_ErrorReadingTheXMLContent,
					sourceFileName, e.getLocalizedMessage());
			StudioLogger.error(AbstractResourceFileParser.class, errMsg, e);
			throw new AndroidException(errMsg);
		} finally {
			if (stringReader != null) {
				stringReader.close();
			}
		}

		element = domParser.getDocument().getDocumentElement();
		parseNode(element, null);
	}

	/**
	 * Parses a XML Node
	 * 
	 * @param element
	 *            The XML Node
	 * @param rootNode
	 *            The XML Node parent (An AbstractResourceNode that has been
	 *            parsed)
	 */
	private void parseNode(Node node, AbstractResourceNode rootNode) {
		if (node instanceof Element) {
			Element element = (Element) node;
			AbstractResourceNode arNode;
			Node xmlNode;
			NodeList xmlChildNodes;
			NamedNodeMap attributes = element.getAttributes();
			NodeType nodeType = identifyNode(element);

			switch (nodeType) {
			case Resources:
				arNode = parseResourcesNode();
				break;
			case String:
				arNode = parseStringNode(attributes);
				break;
			case Color:
				arNode = parseColorNode(attributes);
				break;
			case Dimen:
				arNode = parseDimenNode(attributes);
				break;
			case Drawable:
				arNode = parseDrawableNode(attributes);
				break;
			default:
				arNode = parseUnknownNode(node);
			}

			// Adds the child nodes
			xmlChildNodes = element.getChildNodes();

			for (int i = 0; i < xmlChildNodes.getLength(); i++) {
				xmlNode = xmlChildNodes.item(i);
				parseNode(xmlNode, arNode);
			}

			if (rootNode == null) {
				rootNodes.add(arNode);
			} else {
				rootNode.addChildNode(arNode);
			}
		} else if ((node instanceof Text) && (rootNode != null)) {
			if ((node.getNodeValue() != null) && (node.getNodeValue().trim().length() > 0)) {
				if (rootNode instanceof AbstractSimpleNameResourceNode) {
					AbstractSimpleNameResourceNode asnrNode = (AbstractSimpleNameResourceNode) rootNode;
					if (asnrNode.getNodeValue() == null) {
						asnrNode.setNodeValue(node.getNodeValue());
					}
				} else {
					UnknownNode unknownNode = (UnknownNode) rootNode;
					if (unknownNode.getNodeValue() == null) {
						unknownNode.setNodeValue(node.getNodeValue());
					}
				}
			}
		}
	}

	/**
	 * Identifies an XML Node type as an AbstractResourceNode type.
	 * 
	 * @param xmlNode
	 *            The XML Node
	 * @return The corresponding AndroidManifestNode type to the XML Node
	 */
	private NodeType identifyNode(Element xmlNode) {
		String nodeName = xmlNode.getNodeName();
		NodeType identifiedType = AbstractResourceNode.getNodeType(nodeName);
		return identifiedType;
	}

	/**
	 * Parses a <resources> node
	 * 
	 * @return An AbstractResourceNode object that represents the <resources>
	 *         node
	 */
	private AbstractResourceNode parseResourcesNode() {
		return new ResourcesNode();
	}

	/**
	 * Parses a <string> node
	 * 
	 * @param attributes
	 *            the node attributes list
	 * @return An AbstractResourceNode object that represents the <string> node
	 */
	private AbstractResourceNode parseStringNode(NamedNodeMap attributes) {
		StringNode arNode = new StringNode("");
		Node attribute;
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);

			attrName = attribute.getNodeName();
			attrValue = attribute.getNodeValue();

			if (attrName.equalsIgnoreCase(ATTR_NAME)) {
				arNode.setName(attrValue);
			} else {
				arNode.addUnknownAttribute(attrName, attrValue);
			}
		}

		return arNode;
	}

	/**
	 * Parses a <color> node
	 * 
	 * @param attributes
	 *            the node attributes list
	 * @return An AbstractResourceNode object that represents the <color> node
	 */
	private AbstractResourceNode parseColorNode(NamedNodeMap attributes) {
		ColorNode arNode = new ColorNode("");
		Node attribute;
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);

			attrName = attribute.getNodeName();
			attrValue = attribute.getNodeValue();

			if (attrName.equalsIgnoreCase(ATTR_NAME)) {
				arNode.setName(attrValue);
			} else {
				arNode.addUnknownAttribute(attrName, attrValue);
			}
		}

		return arNode;
	}

	/**
	 * Parses a <dimen> node
	 * 
	 * @param attributes
	 *            the node attributes list
	 * @return An AbstractResourceNode object that represents the <diment> node
	 */
	private AbstractResourceNode parseDimenNode(NamedNodeMap attributes) {
		DimenNode arNode = new DimenNode("");
		Node attribute;
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);

			attrName = attribute.getNodeName();
			attrValue = attribute.getNodeValue();

			if (attrName.equalsIgnoreCase(ATTR_NAME)) {
				arNode.setName(attrValue);
			} else {
				arNode.addUnknownAttribute(attrName, attrValue);
			}
		}

		return arNode;
	}

	/**
	 * Parses a <drawable> node
	 * 
	 * @param attributes
	 *            the node attributes list
	 * @return An AbstractResourceNode object that represents the <drawable>
	 *         node
	 */
	private AbstractResourceNode parseDrawableNode(NamedNodeMap attributes) {
		DrawableNode arNode = new DrawableNode("");
		Node attribute;
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);

			attrName = attribute.getNodeName();
			attrValue = attribute.getNodeValue();

			if (attrName.equalsIgnoreCase(ATTR_NAME)) {
				arNode.setName(attrValue);
			} else {
				arNode.addUnknownAttribute(attrName, attrValue);
			}
		}

		return arNode;
	}

	/**
	 * Parses an unknown node
	 * 
	 * @param node
	 *            The xml node
	 * @return An AbstractResourceNode object that represents the unknown node
	 */
	private AbstractResourceNode parseUnknownNode(Node node) {
		UnknownNode arNode = new UnknownNode(node.getNodeName());
		NamedNodeMap attributes = node.getAttributes();
		Node attribute;
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);

			attrName = attribute.getNodeName();
			attrValue = attribute.getNodeValue();

			arNode.addUnknownAttribute(attrName, attrValue);
		}

		return arNode;
	}
}
