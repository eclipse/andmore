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
 * Daniel Barboza Franco (Eldorado Research Institute) -  [243167] - Zoom mechanism not working properly 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248663] - Dependency between protocol and SWTRemoteDisplay
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.img;

import java.io.DataInput;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.sequoyah.vnc.vncviewer.graphics.IPainterContext;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.ISWTPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.AbstractVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.PixelFormat;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

/**
 * This class renders the screen sent by a VNC Server using 
 * the SWT Image object
 */
public class Painter extends AbstractVNCPainter implements ISWTPainter {

	protected Image image; 

	protected ImageData imgData;
	
	protected GC imageGC;

	protected PaletteData paletteData;
	
	private Display display;
	
	private List<SWTRemoteDisplayImg> remoteDisplays;
	
	
	public void addSWTRemoteDisplayImg(SWTRemoteDisplayImg rdi) {
		remoteDisplays.add(rdi);
	}
	
	public void removeSWTRemoteDisplayImg(SWTRemoteDisplayImg rdi) {
		remoteDisplays.remove(rdi);
	}
	
	public Painter(SWTRemoteDisplayImg swtDisplay) {
		super();
		remoteDisplays = new ArrayList<SWTRemoteDisplayImg>();
		this.display = swtDisplay.getDisplay();
	}

	public void setPixelFormat(PixelFormat pixelFormat) {
		super.setPixelFormat(pixelFormat);
		paletteData = new PaletteData(pixelFormat.getRedMax()<<pixelFormat.getRedShift(), pixelFormat.getGreenMax()<<pixelFormat.getGreenShift(), pixelFormat.getBlueMax()<<pixelFormat.getBlueShift());		
	}

	public void setSize(int width, int height){
		super.setSize(width, height);
		dispose();
		image = new Image(display, width, height);
		
		
		imageGC = new GC(image);
		imgData = new ImageData(width, height, pixelFormat.getDepth(), paletteData);
	}
	
	protected void fillRect(int pixel, int x, int y, int width, int height) {
		//Color color = new Color(image.getDevice(), paletteData.getRGB(pixel));
		//imageGC.setBackground(color);
		
		int [] pixels;
		pixels = new int[width];
		
		for (int i=0; i<width; i++ ) {
			pixels[i] = pixel; 
		}

		for (int j=0; j<height; j++) {
			imgData.setPixels(x, y+j, width, pixels, 0);	
		}

		//color.dispose();
	}

	protected void setPixels(int x, int y, int width, int[] pixels, int startIndex) {
		//We do not need this feature in this implementation
	}

	protected void setPixels(int x, int y, int width, int height,
			int[] pixels, int start) {
		
		
		for (int i=0; i<height; i++) {
			imgData.setPixels(x, y+i, width, pixels, width*i);	
		}
		
	}
	
	public void updateRectangle(int x1, int y1, int x2, int y2) {
		
		for (SWTRemoteDisplayImg rd : remoteDisplays) {
			double zoom = rd.getZoomFactor();
			
			int a, b, r, s;
			a = (int) (x1 * zoom);
			b = (int) (y1 * zoom);
			r = (int) ((x2-x1) * zoom);
			s = (int) ((y2-y1) * zoom);
			
			// Cover rounded values
			a--;
			b--;
			r++;
			s++;
			
			rd.redrawScreen(a, b, r, s);
		}
		
	}

	@Override
	protected IPainterContext getPainterContext() {
		return new IPainterContext() {

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

			public int[] readpixels(DataInput is, int w, int h) throws Exception {
				return  Painter.this.readPixels(is, w, h);
			}
		};
	}

	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
		if (imageGC != null) {
			imageGC.dispose();
			imageGC = null;
		}
	}

	public ImageData getImageData() {
		return imgData;
	}
	
	
}
