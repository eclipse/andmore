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
 * Matheus Tait Lima (Eldorado) - Bug [326793] - Improvements on the String Arrays handling
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel.node;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;

/**
 * This class represents an array entry in a localization file.
 */
public class StringArray {

	/*
	 * The LocalizationFile which the StringArray belongs to
	 */
	private StringLocalizationFile localizationFile = null;

	/*
	 * Key used in the localization process
	 */
	private String key = null;

	/*
	 * Associated value for the language represented by the localizationFile
	 */
	private Map<Integer, StringNode> values = null;

	/**
	 * Constructor method
	 * 
	 * @param key
	 * @param value
	 */
	public StringArray(String key) {
		this.key = key;
		this.values = new HashMap<Integer, StringNode>();
	}

	/**
	 * Get the LocalizationFile which the StringArray belongs to
	 * 
	 * @return the LocalizationFile which the StringArray belongs to
	 */
	public StringLocalizationFile getLocalizationFile() {
		return localizationFile;
	}

	/**
	 * Set the LocalizationFile which the StringArray belongs to
	 * 
	 * @param localizationFile
	 *            the LocalizationFile which the StringArray belongs to
	 */
	public void setLocalizationFile(StringLocalizationFile localizationFile) {
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
	public List<StringNode> getValues() {
		List<StringNode> result = new ArrayList<StringNode>();
		int lastIndex = getLastIndex();
		if (lastIndex >= 0) {
			StringNode[] stringNodes = new StringNode[lastIndex + 1];

			for (int i = 0; i <= lastIndex; i++) {
				stringNodes[i] = new StringNode("", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}

			for (Map.Entry<Integer, StringNode> entry : this.values.entrySet()) {
				stringNodes[entry.getKey()] = entry.getValue();
			}
			result = Arrays.asList(stringNodes);
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
	public StringNode addValue(String value) {
		return addValue(value, null);
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
	public StringNode addValue(String value, Integer position) {
		StringNode newNode = null;

		if (position == null) {
			int lastNumber = -1;
			if (this.values.size() > 0) {
				// Generate new key
				StringNode lastNode = this.values.get(this.values.size() - 1);
				lastNumber = StringArray.findItemPosition(lastNode.getKey());
			}
			position = lastNumber + 1;
		}

		DecimalFormat formatter = new DecimalFormat("000"); //$NON-NLS-1$
		String virtualKey = this.key + "_" //$NON-NLS-1$
				+ formatter.format(position.intValue());

		newNode = new StringNode(virtualKey, value);
		this.values.put(new Integer(position), newNode);

		return newNode;
	}

	/**
	 * Get the last index of the array
	 * 
	 * @return the last index of the array
	 */
	private int getLastIndex() {
		int lastIndex = -1;
		for (Map.Entry<Integer, StringNode> entry : this.values.entrySet()) {
			if (entry.getKey().intValue() > lastIndex) {
				lastIndex = entry.getKey().intValue();
			}
		}
		return lastIndex;
	}

	/**
	 * Remove string item
	 * 
	 * @param stringNode
	 *            the StringNode to be removed
	 */
	public void removeValue(StringNode stringNode) {
		// int position = Integer.parseInt(stringNode.getKey().split("_")[1]);
		int position = StringArray.findItemPosition(stringNode.getKey());
		this.values.remove(new Integer(position));
	}

	/**
	 * Check if a key is part of this array
	 * 
	 * @param key
	 *            the entry key
	 * @return true if it's part of this array, false otherwise
	 */
	public boolean isPartOfTheArray(String key) {
		boolean result = false;
		// String arrayKey = key.split("_")[0];
		String arrayKey = StringArray.getArrayKeyFromItemKey(key);
		if (arrayKey != null) {
			result = this.key.equals(arrayKey);
		}
		return result;
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
		return this.getKey().equals(((StringArray) obj).getKey());
	}

	/**
	 * Check if it's an array item based on its key
	 * 
	 * @param key
	 *            item key
	 * @return true if it's an array item, false otherwise
	 */
	public static boolean isArrayItem(String key) {
		boolean result = true;

		try {
			int separatorIndex = key.lastIndexOf("_"); //$NON-NLS-1$
			String arrayKey = key.substring(0, separatorIndex);
			String itemPosition = key.substring(separatorIndex + 1, key
					.length());
			if ((arrayKey == null) || (arrayKey.equals(""))) { //$NON-NLS-1$
				result = false;
			}
			if ((itemPosition == null) || (itemPosition.equals("")) //$NON-NLS-1$
					|| (itemPosition.length() != 3)) {
				result = false;
			}
			Integer.parseInt(itemPosition);
		} catch (Exception e) {
			result = false;
		}

		return result;
	}

	/**
	 * Find the position of an array item
	 * 
	 * @param key
	 *            array item key
	 * @return position of the array item
	 */
	public static int findItemPosition(String key) {
		int position = -1;

		try {
			int separatorIndex = key.lastIndexOf("_"); //$NON-NLS-1$
			position = Integer.parseInt(key.substring(separatorIndex + 1, key
					.length()));
		} catch (Exception e) {
			// do nothing
		}

		return position;
	}

	/**
	 * Get the array key of the array item passed as parameter
	 * 
	 * @param key
	 *            array item key
	 * @return the array key of the array item passed as parameter
	 */
	public static String getArrayKeyFromItemKey(String key) {
		String result = null;

		try {
			int separatorIndex = key.lastIndexOf("_"); //$NON-NLS-1$
			result = key.substring(0, separatorIndex);
		} catch (Exception e) {
			// do nothing
		}

		return result;
	}

}
