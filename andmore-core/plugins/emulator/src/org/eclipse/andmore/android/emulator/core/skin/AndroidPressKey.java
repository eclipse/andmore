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
package org.eclipse.andmore.android.emulator.core.skin;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import org.eclipse.swt.graphics.Rectangle;

/**
 * DESCRIPTION: This is a default implementation of the interface
 * IAndroidEmulatorKey
 *
 * RESPONSIBILITY: - Provide an easy way to find the keys pressed during mouse
 * interaction at skin - Provide means of retrieving the keysym associated with
 * each key
 *
 * COLABORATORS: None.
 *
 * USAGE: Provide a coordinate to the isInsideKey methods to test if the
 * coordinate is inside the key area Use the getKeysym of a key to retrieve the
 * code that needs to be sent in a key event message to the server, informing
 * that a key was pressed or released
 */
public class AndroidPressKey implements IAndroidKey {
	// Constants used in the isFlipValid method
	private static final int FLIP_SLIDE_OPENED_ONLY = 0;

	private static final int FLIP_SLIDE_CLOSED_ONLY = 1;

	private static final int FLIP_SLIDE_OPENED_AND_CLOSED = 2;

	/**
	 * Fields that can be found in an ordinary key.xml file
	 */
	private final String name;

	private final String toolTip;

	private final String keycode;

	private final Rectangle keyArea;

	private final int flipSlideEnabledCode;

	private final Collection<String> morphingModeCollection = new LinkedHashSet<String>();

	/**
	 * Creates a new AndroidPressKey object.
	 *
	 * @param name
	 *            The key name. This is usually a human readable skin, that
	 *            provides key identification
	 * @param keycode
	 *            The code that will be sent to server if the key is pressed
	 * @param toolTip
	 *            The text that will be shown as the key tool tip.
	 * @param startx
	 *            X coordinate of the upper left corner of the key
	 * @param starty
	 *            Y coordinate of the upper left corner of the key
	 * @param endx
	 *            X coordinate of the lower right corner of the key
	 * @param endy
	 *            Y coordinate of the lower right corner of the key
	 * @param morphingModes
	 *            A comma separated list of morphing modes to which this key
	 *            applies, or null if not applicable
	 * @param flipenabled
	 *            true if this key is valid in closed flip mode; false if the
	 *            key is valid in opened flip mode
	 */
	public AndroidPressKey(String name, String keycode, String toolTip, int startx, int starty, int endx, int endy,
			String morphingModes, int flipEnabledCode) {
		this(name, keycode, toolTip, new Rectangle(startx, starty, endx - startx, endy - starty), morphingModes,
				flipEnabledCode);
	}

	/**
	 * Creates a new AndroidPressKey object.
	 *
	 * @param name
	 *            The key name. This is usually a human readable skin, that
	 *            provides key identification
	 * @param keycode
	 *            The code that will be sent to server if the key is pressed
	 * @param toolTip
	 *            The text that will be shown as the key tool tip.
	 * @param key
	 *            A rectangle that represents the key area at skin
	 * @param morphingModes
	 *            A comma separated list of morphing modes to which this key
	 *            applies, or null if not applicable
	 * @param flipenabled
	 *            true if this key is valid in closed flip mode; false if the
	 *            key is valid in opened flip mode
	 */
	public AndroidPressKey(String name, String keycode, String toolTip, Rectangle key, String morphingModes,
			int flipEnabledCode) {
		this.name = name;
		this.keycode = keycode;
		this.toolTip = toolTip;
		this.keyArea = key;
		this.flipSlideEnabledCode = flipEnabledCode;

		if (morphingModes != null) {
			StringTokenizer st = new StringTokenizer(morphingModes, ",");
			String token;

			while (st.hasMoreTokens()) {
				token = st.nextToken();
				morphingModeCollection.add(token);
			}
		}
	}

	/**
	 * @see IAndroidKey#getKeysym()
	 */
	@Override
	public String getKeysym() {
		return keycode;
	}

	/**
	 * @see IAndroidKey#isInsideKey(int, int)
	 */
	@Override
	public boolean isInsideKey(int x, int y) {
		return keyArea.contains(x, y);
	}

	/**
	 * Retrieves the X coordinate of the lower right corner of the key
	 *
	 * @return X coordinate of the lower right corner of the key
	 */
	public int getEndx() {
		return keyArea.x + keyArea.width;
	}

	/**
	 * Retrieves the Y coordinate of the lower right corner of the key
	 *
	 * @return Y coordinate of the lower right corner of the key
	 */
	public int getEndy() {
		return keyArea.y + keyArea.height;
	}

	/**
	 * Tests if the key is valid in the current flip/slide mode
	 *
	 * @param isFlipSlideClosed
	 *            True if the flip/slide is currently closed False if the
	 *            flip/slide is currently opened
	 *
	 * @return true if the key is valid in the current flip/slide mode; false
	 *         otherwise
	 */
	public boolean isFlipSlideValid(boolean isFlipSlideClosed) {
		boolean flipSlideValid = false;

		if ((flipSlideEnabledCode == FLIP_SLIDE_OPENED_AND_CLOSED)
				|| (isFlipSlideClosed && (flipSlideEnabledCode == FLIP_SLIDE_CLOSED_ONLY))
				|| (!isFlipSlideClosed && (flipSlideEnabledCode == FLIP_SLIDE_OPENED_ONLY))) {
			flipSlideValid = true;
		}

		return flipSlideValid;
	}

	/**
	 * Retrieves the key name
	 *
	 * @return The key name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the tool tip text of the key.
	 * 
	 * @return The tool tip text of the key.
	 */
	@Override
	public String getToolTip() {
		return toolTip;
	}

	/**
	 * Retrieves the X coordinate of the upper left corner of the key
	 *
	 * @return X coordinate of the upper left corner of the key
	 */
	public int getStartx() {
		return keyArea.x;
	}

	/**
	 * Retrieves the Y coordinate of the upper left corner of the key
	 *
	 * @return Y coordinate of the upper left corner of the key
	 */
	public int getStarty() {
		return keyArea.y;
	}

	/**
	 * Retrieves a rectangle that represents the key area at skin
	 *
	 * @return A rectangle that represents the key area at skin
	 */
	@Override
	public Rectangle getKeyArea() {
		return keyArea;
	}

	/**
	 * Tests if the key applies to the provided morphing mode
	 *
	 * @param morphingMode
	 *            The morphing mode name
	 * @return true if the key applies to the morphing mode; false otherwise
	 */
	public boolean hasMorphingMode(String morphingMode) {
		if (morphingMode != null) {
			return morphingModeCollection.contains(morphingMode);
		} else {
			return false;
		}
	}
}
