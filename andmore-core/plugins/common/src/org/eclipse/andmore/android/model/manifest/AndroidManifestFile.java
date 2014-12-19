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

package org.eclipse.andmore.android.model.manifest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidStatus;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.model.manifest.dom.AbstractBuildingBlockNode;
import org.eclipse.andmore.android.model.manifest.dom.ActionNode;
import org.eclipse.andmore.android.model.manifest.dom.ActivityNode;
import org.eclipse.andmore.android.model.manifest.dom.AndroidManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.CommentNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.AndroidManifestNode.NodeType;
import org.eclipse.andmore.android.model.manifest.parser.AndroidManifestParser;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Class that represents an AndroidManifest.xml file
 */
@SuppressWarnings("deprecation")
public class AndroidManifestFile extends AndroidManifestParser {
	/**
	 * Adds an AndroidManifestNode to the file
	 * 
	 * @param node
	 *            The node to be added
	 */
	public void addNode(AndroidManifestNode node) {
		if ((node != null) && !rootNodes.contains(node)) {
			rootNodes.add(node);
		}
	}

	/**
	 * Removes an AndroidManifestNode from the file
	 * 
	 * @param node
	 *            The node to be removed
	 */
	public void removeNode(AndroidManifestNode node) {
		if ((node != null) && !rootNodes.contains(node)) {
			rootNodes.add(node);
		}
	}

	/**
	 * Retrieves an array containing all nodes present on xml root. If the file
	 * is valid, there will be only one node, the <manifest> node
	 * 
	 * @return an array containing all nodes present on xml root.
	 */
	public AndroidManifestNode[] getNodes() {
		AndroidManifestNode[] nodes = new AndroidManifestNode[rootNodes.size()];
		nodes = rootNodes.toArray(nodes);

		return nodes;
	}

	/**
	 * Retrieves the <manifest> node
	 * 
	 * @return the <manifest> node
	 */
	public ManifestNode getManifestNode() {
		ManifestNode manifestNode = null;

		for (AndroidManifestNode node : rootNodes) {
			if (node.getNodeType() == NodeType.Manifest) {
				manifestNode = (ManifestNode) node;
				break;
			}
		}

		return manifestNode;
	}

	/**
	 * Retrieves the <application> node from the manifest file
	 * 
	 * @return The <application> node of the manifest file.
	 */
	public ApplicationNode getApplicationNode() {
		// Retrieve <manifest> node and return application node
		return getManifestNode().getApplicationNode();

	}

	/**
	 * Retrieves a building block node, which can be of the following types:
	 * NodeType.Activity NodeType.Provider NodeType.Receiver NodeType.Service
	 * 
	 * @param type
	 *            The NodeType.
	 * @param androidName
	 *            The android:name property value. Should be the fully qualified
	 *            name of the building block class. For example, for the
	 *            Activity class "Test" located in the package "com.motorola",
	 *            the androidName parameter should be "com.motorola.Teste"
	 * 
	 * @return A AbstractBuildingBlockNode that represents the building block.
	 *         If no matching node is found or the type passed is invalid, null
	 *         is returned.
	 */
	public AbstractBuildingBlockNode getBuildingBlockNode(NodeType type, String androidName) {
		// Result
		AbstractBuildingBlockNode resultNode = null;

		// Candidate list of nodes to iterate through
		List<AbstractBuildingBlockNode> candidateList = new LinkedList<AbstractBuildingBlockNode>();

		// Retrieve the Manifest node to check the default package
		ManifestNode manifestNode = getManifestNode();
		String manifestPackage = manifestNode.getNodeProperties().get(PROP_PACKAGE);

		// Compare the qualified name from the parameter with the manifest
		// package. If equal, we can use the class name for comparison purposes.
		String androidNamePackage = androidName.substring(0, androidName.lastIndexOf('.'));
		String shortAndroidName = new String();

		if (manifestPackage.equals(androidNamePackage)) {
			shortAndroidName = androidName.substring(androidName.lastIndexOf('.'));
		}

		// Retrieve the application node
		ApplicationNode applicationNode = getApplicationNode();

		// Check the building block type
		switch (type) {
		case Activity:
			candidateList.addAll(applicationNode.getActivityNodes());
			break;
		case Provider:
			candidateList.addAll(applicationNode.getProviderNodes());
			break;
		case Receiver:
			candidateList.addAll(applicationNode.getReceiverNodes());
			break;
		case Service:
			candidateList.addAll(applicationNode.getServiceNodes());
			break;
		default:
			break;
		}

		// Search the candidate list for the target node
		for (AbstractBuildingBlockNode node : candidateList) {
			// In the case that shortAndroidName is not null or empty, we check
			// if it's like that in the manifest first
			if ((shortAndroidName != null) && (shortAndroidName.length() > 0)) {
				if (node.getNodeProperties().get(PROP_NAME).equals(shortAndroidName)) {
					resultNode = node;
					break;
				}
			}

			if (node.getNodeProperties().get(PROP_NAME).equals(androidName)) {
				// We found the node!
				resultNode = node;
				break;
			}
		}

		return resultNode;
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
			StudioLogger.error(AndroidManifestFile.class,
					UtilitiesNLS.EXC_AndroidManifestFile_ErrorCreatingTheDocumentBuilder, e);
			throw new AndroidException(UtilitiesNLS.EXC_AndroidManifestFile_ErrorCreatingTheDocumentBuilder);
		}

