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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

/**
 * DESCRIPTION: This class represents a skin layout file
 *
 * RESPONSIBILITY: Represent all the contents of a skin layout file
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the class public APIs to retrieve data of the skin
 */
public class LayoutFileModel {
	/**
	 * Event to rotate the screen
	 */
	private static final String ROTATE_SCREEN_EVENT = "EV_SW:0:0";

	/**
	 * Command to rotate the screen
	 */
	private static final String ROTATE_SCREEN_CMD = "5 0 0";

	/**
	 * Command to get the screen back to its default orientation
	 */
	private static final String RETURN_TO_DEFAULT_SCREEN_CMD = "5 0 1";

	/**
	 * A collection containing all layouts read from the layout file
	 */
	private Collection<LayoutBean> layouts;

	/**
	 * A collection containing all parts read from the layout file
	 */
	private Collection<PartBean> parts = new LinkedHashSet<PartBean>();

	/**
	 * The keyboard charmap used by this skin
	 */
	private String keyboardCharmap;

	/**
	 * The default network speed used by this skin
	 */
	private String networkSpeed;

	/**
	 * The default network delay used by this skin
	 */
	private String networkDelay;

	/**
	 * Sets the keyboard charmap used by this skin
	 * 
	 * @param keyboardCharmap
	 *            The keyboard charmap name
	 */
	void setKeyboardCharmap(String keyboardCharmap) {
		this.keyboardCharmap = keyboardCharmap;
	}

	/**
	 * Sets the default network speed used by this skin
	 * 
	 * @param keyboardCharmap
	 *            The default network speed used by this skin
	 */
	void setNetworkSpeed(String networkSpeed) {
		this.networkSpeed = networkSpeed;
	}

	/**
	 * Sets the default network delay of this skin
	 * 
	 * @param The
	 *            default network delay
	 */
	void setNetworkDelay(String networkDelay) {
		this.networkDelay = networkDelay;
	}

	/**
	 * Creates a new part, registers it and returns it to the user
	 * 
	 * This version is used when the skin is simple, i.e. when there is a single
	 * part and no layouts defined. To support landscape/portrait rotation, we
	 * create "pseudo-layouts" at memory as well.
	 * 
	 * @return The part
	 */
	PartBean newPart() {
		return newPart(PartBean.UNIQUE_PART);
	}

	/**
	 * Creates a new part, registers it and returns it to the user Use this
	 * version when the skin have multiple parts (i.e., if there is a "parts"
	 * element in the layout file)
	 * 
	 * @param name
	 *            The part name
	 * 
	 * @return The part
	 */
	PartBean newPart(String name) {
		PartBean bean = new PartBean(name);
		parts.add(bean);
		return bean;
	}

	/**
	 * Creates a new layout, registers it and returns it to the user
	 * 
	 * @param name
	 *            The layout name
	 * 
	 * @return The layout
	 */
	LayoutBean newLayout(String name) {
		LayoutBean bean = new LayoutBean(name);
		if (layouts == null) {
			layouts = new LinkedHashSet<LayoutBean>();
		}
		layouts.add(bean);

		return bean;
	}

	/**
	 * Retrieves the keyboard charmap used by this skin
	 * 
	 * @return The keyboard charmap name
	 */
	public String getKeyboardCharmap() {
		return keyboardCharmap;
	}

	/**
	 * Retrieves the default network speed of this skin
	 * 
	 * @return The default network speed
	 */
	public String getNetworkSpeed() {
		return networkSpeed;
	}

	/**
	 * Retrieves the default network delay of this skin
	 * 
	 * @return The default network delay
	 */
	public String getNetworkDelay() {
		return networkDelay;
	}

	public List<String> getLayoutNames() {
		List<String> layoutNames = new LinkedList<String>();
		if (layouts != null) {
			for (LayoutBean bean : layouts) {
				layoutNames.add(bean.getName());
			}
		}

		return layoutNames;
	}

	public Collection<String> getPartNames() {
		Collection<String> partNames = new LinkedHashSet<String>();
		for (PartBean bean : parts) {
			partNames.add(bean.getName());
		}

		return partNames;
	}

	public Collection<String> getLayoutPartNames(String layoutName) {
		Collection<String> layoutPartNames = new LinkedHashSet<String>();
		LayoutBean bean = getLayoutByName(layoutName);
		if (bean != null) {
			Collection<PartRefBean> partRefs = bean.getPartRefs();
			if (partRefs != null) {
				for (PartRefBean aRef : partRefs) {
					layoutPartNames.add(aRef.getPartName());
				}
			}
		}

		return layoutPartNames;
	}

	public Point getPartPositionAtLayout(String layoutName, String partName, File skinFilesPath) {
		Point partPosition = null;

		LayoutBean bean = getLayoutByName(layoutName);
		if (bean != null) {
			Collection<PartRefBean> partRefs = bean.getPartRefs();
			for (PartRefBean prBean : partRefs) {
				if (prBean.getPartName().equals(partName)) {
					int x = Integer.parseInt(prBean.getX());
					String yStr = prBean.getY();
					int y;
					if (yStr == null) {
						y = getBackgroundWidth(partName, skinFilesPath);
					} else {
						y = Integer.parseInt(yStr);
					}

					partPosition = new Point(x, y);
					break;
				}
			}
		} else {
			partPosition = new Point(0, 0);
		}

		return partPosition;
	}

