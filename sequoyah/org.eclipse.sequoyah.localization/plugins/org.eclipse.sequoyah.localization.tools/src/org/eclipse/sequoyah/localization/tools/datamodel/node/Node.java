/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Augusto Gorri (Eldorado) - Bug 323036 - Add support to other Localizable Resources
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel.node;

import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;

/**
 * This class represents an entry in a localization file.
 * 
 * It basically stores a <key>:<value> pair, which contains the key used in the
 * localization process and the associated value in that specific language
 * represented by the given localization file.
 * 
 * It also stores or refers to any state data or meta-data associated with the
 * <key>:<value> pair that is represented by it.
 */
public class Node implements Comparable<Node> {

	/*
	 * The LocalizationFile which the StringNode belongs to
	 */
	protected LocalizationFile localizationFile = null;

	/*
	 * Key used in the localization process
	 */
	protected String key = null;

	/*
	 * Associated value for the language represented by the localizationFile
	 */
	protected String value = null;

	/*
	 * Whether the value has been edited by the Localization Editor and differs
	 * from the value saved in the localizationFile
	 */
	protected boolean dirty;

	/*
	 * Whether there are changes in the associated meta-data
	 */
	protected boolean dirtyMetaData;

	/*
	 * Whether there are changes in the associated extra-info
	 */
	protected boolean dirtyExtraInfo;

	/*
	 * Comment associated to the node
	 */
	protected NodeComment nodeComment = null;

	/**
	 * Default constructor method
	 * 
	 */
	public Node() {
	}

	/**
	 * Constructor method
	 * 
	 * @param key
	 * @param value
	 */
	public Node(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Get the LocalizationFile which the StringNode belongs to
	 * 
	 * @return the LocalizationFile which the StringNode belongs to
	 */
	public LocalizationFile getLocalizationFile() {
		return localizationFile;
	}

	/**
	 * Set the LocalizationFile which the StringNode belongs to
	 * 
	 * @param localizationFile
	 *            the LocalizationFile which the StringNode belongs to
	 */
	public void setLocalizationFile(LocalizationFile localizationFile) {
		this.localizationFile = localizationFile;
	}

	/**
	 * Get the key used in the localization process
	 * 
	 * @return key used in the localization process
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key used in the localization process
	 * 
	 * @param key
	 *            key used in the localization process
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the value associated to the key for the language represented by the
	 * localizationFile
	 * 
	 * @return value associated to the key
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value associated to the key for the language represented by the
	 * localizationFile
	 * 
	 * @param value
	 *            value associated to the key
	 */
	public void setValue(String value) {
		if ((this.value != null) && (!this.value.equals(value))) {
			setDirty(true);
		}
		this.value = value;
	}

	/**
	 * Check whether the value has been edited by the Localization Editor and
	 * differs from the value saved in the localizationFile
	 * 
	 * @return true if he value has been edited by the Localization Editor and
	 *         differs from the value saved in the localizationFile, false
	 *         otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Set whether the value has been edited by the Localization Editor and
	 * differs from the value saved in the localizationFile
	 * 
	 * @param dirty
	 *            true if he value has been edited by the Localization Editor
	 *            and differs from the value saved in the localizationFile,
	 *            false otherwise
	 */
	public void setDirty(boolean dirty) {
		// propagate the state if dirty = true
		if (dirty) {
			this.getLocalizationFile().setDirty(dirty);
		}
		this.dirty = dirty;
	}

	/**
	 * Check whether there are changes in the associated meta-data
	 * 
	 * @return true if there are changes in the associated meta-data, false
	 *         otherwise
	 */
	public boolean isDirtyMetaData() {
		return dirtyMetaData;
	}

	/**
	 * Set whether there are changes in the associated meta-data
	 * 
	 * @param dirtyMetaData
	 *            true if there are changes in the associated meta-data, false
	 *            otherwise
	 */
	public void setDirtyMetaData(boolean dirtyMetaData) {
		this.dirtyMetaData = dirtyMetaData;
	}

	/**
	 * Check whether there are changes in the associated extra-info
	 * 
	 * @return true if there are changes in the associated extra-info, false
	 *         otherwise
	 */
	public boolean isDirtyExtraInfo() {
		return dirtyExtraInfo;
	}

	/**
	 * Set whether there are changes in the associated extra-info
	 * 
	 * @param dirtyExtraInfo
	 *            true if there are changes in the associated extra-info, false
	 *            otherwise
	 */
	public void setDirtyExtraInfo(boolean dirtyExtraInfo) {
		this.dirtyExtraInfo = dirtyExtraInfo;
	}

	/**
	 * Get the comment associated to the node
	 * 
	 * @return comment associated to the node
	 */
	public NodeComment getNodeComment() {
		return nodeComment;
	}

	/**
	 * Set the comment associated to the node
	 * 
	 * @param stringNodeComment
	 *            comment to be associated to the node
	 */
	public void setNodeComment(NodeComment nodeComment) {
		this.nodeComment = nodeComment;
		if (this.nodeComment != null) {
			this.nodeComment.setNode(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo()
	 */
	public int compareTo(Node o) {
		return this.getKey().compareTo(o.getKey());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [localizationFile=" + localizationFile + ", key=" + key //$NON-NLS-1$ //$NON-NLS-2$
				+ ", value=" + value + ", dirty=" + dirty + ", nodeComment=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ nodeComment + "]"; //$NON-NLS-1$
	}

}