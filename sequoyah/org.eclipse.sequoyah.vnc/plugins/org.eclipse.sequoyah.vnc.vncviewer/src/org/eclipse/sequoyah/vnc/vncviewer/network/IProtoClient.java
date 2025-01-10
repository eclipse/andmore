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

import org.eclipse.sequoyah.vnc.vncviewer.exceptions.ProtoClientException;

public interface IProtoClient {
	
	/**
	 * Starts the protocol. This method sends input events, requests display updates and processes the received data.
	 * @param host the host String. 
	 * @param port the server port number.
	 */
	void runProtocol(String host, int port, String password) throws Exception;
	
	/**
	 * Performs the restart of a running Protocol instance, using the same port and host predefined in the run method.
	 */
	void restartProtocol() throws Exception;
	
	/**
	 *  Stops the protocol execution. Closes the connection to the server and kills all running threads.
	 * @throws ProtoClientException 
	 */
	void stopProtocol() throws ProtoClientException;
	
	/**
	 * Send to the server a key event fired at the client's side.
	 */
	void keyEvent(VNCKeyEvent event) throws Exception;
	
	/**
	 * Send to the server a key event fired at the client's side.
	 */
	void mouseEvent(VNCMouseEvent event) throws Exception;

	/**
	 * Performs the framebuffer update request
	 * @param x requested area top level corner X position
	 * @param y requested area top level corner Y position
	 * @param width requested area width
	 * @param height requested area height
	 * @param incremental incremental flag
	 * @throws Exception
	 */
	void fbUpdateRequest(int x, int y, int width, int height, boolean incremental) throws Exception;

	/**
	 * Performs the whole framebuffer update request.
	 * It's equivalent to call <code>fbUpdateRequest(0, 0, getWidth(), getHeight(), incremental)</code> 
	 */
	void fbUpdateRequest(boolean incremental) throws Exception;

	/**
	 * Returns the current server name.
	 */
	String getServerName();
	
	/**
	 * Returns the screen height. 
	 */
	int getHeight();
	
	/**
	 * Returns the screen width.
	 */
	int getWidth();

	void setPainter(IVNCPainter painter);
	
	/**
	 * Flag indicating whether the paint methods are enabled or not.
	 * Used when the protocol is running but there is no UI object to render the images.  
	 */
	boolean isPaintEnabled();
	
	/**
	 * Set the paintEnabled flag value.
	 */
	void setPaintEnabled(boolean enabled);
	
}

