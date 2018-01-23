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
 * Daniel Barboza Franco (Motorola) - Bug [227793] - Implementation of the several enc(...). A little improvement into the reading mechanism (readPixels()).
 * Daniel Barboza Franco (Motorola) - Bug [242129] - Raw enconding not implemented correctly
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.network;

import java.io.DataInput;
import java.io.IOException;

import org.eclipse.sequoyah.vnc.vncviewer.exceptions.ProtoClientException;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.HexTilePaintStrategy;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IPaintStrategy;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IPainterContext;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.RREPaintStrategy;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.RawPaintStrategy;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.ZRLEPaintStrategy;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.ZlibPaintStrategy;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;

/**
 * Abstract class that encapsulates common functionality for IVNCPainter 
 * successors 
 */
public abstract class AbstractVNCPainter implements IVNCPainter {

	/**
	 * Pixel format currently used
	 */
	protected PixelFormat pixelFormat;

	/**
	 * Bytes per pixel
	 */
	protected int bytesPerPixel;

	/**
	 * Framebuffer width
	 */
	private int width;

	/**
	 * Framebuffer height
	 */
	private int height;
	
	private IPaintStrategy rawPaintStrategy;

	private IPaintStrategy rrePaintStrategy;

	private IPaintStrategy hexTilePaintStrategy;

	private  IPaintStrategy zlibPaintStrategy;

	private IPaintStrategy zrlePaintStrategy;

	public AbstractVNCPainter() {
		IPainterContext pc = getPainterContext();
		rawPaintStrategy = new RawPaintStrategy(pc);
		rrePaintStrategy = new RREPaintStrategy(pc);
		hexTilePaintStrategy = new HexTilePaintStrategy(pc);
		zlibPaintStrategy = new ZlibPaintStrategy(pc);
		zrlePaintStrategy = new ZRLEPaintStrategy(pc);
	}

	
	public int[] getSupportedEncodings() {
		return new int[] { IRFBConstants.HEXTILE_ENCODING,
				IRFBConstants.ZRLE_ENCODING, IRFBConstants.ZLIB_ENCODING,
				IRFBConstants.RRE_ENCODING, IRFBConstants.RAW_ENCODING, };
	}

	public static int[] getSupportedEncodingsStatic() {
		return new int[] { IRFBConstants.HEXTILE_ENCODING,
				IRFBConstants.ZRLE_ENCODING, IRFBConstants.ZLIB_ENCODING,
				IRFBConstants.RRE_ENCODING, IRFBConstants.RAW_ENCODING, };
	}

	public void processRectangle(RectHeader rectHeader, DataInput in) throws Exception {
		IPaintStrategy ps = getPaintStrategy(rectHeader.getEncoding());
		
		if (ps != null) {
			ps.processRectangle(rectHeader, in);
		} else {
			log(AbstractVNCPainter.class).error("This encoding is not supported."); //$NON-NLS-1$
		}
		
	}

	public PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	public void setPixelFormat(PixelFormat pixelFormat) {
		this.pixelFormat = pixelFormat;
		bytesPerPixel = ((pixelFormat.getBitsPerPixel())/8);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	
	
	
	
	
	
	private byte[] readpixelsbytes(DataInput in, int bytesNum) throws ProtoClientException{

	    byte pixelsb[] = new byte[bytesNum];

	    // Read the array of pixels from the VNC Server		
	    try {
	        in.readFully(pixelsb, 0, bytesNum);
	    }
	    catch (IOException ioe){
	        log(VNCProtocol.class).error("Rectangle message error: " + ioe.getMessage()); //$NON-NLS-1$
	        throw new ProtoClientException("Rectangle message error."); //$NON-NLS-1$
	    }

	    return pixelsb;
	}
	
	
	public int[] readPixels(DataInput is, int w, int h) throws Exception {
		
		byte pixelsBucket[] = {0x00};
		int result[] = new int[w*h];
				
		int pixelsNum = w * h;
		int bytesToRead = pixelsNum * ((pixelFormat.getBitsPerPixel())/8);
		
		pixelsBucket = readpixelsbytes(is, bytesToRead);
		
		for (int j=0; j < (pixelsNum); j++) {
			//int line = ((int)(j / width));
			//int bytesPerPixel = ((pixelFormat.getBitsPerPixel())/8);
			int pos = j * bytesPerPixel;
		
			int pixel = 0;

			/*
			if (bytesPerPixel == 1) {
				pixel = is.readByte() & 0xFF;
			} 
			else {*/
			boolean bigEndian = pixelFormat.getBigEndianFlag() != 0;
			if (bigEndian) {
				for (int k = 0; k < bytesPerPixel ; k++) {
					//int b = is.readUnsignedByte();
					//pixel = (pixel << 8) | (b & 0xFF);
						
					pixel = (pixel << 8) |(pixelsBucket[pos + k] & 0xFF);
				}
			}
			else {
				for (int i = bytesPerPixel - 1 ; i >= 0 ; i--) {
					
					pixel = (pixel << 8) |(pixelsBucket[pos + i] & 0xFF);
				}
			}

			result[j] = pixel;
		}

		
		return result;
	}
	
	
	
	
	/**
	 * Reads the pixel from the input stream according to the current pixel 
	 * format endianness and <code>bytesPerPixel</code> pixel representation length.   
	 * @param is <code>DataInputStream</code> stream to read the pixel value from
	 * @param bytesPerPixel number of bytes per pixel
	 * @return read pixel
	 * @throws Exception
	 */
	protected int readPixel(DataInput is, int bytesPerPixel) throws Exception {
		int result = 0;
		if (bytesPerPixel == 1) {
			result = is.readByte() & 0xFF;
		} else {
			boolean bigEndian = pixelFormat.getBigEndianFlag() != 0;
			if (bigEndian) {
				for (int jj = 0; jj < bytesPerPixel ; jj++) {
					int b = is.readUnsignedByte();
					result = (result << 8) | (b & 0xFF);
				}
			}
			else {
				for (int jj = 0; jj < bytesPerPixel ; jj++) {
					int b = is.readUnsignedByte();
					result = result | ((b & 0xFF) << jj*8);
				}
			}
		}
		return result;
	}
	
	/**
	 * Reads pixel according to the current pixel format from the <code>DataInputStream</code>
	 * @param is input stream to read from
	 * @return the pixel value
	 * @throws Exception
	 */
	protected int readPixel(DataInput is) throws Exception {
		return readPixel(is, bytesPerPixel);
	}

	protected abstract IPainterContext getPainterContext();

	/**
	 * Returns the paint strategy object for the encoding
	 * @param encoding
	 * @return the paint strategy object for the encoding
	 */
	protected IPaintStrategy getPaintStrategy(int encoding) {
		switch (encoding) {
		case IRFBConstants.RAW_ENCODING:
			return rawPaintStrategy;
		case IRFBConstants.RRE_ENCODING:
			return rrePaintStrategy;
		case IRFBConstants.HEXTILE_ENCODING:
			return hexTilePaintStrategy;
		case IRFBConstants.ZLIB_ENCODING:
			return zlibPaintStrategy;
		case IRFBConstants.ZRLE_ENCODING:
			return zrlePaintStrategy;
		default:
			return null;
		}
	}

}
