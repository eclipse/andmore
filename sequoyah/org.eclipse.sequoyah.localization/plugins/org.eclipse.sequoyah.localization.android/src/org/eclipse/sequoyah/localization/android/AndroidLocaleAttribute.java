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
 * name (company) - description.
 * Marcel Gorri (Eldorado) - Bug 325110 - Add support to new Android Localization qualifiers
 * Marcel Gorri (Eldorado) - Bug 325630 - Fix validation of some Android localization qualifiers
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android;

import java.awt.Dimension;
import java.util.HashMap;

import org.eclipse.sequoyah.localization.android.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.datamodel.LocaleAttribute;

public class AndroidLocaleAttribute extends LocaleAttribute {

	public enum AndroidLocaleAttributes {
		COUNTRY_CODE, NETWORK_CODE, LANGUAGE, REGION, SCREEN_SIZE, SCREEN_ASPECT, SCREEN_ORIENTATION, SMALLEST_WIDTH, AVAILABLE_WIDTH, AVAILABLE_HEIGHT, DOCK_MODE, NIGHT_MODE, PIXEL_DENSITY, TOUCH_TYPE, KEYBOARD_STATE, TEXT_INPUT_METHOD, NAVIGATION_KEY_STATE, NAVIGATION_METHOD, SCREEN_DIMENSION, API_VERSION, COUNT
	};

	private int androidType;

	private String stringValue = ""; //$NON-NLS-1$

	protected boolean isSet = false;

