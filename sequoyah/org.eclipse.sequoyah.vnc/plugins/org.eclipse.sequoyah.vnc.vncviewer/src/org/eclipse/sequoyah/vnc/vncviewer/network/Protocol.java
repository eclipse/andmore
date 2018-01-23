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
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.network;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * An abstract class that defines the main behavior of a remote desktop protocol.
 */
abstract public class Protocol implements IProtoClient {

	
	protected Socket socket;
	protected DataInputStream in = null;
	protected OutputStream out = null;
	protected boolean paintEnabled = false;
	
	protected String host;
	protected int port;
	protected String password;

	/**
	 * Open a connection to the specified server.
	 * @param host the host String.
	 * @param port the server port.
	 */
	protected void openConnection(String host, int port, String password) throws Exception {

		this.host = host;
		this.port = port;
		this.password = password;

		socket = new Socket(host, port);
		socket.setReceiveBufferSize(1024*64);
		
		in = new DataInputStream(socket.getInputStream());
		out = socket.getOutputStream();
	}

	
	
	
	
	
	/**
	 * Open a connection to the specified server.
	 * @param host the host String.
	 * @param port the server port.
	 * @param timeout the connection timeout in milliseconds
	 */
	protected void openConnection(String host, int port, int timeout) throws Exception {

		InetSocketAddress socketAdress = new InetSocketAddress(host, port);
		
		socket = new Socket();
		socket.connect(socketAdress, timeout);
		

		in = new DataInputStream(socket.getInputStream());
		out = socket.getOutputStream();
	}
	
	
	
	/**
	 * Close the connection to the current server.
	 */
	protected void closeConnection() throws Exception {
		out.close();
		in.close();
		socket.close();
	}
	
	
	/**
	 * Return the connection's state.
	 * @return true if the connection is alive, false if not.
	 */
	public boolean isConnected(){
		boolean isConnected = false;
		
		if ((socket != null) && (socket.isConnected()) && (!socket.isClosed()) ) {
			isConnected = true;
		}
		
		return isConnected;
	}
	
	
	
	public boolean isPaintEnabled() {
		return paintEnabled;
	}
	

	public void setPaintEnabled(boolean enabled) {
		paintEnabled = enabled;
	}
	
}

