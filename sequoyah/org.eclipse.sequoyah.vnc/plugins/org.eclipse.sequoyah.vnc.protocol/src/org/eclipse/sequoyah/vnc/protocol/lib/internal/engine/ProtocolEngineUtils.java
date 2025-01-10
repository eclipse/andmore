/********************************************************************************
 * Copyright (c) 2009 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.internal.engine;

import java.util.Collection;

/**
 * DESCRIPTION: This class provides utility methods that can be used by any 
 * engine related class. <br>
 * 
 * RESPONSIBILITY: Provide additional services to other classes, that are not related
 * to a single class needs.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: Those methods are meant to be used by engine classes only. <br>
 * 
 */
class ProtocolEngineUtils
{   
    /**
     * Tests if a given message id is an outgoing message
     * 
     * @param messageId
     *            The id to be tested
     * @param incomingMessages A collection of the incoming messages ids, used to validate 
     *            if a message can be retrieved from the input stream.
     * @param outgoingMessages A collection of the outgoing messages ids, used to validate 
     *            if a message can be sent through the output stream.
     * 
     * @return True if the message is defined as outgoing message. False
     *         otherwise
     */
    static boolean isOutgoingMessage(String messageId, Collection<String> incomingMessages, 
            Collection<String> outgoingMessages) {
        boolean isOutgoingMessage = false;
        if ((outgoingMessages.contains(messageId))
                && (!incomingMessages.contains(messageId))) {
            isOutgoingMessage = true;
        }

        return isOutgoingMessage;
    }
    
    /**
     * Tests if a given message id is an incoming message
     * 
     * @param messageId
     *            The id to be tested
     * @param incomingMessages A collection of the incoming messages ids, used to validate 
     *            if a message can be retrieved from the input stream.
     * @param outgoingMessages A collection of the outgoing messages ids, used to validate 
     *            if a message can be sent through the output stream.
     * 
     * @return True if the message is defined as incoming message. False
     *         otherwise
     */
    static boolean isIncomingMessage(String messageId, Collection<String> incomingMessages, 
            Collection<String> outgoingMessages) {
        boolean isIncomingMessage = false;
        if ((incomingMessages.contains(messageId))
                && (!outgoingMessages.contains(messageId))) {
            isIncomingMessage = true;
        }

        return isIncomingMessage;
    }
}
