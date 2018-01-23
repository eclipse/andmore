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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans;

import org.eclipse.sequoyah.vnc.protocol.lib.IRawDataHandler;

/**
 * DESCRIPTION: This class is the bean that represents a raw data element. It
 * contains setters and getters to all information contained at the element
 * specification. <br>
 * 
 * RESPONSIBILITY: Hold and provide data regarding a raw data element
 * 
 * COLABORATORS: IMsgDataBean: interface being implemented.<br>
 * ProtocolMsgDefinition: The bean the represents the entire message definition.
 * It contains several instances of IMsgDataBean, including this one.<br>
 * 
 * USAGE: The class is intended to be instantiated when creating message
 * definitions. By using the information contained here the protocol engine
 * class is able to parse and serialize a raw data element defined by the
 * message definition.<br>
 * 
 */
public class RawDataBean implements IMsgDataBean {
	// Element field, as defined in the ProtocolMessage extension point
	private IRawDataHandler handler;

	/*
	 * Getters section
	 */
	public IRawDataHandler getHandler() {
		return handler;
	}

	/*
	 * Setters section
	 */
	public void setHandler(IRawDataHandler handler) {
		this.handler = handler;
	}
}
