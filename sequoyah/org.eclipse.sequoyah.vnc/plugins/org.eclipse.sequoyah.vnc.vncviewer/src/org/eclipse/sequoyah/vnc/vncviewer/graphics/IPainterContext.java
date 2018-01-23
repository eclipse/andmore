/********************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 *
 * Contributors:
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.graphics;

import java.io.DataInput;

import org.eclipse.sequoyah.vnc.vncviewer.network.PixelFormat;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;

/**
 * Public interface that stores the context of the rendering the
 * remote framebuffer
 *
 */
public interface IPainterContext {

	/**
	 * Processes the rectangle using the strategy corresponding to the rectangle encoding
	 * and other possible user preferences   
	 * @param rh <code>RectHeader</code> rectangle to process
	 * @param in stream to read additional data from
	 * @throws Exception
	 */
	public void processRectangle(RectHeader rh, DataInput in) throws Exception;

	/**
	 * Returns the <code>PixelFormat</code> pixel format that is now in use
	 * @return the <code>PixelFormat</code> pixel format that is now in use
	 */
	PixelFormat getPixelFormat();
	
	/**
	 * Returns the number of bytes per pixel according to the current pixel format 
	 * @return the number of bytes per pixel according to the current pixel format
	 * @see <code>getPixelFormat</code>
	 */
	int getBytesPerPixel();

	/**
	 * Reads the pixel from the input stream according to the current pixel format
	 * @param is <code>DataInputStream</code> stream to read the pixel value from
	 * @return read pixel
	 * @throws Exception
	 */
	int readPixel(DataInput is) throws Exception;

	/**
	 * Reads the pixel from the input stream according to the current pixel 
	 * format endianness and <code>bytesPerPixel</code> pixel representation length.   
	 * @param is <code>DataInputStream</code> stream to read the pixel value from
	 * @param bytesPerPixel number of bytes per pixel
	 * @return read pixel
	 * @throws Exception
	 */
	int readPixel(DataInput is, int bytesPerPixel) throws Exception;
	
	
	
	
	// TODO : insert javadoc here 
	int[] readpixels(DataInput is, int w, int h) throws Exception;
	
	
	
	
	/**
	 * Fills the rectangle with top left corner <code>x</code>, <code>y</code>
	 * and width <code>width</code> and height <code>height</code> with the pixel
	 * <code>pixel</code> 
	 * @param pixel the pixel to fill the rectangle
	 * @param x the x position of the rectangle's top left corner 
	 * @param y the y position of the rectangle's top left corner
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 */
	void fillRect(int pixel, int x, int y, int width, int height);

	/**
	 * Sets the rectangle of pixels with top left coordinats <code>x</code>
	 * and <code>y</code> to the values from the array 
	 * <code>pixels</code> starting at <code>startIndex</code>.
	 *
	 * @param x the x position of rectangles' top left the corner
	 * @param y the y position of rectangles' top left the corner
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 * @param pixels the pixels to set
	 * @param start the index at which to begin setting
	 */
	void setPixels(int x, int y, int width, int height, int[] pixels, int start);
}
