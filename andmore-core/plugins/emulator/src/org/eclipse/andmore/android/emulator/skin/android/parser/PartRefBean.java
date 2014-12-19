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
package org.eclipse.andmore.android.emulator.skin.android.parser;

/**
 * DESCRIPTION: This class represents a part reference. It links a layout with a
 * part from the same layout file
 *
 * RESPONSIBILITY: Represent part references and provide link information
 * between parts and layouts
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser and
 * LayoutFileModel classes only
 */
public class PartRefBean implements ILayoutConstants, ILayoutBean {
	/**
	 * The name of this part reference
	 */
	private String refName;

	/**
	 * The name of the part that this reference points to
	 */
	private String partName;

	/**
	 * The X position of the part into the layout
	 */
	private String x;

	/**
	 * The Y position of the part into the layout
	 */
	private String y;

	/**
	 * The rotation applied to the part, when into a layout
	 */
	private String rotation;

	/**
	 * Constructor Creates a part reference with the given name
	 * 
	 * @param refName
	 *            The part reference name
	 */
	PartRefBean(String refName) {
		this.refName = refName;
	}

	/**
	 * Retrieves the name of the part being referenced
	 * 
	 * @return The part name
	 */
	String getPartName() {
		return partName;
	}

	/**
	 * Retrieves the reference name
	 * 
	 * @return The reference name
	 */
	String getRefName() {
		return refName;
	}

	/**
	 * Retrieves the X position where to draw this part at the layout
	 * 
	 * @return The X position
	 */
	String getX() {
		return x;
	}

	/**
	 * Retrieves the Y position where to draw this part at the layout
	 * 
	 * @return The Y position
	 */
	String getY() {
		return y;
	}

	/**
	 * Retrieves how many rotations to perform on the part when drawing it into
	 * the layout
	 * 
	 * @return How many rotations to perform on the part
	 */
	String getRotation() {
		return rotation;
	}

	@Override
	public String toString() {
		return "PartRef: " + refName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.skin.android.parser.ILayoutBean#
	 * setKeyValue(java.lang.String, java.lang.String)
	 */
	@Override
	public void setKeyValue(String key, String value) {
		if (ATTR_X.equals(key)) {
			x = value;
		} else if (ATTR_Y.equals(key)) {
			y = value;
		} else if (ATTR_NAME.equals(key)) {
			partName = value;
		} else if (PARTREF_ROTATION.equals(key)) {
			rotation = value;
		}
	}
}
