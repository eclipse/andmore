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
package org.eclipse.andmore.android.emulator.ui.controls.nativewindow;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IInputLogic;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.utils.TelnetAndroidInput;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class NativeWindowComposite extends ScrolledComposite implements IAndroidComposite {
	/**
	 * Preference key of the Question Dialog about changing zoom
	 * 
	 */
	private static String LOOSE_ORIGINAL_SCALE_KEY_PREFERENCE = "loose.original.scale";

	// Constants
	private static final double MINIMUM_ZOOM_FACTOR = 0.10;

	private static final double ZOOM_FIT = 0.0;

	private Composite contentComposite;

	private IAndroidEmulatorInstance androidInstance;

	private long windowHandle;

	private long originalParentHandle;

	private long windowProperties;

	private Point windowSize;

	private Point nativeWindowSize;

	private NativeWindowMonitor nativeWindowMonitor;

	protected boolean resizing;

	private boolean isFitToWindow;

	private double zoomFactor = 0.99;

	private double fitZoomFactor = ZOOM_FIT;

	private boolean forceNativeWindowSizeUpdate;

	private boolean isOriginalScale;

	private boolean zoomLocked;

	private class NativeWindowMonitor extends Timer {
		private Timer timer;

		private MonitorTask monitorTask;

		public NativeWindowMonitor(long interval) {
			timer = new Timer();
			monitorTask = new MonitorTask();
			timer.schedule(monitorTask, interval, interval);
		}

		private class MonitorTask extends TimerTask {
			@Override
			public void run() {
				Point newWindowSize = NativeUIUtils.getWindowSize(originalParentHandle, windowHandle);
				if ((windowHandle <= 0) || !newWindowSize.equals(windowSize)) {
					Display display = Display.getDefault();
					if (!display.isDisposed()) {
						try {
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									updateContentComposite();
								}
							});
						} catch (SWTException e) {
							// Do nothing in case the widget is disposed, occurs
							// when the tool is closing.
						}
					}
				}

				if (NativeUIUtils.isWindowEnabled(windowHandle)) {
					Display display = Display.getDefault();
					if (!display.isDisposed()) {
						try {
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									if (!contentComposite.isDisposed()) {
										contentComposite.forceFocus();
									}
								}
							});
						} catch (SWTException e) {
							// Do nothing in case the widget is disposed, occurs
							// when the tool is closing.
						}
					}
				}
			}
		}

		public void stopMonitoring() {
			timer.cancel();
			timer = null;
			monitorTask = null;
		}
	}

	public NativeWindowComposite(Composite parent, IAndroidSkin androidSkin, final IAndroidEmulatorInstance instance) {
		super(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

		info("Creating Native Window Composite for " + instance.getName());

		getVerticalBar().setEnabled(true);
		getHorizontalBar().setEnabled(true);
		this.setLayout(new FillLayout());

		androidInstance = instance;

		nativeWindowMonitor = new NativeWindowMonitor(500);

		addControlListener(new ControlAdapter() {
			final boolean[] running = new boolean[1];

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
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// do nothing
					}

					if (running[0]) {
						return;
					}
					running[0] = true;
					Display.getCurrent().asyncExec(new Runnable() {
						@Override
						public void run() {
							running[0] = false;
							if (!getShell().isDisposed()) {
								calculateFitZoomFactor(forceNativeWindowSizeUpdate);
								applyZoomFactor();
							}

						}
					});
				}
			}
		});

		createContentComposite(instance);
		info("Created Native Window Composite for " + instance.getName());
	}

	/**
	 * Creates the content composite that will be parent of emulator native
	 * window
	 * 
	 * @param instance
	 *            A android instance from which the composite could be retrieved
	 *            if it is already created
	 */
	public void createContentComposite(IAndroidEmulatorInstance instance) {
		contentComposite = instance.getComposite();
		if (contentComposite != null) {
			info("Instance already has a composite");
			contentComposite.setParent(this);
			contentComposite.setVisible(true);
		} else {
			contentComposite = new Composite(this, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		}

		this.setContent(contentComposite);
		if (instance.getProperties().getProperty("Command_Line").contains("-scale")) {
			isOriginalScale = true;
		}

		// Force to update native window size at 100% when first using
		// nativeWindowSize field
		forceNativeWindowSizeUpdate = true;

		// Avoid perform apply zoom factor when creating composite
		zoomLocked = true;
		draw();
	}

	/**
	 * Changes the parent from OS to content composite keeping the original
	 * properties and parent window reference
	 */
	private void draw() {
		if (contentComposite != null) {
			windowHandle = androidInstance.getWindowHandle();

			// If the instance does not contain the window handle, it should be
			// retrieved
			// from native emulator window and assigned to instance
			if (windowHandle <= 0) {
				int port = AndroidLogicUtils
						.getEmulatorPort(DDMSFacade.getSerialNumberByName(androidInstance.getName()));
				windowHandle = NativeUIUtils.getWindowHandle(androidInstance.getName(), port);

				androidInstance.setWindowHandle(windowHandle);
			}

			if ((windowProperties <= 0) && (windowHandle > 0)) {
				windowProperties = NativeUIUtils.getWindowProperties(windowHandle);
				info("Native Window Properties:" + windowProperties);
			}

			// Set Window Style
			if (windowHandle > 0) {
				NativeUIUtils.setWindowStyle(windowHandle);
			}

			if (originalParentHandle <= 0) {
				originalParentHandle = windowHandle;
			}

			// Retrieve window size before changing parent
			if (windowHandle > 0) {
				windowSize = NativeUIUtils.getWindowSize(originalParentHandle, windowHandle);
			}

			// Set the new Parent and store the original parent
			if ((originalParentHandle <= 0) || (originalParentHandle == windowHandle)) {
				if (windowHandle > 0) {
					originalParentHandle = NativeUIUtils.embedWindow(windowHandle, contentComposite);
					info("Native Window Parent:" + originalParentHandle);
				}
			} else {
				NativeUIUtils.embedWindow(windowHandle, contentComposite);
			}

			if (windowSize == null) {
				windowSize = new Point(700, 500);
			}

			// Update composite size
			contentComposite.setSize(contentComposite.computeSize(windowSize.x, windowSize.y, true));
			contentComposite.redraw();
			this.update();

			this.setMinSize(contentComposite.computeSize(windowSize.x, windowSize.y));
			this.layout();
		} else {
			createContentComposite(androidInstance);
		}
	}

	public void changeToNextLayout() {
		contentComposite.setVisible(false);

		contentComposite.setLocation(0, 0);

		NativeUIUtils.sendNextLayoutCommand(originalParentHandle, windowHandle);

		updateContentComposite();

		forceNativeWindowSizeUpdate = true;
		if (isFitToWindow) {
			// Force update to fit zoom factor
			setZoomFactor(ZOOM_FIT);
		}
		applyZoomFactor();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// do nothing
		}

		contentComposite.setVisible(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		info("Disposing Native Window Composite");
		if (nativeWindowMonitor != null) {
			nativeWindowMonitor.stopMonitoring();
			nativeWindowMonitor = null;
			info("Disposed Native Window Monitor");
		}
		if (windowHandle > 0) {
			info("Restoring original properties for window: " + windowHandle);
			NativeUIUtils.setWindowProperties(windowHandle, windowProperties);

			boolean shallUnembed = AndroidPlugin.getDefault().getPreferenceStore()
					.getBoolean(AndroidPlugin.SHALL_UNEMBED_EMULATORS_PREF_KEY);
			if ((originalParentHandle > 0) && shallUnembed) {
				info("Setting original parent: " + originalParentHandle + " for window" + windowHandle);
				NativeUIUtils.unembedWindow(windowHandle, originalParentHandle);
				// Force update when redrawing
				androidInstance.setWindowHandle(0);
				info("Restoring window: " + windowHandle);
				NativeUIUtils.restoreWindow(windowHandle);
			}

		}

		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			info("Trying to store the content composite in instance");
			if (contentComposite != null) {
				info("Is instance started? :" + androidInstance.isStarted());
				if (androidInstance.isStarted()) {
					try {
						contentComposite.setParent(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
						this.setContent(null);
						contentComposite.setVisible(true);
						androidInstance.setComposite(contentComposite);
					} catch (Exception e) {
						error("Error trying to store the content composite :" + e.getMessage());
					}
				}
			}
		}
		super.dispose();
	}

	/**
	 * Apply the zoom factor to the instance
	 */
	@Override
	public void applyZoomFactor() {
		if (!isOriginalScale && !zoomLocked) {
			contentComposite.setLocation(0, 0);
			IInputLogic inputLogic = androidInstance.getInputLogic();
			TelnetAndroidInput telnetAndroidInput = (TelnetAndroidInput) inputLogic;
			NativeUIUtils.hideWindow(windowHandle);

			if (isFitToWindow) {
				telnetAndroidInput.sendWindowScale(fitZoomFactor);
			} else {
				telnetAndroidInput.sendWindowScale(zoomFactor);
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// do nothing
			}
			telnetAndroidInput.dispose();
			NativeUIUtils.showWindow(windowHandle);
			updateContentComposite();
		}
	}

	void calculateFitZoomFactor(boolean requireNativeSizeUpdate) {
		// Compute new zoom factor if the zoom mode is "Fit to Window"
		Rectangle clientArea = getClientArea();
		if ((clientArea.width == 0) || (clientArea.height == 0)) {
			// zoom factor cannot be zero, otherwise an
			// IllegalArgumentException
			// is raised in some SWT methods
			fitZoomFactor = MINIMUM_ZOOM_FACTOR;
		} else {
			// if the layout was changed, it is needed to retrieve the native
			// window size at 100%
			// that size is required to the correct ratio calculus
			if (requireNativeSizeUpdate) {
				forceNativeWindowSizeUpdate = false;
				updateNativeWindowSize();
			} else {
				double widthRatio = (double) (clientArea.width) / nativeWindowSize.x;
				double heightRatio = (double) (clientArea.height) / nativeWindowSize.y;
				fitZoomFactor = (Math.min(widthRatio, heightRatio) > MINIMUM_ZOOM_FACTOR ? Math.min(widthRatio,
						heightRatio) : MINIMUM_ZOOM_FACTOR);
			}
		}
	}

	/**
	 * This method brings the emulator window to 100% zoom factor to retrieve
	 * their native size
	 */
	private void updateNativeWindowSize() {
		info("Updating Native Window Size");
		setZoomFactor(1.0d);
		applyZoomFactor();

		nativeWindowSize = NativeUIUtils.getWindowSize(originalParentHandle, windowHandle);

		setZoomFactor(ZOOM_FIT);
		applyZoomFactor();
		info("Updated Native Window Size");
	}

	private void updateContentComposite() {
		if (!this.isDisposed()) {
			windowSize = NativeUIUtils.getWindowSize(originalParentHandle, windowHandle);
			if (windowSize != null) {
				if ((contentComposite != null) && !contentComposite.isDisposed()) {
					contentComposite.setSize(windowSize.x, windowSize.y);
					contentComposite.redraw();
				}
				this.setMinSize(windowSize.x, windowSize.y);
				draw();
				this.redraw();
				info("Updated Content Composite");
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
		if (isFitToWindow) {
			return fitZoomFactor;
		}
		return zoomFactor;
	}

	/**
	 * Sets the zoom factor.
	 * 
	 * @param zoom
	 *            the zoom factor
	 * 
	 */
	@Override
	public void setZoomFactor(double zoom) {
		boolean execute = true;
		zoomLocked = false;
		if (isOriginalScale) {
			execute = DialogWithToggleUtils.showQuestion(LOOSE_ORIGINAL_SCALE_KEY_PREFERENCE,
					EmulatorNLS.QUESTION_NativeWindow_LooseOriginalScale_Title,
					EmulatorNLS.QUESTION_NativeWindow_LooseOriginalScale_Text);
		}
		if (execute) {
			isOriginalScale = false;

			if (zoom == ZOOM_FIT) {
				isFitToWindow = true;
				calculateFitZoomFactor(forceNativeWindowSizeUpdate);
			} else {
				isOriginalScale = false;
				isFitToWindow = false;
			}
			zoomFactor = zoom;
		}
	}

	@Override
	public boolean setFocus() {
		NativeUIUtils.setWindowFocus(windowHandle);
		return super.setFocus();
	}

	@Override
	public void applyLayout(String layoutName) {
		setLayout(new FillLayout());
		draw();
	}

	@Override
	public KeyListener getKeyListener() {
		return null;
	}

	@Override
	public MouseListener getMouseListener() {
		return null;
	}

	@Override
	public MouseMoveListener getMouseMoveListener() {
		return null;
	}

	@Override
	public boolean isFitToWindowSelected() {
		return isFitToWindow;
	}
}
