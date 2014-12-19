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

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.ISWTPainter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * DESCRIPTION: This class implements the composite that displays the contents
 * of the CLI display, according to the provided painter object.
 *
 * RESPONSIBILITY: - Display the contents of the CLI display at screen
 *
 * COLABORATORS: None.
 *
 * USAGE: Add the composite to a view or other equivalent SWT object for it to
 * display the result of the communication through VNC Protocol
 *
 */
public class RemoteCLIDisplay extends Composite {
	private Canvas canvas;

	private Image screen = null;

	private ISWTPainter painter;

	private Timer refreshTimer;

	private static long FIRST_REFRESH_DELAY_MS = 500; /*
													 * Time in milliseconds for
													 * the first update
													 */

	private static long REFRESH_DELAY_PERIOD_MS = 300; /*
														 * Time in milliseconds
														 * between 2 updates
														 */

	private boolean active = false;

	private double zoomFactor = 1;

	/**
	 * Creates a new RemoteCLIDisplay object.
	 *
	 * @param parent
	 *            The parent composite
	 * @param cliPainter
	 *            The object where to retrieve pixels from
	 */
	public RemoteCLIDisplay(Composite parent, ISWTPainter cliPainter) {
		super(parent, SWT.BACKGROUND);
		this.painter = cliPainter;
		this.setLayout(parent.getLayout());
		canvas = new Canvas(this, SWT.BACKGROUND);
	}

	/**
	 * Starts the display refresh
	 */
	synchronized public void start() {
		addRefreshTimer();
		setRunning(true);
	}

	/**
	 * Stops the display refresh
	 */
	synchronized public void stop() {
		setRunning(false);
		refreshTimer.cancel();

		if (!canvas.isDisposed()) {
			GC gc = new GC(canvas);
			canvas.drawBackground(gc, 0, 0, canvas.getSize().x, canvas.getSize().y);
			gc.dispose();
		}
	}

	/**
	 * Adds a timer that schedules the screen's update in a fixed period.
	 */
	private void addRefreshTimer() {
		refreshTimer = new Timer();

		final IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(this);

		refreshTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (instance.getHasCli()) {
					// Request CLI Update here (when applicable)

					getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							updateScreen();
						}
					});
				}
			}
		}, FIRST_REFRESH_DELAY_MS, REFRESH_DELAY_PERIOD_MS);
	}

	/**
	 * Performs the screen update itself
	 */
	private void updateScreen() {
		if (screen != null) {
			screen.dispose();
		}

		if ((!getDisplay().isDisposed()) && (isDisplayActive())) {
			if ((painter.getImageData() != null) && (!canvas.isDisposed())) {
				screen = new Image(canvas.getDisplay(), painter.getImageData().scaledTo(
						(int) (painter.getImageData().width * zoomFactor),
						(int) (painter.getImageData().height * zoomFactor)));

				GC gc = new GC(canvas);
				gc.drawImage(screen, 0, 0);
				gc.dispose();
			}
		} else {
			stop();
		}
	}

	/**
	 * Returns true if the component is running.
	 */
	synchronized public boolean isDisplayActive() {
		return active;
	}

	/**
	 * Gets the Canvas used to show the screen.
	 *
	 * @return the Canvas object.
	 */
	public Canvas getCanvas() {
		return canvas;
	}

	/**
	 * Retrieves the image being drawn at display
	 *
	 * @return The image being drawn at display
	 */
	public Image getScreen() {
		return screen;
	}

	/**
	 * Retrieves the display width
	 *
	 * @return The display width
	 */
	public int getScreenWidth() {
		return painter.getWidth();
	}

	/**
	 * Retrieves the display height
	 *
	 * @return The display height
	 */
	public int getScreenHeight() {
		return painter.getHeight();
	}

	/**
	 * Sets the current state of the display
	 *
	 * @param running
	 *            true if the display is refreshing; false otherwise
	 */
	synchronized private void setRunning(boolean running) {
		this.active = running;
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		if (isDisplayActive()) {
			stop();
		}

		if (screen != null) {
			screen.dispose();
		}

		canvas.dispose();
		super.dispose();
	}

	/**
	 * @see org.eclipse.swt.widgets.Control#setBackground(Color)
	 */
	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		canvas.setBackground(color);
	}

	/**
	 * Retrieves the current zoom factor being applied to the screen
	 *
	 * @return The current zoom factor
	 */
	public double getZoomFactor() {
		return zoomFactor;
	}

	/**
	 * Sets a new zoom factor to the screen
	 *
	 * @param zoomFactor
	 *            The zoom factor to set to the screen
	 */
	public void setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;

		IAndroidEmulatorInstance instance = UIHelper.getInstanceAssociatedToControl(this);
		if (instance.getHasCli()) {
			updateScreen();
		}
	}
}
