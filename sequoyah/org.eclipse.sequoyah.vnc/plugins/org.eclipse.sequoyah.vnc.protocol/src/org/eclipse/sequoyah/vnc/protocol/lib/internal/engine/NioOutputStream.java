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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
public class NioOutputStream extends OutputStream
{    
    /**
     * Buffer size : 100 kB
     */
    private static final int BUFFER_SIZE = 102400;  
    
    /**
     * The socket channel from where the bytes are written
     */
    private SocketChannel channel;
    
    /**
     * The intermediate buffer that stores the data to be sent through the channel. 
     * The data from the buffer is sent through the channel when the flush method is invoked
     */
    private ByteBuffer buffer;
    
    /**
     * Constructor
     * Stores the channel and setups the buffer
     * 
     * @param channel The channel where to write data
     */
    NioOutputStream(SocketChannel channel) {
        this.channel = channel;
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException {        
        if (channel.isOpen()) {
            flush();
            channel.close();    
        }
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException {
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        if (buffer.remaining() < 4) {
            flush();
        }   
        buffer.put((byte)(b & 0xff));
        flush();
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    public void write(byte b[]) throws IOException {
        if (buffer.remaining() < b.length) {
            flush();
        }
        buffer.put(b);
        flush();
    }
    
    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte b[], int off, int len) throws IOException {
        if (buffer.remaining() < len) {
            flush();
        }
        buffer.put(b, off, len);
        flush();
    }
}
