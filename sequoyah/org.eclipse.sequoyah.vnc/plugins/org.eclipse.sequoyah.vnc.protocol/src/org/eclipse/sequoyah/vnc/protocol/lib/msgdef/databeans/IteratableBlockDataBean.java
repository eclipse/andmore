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

import java.util.Collection;

/**
 * DESCRIPTION: This class is the bean that represents a iteratable block data
 * element. It contains setters and getters to all information contained at the
 * element specification. <br>
 * 
 * RESPONSIBILITY: Hold and provide data regarding a iteratable block data
 * element
 * 
 * COLABORATORS: IMsgDataBean: interface being implemented.<br>
 * ProtocolMsgDefinition: The bean the represents the entire message definition.
 * It contains several instances of IMsgDataBean, including this one.<br>
 * 
 * USAGE: The class is intended to be instantiated when creating message
 * definitions. By using the information contained here the protocol engine
 * class is able to parse and serialize an iteratable block element defined by
 * the message definition.<br>
 * 
 */
public class IteratableBlockDataBean implements IMsgDataBean {

	// Element fields, as defined in the ProtocolMessage extension point
	private String id;
	private String iterateOnField;
	private Collection<IMsgDataBean> dataBeans;

	/*
	 * Getters section
	 */
	public String getIterateOnField() {
		return iterateOnField;
	}

	public Collection<IMsgDataBean> getDataBeans() {
		return dataBeans;
	}

	public String getId() {
		return id;
	}

	/*
	 * Setters section
	 */
	public void setId(String id) {
		this.id = id;
	}

	public void setIterateOnField(String iterateOnField) {
		this.iterateOnField = iterateOnField;
	}

	public void setDataBeans(Collection<IMsgDataBean> dataBeans) {
		this.dataBeans = dataBeans;
	}
}
