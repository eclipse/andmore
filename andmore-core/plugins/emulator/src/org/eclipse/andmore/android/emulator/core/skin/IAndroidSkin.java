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
import java.util.Properties;

import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.sequoyah.vnc.vncviewer.config.EclipsePropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

/**
 * This interface must be implemented by anyone who wishes to contribute to the
 * skin extension point
 */
public interface IAndroidSkin {
	IPropertiesFileHandler DEFAULT_PROPS_HANDLER = new EclipsePropertiesFileHandler();

	String DEFAULT_VNC_CONFIG_FILE = "resources/vnc_viewer.conf";

	/**
	 * Retrieves an image data object containing one pressed image pixels and
	 * attributes
	 *
	 * @return An image data object containing pixels and image attributes
	 *
	 * @throws SkinException
	 *             If any problem occurs while retrieving the image data. If a
	 *             new exception is being created, it is expected that it
	 *             provides a message to display to the user
	 */
	ImageData getPressedImageData(String layoutName) throws SkinException;

	/**
	 * Retrieves an image data object containing one released image pixels and
	 * attributes
	 *
	 * @return An image data object containing pixels and image attributes
	 *
	 * @throws SkinException
	 *             If any problem occurs while retrieving the image data If a
	 *             new exception is being created, it is expected that it
	 *             provides a message to display to the user
	 */
	ImageData getReleasedImageData(String layoutName) throws SkinException;

	/**
	 * Retrieves an image data object containing one enter image pixels and
	 * attributes
	 *
	 * @return An image data object containing pixels and image attributes
	 *
	 * @throws SkinException
	 *             If any problem occurs while retrieving the image data If a
	 *             new exception is being created, it is expected that it
	 *             provides a message to display to the user
	 */
	ImageData getEnterImageData(String layoutName) throws SkinException;

	/**
	 * Retrieves a collection containing all keys that are supported by the
	 * handset represented by this skin
	 *
	 * @return The key collection read from skin
	 */
	Collection<IAndroidKey> getKeyDataCollection(String layoutName);

	/**
	 * Retrieves a bean containing all skin data that does not refer to the keys
	 *
	 * @return The skin bean
	 *
	 * @throws SkinException
	 *             If any problem occurs while retrieving the skin data If a new
	 *             exception is being created, it is expected that it provides a
	 *             message to display to the user
	 */
	AndroidSkinBean getSkinBean(String layoutName) throws SkinException;

	/**
	 * Tests if flip is supported by the phone represented by this skin
	 *
	 * @return true if flip is supported; false otherwise
	 */
	boolean isFlipSupported();

	/**
	 * Set where the skin files are located based on the emulator root dir
	 * 
	 * @param emulatorInstallDir
	 *            Root of emulator installation
	 * 
	 * @throws SkinException
	 *             If the path provided does not contain a valid skin
	 */
	void setSkinFilesPath(String emulatorInstallDir) throws SkinException;

	/**
	 * Retrieves the names of all available layouts of the skin
	 * 
	 * @return A collection containing the names of all available layouts
	 */
	public Collection<String> getAvailableLayouts();

	/**
	 * Checks if the current layout is rotated (i.e. demands screen rotation)
	 */
	boolean isSwapWidthHeightNeededAtLayout(String layoutName);

	/**
	 * Retrieves the command to send to the emulator to switch screen
	 * 
	 * @return The command to send to the emulator to switch screen
	 */
	String getLayoutScreenCommand(String layoutName);

	/**
	 * Finds which layout comes next to referenceLayout
	 * 
	 * @param referenceLayout
	 *            The layout to be used as reference on next layout calculation
	 * 
	 * @return The next layout name
	 */
	public String getNextLayout(String referenceLayout);

	/**
	 * Finds which layout is previous to referenceLayout
	 * 
	 * @param referenceLayout
	 *            The layout to be used as reference on previous layout
	 *            calculation
	 * 
	 * @return The previous layout name
	 */
	public String getPreviousLayout(String referenceLayout);

	/**
	 * Retrieves what is the background color to be applied at the provided
	 * layout
	 * 
	 * @param layoutName
	 *            The layout name in which to apply the background color
	 * 
	 * @return A RGB object describing the color
	 */
	public RGB getBackgroundColor(String layoutName);

	/**
	 * @return
	 */
	Properties getKeyCodes();

	/**
	 * Return the dpad-rotation if present on a given layout or 0 otherwise.
	 * 
	 * @param layoutName
	 * @return
	 */
	int getDpadRotation(String layoutName);
}
