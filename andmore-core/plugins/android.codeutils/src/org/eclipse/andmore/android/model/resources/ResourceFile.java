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

package org.eclipse.andmore.android.model.resources;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.model.resources.parser.AbstractResourceFileParser;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode;
import org.eclipse.andmore.android.model.resources.types.AbstractSimpleNameResourceNode;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.UnknownNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Class that represents a resource file
 */
@SuppressWarnings("deprecation")
public class ResourceFile extends AbstractResourceFileParser {
	/**
	 * Adds a resource entry to the resources file
	 * 
	 * @param node
	 *            The entry to be added
	 * @return true if the entry has been added or false otherwise
	 */
	public boolean addResourceEntry(AbstractResourceNode node) {
		boolean added = false;

		if (!rootNodes.contains(node)) {
			rootNodes.add(node);
			added = true;
		}

		return added;
	}

	/**
	 * Removes a resource entry from the resources file
	 * 
	 * @param node
	 *            the entry to be removed
	 * @return true if the entry has been removed or false otherwise
	 */
	public boolean removeResourceEntry(AbstractResourceNode node) {
		boolean removed = false;

		if (rootNodes.contains(node)) {
			rootNodes.remove(node);
			removed = true;
		}

		return removed;
	}

	/**
	 * Retrieves an array containing all root nodes of the resources file. If
	 * the file is well-formed, only the <resources> node must be present in the
	 * array.
	 * 
	 * @return an array containing all root nodes of the resources file.
	 */
	public AbstractResourceNode[] getResourceEntries() {
		AbstractResourceNode[] nodes = new AbstractResourceNode[rootNodes.size()];

		nodes = rootNodes.toArray(nodes);

		return nodes;
	}

	/**
	 * Retrieves the <resources> main node
	 * 
	 * @return the <resources> main node or null if it does not exist.
	 */
	public ResourcesNode getResourcesNode() {
		ResourcesNode resourcesNode = null;

		for (AbstractResourceNode node : rootNodes) {
			if (node.getNodeType() == NodeType.Resources) {
				resourcesNode = (ResourcesNode) node;
				break;
			}
		}

		return resourcesNode;
	}

	/**
	 * Retrieves an IDocument object containing the xml content for the file
	 * 
	 * @return an IDocument object containing the xml content for the file
	 */
	public IDocument getContent() throws AndroidException {
		IDocument document = null;
		DocumentBuilder documentBuilder = null;

		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			StudioLogger.error(ResourceFile.class, CodeUtilsNLS.EXC_ResourceFile_ErrorCreatingTheDocumentBuilder, e);
			throw new AndroidException(CodeUtilsNLS.EXC_ResourceFile_ErrorCreatingTheDocumentBuilder);
		}

		Document xmlDocument = documentBuilder.newDocument();

		for (AbstractResourceNode node : rootNodes) {
			addNode(xmlDocument, null, node);
		}

		document = new org.eclipse.jface.text.Document(getXmlContent(xmlDocument));

