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
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;

/**
 * DESCRIPTION: This interface describes the contract to be used by an object
 * which purpose is to handle a raw data field received/sent from/to the
 * communication stream. <br>
 * 
 * RESPONSIBILITY: Define methods that shall be implemented by objects that
 * read/write a raw data field from/to the communication stream.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: The message definition contributors must provide a handler to raw data
 * fields if the message contains such fields. The raw data handlers must
 * implement this interface.<br>
 * 
 */
public interface IRawDataHandler {

	/**
	 * Reads raw data fields from stream. <br>
	 * <br>
	 * The method provides an input stream from which the reader can retrieve
	 * data from the communication stream, as well as the message fields that
	 * were already retrieved until the invocation time. Besides, the
	 * user has access to a handle of the connection as well as to a 
	 * flag indicating if the protocol is big endian or not.<br>
	 * <br>
	 * The user is intended to read the needed data from the stream and return
	 * the field data in a Map implementation. The map entry key is the field
	 * name, which can be used by the message handler to retrieve the value read
	 * for further processing after this message is fully parsed.
	 * 
	 * If this protocol implementation needs only to write the raw field, this
	 * method can be left blank.
	 * 
	 * @param handle
	 *            The object that identifies by from which connection the 
	 *            data must be read from. This can be used, for example, to 
	 *            associate the connection to any user model that needs updating
	 * @param dataStream
	 *            The communication stream where to read bytes from.
	 * @param fields
	 *            All message fields already read from the stream until the
	 *            current moment.
	 * @param isBigEndian
	 *            True if the protocol is big endian; false if it is little
	 *            endian.
	 * 
	 * @return A map containing the raw data fields already read. The keys must
	 *         match the field name that will be retrieved out of the protocol
	 *         message in the message handler.
	 * 
	 * @throws IOException
	 *             If an IO error occurs while trying to read from the input
	 *             stream.
	 * @throws ProtocolRawHandlingException
	 *             If the raw field cannot be parsed.
	 */
	Map<String, Object> readRawDataFromStream(ProtocolHandle handle, 
			DataInput dataStream, IMessageFieldsStore fields, 
			boolean isBigEndian) throws IOException, ProtocolRawHandlingException;

	/**
	 * Writes raw data fields to stream. <br>
	 * <br>
	 * The method provides an output stream to which the writer can send data to
	 * the communication stream, as well as all the protocol message fields.
	 * Besides, the implementor has access to a handle of the connection as well 
	 * as to a flag indicating if the protocol is big endian or not.
	 * 
	 * If this protocol implementation needs only to read the raw field, this
	 * method can be left blank.
	 * 
	 * @param handle
	 *            The object that identifies by to which connection the 
	 *            data must be written to. This can be used, for example, to 
	 *            associate the connection to any user model that needs updating
	 * @param dataStream
	 *            The stream where to write bytes.
	 * @param messageToGetInformationFrom
	 *            The message that shall be queried for data to write to stream
	 * @param isBigEndian
	 *            True if the protocol is big endian; false if it is little
	 *            endian.
	 * 
	 * @throws ProtocolRawHandlingException
	 *             If the method fails due to protocol logic error (eg. If the
	 *             protocol message do not have necessary data or have
	 *             incomplete/erroneous data).
	 */
	void writeRawDataToStream(ProtocolHandle handle, ByteArrayOutputStream dataStream, 
			ProtocolMessage messageToGetInformationFrom, boolean isBigEndian)
			throws ProtocolRawHandlingException;
}
