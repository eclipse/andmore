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
package org.eclipse.andmore.android.emulator.ui.controls;

import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;

/**
 * This interface defines the methods that must be implemented by the composites
 * that holds Android Emulator instances.
 */
public interface IAndroidComposite {

	/**
	 * Applies the zoom factor to the components of the composite.
	 */
	void applyZoomFactor();

	/**
	 * Gets the current zoom factor.
	 * 
	 * @return the zoom factor
	 */
	double getZoomFactor();

	/**
	 * Sets the zoom factor.
	 * 
	 * @param zoom
	 *            the zoom factor
	 */
	void setZoomFactor(double zoom);

	/**
	 * Applies the layout to the components of the composite.
	 * 
	 * @param layoutName
	 *            The name of the layout to apply
	 */
	void applyLayout(String layoutName);

	/**
	 * Gets is the selected fit to window option.
	 * 
	 * @return the zoom factor
	 */
	boolean isFitToWindowSelected();

	/**
	 * Retrieves the key listener to apply to the main display
	 * 
	 * @return The key listener
	 */
	KeyListener getKeyListener();

	/**
	 * Retrieves the mouse listener to apply to the main display
	 * 
	 * @return The mouse listener
	 */
	MouseListener getMouseListener();

	/**
	 * Retrieves the mouse move listener to apply to the main display
	 * 
	 * @return The mouse move listener
	 */
	MouseMoveListener getMouseMoveListener();

}