		Document xmlDocument = documentBuilder.newDocument();

		for (AndroidManifestNode node : rootNodes) {
			addNode(xmlDocument, null, node);
		}

		document = new org.eclipse.jface.text.Document(getXmlContent(xmlDocument));

		return document;
	}

	/**
	 * Recursive function to build a XML file from AndroidManifestNode objects
	 * 
	 * @param xmlDocument
	 *            The XML Document
	 * @param xmlParentNode
	 *            The XML parent node
	 * @param nodeToAdd
	 *            The AndroidManifestNode to be added
	 */
	private void addNode(Document xmlDocument, Node xmlParentNode, AndroidManifestNode nodeToAdd) {
		Node xmlNode;

		if (nodeToAdd instanceof CommentNode) {
			CommentNode commentNode = (CommentNode) nodeToAdd;
			xmlNode = xmlDocument.createComment(commentNode.getComment());
		} else {
			xmlNode = xmlDocument.createElement(nodeToAdd.getNodeName());
			Map<String, String> attributes = nodeToAdd.getNodeProperties();
			Map<String, String> unknownAttributes = nodeToAdd.getNodeUnknownProperties();
			AndroidManifestNode[] children = nodeToAdd.getChildren();
			AndroidManifestNode[] unknown = nodeToAdd.getUnkownChildren();

			// Adds valid attributes
			if ((attributes != null) && (attributes.size() > 0)) {
				NamedNodeMap xmlAttributes = xmlNode.getAttributes();

				for (String attrName : attributes.keySet()) {
					Attr attr = xmlDocument.createAttribute(attrName);
					attr.setValue(attributes.get(attrName));
					xmlAttributes.setNamedItem(attr);
				}
			}

			// Adds invalid attributes
			if ((unknownAttributes != null) && (unknownAttributes.size() > 0)) {
				NamedNodeMap xmlAttributes = xmlNode.getAttributes();

				for (String attrName : unknownAttributes.keySet()) {
					Attr attr = xmlDocument.createAttribute(attrName);
					attr.setNodeValue(unknownAttributes.get(attrName));
					xmlAttributes.setNamedItem(attr);
				}
			}

			// Adds known child nodes
			for (AndroidManifestNode child : children) {
				addNode(xmlDocument, xmlNode, child);
			}

			// Adds unknown child nodes
			for (AndroidManifestNode child : unknown) {
				addNode(xmlDocument, xmlNode, child);
			}
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
		StringWriter writer = new StringWriter();
		String content = null;

		outputFormat.setEncoding("UTF-8");
		outputFormat.setLineSeparator(System.getProperty("line.separator"));
		outputFormat.setIndenting(true);

		xmlSerializer.setOutputCharStream(writer);
		xmlSerializer.setOutputFormat(outputFormat);

		try {
			xmlSerializer.serialize(xmlDocument);
			content = writer.toString();
		} catch (IOException e) {
			StudioLogger.error(AndroidManifestFile.class,
					UtilitiesNLS.EXC_AndroidManifestFile_ErrorFormattingTheXMLOutput, e);
			throw new AndroidException(UtilitiesNLS.EXC_AndroidManifestFile_ErrorFormattingTheXMLOutput);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}

		return content;
	}

	/**
	 * Gets all file problems: Errors and Warnings
	 * 
	 * @return all file problems
	 */
	public IStatus[] getProblems() {
		ManifestNode manifestNode = getManifestNode();
		IStatus[] errors;

		if (manifestNode == null) {
			errors = new IStatus[] { new AndroidStatus(IStatus.ERROR,
					UtilitiesNLS.ERR_AndroidManifestFile_TheFileAndroidManifestXmlIsMalFormed) };
		} else {
			errors = getManifestNode().getRecursiveNodeErrors();
		}

		return errors;
	}

	/**
	 * Gets all file errors
	 * 
	 * @return all file errors
	 */
	public IStatus[] getErrors() {
		List<IStatus> errors = new LinkedList<IStatus>();

		for (IStatus status : getProblems()) {
			if (status.getSeverity() == IStatus.ERROR) {
				errors.add(status);
			}
		}

		return errors.toArray(new IStatus[0]);
	}

	/**
	 * Checks if the file has errors in the model
	 * 
	 * @return true if the file has errors in the model and false otherwise
	 */
	public boolean hasErrors() {
		boolean hasErrors = false;

		for (IStatus status : getProblems()) {
			if (status.getSeverity() == IStatus.ERROR) {
				hasErrors = true;
				break;
			}
		}

		return hasErrors;
	}

	public AndroidManifestNode getNode(NodeType nodeType) {
		AndroidManifestNode requiredNode = null;
		AndroidManifestNode[] manifestChildren = null;
		for (AndroidManifestNode node : rootNodes) {
			if (node instanceof ManifestNode) {
				manifestChildren = ((ManifestNode) node).getChildren();
				break;
			}
		}

		if ((manifestChildren != null) && (manifestChildren.length > 0)) {
			for (AndroidManifestNode manifestChild : manifestChildren) {
				if (manifestChild.getNodeType().equals(nodeType)) {
					requiredNode = manifestChild;
					break;
				}
			}
		}
		return requiredNode;
	}

	/**
	 * This method sets the main activity of and android project be the class
	 * identified by {@code className}.
	 * 
	 * @param className
	 *            The name of the class to be set as the main activity.
	 * @param isMainActivity
	 *            If true, the activity will be set as main activity. If false,
	 *            the activity will no longer be a main activity.
	 * @return True if the activity exist, is declared on the manifest and was
	 *         successfully set as the main activity. False otherwise.
	 * */
	public boolean setAsMainActivity(String className, boolean isMainActivity) {
		boolean result = false;

		List<ActivityNode> activityNodes = getApplicationNode().getActivityNodes();

		for (ActivityNode activityNode : activityNodes) {
			if (activityNode.getName().equals(className)) {
				result = activityNode.setAsMainActivity(isMainActivity);
				break;
			}
		}

		return result;
	}

	/**
	 * Convenience method that returns the main activity of the application. The
	 * main activity is the one declared with the intent filter <action
	 * android:name="android.intent.action.MAIN"/> If more than one main
	 * activity is declared, then the first one declared is returned. This
	 * behavior follows the android behavior to choose the main activity.
	 * */
	public ActivityNode getMainActivity() {
		ActivityNode mainActivity = null;
		ApplicationNode appNode = getApplicationNode();
		List<ActivityNode> activities = appNode.getActivityNodes();

		for (ActivityNode activity : activities) {
			for (IntentFilterNode intent : activity.getIntentFilterNodes()) {
				for (ActionNode actionNode : intent.getActionNodes()) {
					if (actionNode.getNodeProperties().get("android:name").equals("android.intent.action.MAIN")) {
						mainActivity = activity;
						break;
					}
				}
				if (mainActivity != null) {
					break;
				}
			}
			if (mainActivity != null) {
				break;
			}
		}

		return mainActivity;
	}

}
