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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;

public class ZlibPaintStrategy extends AbstractPaintStrategy {

	public ZlibPaintStrategy(IPainterContext context) {
		super(context);
	}

	public void processRectangle(RectHeader rh, DataInput in) throws Exception {
		int x = rh.getX();
		int y = rh.getY();
		int width = rh.getWidth();
		int height = rh.getHeight();

		int compressedDataLength = in.readInt();
		byte[] compressedDataBuffer = new byte[compressedDataLength];

/*        log(ZlibPaintStrategy.class).debug("Processing rectangle defined by: x=" + 
                x + "; y=" + y + "; w=" + width + "; h=" + height + "zlibDataLength=" + 
                compressedDataLength + ".");*/
		
		in.readFully(compressedDataBuffer, 0, compressedDataLength);
		byte uncompressedDataBuffer[] = new byte[width * height* getContext().getBytesPerPixel()];

		Inflater zlibInflater = new Inflater();
		zlibInflater.setInput(compressedDataBuffer, 0, compressedDataLength);
		try {
			zlibInflater.inflate(uncompressedDataBuffer, 0, uncompressedDataBuffer.length);
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		compressedDataBuffer = null;
		
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(uncompressedDataBuffer)); 
		
		int pixels[] = new int[width * height];
		IPainterContext pc = getContext();
		for (int i = 0, e = width * height; i < e; i++) {
			pixels[i] = pc.readPixel(is);
		}
		getContext().setPixels(x, y, width, height, pixels, 0);
	}
}
