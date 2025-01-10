/********************************************************************************
 * Copyright (c) 2009 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.internal.engine;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.vnc.protocol.lib.IRawDataHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.MessageFieldsStore;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidDefinitionException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidInputStreamDataException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidMessageException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.MessageHandleException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.ProtocolMsgDefinition;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.FixedSizeDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.IMsgDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.IteratableBlockDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.RawDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.VariableSizeDataBean;

/**
 * DESCRIPTION: This class constructs ProtocolMessage objects, given a DataInput
 * object and a message definition.<br>
 * 
 * RESPONSIBILITY: Read data from a stream following the specifications defined by 
 * a message definition object, returning an abstraction of a protocol message in 
 * the form of a ProtocolMessage object.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: This class is intended to be used by the consumer thread only.
 * 
 */
class MessageReader
{    
    /**
     * Reads a message from the input stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param code
     *            The code of the message to be read.
     * @param messageDef
     *            The message definition bean that will guide the decoding of
     *            the message fields to be read from the input stream.
     * @param eng
     *            The engine that received the message to be read            
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to decode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to read/write the data from/to the
     *             stream during a raw field handling.
     * @throws MessageHandleException
     *             If the message cannot be handled.
     */
    static ProtocolMessage readReceivedMessage(DataInput in, long code, ProtocolMsgDefinition messageDef, 
            ProtocolEngine eng)
            throws ProtocolRawHandlingException, InvalidDefinitionException,
            InvalidInputStreamDataException, InvalidMessageException,
            MessageHandleException, IOException {
        ProtocolMessage message = null;
        
        // Certifies that the message definition is an incoming message
        // definition.
        if (ProtocolEngineUtils.isIncomingMessage(messageDef.getId(), 
                eng.getIncomingMessages(), eng.getOutgoingMessages())) {

            // Creates a message object to hold the retrieved data.
            message = new ProtocolMessage(code);

            // For each field data bean inside the message definition, filter
            // and read the information.
            List<IMsgDataBean> messageDataDefList = messageDef.getMessageData();
            for (IMsgDataBean messageDataDef : messageDataDefList) {
                readFilterMessageDef(in, messageDataDef, message, null, -1, eng);
            }
        } else {
            BasePlugin.logWarning("The code does not identify an incomming message. It will not be ignored.");//$NON-NLS-1$
        }
        
        return message;
    }
    
    /**
     * Determines which is the provided message data bean type and calls the
     * correct handler for it to perform read operation from the input stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param messageDataDef
     *            The message data object that needs filtering.
     * @param message
     *            The message that is being decoded.
     * @param iterableBlockId
     *            The id of the iteratable block, as defined at the
     *            ProtocolMessage extension.
     * @param index
     *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to decode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to read the data from the stream during
     *             a raw field handling.
     */
    private static void readFilterMessageDef(DataInput in, IMsgDataBean messageDataDef,
            ProtocolMessage message, String iterableBlockId, int index, ProtocolEngine eng)
            throws ProtocolRawHandlingException, InvalidDefinitionException,
            InvalidInputStreamDataException, IOException {
        if (messageDataDef instanceof FixedSizeDataBean) {
            readFixedSizeData(in, (FixedSizeDataBean) messageDataDef, message,
                    iterableBlockId, index, eng);
            FixedSizeDataBean fixedMsgDataDef = (FixedSizeDataBean) messageDataDef;
            message.setFieldSize(fixedMsgDataDef.getFieldName(),
                    iterableBlockId, index, fixedMsgDataDef
                            .getFieldSizeInBytes());

        } else if (messageDataDef instanceof VariableSizeDataBean) {
            readVariableSizeData(in, (VariableSizeDataBean) messageDataDef,
                    message, iterableBlockId, index, eng);
            VariableSizeDataBean variableMsgDataDef = (VariableSizeDataBean) messageDataDef;

            message.setFieldSize(variableMsgDataDef.getSizeFieldName(),
                    iterableBlockId, index, variableMsgDataDef
                            .getSizeFieldSizeInBytes());

        } else if (messageDataDef instanceof RawDataBean) {
            readRawData(in, (RawDataBean) messageDataDef, message, iterableBlockId,
                    index, eng);
        } else {
            readIteratableBlock(in, (IteratableBlockDataBean) messageDataDef,
                    message, eng);
        }
    }

    /**
     * Performs the read operation of a fixed size data field from the input
     * stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param messageDataDef
     *            The object that contains the definition of how to read the
     *            field data from the input stream.
     * @param message
     *            The message that is being read.
     * @param iterableBlockId
     *            The id of the iteratable block, as defined at the message
     *            definition.
     * @param index
     *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to decode the message.
     */
    private static void readFixedSizeData(DataInput in, FixedSizeDataBean messageDataDef,
            ProtocolMessage message, String iterableBlockId, int index, ProtocolEngine eng)
            throws InvalidDefinitionException, InvalidInputStreamDataException,
            IOException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.
        String fieldName = messageDataDef.getFieldName();
        boolean isSigned = messageDataDef.isFieldSigned();
        int fieldSize = messageDataDef.getFieldSizeInBytes();

