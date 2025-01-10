/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.Collection;
import java.util.Map;

import org.eclipse.sequoyah.localization.tools.i18n.Messages;

/**
 * This class represents a Locale Attribute.
 * 
 * When creating a Locale Pack, a set of attributes is used for defining this
 * pack. For instance, I can have a locale pack for "English - US - Landscape",
 * each one of this characteristic is an attribute, and these 3 attributes
 * define this specific locale pack.
 * 
 * An attribute has a name, type, values (display and folder values), and
 * optionally a set of allowed values.
 */
public class LocaleAttribute {

	/***
	 * Types allowed for the locale attributes. Not all of them are used in a
	 * the L10n schema
	 */
	public static int STRING_TYPE = 0;

	public static int FIXED_TEXT_TYPE = 1; // A string with one of the

	// pre-defines allowed type

	protected String displayName = ""; //$NON-NLS-1$

	protected int type = STRING_TYPE;

	protected int fixedSize = 0; // Size of the attribute. Zero means that it

	// does not have fixed size
	protected int maximumSize = 0; // Maximum size of the attribute. Zero means

	// that it does not have a maximum size
	protected Map<String, String> allowedValues = null;

	protected String displayValue = null; // Human readable name to be used for

	// display purposes
	protected String folderValue = null; // Value to be used when creating

	// folder names

	/***
	 * The constructor: creates a new LocaleAttribute object
	 * 
	 * @param displayName
	 *            Name of this attribute in a human readable format, for display
	 *            purposes.
	 * @param type
	 *            Type of this attribute (one of the pre-defined types).
	 * @param size
	 *            Size of this attribute value. 0 means it does not have fixed
	 *            size.
	 * @param maxSize
	 *            Maximum size of this attribute value. 0 means it does not a
	 *            maximum size.
	 * @param allowedValues
	 *            Map containing all values that are allowed (used if the type
	 *            is FIXED_TEXT_TYPE). Each allowed value in its human readable
	 *            format should map to the format actually used for the file
	 *            creation.
	 * @param displayValue
	 *            Value for this attribute as a human readable string (for
	 *            display purposes).
	 * @param folderValue
	 *            Value for this attribute to be used in folder names.
	 */
	public LocaleAttribute(String displayName, int type, int size, int maxSize,
			Map<String, String> allowedValues, String displayValue,
			String folderValue) {
		if (displayName == null) {
			throw new IllegalArgumentException(
					Messages.LocaleAttribute_Exception_NameCannotBeNull);
		}

		// For the case of FIXED_TEXT_TYPE, check if the value is coherent
		if (type == FIXED_TEXT_TYPE) {
			if (allowedValues == null) {
				throw new IllegalArgumentException(
						Messages.LocaleAttribute_Exception_AllowedTypesNeeded);
			}

			if (!allowedValues.containsKey(displayValue)) {
				throw new IllegalArgumentException(
						Messages.LocaleAttribute_Exception_ValueNotAllowed);
			}
		}

		// Sets the initial parameters of this attribute
		this.displayName = displayName;
		this.setAllowedValues(allowedValues);
		this.type = type;
		this.displayValue = displayValue;
		this.folderValue = folderValue;
		this.fixedSize = size;
		this.maximumSize = maxSize;
	}

	/***
	 * Checks is a given value is allowed for this attribute. When checking if a
	 * value is allowed you MUST use the displayValue.
	 * 
	 * @param value
	 *            Value to be checked
	 * @return true is the displayValue is allowed.
	 */
	public boolean isValueAllowed(String value) {
		boolean result = false;
		if (type != FIXED_TEXT_TYPE) {
			// If type is not FIXED_TEXT_TYPE, any value is allowed
			result = true;
		}
		if (allowedValues != null) {
			result = allowedValues.containsKey(value);
		}

		return result;
	}

	/**
	 * Gets the display name of this attribute. The display name is a human
	 * readable name, for display purposes Do not mistake with the displayValue
	 * that is the value of this field for display purposes.
	 * 
	 * @return the name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Sets the display name of this attribute. The display name is a human
	 * readable name, for display purposes. Do not mistake with the displayValue
	 * that is the value of this field for display purposes.
	 * 
	 * @param name
	 *            the name
	 */
	public void setDisplayName(String name) {
		this.displayName = name;
	}

	/**
	 * Gets the type of this attribute
	 * 
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the type of this attribute
	 * 
	 * @param type
	 *            the type
	 */
	public void setType(int type) {
		if ((type < STRING_TYPE) || (type > FIXED_TEXT_TYPE)) {
			throw new IllegalArgumentException(
					Messages.LocaleAttribute_Exception_TypeDontExist);
		}
		this.type = type;
	}

