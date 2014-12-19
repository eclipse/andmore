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
package org.eclipse.andmore.android.emulator.skin.android;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.skin.AndroidPressKey;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidKey;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.skin.android.parser.LayoutFileModel;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

/**
 * DESCRIPTION: Utility class for translating Google skin files data into
 * objects suitable to the viewer. This includes: - Generating image data
 * objects in memory that represent the exact images that will be drawn using
 * SWT Image/GC/Transform graphic objects. It is necessary to create new image
 * data objects from scratch because the operations performed by
 * Image/GC/Transform affects only the display, keeping the associated image
 * data objects intact. It would also be very expensive to generate those images
 * at screen, after every mouse event (including mouse moves, which are
 * frequent) - Generating a key data collection suitable for a given layout. The
 * key data objects must contain positioning information that depends on the
 * position of elements inside the layout. It is this class job to discover what
 * are the coordinates to set at the keys - Translate the display position for a
 * given layout. As with the key data collection, the coordinates of the display
 * depends on the layout description and must be calculated.
 *
 * RESPONSIBILITY: Provide translation functionalities to convert Google format
 * to a format suitable to the viewer
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by AndroidSkin class only
 * 
 * 
 * 
 * This class uses several concepts, that are described below:
 * 
 * LAYOUT FILE MODEL: A representation of the file "layout", saved as part of
 * the skin in the skin folder. Attributes such as keyboard charmap and network
 * are global to a layout file LAYOUT: A collection of parts, with some part
 * integration features, such as part positioning and part rotation. Parts can
 * be rotated independently in a layout. PART: The minimum skin view. It
 * contains buttons and background image. A display is optional OFFSET: If a
 * part is declared to be drawn in a layout using negative coordinates, or if a
 * button is declared to be drawn in a part using negative coordinates, the
 * calculated layout/part offset is different from 0 in either (x, y)
 * directions. If such situation happen, it is mandatory to generate a bigger
 * image in memory to have the layout/part drawn. EXPANDED PART IMAGE: Is how is
 * called the image generated in memory due to part offset being different from
 * zero. Layouts always have images generated in memory, but parts can be
 * represented as a simple file load if no button demands an offset different
 * from (0, 0)
 * 
 * Considerations about the differences between Google format and viewer format:
 * 
 * a) Google format supports negative coordinates for parts/buttons. To generate
 * images that are renderable by SWT, the viewer module must translate the
 * parts/buttons so that they fit in a single image data b) Viewer demands a
 * pair of pressed, released and enter images and a set of IAndroidKeys. Google
 * provides several images to use as filter when a button is pressed. To
 * increase the performance, all the images/keys are generated during instance
 * start time, being reused afterwards c) In Google format, the part position at
 * the layout is ALWAYS set to the upper-left corner of the PART, not the
 * layout. Therefore, depending on the rotation parameter we need to calculate
 * what is the position to draw the part relative to the layout
 * 
 * --------------------------- | | 1) OLX / OLY : Offset due to layout, if one
 * or more part | ------------------ | position coordinates are less than 0 | |
 * ------------ | | 2) OPX / OPY : Offset due to part, if one of more button | |
 * | | | | position coordinates are less than 0 | |OPX| | | | 3) LAYOUT : Layout
 * image area | | ----- | | | 4) PART : Part expanded image area | | |BTN| | | |
 * 5) BACKGR : Original part background image, which demanded | | | | | | |
 * expansion due to BTN | | ----- | | | 6) BTN : A button with negative x
 * coordinate related to | | | BACKGR | | | BACKGR | | ------------ | | | | PART
 * | | | OLX ------------------ | | LAYOUT | ---------------------------
 *
 */
public class AndroidSkinTranslator {
	/**
	 * Constant that describes a QWERTY charmap in the layout
	 */
	private static final String CHARMAP_QWERTY = "qwerty";

	/**
	 * Constant that describes a QWERTY2 charmap in the layout
	 */
	private static final String CHARMAP_QWERTY2 = "qwerty2";

	/**
	 * Path to the QWERTY codes inside the plugin
	 */
	private static final String CHARMAP_QWERTY_FILE = "res/qwerty.properties";

	/**
	 * Constant that describes a AVRCP charmap in the layout
	 */
	private static final String CHARMAP_AVRCP = "AVRCP";

	/**
	 * Path to the AVRCP codes inside the plugin
	 */
	private static final String CHARMAP_AVRCP_FILE = "res/AVRCP.properties";

	private static final String CHARMAP_OPHONE_QWERTY_FILE = "res/ophone_qwerty.properties";

	private static final String CHARMAP_OPHONE_AVRCP_FILE = "res/ophone_AVRCP.properties";

	/**
	 * d-pad keys
	 */
	private static final String DPAD_DOWN = "DPAD_DOWN";

	private static final String DPAD_UP = "DPAD_UP";

	private static final String DPAD_LEFT = "DPAD_LEFT";

	private static final String DPAD_RIGHT = "DPAD_RIGHT";

