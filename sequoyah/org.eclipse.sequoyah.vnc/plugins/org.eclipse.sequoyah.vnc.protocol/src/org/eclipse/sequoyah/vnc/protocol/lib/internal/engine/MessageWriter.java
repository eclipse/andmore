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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.vnc.protocol.lib.IRawDataHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidDefinitionException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidMessageException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.ProtocolMsgDefinition;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.FixedSizeDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.IMsgDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.IteratableBlockDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.RawDataBean;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.databeans.VariableSizeDataBean;

/**
 * DESCRIPTION: This class sends out ProtocolMessage objects, given a OutputStream
 * and a message definition.<br>
 * 
 * RESPONSIBILITY: Read data from the ProtocolMessage object and serialize it to send through
 * the output stream, following the specifications defined by a message definition object.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: This class is intended to be used by the engine event handler thread only.
 * 
 */
class MessageWriter
{
    /**
     * Performs the actual send message operation
     * 
     * @param out
     *            The stream to where the outgoing bytes flow
     * @param message
     *            The message to send.
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to encode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to write the data to the stream due to
     *             a generic reason.
     * @throws IOException
     *             If an output stream disconnection is detected while writing
     *             data.
     */
    static final void doSendMessage(OutputStream out, ProtocolMessage message, ProtocolEngine eng)
            throws ProtocolRawHandlingException, InvalidMessageException,
            InvalidDefinitionException, IOException {
        // Can only send a message if the protocol is connected
        if (eng.isConnected()) {
            // Creates a byte output stream to store all bytes that will be sent
            // through the real output stream after the message is completely
            // serialized
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            // Find the message definition given the message code
            ProtocolMsgDefinition msgDefinition = eng.getDefinitionByCode(message.getCode());

            if ((msgDefinition != null) && (ProtocolEngineUtils.isOutgoingMessage(msgDefinition.getId(), 
                    eng.getIncomingMessages(), eng.getOutgoingMessages()))) {
                
                // Serialize the message code
                boolean isCodeSigned = msgDefinition.isMsgCodeSigned();
                int codeSize = msgDefinition.getMsgCodeSizeInBytes();
                writeNumberToStream(byteOutputStream, new Long(message
                        .getCode()), codeSize, isCodeSigned, eng);

                // Serialize each field from the message definition
                List<IMsgDataBean> msgDataList = msgDefinition.getMessageData();
                for (IMsgDataBean messageData : msgDataList) {
                    writeFilterMessageDef(messageData, byteOutputStream,
                            message, null, -1, eng);
                }

                // After the message is completely serialized, write the message
                // to the real stream.
                try {
                    if (out != null) {
                        byteOutputStream.writeTo(out);
                        out.flush();
                    }
                } catch (IOException e) {
                    // The input stream is probably broken
                    eng.requestRestart();
                    throw e;
                }
            }
        }
    }

    /**
     * Determines which is the provided message data bean type and calls the
     * correct handler for it to perform write operation to the provided output
     * stream.
     * 
     * @param messageDataDef
     *            The message data object that needs filtering.
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param message
     *            The message that is going to be sent.
     * @param iterableBlockId
     *            The id of the iteratable block, as defined at the
     *            ProtocolMessage extension.
     * @param index
     *            The iteration index. Range: 0 ~~ (iteratableBlockLength - 1)
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to encode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to write the data to the stream during
     *             a raw field handling.
     */
    private static void writeFilterMessageDef(IMsgDataBean messageDataDef,
            ByteArrayOutputStream streamToWriteTo, ProtocolMessage message,
            String iteratableBlockId, int index, ProtocolEngine eng)
            throws ProtocolRawHandlingException, InvalidMessageException,
            InvalidDefinitionException {
        if (messageDataDef instanceof FixedSizeDataBean) {
            writeFixedSizeDataToStream(streamToWriteTo,
            		(FixedSizeDataBean) messageDataDef, message, iteratableBlockId, index, eng);
        } else if (messageDataDef instanceof VariableSizeDataBean) {
            writeVariableSizeDataToStream(streamToWriteTo,
                    (VariableSizeDataBean) messageDataDef, message, eng);
        } else if (messageDataDef instanceof RawDataBean) {
            writeRawDataToStream(streamToWriteTo, (RawDataBean) messageDataDef,
                    message, eng);
        } else {
            writeIteratableBlockToStream(streamToWriteTo,
                    (IteratableBlockDataBean) messageDataDef, message, eng);
        }
    }

    /**
     * Performs the write operation of a fixed size data field to the output
     * stream.
     * 
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param messageDataDef
     *            The object that contains the definition of how to write
     *            message data to the output stream.
     * @param message
     *            The message that is going to be sent.
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to encode the message.
     */
    private static void writeFixedSizeDataToStream(
            ByteArrayOutputStream streamToWriteTo,
            FixedSizeDataBean messageDataDef, ProtocolMessage message, String iteratableBlockId, int index, ProtocolEngine eng)
            throws InvalidMessageException, InvalidDefinitionException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.
        String fieldName = messageDataDef.getFieldName();
        boolean isSigned = messageDataDef.isFieldSigned();
        int fieldSize = messageDataDef.getFieldSizeInBytes();
        Object value = messageDataDef.getValue();