	/***
	 * Sets the max size for the value of this attribute. 0 means that attribute
	 * has no maximum size.
	 * 
	 * @param size
	 */
	public void setMaxSize(int size) {
		this.maximumSize = size;
	}

	/***
	 * Gets the max size for the value of this attribute. 0 means that attribute
	 * has no maximum size.
	 * 
	 * @return the size
	 */
	public int getMaxSize() {
		return maximumSize;
	}

	/***
	 * Sets the size for the value of this attribute. 0 means that attribute has
	 * no fixed size.
	 * 
	 * @param size
	 */
	public void setSize(int size) {
		this.fixedSize = size;
	}

	/***
	 * Gets the size for the value of this attribute. 0 means that attribute has
	 * no fixed size.
	 * 
	 * @return the size
	 */
	public int getSize() {
		return fixedSize;
	}

	/***
	 * Sets the set of allowed display values for this attribute (used when the
	 * attribute type is FIXED_TEXT_TYPE).
	 * 
	 * @param allowedValues
	 *            the allowed display values for this attribute. The map relates
	 *            the display values with the correspondent values used for
	 *            folder names.
	 */
	public void setAllowedValues(Map<String, String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	/***
	 * Gets the set of allowed types for this attribute
	 * 
	 * @return the allowed values for this attribute (human readable form).
	 */
	public Collection<String> getAllowedValues() {
		if (allowedValues == null) {

			return null;
		} else {
			return allowedValues.keySet();
		}
	}

	/***
	 * Sets the value of this attribute.
	 * 
	 * @param displayValue
	 *            Human readable value for display purposes
	 * @param folderValue
	 *            Value to be used for folder names
	 */
	public void setValues(String displayValue, String folderValue) {

		// Do not allow null values for an attribute
		if ((displayValue == null) || (folderValue == null)) {
			throw new IllegalAccessError(
					Messages.LocaleAttribute_Exception_ValueNotNull);
		}

		// Checks if the value is allowed
		if (type == FIXED_TEXT_TYPE) {
			if (!allowedValues.containsKey(displayValue)) {
				throw new IllegalAccessError(
						Messages.LocaleAttribute_Exception_ValueNotAllowed2);
			}
		}

		this.displayValue = displayValue;
		this.folderValue = folderValue;
	}

	/***
	 * Sets both display value and folder value based on a display value. The
	 * folder value is retrieved from the allowedValues map. IMPORTANT: This
	 * method should only be used if the attribute type is FIXED_TEXT_TYPE.
	 * 
	 * NOTE: This mechanism of setting folder values based on display values is
	 * not completely implemented yet. For now this method is setting both
	 * values as equal.
	 * 
	 * @param displayValue
	 */
	public void setValuesBasedOnDisplayValue(String displayValue) {
		if (displayValue == null) {
			// Display value cannot be null
			throw new IllegalArgumentException(
					Messages.LocaleAttribute_Exception_ValueNotNull2);
		}

		if (type != FIXED_TEXT_TYPE) {
			// Method only makes sense if attribute of of fixed text type
			throw new IllegalArgumentException(
					Messages.LocaleAttribute_Exception_NotAllowedMethod);
		}

		if (allowedValues == null) {
			// For fixed text types, there must be allowed values
			// throw new
			// IllegalArgumentException(Messages.LocaleAttribute_Exception_NullAllowedValues);
		} else if (!allowedValues.containsKey(displayValue)) {
			// Display value is not allowed
			// throw new
			// IllegalArgumentException(Messages.LocaleAttribute_Exception_ValueNotAllowed3);
		}

		// Finally, sets the values
		this.displayValue = displayValue;
		this.folderValue = displayValue;
	}

	/***
	 * Gets the display value of this attribute. Its a human readable value to
	 * be used for display purposes
	 * 
	 * @return the value
	 */
	public String getDisplayValue() {
		return displayValue;

	}

	/***
	 * Gets the folder value of this attribute. Its value to be used for
	 * creating folders
	 * 
	 * @return the value
	 */
	public String getFolderValue() {
		return folderValue;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		boolean equal = false;

		if (arg0 instanceof LocaleAttribute) {

			equal = this.displayName.equals(((LocaleAttribute) arg0)
					.getDisplayName())
					&& this.folderValue.equals(((LocaleAttribute) arg0)
							.getFolderValue());
		}

		return equal;
	}
}
