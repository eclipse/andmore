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

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.swt.graphics.RGB;

/**
 * DESCRIPTION: This class represents a layout structure from the layout file
 *
 * RESPONSIBILITY: Represent layout structures
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser and
 * LayoutFileModel classes only
 */
public class LayoutBean implements ILayoutConstants, ILayoutBean {
	/**
	 * Default name of a landscape layout (used when creating pseudo-layouts)
	 * Pseudo-layouts are created when the skin has only one part and no layouts
	 */
	public static final String DEFAULT_LAYOUT_NAME = "default";

	/**
	 * Default name of a portrait layout (used when creating pseudo-layouts)
	 * Pseudo-layouts are created when the skin has only one part and no layouts
	 */
	public static final String ROTATED_LAYOUT_NAME = "rotated";

	/**
	 * The layout structure name
	 */
	private String name;

	/**
	 * The layout width
	 */
	private String width;

	/**
	 * The layout height
	 */
	private String height;

	/**
	 * Thw layout color
	 */
	private RGB color;

	/**
	 * The event command used to switch to this layout
	 */
	private String event;

	/**
	 * Dpad rotation, steps of 90�
	 */
	private int dpadRotation;

	/**
	 * The collection of parts that integrate this layout
	 */
	private Collection<PartRefBean> parts = new LinkedHashSet<PartRefBean>();

	/**
	 * Constructor Builds a new layout structure with the given name
	 * 
	 * @param name
	 *            The layout name
	 */
	LayoutBean(String name) {
		this.name = name;
	}

	/**
	 * Creates a new reference to a part, registers it and returns it to the
	 * user
	 * 
	 * @param refName
	 *            The name to assign to the part reference
	 * 
	 * @return The part reference
	 */
	PartRefBean newPartRef(String refName) {
		PartRefBean bean = new PartRefBean(refName);
		parts.add(bean);
		return bean;
	}

	/**
	 * Retrieves the layout width
	 * 
	 * @return The layout width
	 */
	String getWidth() {
		return width;
	}

	/**
	 * Retrieves the layout height
	 * 
	 * @return The layout height
	 */
	String getHeight() {
		return height;
	}

	/**
	 * Retrieves the layout color
	 * 
	 * @return The layout color
	 */
	RGB getColor() {
		return color;
	}

	/**
	 * Retrieves the event to switch to this layout
	 * 
	 * @param event
	 *            The event
	 */
	String getEvent() {
		return event;
	}

	/**
	 * @return the dpadRotation
	 */
	int getDpadRotation() {
		return dpadRotation;
	}

	/**
	 * Retrieves this layout name
	 * 
	 * @return This layout name
	 */
	String getName() {
		return name;
	}

	/**
	 * Retrieves all references to parts from this layout
	 * 
	 * @return A collection containing part references
	 */
	Collection<PartRefBean> getPartRefs() {
		return parts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Layout: " + name;
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
		if (ATTR_WIDTH.equals(key)) {
			width = value;
		} else if (ATTR_HEIGHT.equals(key)) {
			height = value;
		} else if (LAYOUT_COLOR.equals(key)) {
			Integer colorInt = Integer.decode(value);
			int blue = colorInt.intValue() & 0xFF;
			int green = (colorInt.intValue() & 0xFF00) >> 8;
			int red = (colorInt.intValue() & 0xFF0000) >> 16;
			RGB rgb = new RGB(red, green, blue);

			color = rgb;
		} else if (LAYOUT_EVENT.equals(key)) {
			event = value;
		} else if (DPAD_ROTATION.equals(key)) {
			int intValue;
			try {
				intValue = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// Assume there's no rotation
				intValue = 0;
			}
			dpadRotation = intValue;
		}
	}
}