	public int getPartRotationAtLayout(String layoutName, String partName) {
		int partRotation = 0;

		LayoutBean bean = getLayoutByName(layoutName);
		if (bean != null) {
			Collection<PartRefBean> partRefs = bean.getPartRefs();
			for (PartRefBean prBean : partRefs) {
				if (prBean.getPartName().equals(partName)) {
					String rotStr = prBean.getRotation();
					if (rotStr != null) {
						partRotation = Integer.parseInt(rotStr);
					}
					break;
				}
			}
		}

		return partRotation;
	}

	public int getDpadRotation(String layoutName) {
		int dPadRotation = 0;
		LayoutBean bean = getLayoutByName(layoutName);
		if (bean != null) {
			dPadRotation = bean.getDpadRotation();
		}

		return dPadRotation;
	}

	public Collection<String> getButtonNames(String partName) {
		Collection<String> buttonNames = new LinkedHashSet<String>();

		PartBean bean = getPartByName(partName);
		if (bean != null) {
			Collection<ImagePositionBean> buttons = bean.getButtons();
			if (buttons != null) {
				for (ImagePositionBean button : buttons) {
					buttonNames.add(button.getName());
				}
			}
		}
		return buttonNames;
	}

	public int getLayoutWidth(String layoutName) {
		int width = 0;
		LayoutBean layout = getLayoutByName(layoutName);
		if (layout != null) {
			width = Integer.parseInt(layout.getWidth());
		}

		return width;
	}

	public int getLayoutHeight(String layoutName) {
		int height = 0;
		LayoutBean layout = getLayoutByName(layoutName);
		if (layout != null) {
			height = Integer.parseInt(layout.getHeight());
		}

		return height;
	}

	public RGB getLayoutColor(String layoutName, File skinFilesPath) {
		RGB color = null;
		LayoutBean layout = getLayoutByName(layoutName);
		if (layout != null) {
			color = layout.getColor();
			if (color == null) {
				String mainPart = getMainPartName(layoutName);
				File image = getBackgroundImage(mainPart, skinFilesPath);
				ImageData img = new ImageData(image.getAbsolutePath());
				int pixel = img.getPixel(0, 0);
				color = img.palette.getRGB(pixel);
				layout.setKeyValue(
						ILayoutConstants.LAYOUT_COLOR,
						"0x" + Integer.toHexString(color.red) + Integer.toHexString(color.green)
								+ Integer.toHexString(color.blue));
			}
		} else {
			color = new RGB(255, 255, 255);
		}

		return color;
	}

	public String getLayoutEvent(String layoutName) {
		String event = "";
		LayoutBean layout = getLayoutByName(layoutName);
		if (layout != null) {
			event = layout.getEvent();
		}

		return event;
	}

	public String getLayoutSwitchCommand(String layoutName) {
		LayoutBean bean = getLayoutByName(layoutName);
		String event = bean.getEvent();
		if (ROTATE_SCREEN_EVENT.equals(event)) {
			return ROTATE_SCREEN_CMD;
		} else {
			return RETURN_TO_DEFAULT_SCREEN_CMD;
		}
	}

	public boolean isSwapWidthHeightNeededAtLayout(String layoutName) {
		return isSwapWidthHeightNeededAtLayout(layoutName, getMainPartName(layoutName));
	}

	public boolean isSwapWidthHeightNeededAtLayout(String layoutName, String partName) {
		boolean isRotated = false;
		if (partName != null) {
			int rotation = getPartRotationAtLayout(layoutName, partName);
			isRotated = rotation % 2 != 0;
		}
		return isRotated;
	}

	public File getBackgroundImage(String partName, File skinFilesPath) {
		File backgroundFile = null;
		PartBean part = getPartByName(partName);
		if (part != null) {
			ImagePositionBean backgroundBean = part.getBackground();
			if (backgroundBean != null) {
				backgroundFile = new File(skinFilesPath, backgroundBean.getImageLocation().getName());
			}
		}

		return backgroundFile;
	}

	public Point getBackgroundPosition(String partName) {
		Point bgPosition = null;
		PartBean part = getPartByName(partName);
		if (part != null) {
			ImagePositionBean bgBean = part.getBackground();
			String xStr = null;
			String yStr = null;
			if (bgBean != null) {

				xStr = bgBean.getXPos();
				yStr = bgBean.getYPos();
			}

			if ((xStr != null) && (yStr != null)) {
				bgPosition = new Point(Integer.parseInt(xStr), Integer.parseInt(yStr));
			} else {
				bgPosition = new Point(0, 0);
			}
		}

		return bgPosition;
	}