	/**
	 * Translates the button information inside the layout file model into
	 * viewer compatible IAndroidKeys <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represent the layout file
	 * @param layoutName
	 *            The name of the layout where to look for buttons in the model,
	 *            or <code>null</code> if no layout is available
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return A collection of IAndroidKeys, describing the keys, codes and
	 *         positions of the buttons in the skin <br>
	 * <br>
	 * @throws SkinException
	 *             If an error that prevents the translation happens, such as
	 *             errors when opening required files or charmap not supported
	 */
	static Collection<IAndroidKey> generateAndroidKeys(LayoutFileModel layoutFile, String layoutName, File skinFilesPath)
			throws SkinException {

		// Retrieve a map of keycodes related to the keyboard declared at the
		// layout
		Collection<IAndroidKey> keyCollection = new LinkedHashSet<IAndroidKey>();

		Properties keycodes = getKeycodes(layoutFile, skinFilesPath);

		// Retrieve the names of the parts that compose the layout
		Collection<String> partNames = layoutFile.getLayoutPartNames(layoutName);

		// Gets the layout offset for position adjustments
		Point offsetL = getLayoutOffset(layoutFile, layoutName, skinFilesPath);

		// Iterates on the parts, looking for their buttons

		for (String partName : partNames) {

			Point offsetP = getPartOffset(layoutFile, partName);
			Point partSize = getPartImageSize(layoutFile, partName, skinFilesPath, offsetP);

			// Iterates on the buttons of the part, creating an IAndroidKey for
			// each in the end
			Collection<String> buttonNames = layoutFile.getButtonNames(partName);

			for (String buttonName : buttonNames) {
				String k = buttonName.toUpperCase().replace("-", "_");
				String keyCodeStr = (String) keycodes.get(k);

				// The IAndroidKey will only be generated if a keycode with the
				// same name is found
				if (keyCodeStr != null) {

					// Retrieve the button parameters needed for the key
					// generation
					// - The button position must be translated due to the
					// layout and eventual offsets
					// - The button w/h must be interpreted according to the
					// rotation parameter. If the
					// rotation is odd (landscape), the button width must be the
					// key height and vice-versa.
					// Otherwise (even, portrait), we can use the button w/h at
					// the keys as is.
					int buttonW = layoutFile.getButtonWidth(partName, buttonName, skinFilesPath);
					int buttonH = layoutFile.getButtonHeight(partName, buttonName, skinFilesPath);
					Point buttonPos = translateButtonPosition(layoutFile, layoutName, partName, buttonName,
							skinFilesPath, offsetP, partSize);

					int startX = offsetL.x + buttonPos.x;
					int startY = offsetL.y + buttonPos.y;
					int endX, endY;

					if (layoutFile.isSwapWidthHeightNeededAtLayout(layoutName, partName)) {
						endX = startX + buttonH;
						endY = startY + buttonW;
					} else {
						endX = startX + buttonW;
						endY = startY + buttonH;
					}

					int dpadRotation = layoutFile.getDpadRotation(layoutName);

					keyCodeStr = getRotatedKeyCode(k, keyCodeStr, dpadRotation, keycodes);

					AndroidPressKey key = new AndroidPressKey(buttonName, keyCodeStr, buttonName, startX, startY, endX,
							endY, "", 0);

					keyCollection.add(key);

				}
			}

		}

		return keyCollection;
	}

