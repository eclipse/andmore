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
package org.eclipse.andmore.android.emulator.ui.controls.skin;

import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.skin.AndroidSkinBean;
import org.eclipse.andmore.android.emulator.core.skin.ISkinKeyXmlTags;
import org.eclipse.andmore.android.emulator.ui.IAndroidUIConstants;
import org.eclipse.andmore.android.emulator.ui.controls.RemoteCLIDisplay;
import org.eclipse.andmore.android.emulator.ui.controls.UIHelper;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * DESCRIPTION: This class implements the layout used to draw skins It is a very
 * specific implementation that applies only to Android Emulator The position
 * and size of the widgets are read from the skin bean provided during layout
 * construction
 *
 * RESPONSIBILITY: - Draw the widgets that composes the skin in the correct
 * position and with the correct size
 *
 * COLABORATORS: None.
 *
 * USAGE: Use of this class is restricted to the classes in this package
 */
class AndroidSkinLayout extends Layout {
	/**
	 * Object that holds information from the skin.xml file of the skin being
	 * currently used
	 */
	private final AndroidSkinBean skin;

	/**
	 * Reference to the CLI display that will be placed by this layout
	 */
	private RemoteCLIDisplay cliDisplayChild = null;

	/**
	 * Reference to the main display that will be placed by this layout
	 */
	private SWTRemoteDisplay mainDisplayChild = null;

	/**
	 * Flag that indicates if the skin contains an open external display
	 * placeholder
	 */
	private final boolean openExternalDisplayAvailable;

	/**
	 * Flag that indicates if the skin contains an external display placeholder
	 */
	private final boolean externalDisplayAvailable;

	/**
	 * Flag that indicates that the flip is supported. This flag has nothing to
	 * do with slide
	 */
	private final boolean isFlipSupported;

	/**
	 * Creates a new AndroidSkinLayout object. As the image dimensions are not
	 * provided with this constructor, it is not possible to have automatic zoom
	 * factor calculation. Using this constructor will set the initial zoom
	 * policy to "100%"
	 *
	 * @param skin
	 *            An skin bean containing the parameters used to place the
	 *            widgets
	 * @param isFlipSupported
	 *            Flag that indicates if flip (not slide) is supported
	 */
	AndroidSkinLayout(AndroidSkinBean skin, boolean isFlipSupported) {
		this.skin = skin;

		this.isFlipSupported = isFlipSupported;
		openExternalDisplayAvailable = skin.isOpenExternalDisplayAvailable();
		externalDisplayAvailable = skin.isExternalDisplayAvailable();
	}

	/**
	 * @see org.eclipse.swt.widgets.Layout#computeSize(Composite, int, int,
	 *      boolean)
	 */
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if (!(composite instanceof SkinComposite)) {
			throw new IllegalArgumentException();
		}

		Point size;

		// Retrieve needed data from composite
		IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(composite);
		boolean flipSlideClosed = false;
		if (instance != null) {
			// flipSlideClosed = instance.isFlipSlideClosed();
			flipSlideClosed = false;
		}
		double zoomFactor = ((SkinComposite) composite).getZoomFactor();

		if (externalDisplayAvailable) {
			if (openExternalDisplayAvailable) {
				if (!flipSlideClosed) {
					// Scenario 1:
					// A) Available displays: INTERNAL, OPEN EXTERNAL, EXTERNAL
					// B) Flip is open
					// C) Displays being showed: INTERNAL, OPEN EXTERNAL
					size = allAvailable(zoomFactor);
				} else {
					// Scenario 2:
					// A) Available displays: INTERNAL, OPEN EXTERNAL, EXTERNAL
					// B) Flip is closed
					// C) Display being showed: EXTERNAL
					size = flipClosed(zoomFactor);
				}
			} else {
				if (!flipSlideClosed) {
					// Scenario 3:
					// A) Available displays: INTERNAL, EXTERNAL
					// B) Flip is opened
					// C) Display being showed: INTERNAL
					size = openExternalUnavailable(zoomFactor);
				} else {
					// Scenario 4:
					// A) Available displays: INTERNAL, EXTERNAL
					// B) Flip is closed
					// C) Display being showed: EXTERNAL
					size = openExternalUnavailableAndFlipClosed(zoomFactor);
				}
			}
		} else {
			// Scenario 5:
			// A) Available display: INTERNAL
			// B) Flip is opened or closed
			// C) Display being showed: INTERNAL
			size = onlyInternal(zoomFactor);
		}