	public int getBackgroundWidth(String partName, File skinFilesPath) {
		int width = -1;
		PartBean part = getPartByName(partName);
		if (part != null) {
			ImagePositionBean bgBean = part.getBackground();
			if (bgBean != null) {
				width = bgBean.getWidth(skinFilesPath);
			}
		}

		return width;
	}

	public int getBackgroundHeight(String partName, File skinFilesPath) {
		int height = -1;
		PartBean part = getPartByName(partName);
		if (part != null) {
			ImagePositionBean bgBean = part.getBackground();
			if (bgBean != null) {
				height = bgBean.getHeight(skinFilesPath);
			}
		}

		return height;
	}

	public File getButtonImage(String partName, String buttonName) {
		File buttonFile = null;
		ImagePositionBean button = getButtonByName(partName, buttonName);
		if (button != null) {
			buttonFile = button.getImageLocation();
		}

		return buttonFile;
	}

	public Point getButtonPosition(String partName, String buttonName) {
		Point buttonPos = null;
		ImagePositionBean button = getButtonByName(partName, buttonName);
		if (button != null) {
			buttonPos = new Point(Integer.parseInt(button.getXPos()), Integer.parseInt(button.getYPos()));
		}

		return buttonPos;
	}

	public int getButtonWidth(String partName, String buttonName, File skinFilesPath) {
		int width = -1;
		ImagePositionBean button = getButtonByName(partName, buttonName);
		if (button != null) {
			width = button.getWidth(skinFilesPath);
		}

		return width;
	}

	public int getButtonHeight(String partName, String buttonName, File skinFilesPath) {
		int height = -1;
		ImagePositionBean button = getButtonByName(partName, buttonName);
		if (button != null) {
			height = button.getHeight(skinFilesPath);
		}

		return height;
	}

	public Point getDisplayPosition(String partName) {
		Point displayPos = null;

		PartBean bean = getPartByName(partName);
		if (bean != null) {
			RectangleBean dispBean = bean.getDisplay();
			displayPos = new Point(Integer.parseInt(dispBean.getXPos()), Integer.parseInt(dispBean.getYPos()));
		}

		return displayPos;
	}

	public int getDisplayWidth(String partName) {
		int width = -1;

		PartBean bean = getPartByName(partName);
		if (bean != null) {
			RectangleBean dispBean = bean.getDisplay();
			width = Integer.parseInt(dispBean.getWidth());
		}

		return width;
	}

	public int getDisplayHeight(String partName) {
		int height = -1;

		PartBean bean = getPartByName(partName);
		if (bean != null) {
			RectangleBean dispBean = bean.getDisplay();
			height = Integer.parseInt(dispBean.getHeight());
		}

		return height;
	}

	private LayoutBean getLayoutByName(String layoutName) {
		LayoutBean layoutToReturn = null;

		if (layouts != null) {
			Iterator<LayoutBean> it = layouts.iterator();
			while (it.hasNext()) {
				LayoutBean bean = it.next();
				if (bean.getName().equals(layoutName)) {
					layoutToReturn = bean;
					break;
				}
			}
		}

		return layoutToReturn;
	}

	public PartBean getPartByName(String partName) {
		PartBean partToReturn = null;

		Iterator<PartBean> it = parts.iterator();
		while (it.hasNext()) {
			PartBean bean = it.next();
			if (bean.getName().equals(partName)) {
				partToReturn = bean;
				break;
			}
		}

		return partToReturn;
	}

	private ImagePositionBean getButtonByName(String partName, String buttonName) {
		ImagePositionBean buttonToReturn = null;
		PartBean pBean = getPartByName(partName);
		if (pBean != null) {
			Collection<ImagePositionBean> buttons = pBean.getButtons();
			if (buttons != null) {
				for (ImagePositionBean bBean : buttons) {
					if (bBean.getName().equals(buttonName)) {
						buttonToReturn = bBean;
						break;
					}
				}
			}
		}

		return buttonToReturn;
	}

	/**
	 * Retrieves a layout main part, i.e. the first part that contains a display
	 * In future releases, check if it is needed to change this concept of
	 * "main part"
	 * 
	 * @param layoutName
	 *            The layout which main part is to be discovered
	 * 
	 * @return The name of the layout main part
	 */
	public String getMainPartName(String layoutName) {
		String mainPartName = null;
		for (LayoutBean layout : layouts) {
			if (layout.getName().equals(layoutName)) {
				Collection<PartRefBean> allRefs = layout.getPartRefs();
				for (PartRefBean partRef : allRefs) {
					String partName = partRef.getPartName();
					for (PartBean aPart : parts) {
						String aPartName = aPart.getName();
						if ((aPartName.equals(partName)) && (aPart.getDisplay() != null)) {
							mainPartName = aPartName;
							break;
						}
					}
					if (mainPartName != null) {
						break;
					}
				}
			}
			if (mainPartName != null) {
				break;
			}
		}

		return mainPartName;
	}

	public boolean partHasBg(String partName) {
		PartBean part = getPartByName(partName);
		ImagePositionBean background = part.getBackground();
		return background != null;
	}
}
