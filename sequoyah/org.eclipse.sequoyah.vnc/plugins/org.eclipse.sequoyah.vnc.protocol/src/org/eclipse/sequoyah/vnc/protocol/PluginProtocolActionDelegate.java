/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo
 *
 * Contributors:
 * Daniel Barboza Franco - Bug [233775] - Does not have a way to enter the session password for the vnc connection
 * Daniel Barboza Franco - Bug [233062] - Protocol connection port is static.
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233064] - Add reconnection mechanism to avoid lose connection with the protocol
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer 
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah 
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol;

import java.util.Collection;
import java.util.Map;

import org.eclipse.sequoyah.vnc.protocol.exceptions.MalformedProtocolExtensionException;
import org.eclipse.sequoyah.vnc.protocol.internal.model.PluginProtocolModel;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolExceptionHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.ProtocolMsgDefinition;

/**
 * DESCRIPTION: This class contains every method call that can be used by a
 * plugin that depends on this one. <br>
 * 
 * RESPONSIBILITY: Provide an unique entry point for actions demanded from
 * elements outside the plugin domain. Note that it is needed to have all the
 * protocol data correctly declared as extensions before using this class.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: Call any of the public static methods to trigger actions on the
 * protocol framework, using the data declared at the extensions of the points
 * declared by this plugin.<br>
 * 
 */
public class PluginProtocolActionDelegate {

	/**
	 * Starts the protocol identified by protocolId, taking part as client in
	 * the communication
	 * 
	 * @param protocolId
	 *            The id of the protocol to run
	 * @param host
	 *            The host where the server is located
	 * 
	 * @return A handle to identify the connection just made
	 * 
	 * @throws MalformedProtocolExtensionException
	 *             DOCUMENT ME!!
	 */
	public static ProtocolHandle requestStartProtocolAsClient(String protocolId,
			IProtocolExceptionHandler exceptionHandler, String host, int port,
			Map<String, Object> parameters) throws MalformedProtocolExtensionException {

		PluginProtocolModel model = PluginProtocolModel.getInstance();
		Map<Long, ProtocolMsgDefinition> allMessages = model
				.getAllProtocolMessages(protocolId);
		Collection<String> incomingMessages = model
				.getServerMessages(protocolId);
		Collection<String> outgoingMessages = model
				.getClientMessages(protocolId);
		IProtocolHandshake protocolInitializer = model
				.getProtocolInit(protocolId);
		boolean isBigEndianProtocol = model.isBigEndianProtocol(protocolId);

		return ProtocolActionDelegate.requestStartProtocolAsClient(allMessages,
				incomingMessages, outgoingMessages, protocolInitializer,
				exceptionHandler, isBigEndianProtocol, host, port, parameters);
	}

	/**
	 * Starts the protocol identified by protocolId, taking part as server in
	 * the communication
	 * 
	 * @param protocolId
	 *            The id of the protocol to run
	 * @param serverPort
	 *            The port where the server will listen to requests
	 * @param exceptionHandler
	 *            An optional custom handler for exceptions caused in the framework            
     *
     * @return A handle to identify the connection just made
	 * 
	 * @throws MalformedProtocolExtensionException
	 *             DOCUMENT ME!!
	 */
	public static ProtocolHandle requestStartProtocolAsServer(String protocolId, int serverPort, 
			IProtocolExceptionHandler exceptionHandler) throws MalformedProtocolExtensionException {

		PluginProtocolModel model = PluginProtocolModel.getInstance();
		Map<Long, ProtocolMsgDefinition> allMessages = model
				.getAllProtocolMessages(protocolId);
		Collection<String> incomingMessages = model
				.getClientMessages(protocolId);
		Collection<String> outgoingMessages = model
				.getServerMessages(protocolId);
		IProtocolHandshake protocolInitializer = model
				.getProtocolInit(protocolId);
		boolean isBigEndianProtocol = model.isBigEndianProtocol(protocolId);

		return ProtocolActionDelegate.requestStartProtocolAsServer(serverPort, allMessages,
				incomingMessages, outgoingMessages, protocolInitializer,
				exceptionHandler, isBigEndianProtocol);
	}

	/**
	 * Stops the provided protocol instance
	 * 
	 * @param handle
	 *            An object provided at the connection time, that identifies 
	 *            the connection that is to be stopped
	 */
	public static void requestStopProtocol(ProtocolHandle handle) {

		ProtocolActionDelegate.requestStopProtocol(handle);
	}

	/**
	 * Restarts the provided protocol instance
	 * 
	 * @param handle
	 *            An object provided at the connection time, that identifies 
	 *            the connection that is to be restarted
	 * 
	 */
	public static void requestRestartProtocol(ProtocolHandle handle) {

		ProtocolActionDelegate.requestRestartProtocol(handle);
	}

	/**
	 * Sends a message to the server part
	 * 
	 * @param handle
	 *            An object provided at the connection time, that identifies 
	 *            the client connection through which a message will be sent 
	 *            to the server to which it is connected
	 * 
	 * @param message
	 *            The message to send to the server
	 * 
	 */
	public static void sendMessageToServer(
			ProtocolHandle handle, ProtocolMessage message) {

		ProtocolActionDelegate
				.sendMessageToServer(handle, message);
	}
	
	/**
	 * Tests if the protocol identified by handle is running or not
	 * 
	 * @param handle The handle that identifies a protocol instance
	 * 
	 * @return True if the protocol is running, false otherwise
	 */
	public static boolean isProtocolRunning(ProtocolHandle handle) {
	    return ProtocolActionDelegate.isProtocolRunning(handle);
	}
}
