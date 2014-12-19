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

/**
 * DESCRIPTION: This class represents a part structure from the layout file
 *
 * RESPONSIBILITY: Represent part structures
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser and
 * LayoutFileModel classes only
 */
public class PartBean {
	/**
	 * The string used as name of a part, at skins that have a single part
	 */
	public static final String UNIQUE_PART = "___UNIQUE___";

	/**
	 * The part name
	 */
	private String name;

	/**
	 * The part background data
	 */
	private ImagePositionBean background;

	/**
	 * The part display data
	 */
	private RectangleBean display;

	/**
	 * The part buttons
	 */
	private Collection<ImagePositionBean> buttons = new LinkedHashSet<ImagePositionBean>();

	/**
	 * Constructor Builds a new part structure with the given name
	 * 
	 * @param name
	 *            The layout name
	 */
	PartBean(String name) {
		this.name = name;
	}

	/**
	 * Creates a new button, registers it and returns it to the user
	 * 
	 * @param buttonName
	 *            The name to assign to the button
	 * 
	 * @return The button
	 */
	ImagePositionBean newButton(String buttonName) {
		ImagePositionBean bean = new ImagePositionBean(buttonName);
		buttons.add(bean);
		return bean;
	}

	/**
	 * Creates a new display and registers it
	 * 
	 * @return The display
	 */
	RectangleBean newDisplay() {
		display = new RectangleBean();
		return display;
	}

	/**
	 * Creates a new background and registers it
	 * 
	 * @param bgName
	 *            The name of the background image
	 * 
	 * @return The background
	 */
	ImagePositionBean newBackground(String bgName) {
		background = new ImagePositionBean(bgName);
		return background;
	}

	/**
	 * Retrieves the part background information
	 * 
	 * @return The part background information
	 */
	ImagePositionBean getBackground() {
		return background;
	}

	/**
	 * Retrieves the part display information
	 * 
	 * @return The part display information
	 */
	RectangleBean getDisplay() {
		return display;
	}

	/**
	 * Retrieves the part name
	 * 
	 * @return The part name
	 */
	String getName() {
		return name;
	}

	Collection<ImagePositionBean> getButtons() {
		return buttons;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Part: " + name;
	}
}
