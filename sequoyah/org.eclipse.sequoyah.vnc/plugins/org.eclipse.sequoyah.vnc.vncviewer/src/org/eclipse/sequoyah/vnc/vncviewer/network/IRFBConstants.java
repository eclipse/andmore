/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and Others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo (Eldorado Research Institute) 
 * [246212] - Enhance encapsulation of protocol implementer
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.network;

public interface IRFBConstants {
	/**
	 * Constant used to determine the Framebuffer update request message size in
	 * bytes.
	 */
	static final int FB_UPDATE_REQUEST_MESSAGE_SIZE = 10; /*
	 * number of bytes
	 * sent to the
	 * server in the
	 * framebuffer
	 * update request
	 */

	/* Client to Server message types */

	/**
	 * Constant used to represent the Set Pixel Format RFB Client message.
	 */
	public final static int SET_PIXEL_FORMAT = 0;

	/**
	 * Constant used to represent the Set Encondings RFB Client message.
	 */
	public final static int SET_ENCODINGS = 2;

	/**
	 * Constant used to represent the Framebuffer Update Request RFB Client
	 * message.
	 */
	public final static int FRAMEBUFFER_UPDATE_REQUEST = 3;

	/**
	 * Constant used to represent the Key Event RFB Client message.
	 */
	public final static int KEY_EVENT = 4;

	/**
	 * Constant used to represent the Pointer Event RFB Client message.
	 */
	public final static int POINTER_EVENT = 5;

	/**
	 * Constant used to represent the Client Cut Text RFB Client message.
	 */
	public final static int CLIENT_CUT_TEXT = 6;

	/* Server to Client message types */

	/**
	 * Constant used to represent the Framebuffer Update RFB Server message.
	 */
	public final static int FRAMEBUFFER_UPDATE = 0;

	/**
	 * Constant used to represent the Set Colour Map Entries RFB Server message.
	 */
	public final static int SET_COLOUR_MAP_ENTRIES = 1;

	/**
	 * Constant used to represent the Bell RFB Server message.
	 */
	public final static int BELL = 2;

	public final static int RAW_ENCODING = 0;
	public final static int COPY_RECT_ENCODING = 1;
	public final static int RRE_ENCODING = 2;
	public final static int HEXTILE_ENCODING = 5;
	public final static int ZRLE_ENCODING = 16;

	public final static int ZLIB_ENCODING = 6;

	public static final int SECURITY_TYPE_INVALID = 0;
	public static final int SECURITY_TYPE_NONE = 1;
	public static final int SECURITY_TYPE_VNC = 2;
	public static final int SECURITY_TYPE_RA2 = 5;
	public static final int SECURITY_TYPE_RA2NE = 6;
	public static final int SECURITY_TYPE_TIGHT = 16;
	public static final int SECURITY_TYPE_ULTRA = 17;
	public static final int SECURITY_TYPE_TLS = 18;
	public static final int SECURITY_TYPE_VENCRYPT = 19;

	/**
	 * Constant used to represent the Server Cut Text RFB Server message.
	 */
	public final static int SERVER_CUT_TEXT = 3;

	public static int SHARED_FLAG = 1;
}
