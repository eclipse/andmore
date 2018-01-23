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

package org.eclipse.sequoyah.vnc.vncviewer.network;

import java.io.DataInput;

import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolException;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;

/**
 * This class is used to handle the pixel data used within the RFB (VNC)
 * Protocol. <br>
 * <br>
 * More information about the pixel format parameters can be read in to the RFB
 * Protocol specification.
 */
public class PixelFormat {
	
	private int 
		/* The variables below are explained in the VNC Protocol Specification - aka The RFB Protocol */
		bitsPerPixel,
		depth,
		bigEndianFlag,
		trueColourFlag,
		
		redMax,
		greenMax,
		blueMax,
		
		redShift,
		greenShift,
		blueShift;
	
	private static int paddingSize = 3;  /* the padding is a number of unused bytes that completes an Word of data */


	public PixelFormat() {

	}

	
	/**
	 * Gets the pixel data from the server.
	 * 
	 * @param in the DataInputStream that reads data coming from the server.
	 */
	public void getPixelFormat(DataInput in) throws ProtocolException {

		try {
			bitsPerPixel = in.readUnsignedByte();
			depth = in.readUnsignedByte();
			bigEndianFlag = in.readUnsignedByte();
			trueColourFlag = in.readUnsignedByte();

			redMax = in.readUnsignedShort();
			greenMax = in.readUnsignedShort();
			blueMax = in.readUnsignedShort();

			redShift = in.readUnsignedByte();
			greenShift = in.readUnsignedByte();
			blueShift = in.readUnsignedByte();

			byte[] padding = new byte[paddingSize];
			in.readFully(padding);
			
		}
		catch (Exception e){
			log(PixelFormat.class).error("Pixel Format read error: " + e.getMessage()); //$NON-NLS-1$
			throw new ProtocolException("Pixel Format read error."); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the bitsPerPixel value.
	 * @return the bitsPerPixel value.
	 */
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}

	/**
	 * Gets the depth value.
	 * @return the depth value.
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Gets the bigEndianFlag value.
	 * @return the bigEndianFlag value.
	 */
	public int getBigEndianFlag() {
		return bigEndianFlag;
	}

	/**
	 * Gets the trueColourFlag value.
	 * @return the trueColourFlag value.
	 */
	public int getTrueColourFlag() {
		return trueColourFlag;
	}

	/**
	 * Gets the redMax value.
	 * @return the redMax value.
	 */
	public int getRedMax() {
		return redMax;
	}

	/**
	 * Gets the greenMax value.
	 * @return the greenMax value.
	 */
	public int getGreenMax() {
		return greenMax;
	}

	/**
	 * Gets the blueMax value.
	 * @return the blueMax value.
	 */
	public int getBlueMax() {
		return blueMax;
	}

	/**
	 * Gets the redShift value.
	 * @return the redShift value.
	 */
	public int getRedShift() {
		return redShift;
	}

	/**
	 * Gets the greenShift value.
	 * @return the greenShift value.
	 */
	public int getGreenShift() {
		return greenShift;
	}

	/**
	 * Gets the blueShift value.
	 * @return the blueShift value.
	 */
	public int getBlueShift() {
		return blueShift;
	}


}

