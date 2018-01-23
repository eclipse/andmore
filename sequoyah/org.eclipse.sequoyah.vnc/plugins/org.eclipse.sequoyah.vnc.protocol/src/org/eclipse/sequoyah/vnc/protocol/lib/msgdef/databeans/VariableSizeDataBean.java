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
 * Fabio Rigo - Bug [242757] - Protocol does not support Unicode on variable sized fields
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [242924] - There is no way to keep the size of a Variable Size Data read
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans;

/**
 * DESCRIPTION: This class is the bean that represents a variable size data
 * element. It contains setters and getters to all information contained at the
 * element specification. <br>
 * 
 * RESPONSIBILITY: Hold and provide data regarding a variable size data element
 * 
 * COLABORATORS: IMsgDataBean: interface being implemented.<br>
 * ProtocolMsgDefinition: The bean the represents the entire message definition.
 * It contains several instances of IMsgDataBean, including this one.<br>
 * 
 * USAGE: The class is intended to be instantiated when creating message
 * definitions. By using the information contained here the protocol engine
 * class is able to parse and serialize a variable size data element defined by
 * the message definition.<br>
 * 
 */
public class VariableSizeDataBean implements IMsgDataBean {
	// Element fields, as defined in the ProtocolMessage extension point
	private String sizeFieldName;
	private boolean isSizeFieldSigned;
	private int sizeFieldSizeInBytes;
	private String valueFieldName;
	private String value;
	private String charsetName;

    /*
	 * Getters section
	 */
	public boolean isSizeFieldSigned() {
		return isSizeFieldSigned;
	}

	public int getSizeFieldSizeInBytes() {
		return sizeFieldSizeInBytes;
	}

	public String getValueFieldName() {
		return valueFieldName;
	}

    public String getCharsetName() {
        return charsetName;
    }
	
	public String getValue() {
		return value;
	}
	
	public String getSizeFieldName() {
		return sizeFieldName;
	}
	

	/*
	 * Setters section
	 */
	public void setValue(String value) {
		this.value = value;
	}

	public void setSizeFieldSigned(boolean isSizeFieldSigned) {
		this.isSizeFieldSigned = isSizeFieldSigned;
	}

	public void setSizeFieldSizeInBytes(int sizeFieldSizeInBytes) {
		this.sizeFieldSizeInBytes = sizeFieldSizeInBytes;
	}

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }
	
	public void setValueFieldName(String valueFieldName) {
		this.valueFieldName = valueFieldName;
	}

	public void setSizeFieldName(String sizeFieldName) {
		this.sizeFieldName = sizeFieldName;
	}
}