		return document;
	}

	/**
	 * Recursive function to build a XML file from AbstractResourceNode objects
	 * 
	 * @param xmlDocument
	 *            The XML Document
	 * @param xmlParentNode
	 *            The XML parent node
	 * @param nodeToAdd
	 *            The AndroidManifestNode to be added
	 */
	private void addNode(Document xmlDocument, Node xmlParentNode, AbstractResourceNode nodeToAdd) {
		Node xmlNode = xmlDocument.createElement(nodeToAdd.getNodeName());
		String[] attributes = nodeToAdd.getAttributes();
		String[] unknownAttributes = nodeToAdd.getUnknownAttributes();
		AbstractResourceNode[] children = nodeToAdd.getChildNodes();
		AbstractResourceNode[] unknownChildren = nodeToAdd.getUnknownChildNodes();

		// Sets the node value
		if (nodeToAdd instanceof AbstractSimpleNameResourceNode) {
			AbstractSimpleNameResourceNode asnrNode = (AbstractSimpleNameResourceNode) nodeToAdd;
			if (asnrNode.getNodeValue() != null) {
				xmlNode.appendChild(xmlDocument.createTextNode(asnrNode.getNodeValue()));
			}
		} else if (nodeToAdd.getNodeType() == NodeType.Unknown) {
			UnknownNode unknownNode = (UnknownNode) nodeToAdd;

			if (unknownNode.getNodeValue() != null) {
				xmlNode.appendChild(xmlDocument.createTextNode(unknownNode.getNodeValue()));
			}
		}

		// Adds valid attributes
		if (attributes.length > 0) {
			NamedNodeMap xmlAttributes = xmlNode.getAttributes();

			for (String attrName : attributes) {
				Attr attr = xmlDocument.createAttribute(attrName);
				attr.setValue(nodeToAdd.getAttributeValue(attrName));
				xmlAttributes.setNamedItem(attr);
			}
		}

		// Adds invalid attributes
		if (unknownAttributes.length > 0) {
			NamedNodeMap xmlAttributes = xmlNode.getAttributes();

			for (String attrName : unknownAttributes) {
				Attr attr = xmlDocument.createAttribute(attrName);
				attr.setValue(nodeToAdd.getUnknownAttributeValue(attrName));
				xmlAttributes.setNamedItem(attr);
			}
		}

		// Adds known child nodes
		for (AbstractResourceNode child : children) {
			addNode(xmlDocument, xmlNode, child);
		}

		// Adds unknown child nodes
		for (AbstractResourceNode child : unknownChildren) {
			addNode(xmlDocument, xmlNode, child);
		}

		if (xmlParentNode == null) {
			xmlDocument.appendChild(xmlNode);
		} else {
			xmlParentNode.appendChild(xmlNode);
		}
	}

	/**
	 * Creates the XML content from a XML Document
	 * 
	 * @param xmlDocument
	 *            The XML Document
	 * @return a String object containing the XML content
	 */
	private String getXmlContent(Document xmlDocument) throws AndroidException {
		// Despite Xerces is deprecated, its formatted xml source output works
		// better than W3C xml output classes
		OutputFormat outputFormat = new OutputFormat();
		XMLSerializer xmlSerializer = new XMLSerializer();
		StringWriter writer = null;
		String content = null;
		try {
			writer = new StringWriter();

			outputFormat.setEncoding("UTF-8");
			outputFormat.setLineSeparator(System.getProperty("line.separator"));
			outputFormat.setIndenting(true);

			xmlSerializer.setOutputCharStream(writer);
			xmlSerializer.setOutputFormat(outputFormat);

			xmlSerializer.serialize(xmlDocument);
			content = writer.toString();
		} catch (IOException e) {
			StudioLogger.error(ResourceFile.class, CodeUtilsNLS.EXC_ResourceFile_ErrorFormattingTheXMLOutput, e);
			throw new AndroidException(CodeUtilsNLS.EXC_ResourceFile_ErrorFormattingTheXMLOutput);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while retrieving resource xml content. "
							+ e.getMessage());
				}
			}
		}

		return content;
	}

	/**
	 * Returns a new resource name to create a new resource entry. This method
	 * grants that you are not creating a resource with a duplicated name.
	 * 
	 * @param baseName
	 *            The initial resource name
	 * @return The baseName value if a resource with this name does not exist or
	 *         a new suggested name otherwise
	 */
	public String getNewResourceName(String baseName) {
		int count = 0;
		String newName = baseName;
		boolean found = true;

		if (getResourcesNode() != null) {
			while (found) {
				found = false;
				for (AbstractResourceNode resNode : getResourcesNode().getChildNodes()) {
					newName = baseName + (count == 0 ? "" : "_" + Integer.toString(count));

					if (resNode instanceof AbstractSimpleNameResourceNode) {
						AbstractSimpleNameResourceNode validResNode = (AbstractSimpleNameResourceNode) resNode;
						if (validResNode.getName().equals(newName)) {
							found = true;
							count++;
							break;
						}
					}
				}
			}
		}

		return newName;
	}

}
