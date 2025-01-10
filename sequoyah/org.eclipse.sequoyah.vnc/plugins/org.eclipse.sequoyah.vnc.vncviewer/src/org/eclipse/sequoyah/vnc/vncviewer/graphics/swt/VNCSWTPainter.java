/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Franco (Motorola)
 *
 * Contributors:
 * Fabio Rigo - Bug [221741] - Support to VNC Protocol Extension
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt;

import java.io.DataInput;

import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.PixelFormat;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;
import org.eclipse.swt.graphics.ImageData;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;

/**
 * This class renders the screen sent by a VNC Server using SWT.
 */
public class VNCSWTPainter implements IVNCPainter, ISWTPainter {

	// static final int COLOR_DEPTH = 24;

	static final int RED_MASK = 0x0000ff;
	static final int GREEN_MASK = 0x00ff00;
	static final int BLUE_MASK = 0xff0000;

	public final static int RAW_ENCODING = 0;
	public final static int COPY_RECT_ENCODING = 1;
	public final static int RRE_ENCODING = 2;
	public final static int HEXTILE_ENCODING = 5;
	public final static int ZRLE_ENCODING = 16;

	private PixelFormat pixelFormat;

	private int fbWidth; /* The framebuffer width */
	private int fbHeight; /* The framebuffer height */

	private ImageData imgData;

	public PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	public void setPixelFormat(PixelFormat pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public void setSize(int width, int height) {

		fbWidth = width;
		fbHeight = height;

		imgData = new ImageData(width, height, pixelFormat.getDepth(),
				new org.eclipse.swt.graphics.PaletteData(RED_MASK, GREEN_MASK,
						BLUE_MASK));

	}

	public void processRectangle(int encoding, byte[] pixelsb, int x, int y,
			int width, int height) {

		switch (encoding) {

		case RAW_ENCODING:

			processRaw(pixelsb, x, y, width, height);
			break;

		case COPY_RECT_ENCODING:
		case RRE_ENCODING:
		case HEXTILE_ENCODING:
		case ZRLE_ENCODING:

		default:
			log(VNCSWTPainter.class).error("This encoding is not supported."); //$NON-NLS-1$

		}

	}

	private void processRaw(byte[] pixelsb, int x, int y, int width, int height) {

		int pixelsNum = width * height;

		/*
		 * The fbWidth is used instead of width because it's not possible to
		 * paint an rectangle area in the middle of the ImageData.
		 */
		int pixels[] = new int[fbWidth * height];

		final int padding = fbWidth - x - width;

		if ((x + width <= fbWidth) && (y <= fbHeight)) {
			imgData.getPixels(0, y, fbWidth * height, pixels, 0);
		}


		/*
		 * The area effectively painted is composed by:
		 *   1) the rectangle of data sent by the server
		 *   2) the gap between the framebuffer's left border and the rectangle
		 *   3) the padding between the rectangle and the framebuffer's right border
		 *   
		 *            x   x+w
		 *      _________________ 
		 *      |                |
		 * y    |_ _ _ ____ _ _ _|
		 *      |  2  | 1 |  3   |
		 * y+h  |_ _ _|___| _ _ _|
		 *      |________________|
		 */
		
		for (int j = 0; j < (pixelsNum); j++) {
			int pixel;
			int line = ((int) (j / width));
			int bytesPerPixel = ((pixelFormat.getBitsPerPixel()) / 8);
			int pos = j * bytesPerPixel;

			// converts a pixel sent by the server into a SWT pixel using the
			// rules specified in the protocol

			int red, green, blue;
			pixel = 0;

			if (pixelFormat.getBigEndianFlag() != 0) {

				for (int i = 0; i < bytesPerPixel; i++) {

					pixel = (pixel << 8) | (pixelsb[pos + i] & 0xFF);
				}

			} else { /*
						 * If pixel value is in little-endian format, swap the
						 * bytes
						 */

				for (int i = bytesPerPixel - 1; i >= 0; i--) {

					pixel = (pixel << 8) | (pixelsb[pos + i] & 0xFF);
				}
			}

			/*
			 * For more information about the Max and Shift values see the RFB
			 * Protocol
			 */
			red = pixel >> pixelFormat.getRedShift();
			red = red & pixelFormat.getRedMax();
			red = (red * (0xFF)) / pixelFormat.getRedMax(); /*
															 * Scale the color
															 * to SWT
															 */

			green = pixel >> pixelFormat.getGreenShift();
			green = green & pixelFormat.getGreenMax();
			green = (green * (0xFF)) / pixelFormat.getGreenMax();

			blue = pixel >> pixelFormat.getBlueShift();
			blue = blue & pixelFormat.getBlueMax();
			blue = (blue * (0xFF)) / pixelFormat.getBlueMax();

			pixel = (blue << 16) | (green << 8) | red;

			pixels[(line * padding) + j + ((line + 1) * x)] = pixel;

		}

		imgData.setPixels(0, y, fbWidth * height, pixels, 0);

	}

	// TODO : in the future all the rectangles will be sent to the painter and
	// then the sender will call the render method
	public void render() {

	}

	public ImageData getImageData() {
		return imgData;
	}

	public int getHeight() {
		return fbHeight;
	}

	public int getWidth() {
		return fbWidth;
	}

	public int[] getSupportedEncodings() {
		// TODO Auto-generated method stub
		return null;
	}

	public void processRectangle(RectHeader rectHeader, DataInput in)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void updateRectangle(int x1, int y1, int x2, int y2) {
		// TODO Auto-generated method stub
		
	}

}
