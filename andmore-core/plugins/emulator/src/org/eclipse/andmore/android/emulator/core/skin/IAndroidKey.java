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

import org.eclipse.swt.graphics.Rectangle;

/**
 * This interface should be used by anyone who wishes to implement a class which
 * contain key information for Motorola handsets
 */
public interface IAndroidKey {
	/**
	 * This method returns the keysym code associated to this key
	 *
	 * @return The keysym code
	 */
	String getKeysym();

	/**
	 * This method tests if a certain (x, y) coordinate is inside this key
	 *
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 *
	 * @return true if the provided coordinate is internal to the key; false
	 *         otherwise
	 */
	boolean isInsideKey(int x, int y);

	/**
	 * Retrieves a rectangle that corresponds to the drawing area of the key at
	 * the skin
	 *
	 * @return The key area
	 */
	Rectangle getKeyArea();

	/**
	 * Retrieves the text tool tip of the key.
	 * 
	 * @return The text tool tip of the key.
	 */
	String getToolTip();
}
