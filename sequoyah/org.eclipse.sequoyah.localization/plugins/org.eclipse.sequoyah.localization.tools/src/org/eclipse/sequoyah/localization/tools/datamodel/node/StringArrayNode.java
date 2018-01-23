/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcel Gorri (Eldorado)
 * Vinicius Hernandes (Eldorado)
 * 
 * Contributors:
 * Paulo Faria (Eldorado) - Add methods for not to lose comments on save (currently only on update)
 * Fabricio Violin (Eldorado) - Bug [317065] - Localization file initialization bug
 * Thiago Junqueira (Eldorado) - Bug [317065] - Override setKey method to correctly update the values map.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents an array entry in a localization file.
 */
public class StringArrayNode extends StringNode {

	/*
	 * Associated value for the language represented by the localizationFile
	 */
	private ArrayList<StringArrayItemNode> values = null;

	/**
	 * Constructor method
	 * 
	 * @param key
	 * @param value
	 */
	public StringArrayNode(String key) {
		super(key, null);
		this.values = new ArrayList<StringArrayItemNode>();
	}

	/**
	 * Get the value associated to the key for the language represented by the
	 * localizationFile
	 * 
	 * @return value associated to the key
	 */
	public List<StringArrayItemNode> getValues() {
		return values;
	}

	public StringArrayItemNode getArrayItemByIndex(int index) {
		StringArrayItemNode theNode = null;
		Iterator<StringArrayItemNode> iterator = values.iterator();
		while (iterator.hasNext() && theNode == null) {
			StringArrayItemNode item = iterator.next();
			if (item.getPosition() == index) {
				theNode = item;
			}
		}

		return theNode;
	}

	/**
	 * Retrieves the values contained in this array as a list of Strings
	 * 
	 * @return
	 */
	public List<String> getStringValues() {
		LinkedList<String> result = new LinkedList<String>();

		for (StringNode node : this.values) {
			result.add(node.getValue());
		}

		return result;
	}

	/**
	 * Set the value associated to the array for the language represented by the
	 * localizationFile
	 * 
	 * @param value
	 *            value associated to the array
	 * @return the StringNode created
	 */
	public StringArrayItemNode addValue(String value) {
		return addValue(value, -1);
	}

	/**
	 * Set the value associated to the array in a specific position for the
	 * language represented by the localizationFile
	 * 
	 * @param value
	 *            value associated to the key
	 * @param position
	 *            position where the new value will be added
	 * @return the StringNode created
	 */
	public StringArrayItemNode addValue(String value, int position) {
		return addValue(value, position, false);
	}

	public StringArrayItemNode addValue(String value, int position,
			boolean overwrite) {
		StringArrayItemNode newNode = null;
		int realPosition = position;

		if (position == -1) {
			realPosition = getLastIndex() + 1;
		}

		newNode = new StringArrayItemNode(value, this, realPosition);
		ensurePosition(overwrite ? position + 1 : position);
		if (overwrite) {
			values.set(realPosition, newNode);
		} else {
			values.add(realPosition, newNode);
		}

		// for each item in the end of the array, fix their position
		for (int i = realPosition + 1; i < values.size(); i++) {
			values.get(i).setPosition(values.get(i).getPosition() + 1);
		}

		return newNode;
	}

	private void ensurePosition(int index) {
		for (int i = values.size(); i < index; i++) {
			addValue(null, i);
		}
	}

	/**
	 * Get the last index of the array
	 * 
	 * @return the last index of the array
	 */
	private int getLastIndex() {
		int index = -1;
		if (values.size() > 0) {
			StringArrayItemNode last = values.get(values.size() - 1);
			index = last.getPosition();
		}

		return index;
	}

	/**
	 * Remove string item
	 * 
	 * @param stringNode
	 *            the StringNode to be removed
	 */
	public void removeValue(StringArrayItemNode stringNode) {
		int position = stringNode.getPosition();
		values.remove(position);
		// for each item in the end of the array, fix their position
		for (int i = position; i < values.size(); i++) {
			values.get(i).setPosition(values.get(i).getPosition() - 1);
		}
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
		return this.getKey().equals(((StringArrayNode) obj).getKey());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.localization.tools.datamodel.node.Node#setKey(java
	 * .lang.String)
	 */
	@Override
	public void setKey(String key) {
		super.setKey(key);
		for (StringArrayItemNode node : values) {
			node.setKey(key);
		}
	}

	@Override
	public String toString() {
		return "StringArrayNode [values=" + values + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