        if (fieldName != null) {
            if (value == null) {
                // If the message definition does not contain a value defined,
                // that means that it is a field is supposed to be defined by
                // the message object. See more information about the
                // "value" attribute in ProtocolMessage extension point
                // documentation.
            	if ((iteratableBlockId != null) && (index >= 0)) {
            		                    value = message.getFieldValue(fieldName, iteratableBlockId, index);
            		                } else {
            		                    value = message.getFieldValue(fieldName);
            		                }
            }
            if (value instanceof Number) {
                // If a value is defined, then write the field to the output
                // stream.
                writeNumberToStream(streamToWriteTo, (Number) value, fieldSize,
                        isSigned, eng);
            } else {
                // If a value is not defined, than raise a protocol exception
                // to warn the caller that this it provided an invalid message
                // object to this method.
                BasePlugin.logError("Value retrieved from message is not a number.");//$NON-NLS-1$
                throw new InvalidMessageException(
                        "Field does not contain a number"); //$NON-NLS-1$
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
     * Performs the write operation of a variable size data field to the output
     * stream.
     * 
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param messageDataDef
     *            The object that contains the definition of how to write
     *            message data to the output stream.
     * @param message
     *            The message that is going to be sent.
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to encode the message.
     */
    private static void writeVariableSizeDataToStream(
            ByteArrayOutputStream streamToWriteTo,
            VariableSizeDataBean messageDataDef, ProtocolMessage message, ProtocolEngine eng)
            throws InvalidMessageException, InvalidDefinitionException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.
        boolean isSizeSigned = messageDataDef.isSizeFieldSigned();
        int sizeFieldSize = messageDataDef.getSizeFieldSizeInBytes();
        String valueFieldName = messageDataDef.getValueFieldName();
        String charsetName = messageDataDef.getCharsetName();
        Object value = messageDataDef.getValue();

        if (valueFieldName != null) {
            if (value == null) {
                // If the message definition does not contain a value defined,
                // that means that it is a field is supposed to be defined by
                // the message object. See more information about the
                // "value" attribute in ProtocolMessage extension point
                // documentation.
                value = message.getFieldValue(valueFieldName);
            }
            if (value instanceof String) {
                // If a value is defined, then write the field to the output
                // stream.
                // In the variable size data field case, it is needed to write
                // firstly
                // the size of the field, and then its contents.
                try {
                    byte[] valueBytes = ((String) value).getBytes(charsetName);
                    int valueBytesSize = valueBytes.length;
                    writeNumberToStream(streamToWriteTo, valueBytesSize,
                            sizeFieldSize, isSizeSigned, eng);

                    streamToWriteTo.write(valueBytes, 0, valueBytes.length);                    
                } catch (UnsupportedEncodingException e) {
                    // If the encoding provided is not supported, that means
                    // that the message definition is incorrect.
                    BasePlugin.logError("An invalid charset name was provided for the field.");//$NON-NLS-1$
                    throw new InvalidDefinitionException(
                            "Invalid charset name provided at message definition", e); //$NON-NLS-1$
                }
            } else {
                // If a value is not defined, than raise a protocol exception
                // to warn the caller that this it provided an invalid message
                // object to this method.
                BasePlugin.logError("Value retrieved from message is not a string.");//$NON-NLS-1$
                throw new InvalidMessageException(
                        "Value field does not contain a string"); //$NON-NLS-1$
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
     * Performs the write operation of a raw data field to the output stream.
     * 
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param messageDataDef
     *            The object that contains the definition of how to write
     *            message data to the output stream.
     * @param message
     *            The message that is going to be sent.
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws ProtocolRawHandlingException
     *             If it is not possible to write the data to the stream during
     *             a raw field handling.
     */
    private static void writeRawDataToStream(ByteArrayOutputStream streamToWriteTo,
            RawDataBean messageDataDef, ProtocolMessage message, ProtocolEngine eng)
            throws ProtocolRawHandlingException {
        // Creates a temporary output stream to provide to the raw data writer
        // handler. Then delegates the write operation to the handler class.
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        IRawDataHandler handler = messageDataDef.getHandler();

        handler.writeRawDataToStream(eng.getHandle(), tempStream, message,
                eng.isBigEndianProtocol());

        try {
            tempStream.writeTo(streamToWriteTo);
        } catch (IOException e) {            
            // Do nothing. ByteArrayOutputStreams cannot be closed.
            BasePlugin.logWarning("An IOException was thrown by the raw field handler.");//$NON-NLS-1$
        }
    }

    /**
     * Performs the write operation of an iteratable block to the output stream.
     * 
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param messageDataDef
     *            The object that contains the definition of how to write
     *            message data to the output stream.
     * @param message
     *            The message that is going to be sent.
     * @param eng
     *            The engine that is requesting a message to be sent 
     * 
     * @throws InvalidMessageException
     *             If the data collected from the message is invalid,
     *             considering the message definition made.
     * @throws InvalidDefinitionException
     *             If the message definition does not contain all necessary
     *             information to encode the message.
     * @throws ProtocolRawHandlingException
     *             If it is not possible to write the data to the stream during
     *             a raw field handling.
     */
    private static void writeIteratableBlockToStream(
            ByteArrayOutputStream streamToWriteTo,
            IteratableBlockDataBean messageDataDef, ProtocolMessage message, ProtocolEngine eng)
            throws ProtocolRawHandlingException, InvalidMessageException,
            InvalidDefinitionException {
        // Retrieve data from the message definition object.
        // The fields description and how to use then can be found at
        // the ProtocolMessage extension point documentation.
        String iterateOnField = messageDataDef.getIterateOnField();
        String iteratableBlockId = messageDataDef.getId();

        // Retrieves the number of times to iterate.
        Object fieldValue = message.getFieldValue(iterateOnField);

        if (fieldValue instanceof Integer) {
            // If it is possible to discover how much times to iterate, perform
            // the iteration. But firstly collect the internal beans to iterate
            // on.
            int numberOfIterations = ((Integer) fieldValue).intValue();
            Collection<IMsgDataBean> internalBeans = messageDataDef
                    .getDataBeans();

            for (int i = 0; i < numberOfIterations; i++) {
                for (IMsgDataBean internalBean : internalBeans) {
                    // Filters the internal beans just like sendMessage method
                    // does to message fields not inside iteratable blocks.
                    writeFilterMessageDef(internalBean, streamToWriteTo,
                            message, iteratableBlockId, i, eng);
                }
            }
        } else {
            // If the number of iterations is not defined, than raise a protocol
            // exception to warn the caller that this it provided an invalid
            // message object to this method.
            BasePlugin.logError("It was not possible to determine how many iterations are expected.");//$NON-NLS-1$
            throw new InvalidMessageException(
                    "Iterate on field value is not numeric"); //$NON-NLS-1$
        }
    }

    /**
     * Writes number to the output stream, observing its signal and how many
     * bytes it should use in the message. The signal and number of bytes are as
     * defined in the message definition for this field.
     * 
     * @param streamToWriteTo
     *            The output stream where the message will be written.
     * @param data
     *            The number that needs to be written to the output stream.
     * @param numberOfBytes
     *            How many bytes this number needs to use in the message.
     * @param isSigned
     *            True if this is a signed field; False if this is a unsigned
     *            field.
     * @param eng
     *            The engine that is requesting a message to be sent 
     */
    private static void writeNumberToStream(ByteArrayOutputStream streamToWriteTo,
            Number data, int numberOfBytes, boolean isSigned, ProtocolEngine eng) {
        byte[] bytesToWrite = getUnsignedBytes(data, numberOfBytes);
        if ((isSigned) && (data.longValue() < 0)) {
            // If the number should be written as "signed" to the output stream,
            // then it is needed to calculate its two-complement for determining
            // its byte representation. This is what is done in the next lines.
            for (int i = 0; i < numberOfBytes; i++) {
                bytesToWrite[i] = (byte) (bytesToWrite[i] ^ bytesToWrite[i]);
            }
            bytesToWrite[numberOfBytes - 1] = (byte) (bytesToWrite[numberOfBytes - 1] + 1);
        }

        // Depending on the protocol setting of big/little endian, the number
        // bytes need to be written in a specific order.
        if (eng.isBigEndianProtocol()) {
            for (int i = 0; i < numberOfBytes; i++) {
                streamToWriteTo.write(bytesToWrite[i]);
            }
        } else {
            for (int i = numberOfBytes - 1; i >= 0; i--) {
                streamToWriteTo.write(bytesToWrite[i]);
            }
        }
    }
    
    /**
     * Given a number, get the unsigned byte representation of it.
     * 
     * @param number
     *            The number to be converted to a byte representation.
     * @param numberOfBytes
     *            The number of bytes to use at the representation.
     * 
     * @return The byte representation of the provided number, using the
     *         provided number of bytes.
     */
    private static byte[] getUnsignedBytes(Number number, int numberOfBytes) {
        long dataTmp = Math.abs(number.longValue());
        byte[] bytes = new byte[numberOfBytes];

        // For each 8 bits of the number binary representation, create a
        // correspondent byte and shift the original number to prepare it
        // for subsequent translations.
        for (int i = 0; i < numberOfBytes; i++) {
            byte oneByte = (byte) (dataTmp & 0xFF);
            dataTmp >>= 8;
            bytes[numberOfBytes - i - 1] = oneByte;
        }

        return bytes;
    }
}
