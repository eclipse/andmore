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
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.network;


public class VNCProtocolData {

	private IVNCPainter vncPainter;
	private int fbWidth;
	private int fbHeight;
	private PixelFormat pixelFormat;
	private String serverName;
	private boolean paintEnabled;
	private String password;

	public String getServerName() {
		return serverName;
	}

	/**
	 * Returns the IVNCPainter assigned to this client.
	 */
	public IVNCPainter getVncPainter() {
		return vncPainter;
	}

	public PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	public int getFbWidth() {
		return fbWidth;
	}

	public int getFbHeight() {
		return fbHeight;
	}

	public String getPassword() {
		return password;
	}

	public boolean isPaintEnabled() {
		return paintEnabled;
	}

	/**
	 * Sets the vncPainter value.
	 */
	public void setVncPainter(IVNCPainter vncPainter) {
		this.vncPainter = vncPainter;
	}

	public void setPaintEnabled(boolean paintEnabled) {
		this.paintEnabled = paintEnabled;
	}

	public void setPixelFormat(PixelFormat pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setFbWidth(int fbWidth) {
		this.fbWidth = fbWidth;
	}

	public void setFbHeight(int fbHeight) {
		this.fbHeight = fbHeight;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
