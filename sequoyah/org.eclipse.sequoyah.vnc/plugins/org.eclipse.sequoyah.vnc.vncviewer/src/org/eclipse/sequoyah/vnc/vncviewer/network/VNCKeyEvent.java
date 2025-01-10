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
 * This class is used to represent VNC specific key event information.
 */
public class VNCKeyEvent {
	
	
	/* The keysym variable represents a key. See the VNC Protocol specification for more information */
	private int keysym;
	private boolean pressed;
	
	/**
	 * @param keysym the keysym code.
	 * @param pressed true if the key is pressed, false if it's released.
	 */
	public VNCKeyEvent(int keysym, boolean pressed) {
		
		setKeysym(keysym);
		setPressed(pressed);
		
	}
	
	/**
	 * Returns the associated keysym.
	 */
	public int getKeysym() {
		return keysym;
	}
	
	/**
	 * Sets the keysym value.
	 */
	public void setKeysym(int keysym) {
		this.keysym = keysym;
	}
	
	/**
	 * @return true if the associated key is pressed and false if it is released.
	 */
	public boolean isPressed() {
		return pressed;
	}
	
	/**
	 * Sets the pressed status for the associated key.
	 * @param pressed true if the associated key is pressed, false if it's released. 
	 */
	public void setPressed(boolean pressed) {
		this.pressed = pressed;
	}
	
	

}
