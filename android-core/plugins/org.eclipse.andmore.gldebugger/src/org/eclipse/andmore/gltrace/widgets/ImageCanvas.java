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

package org.eclipse.andmore.gltrace.widgets;

import org.eclipse.andmore.gltrace.GlTracePlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

import java.io.File;

public class ImageCanvas extends Canvas {
	private static final int SCROLLBAR_INCREMENT = 20;

	private Point mOrigin;

	private ScrollBar mHorizontalScrollBar;
	private ScrollBar mVerticalScrollBar;

	private Image mImage;
	private boolean mFitToCanvas;

	public ImageCanvas(Composite parent) {
		super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL | SWT.H_SCROLL);
		mOrigin = new Point(0, 0);

		mHorizontalScrollBar = getHorizontalBar();
		mVerticalScrollBar = getVerticalBar();

		mFitToCanvas = true;

		setScrollBarIncrements();
		setScrollBarPageIncrements(getClientArea());

		updateScrollBars();

		SelectionListener scrollBarSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == mHorizontalScrollBar) {
					scrollHorizontally();
				} else {
					scrollVertically();
				}
			}
		};

		mHorizontalScrollBar.addSelectionListener(scrollBarSelectionListener);
		mVerticalScrollBar.addSelectionListener(scrollBarSelectionListener);

		addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				setScrollBarPageIncrements(getClientArea());
				updateScrollBars();
			}
		});

		addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event e) {
				paintCanvas(e.gc);
			}
		});
	}

	public void setFitToCanvas(boolean en) {
		mFitToCanvas = en;
		updateScrollBars();
		redraw();
	}

	public void setImage(Image image) {
		if (mImage != null) {
			mImage.dispose();
		}

		mImage = image;
		mOrigin = new Point(0, 0);
		updateScrollBars();
		redraw();
	}

	private void updateScrollBars() {
		Rectangle client = getClientArea();

		int imageWidth, imageHeight;
		if (mImage != null & !mFitToCanvas) {
			imageWidth = mImage.getBounds().width;
			imageHeight = mImage.getBounds().height;
		} else {
			imageWidth = client.width;
			imageHeight = client.height;
		}

		mHorizontalScrollBar.setMaximum(imageWidth);
		mVerticalScrollBar.setMaximum(imageHeight);
		mHorizontalScrollBar.setThumb(Math.min(imageWidth, client.width));
		mVerticalScrollBar.setThumb(Math.min(imageHeight, client.height));

		int hPage = imageWidth - client.width;
		int vPage = imageHeight - client.height;
		int hSelection = mHorizontalScrollBar.getSelection();
		int vSelection = mVerticalScrollBar.getSelection();
		if (hSelection >= hPage) {
			if (hPage <= 0) {
				hSelection = 0;
			}
			mOrigin.x = -hSelection;
		}

		if (vSelection >= vPage) {
			if (vPage <= 0) {
				vSelection = 0;
			}
			mOrigin.y = -vSelection;
		}

		redraw();
	}

	private void setScrollBarPageIncrements(Rectangle clientArea) {
		mHorizontalScrollBar.setPageIncrement(clientArea.width);
		mVerticalScrollBar.setPageIncrement(clientArea.height);
	}

	private void setScrollBarIncrements() {
		// The default increment is 1 pixel. Assign a saner default.
		mHorizontalScrollBar.setIncrement(SCROLLBAR_INCREMENT);
		mVerticalScrollBar.setIncrement(SCROLLBAR_INCREMENT);
	}

	private void scrollHorizontally() {
		if (mImage == null) {
			return;
		}

		int selection = mHorizontalScrollBar.getSelection();
		int destX = -selection - mOrigin.x;
		Rectangle imageBounds = mImage.getBounds();
		scroll(destX, 0, 0, 0, imageBounds.width, imageBounds.height, false);
		mOrigin.x = -selection;
	}

	private void scrollVertically() {
		if (mImage == null) {
			return;
		}

		int selection = mVerticalScrollBar.getSelection();
		int destY = -selection - mOrigin.y;
		Rectangle imageBounds = mImage.getBounds();
		scroll(0, destY, 0, 0, imageBounds.width, imageBounds.height, false);
		mOrigin.y = -selection;
	}

	private void paintCanvas(GC gc) {
		gc.fillRectangle(getClientArea()); // clear entire client area
		if (mImage == null) {
			return;
		}

		Rectangle rect = mImage.getBounds();
		Rectangle client = getClientArea();

		if (mFitToCanvas && rect.width > 0 && rect.height > 0) {
			double sx = (double) client.width / (double) rect.width;
			double sy = (double) client.height / (double) rect.height;

			if (sx < sy) {
				// if we need to scale more horizontally, then reduce the client
				// height
				// appropriately so that aspect ratios are maintained
				gc.drawImage(mImage, 0, 0, rect.width, rect.height, 0, 0, client.width, (int) (rect.height * sx));
				drawBorder(gc, 0, 0, client.width, (int) (rect.height * sx));
			} else {
				// scale client width to maintain aspect ratio
				gc.drawImage(mImage, 0, 0, rect.width, rect.height, 0, 0, (int) (rect.width * sy), client.height);
				drawBorder(gc, 0, 0, (int) (rect.width * sy), client.height);
			}
		} else {
			gc.drawImage(mImage, mOrigin.x, mOrigin.y);
			drawBorder(gc, mOrigin.x, mOrigin.y, rect.width, rect.height);
		}
	}

	private void drawBorder(GC gc, int x, int y, int width, int height) {
		Color origFg = gc.getForeground();
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(x, y, width, height);
		gc.setForeground(origFg);
	}

	@Override
	public void dispose() {
		if (mImage != null && !mImage.isDisposed()) {
			mImage.dispose();
		}
	}

	public void exportImageTo(File file) {
		if (mImage == null || file == null) {
			return;
		}

		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { mImage.getImageData() };

		try {
			imageLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Save Image", "Error saving image", new Status(IStatus.ERROR,
					GlTracePlugin.PLUGIN_ID, e.toString()));
		}
	}
}
