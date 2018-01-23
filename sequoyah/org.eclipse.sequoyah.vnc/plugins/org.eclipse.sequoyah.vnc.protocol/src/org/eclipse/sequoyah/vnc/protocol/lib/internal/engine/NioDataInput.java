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
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;

/**
 * DESCRIPTION: This class is designed to wrap all java.nio based logic for the input stream  
 * in a object implementing the DataInput contract. <br>
 * 
 * RESPONSIBILITY: Provide a DataInput abstraction to the socket channel for all protocol plugin
 * to use.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: Invoke any of the methods from DataInput interface to obtain primitives from the input
 * stream. <br>
 * 
 */
public class NioDataInput implements DataInput
{
    /**
     * Adopted timeout for input stream
     */
    private static final int IN_STREAM_TIMEOUT_MS = 3000;
    
    /**
     * Buffer size : 100 kB
     */
    private static final int BUFFER_SIZE = 102400;
    
    /**
     * The socket channel from where the bytes are read
     */
    private SocketChannel channel;
    
    /**
     * The intermediate buffer that stores the data received from the channel. 
     * The data from the buffer is provided to the user on demand
     */
    private ByteBuffer buffer;
    
    /**
     * Constructor
     * Stores the channel and setups the buffer
     * 
     * @param channel The channel where to take data from
     */
    NioDataInput(SocketChannel channel) {
        this.channel = channel;
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.flip();        
    }
    
