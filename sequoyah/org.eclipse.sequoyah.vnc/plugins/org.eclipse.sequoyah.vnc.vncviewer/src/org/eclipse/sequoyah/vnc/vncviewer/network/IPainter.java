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
 * This interface defines the main behavior of an object used to render the 
 * framebuffer representation inside the protocol.
 */
public interface IPainter {

	
	/**
	 * Sets the screen size.
	 * @param width the screen's width
	 * @param height the screen's height
	 */
	public void setSize(int width, int height);

	/**
	 * Return the screen width.
	 */
	public int getWidth();


	/**
	 * Return the screen height.
	 */
	public int getHeight();
	

	
	
}
