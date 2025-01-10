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
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.MessageHandleException;

/**
 * DESCRIPTION: This interface describes the contract to be used by an object
 * which purpose is to handle a protocol message. <br>
 * 
 * RESPONSIBILITY: Define methods that shall be implemented by objects that
 * handles a protocol message.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: The message contributors must provide a handler to the message, which
 * must implement this interface.<br>
 * 
 */
public interface IMessageHandler {

	/**
	 * Handles a protocol message after it has been completely received and
	 * parsed.
	 * 
	 * @param handle
	 *            The object that identifies by which connection the 
	 *            message was received.
	 * @param message
	 *            The object that contains all the message parsed fields.
	 * 
	 * @return If a message should be sent in response to the handled one,
	 *         return the response message. If not, return <code>null</code>.
	 * 
	 * @throws MessageHandleException
	 *             If the message cannot be handled.
	 */
	ProtocolMessage handleMessage(ProtocolHandle handle, ProtocolMessage message) throws MessageHandleException;
}