	/**
	 * @param keyCodeStr
	 * @param keyCodeStr2
	 * @param dpadRotation
	 * @param keycodes
	 * @return
	 */
	private static String getRotatedKeyCode(String keyName, String keyCodeStr, int dpadRotation, Properties keycodes) {
		String keyCode = keyCodeStr;
		switch (dpadRotation % 4) {
		case 1:
			if (DPAD_DOWN.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_RIGHT);
			} else if (DPAD_LEFT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_DOWN);
			} else if (DPAD_RIGHT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_UP);
			} else if (DPAD_UP.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_LEFT);
			}
			break;
		case 2:
			if (DPAD_DOWN.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_UP);
			} else if (DPAD_LEFT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_RIGHT);
			} else if (DPAD_RIGHT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_LEFT);
			} else if (DPAD_UP.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_DOWN);
			}
			break;
		case 3:
			if (DPAD_DOWN.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_LEFT);
			} else if (DPAD_LEFT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_UP);
			} else if (DPAD_RIGHT.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_DOWN);
			} else if (DPAD_UP.equals(keyName)) {
				keyCode = (String) keycodes.get(DPAD_RIGHT);
			}
			break;
		default:
			// Does nothing, no rotation needed.
			break;
		}
		return keyCode;
	}

	/**
	 * Merge two ImageData objects and return the merge. <br>
	 * <br>
	 * 
	 * @param srcData
	 *            The source ImageData
	 * @param dstData
	 *            The destination ImageData
	 * @param dstX
	 *            The x position in dstData where srcData will be merged
	 * @param dstY
	 *            The y position in dstData where srcData will be merged <br>
	 * <br>
	 * @return An array containing released and pressed image data at the
	 *         positions 0 and 1, respectively
	 */
	static ImageData mergeImageGC(ImageData srcData, ImageData dstData, int dstX, int dstY) {

		Shell s = new Shell();

		Image srcImg = new Image(s.getDisplay(), srcData);
		Image dstImg = new Image(s.getDisplay(), dstData);

		GC gc = new GC(dstImg);
		gc.drawImage(srcImg, dstX, dstY);
		gc.dispose();

		return dstImg.getImageData();

	}

	/**
	 * Creates the layout images for released and pressed buttons <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The name of the layout where to look for images in the model
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return An array containing released and pressed image data at the
	 *         positions 0 and 1, respectively
	 */
	static ImageData[] generateLayoutImages(LayoutFileModel layoutFile, String layoutName, File skinFilesPath) {

		ImageData[] layoutImgs = new ImageData[3];

		// Gets the layout offset for position adjustments
		Point offsetL = getLayoutOffset(layoutFile, layoutName, skinFilesPath);

		// Iterates on the names of the parts that compose the layout
		Collection<String> partNames = layoutFile.getLayoutPartNames(layoutName);

		for (String partName : partNames) {

			if (!layoutFile.partHasBg(partName)) {
				continue;
			}

			if (partName.equals("portrait") || partName.equals("landscape")) {
				if (!partName.equals(layoutName)) {
					continue;
				}
			}

			// Gets the part offset for position adjustments and images loading
			// decision
			Point offsetP = getPartOffset(layoutFile, partName);
			Point partSize = getPartImageSize(layoutFile, partName, skinFilesPath, offsetP);
			int bgH = layoutFile.getBackgroundHeight(partName, skinFilesPath);
			int bgW = layoutFile.getBackgroundWidth(partName, skinFilesPath);

			// Loads the part images for released and pressed. If there is a
			// part offset, it is needed
			// to generated an expanded part image data
			ImageData[] bgDatas = new ImageData[3];
			if ((offsetP.x > 0) || (offsetP.y > 0) || (partSize.x > offsetP.x + bgW) || (partSize.y > offsetP.y + bgH)) {
				bgDatas[0] = generateExpandedPartImageData(layoutFile, layoutName, partName, offsetP, skinFilesPath,
						offsetP, partSize);
			} else {
				bgDatas[0] = getImageData(layoutFile, partName, null, skinFilesPath);
			}

			bgDatas[1] = generateMergedWithButtonsImage(layoutFile, (ImageData) bgDatas[0].clone(), partName,
					skinFilesPath, offsetP, false);

			bgDatas[2] = generateMergedWithButtonsImage(layoutFile, (ImageData) bgDatas[0].clone(), partName,
					skinFilesPath, offsetP, true);

			// Loop for generating layout images based on the part images above
			for (int img = 0; img < 3; img++) {
				// A layout image is created only if it wasn't yet, no matter
				// how many parts the layout has.
				if (layoutImgs[img] == null) {
					Point layoutSize = getLayoutImageSize(layoutFile, layoutName, skinFilesPath, offsetL);
					layoutImgs[img] = generateImageDataWithBackground(layoutFile, layoutName, layoutSize.x,
							layoutSize.y, bgDatas[img], skinFilesPath);

				}

				// Copy the part image pixels into the layout image
				// - Rotation units rotates the image in CLOCKWISE direction
				// - The part position must be translated because:
				// a) At Google layout file, the (x,y) position represents the
				// upper left corner of the
				// part image, no matter what is the rotation value
				// b) We are generating a layout image referenced at the upper
				// left corner of the layout
				// image, and we must know where we must place the part image in
				// terms of the layout reference
				// - The part is rotated before merged to the layout image
				int rotation = layoutFile.getPartRotationAtLayout(layoutName, partName);
				Point partPos = translatePartPosition(layoutFile, layoutName, partName, skinFilesPath, offsetP,
						partSize);
				bgDatas[img] = generateRotatedImage(bgDatas[img], rotation);

				int startX = offsetL.x - offsetP.x + partPos.x;
				int startY = offsetL.y - offsetP.y + partPos.y;
				ImageData merge = mergeImageGC(bgDatas[img], layoutImgs[img], startX, startY);
				layoutImgs[img] = merge;

			}
		}

		return layoutImgs;
	}

	/**
	 * Translates the display information from the layout file into an Point
	 * referenced at the upper left corner of the layout image (or part image,
	 * if layouts are not supported by the skin) <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The name of the layout being used
	 * @param partName
	 *            The name of the part which contains the display
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return A point referenced at the upper left corner of the layout image,
	 *         where to draw the display
	 */
	static Point translateDisplayPosition(LayoutFileModel layoutFile, String layoutName, String partName,
			File skinFilesPath) {
		// Gets the parameters necessary for calculation
		Point displayPos = layoutFile.getDisplayPosition(partName);
		int displayW = layoutFile.getDisplayWidth(partName);
		int displayH = layoutFile.getDisplayHeight(partName);
		Point offsetP = getPartOffset(layoutFile, partName);
		Point partSize;
		if (!layoutFile.partHasBg(partName)) {
			partSize = getPartImageSize(layoutFile, layoutName, skinFilesPath, offsetP);
		} else {
			partSize = getPartImageSize(layoutFile, partName, skinFilesPath, offsetP);
		}

		// Update the display position, considering part offset/size and
		// rotation
		displayPos = translatePartElementPosition(layoutFile, layoutName, partName, skinFilesPath, displayPos,
				displayW, displayH, offsetP, partSize);

		// Adjusts the position (according to the layout offset) and returns to
		// the caller
		Point offsetL = getLayoutOffset(layoutFile, layoutName, skinFilesPath);
		displayPos.x += offsetL.x;
		displayPos.y += offsetL.y;

		return displayPos;
	}

	/**
	 * Retrieves the keycodes to be used at the keys. Uses as parameter the
	 * keyboard charmap declared at the layout file model <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file <br>
	 * <br>
	 * @return A map of properties containing the keycodes for each key of the
	 *         charmap declared at the layout file <br>
	 * <br>
	 * @throws SkinException
	 *             If the keycode file cannot be loaded
	 */
	static Properties getKeycodes(LayoutFileModel layoutFile, File skinFilesPath) throws SkinException {
		String charmap = layoutFile.getKeyboardCharmap();
		Properties keycodes = new Properties();
		InputStream is = null;
		URL url = null;
		try {
			// If nothing is specified, use the QWERTY charmap
			// If it is specified QWERTY or QWERTY2, use QWERTY charmap too
			if ((charmap == null) || (charmap.equals(CHARMAP_QWERTY)) || (charmap.equals(CHARMAP_QWERTY2))) {

				url = EmulatorPlugin.getDefault().getBundle()
						.getResource(SdkUtils.isOphoneSDK() ? CHARMAP_OPHONE_QWERTY_FILE : CHARMAP_QWERTY_FILE);
			} else if (charmap.equals(CHARMAP_AVRCP)) {
				url = EmulatorPlugin.getDefault().getBundle()
						.getResource(SdkUtils.isOphoneSDK() ? CHARMAP_OPHONE_AVRCP_FILE : CHARMAP_AVRCP_FILE);
			} else {
				warn("The skin at " + skinFilesPath.getAbsolutePath() + " does not use a supported charmap");
				return keycodes;
			}

			is = url.openStream();
			keycodes.load(is);
		} catch (IOException e) {
			error("There was an error reading the file " + url);
			throw new SkinException(EmulatorNLS.ERR_AndroidSkinTranslator_ErrorReadingKeycodeFile);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				StudioLogger.error("Could not close input stream: ", e.getMessage()); //$NON-NLS-1$
			}
		}

		return keycodes;
	}

	public static Properties getQwertyKeyMap() {
		Properties keycodes = new Properties();
		URL url = EmulatorPlugin.getDefault().getBundle()
				.getResource(SdkUtils.isOphoneSDK() ? CHARMAP_OPHONE_QWERTY_FILE : CHARMAP_QWERTY_FILE);

		InputStream in;
		try {
			in = url.openStream();
			keycodes.load(in);
		} catch (IOException e) {
			error("There was an error reading the file " + url);
		}

		return keycodes;

	}

	/**
	 * Calculates and retrieves the layout offset. The offset is different from
	 * (0, 0) if any part has negative coordinates (x or y) according to the
	 * specification of the layout file <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The layout that we wish to have the offset calculated
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return The layout offset
	 */
	private static Point getLayoutOffset(LayoutFileModel layoutFile, String layoutName, File skinFilesPath) {
		int minX = 0;
		int minY = 0;

		Collection<String> partNames = layoutFile.getLayoutPartNames(layoutName);

		for (String partName : partNames) {
			if (partName.equals("portrait") || partName.equals("landscape")) {
				if (!partName.equals(layoutName)) {
					continue;
				}
			}

			Point offsetP = getPartOffset(layoutFile, partName);

			Point partSize;
			if (layoutFile.partHasBg(partName)) {
				partSize = getPartImageSize(layoutFile, partName, skinFilesPath, offsetP);
			} else {
				continue;
				// partSize = getPartImageSize(layoutFile, layoutName,
				// skinFilesPath, offsetP);
			}

			// The part position needs translation because its coordinates may
			// change due to
			// the buttons position (if the part offset is different from (0,
			// 0))
			Point partPos = translatePartPosition(layoutFile, layoutName, partName, skinFilesPath, offsetP, partSize);
			if (partPos.x < minX) {
				minX = partPos.x;
			}
			if (partPos.y < minY) {
				minY = partPos.y;
			}
		}

		Point layoutOffset = new Point(Math.abs(minX), Math.abs(minY));

		return layoutOffset;
	}

	/**
	 * Calculates and retrieves a part offset. The offset is different from (0,
	 * 0) if any button that belong to the part has negative coordinates (x or
	 * y) according to the specification of the layout file <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param partName
	 *            The part that we wish to have the offset calculated <br>
	 * <br>
	 * @return The part offset
	 */
	private static Point getPartOffset(LayoutFileModel layoutFile, String partName) {
		int minX = 0;
		int minY = 0;

		Collection<String> buttonNames = layoutFile.getButtonNames(partName);

		for (String buttonName : buttonNames) {
			Point buttonPos = layoutFile.getButtonPosition(partName, buttonName);
			if (buttonPos.x < minX) {
				minX = buttonPos.x;
			}
			if (buttonPos.y < minY) {
				minY = buttonPos.y;
			}
		}

		Point offset = new Point(Math.abs(minX), Math.abs(minY));

		return offset;
	}

	/**
	 * Retrieves the size of the layout image. <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The name of the layout to have its size calculated
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return The size of the layout image
	 */
	private static Point getLayoutImageSize(LayoutFileModel layoutFile, String layoutName, File skinFilesPath,
			Point layoutOffset) {
		int maxX = 0;
		int maxY = 0;

		// Iterates on the layout parts, looking for the area required by each
		// of them

		Collection<String> partNames = layoutFile.getLayoutPartNames(layoutName);
		for (String partName : partNames) {

			if (partName.equals("portrait") || partName.equals("landscape")) {
				if (!partName.equals(layoutName)) {
					continue;
				}
			}

			// The part position must be translated because:
			// - At Google layout file, the part (x,y) position represents the
			// upper left corner of the
			// PART image, no matter what is the rotation value
			// - We are generating a layout image referenced at the upper left
			// corner of the LAYOUT
			// image, and we must know where we must place the part image in
			// terms of the layout reference
			Point offsetP = getPartOffset(layoutFile, partName);
			Point partSize = getPartImageSize(layoutFile, partName, skinFilesPath, offsetP);
			Point partPos = translatePartPosition(layoutFile, layoutName, partName, skinFilesPath, offsetP, partSize);
			if (layoutFile.isSwapWidthHeightNeededAtLayout(layoutName, partName)) {
				// If the part is in landscape direction, we sum the width at y
				// and
				// height at x
				//
				// --------------
				// Y | width |
				// | |
				// A | h | --------------------------
				// X | e | | height |
				// I | i | | w |
				// S | g | | i |
				// | h | | d |
				// | t | | t |
				// | | | h ROTATED PART |
				// | PART | --------------------------
				// --------------
				// X AXIS
				//
				maxX = Math.max(maxX, layoutOffset.x + partPos.x + partSize.y);
				maxY = Math.max(maxY, layoutOffset.y + partPos.y + partSize.x);
			} else {
				// If the part is in portrait direction, we sum the w/h as is
				maxX = Math.max(maxX, layoutOffset.x + partPos.x + partSize.x);
				maxY = Math.max(maxY, layoutOffset.y + partPos.y + partSize.y);
			}
		}

		Point imgSize = new Point(maxX, maxY);

		return imgSize;
	}

	/**
	 * Retrieves the minimum size of the part image. <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param partName
	 *            The name of the part to have its size calculated
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return The minimum size of the part image
	 */
	private static Point getPartImageSize(LayoutFileModel layoutFile, String partName, File skinFilesPath,
			Point partOffset) {
		// The initial maximum is set to be the width/height of the original
		// image + part offset
		//
		// It will be the final part size IF there are no buttons with negative
		// coordinates AND
		// there are no buttons that is positioned in a coordinate close to the
		// edge enough for not
		// fitting (considering their width/height)
		int bgWidth = layoutFile.getBackgroundWidth(partName, skinFilesPath);
		int bgHeight = layoutFile.getBackgroundHeight(partName, skinFilesPath);
		Point bgPos = layoutFile.getBackgroundPosition(partName);
		int maxX = partOffset.x + bgPos.x + bgWidth;
		int maxY = partOffset.y + bgPos.y + bgHeight;

		// Iterates on the buttons, looking for the area required by each of
		// them
		Collection<String> buttonNames = layoutFile.getButtonNames(partName);

		for (String buttonName : buttonNames) {
			int btWidth = layoutFile.getButtonWidth(partName, buttonName, skinFilesPath);
			int btHeight = layoutFile.getButtonHeight(partName, buttonName, skinFilesPath);
			Point buttonPos = layoutFile.getButtonPosition(partName, buttonName);

			// If a button is described to be drawn outside the current maximum
			// (x,y) position, update the (x,y) to make it fit
			//
			// -------------- --------------
			// (x,y)| | | |
			// --- | | |
			// | | button | | (x,y) |
			// --- with | | ----
			// | negative | | | | button positioned in coordinates
			// | coordinate| | | | inside the part, but with width
			// | | | ---- large enough not to fit
			// | | | |
			// | | | |
			// | PART | | PART |
			// -------------- --------------
			//
			//
			maxX = Math.max(maxX, partOffset.x + buttonPos.x + btWidth);
			maxY = Math.max(maxY, partOffset.y + buttonPos.y + btHeight);
		}

		Point imgSize = new Point(maxX, maxY);

		return imgSize;
	}

	/**
	 * Utility method for loading a image, given the model and part/button <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param partName
	 *            The name of the part to have its background image loaded, or
	 *            that contains the button
	 * @param buttonName
	 *            The name of the button we wish the image loaded, or
	 *            <code>null</code> if we aim to load the part background image
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return An image data object, containing the image pixels
	 */
	private static ImageData getImageData(LayoutFileModel layoutFile, String partName, String buttonName,
			File skinFilesPath) {
		File f;
		if (buttonName == null) {
			f = layoutFile.getBackgroundImage(partName, skinFilesPath);
		} else {
			f = new File(skinFilesPath, layoutFile.getButtonImage(partName, buttonName).getName());
		}

		return new ImageData(f.getAbsolutePath());
	}

	/**
	 * Creates a new expanded part image that contains enough space for drawing
	 * all the buttons <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The name of the layout containing the part, if there is one,
	 *            or <code>null</code>
	 * @param partName
	 *            The part to be expanded
	 * @param offset
	 *            The part offset to use when positioning the part image at the
	 *            expanded image
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found <br>
	 * <br>
	 * @return An image data containing the part image with more space at the
	 *         background area
	 */
	private static ImageData generateExpandedPartImageData(LayoutFileModel layoutFile, String layoutName,
			String partName, Point offset, File skinFilesPath, Point partOffset, Point partSize) {
		ImageData img = null;
		if (partName != null) {
			// Creates an image data with dimensions defined by partSize, and
			// background color, depth
			// and palette defined by bgImg
			ImageData bgImg = getImageData(layoutFile, partName, null, skinFilesPath);
			img = generateImageDataWithBackground(layoutFile, layoutName, partSize.x, partSize.y, bgImg, skinFilesPath);

			// Merges the bgImg pixels at the image data
			int[] row = new int[bgImg.width];
			for (int i = 0; i < bgImg.height; i++) {
				bgImg.getPixels(0, i, bgImg.width, row, 0);
				img.setPixels(offset.x, offset.y + i, bgImg.width, row, 0);
			}

		}

		return img;
	}

	/**
	 * Creates an image data of dimensions x, y and the same background color as
	 * srcImg <br>
	 * <br>
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The name of the layout that may have a background color
	 *            defined
	 * @param width
	 *            The width of the image
	 * @param height
	 *            The height of the image
	 * @param srcImg
	 *            An image that we wish to have its depth, palette (and perhaps
	 *            background color) copied to the new image <br>
	 * <br>
	 * @return An image of size (width, height), same depth and palette of
	 *         srcImg and with filled with the background color defined by the
	 *         model or srcImg
	 */
	private static ImageData generateImageDataWithBackground(LayoutFileModel layoutFile, String layoutName, int width,
			int height, ImageData srcImg, File skinFilesPath) {
		ImageData img = new ImageData(width, height, srcImg.depth, srcImg.palette);

		// Discover what is the background color. This is needed to fill the
		// spaces around the layout image
		RGB bgColor = layoutFile.getLayoutColor(layoutName, skinFilesPath);
		int bgPixel = srcImg.palette.getPixel(bgColor);

		// Set the background color to the entire layout image
		for (int i = 0; i < img.width; i++) {
			for (int j = 0; j < img.height; j++) {
				img.setPixel(i, j, bgPixel);
			}
		}

		return img;
	}

	/**
	 * Creates a new image, based on imageToRotate, rotated according to
	 * rotation <br>
	 * <br>
	 * 
	 * @param imageToRotate
	 *            The image that is used as source for rotation
	 * @param rotation
	 *            (rotation * 90) results in how many degrees to rotate
	 *            CLOCKWISE <br>
	 * <br>
	 * @return The rotated image
	 */
	private static ImageData generateRotatedImage(ImageData imageToRotate, int rotation) {
		// For each rotation case, generates an appropriate image data (with
		// dimensions w/h or h/w
		// depending on whether it is landscape or portrait) and copies the
		// imageToRotate pixels at the
		// appropriate positions
		ImageData rotated;
		switch (rotation % 4) {
		case 1:
			// 0------- 0 j h
			// | --- | ---------
			// j| | | | | --- |
			// | --- | | | | |
			// | | | --- |
			// h------- ---------
			rotated = new ImageData(imageToRotate.height, imageToRotate.width, imageToRotate.depth,
					imageToRotate.palette);
			for (int i = 0; i < imageToRotate.width; i++) {
				for (int j = 0; j < imageToRotate.height; j++) {
					rotated.setPixel(imageToRotate.height - j - 1, i, imageToRotate.getPixel(i, j));
				}
			}
			break;
		case 2:
			// 0 i w 0 i w
			// 0------- 0-------
			// | --- | | |
			// j| | | | j| --- |
			// | --- | | | | |
			// | | | --- |
			// h------- h-------
			rotated = new ImageData(imageToRotate.width, imageToRotate.height, imageToRotate.depth,
					imageToRotate.palette);
			for (int i = 0; i < imageToRotate.width; i++) {
				for (int j = 0; j < imageToRotate.height; j++) {
					rotated.setPixel(imageToRotate.width - i - 1, imageToRotate.height - j - 1,
							imageToRotate.getPixel(i, j));
				}
			}
			break;
		case 3:
			// 0 i w
			// 0------- 0 j h
			// | --- | 0---------
			// j| | | | | --- |
			// | --- | i| | | |
			// | | | --- |
			// h------- w---------
			rotated = new ImageData(imageToRotate.height, imageToRotate.width, imageToRotate.depth,
					imageToRotate.palette);
			for (int i = 0; i < imageToRotate.width; i++) {
				for (int j = 0; j < imageToRotate.height; j++) {
					rotated.setPixel(j, imageToRotate.width - i - 1, imageToRotate.getPixel(i, j));
				}
			}
			break;
		default:
			// If 0, there is no need to rotate
			rotated = imageToRotate;
			break;
		}

		return rotated;
	}

	/**
	 * Creates an image that contains buttons with proper transparency. The
	 * transparency is defined by the isEnter parameter
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param baseImage
	 *            The image to use as base for generation. Must be a copy,
	 *            because it will be changed in-place.
	 * @param partName
	 *            The name of the part where to look for buttons in the model
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found
	 * @param partOffset
	 *            What is the calculated offset for the given part
	 * @param isEnter
	 *            Whether the image being created will be used for enter or
	 *            pressed
	 * @return
	 */
	private static ImageData generateMergedWithButtonsImage(LayoutFileModel layoutFile, ImageData baseImage,
			String partName, File skinFilesPath, Point partOffset, boolean isEnter) {
		// Iterate on the buttons, merging the buttons pixels to the base image
		Collection<String> buttonNames = layoutFile.getButtonNames(partName);
		for (String buttonName : buttonNames) {
			ImageData buttonID = getImageData(layoutFile, partName, buttonName, skinFilesPath);
			Point buttonPos = layoutFile.getButtonPosition(partName, buttonName);
			buttonPos.x += partOffset.x;
			buttonPos.y += partOffset.y;
			mergeButtonData(baseImage, buttonID, buttonPos, isEnter);
		}

		return baseImage;
	}

	/**
	 * Merges the button data to the base image <br>
	 * <br>
	 * 
	 * @param baseImage
	 *            The image that will be modified
	 * @param buttonImage
	 *            The image that have the source pixels for the merge operation
	 * @param buttonPos
	 *            Where the button is located
	 * @param isEnter
	 *            Whether the image being created will be used for enter or
	 *            pressed
	 */
	private static void mergeButtonData(ImageData baseImage, ImageData buttonImage, Point buttonPos, boolean isEnter) {
		// Pixel/alpha buffers
		int[] baseImgPixels = new int[buttonImage.width];
		int[] buttonImgPixels = new int[buttonImage.width];
		byte[] buttonAlphas = new byte[buttonImage.width];
		int[] intButtonAlphas = new int[buttonImage.width];

		// For each pixel row, get the button pixel data, apply the transparency
		// defined by alpha and copy data to the base image
		for (int i = 0; i < buttonImage.height; i++) {
			baseImage.getPixels(buttonPos.x, buttonPos.y + i, buttonImage.width, baseImgPixels, 0);
			buttonImage.getPixels(0, i, buttonImage.width, buttonImgPixels, 0);
			buttonImage.getAlphas(0, i, buttonImage.width, buttonAlphas, 0);

			for (int j = 0; j < buttonAlphas.length; j++) {
				// As buttonAlphas is a signed byte array with range -127 to
				// 128, and alpha is
				// an integer in the range 0 to 255, overflows can happen. This
				// calculation assures
				// that the alpha variable has correct value in an integer
				// array.
				intButtonAlphas[j] = (buttonAlphas[j] >= 0 ? buttonAlphas[j]
						: ((buttonAlphas[j]) & ((byte) 0x7F)) + 128);
			}

			if (!isEnter) {
				for (int j = 0; j < buttonAlphas.length; j++) {
					if (intButtonAlphas[j] > 0) {
						intButtonAlphas[j] += (255 - intButtonAlphas[j]) / 4;
					}
				}
			}

			addTransparency(baseImgPixels, buttonImgPixels, intButtonAlphas, baseImage.palette, buttonImage.palette);

			baseImage.setPixels(buttonPos.x, buttonPos.y + i, buttonImage.width, baseImgPixels, 0);
		}
	}

	/**
	 * Calculates transparency for the button pixels and sets them to the base
	 * pixels buffer <br>
	 * <br>
	 * 
	 * @param basePixels
	 *            The buffer containing pixels for a given line of the base
	 *            image
	 * @param buttonPixels
	 *            The buffer containing pixels for a given line of the button
	 *            image
	 * @param buttonAlphas
	 *            The buffer containing alpha information for a given line of
	 *            the button image
	 * @param basePalette
	 *            The color palette used by the base image
	 * @param buttonPalette
	 *            The color palette used by the button image
	 */
	private static void addTransparency(int[] basePixels, int[] buttonPixels, int[] buttonAlphas,
			PaletteData basePalette, PaletteData buttonPalette) {
		for (int i = 0; i < buttonPixels.length; i++) {
			RGB buttonRgb = buttonPalette.getRGB(buttonPixels[i]);
			RGB baseRgb = basePalette.getRGB(basePixels[i]);

			RGB newRgb = new RGB(calculateMerge(baseRgb.red, buttonRgb.red, buttonAlphas[i]), calculateMerge(
					baseRgb.green, buttonRgb.green, buttonAlphas[i]), calculateMerge(baseRgb.blue, buttonRgb.blue,
					buttonAlphas[i]));

			basePixels[i] = basePalette.getPixel(newRgb);
		}
	}

	/**
	 * Calculates the transparency for a single color component <br>
	 * <br>
	 * 
	 * @param background
	 *            The background color component
	 * @param foreground
	 *            The foreground color component
	 * @param alpha
	 *            The alpha to be applied. 0 means pure transparent (background
	 *            color is used). 255 means pure opaque (foreground color is
	 *            used) <br>
	 * <br>
	 * @return The resulting color
	 */
	private static int calculateMerge(int background, int foreground, int alpha) {
		// weighted medium of foreground color and background color, with alpha
		// as parameter
		return (foreground * alpha + background * (255 - alpha)) / 255;
	}

	/**
	 * Translates the part position information from the Google format to the
	 * upper-left reference used by the viewer
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The layout where the part is included
	 * @param partName
	 *            The part to have its position calculated
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found
	 * 
	 * @return The point where the part must be drawn in the layout, using as
	 *         reference the upper-left corner of the layout image
	 */
	private static Point translatePartPosition(LayoutFileModel layoutFile, String layoutName, String partName,
			File skinFilesPath, Point partOffset, Point partSize) {
		// Collect needed data
		int rotation = layoutFile.getPartRotationAtLayout(layoutName, partName);
		Point partPos = layoutFile.getPartPositionAtLayout(layoutName, partName, skinFilesPath);
		int bgWidth;
		int bgHeight;
		if (layoutFile.partHasBg(partName)) {
			bgWidth = layoutFile.getBackgroundWidth(partName, skinFilesPath);
			bgHeight = layoutFile.getBackgroundHeight(partName, skinFilesPath);
		} else {
			bgWidth = layoutFile.getBackgroundWidth(layoutName, skinFilesPath);
			bgHeight = layoutFile.getBackgroundHeight(layoutName, skinFilesPath);
		}
		int extraOnEndW = partSize.x - bgWidth - partOffset.x;
		int extraOnEndH = partSize.y - bgHeight - partOffset.y;

		// Calculate translation
		switch (rotation % 4) {
		case 1:
			// Landscape, top of part image is at the right (90 degrees
			// clockwise rotation)
			// The point we must return is the one at the bottom-left corner of
			// the part, considering
			// offset and extra space in the end of the part image (which was
			// added so that buttons
			// at the right side of the part fit)
			//
			// BEFORE AFTER
			// (0,0) (0,0)
			// --------- ---------
			// | --- | | --- |
			// | | | | | | | |
			// | --- | | --- |
			// --------- ---------
			partPos.x = partPos.x - partOffset.y - bgHeight;
			partPos.y = partPos.y - extraOnEndW;
			break;
		case 2:
			// Portrait, top of part image is at the bottom (180 degrees
			// clockwise rotation)
			// The point we must return is the one at the bottom-right corner of
			// the part
			//
			// BEFORE AFTER
			// (0,0)
			// ------- -------
			// | | | |
			// | --- | | --- |
			// | | | | | | | |
			// | --- | | --- |
			// ------- -------
			// (0,0)
			partPos.x = partPos.x - bgWidth;
			partPos.y = partPos.y - bgHeight;
			break;
		case 3:
			// Landscape, top of part image is at the left (270 degrees
			// clockwise rotation)
			// The point we must return is the one at the top-right corner of
			// the part, considering
			// offset and extra space in the end of the part image (which was
			// added so that buttons
			// at the right side of the part fit)
			//
			// BEFORE AFTER
			// (0,0)
			// --------- ---------
			// | --- | | --- |
			// | | | | | | | |
			// | --- | | --- |
			// --------- ---------
			// (0,0)
			partPos.x = partPos.x - extraOnEndH;
			partPos.y = partPos.y - partOffset.x - bgWidth;
			break;
		default:
			// No translation is needed when there is no rotation
			break;
		}

		return partPos;
	}

	/**
	 * Translates the button position information from the Google format to the
	 * upper-left reference used by the viewer
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The layout where the part is included, or <code>null</code> if
	 *            the skin does not support layout
	 * @param partName
	 *            The part where the button is included
	 * @param buttonName
	 *            The button to have its position calculated
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found
	 * 
	 * @return The point where the button must be drawn in the part, using as
	 *         reference the upper-left corner of the part image
	 */
	private static Point translateButtonPosition(LayoutFileModel layoutFile, String layoutName, String partName,
			String buttonName, File skinFilesPath, Point partOffset, Point partSize) {
		// Collect button data
		Point buttonPos = layoutFile.getButtonPosition(partName, buttonName);
		int buttonW = layoutFile.getButtonWidth(partName, buttonName, skinFilesPath);
		int buttonH = layoutFile.getButtonHeight(partName, buttonName, skinFilesPath);

		// Update the button position, considering part offset/size and rotation
		buttonPos = translatePartElementPosition(layoutFile, layoutName, partName, skinFilesPath, buttonPos, buttonW,
				buttonH, partOffset, partSize);

		return buttonPos;
	}

	/**
	 * Translates a part element position (display/buttons) from the Google
	 * format to the upper-left reference used by the viewer
	 * 
	 * @param layoutFile
	 *            The model that represents the layout file
	 * @param layoutName
	 *            The layout where the part is included, or <code>null</code> if
	 *            the skin does not support layout
	 * @param partName
	 *            The part where the element is included
	 * @param skinFilesPath
	 *            The path to the skin dir, where skin files can be found
	 * @param partElementPos
	 *            The position of the part element as described by layoutFile
	 * @param partElementWidth
	 *            The width of the part element as described by layoutFile
	 * @param partElementHeight
	 *            The height of the part element as described by layoutFile
	 * 
	 * @return The point where the element must be drawn in the part, using as
	 *         reference the upper-left corner of the part image
	 */
	private static Point translatePartElementPosition(LayoutFileModel layoutFile, String layoutName, String partName,
			File skinFilesPath, Point partElementPos, int partElementWidth, int partElementHeight, Point partOffset,
			Point partSize) {
		Point translated = new Point(0, 0);
		int rotation = layoutFile.getPartRotationAtLayout(layoutName, partName);

		// Due to rotation, the part position will be referenced to a
		// non-appropriate image corner.
		// The following operation guarantees that the part position is still at
		// the upper left corner
		// even after rotation.
		Point partPos = translatePartPosition(layoutFile, layoutName, partName, skinFilesPath, partOffset, partSize);

		// Calculate position.
		//
		// OBS: Every time we need the part size for our the calculation, we
		// must subtract the part offset
		// as well. This is because during part size calculation, we have
		// already summed the offset and we
		// need to rework the offset due to rotation (i.e., sometimes we need to
		// sum offset.y instead of
		// offset.x due to rotation, and vice-versa). This is being illustrated
		// at the lines below with
		// parenthesis.
		switch (rotation % 4) {
		case 1:
			// BEFORE AFTER
			// (0,0)
			// --------- (0,0)
			// | | -----------
			// |(x,y) | (x,y) |
			// | --- | | --- |
			// | | | | | | | |
			// | --- | | --- |
			// --------- -----------
			translated.x = partPos.x - partOffset.x + (partSize.y - partOffset.y) - partElementPos.y
					- partElementHeight;
			translated.y = partPos.y - partOffset.y + partOffset.x + partElementPos.x;
			break;
		case 2:
			// BEFORE AFTER
			// (0,0) (0,0)
			// --------- ---------
			// | | (x,y) --- |
			// |(x,y) | | | | |
			// | --- | | --- |
			// | | | | | |
			// | --- | | |
			// --------- ---------
			translated.x = partPos.x + (partSize.x - partOffset.x) - partOffset.x - partElementPos.x - partElementWidth;
			translated.y = partPos.y + (partSize.y - partOffset.y) - partOffset.y - partElementPos.y
					- partElementHeight;
			break;
		case 3:
			// BEFORE AFTER
			// (0,0)
			// --------- (0,0)
			// | | -----------
			// |(x,y) | |(x,y)--- |
			// | --- | | | | |
			// | | | | | --- |
			// | --- | | |
			// --------- -----------
			translated.x = partPos.x - partOffset.x + partOffset.y + partElementPos.y;
			translated.y = partPos.y - partOffset.y + (partSize.x - partOffset.x) - partElementPos.x - partElementWidth;
			break;
		default:
			translated.x = partElementPos.x + partPos.x;
			translated.y = partElementPos.y + partPos.y;
			break;
		}

		return translated;
	}
}
