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
package org.eclipse.andmore.android.emulator.ui.controls.maindisplay;

import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IInputLogic;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.skin.android.AndroidSkinTranslator;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.controls.UIHelper;
import org.eclipse.andmore.android.emulator.ui.handlers.IHandlerConstants;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * This class is the composite that holds the main display and it is shown by
 * the Emulator Main Display View.
 */
public class MainDisplayComposite extends Composite implements IAndroidComposite {

	/**
	 * The zoom factor whose default value is 1.0 (100%)
	 */
	private double zoomFactor = 1.0;

	private double fitZoomfactor;

	// Minimum value to be used as zoom factor. This is necessary to avoid
	// divisions to zero
	private static final double MINIMUM_ZOOM_FACTOR = 0.0001;

	private static final double ZOOM_FIT = 0.0;

	/**
	 * The flag indicating that Ctrl key is pressed
	 */
	private boolean ctrlPressed = false;

	/**
	 * SWT key pressed/released events listener.
	 */
	private KeyListener keyListener;

	private MouseListener mouseListener;

	private MouseMoveListener mouseMoveListener;

	private IInputLogic androidInput;

	private boolean isMouseLeftButtonPressed;

	private boolean isFitToWindow;

	private IAndroidEmulatorInstance androidInstance;

	private Properties keyMap;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            composite
	 * @param style
	 *            style
	 * @param baseWidth
	 *            the default main display width
	 * @param baseHeight
	 *            the default main display height
	 */
	public MainDisplayComposite(Composite parent, int style, int baseWidth, int baseHeight,
			IAndroidEmulatorInstance instance) {
		super(parent, style);

		androidInput = instance.getInputLogic();

		androidInstance = instance;

		isMouseLeftButtonPressed = false;

		keyMap = AndroidSkinTranslator.getQwertyKeyMap();

		addListener();

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

		keyListener = null;
		mouseListener = null;
		mouseMoveListener = null;

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
	 * Updates the composite size when zoom is changed.
	 * 
	 * @param zoom
	 *            the zoom factor
	 */
	@Override
	public void setZoomFactor(double zoomFactor) {
		info("Update detached view composite size");
		if (zoomFactor == ZOOM_FIT) {
			isFitToWindow = true;
		} else {
			isFitToWindow = false;
		}
		this.zoomFactor = zoomFactor;
	}

	/**
	 * Gets the zoom factor.
	 * 
	 * @return zoom the zoom factor.
	 */
	@Override
	public double getZoomFactor() {
		return zoomFactor;
	}

	/**
	 * Applies the zoom factor to the components of the composite, updating the
	 * composite size to hold totally the main display.
	 */
	@Override
	public void applyZoomFactor() {

		SWTRemoteDisplay mainDisplay = UIHelper.getRemoteDisplayAssociatedToControl(this);
		IRemoteDisplay.Rotation rotation = mainDisplay.getRotation();

		int baseHeight;
		int baseWidth;

		switch (rotation) {
		case ROTATION_90DEG_COUNTERCLOCKWISE:
			baseHeight = mainDisplay.getScreenWidth();
			baseWidth = mainDisplay.getScreenHeight();
			break;
		default:
			baseHeight = mainDisplay.getScreenHeight();
			baseWidth = mainDisplay.getScreenWidth();

		}

		int width;
		int height;
		if (isFitToWindow) {
			Rectangle clientArea = getParent().getClientArea();
			if ((clientArea.width == 0) || (clientArea.height == 0)) {
				// zoom factor cannot be zero, otherwise an
				// IllegalArgumentException
				// is raised in some SWT methods
				fitZoomfactor = MINIMUM_ZOOM_FACTOR;
			} else {
				double widthRatio = (double) (clientArea.width) / baseWidth;
				double heightRatio = (double) (clientArea.height) / baseHeight;
				fitZoomfactor = Math.min(widthRatio, heightRatio);
			}
			width = new Double(baseWidth * fitZoomfactor).intValue();
			height = new Double(baseHeight * fitZoomfactor).intValue();

			if (mainDisplay != null) {
				mainDisplay.setZoomFactor(fitZoomfactor);
			}
		} else {
			width = new Double(baseWidth * zoomFactor).intValue();
			height = new Double(baseHeight * zoomFactor).intValue();

			if (mainDisplay != null) {
				mainDisplay.setZoomFactor(zoomFactor);
			}
		}

		setSize(width, height);
	}

	/**
	 * Adds listener for SWT events.
	 */
	private void addListener() {
		// add listener to handle keyboard key pressing
		keyListener = new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {

				int keyCode = arg0.keyCode;

				if (keyCode == SWT.CTRL) {
					ctrlPressed = true;
				} else {
					// send message to emulator
					androidInput.sendKey(arg0.character, keyCode, keyMap);
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

		// listener to change the zoom factor using Ctrl + Mouse Wheel
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(MouseEvent event) {
				if (ctrlPressed) {

					if ((event.count > 0) && (zoomFactor < IHandlerConstants.MAXIMUM_ZOOM)) {
						// increase zoom factor
						setZoomFactor(zoomFactor + IHandlerConstants.STEP_ZOOM);
						applyZoomFactor();
					}

					else if ((event.count < 0) && (zoomFactor > IHandlerConstants.MINIMUM_ZOOM)) {
						// decrease zoom factor
						setZoomFactor(zoomFactor - IHandlerConstants.STEP_ZOOM);
						applyZoomFactor();
					}
				}
			}
		});

		mouseListener = new MouseAdapter() {
			/**
			 * @see org.eclipse.swt.events.MouseListener#mouseUp(MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				handleMouseUp(e);
			}

			/**
			 * @see org.eclipse.swt.events.MouseListener#mouseDown(MouseEvent)
			 */
			@Override
			public void mouseDown(MouseEvent e) {
				setFocus();
				handleMouseDown(e);
			}
		};

		mouseMoveListener = new MouseMoveListener() {
			/**
			 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(MouseEvent)
			 */
			@Override
			public void mouseMove(MouseEvent e) {
				handleMouseMove(e);
			}
		};

		getParent().addControlListener(new ControlAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse
			 * .swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(ControlEvent event) {
				if (isFitToWindow) {
					applyZoomFactor();
				}
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.emulator.ui.controls.IAndroidComposite#
	 * applyLayout(java.lang.String)
	 */
	@Override
	public void applyLayout(String layoutName) {
		// do nothing
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

	/**
	 * Gets the listener that handles SWT mouse clicking events.
	 * 
	 * @return the MouseListener object
	 */
	@Override
	public MouseListener getMouseListener() {
		return mouseListener;
	}

	/**
	 * Gets the listener that handles SWT mouse moving events.
	 * 
	 * @return the MouseMoveListener object
	 */
	@Override
	public MouseMoveListener getMouseMoveListener() {
		return mouseMoveListener;
	}

	/**
	 * Handles the mouse up event on the skin composite
	 * 
	 * @param e
	 *            The mouse up event
	 */
	private void handleMouseUp(MouseEvent e) {
		if (e.button == 1) {
			isMouseLeftButtonPressed = false;
			UIHelper.ajustCoordinates(e, this);
			androidInput.sendMouseUp(e.x, e.y);
		}
	}

	/**
	 * Handles the mouse down event on the skin composite
	 * 
	 * @param e
	 *            The mouse down event
	 */
	private void handleMouseDown(MouseEvent e) {
		if (e.button == 1) {
			UIHelper.ajustCoordinates(e, this);
			androidInput.sendMouseDown(e.x, e.y);
			isMouseLeftButtonPressed = true;
		}

	}

	/**
	 * Handles the mouse move event on the skin composite
	 * 
	 * @param e
	 *            The mouse move event
	 */
	private void handleMouseMove(MouseEvent e) {
		if (isMouseLeftButtonPressed) {
			UIHelper.ajustCoordinates(e, this);
			androidInput.sendMouseMove(e.x, e.y);
		}
	}

	@Override
	public boolean isFitToWindowSelected() {
		return isFitToWindow;
	}
}
