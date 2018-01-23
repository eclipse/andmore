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
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Barboza Franco (Eldorado Research Institute) - [271205] - Remove log for mouse, keyboard and screen events
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.graphics;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.eclipse.sequoyah.vnc.vncviewer.network.PixelFormat;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;

public class ZRLEPaintStrategy extends AbstractPaintStrategy {

	private MemoryBlockInputStream memoryBlockStream = new MemoryBlockInputStream();

	private DataInput zlibStream = null;

	public ZRLEPaintStrategy(IPainterContext context) {
		super(context);
	}

	protected int getBytesPerCPixel() {
		int result = 0;
		PixelFormat pixelFormat = getContext().getPixelFormat();
		if (pixelFormat.getTrueColourFlag() != 0
				&& pixelFormat.getBitsPerPixel() == 32
				&& pixelFormat.getDepth() <= 24) {
			result = 3;
		} else {
			result = getContext().getBytesPerPixel();
		}
		return result;
	}

	public void processRectangle(RectHeader rh, DataInput in)
			throws Exception {
		int x = rh.getX();
		int y = rh.getY();
		int width = rh.getWidth();
		int height = rh.getHeight();

/*        log(ZlibPaintStrategy.class).debug("Processing rectangle defined by: x=" + 
                x + "; y=" + y + "; w=" + width + "; h=" + height + ".");*/
		
		DataInput zlibInputStream = getZlibInputStream();		
		memoryBlockStream.readBlock(in);

		int[] tilePixels = new int[64 * 64];

		for (int tileY = y; tileY < y + height; tileY += 64) {
			int tileHeight = Math.min(y + height - tileY, 64);
			for (int tileX = x; tileX < x + width; tileX += 64) {
				int tileWidth = Math.min(x + width - tileX, 64);
				int mode = zlibInputStream.readUnsignedByte();
				boolean rle = (mode & 128) != 0;
				int palSize = mode & 127;
				int[] palette = new int[128];
				readZrlePalette(zlibInputStream, palette, palSize);
				if (palSize == 1) {
					int pix = palette[0];
					getContext().fillRect(pix, tileX, tileY, tileWidth,
							tileHeight);
					continue;
				}

				if (!rle) {
					if (palSize == 0) {
						readZrleRawPixels(zlibInputStream, tilePixels,
								tileWidth, tileHeight);
					} else {
						readZrlePackedPixels(zlibInputStream, tilePixels,
								tileWidth, tileHeight, palette, palSize);
					}
				} else {
					if (palSize == 0) {
						readZrlePlainRLEPixels(zlibInputStream, tilePixels,
								tileWidth, tileHeight);
					} else {
						readZrlePackedRLEPixels(zlibInputStream, tilePixels,
								tileWidth, tileHeight, palette);
					}
				}
				processUpdatedZrleTile(tilePixels, tileX, tileY, tileWidth,
						tileHeight);
			}
		}
	}

	void readZrlePalette(DataInput zrleInStream, int[] palette,
			int palSize) throws Exception {
		readPixels(zrleInStream, palette, palSize);
	}

	void readZrleRawPixels(DataInput zrleInStream, int tilePixels[],
			int tw, int th) throws Exception {
		readPixels(zrleInStream, tilePixels, tw * th);
	}

	void readZrlePackedPixels(DataInput zrleInStream, int[] tilePixels,
			int tw, int th, int[] palette, int palSize) throws Exception {

		int bppp = ((palSize > 16) ? 8 : ((palSize > 4) ? 4
				: ((palSize > 2) ? 2 : 1)));
		int ptr = 0;

		for (int i = 0; i < th; i++) {
			int eol = ptr + tw;
			int b = 0;
			int nbits = 0;

			while (ptr < eol) {
				if (nbits == 0) {
					b = zrleInStream.readUnsignedByte();
					nbits = 8;
				}
				nbits -= bppp;
				int index = (b >> nbits) & ((1 << bppp) - 1) & 127;
				tilePixels[ptr++] = palette[index];
			}
		}
	}

	void readZrlePlainRLEPixels(DataInput zrleInStream, int[] tilePixels,
			int tw, int th) throws Exception {
		int ptr = 0;
		int end = ptr + tw * th;
		while (ptr < end) {
			int pix = readPixel(zrleInStream);
			int len = 1;
			int b;
			do {
				b = zrleInStream.readUnsignedByte();
				len += b;
			} while (b == 255);

			if (!(len <= end - ptr))
				throw new Exception("ZRLE decoder: assertion failed" //$NON-NLS-1$
						+ " (len <= end-ptr)"); //$NON-NLS-1$
			while (len-- > 0) {
				tilePixels[ptr++] = pix;
			}
		}
	}

	void readZrlePackedRLEPixels(DataInput zrleInStream,
			int[] tilePixels, int tw, int th, int[] palette) throws Exception {

		int ptr = 0;
		int end = ptr + tw * th;
		while (ptr < end) {
			int index = zrleInStream.readUnsignedByte();
			int len = 1;
			if ((index & 128) != 0) {
				int b;
				do {
					b = zrleInStream.readUnsignedByte();
					len += b;
				} while (b == 255);

				if (!(len <= end - ptr))
					throw new Exception("ZRLE decoder: assertion failed" //$NON-NLS-1$
							+ " (len <= end - ptr)"); //$NON-NLS-1$
			}

			index &= 127;
			int pix = palette[index];
			while (len-- > 0) {
				tilePixels[ptr++] = pix;
			}
		}
	}

	void readPixels(DataInput is, int[] dst, int count) throws Exception {
		int bytesPerCPixel = getBytesPerCPixel();
		IPainterContext pc = getContext();
		for (int i = 0; i < count; i++) {
			dst[i] = pc.readPixel(is, bytesPerCPixel);
		}
	}

	int readPixel(DataInput is) throws Exception {
		return getContext().readPixel(is, getBytesPerCPixel());
	}

	void processUpdatedZrleTile(int[] tilePixels, int x, int y, int width,
			int height) {
		getContext().setPixels(x, y, width, height, tilePixels, 0);
	}

	private DataInput getZlibInputStream() {
		if (zlibStream == null) {
			zlibStream = new DataInputStream(new InflaterInputStream(
					memoryBlockStream, new Inflater(), 16*1024));
		}
		return zlibStream;
	}

	protected static class MemoryBlockInputStream extends InputStream {

		protected byte buffer[];

		protected int bufferLength;

		int pointer = 0;
		
		@Override
		public int read() throws IOException {
			if (buffer != null) {
				if (pointer < bufferLength) {
					return buffer[pointer++] & 0xFF;
				} else {
					return -1;
				}
			} else {
				throw new IOException("EOF"); //$NON-NLS-1$
			}
		}

		public void readBlock(DataInput in) throws Exception {
			int n = in.readInt();
			if (buffer == null || buffer.length < n) {
			  buffer = new byte[n];
			}		
			in.readFully(buffer, 0, n);
			bufferLength = n;
			pointer = 0;
		}

		@Override
		public int available() throws IOException {
			if (buffer != null) {
				return bufferLength - pointer;
			} else {
				return 0;
			}
		}
	}
	
}