        if (fieldName != null) {
            // Reads the number from the input stream, considering the
            // signed data of the field.
            Number value = getNumberDataFromInputStream(in, fieldSize, isSigned, eng);

            if (value != null) {
                // If a value was read, then store the read data into the
                // message.
                message.setFieldValue(fieldName, iterableBlockId, index, value);
            }
        } else {
            // The definition of this fixed data does not contain all
            // information it should have.
            BasePlugin.logError("The field name was not provided by the message definition.");//$NON-NLS-1$
            throw new InvalidDefinitionException(
                    "Incomplete fixed data element"); //$NON-NLS-1$
        }
    }

    /**
     * Performs the read operation of a variable size data field from the input
     * stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param messageDataDef
     *            The object that contains the definition of how to read the
     *            field data from the input stream.
     * @param message
     *            The message that is being read.
     * @param iterableBlockId
     *            The id of the iteratable block, as defined in the message
     *            definition.
     * @param index
     *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to decode the message.
     */
    private static void readVariableSizeData(DataInput in, VariableSizeDataBean messageDataDef,
            ProtocolMessage message, String iterableBlockId, int index, ProtocolEngine eng)
            throws InvalidDefinitionException, InvalidInputStreamDataException,
            IOException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.

        String sizeFieldName = messageDataDef.getSizeFieldName();
        boolean isSizeSigned = messageDataDef.isSizeFieldSigned();
        int sizeFieldSize = messageDataDef.getSizeFieldSizeInBytes();
        String valueFieldName = messageDataDef.getValueFieldName();
        String charsetName = messageDataDef.getCharsetName();

        if (valueFieldName != null) {
            // Reads the size from the input stream, considering the
            // signed data of the field.
            Number size = getNumberDataFromInputStream(in, sizeFieldSize,
                    isSizeSigned, eng);

            // Based on the size read before, collect all bytes that comprise
            // the string (variable data field)
            byte[] valueArray = new byte[size.intValue()];

            try {
                in.readFully(valueArray);
            } catch (IOException e) {
                // The input stream is probably broken
                BasePlugin.logWarning("An IOException was detected while reading the variable size field. Requesting restart.");//$NON-NLS-1$
                eng.requestRestart();
                throw e;
            }         

            // If a value was read, then store the read data into the
            // message.
            try {
                String value = new String(valueArray, charsetName);
                message
                .setFieldValue(valueFieldName, iterableBlockId, index,
                        value);

                if (sizeFieldName != null && !sizeFieldName.equals("")) { //$NON-NLS-1$
                    message.setFieldValue(sizeFieldName, iterableBlockId, index,
                            new Integer(sizeFieldSize).toString());
                }
            } catch (UnsupportedEncodingException e) {
                // If the encoding provided is not supported, that means
                // that the message definition is incorrect.
                BasePlugin.logError("An invalid charset name was provided for the field.");//$NON-NLS-1$
                throw new InvalidDefinitionException(
                        "Invalid charset name provided at message definition", e); //$NON-NLS-1$
            }
        } else {
            // The definition of this fixed data does not contain all
            // information it should have.
            BasePlugin.logError("The field name was not provided by the message definition.");//$NON-NLS-1$
            throw new InvalidDefinitionException(
                    "Incomplete fixed data element"); //$NON-NLS-1$
        }
    }

    /**
     * Performs the read operation of a raw data field from the input stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param messageDataDef
     *            The object that contains the definition of how to read the
     *            field data from the input stream.
     * @param message
     *            The message that is being read.
     * @param iterableBlockId
     *            The id of the iteratable block, as defined in the message
     *            definition.
     * @param index
     *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to read the data from the stream during
     *             a raw field handling.
     * 
     */
    private static void readRawData(DataInput in, RawDataBean messageDataDef,
            ProtocolMessage message, String iterableBlockId, int index, ProtocolEngine eng)
            throws ProtocolRawHandlingException, IOException {
        IRawDataHandler handler = (IRawDataHandler) messageDataDef.getHandler();

        // Creates a message store object for the raw data handler to have
        // access to the data already read until this moment. Note that the
        // message store object is read-only.
        MessageFieldsStore currentlyReadFields = new MessageFieldsStore(
                message, iterableBlockId, index);

        // Delegates the read operation to the raw data handler
        Map<String, Object> returnedFields = null;
        try {
            returnedFields = handler
                    .readRawDataFromStream(eng.getHandle(), in,
                            currentlyReadFields, eng.isBigEndianProtocol());

            // Merge the data read by the handler to the current message being
            // read.
            for (String fieldName : returnedFields.keySet()) {
                Object fieldValue = returnedFields.get(fieldName);
                message.setFieldValue(fieldName, iterableBlockId, index,
                        fieldValue);
            }
        } catch (IOException e) {
            // The input stream is probably broken
            BasePlugin.logWarning("An IOException was thrown by the raw field handler. A restart is being requested.");//$NON-NLS-1$
            eng.requestRestart();
            throw e;
        }
    }

    /**
     * Performs the read operation of a iteratable block from the input stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param messageDataDef
     *            The object that contains the definition of how to read the
     *            iteratable block from the input stream.
     * @param message
     *            The message that is being read.
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to decode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to read the data from the stream during
     *             a raw field handling.
     */
    private static void readIteratableBlock(DataInput in, IteratableBlockDataBean messageDataDef,
            ProtocolMessage message, ProtocolEngine eng) throws ProtocolRawHandlingException,
            InvalidDefinitionException, InvalidInputStreamDataException,
            IOException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.
        String iterateOnField = messageDataDef.getIterateOnField();
        String iteratableBlockId = messageDataDef.getId();

        // Retrieves the number of times to iterate.
        Object valueObj = message.getFieldValue(iterateOnField);

        if (valueObj instanceof Integer) {
            // If it is possible to discover how much times to iterate, perform
            // the iteration. But firstly collect the internal beans to iterate
            // on.
            int numberOfIterations = (Integer) valueObj;
            Collection<IMsgDataBean> internalMsgDefs = messageDataDef
                    .getDataBeans();

            for (int i = 0; i < numberOfIterations; i++) {
                for (IMsgDataBean internalMsgDef : internalMsgDefs) {
                    // Filters the internal beans just like readReceivedMessage
                    // method does to message fields not inside iteratable
                    // blocks.
                    readFilterMessageDef(in, internalMsgDef, message,
                            iteratableBlockId, i, eng);
                }
            }
        } else {
            // If the number of iterations is not defined, than raise a protocol
            // exception to warn the caller that this it provided an invalid
            // message object to this method.
            BasePlugin.logError("It was not possible to determine how many iterations are expected.");//$NON-NLS-1$
            throw new InvalidInputStreamDataException(
                    "Iterate on field value is not numeric"); //$NON-NLS-1$
        }
    }

    /**
     * Reads a number from the input stream.
     * 
     * @param in 
     *            The stream from where the incoming bytes flow
     * @param numberOfBytes
     *            How many bytes to collect from the input stream to form the
     *            number.
     * @param isSigned
     *            True if the bytes collected are signed two-complement
     *            representation; False if they can be interpreted as unsigned
     *            (hence not requiring two-complement translation).
     * @param eng
     *            The engine that received the message to be read 
     * 
     * @return The number read from the input stream.
     * 
     * @throws IOException
     *             If an input stream disconnection is detected while reading
     *             data.
     * @throws InvalidInputStreamDataException
     *             If the data collected from the input stream is invalid,
     *             considering the message definition made.
     */
    private static Number getNumberDataFromInputStream(DataInput in, int numberOfBytes,
            boolean isSigned, ProtocolEngine eng) throws InvalidInputStreamDataException,
            IOException {
        Number value = null;
        try {
            switch (numberOfBytes) {
            case 1: // reads a byte
                if (isSigned) {                    
                    value = in.readByte();
                } else {                    
                    value = in.readUnsignedByte();
                }
                break;
            case 2: // reads a short
                if (isSigned) {
                    value = in.readShort();
                } else {
                    value = in.readUnsignedShort();
                }
                break;
            case 3:
                if (isSigned) {
                    long tmpval;
                    tmpval = in.readByte();
                    tmpval <<= 8;
                    tmpval += in.readShort();

                    value = tmpval;

                } else {
                    long tmpval;
                    tmpval = in.readUnsignedByte();
                    tmpval <<= 8;
                    tmpval += in.readUnsignedShort();

                    value = tmpval;
                }
                break;

            case 4: // reads an integer
                value = in.readInt();
                break;
            case 8: // reads a long
                value = in.readLong();
                break;
            default:
                // If the parameter numberOfBytes if not equal any of the
                // previous
                // alternatives, then it is not in a correct format, as there
                // are
                // not number representations with different sizes available.
                // Throw
                // an exception to warn the caller that it provided an invalid
                // parameter.
                BasePlugin.logError("It is not possible to read the number, because it doesn't have a supported field size.");//$NON-NLS-1$
                throw new InvalidInputStreamDataException(
                        "Unrecognized field size"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            // The input stream is probably broken
            BasePlugin.logWarning("An IOException was detected while reading from input stream. Requesting a restart.");//$NON-NLS-1$
            eng.requestRestart();
            throw e;
        }

        return value;
    }
}
