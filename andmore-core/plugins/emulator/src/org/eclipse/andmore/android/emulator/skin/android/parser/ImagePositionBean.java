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

import java.io.File;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.PlatformUI;

/**
 * DESCRIPTION: This class represents a node containing image, x and y data
 *
 * RESPONSIBILITY: Represent image nodes of the layout file
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser and
 * LayoutFileModel classes only
 */
public class ImagePositionBean implements ILayoutConstants, ILayoutBean {
	/**
	 * Name of the node
	 */
	private String name;

	/**
	 * Location of the file that contains the image
	 */
	private File imageLocation;

	/**
	 * X position where to draw the image
	 */
	private String xPos;

	/**
	 * Y position where to draw the image
	 */
	private String yPos;

	private int width = -1;

	private int height = -1;

	/**
	 * Constructor Creates the node and assign a name to it
	 */
	ImagePositionBean(String name) {
		this.name = name;
	}

	/**
	 * Retrieves the image location
	 * 
	 * @return The image location
	 */
	File getImageLocation() {
		return imageLocation;
	}

	/**
	 * Retrieves the X position where to draw the image
	 * 
	 * @return The image X position
	 */
	String getXPos() {
		return xPos;
	}

	/**
	 * Retrieves the Y position where to draw the image
	 * 
	 * @return The image Y position
	 */
	String getYPos() {
		return yPos;
	}

	int getWidth(File skinFilesPath) {
		if (width == -1) {
			populateWidthHeight(skinFilesPath);
		}

		return width;
	}

	int getHeight(File skinFilesPath) {
		if (width == -1) {
			populateWidthHeight(skinFilesPath);
		}

		return height;
	}

	/**
	 * Retrieves the name of this node
	 * 
	 * @return The node name
	 */
	String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ImagePosition: " + name;
	}

	private void populateWidthHeight(final File skinFilesPath) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ImageData id = new ImageData(new File(skinFilesPath, imageLocation.getName()).getAbsolutePath());
				width = id.width;
				height = id.height;
			}
		});
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
		if (ATTR_IMAGE.equals(key)) {
			imageLocation = new File(value);
		} else if (ATTR_X.equals(key)) {
			xPos = value;
		} else if (ATTR_Y.equals(key)) {
			yPos = value;
		}
	}
}
