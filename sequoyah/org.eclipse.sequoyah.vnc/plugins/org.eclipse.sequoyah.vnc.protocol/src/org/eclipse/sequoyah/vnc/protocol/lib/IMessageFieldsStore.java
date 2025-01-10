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
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246916] - Add the correct Number objects to ProtocolMessage objects on reading from input stream
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

import java.util.Collection;

/**
 * DESCRIPTION: This interface describes the contract to be followed by an
 * object which purpose is to store message fields. <br>
 * 
 * RESPONSIBILITY: Define methods that shall be implemented by objects that
 * store message fields, so that a user can query for field values.<br>
 * 
 * COLABORATORS: MessageFieldsStore: default implementor of the interface.<br>
 * 
 * USAGE: The class is intended to be instantiated by the protocol
 * framework. The user can query the IMessageFieldsStore for message field
 * values by using any of the method of this interface.<br>
 * 
 */
public interface IMessageFieldsStore {
	/**
	 * Retrieves the names of all fields currently being stored by the class.
	 * 
	 * @return A collection containing all message fields names.
	 */
	Collection<String> getAllFieldNames();

	/**
	 * Retrieves the value of a field, given its name. <br>
	 * <br>
	 * The field is looked up in the store with the name as provided. This
	 * method is able to retrieve fields registered by iteratable blocks as long
	 * as their internal names are provided. In case their internal names are
	 * not known, one can use the other <code>getFieldValue</code> version in
	 * this interface to retrieve their values.
	 * 
	 * @param fieldName
	 *            The name of the field to retrieve value from.
	 * 
	 * @return The value of the field identified by <i>fieldName</i>.
	 */
	Object getFieldValue(String fieldName);

	/**
	 * Retrieves the value of a field that was registered by an iteratable
	 * block. <br>
	 * <br>
	 * Iteratable blocks are so-called the subsets of a message that can be sent
	 * or received multiple times at the stream. The quantity of iterations are
	 * defined by another field in the message, which is read prior to the block
	 * from the stream.
	 * 
	 * @param fieldName
	 *            The name of the field to retrieve value from.
	 * @param iterableBlockId
	 *            The id of the iteratable block, as defined in the message
	 *            definition
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
	 * 
	 * @return The value of the field identified by <i>fieldName</i>,
	 *         <i>iterableBlockId</i> and <i>index</i>.
	 */
	Object getFieldValue(String fieldName, String iterableBlockId, int index);
	
	/**
	 * Retrieves the size of a field, given its name. <br>
	 * <br>
	 * The field is looked up in the store with the name as provided. This
	 * method is able to retrieve fields registered by iteratable blocks as long
	 * as their internal names are provided. In case their internal names are
	 * not known, one can use the other <code>getFieldValue</code> version in
	 * this interface to retrieve their values.
	 * 
	 * @param fieldName
	 *            The name of the field to retrieve size from.
	 * 
	 * @return The size of the field identified by <i>fieldName</i>.
	 */
	Object getFieldSize(String fieldName);

	/**
	 * Retrieves the size of a field that was registered by an iteratable
	 * block. <br>
	 * <br>
	 * Iteratable blocks are so-called the subsets of a message that can be sent
	 * or received multiple times at the stream. The quantity of iterations are
	 * defined by another field in the message, which is read prior to the block
	 * from the stream.
	 * 
	 * @param fieldName
	 *            The name of the field to retrieve size from.
	 * @param iterableBlockId
	 *            The id of the iteratable block, as defined in the message
	 *            definition
	 * @param index
	 *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
	 * 
	 * @return The size of the field identified by <i>fieldName</i>,
	 *         <i>iterableBlockId</i> and <i>index</i>.
	 */
	Object getFieldSize(String fieldName, String iterableBlockId, int index);
	
}
