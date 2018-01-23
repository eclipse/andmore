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

/**
 * DESCRIPTION: This class is the bean that represents a fixed size data
 * element. It contains setters and getters to all information contained at the
 * element specification. <br>
 * 
 * RESPONSIBILITY: Hold and provide data regarding a fixed size data element
 * 
 * COLABORATORS: IMsgDataBean: interface being implemented.<br>
 * ProtocolMsgDefinition: The bean the represents the entire message definition.
 * It contains several instances of IMsgDataBean, including this one.<br>
 * 
 * USAGE: The class is intended to be instantiated when creating message
 * definitions. By using the information contained here the protocol engine
 * class is able to parse and serialize a fixed size data element defined by the
 * message definition.<br>
 * 
 */
public class FixedSizeDataBean implements IMsgDataBean {

	// Element fields
	private String fieldName;
	private boolean isFieldSigned;
	private int fieldSizeInBytes;
	private Integer value;

	/*
	 * Getters section
	 */
	public Integer getValue() {
		return value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public boolean isFieldSigned() {
		return isFieldSigned;
	}

	public int getFieldSizeInBytes() {
		return fieldSizeInBytes;
	}

	/*
	 * Setters section
	 */
	public void setValue(Integer value) {
		this.value = value;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public void setFieldSigned(boolean isFieldSigned) {
		this.isFieldSigned = isFieldSigned;
	}

	public void setFieldSizeInBytes(int fieldSizeInBytes) {
		this.fieldSizeInBytes = fieldSizeInBytes;
	}
}
