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
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.internal.model;

import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;

/**
 * DESCRIPTION: This class represents a bean that holds data retrieved from the
 * Protocol Definition extensions. <br>
 * 
 * RESPONSIBILITY: Store and provide data regarding a protocol definition.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: The framework sets the data according to user extension declarations.
 * Use the getter methods to retrieve that data.<br>
 * 
 */
public class ProtocolBean {

	// Element fields
	private String protocolId;
	private String parentProtocol;
	private boolean isBigEndianProtocol;
	private IProtocolHandshake protocolInitSeed;

	/*
	 * Setters section
	 */
	public void setProtocolId(String protocolId) {
		this.protocolId = protocolId;
	}

	public void setParentProtocol(String parentProtocol) {
		this.parentProtocol = parentProtocol;
	}

	public void setBigEndianProtocol(boolean isBigEndianProtocol) {
		this.isBigEndianProtocol = isBigEndianProtocol;
	}

	public void setProtocolInitSeed(
			IProtocolHandshake protocolInitSeed) {
		this.protocolInitSeed = protocolInitSeed;
	}

	/*
	 * Getters section
	 */
	public String getProtocolId() {
		return protocolId;
	}

	public String getParentProtocol() {
		return parentProtocol;
	}

	public boolean isBigEndianProtocol() {
		return isBigEndianProtocol;
	}

	public IProtocolHandshake getProtocolInit() {

		// Use reflection to guarantee that every time the method is invoked
		// a new instance of the class will be created to return to the user.
		// The "seed" object (the one created by the extension framework) is
		// kept intact
		Class<? extends IProtocolHandshake> classObj = protocolInitSeed
				.getClass();
		IProtocolHandshake newInstance = null;
		try {
			newInstance = classObj.newInstance();
		} catch (Exception e) {
			// TODO This is a temporary exception handling
			e.printStackTrace();
		}

		return newInstance;
	}
}