		return size;
	}

	/**
	 * This method is called by computeSize when all displays are available and
	 * the flip is opened
	 * 
	 * @param zoomFactor
	 *            The zoom factor used to calculate the size
	 * 
	 * @return The size
	 */
	private Point allAvailable(double zoomFactor) {
		Point size;

		int x1 = skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_X);
		int y1 = Math.min(skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_Y),
				skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_Y));
		int x2 = skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_X)
				+ skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_WIDTH);
		int y2 = Math.max(
				skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_Y)
						+ skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT),
				skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_Y)
						+ skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_HEIGHT));

		size = new Point((int) ((x2 - x1) * zoomFactor), (int) ((y2 - y1) * zoomFactor));

		return size;
	}

	/**
	 * This method is called by computeSize when all displays are available and
	 * the flip is closed
	 * 
	 * @param zoomFactor
	 *            The zoom factor used to calculate the size
	 * 
	 * @return The size
	 */
	private Point flipClosed(double zoomFactor) {
		Point size = new Point(
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_WIDTH) * zoomFactor),
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_HEIGHT) * zoomFactor));

		return size;
	}

	/**
	 * This method is called by computeSize when all displays but open external
	 * display are available and the flip is opened
	 * 
	 * @param zoomFactor
	 *            The zoom factor used to calculate the size
	 * 
	 * @return The size
	 */
	private Point openExternalUnavailable(double zoomFactor) {
		Point size = new Point(
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH) * zoomFactor),
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT) * zoomFactor));

		return size;
	}

	/**
	 * This method is called by computeSize when all displays but open external
	 * display are available and the flip is closed
	 * 
	 * @param zoomFactor
	 *            The zoom factor used to calculate the size
	 * 
	 * @return The size
	 */
	private Point openExternalUnavailableAndFlipClosed(double zoomFactor) {
		Point size = new Point(
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_WIDTH) * zoomFactor),
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_HEIGHT) * zoomFactor));

		return size;
	}

	/**
	 * This method is called by computeSize when only the internal display is
	 * available
	 * 
	 * @param zoomFactor
	 *            The zoom factor used to calculate the size
	 * 
	 * @return The size
	 */
	private Point onlyInternal(double zoomFactor) {
		Point size = new Point(
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH) * zoomFactor),
				(int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT) * zoomFactor));

		return size;
	}

	/**
	 * @see org.eclipse.swt.widgets.Layout#layout(Composite, boolean)
	 */
	@Override
	protected void layout(Composite composite, boolean flushCache) {
		if (!(composite instanceof SkinComposite)) {
			// If this composite is not a SkinComposite, no layout should be
			// done
			return;
		}

		boolean canProceed = true;

		if (flushCache || (mainDisplayChild == null) || (cliDisplayChild == null)) {
			Control[] children = composite.getChildren();
			canProceed = checkChidren(children);
		}

		if (canProceed) {
			// Retrieve needed data from composite
			SkinComposite skinComposite = (SkinComposite) composite;
			IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(composite);
			boolean flipSlideClosed = false;
			if (instance != null) {
				// flipSlideClosed = instance.isFlipSlideClosed();
				flipSlideClosed = false;
			}

			skinComposite.recalculateZoomFactor();
			skinComposite.updateDisplayRectangle();

			double zoomFactor = skinComposite.getZoomFactor();
			Rectangle displayRectangle = skinComposite.getDisplayRectangle();

			// Handling main display case
			if (mainDisplayChild != null) {
				layoutMainDisplay(zoomFactor, displayRectangle);

				if ((isFlipSupported) && (flipSlideClosed)) {
					// On phones that support flip, the main display is hidden
					// when the flip is closed
					mainDisplayChild.setVisible(false);
				} else {
					mainDisplayChild.setVisible(true);
				}
			}

			// Handling CLI display case
			if (cliDisplayChild != null) {
				layoutCliDisplay(zoomFactor, displayRectangle, flipSlideClosed);

				if (((externalDisplayAvailable) && (!flipSlideClosed))
						|| ((openExternalDisplayAvailable) && (flipSlideClosed))) {
					// The CLI display is shown in 2 situations:
					// 1. When the flip is closed and there is information about
					// external display
					// 2. When the flip is opened and there is information about
					// open external display
					cliDisplayChild.setVisible(true);
				} else {
					cliDisplayChild.setVisible(false);
				}
			}
		}

	}

	/**
	 * Checks if the composite children are as expected by the layout It is
	 * needed to check if the composite's children are, at most: 1. One main
	 * display 2. One CLI display
	 *
	 * @param children
	 *            Array of composite children to check
	 *
	 * @return true if children are as expected; false otherwise
	 */
	private boolean checkChidren(Control[] children) {

		RemoteCLIDisplay cliDisplayInstance = null;
		SWTRemoteDisplay mainDisplayInstance = null;
		boolean childrenOk = true;

		// We need to check if the composite's children are, at most:
		//
		// 1. One main display
		// 2. One CLI display
		for (Control child : children) {
			if (child instanceof SWTRemoteDisplay) {
				if (mainDisplayInstance == null) {
					mainDisplayInstance = (SWTRemoteDisplay) child;
					mainDisplayChild = mainDisplayInstance;
				} else {
					childrenOk = false;

					break;
				}
			} else if (child instanceof RemoteCLIDisplay) {
				if (cliDisplayInstance == null) {
					cliDisplayInstance = (RemoteCLIDisplay) child;
					cliDisplayChild = cliDisplayInstance;
				} else {
					childrenOk = false;

					break;
				}
			} else {
				childrenOk = false;

				break;
			}
		}

		return childrenOk;
	}

	/**
	 * Performs the layout for main display
	 * 
	 * @param zoomFactor
	 *            The zoom factor to use when dimensioning the main display.
	 * @param displayRectangle
	 *            The area of skin that is being shown at the view. It can be
	 *            only a part of the skin if the view client area is smaller
	 *            than the skin size. It is used to calculate translation.
	 */
	private void layoutMainDisplay(double zoomFactor, Rectangle displayRectangle) {
		// Computes main display position
		int x = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_X) * zoomFactor)
				- displayRectangle.x;
		int y = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_Y) * zoomFactor)
				- displayRectangle.y;
		int width = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH) * zoomFactor * skin
				.getEmbeddedViewScale());
		int height = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT) * zoomFactor * skin
				.getEmbeddedViewScale());

		// Sets main display size and position
		mainDisplayChild.setZoomFactor(zoomFactor * skin.getEmbeddedViewScale());
		mainDisplayChild.setBounds(x, y, width, height);
	}

	/**
	 * Performs the layout for CLI display
	 * 
	 * @param zoomFactor
	 *            The zoom factor to use when dimensioning the main display
	 * @param displayRectangle
	 *            The area of skin that is being shown at the view. It can be
	 *            only a part of the skin if the view client area is smaller
	 *            than the skin size. It is used to calculate translation.
	 * @param isFlipSlideClosed
	 *            True if the flip or slide is currently closed. False
	 *            otherwise.
	 */
	private void layoutCliDisplay(double zoomFactor, Rectangle displayRectangle, boolean flipSlideClosed) {
		// Computes CLI display position
		int x = 0;
		int y = 0;
		int width = 0;
		int height = 0;

		if (!flipSlideClosed && openExternalDisplayAvailable) {
			x = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_X) * zoomFactor)
					- displayRectangle.x;
			y = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_Y) * zoomFactor)
					- displayRectangle.y;
			width = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_WIDTH) * zoomFactor * IAndroidUIConstants.DISPLAY_TO_SKIN_RATIO);
			height = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_HEIGHT) * zoomFactor * IAndroidUIConstants.DISPLAY_TO_SKIN_RATIO);
		} else if (flipSlideClosed && externalDisplayAvailable) {
			x = (int) ((skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_X) * zoomFactor) - displayRectangle.x);
			y = (int) ((skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_Y) * zoomFactor) - displayRectangle.y);
			width = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_WIDTH) * zoomFactor * IAndroidUIConstants.DISPLAY_TO_SKIN_RATIO);
			height = (int) (skin.getSkinPropertyValue(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_HEIGHT) * zoomFactor * IAndroidUIConstants.DISPLAY_TO_SKIN_RATIO);
		}

		// Sets cli display size
		cliDisplayChild.setZoomFactor(zoomFactor * IAndroidUIConstants.DISPLAY_TO_SKIN_RATIO);
		cliDisplayChild.setBounds(x, y, width, height);
	}

	void dispose() {
		cliDisplayChild = null;
		mainDisplayChild = null;
	}
}
