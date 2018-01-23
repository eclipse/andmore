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
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer 
 * Daniel Barboza Franco (Eldorado Research Institute) - [275650] - Canvas rotation
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.graphics;

import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;
import org.eclipse.swt.widgets.Event;

/**
 * This interface defines the default behavior of a Remote Display component.
 * <br>
 * <br>
 * Classes implementing this Interface must extend the widget container
 * corresponding to the specific tool kit. Example: SWT - implementors using SWT
 * must extend the Composite class.
 */
public interface IRemoteDisplay {
	
	
	public enum Rotation {
		ROTATION_0DEG (0),
		ROTATION_90DEG_CLOCKWISE (90),
		ROTATION_90DEG_COUNTERCLOCKWISE (-90),
		ROTATION_180DEG (180);
		
		private int value;
		
		private Rotation(int value) {
			this.value = value;
		}
		
		public int value(){
			return this.value;
		}
		
	}
	

	/**
	 * Creates the connection to the server using the protocol specified.
	 * 
	 * @param handle
	 *            A handle to identify the connection made through the protocol
	 *            plugin
	 */
	public void start(ProtocolHandle handle) throws Exception;

	/**
	 * Stops the connection with the server.
	 */
	public void stop();

	/**
	 * Restarts the VNCDisplay respecting the number of retries specified.
	 */
	public void restart() throws Exception;

	/**
	 * Requests a screen update to the associated server.
	 */
	public void updateScreen() throws Exception;

	/**
	 * Reports to the server that a key event occurred at the client's side.
	 * 
	 * @param event
	 *            the associated event.
	 */
	public void keyEvent(Event event) throws Exception;

	/**
	 * Returns the screen's width.
	 */
	public int getScreenWidth();

	/**
	 * Returns the screen's height.
	 */
	public int getScreenHeight();

	/**
	 * Returns the rotation applied to the canvas.
	 * @return The amount of degrees. This value is one of {-90, 0, 90, 180}
	 */
	public Rotation getRotation();

	/**
	 *  Set the amount of degrees used to rotate the canvas. Valid values are: -90, 0, 90, 180
	 */
	public void setRotation(Rotation degrees);
	
	
	/**
	 * Returns the Display status.
	 */
	public boolean isActive();

	/**
	 * Returns the VNCProtocolData associated to the Display.
	 */
	public VNCProtocolData getProtocolData();

	public void setPropertiesFileHandler(
			IPropertiesFileHandler propertiesFileHandler);

	// public IVNCPainter getPainter();
}