    /**
     * Closes the channel and returns the remaining bytes from the buffer
     * 
     * @return The remaining contents of the buffer
     * 
     * @throws IOException If the channel cannot be closed
     * 
     * @see java.io.Closeable#close()
     */
    byte[] close() throws IOException {
        byte[] remainingBytes = null;
        
        if (channel != null) {
            channel.close();            
        }
        
        if (buffer != null) {
            int remaining = buffer.remaining();
            remainingBytes = new byte[remaining];
            buffer.get(remainingBytes);
        }
        
        return remainingBytes;
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readBoolean()
     */
    public boolean readBoolean() throws IOException
    {
        byte[] bytes = getBytesFromBuffer(1);
        return (bytes[0] != 0);
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readByte()
     */
    public byte readByte() throws IOException
    {
        return readByte(true);
    }

    /**
     * Performs the readByte operation, but allows the user to choose if the timeout
     * feature will be enabled or not
     * 
     * @param timeoutEnabled True if the timeout feature will be enabled during the
     *                       read operation; False otherwise
     *                       
     * @see java.io.DataInput#readByte()
     */
    byte readByte(boolean timeoutEnabled) throws IOException {
        int[] bytes = asIntArray(getBytesFromBuffer(1, timeoutEnabled));        
        return (byte) bytes[0];
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readChar()
     */
    public char readChar() throws IOException
    {
        int[] bytes = asIntArray(getBytesFromBuffer(2)); 
        return (char)((bytes[0] << 8) + (bytes[1] << 0));
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readDouble()
     */
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readFloat()
     */
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readFully(byte[])
     */
    public void readFully(byte[] b) throws IOException
    {
        readFully(b, 0, b.length);          
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readFully(byte[], int, int)
     */
    public void readFully(byte[] b, int off, int len) throws IOException
    {            
        byte[] bytes = getBytesFromBuffer(len - off);
        System.arraycopy(bytes, 0, b, off, len);
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readInt()
     */
    public int readInt() throws IOException
    {
        int[] bytes = asIntArray(getBytesFromBuffer(4));
        return ((bytes[0] << 24) + (bytes[1] << 16) + (bytes[2] << 8) + (bytes[3] << 0));
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readLine()
     */
    public String readLine() throws IOException
    {
        boolean readLine = false;
        char buf[] = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        try
        {
            while (true) {
                if (readLine == true) {
                    break;
                } else { 
                    switch (c = readUnsignedByte()) {
                        case '\n':
                            readLine = true;
                            break;

                        case '\r':                        
                            break;

                        default:
                            if (--room < 0) {
                                char[] previousBuffer = buf;
                                buf = new char[offset + 128];
                                room = buf.length - offset - 1;
                                System.arraycopy(previousBuffer, 0, buf, 0, offset);
                            }
                            buf[offset++] = (char) c;
                            break;
                    }
                }
            }
        }
        catch (EOFException e)
        {
            // Escape from the while block if end of file is reached.
            
            if (offset == 0) {
                return null;
            }
        }

        return String.copyValueOf(buf, 0, offset);
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readLong()
     */
    public long readLong() throws IOException
    {
        byte[] bytes = getBytesFromBuffer(8);
        return (((long)bytes[0] << 56) +
                ((long)(bytes[1] & 255) << 48) +
                ((long)(bytes[2] & 255) << 40) +
                ((long)(bytes[3] & 255) << 32) +
                ((long)(bytes[4] & 255) << 24) +
                ((bytes[5] & 255) << 16) +
                ((bytes[6] & 255) <<  8) +
                ((bytes[7] & 255) <<  0));
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readShort()
     */
    public short readShort() throws IOException
    {
        int[] bytes = asIntArray(getBytesFromBuffer(2));
        return (short)((bytes[0] << 8) + (bytes[1] << 0));
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readUTF()
     */
    public String readUTF() throws IOException
    {
        // TODO
        return "";//$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readUnsignedByte()
     */
    public int readUnsignedByte() throws IOException
    {
        int[] bytes = asIntArray(getBytesFromBuffer(1));
        return bytes[0];
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#readUnsignedShort()
     */
    public int readUnsignedShort() throws IOException
    {
        int[] bytes = asIntArray(getBytesFromBuffer(2));
        return (bytes[0] << 8) + (bytes[1] << 0);
    }

    /*
     * (non-Javadoc)
     * @see java.io.DataInput#skipBytes(int)
     */
    public int skipBytes(int n) throws IOException
    {
        int bytesSkipped = 0;
        
        try
        {
            for (bytesSkipped=0; bytesSkipped<n; bytesSkipped++) {
                getBytesFromBuffer(1);    
            }
        }
        catch (EOFException e)
        {
            // Only catch the exception so that we do not throw it, because it is forbidden by the
            // DataInput specification. 
        }
        
        return bytesSkipped;
    }
    
    /**
     * Retrieves numOfBytes bytes from the buffer
     * 
     * @param numOfBytes How many bytes to retrieve from buffer
     * 
     * @return An array containing the next numOfBytes of the buffer
     * 
     * @throws IOException If an I/O error occurs in the channel, or if a timeout happens
     */
    private byte[] getBytesFromBuffer(int numOfBytes) throws IOException {
        return getBytesFromBuffer(numOfBytes, true);
    }
    
    /**
     * Retrieves numOfBytes bytes from the buffer, but allows the user to choose if the timeout
     * feature will be enabled or not
     * 
     * @param numOfBytes How many bytes to retrieve from buffer
     * @param timeoutEnabled True if the timeout feature will be enabled during the
     *                       read operation; False otherwise
     * 
     * @return An array containing the next numOfBytes of the buffer
     * 
     * @throws IOException If an I/O error occurs in the channel, or if a timeout happens (in case of
     *                     timeout enabled)
     */
    private byte[] getBytesFromBuffer(int numOfBytes, boolean timeoutEnabled) throws IOException {
        byte[] bytes = new byte[numOfBytes];        
        boolean allBytesRetrieved = false;
        int bytesGot = 0;
        int toBeGot = numOfBytes;
        int length;
        
        while (!allBytesRetrieved) {
            length = Math.min(toBeGot, buffer.remaining());
            buffer.get(bytes, bytesGot, length);
            bytesGot += length;
            toBeGot -= length;
                       
            if (bytesGot >= numOfBytes) {
                allBytesRetrieved = true;
            } else {                
                fillBuffer(bytes.length - bytesGot, timeoutEnabled); 
            }
        }

        return bytes;
    }
        
    /**
     * Fills the buffer with the next bytes available in the channel 
     * A timeout on the read operation can be enabled or not
     * 
     * @param bytesToFill The minimum number of bytes to read from the channel
     * @param timeoutEnabled True if the timeout feature will be enabled during the
     *                       read operation; False otherwise
     *                       
     * @throws IOException If an I/O error occurs in the channel, or if a timeout happens (in case of
     *                     timeout enabled)
     */
    private void fillBuffer(int bytesToFill, boolean timeoutEnabled) throws IOException { 
        buffer.clear();
        long lastCheckpoint = System.currentTimeMillis();
        int totalBytes = 0;
        
        while ((buffer.remaining() > 0)&&(buffer.position() < bytesToFill)) {
            int bytesRead = channel.read(buffer);
            
            if (bytesRead <= 0) {               
                // Error condition: no data arrived through the input stream for 
                // IN_STREAM_TIMEOUT_MS
                if ((timeoutEnabled)&&
                        (System.currentTimeMillis() > lastCheckpoint + IN_STREAM_TIMEOUT_MS)) {
                    
                    BasePlugin.logError("No data arrived through the input stream channel for " //$NON-NLS-1$
                    		+ IN_STREAM_TIMEOUT_MS + " milliseconds. Timeout expired.");//$NON-NLS-1$
                    buffer.flip();
                    throw new EOFException("Timeout on communication detected!");//$NON-NLS-1$
                }
                
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // Do nothing. 
                }
            } else {
                lastCheckpoint = System.currentTimeMillis();
                totalBytes += bytesRead;
            } 
        }
        
        buffer.flip();
    }
    
    /**
     * Auxiliary method that builds an int array out of a byte array
     * 
     * @param byteArray The byte array to be translated
     * 
     * @return The resulting int array
     */
    private int[] asIntArray(byte[] byteArray) {
        int[] intArray = new int[byteArray.length];
        for (int i=0; i<byteArray.length; i++) {
            intArray[i] = byteArray[i] & 0xff;
        }
        return intArray;
    }
}
