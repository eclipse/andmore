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

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Collection;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IInputLogic;
import org.eclipse.andmore.android.emulator.core.skin.AndroidPressKey;
import org.eclipse.andmore.android.emulator.core.skin.AndroidSkinBean;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidKey;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.controls.UIHelper;
import org.eclipse.andmore.android.emulator.ui.handlers.IHandlerConstants;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.PlatformUI;

/**
 * DESCRIPTION: This class implements the UI part of the skin
 * 
 * RESPONSIBILITY: - Provide the skin image with correct layout - Provide means
 * for the user to send events to the emulator through the phone emulated
 * keyboard - Provide means for the user to change the zoom and window scrolling
 * properties
 * 
 * COLABORATORS: None.
 * 
 * USAGE: Create an instance of this class every time an phone instance is to be
 * displayed Call the public methods to interact with the skin.
 */
public class SkinComposite extends Composite implements IAndroidComposite {
	// Constants for defining interval between keystrokes
	// when using long mouse click
	public static final int FIRST_REFRESH_DELAY_MS = 300;

	public static final int REFRESH_DELAY_PERIOD_MS = 100;

	// Minimum value to be used as zoom factor. This is necessary to avoid
	// divisions to zero
	private static final double MINIMUM_ZOOM_FACTOR = 0.0001;

	// The step increased or decreased from the zoom factor using mouse wheel
	private static final double ZOOM_STEP = 0.5;

	/**
	 * A rectangle that represents the part of the currentSkin image that fits
	 * into the view/screen. It must fit inside the currentSkinImage borders (0,
	 * 0, current skin image width, current skin image height)
	 */
	private final Rectangle displayRectangle = new Rectangle(0, 0, 0, 0);

	/**
	 * The skin elements provider
	 */
	private IAndroidSkin skin;

	/**
	 * The image that is currently drawn at screen. It is one image provided by
	 * the skin that is scaled by the current zoom factor
	 */
	private Image currentSkinImage;

	/**
	 * The key that mouse is over at the current moment
	 */
	private IAndroidKey currentKey;

	/**
	 * The flag indicating that Ctrl key is pressed
	 */
	private boolean ctrlPressed = false;

	/**
	 * SWT key pressed/released events listener
	 */
	private KeyListener keyListener;

	private MouseListener mainDisplayMouseListener;

	private MouseMoveListener mainDisplayMouseMoveListener;

	/**
	 * Flag that indicates if the skin can use the scroll bars to draw itself
	 * bigger than the view client area
	 */
	private boolean scrollBarsUsed = true;

	/**
	 * True if the mouse left button is pressed. False otherwise
	 */
	private boolean isMouseLeftButtonPressed;

	/**
	 * True if the mouse right button is pressed. False otherwise
	 */
	private boolean isMouseRightButtonPressed;

	/**
	 * The zoom factor whose default value is 1.0 (100%)
	 */
	private double zoomFactor = 1.0;

	private double embeddedViewScale = 1.0;

	private IInputLogic androidInput;

	private boolean isMouseMainDisplayLeftButtonPressed;

	IAndroidEmulatorInstance androidInstance;

