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
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc *
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer  
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.network;

import java.io.DataInput;
import java.io.OutputStream;

import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolException;

/**
 * This class implements the version 3.3 of the RFB Protocol.
 */
public class VNCProtocol33 extends VNCProtocol {

	/**
	 * Constant that represents the RFB protocol version.
	 */
	static final String RFB_VERSION = "RFB 003.003\n"; //$NON-NLS-1$ 
														 /* used to compare the
														 * implemented version
														 * with the server
														 * version
														 */

	@Override
	protected String getVersion() {
		return RFB_VERSION;
	}

	@Override
	protected int[] readSecurityTypes(DataInput in) throws Exception {
		int[] result = null;
		int secType = in.readInt();
		switch (secType) {
		case SECURITY_TYPE_INVALID:
			handshakeFail(in);
			break;
		case SECURITY_TYPE_NONE:
		case SECURITY_TYPE_VNC:
			result = new int[1];
			result[0] = secType;
			break;
		default:
			throw new Exception(
					"VNC security negotiation error: Unknown security type"); //$NON-NLS-1$
		}
		return result;
	}

	@Override
	protected void sendSecurityType(OutputStream out, int securityType)
			throws Exception {
	}

	@Override
	protected void readAuthenticationResult(DataInput in) throws Exception {

		/* Version 3.3 doesn't have SecurityResult */
	}

	/**
	 * Constant that defines the number of bytes read in the handshake phase.
	 */
	static final int HANDSHAKE_MESSAGE_SIZE = 12; /*
													 * number of bytes read in
													 * the handshake phase
													 */

	/**
	 * This method compares each byte of the RFB Protocol client version using
	 * the String sent by the Server.
	 */
	protected void compareVersion(byte[] b) throws Exception {

		if (!(new String(b)).equals(RFB_VERSION)) {

			throw new ProtocolException("Wrong protocol version."); //$NON-NLS-1$

		}

	}

	/**
	 * Implements the handshake phase of the RFB Protocol.
	 */
	protected void handShake(DataInput in, OutputStream out)
			throws Exception {

		int rfbSecType;
		byte[] b = new byte[HANDSHAKE_MESSAGE_SIZE];

		in.readFully(b, 0, HANDSHAKE_MESSAGE_SIZE);

		compareVersion(b);
		out.write(b);

		rfbSecType = in.readInt(); /* The security type used by the server */
		/* The Sec-types handling is not implemented yet */
	}

}
