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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.network;


/**
 * This interface provides keysyms constant codes.
 * A keysym represents a key from an input device like a keyboard.
 * For more information see the RFB Protocol specification.
 */
public interface KeysymConstants {
	
	
	
	public static int XK_SHIFT_L		= 0xFFE1	; /* Left shift */
	public static int XK_SHIFT_R		= 0xFFE2	; /* Right shift */
	public static int XK_CONTROL_L		= 0xFFE3	; /* Left control */
	public static int XK_CONTROL_R		= 0xFFE4	; /* Right control */
	public static int XK_CAPS_LOCK		= 0xFFE5	; /* Caps lock */
	public static int XK_SHIFT_LOCK		= 0xFFE6	; /* Shift lock */

	public static int XK_META_L			= 0xFFE7	; /* Left meta */
	public static int XK_META_R			= 0xFFE8	; /* Right meta */
	public static int XK_ALT_L			= 0xFFE9	; /* Left alt */
	public static int XK_ALT_R			= 0xFFEA	; /* Right alt */
	public static int XK_SUPER_L		= 0xFFEB	; /* Left super */
	public static int XK_SUPER_R		= 0xFFEC	; /* Right super */
	public static int XK_HYPER_L		= 0xFFED	; /* Left hyper */
	public static int XK_HYPER_R		= 0xFFEE	; /* Right hyper */
	
	
	

}
