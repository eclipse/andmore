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
package org.eclipse.sequoyah.vnc.protocol.lib.msgdef;

import java.util.List;

import org.eclipse.sequoyah.vnc.protocol.lib.IMessageHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.IMsgDataBean;

/**
 * DESCRIPTION: This class is the bean that represents a message definition
 * element. It contains setters and getters to all information contained at the
 * element specification. <br>
 * 
 * RESPONSIBILITY: Hold and provide data regarding a whole message definition,
 * including all its data fields
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: The class is intended to be instantiated when creating message
 * definitions. By using the information contained here the protocol engine
 * class is able to parse and serialize a message.<br>
 * 
 */
public class ProtocolMsgDefinition {
	// Element fields, as defined in the ProtocolMessage extension point
	private String id;
	private long code;
	private boolean isMsgCodeSigned;
	private int msgCodeSizeInBytes;
	private IMessageHandler handler;
	private List<IMsgDataBean> messageData;

	/*
	 * Getters section
	 */
	public String getId() {
		return id;
	}

	public long getCode() {
		return code;
	}

	public boolean isMsgCodeSigned() {
		return isMsgCodeSigned;
	}

	public int getMsgCodeSizeInBytes() {
		return msgCodeSizeInBytes;
	}

	public IMessageHandler getHandler() {
		return handler;
	}

	public List<IMsgDataBean> getMessageData() {
		return messageData;
	}

	/*
	 * Setters section
	 */
	public void setId(String id) {
		this.id = id;
	}

	public void setCode(long code) {
		this.code = code;
	}

	public void setMsgCodeSigned(boolean isMsgCodeSigned) {
		this.isMsgCodeSigned = isMsgCodeSigned;
	}

	public void setMsgCodeSizeInBytes(int msgCodeSizeInBytes) {
		this.msgCodeSizeInBytes = msgCodeSizeInBytes;
	}

	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}

	public void setMessageData(List<IMsgDataBean> messageData) {
		this.messageData = messageData;
	}

}
