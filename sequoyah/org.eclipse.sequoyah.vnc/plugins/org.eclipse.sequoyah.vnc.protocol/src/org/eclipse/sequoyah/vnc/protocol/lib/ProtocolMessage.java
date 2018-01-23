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
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246916] - Add the correct Number objects to ProtocolMessage objects on reading from input stream
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

/**
 * DESCRIPTION: This class is the default implementation of a protocol message.
 * <br>
 * 
 * RESPONSIBILITY: Provide a default object for storing message fields data.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: The class can be instantiated by the protocol framework or by the
 * user. The protocol framework will instantiate it to parse the data from the
 * communication stream and provide it to the message handler. The user, on the
 * other hand, can instantiate it from any part of his/her code and call the
 * ProtocolActionDelegate.sendMessageToServer() method or instantiate it from a
 * message handler and return it as a response to the handled message (see
 * IMessageHandler interface).<br>
 * <br>
 * 
 * Any applicable fields can be set by means of the setter methods in the
 * interface. The getter methods described at the IMessageFieldsStore interface
 * (indirectly implemented by this class) can be used to retrieve the fields
 * values.
 */
public class ProtocolMessage extends MessageFieldsStore {

	/**
	 * The code that identifies the message.
	 */
	private long code;

	/**
	 * Default constructor.<br>
	 * Creates a message representing the message of the given code
	 * 
	 * @param code
	 *            The code that identifies the message.
	 */
	public ProtocolMessage(long code) {
		setCode(code);
	}

	/**
	 * Sets the message code. This code is used to identify the message when
	 * serializing/deserializing from the communication streams.
	 */
	public void setCode(long code) {
		this.code = code;
	}

	/**
	 * Gets the message code. This code is used to identify the message when
	 * serializing/deserializing from the communication streams.
	 */
	public long getCode() {
		return code;
	}

	/**
	 * Sets the a field value at the message object. The protocol framework sets
	 * the field values when it reads the messages from the input stream for
	 * later use in the message handlers. The user must set field values for the
	 * protocol framework to access the data to write to the communication
	 * channel when sending the message.
	 * 
	 * @param fieldName
	 *            The name of the field, as defined in the message definition
	 * @param fieldValue
	 *            The value to be set under <i>fieldName</i> key.
	 */
	public void setFieldValue(String fieldName, Object fieldValue) {
		doSetFieldValue(fieldName, getIteratableBlockId(), getIndex(),
				fieldValue);
	}

	/**
	 * Sets the a field value at the message object. The protocol framework sets
	 * the field values when it reads the messages from the input stream for
	 * later use in the message handlers. The user must set field values for the
	 * protocol framework to access the data to write to the communication
	 * channel when sending the message.
	 * 
	 * @param fieldName
	 *            The name of the field, as defined in the message definition
	 * @param iterableBlockId
	 *            The id of the iteratable block, as defined in the message
	 *            definition
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
	 * @param fieldValue
	 *            The value to be set under <i>fieldName</i> key.
	 * 
	 */
	public void setFieldValue(String fieldName, String iterableBlockId,
			int index, Object fieldValue) {
		doSetFieldValue(fieldName, iterableBlockId, index, fieldValue);
	}

	/**
	 * Sets a generic field value (either fixed, variable or raw).<br>
	 * This method have two different kinds of treatments to iteratable blocks.
	 * If the parameters iterableBlockId and index are reset, then it uses the
	 * field name as-is as the key. On the other hand, if the previous
	 * parameters are set, then it uses the parents' <code>generateKey</code>
	 * method for creating an internal key representation for the iteration
	 * block value.
	 * 
	 * @see MessageFieldsStore#generateKey(String, String, int)
	 * 
	 * @param fieldName
	 *            The name of the field that contains the value to be set.
	 * @param iterableBlockId
	 *            The id of the iteratable block, as defined in the message
	 *            definition
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1).
	 * @param fieldValue
	 *            The value to be set under <i>fieldName</i> key.
	 */
	private void doSetFieldValue(String fieldName, String iterableBlockId,
			int index, Object fieldValue) {
		if ((iterableBlockId != null) && (index >= 0)) {
			messageFieldsValues.put(generateKey(fieldName, iterableBlockId,
					index), fieldValue);
		} else {
			messageFieldsValues.put(fieldName, fieldValue);
		}
	}
	
	

	/**
	 * Sets the size of a field at the message object. The protocol framework sets
	 * the field size when it reads the messages from the input stream for
	 * later use in the message handlers. 
	 * 
	 * @param fieldName
	 *            The name of the field, as defined in the message definition
	 * @param fieldSize
	 *            The size to be set under <i>fieldName</i> key.
	 */
	public void setFieldSize(String fieldName, int fieldSize) {
		doSetFieldSize(fieldName, getIteratableBlockId(), getIndex(),
				fieldSize);
	}
	
	/**
	 * Sets the size of a field at the message object. The protocol framework sets
	 * the field sizes when it reads the messages from the input stream for
	 * later use in the message handlers.
	 * 
	 * @param fieldName
	 *            The name of the field, as defined in the message definition
	 * @param iterableBlockId
	 *            The id of the iteratable block, as defined in the message
	 *            definition
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
	 * @param fieldSize
	 *            The size to be set under <i>fieldName</i> key.
	 * 
	 */
	public void setFieldSize(String fieldName, String iterableBlockId,
			int index, int fieldSize) {
		doSetFieldSize(fieldName, iterableBlockId, index, fieldSize);
	}
	
	private void doSetFieldSize(String fieldName, String iterableBlockId,
			int index, int fieldSize) {
		if ((iterableBlockId != null) && (index >= 0)) {
			messageFieldsSizes.put(generateKey(fieldName, iterableBlockId,
					index), fieldSize);
		} else {
			messageFieldsSizes.put(fieldName, fieldSize);
		}
	}
	
}
