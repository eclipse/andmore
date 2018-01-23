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
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DESCRIPTION: This class is the default implementation of the
 * IMessageFieldsStore interface. <br>
 * 
 * RESPONSIBILITY: Provide a default object for storing message fields data.<br>
 * 
 * COLABORATORS: IMessageFieldsStore: interface being implemented.<br>
 * 
 * USAGE: The class is intended to be instantiated by the protocol framework.
 * The user can query for message field values by using any of the method of
 * this interface.<br>
 * 
 */
public class MessageFieldsStore implements IMessageFieldsStore {

	/**
	 * Char sequence used in the key generation to isolate the key components.
	 */
	private static final String INTERNAL_SEPARATOR = "$$"; //$NON-NLS-1$

	/**
	 * A map containing all the message fields names and values.
	 */
	protected Map<String, Object> messageFieldsValues = new HashMap<String, Object>();

	/**
	 * A map containing all the message fields names and sizes.
	 */
	protected Map<String, Integer> messageFieldsSizes = new HashMap<String, Integer>();

	
	/**
	 * The identifier of the iteratable block set as default, or
	 * <code>null</code> if chosen not to use a default iteratable block.
	 */
	private String iteratableBlockId;

	/**
	 * The iteration index set as default, or <code>null</code> if chosen not
	 * to use a default iteratable block.
	 */
	private int index;

	/**
	 * The default constructor. <br>
	 * Used when it is chosen not to use a default iteratable block.
	 */
	public MessageFieldsStore() {
		iteratableBlockId = null;
		index = -1;
	}

	/**
	 * Constructor. <br>
	 * Used when it is chosen to use a default iteratable block.
	 * 
	 * <b>Note</b>: Setting a default iteratable block only makes sense if a
	 * message that contains the iteratable block defined is provided. If the
	 * provided message is <code>null</code>, then the constructor ignores
	 * the provided parameters and sets the object state as if was chosen not to
	 * use a default iteratable block.
	 * 
	 * @param message
	 *            The message to create a message fields store object from.
	 *            Should not be <code>null</code>.
	 * @param iteratableBlockId
	 *            The id of the iteratable block, as defined at the
	 *            ProtocolMessage extension and that should be used as default
	 *            in this object.
	 * @param index
	 *            The iteration index that should be used as default in this
	 *            object. Range: 0 ~~ (iteratableBlockLength - 1).
	 */
	public MessageFieldsStore(ProtocolMessage message,
			String iteratableBlockId, int index) {
		if (message != null) {
			this.iteratableBlockId = iteratableBlockId;
			this.index = index;

			for (String fieldName : message.getAllFieldNames()) {
				messageFieldsValues.put(fieldName, message
						.getFieldValue(fieldName));
			}
		} else {
		    this.iteratableBlockId = null;
		    this.index = -1;
		}
	}

	/**
	 * @see IMessageFieldsStore#getAllFieldNames()
	 */
	public Collection<String> getAllFieldNames() {
		return messageFieldsValues.keySet();
	}

	/**
	 * @see IMessageFieldsStore#getFieldValue(String)
	 */
	public Object getFieldValue(String fieldName) {
		Object value = null;

		// If there is a default iteratable block set, use the generateKey
		// method to calculate what is the composite key that may be identifying
		// the field at this iteration
		if ((iteratableBlockId != null) && (index >= 0)) {
			value = messageFieldsValues.get(generateKey(fieldName,
					iteratableBlockId, index));
		}

		// If no value is found by using the composite key or if there is not a
		// default iteratable block set, then use the regular query
		if ((value == null) || (iteratableBlockId == null) || (index < 0)) {
			value = messageFieldsValues.get(fieldName);
		}

		return value;
	}

	/**
	 * @see IMessageFieldsStore#getFieldValue(String, String, int)
	 */
	public Object getFieldValue(String fieldName, String iterableBlockId,
			int index) {
		Object value = null;

		if ((iterableBlockId != null) && (index >= 0)) {
			// If the provided parameters are valid, use the generateKey method
			// to calculate what is the composite key that may be identifying
			// the field for the given iteration.
			value = messageFieldsValues.get(generateKey(fieldName,
					iterableBlockId, index));
		} else {
			// If the provided parameters are invalid, ignore them and use the
			// regular query
			value = messageFieldsValues.get(fieldName);
		}

		return value;
	}

	/**
	 * Generates the internal key for a field that is defined inside an
	 * iteratable block.
	 * 
	 * @param fieldName
	 *            The name of the field in the iteratable block.
	 * @param iterableBlockId
	 *            The id of the iteratable block
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1).
	 * 
	 * @return The internal key.
	 */
	protected String generateKey(String fieldName, String iterableBlockId,
			int index) {
		return (iterableBlockId + INTERNAL_SEPARATOR + fieldName
				+ INTERNAL_SEPARATOR + index);
	}

	/**
	 * Retrieves the default iteratable block id for this message fields store.
	 * 
	 * @return The default iteratable block id or <code>null</code> if chosen
	 *         not to use a default iteratable block.
	 */
	protected String getIteratableBlockId() {
		return iteratableBlockId;
	}

	/**
	 * Retrieves the default iteration index for this message fields store.
	 * 
	 * @return The default iteration index or <code>null</code> if chosen not
	 *         to use a default iteratable block.
	 */
	protected int getIndex() {
		return index;
	}


	/**
	 * @see IMessageFieldsStore#getFieldSize(String)
	 */
	public Integer getFieldSize(String fieldName) {
		Integer size= null;
		
		// If there is a default iteratable block set, use the generateKey
		// method to calculate what is the composite key that may be identifying
		// the field at this iteration
		if ((iteratableBlockId != null) && (index >= 0)) {
			size = messageFieldsSizes.get(generateKey(fieldName,
					iteratableBlockId, index));
		}

		// If no value is found by using the composite key or if there is not a
		// default iteratable block set, then use the regular query
		if ((size == null) || (iteratableBlockId == null) || (index < 0)) {
			size = messageFieldsSizes.get(fieldName);
		}

		return size;
	}

	
	/**
	 * @see IMessageFieldsStore#getFieldSize(String, String, int)
	 */
	public Integer getFieldSize(String fieldName, String iterableBlockId,
			int index) {
		Integer size = null;

		if ((iterableBlockId != null) && (index >= 0)) {
			// If the provided parameters are valid, use the generateKey method
			// to calculate what is the composite key that may be identifying
			// the field for the given iteration.
			size = messageFieldsSizes.get(generateKey(fieldName,
					iterableBlockId, index));
		} else {
			// If the provided parameters are invalid, ignore them and use the
			// regular query
			size = messageFieldsSizes.get(fieldName);
		}

		return size;
	}
}
