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
import static org.eclipse.andmore.android.common.log.StudioLogger.info;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.skin.AndroidSkinBean;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidKey;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.ISkinKeyXmlTags;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.skin.android.parser.LayoutFileModel;
import org.eclipse.andmore.android.emulator.skin.android.parser.LayoutFileParser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

public class AndroidSkin implements IAndroidSkin {
	/**
	 * The released images of the skin, as read from the skin files
	 */
	private final Map<String, ImageData> releasedImagesPool = new HashMap<String, ImageData>();

	/**
	 * The pressed images of the skin, as read from the skin files
	 */
	private final Map<String, ImageData> pressedImagesPool = new HashMap<String, ImageData>();

	/**
	 * The enter images of the skin, as read from the skin files
	 */
	private final Map<String, ImageData> enterImagesPool = new HashMap<String, ImageData>();

	/**
	 * The keys of the skin, as read from the skin files
	 */
	private final Map<String, Collection<IAndroidKey>> androidKeysPool = new HashMap<String, Collection<IAndroidKey>>();

	/**
	 * The folder where to look for skin files
	 */
	private File skinFilesPath;

	/**
	 * A model containing the parsed layout file
	 */
	private LayoutFileModel layoutFile;

	private Properties keycodes;

	/**
	 * Retrieves data being kept in a pool
	 * 
	 * @param layoutName
	 *            The name of the layout which object has been requested
	 * @param pool
	 *            The pool where to look for data
	 * 
	 * @return An object from the pool that matches the request
	 * 
	 * @throws SkinException
	 *             If either the layout file was not loaded, or if the
	 *             images/keys cannot be generated
	 */
	private ImageData getImageFromPool(String layoutName, Map<String, ImageData> pool) throws SkinException {
		if (layoutFile == null) {
			error("User has tried to request skin data without setting a valid skin files path");
			throw new SkinException(EmulatorNLS.ERR_AndroidSkin_NoLayoutLoaded);
		}

		// Tries to get data from the pool. If it is not available for the
		// current layout yet, load all resources related to the layout to the
		// pools.
		ImageData id = pool.get(layoutName);

		if (id == null) {

			addImagesToPools(layoutName);
			id = pool.get(layoutName);
		}

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getKeyDataCollection(java.lang.String)
	 */
	@Override
	public Collection<IAndroidKey> getKeyDataCollection(String layoutName) {
		Collection<IAndroidKey> androidKeys;
		try {
			if (layoutFile == null) {
				error("User has tried to request skin data without setting a valid skin files path");
				throw new SkinException(EmulatorNLS.ERR_AndroidSkin_NoLayoutLoaded);
			}

			// Tries to get data from the pool. If it is not available for the
			// current layout yet, load all resources related to the layout to
			// the pools.
			androidKeys = androidKeysPool.get(layoutName);

			if (androidKeys == null) {

				System.gc();
				androidKeys = AndroidSkinTranslator.generateAndroidKeys(layoutFile, layoutName, skinFilesPath);
				System.gc();
				androidKeysPool.put(layoutName, androidKeys);
			}
		} catch (SkinException e) {
			androidKeys = new HashSet<IAndroidKey>();
			error("The key data could not be retrieved from skin files. Cause: " + e.getMessage());
			EclipseUtils.showErrorDialog(e);
		}

		return androidKeys;
	}

	@Override
	public Properties getKeyCodes() {
		if ((keycodes == null) || ((keycodes != null) && keycodes.isEmpty())) {
			try {
				keycodes = AndroidSkinTranslator.getKeycodes(layoutFile, skinFilesPath);
			} catch (SkinException e) {
				keycodes = new Properties();
				error("The key data could not be retrieved from skin files. Cause: " + e.getMessage());
			}
		}

		return keycodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getPressedImageData(java.lang.String)
	 */
	@Override
	public ImageData getPressedImageData(String layoutName) throws SkinException {
		return getImageFromPool(layoutName, pressedImagesPool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#getEnterImageData
	 * (java.lang.String)
	 */
	@Override
	public ImageData getEnterImageData(String layoutName) throws SkinException {
		return getImageFromPool(layoutName, enterImagesPool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getReleasedImageData(java.lang.String)
	 */
	@Override
	public ImageData getReleasedImageData(String layoutName) throws SkinException {
		return getImageFromPool(layoutName, releasedImagesPool);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#getSkinBean
	 * (java.lang.String)
	 */
	@Override
	public AndroidSkinBean getSkinBean(String layoutName) throws SkinException {
		if (layoutFile == null) {
			error("User has tried to request additional skin data without setting a valid skin files path");
			throw new SkinException(EmulatorNLS.ERR_AndroidSkin_NoLayoutLoaded);
		}

		AndroidSkinBean bean = new AndroidSkinBean();

		// Fills the skin bean with information related to the part chosen for
		// display. This bean must have
		// at least display positioning information and scale.
		String partName = layoutFile.getMainPartName(layoutName);
		Point dPosition = AndroidSkinTranslator.translateDisplayPosition(layoutFile, layoutName, partName,
				skinFilesPath);
		int dWidth = layoutFile.getDisplayWidth(partName);
		int dHeight = layoutFile.getDisplayHeight(partName);

		int dw, dh;
		if (layoutFile.isSwapWidthHeightNeededAtLayout(layoutName)) {
			dw = dHeight;
			dh = dWidth;
		} else {
			dw = dWidth;
			dh = dHeight;
		}

		bean.addSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_X, dPosition.x);
		bean.addSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_Y, dPosition.y);
		bean.addSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH, dw);
		bean.addSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT, dh);
		bean.addSkinPropertyValue(ISkinKeyXmlTags.SKIN_EMBEDDED_VIEW_SCALE, 10);

		return bean;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#isFlipSupported
	 * ()
	 */
	@Override
	public boolean isFlipSupported() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#setSkinFilesPath
	 * (java.lang.String)
	 */
	@Override
	public void setSkinFilesPath(String skinFilesPath) throws SkinException {

		this.skinFilesPath = new File(skinFilesPath);
		if (this.skinFilesPath.isDirectory()) {

			layoutFile = LayoutFileParser.readLayout(this.skinFilesPath);

		} else {
			error("Provided skin files path is not a directory. Setting the skin files path operation has failed.");
			throw new SkinException(EmulatorNLS.ERR_AndroidSkin_ProvidedSkinPathIsNotADirectory);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#isRotatedLayout
	 * (java.lang.String)
	 */
	@Override
	public boolean isSwapWidthHeightNeededAtLayout(String layoutName) {
		return layoutFile.isSwapWidthHeightNeededAtLayout(layoutName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getAvailableLayouts()
	 */
	@Override
	public Collection<String> getAvailableLayouts() {
		return layoutFile.getLayoutNames();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getLayoutScreenCommand(java.lang.String)
	 */
	@Override
	public String getLayoutScreenCommand(String layoutName) {
		return layoutFile.getLayoutSwitchCommand(layoutName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#nextLayout
	 * (java.lang.String)
	 */
	@Override
	public String getNextLayout(String referenceLayout) {
		info("Calculating the next layout");
		String next = null;
		if (referenceLayout != null) {
			List<String> layoutsList = new ArrayList<String>(getAvailableLayouts());

			// Switches to the next layout if the skin supports it. The if/else
			// clause
			// implements a circular list
			if (layoutsList.size() > 1) {
				int currentLayoutNum = layoutsList.indexOf(referenceLayout);
				if (currentLayoutNum != layoutsList.size() - 1) {
					next = layoutsList.get(++currentLayoutNum);
				} else {
					next = layoutsList.get(0);
				}
				info("Next layout:  " + next);
			} else {
				warn("The skin doesn't have multiple layouts. The operation was not performed");
			}
		} else {
			warn("The skin doesn't have multiple layouts. The operation was not performed");
		}

		return next;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#previousLayout
	 * (java.lang.String)
	 */
	@Override
	public String getPreviousLayout(String referenceLayout) {
		info("Calculating the previous layout");
		String previous = null;
		if (referenceLayout != null) {
			List<String> layoutsList = new ArrayList<String>(getAvailableLayouts());

			// Switches to the previous layout if the skin supports it. The
			// if/else clause
			// implements a circular list
			if (layoutsList.size() > 1) {
				int currentLayoutNum = layoutsList.indexOf(referenceLayout);

				if (currentLayoutNum != 0) {
					previous = layoutsList.get(--currentLayoutNum);
				} else {
					previous = layoutsList.get(layoutsList.size() - 1);
				}
				info("Previous layout: " + previous);
			} else {
				warn("The skin doesn't have multiple layouts. The operation was not performed");
			}
		} else {
			warn("The skin doesn't have multiple layouts. The operation was not performed");
		}

		return previous;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.core.skin.IAndroidSkin#
	 * getBackgroundColor(java.lang.String)
	 */
	@Override
	public RGB getBackgroundColor(String layoutName) {
		return layoutFile.getLayoutColor(layoutName, skinFilesPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.skin.IAndroidSkin#getDpadRotation
	 * (java.lang.String)
	 */
	@Override
	public int getDpadRotation(String layoutName) {
		return layoutFile.getDpadRotation(layoutName);
	}

	/**
	 * Generates images/keys for the current layout (or main part, if a layout
	 * is not available) and store them at the appropriate pools
	 * 
	 * @param key
	 *            The key to be used to store data at the pools
	 * 
	 * @throws SkinException
	 *             If the layout file cannot be loaded
	 */
	private void addImagesToPools(String key) throws SkinException {
		// Validates the provided string before proceeding
		if ((key == null) || (!layoutFile.getLayoutNames().contains(key))) {
			throw new SkinException(EmulatorNLS.ERR_AndroidSkin_InvalidLayoutProvided);
		}

		System.gc();

		ImageData[] images = AndroidSkinTranslator.generateLayoutImages(layoutFile, key, skinFilesPath);

		info("Adding released/pressed/hover images to the pool");
		releasedImagesPool.put(key, images[0]);
		pressedImagesPool.put(key, images[1]);
		enterImagesPool.put(key, images[2]);

		System.gc();
	}
}
