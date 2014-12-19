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
 * DESCRIPTION: This class represents a node containing x, y, width and height
 * data
 *
 * RESPONSIBILITY: Represent rectangles of the layout file
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser and
 * LayoutFileModel classes only
 */
public class RectangleBean implements ILayoutConstants, ILayoutBean {
	/**
	 * The X position of the rectangle origin
	 */
	private String xPos;

	/**
	 * The Y position of the rectangle origin
	 */
	private String yPos;

	/**
	 * The width of the rectangle
	 */
	private String width;

	/**
	 * The height of the rectangle
	 */
	private String height;

	/**
	 * Retrieves the X position of the rectangle origin
	 * 
	 * @return the X position
	 */
	String getXPos() {
		return xPos;
	}

	/**
	 * Retrieves the Y position of the rectangle origin
	 * 
	 * @return the Y position
	 */
	String getYPos() {
		return yPos;
	}

	/**
	 * Retrieves the width of the rectangle
	 * 
	 * @return The rectangle width
	 */
	String getWidth() {
		return width;
	}

	/**
	 * Retrieves the height of the rectangle
	 * 
	 * @return The rectangle height
	 */
	String getHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Rectangle";
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
			xPos = value;
		} else if (ATTR_Y.equals(key)) {
			yPos = value;
		} else if (ATTR_WIDTH.equals(key)) {
			width = value;
		} else if (ATTR_HEIGHT.equals(key)) {
			height = value;
		}

	}
}
