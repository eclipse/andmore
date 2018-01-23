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
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.imgdata;

import java.io.DataInput;

import org.eclipse.sequoyah.vnc.vncviewer.graphics.IPainterContext;
import org.eclipse.sequoyah.vnc.vncviewer.network.AbstractVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.PixelFormat;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;
import org.eclipse.swt.graphics.ImageData;


/**
 * This class renders the screen sent by a VNC Server using SWT.
 */
class Painter extends AbstractVNCPainter {

	protected ImageData imgData;
	
	protected SWTRemoteDisplayImgData parent;
	
	public Painter(SWTRemoteDisplayImgData parent) {
		super();
		this.parent = parent;
	}

	public void setSize(int width, int height){
		super.setSize(width, height);
		imgData = new ImageData(width, height, pixelFormat.getDepth(), new org.eclipse.swt.graphics.PaletteData(pixelFormat.getRedMax()<<pixelFormat.getRedShift(), pixelFormat.getGreenMax()<<pixelFormat.getGreenShift(), pixelFormat.getBlueMax()<<pixelFormat.getBlueShift()));
	}
	
	protected void fillRect(int pixel, int x, int y, int width, int height) {
		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {
				imgData.setPixel(x+w, y + h, pixel);
			}
		}
	}

	protected void setPixels(int x, int y, int width, int[] pixels, int startIndex) {
		imgData.setPixels(x, y, width, pixels, startIndex);
	}

	protected void setPixels(int x, int y, int width, int height,
			int[] pixels, int start) {
		for (int i = 0; i < height; i++, start+= width) {
			setPixels(x, y+i, width, pixels, start);
		}
	}

	@Override
	protected IPainterContext getPainterContext() {
		return  new IPainterContext() {
			public void fillRect(int pixel, int x, int y, int width, int height) {
				Painter.this.fillRect(pixel, x, y, width, height);
			}

			public PixelFormat getPixelFormat() {
				return Painter.this.getPixelFormat();
			}

			public int getBytesPerPixel() {
				return bytesPerPixel;
			}

			public void processRectangle(RectHeader rh, DataInput in) throws Exception{
				Painter.this.processRectangle(rh, in);
			}

			public int readPixel(DataInput is) throws Exception {
				return Painter.this.readPixel(is);
			}

			public int readPixel(DataInput is, int bytesPerPixel) throws Exception {
				return Painter.this.readPixel(is, bytesPerPixel);
			}

			public void setPixels(int x, int y, int width, int height,
					int[] pixels, int start) {
				Painter.this.setPixels(x, y, width, height, pixels, start);
			}

			public int[] readpixels(DataInput is, int w, int h) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public void updateRectangle(int x1, int y1, int x2, int y2) {
		parent.redrawScreen();
	}
	
	ImageData getImageData() {
		return imgData;
	}

	
}