	/***
	 * Creates a new AndroidLocaleAttribute
	 * 
	 * @param value
	 * @param androidType
	 */
	public AndroidLocaleAttribute(Object value, int androidType) {
		super("", LocaleAttribute.STRING_TYPE, 0, 0, null, "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.androidType = androidType;
		setAndroidValue(value);
		isSet = false;
	}

	/***
	 * Creates an Android Language Attribute based on a android resource
	 * qualifier.
	 * 
	 * @param qualifier
	 */
	public AndroidLocaleAttribute(String qualifier, int type) {
		super("", LocaleAttribute.STRING_TYPE, 0, 0, null, "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		androidType = type;
		Object value = getValueAndTypeFromQualifier(qualifier);
		setAndroidValue(value);

	}

	/***
	 * Gets the Android type.
	 * 
	 * @return
	 */
	public int getAndroidType() {
		return androidType;
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	private String getCountryCodeFolder(String value) {
		return "mcc" + value; //$NON-NLS-1$
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	private String getLanguageFolder(String value) {
		return value.toLowerCase();
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	private String getNetworkCodeFolder(String value) {
		return "mnc" + value; //$NON-NLS-1$
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	private String getPixelFolder(String value) {
		return value + "dpi"; //$NON-NLS-1$
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	private String getRegionCodeFolder(String value) {
		return "r" + value.toUpperCase(); //$NON-NLS-1$
	}

	/***
	 * Gets the string to be used to compose folder names.
	 * 
	 * @param value
	 * @return
	 */
	public String getStringValue() {
		return stringValue;
	}

	/***
	 * - Parse the qualifier in order to retrieve necessary information to be
	 * used for other methods of this class.
	 * 
	 * - Sets the attribute type according to the qualifier type.
	 * 
	 * @param qualifier
	 * @return
	 */
	private Object getValueAndTypeFromQualifier(String strValue) {
		Object result = null;

		if (androidType == AndroidLocaleAttributes.COUNTRY_CODE.ordinal()) {
			result = strValue.substring(3);
		} else if (androidType == AndroidLocaleAttributes.NETWORK_CODE
				.ordinal()) {
			result = strValue.substring(3);
		} else if (androidType == AndroidLocaleAttributes.LANGUAGE.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.REGION.ordinal()) {
			result = strValue.substring(1);
		} else if (androidType == AndroidLocaleAttributes.SCREEN_SIZE.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.SCREEN_ASPECT
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.SCREEN_ORIENTATION
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.DOCK_MODE.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.NIGHT_MODE.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.PIXEL_DENSITY
				.ordinal()) {
			int index = strValue.indexOf("dpi"); //$NON-NLS-1$
			result = strValue.substring(0, index);
		} else if (androidType == AndroidLocaleAttributes.TOUCH_TYPE.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.KEYBOARD_STATE
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.TEXT_INPUT_METHOD
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.NAVIGATION_KEY_STATE
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.NAVIGATION_METHOD
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.SCREEN_DIMENSION
				.ordinal()) {
			String[] numbers = strValue.split("x"); //$NON-NLS-1$
			int x = Integer.parseInt(numbers[0]);
			int y = Integer.parseInt(numbers[1]);
			result = new Dimension(x, y);
		} else if (androidType == AndroidLocaleAttributes.API_VERSION.ordinal()) {
			result = strValue.substring(0, strValue.length());
		} else if (androidType == AndroidLocaleAttributes.SMALLEST_WIDTH
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.AVAILABLE_WIDTH
				.ordinal()) {
			result = strValue;
		} else if (androidType == AndroidLocaleAttributes.AVAILABLE_HEIGHT
				.ordinal()) {
			result = strValue;
		}

		return result;
	}

	/***
	 * Checks if this attribute is set.
	 * 
	 * All attributes may exist for a given language but they will only be used
	 * for creating the path if they are set.
	 */
	public boolean isSet() {
		return isSet;
	}

	/***
	 * Sets the values of this attribute.
	 * 
	 * The object received as param will be parsed according to the android type
	 * of the attribute.
	 * 
	 * So, this method WILL fail if you pass an attribute type that is not the
	 * one expected according to the type of the attribute. For instance, if the
	 * atribute type is Dimension, so the object of the parameter MUST be a
	 * Dimension.
	 * 
	 * @param value
	 */
	public void setAndroidValue(Object value) {

		if (androidType == AndroidLocaleAttributes.COUNTRY_CODE.ordinal()) {
			setCountryCodeNode(value);
		} else if (androidType == AndroidLocaleAttributes.NETWORK_CODE
				.ordinal()) {
			setNetworkCodeNode(value);
		} else if (androidType == AndroidLocaleAttributes.LANGUAGE.ordinal()) {
			setLanguageNode(value);
		} else if (androidType == AndroidLocaleAttributes.REGION.ordinal()) {
			setRegionNode(value);
		} else if (androidType == AndroidLocaleAttributes.SCREEN_SIZE.ordinal()) {
			setScreenSizeNode(value);
		} else if (androidType == AndroidLocaleAttributes.SCREEN_ASPECT
				.ordinal()) {
			setScreenAspectNode(value);
		} else if (androidType == AndroidLocaleAttributes.SCREEN_ORIENTATION
				.ordinal()) {
			setOrientationNode(value);
		} else if (androidType == AndroidLocaleAttributes.DOCK_MODE.ordinal()) {
			setDockNode(value);
		} else if (androidType == AndroidLocaleAttributes.NIGHT_MODE.ordinal()) {
			setNightNode(value);
		} else if (androidType == AndroidLocaleAttributes.PIXEL_DENSITY
				.ordinal()) {
			setPixelNode(value);
		} else if (androidType == AndroidLocaleAttributes.TOUCH_TYPE.ordinal()) {
			setTouchNode(value);
		} else if (androidType == AndroidLocaleAttributes.KEYBOARD_STATE
				.ordinal()) {
			setKeyboardNode(value);
		} else if (androidType == AndroidLocaleAttributes.TEXT_INPUT_METHOD
				.ordinal()) {
			setTextInputNode(value);
		} else if (androidType == AndroidLocaleAttributes.NAVIGATION_KEY_STATE
				.ordinal()) {
			setNavigationKeyStateNode(value);
		} else if (androidType == AndroidLocaleAttributes.NAVIGATION_METHOD
				.ordinal()) {
			setNavigationNode(value);
		} else if (androidType == AndroidLocaleAttributes.SCREEN_DIMENSION
				.ordinal()) {
			setDimensionNode(value);
		} else if (androidType == AndroidLocaleAttributes.API_VERSION.ordinal()) {
			setAPIVersionNode(value);
		} else if (androidType == AndroidLocaleAttributes.SMALLEST_WIDTH
				.ordinal()) {
			setSmallestWidthValue(value);
		} else if (androidType == AndroidLocaleAttributes.AVAILABLE_WIDTH
				.ordinal()) {
			setAvailableWidthValue(value);
		} else if (androidType == AndroidLocaleAttributes.AVAILABLE_HEIGHT
				.ordinal()) {
			setAvailableHeightValue(value);
		} else {
			throw new IllegalArgumentException(Messages.Unknown_Andr_Type);
		}
		isSet = true;
	}

	/***
	 * Sets this attribute.
	 * 
	 * All attributes may exist for a given language but they will only be used
	 * for creating the path if they are set.
	 */
	public void setAttribute() {
		isSet = true;
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setCountryCodeNode(Object value) {
		displayName = "Country Code"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 3;
		maximumSize = 3;
		allowedValues = null;
		setIntValue(value);
		folderValue = getCountryCodeFolder(displayValue);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setScreenAspectNode(Object value) {
		displayName = "Screen Ratio"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setDockNode(Object value) {
		displayName = "Dock Mode"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setNightNode(Object value) {
		displayName = "Night Mode"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setNavigationKeyStateNode(Object value) {
		displayName = "Navigation State"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setDimensionNode(Object value) {
		if (!(value instanceof Dimension)) {
			throw new IllegalArgumentException(Messages.Invalid_Andr_Value);
		}
		displayName = "Screen Dimension"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = null;
		double y = ((Dimension) value).getWidth();
		double x = ((Dimension) value).getHeight();
		displayValue = (int) x + "x" + (int) y; //$NON-NLS-1$
		folderValue = displayValue;
	}

	private void setScreenSizeNode(Object value) {
		displayName = "Screen Size"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setStringValue(value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	public void setIntValue(Object value) {

		Integer intValue = -1;
		if (value instanceof Integer) {
			intValue = (Integer) value;
		} else if (value instanceof String) {
			intValue = Integer.parseInt((String) value);
		} else {
			throw new IllegalArgumentException(Messages.Invalid_Andr_Value);
		}

		if (fixedSize > 0) {
			if (intValue.toString().length() != fixedSize) {
				throw new IllegalArgumentException(
						Messages.Invalid_Andr_Value_Size + fixedSize);
			}
		}

		if (maximumSize > 0) {
			if (intValue.toString().length() > maximumSize) {
				throw new IllegalArgumentException(
						Messages.Invalid_Andr_Value_Size + maximumSize);
			}
		}

		displayValue = intValue.toString();
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setKeyboardNode(Object value) {
		displayName = "Keyboard State"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setLanguageNode(Object value) {
		displayName = "Language"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 2;
		maximumSize = 2;
		allowedValues = null;
		setStringValue(value);
		folderValue = getLanguageFolder(displayValue);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setNavigationNode(Object value) {
		displayName = "Navigation Method"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setNetworkCodeNode(Object value) {
		displayName = "Network Code"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 0;
		maximumSize = 3;
		allowedValues = null;
		setIntValue(value);
		folderValue = getNetworkCodeFolder(displayValue);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setAPIVersionNode(Object value) {
		displayName = "API Version"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 0;
		maximumSize = 3;
		allowedValues = null;
		setAPIValue(value);
		folderValue = displayValue;
	}

	/*
	 * 
	 * @param value
	 */
	private void setAPIValue(Object value) {
		if (value instanceof String) {
			Integer intValue = -1;
			String source = (String) value;
			String intValueAsText = source.substring(1, source.length());
			try {
				intValue = Integer.parseInt((String) intValueAsText);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(Messages.Invalid_Andr_Value);
			}
			displayValue = (String) value;
		}
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setOrientationNode(Object value) {
		displayName = "Screen Orientation"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = new HashMap<String, String>();
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setPixelNode(Object value) {
		displayName = "Pixel Density"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		allowedValues = null;
		setStringValue(value);
		folderValue = getPixelFolder(displayValue);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setRegionNode(Object value) {
		displayName = "Region"; //$NON-NLS-1$
		type = LocaleAttribute.STRING_TYPE;
		fixedSize = 2;
		maximumSize = 2;
		allowedValues = null;
		setStringValue(value);
		folderValue = getRegionCodeFolder(displayValue);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setStringValue(Object value) {
		if (!(value instanceof String)) {
			throw new IllegalArgumentException(Messages.Invalid_Andr_Value);
		}

		if (type == FIXED_TEXT_TYPE) {
			setValuesBasedOnDisplayValue((String) value);
		} else {
			if (fixedSize > 0) {
				if (((String) value).length() != fixedSize) {
					throw new IllegalArgumentException(
							Messages.Invalid_Andr_Value_Size + fixedSize);
				}
			}

			if (maximumSize > 0) {
				if (((String) value).length() > maximumSize) {
					throw new IllegalArgumentException(
							Messages.Invalid_Andr_Value_Size + maximumSize);
				}
			}

			displayValue = (String) value;
		}

	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setTextInputNode(Object value) {
		displayName = "Text Input Method"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		setValuesBasedOnDisplayValue((String) value);
	}

	/**
	 * Sets the type and values of this attribute according to the object
	 * received.
	 * 
	 * @param value
	 */
	private void setTouchNode(Object value) {
		displayName = "Touch Screen Type"; //$NON-NLS-1$
		type = LocaleAttribute.FIXED_TEXT_TYPE;
		fixedSize = 0;
		maximumSize = 0;
		setValuesBasedOnDisplayValue((String) value);
	}
	
	private void setSmallestWidthValue(Object value)
    {
        displayName = "smallestWidth"; //$NON-NLS-1$
        type = LocaleAttribute.FIXED_TEXT_TYPE;
        fixedSize = 0;
        maximumSize = 0;
        setValuesBasedOnDisplayValue((String) value);
    }

    private void setAvailableWidthValue(Object value)
    {

        displayName = "Available width"; //$NON-NLS-1$
        type = LocaleAttribute.FIXED_TEXT_TYPE;
        fixedSize = 0;
        maximumSize = 0;
        setValuesBasedOnDisplayValue((String) value);
    }

    private void setAvailableHeightValue(Object value)
    {
        displayName = "Available height"; //$NON-NLS-1$
        type = LocaleAttribute.FIXED_TEXT_TYPE;
        fixedSize = 0;
        maximumSize = 0;
        setValuesBasedOnDisplayValue((String) value);
    }

	/***
	 * Unsets this attribute.
	 * 
	 * All attributes may exist for a given language but they will only be used
	 * for creating the path if they are set.
	 */
	public void unsetAttribute() {
		isSet = false;
	}

}
