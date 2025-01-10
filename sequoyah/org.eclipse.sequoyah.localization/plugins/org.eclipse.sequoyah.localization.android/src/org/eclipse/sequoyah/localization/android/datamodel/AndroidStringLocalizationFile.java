/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Paulo Faria (Eldorado) - Add methods for not to lose comments on save
 * Daniel Pastore (Eldorado) - Bug 323036 - Add support to other Localizable Resources
 * Matheus Lima (Eldorado) - Bug 326793 - Updating data model so the Array Strings is now a new class
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.localization.android.manager.LocalizationFileManagerFactory;
import org.eclipse.sequoyah.localization.android.manager.StringLocalizationFileManager;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileFactory;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayItemNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.w3c.dom.Document;

/**
 * This class represents a real Android localization file in the project and
 * contains information about XML file saved
 * 
 */
public class AndroidStringLocalizationFile extends StringLocalizationFile {

	static {
		LocalizationFileFactory.getInstance().addFileType(
				AndroidStringLocalizationFile.class.getName(),
				AndroidStringLocalizationFile.class);

		LocalizationFileManagerFactory.getInstance().addManager(
				AndroidStringLocalizationFile.class.getName(),
				StringLocalizationFileManager.class);
	}

	/**
	 * Saved XML (it is used not to lose comments on updates)
	 */
	private Document savedXMLDocument = null;

	/**
	 * Kept to remove a single entry to be removed from savedXMLDocument in the
	 * next save action
	 */
	private Map<String, StringNode> singleEntryToRemove = new HashMap<String, StringNode>();

	/**
	 * Kept to remove only one item inside array to be removed from
	 * savedXMLDocument in the next save action
	 */
	private List<StringArrayItemNode> arrayItemsToRemove = new ArrayList<StringArrayItemNode>();

	/**
	 * Kept to remove an entire array to be removed from savedXMLDocument in the
	 * next save action
	 */
	private Map<String, StringArrayNode> arraysToRemove = new HashMap<String, StringArrayNode>();

	/**
	 * 
	 * @param file
	 * @param localeInfo
	 * @param stringNodes
	 * @param stringArrays
	 */
	public AndroidStringLocalizationFile(LocalizationFileBean bean) {
		super(bean);
	}

	/**
	 * @return the savedXMLDocument
	 */
	public Document getSavedXMLDocument() {
		return savedXMLDocument;
	}

	/**
	 * @param savedXMLDocument
	 *            the savedXMLDocument to set
	 */
	public void setSavedXMLDocument(Document savedXMLDocument) {
		this.savedXMLDocument = savedXMLDocument;
	}

	/**
	 * Removes item from top level node
	 */
	public void removeStringNode(StringNode stringNode) {
		if (containsKey(stringNode.getKey())) {
			// top level: array or string
			removeNode(stringNode.getKey());
			this.setDirty(true);
			// check if it's is an array
			if (stringNode instanceof StringArrayNode) {
				StringArrayNode arrayNode = (StringArrayNode) stringNode;
				arraysToRemove.put(arrayNode.getKey(), arrayNode);
			} else {
				// mark single entry to be removed
				singleEntryToRemove.put(stringNode.getKey(), stringNode);
			}
		}
	}

	/**
	 * Adds StringNode. If necessary undoes removal mark from StringNode fixing
	 * maps to update savedXMLDocument file
	 * 
	 * @param stringNode
	 */
	public StringNode addStringNode(StringNode stringNode) {
		StringNode node = super.addStringNode(stringNode);
		// check if it's is an array
		if (stringNode instanceof StringArrayNode) {
			StringArrayNode arrayNode = (StringArrayNode) stringNode;
			if (arraysToRemove.containsKey(arrayNode.getKey())) {
				arraysToRemove.remove(arrayNode.getKey());
			}
		} else {
			// unmark single entry to be removed
			if (singleEntryToRemove.containsKey(stringNode.getKey())) {
				singleEntryToRemove.remove(stringNode.getKey());
			}
		}
		return node;
	}

	/**
	 * Removes from non top level node
	 * 
	 * @param node
	 * @param index
	 */
	public void removeStringArrayItemNode(StringArrayItemNode child) {
		super.removeStringArrayItemNode(child);
		if (child.getParent().getValues().size() == 0) {
			// item removal make the array useless => remove array
			arraysToRemove.put(child.getParent().getKey(), child.getParent());
			removeNode(child.getParent().getKey());
		} else {
			arrayItemsToRemove.add(child);
		}
	}

	/**
	 * Adds StringArrayItemNode. If necessary undoes removal mark from
	 * StringArrayItemNode fixing maps to update savedXMLDocument file
	 * 
	 * @param stringNode
	 */
	public StringNode addStringArrayItemNode(StringArrayItemNode child) {
		StringNode node = super.addStringArrayItemNode(child);
		if (child.getParent() != null
				&& child.getParent().getValues().size() == 0) {
			// undo removal from item that made the array useless => return
			// array
			arraysToRemove.remove(child.getParent().getKey());
			addStringNode(child.getParent());
		} else {
			// undo removal from item
			// find item on list
			Iterator<StringArrayItemNode> iter = arrayItemsToRemove.iterator();
			while (iter.hasNext()) {
				StringArrayItemNode currentNode = iter.next();
				if (child.getParent().equals(currentNode.getParent())
						&& child.getPosition() == currentNode.getPosition()) {
					iter.remove();
				}
			}
		}
		return node;
	}

	/**
	 * @return the singleEntryToRemove
	 */
	public Map<String, StringNode> getSingleEntryToRemove() {
		return singleEntryToRemove;
	}

	/**
	 * @return the arrayItemsToRemove
	 */
	public List<StringArrayItemNode> getArrayItemsToRemove() {
		return arrayItemsToRemove;
	}

	/**
	 * @return the arraysToRemove
	 */
	public Map<String, StringArrayNode> getArrayEntryToRemove() {
		return arraysToRemove;
	}
}