	/**
	 * Creates a SkinComposite This composite holds the screens in the correct
	 * positions and maps the keys
	 * 
	 * @param parent
	 *            The parent composite in which the UI part of the instance
	 *            shall be created
	 * @param androidSkin
	 *            The skin object that contain data for getting skin information
	 */
	public SkinComposite(Composite parent, IAndroidSkin androidSkin, IAndroidEmulatorInstance instance) {
		super(parent, SWT.BACKGROUND | SWT.H_SCROLL | SWT.V_SCROLL);
		skin = androidSkin;
		androidInstance = instance;

		// Add listeners
		addListeners();
		createListenersForMainDisplay();

		setToolTipText(null);

		androidInput = instance.getInputLogic();

		// Init the scroll bars
		initScrollBars();

		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			hideEmulatorWindow();
		}
	}

	private void hideEmulatorWindow() {
		int port = AndroidLogicUtils.getEmulatorPort(DDMSFacade.getSerialNumberByName(androidInstance.getName()));
		long windowHandle = NativeUIUtils.getWindowHandle(androidInstance.getName(), port);
		androidInstance.setWindowHandle(windowHandle);
		NativeUIUtils.hideWindow(windowHandle);
	}

	/**
	 * Add listeners to the skin composite
	 */
	private void addListeners() {

		addPaintListener(new PaintListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.PaintListener#paintControl(org.eclipse
			 * .swt.events.PaintEvent)
			 */
			@Override
			public void paintControl(PaintEvent e) {
				// This listener is invoked when a regular SWT redraw is
				// invoked. In this case, no keys
				// have changed. That's why we pass "null" as parameter
				drawSkin(e.gc, null);
			}
		});

		addMouseListener(new MouseAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.MouseAdapter#mouseUp(org.eclipse.swt.events
			 * .MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) {
					isMouseLeftButtonPressed = false;

				} else if (e.button == 3) {
					isMouseRightButtonPressed = false;
				}

				// Handle left button mouse up event
				if (e.button == 1) {
					cancelMouseSelection();
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt
			 * .events.MouseEvent)
			 */
			@Override
			public void mouseDown(MouseEvent e) {
				setFocus();
				if (e.button == 1) {
					isMouseLeftButtonPressed = true;
				} else if (e.button == 3) {
					isMouseRightButtonPressed = true;
				}

				if (currentKey != null) {
					ImageData mergedImage = getKeyImageData(currentKey, false);
					setSkinImage(mergedImage, currentKey, true);

					// Handle left button mouse down event
					if ((e.button == 1) && (!isMouseRightButtonPressed) && (currentKey != null)) {
						androidInput.sendClick(currentKey.getKeysym(), true);
					}

					// Handle right button mouse down event
					else if (e.button == 3) {
						cancelMouseSelection();
					}

				}
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse
			 * .swt.events.MouseEvent)
			 */
			@Override
			public void mouseMove(MouseEvent e) {
				int posX = (int) ((e.x + displayRectangle.x) / zoomFactor);
				int posY = (int) ((e.y + displayRectangle.y) / zoomFactor);
				IAndroidKey keyData = getSkinKey(posX, posY);

				if ((isMouseLeftButtonPressed) && (keyData != currentKey)) {
					cancelMouseSelection();
				}

				if (!isMouseLeftButtonPressed && (currentKey != keyData)) {
					changeCurrentKey(keyData);
				}
			}
		});

		// listener to change the zoom factor using Ctrl + Mouse Wheel
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent event) {
				if (ctrlPressed) {

					// the floor and ceil are required if "fits to window" was
					// checked.
					double roundedZoomFactor = Math.floor(zoomFactor / ZOOM_STEP) * ZOOM_STEP;

					if ((event.count > 0) && (roundedZoomFactor < IHandlerConstants.MAXIMUM_ZOOM)) {
						// increase zoom factor
						setZoomFactor(roundedZoomFactor + ZOOM_STEP);
						applyZoomFactor();
					}

					else if ((event.count < 0) && (roundedZoomFactor > IHandlerConstants.MINIMUM_ZOOM)) {
						// decrease zoom factor
						setZoomFactor(roundedZoomFactor - ZOOM_STEP);
						applyZoomFactor();
					}

				}
			}
		});

		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(MouseEvent mouseevent) {
				changeCurrentKey(null);
			}
		});

		addControlListener(new ControlAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse
			 * .swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(ControlEvent event) {
				if (scrollBarsUsed) {
					synchronizeScrollBars();
				} else {
					ImageData imageData = getImageData(false, false);
					setSkinImage(imageData, null, false);
				}
			}
		});

	}

	@Override
	public void applyLayout(String layoutName) {
		// Populate the attributes with information from skin
		AndroidSkinBean skinBean = null;

		try {
			skinBean = skin.getSkinBean(layoutName);
		} catch (SkinException e) {
			error("The skin data could not be retrieved from skin files. Cause: " + e.getMessage());
			EclipseUtils.showErrorDialog(e);
		}

		// Create layout and set it to composite
		if (skinBean != null) {
			// When changing to a new layout, the key may move to another
			// position
			// It does not make sense to keep the old key object
			currentKey = null;

			// Change the background color to the one that applies to the layout
			// being set
			RGB color = skin.getBackgroundColor(layoutName);
			setBackground(new Color(PlatformUI.getWorkbench().getDisplay(), color));

			Layout prevLayout = getLayout();
			if (prevLayout instanceof AndroidSkinLayout) {
				((AndroidSkinLayout) prevLayout).dispose();
			}

			AndroidSkinLayout androidLayout = new AndroidSkinLayout(skinBean, skin.isFlipSupported());
			setLayout(androidLayout);

			embeddedViewScale = skinBean.getEmbeddedViewScale();

			layout();
			redraw();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		if (androidInput != null) {
			androidInput.dispose();
		}

		if (currentSkinImage != null) {
			currentSkinImage.dispose();
		}

		Layout layout = getLayout();
		if (layout instanceof AndroidSkinLayout) {
			((AndroidSkinLayout) layout).dispose();
		}

		skin = null;
		currentKey = null;
		keyListener = null;
		mainDisplayMouseListener = null;
		mainDisplayMouseMoveListener = null;

		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			long hnd = androidInstance.getWindowHandle();
			if (hnd > 0) {
				NativeUIUtils.showWindow(hnd);
				NativeUIUtils.restoreWindow(hnd);
			}

			// Force update on redrawing
			androidInstance.setWindowHandle(0);
		}

		super.dispose();
	}

	/**
	 * Sets the zoom factor to use in the instance
	 */
	@Override
	public void applyZoomFactor() {
		if (getZoomFactor() == 0) {
			scrollBarsUsed = false;
			getVerticalBar().setEnabled(false);
			getHorizontalBar().setEnabled(false);
		} else {
			scrollBarsUsed = true;
			getVerticalBar().setEnabled(true);
			getHorizontalBar().setEnabled(true);
		}

		// Resets translation
		displayRectangle.x = 0;
		displayRectangle.y = 0;

		redrawReleasedImage();

	}

	/**
	 * Sets the flip/slide status of the phone
	 */
	private void redrawReleasedImage() {
		if (currentSkinImage != null) {
			ImageData imageData = getImageData(false, false);
			setSkinImage(imageData, null, false);
			layout();
			redraw();
		}

		if (scrollBarsUsed) {
			synchronizeScrollBars();
		}
	}

	/**
	 * Performs the skin draw operation.
	 * 
	 * @param gcUsedToDraw
	 *            The gc object associated with the skin composite that is used
	 *            to draw the images
	 */
	private void drawSkin(GC gcUsedToDraw, IAndroidKey changedKey) {
		if (currentSkinImage == null) {
			IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(this);
			ImageData initialSkinImage = getImageData(false, false);
			setSkinImage(initialSkinImage, null, false);
			applyLayout(instance.getCurrentLayout());

			if (scrollBarsUsed) {
				synchronizeScrollBars();
			}
		}

		if (displayRectangle != null) {
			int srcXPos, srcYPos, srcWidth, srcHeight;
			int destXPos, destYPos, destWidth, destHeight;
			if (changedKey == null) {
				srcXPos = displayRectangle.x;
				srcYPos = displayRectangle.y;
				srcWidth = displayRectangle.width;
				srcHeight = displayRectangle.height;
				destXPos = 0;
				destYPos = 0;
				destWidth = Math.min(currentSkinImage.getImageData().width, displayRectangle.width);
				destHeight = Math.min(currentSkinImage.getImageData().height, displayRectangle.height);
			} else {
				srcXPos = ((int) (changedKey.getKeyArea().x > 0 ? changedKey.getKeyArea().x * zoomFactor : 0));
				srcYPos = ((int) (changedKey.getKeyArea().y > 0 ? changedKey.getKeyArea().y * zoomFactor : 0));
				srcWidth = ((int) (changedKey.getKeyArea().width * zoomFactor));
				srcHeight = ((int) (changedKey.getKeyArea().height * zoomFactor));
				destXPos = srcXPos - displayRectangle.x;
				destYPos = srcYPos - displayRectangle.y;
				destWidth = srcWidth;
				destHeight = srcHeight;
			}

			gcUsedToDraw.drawImage(currentSkinImage, srcXPos, srcYPos, srcWidth, srcHeight, destXPos, destYPos,
					destWidth, destHeight);
		}
	}

	/**
	 * Loads a new screen image to the currentSkin attribute. This action
	 * updates the skin image that is drawn as skin
	 * 
	 * @param imageToSet
	 *            The new skin pixel data, as retrieved from the skin plugin
	 * @param changedKey
	 *            The key that has changed, if any. If one if provided, only the
	 *            key area should be redrawn. Can be <code>null</code>.
	 * @param forceDraw
	 *            true if a draw is needed after setting the new image; false if
	 *            replacing skin image only will be performed.
	 */
	private void setSkinImage(ImageData imageToSet, IAndroidKey changedKey, boolean forceDraw) {

		recalculateZoomFactor();

		if (imageToSet != null) {
			if (currentSkinImage != null) {
				currentSkinImage.dispose();
			}

			// Scales the chosen image and sets to currentSkin attribute
			//
			// NOTE: width and height cannot be equal to MINIMUM_ZOOM_FACTOR,
			// because this
			// will raise an IllegalArgumentException when constructing the
			// Image object
			int width = (zoomFactor == MINIMUM_ZOOM_FACTOR ? 1 : (int) (imageToSet.width * zoomFactor));
			int height = (zoomFactor == MINIMUM_ZOOM_FACTOR ? 1 : (int) (imageToSet.height * zoomFactor));
			currentSkinImage = new Image(getDisplay(), imageToSet.scaledTo(width, height));

			// It only makes sense to reset the translation if the skin image is
			// really being changed
			// It will happen if we set a image data without specifying a
			// changed key
			if (changedKey == null) {
				displayRectangle.x = 0;
				displayRectangle.y = 0;
			}

			if (forceDraw) {
				layout();

				GC gc = new GC(this);
				drawSkin(gc, changedKey);
				gc.dispose();
			}
		} else {
			info("It was requested to set a skin image that was null. Operation aborted.");
		}
	}

	/**
	 * This method is responsible to set the scroll bar attributes so that they
	 * reflect the size of the current image at the current zoom factor
	 */
	private void synchronizeScrollBars() {
		// Syncronizing only makes sense if there is a skin being drawn
		if (currentSkinImage != null) {

			// Retrieves the current image and client area sizes
			Rectangle imageBound = currentSkinImage.getBounds();
			int cw = getClientArea().width;
			int ch = getClientArea().height;

			// Updates horizontal scroll bar attributes
			ScrollBar horizontal = getHorizontalBar();
			horizontal.setIncrement((cw / 100));
			horizontal.setPageIncrement((cw / 2));
			horizontal.setMaximum(imageBound.width);
			horizontal.setThumb(cw);
			horizontal.setSelection(displayRectangle.x);

			// Updates vertical scroll bar attributes
			ScrollBar vertical = getVerticalBar();
			vertical.setIncrement((ch / 100));
			vertical.setPageIncrement((ch / 2));
			vertical.setMaximum(imageBound.height);
			vertical.setThumb(ch);
			vertical.setSelection(displayRectangle.y);

			if (horizontal.getMaximum() > cw) // Image is wider than client area
			{
				horizontal.setEnabled(true);
			} else {
				horizontal.setEnabled(false);
			}

			if (vertical.getMaximum() > ch) // Image is wider than client area
			{
				vertical.setEnabled(true);
			} else {
				vertical.setEnabled(false);
			}

		}
	}

	/**
	 * Initialize the scroll bars This include: a) setting the initial enabled
	 * state b) adding the necessary listeners
	 */
	private void initScrollBars() {

		ScrollBar horizontal = getHorizontalBar();
		horizontal.setEnabled(false);
		horizontal.addSelectionListener(new SelectionAdapter() {
			/**
			 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Updates the translation
				displayRectangle.x = ((ScrollBar) event.widget).getSelection();

				// Update the UI
				layout();
				redraw();
			}
		});

		ScrollBar vertical = getVerticalBar();
		vertical.setEnabled(false);
		vertical.addSelectionListener(new SelectionAdapter() {
			/**
			 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Updates the translation
				displayRectangle.y = ((ScrollBar) event.widget).getSelection();

				// Update the UI
				layout();
				redraw();
			}
		});

		debug("Initialized scroll bars");
	}

	/**
	 * This method retrieves the key that is placed at the given x,y
	 * coordinates, considering the flip status
	 * 
	 * @param x
	 *            The X coordinate to use at key lookup
	 * @param y
	 *            The Y coordinate to use at key lookup
	 * 
	 * @return The key placed at the given coordinate, or null if none is found
	 */
	private IAndroidKey getSkinKey(int x, int y) {
		IAndroidKey keyToReturn = null;
		IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(this);

		Collection<IAndroidKey> keyAreas = skin.getKeyDataCollection(instance.getCurrentLayout());
		if (keyAreas != null) {
			for (IAndroidKey key : keyAreas) {
				if (key.isInsideKey(x, y)) {
					if (key instanceof AndroidPressKey) {
						AndroidPressKey defaultKeyData = (AndroidPressKey) key;

						// if
						// (!defaultKeyData.isFlipSlideValid(instance.isFlipSlideClosed()))
						if (!defaultKeyData.isFlipSlideValid(false)) {
							continue;
						}

					}

					keyToReturn = key;
					break;
				}
			}
		}

		return keyToReturn;
	}

	/**
	 * Retrieves an image data. If it has never been used at the current
	 * session, loads it from skin. Released image is retrieved if both
	 * parameters are false.
	 * 
	 * @param isPressed
	 *            true if the image to be retrieved is the pressed image
	 * @param isEnter
	 *            true if the image to be retrieved is the enter image. It only
	 *            has effect if isPressed == false
	 * 
	 * @return An image data containing the desired image pixels
	 */
	private ImageData getImageData(boolean isPressed, boolean isEnter) {

		ImageData imageData = null;

		IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(this);

		try {
			if (isPressed) {
				imageData = skin.getPressedImageData(instance.getCurrentLayout());

			} else {
				if (isEnter) {
					imageData = skin.getEnterImageData(instance.getCurrentLayout());

				} else {
					imageData = skin.getReleasedImageData(instance.getCurrentLayout());

				}
			}
		} catch (SkinException e) {
			error("The image requested from skin could not be retrieved. isPressed=" + isPressed + "; message="
					+ e.getMessage());
			EclipseUtils.showErrorDialog(e);
			error("The skin could not provide an important resource. Stopping the instance");
			try {
				instance.stop(true);
			} catch (InstanceStopException e1) {
				error("Error while running service for stopping virtual machine");
				EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_General_CannotRunStopService);
			}
		}

		return imageData;
	}

	/**
	 * Builds an image data that is based on the released image but has the
	 * pressed/enter key area painted with data from the pressed/enter image
	 * 
	 * @param key
	 *            The pressed key
	 * @param isEnter
	 *            Whether the image being retrieved will be used for enter or
	 *            pressed
	 * 
	 * @return An image data built the way described at method description
	 */
	private ImageData getKeyImageData(IAndroidKey key, boolean isEnter) {

		ImageData resultingImage;
		ImageData releasedImage = getImageData(false, false);
		ImageData keyImage = isEnter ? getImageData(false, true) : getImageData(true, false);

		resultingImage = (ImageData) releasedImage.clone();

		Rectangle keyArea = key.getKeyArea();
		int resultingImageSize = resultingImage.width * keyArea.height;
		int[] keyPixelBuffer = new int[resultingImageSize];

		int startY = keyArea.y < 0 ? 0 : keyArea.y;

		keyImage.getPixels(0, startY, resultingImageSize, keyPixelBuffer, 0);

		for (int line = 0; line < keyArea.height; line++) {
			int pos = (line * resultingImage.width) + Math.abs(keyArea.x);
			int startX = Math.abs(keyArea.x);
			startY = (keyArea.y < 0 ? 0 : keyArea.y) + line;
			if (startY < 0) {
				continue;
			}

			int putWidth = keyArea.x < 0 ? keyArea.width - startX : keyArea.width;
			resultingImage.setPixels(startX, startY, putWidth, keyPixelBuffer, pos);
		}

		return resultingImage;
	}

	/**
	 * This method is called when a mouse selection needs to be canceled.
	 * Pressing the right button, releasing the left button or leaving the key
	 * area are examples of typical conditions.
	 */
	private void cancelMouseSelection() {
		// If the mouse timer is different from null, that means that a key is
		// pressed
		// This check is important so that event messages are not sent by
		// mistake

		if (currentKey != null) {

			ImageData newImageData = getImageData(false, true);
			setSkinImage(newImageData, currentKey, true);
			androidInput.sendClick(currentKey.getKeysym(), false);
		}
	}

	private void changeCurrentKey(IAndroidKey newKey) {
		// The following actions are executed only if the key has changed since
		// the last
		// time a mouse move event has happened.
		ImageData newImage;

		if (currentKey != null) {
			// If currentKey is different from null, we know that the mouse
			// cursor has
			// left the area defined by currentKey. That is because from the
			// previous
			// if clause we know that the key has changed, and currentKey
			// attribute
			// state is out-dated until we reach the "currentKey = keyData"
			// statement.
			// In this case, we must draw the RELEASED image version of the key
			// at the
			// key location
			newImage = getImageData(false, false);
			setSkinImage(newImage, currentKey, true);
			setToolTipText(null);
		}

		if (newKey != null) {
			// If keyData is different from null, we know that the mouse cursor
			// has
			// entered the area defined by keyData.
			// In this case, we must draw the ENTER image version of the key at
			// the
			// key location
			newImage = getKeyImageData(newKey, true);
			setSkinImage(newImage, newKey, true);
			setToolTipText(newKey.getToolTip());
		}
		currentKey = newKey;
	}

	/**
	 * Retrieves the display rectangle defined at the current moment.
	 * 
	 * @return The display rectangle
	 */
	Rectangle getDisplayRectangle() {
		return displayRectangle;
	}

	/**
	 * Updates the size and location of the display rectangle, based on the
	 * current attributes of the skin (such as skin size and zoom factor) and
	 * view size
	 */
	void updateDisplayRectangle() {
		// Updating the display rectangle only makes sense if we have a skin
		// being drawn
		if (currentSkinImage != null) {

			// Collect the variables used in computation
			//
			// - clientAreaWidth, clientAreaHeight: dimensions of the view,
			// measured from
			// the upper left corner point (0,0) to the lower right corner point
			// (width, height)
			// - currentSkinWidth, currentSkinHeight: dimensions of the skin
			// picture, already scaled
			// by the zoom factor
			int clientAreaWidth = getClientArea().width;
			int clientAreaHeight = getClientArea().height;
			int currentSkinHeight = currentSkinImage.getImageData().height;
			int currentSkinWidth = currentSkinImage.getImageData().width;

			// Updates the display rectangle y and height
			//
			// FIRST STEP: determine the position of the rectangle's y
			// coordinate.
			// - It starts by calculating if there is any blank area at the
			// bottom
			// of the view.
			// - If there is blank space (blankY > 0) then calculate which
			// point,
			// if set as the display rectangle's y coordinate, would make the
			// blank
			// space to disappear. Store this as a candidate Y.
			// - Check if the candidate Y is valid. If not valid (candidateY <
			// 0)
			// then use the image origin as the final y coordinate
			//
			// SECOND STEP: determine the width dimension of the rectangle
			// - It starts by calculating which would be the coordinate of the
			// lower point, assuming that the image is big enough to contain
			// that
			// coordinate. That value (vEdge) is the sum of the Y coordinate and
			// the view height.
			// - If vEdge is bigger than the current view height, that means
			// that the image will occupy part of the view. Itis necessary to
			// make the rectangle fit in the skin image by making it smaller in
			// height than the view height itself.
			// - If vEdge is smaller than the current view height, that means
			// that
			// the image will not fit in the view. The solution is to make the
			// display rectangle height the same size as the view height. In
			// this
			// second case, the display rectangle will fit in the view height,
			// but
			// will not be bigger than the skin image height itself
			int blankY = clientAreaHeight - (currentSkinHeight - displayRectangle.y);
			if (blankY > 0) {
				int candidateY = displayRectangle.y - blankY;
				if (candidateY > 0) {
					displayRectangle.y = candidateY;
				} else {
					displayRectangle.y = 0;
				}
			}
			int vEdge = displayRectangle.y + clientAreaHeight;
			if (vEdge > currentSkinHeight) {
				displayRectangle.height = currentSkinHeight - displayRectangle.y;
			} else {
				displayRectangle.height = clientAreaHeight;
			}

			// Updates the display rectangle x and width
			// NOTE: a similar logic to the previous one was applied in this
			// case
			int blankX = clientAreaWidth - (currentSkinWidth - displayRectangle.x);
			if (blankX > 0) {
				int candidateX = displayRectangle.x - blankX;
				if (candidateX > 0) {
					displayRectangle.x = candidateX;
				} else {
					displayRectangle.x = 0;
				}
			}
			int hEdge = displayRectangle.x + clientAreaWidth;
			if (hEdge > currentSkinWidth) {
				displayRectangle.width = currentSkinWidth - displayRectangle.x;
			} else {
				displayRectangle.width = clientAreaWidth;
			}

		}
	}

	/**
	 * Recalculates the zoom factor. This is necessary when the screen or image
	 * dimensions change.
	 */
	void recalculateZoomFactor() {

		if (!scrollBarsUsed) {
			// Compute new zoom factor if the zoom mode is "Fit to Window"
			Rectangle clientArea = getClientArea();
			if ((clientArea.width == 0) || (clientArea.height == 0)) {
				// zoom factor cannot be zero, otherwise an
				// IllegalArgumentException
				// is raised in some SWT methods
				setZoomFactor(MINIMUM_ZOOM_FACTOR);
			} else {
				ImageData currentSkin = getImageData(false, false);
				double widthRatio = (double) (clientArea.width) / currentSkin.width;
				double heightRatio = (double) (clientArea.height) / currentSkin.height;
				setZoomFactor(Math.min(widthRatio, heightRatio));
			}
		}

	}

	/**
	 * Gets the current zoom factor.
	 * 
	 * @return the zoom factor
	 */
	@Override
	public double getZoomFactor() {
		return zoomFactor;
	}

	/**
	 * Checks if the current zoom configuration is "fit to screen";
	 * 
	 * @return
	 */
	@Override
	public boolean isFitToWindowSelected() {
		return !scrollBarsUsed;
	}

	/**
	 * Sets the zoom factor.
	 * 
	 * @param zoom
	 *            the zoom factor
	 */
	@Override
	public void setZoomFactor(double zoom) {
		zoomFactor = zoom;
	}

	/**
	 * Gets the listener that handles SWT key pressing and releasing events.
	 * 
	 * @return the KeyListener object
	 */
	@Override
	public KeyListener getKeyListener() {
		return keyListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.ui.controls.IAndroidComposite#
	 * getMouseListener()
	 */
	@Override
	public MouseListener getMouseListener() {
		return mainDisplayMouseListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.ui.controls.IAndroidComposite#
	 * getMouseMoveListener()
	 */
	@Override
	public MouseMoveListener getMouseMoveListener() {
		return mainDisplayMouseMoveListener;
	}

	private void createListenersForMainDisplay() {
		// create listener to handle keyboard key pressing highlighting the key
		// in the skin.
		keyListener = new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent arg0) {

				int keyCode = arg0.keyCode;

				if (keyCode == SWT.CTRL) {
					ctrlPressed = true;
				} else {
					if (keyCode == SWT.ARROW_DOWN || keyCode == SWT.ARROW_LEFT || keyCode == SWT.ARROW_RIGHT
							|| keyCode == SWT.ARROW_UP) {
						int dpadRotation = skin.getDpadRotation(androidInstance.getCurrentLayout());
						keyCode = getRotatedKeyCode(keyCode, dpadRotation);
					}
					// send message to emulator
					androidInput.sendKey(arg0.character, keyCode, skin.getKeyCodes());
				}

			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				int keyCode = arg0.keyCode;

				if (keyCode == SWT.CTRL) {
					ctrlPressed = false;
				}
			}

		};

		mainDisplayMouseMoveListener = new MouseMoveListener() {
			/**
			 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(MouseEvent)
			 */
			@Override
			public void mouseMove(MouseEvent e) {
				if (isMouseMainDisplayLeftButtonPressed) {
					UIHelper.ajustCoordinates(e, SkinComposite.this);
					androidInput.sendMouseMove((int) (e.x / embeddedViewScale), (int) (e.y / embeddedViewScale));
				}
			}
		};

		mainDisplayMouseListener = new MouseAdapter() {
			/**
			 * @see org.eclipse.swt.events.MouseListener#mouseUp(MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) {
					isMouseMainDisplayLeftButtonPressed = false;

					UIHelper.ajustCoordinates(e, SkinComposite.this);
					androidInput.sendMouseUp((int) (e.x / embeddedViewScale), (int) (e.y / embeddedViewScale));
				}
			}

			/**
			 * @see org.eclipse.swt.events.MouseListener#mouseDown(MouseEvent)
			 */
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					UIHelper.ajustCoordinates(e, SkinComposite.this);
					androidInput.sendMouseDown((int) (e.x / embeddedViewScale), (int) (e.y / embeddedViewScale));

					isMouseMainDisplayLeftButtonPressed = true;
				}
			}
		};
	}

	private int getRotatedKeyCode(int keyCode, int dpadRotation) {
		switch (dpadRotation % 4) {
		case 1:
			switch (keyCode) {
			case SWT.ARROW_DOWN:
				keyCode = SWT.ARROW_RIGHT;
				break;
			case SWT.ARROW_LEFT:
				keyCode = SWT.ARROW_DOWN;
				break;
			case SWT.ARROW_RIGHT:
				keyCode = SWT.ARROW_UP;
				break;
			case SWT.ARROW_UP:
				keyCode = SWT.ARROW_LEFT;
				break;
			}
			break;
		case 2:
			switch (keyCode) {
			case SWT.ARROW_DOWN:
				keyCode = SWT.ARROW_UP;
				break;
			case SWT.ARROW_LEFT:
				keyCode = SWT.ARROW_RIGHT;
				break;
			case SWT.ARROW_RIGHT:
				keyCode = SWT.ARROW_LEFT;
				break;
			case SWT.ARROW_UP:
				keyCode = SWT.ARROW_DOWN;
				break;
			}
			break;
		case 3:
			switch (keyCode) {
			case SWT.ARROW_DOWN:
				keyCode = SWT.ARROW_LEFT;
				break;
			case SWT.ARROW_LEFT:
				keyCode = SWT.ARROW_UP;
				break;
			case SWT.ARROW_RIGHT:
				keyCode = SWT.ARROW_DOWN;
				break;
			case SWT.ARROW_UP:
				keyCode = SWT.ARROW_RIGHT;
				break;
			}
			break;
		default:
			// Does nothing, no rotation needed.
			break;
		}
		return keyCode;
	}
}
