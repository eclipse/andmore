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
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233775] - Does not have a way to enter the session password for the vnc connection
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Fantato (Eldorado Research Institute) - Bug [243918] - Wrong "if" test in ProtocolEngine class
 * Fabio Rigo (Eldorado Research Institute)- Bug [242757] - Protocol does not support Unicode on variable sized fields
 * Fabio Rigo - Bug [244067] - The exception handling interface should forward the protocol implementer object
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233064] - Add reconnection mechanism to avoid lose connection with the protocol
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [242924] - There is no way to keep the size of a Variable Size Data read
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233121] - There is no support for proxies when connecting the protocol
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246916] - Add the correct Number objects to ProtocolMessage objects on reading from input stream
 * Daniel Barboza Franco (Eldorado Research Institute) - [257588] - Add support to ServerCutText message
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Mauren Brenner (Eldorado) - [282431] Guard synchronized block against null variable in consumer
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.internal.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.vnc.protocol.lib.IMessageHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolExceptionHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidDefinitionException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidInputStreamDataException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidMessageException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.MessageHandleException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolHandshakeException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.ProtocolMsgDefinition;

/**
 * DESCRIPTION: This class allows protocol communication through a single
 * channel.<br>
 * 
 * RESPONSIBILITY: Open/close/restart the channel among the two parts in the
 * communication. Serialize/deserialize messages, sending or retrieving them
 * to/from the communication streams.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: This class is intended to be used by the protocol framework only.
 * 
 */
public class ProtocolEngine {

	/**
	 * Default value for maximum number of reconnection attempts.
	 */
	private static final int RECONNECTION_MAX = 5;

	/**
	 * A variable that controls the event thread count
	 */
	private static int engineEventCounter = 0;

	/**
	 * A variable that controls the consumer thread count
	 */
	private static int consumerCounter = 0;

	/**
	 * True if the protocol is big endian, false if little endian. Note: If a
	 * number has more than one byte, a big endian protocol sends firstly the
	 * most significant byte to the stream. A little endian protocol, on the
	 * other hand, sends firstly the least significant byte.
	 */
	private boolean isBigEndianProtocol;

	/**
	 * The object used to map this connection at the model
	 */
	private ProtocolHandle handle;

	/**
	 * The message definition map used by this protocol. It contains the
	 * description of all messages supported by the protocol.
	 */
	private Map<Long, ProtocolMsgDefinition> messageDefCollection;

	/**
	 * Number of connection retries left.
	 */
	private int retries;

	/**
	 * Maximum number of reconnection attempts.
	 */
	private int retriesMax = RECONNECTION_MAX;

	/**
	 * This variable is used control concurrency between two consecutive restart
	 * requests. When a restart is successfully done it's value is incremented.
	 * This is necessary to avoid that two consecutive re-connections happen for
	 * the same reason.
	 */
	private int connectionSerialNumber = 0;

	/**
	 * A collection of the incoming messages ids, used to validate if a message
	 * can be retrieved from the input stream.
	 */
	private Collection<String> incomingMessages;

	/**
	 * A collection of the outgoing messages ids, used to validate if a message
	 * can be sent through the output stream.
	 */
	private Collection<String> outgoingMessages;

	/**
	 * The sequence of steps to execute for connection initialization
	 */
	private IProtocolHandshake initProcedure;

	/**
	 * The handler that was registered with the protocol at the moment it
	 * started. It is used for exception handling customization by the user.
	 */
	private IProtocolExceptionHandler exceptionHandler;

	/**
	 * The socket channel opened to the other part of the communication.
	 */
	private SocketChannel sockChannel;

	/**
	 * The host to which the socket is connected.
	 */
	private String host;

	/**
	 * General parameters associated to the protocol implementation, initialized
	 * by the protocol user.
	 */
	private Map<?, ?> parameters;

	/**
	 * The port to which the socket is connected.
	 */
	private int port = -1;

	/**
	 * The timeout defined for socket connection
	 */
	private int timeout = -1;

	/**
	 * True if this protocol is running as server. False if running as client.
	 * This information shall be kept as argument to allow restarting.
	 */
	private boolean isServer;

	/**
	 * The stream from where the incoming bytes flow
	 */
	private NioDataInput in;

	/**
	 * The stream to where the outgoing bytes flow
	 */
	private OutputStream out;

	/**
	 * The consumer that reads bytes from the socket input stream. The consumer
	 * thread monitors the stream to collect its bytes as they arrive.
	 */
	private Consumer consumer;

	/**
	 * The event handler that will control requests for this engine instance
	 */
	private EngineEventHandler eventHandler = new EngineEventHandler();

	/**
	 * Constructor. Sets the attributes that turns the generic engine into a
	 * specific engine, which are: - Protocol handle for identification - An
	 * init/handshaking procedure - Messages used in interaction phase
	 * (definitions and directions) - Specific exception handling procedures -
	 * Role of the engine (server, client)
	 */
	public ProtocolEngine(ProtocolHandle handle, IProtocolHandshake initProcedure,
			Map<Long, ProtocolMsgDefinition> messageDefCollection,
			Collection<String> incomingMessages,
			Collection<String> outgoingMessages,
			IProtocolExceptionHandler exceptionHandler,
			boolean isBigEndianProtocol, boolean isServer, int retries) {

	    BasePlugin.logDebugMessage("ProtocolEngine","A protocol engine is being created.");//$NON-NLS-1$ //$NON-NLS-2$
		this.handle = handle;
		this.initProcedure = initProcedure;
		this.messageDefCollection = messageDefCollection;
		this.incomingMessages = incomingMessages;
		this.outgoingMessages = outgoingMessages;
		this.exceptionHandler = exceptionHandler;
		this.isBigEndianProtocol = isBigEndianProtocol;
		this.isServer = isServer;
		this.retriesMax = (retries >= 0) ? retries : RECONNECTION_MAX;
		this.retries = this.retriesMax;
		
		engineEventCounter++;
		(new Thread(eventHandler, "Protocol Event Handler-"//$NON-NLS-1$
				+ engineEventCounter)).start();
	}

	/**
	 * Guarantees that all threads associated to this engine instance are stopped
	 */
	public void dispose() {
	    BasePlugin.logInfo("The protocol engine is being disposed.");//$NON-NLS-1$
	    if (consumer != null) {
	        consumer.stopConsumer();
	        consumer = null;
	    }
	    
		if (eventHandler != null) {
			eventHandler.stopEventHandler();
			eventHandler = null;
		}
	}

	/**
	 * Starts the communication.
	 * 
	 * @param host
	 *            The host to connect to.
	 * @param port
	 *            The port to connect to.
	 * @param parameters
	 *            A Map with parameters other than host and port, for
	 *            customization purposes. Accepts null if apply.
	 * 
	 */
	public void requestStart(String host, int port, Map<?, ?> parameters) {
		requestStart(host, port, parameters, -1);
	}

	/**
	 * Starts the communication.
	 * 
	 * @param host
	 *            The host to connect to.
	 * @param port
	 *            The port to connect to.
	 * @param parameters
	 *            A Map with parameters other than host and port, for
	 *            customization purposes. Accepts null if apply.
	 * @param timeout
	 *            The maximum time to wait for the connection to remote site to
	 *            open.
	 */
	public void requestStart(String host, int port, Map<?, ?> parameters,
			int timeout) {

		String nextHost = (host != null ? host : this.host);
		int nextPort = (port != -1 ? port : this.port);
		Map<?, ?> nextParameters = (parameters != null ? parameters
				: this.parameters);
		int nextTimeout = (timeout != -1 ? timeout : this.timeout);
		eventHandler.requestStart(sockChannel, nextHost, nextPort, nextTimeout,
				nextParameters);
	}

	/**
	 * Starts the communication.
	 * 
	 * @param protocolImplementer
	 *            The protocol instance that will use this engine to communicate
	 * @param connChannel
	 *            The socket channel that needs to be used by the engine to send and
	 *            receive messages
	 * @param parameters
	 *            A Map with parameters other than host and port, for
	 *            customization purposes. Accepts null if apply.
	 * @param isServer
	 *            True if the engine will run as server; false if it will run as
	 *            client
	 * 
	 */
	public void requestStart(SocketChannel connChannel, Map<?, ?> parameters) {

		SocketChannel nextChannel = (connChannel != null ? connChannel
				: this.sockChannel);
		String nextHost = nextChannel.socket().getInetAddress().getHostAddress();
		int nextPort = nextChannel.socket().getPort();
		Map<?, ?> nextParameters = (parameters != null ? parameters
				: this.parameters);
		eventHandler.requestStart(nextChannel, nextHost, nextPort, -1,
				nextParameters);
	}

	/**
	 * Starts the protocol message exchange, by running the handshaking
	 * procedure and starting the consumer thread.
	 * 
	 * @throws ProtocolHandshakeException
	 * @throws IOException
	 */
	private void doStartProtocol() throws ProtocolHandshakeException, IOException {
	    if (!isRunning()) {
	        BasePlugin.logInfo("Starting protocol.");//$NON-NLS-1$
	        if (sockChannel == null) {
	            
	            // TODO: Verify how proxy based connections will behave with NIO channels
	            
	            /*Boolean bypassProxy = (Boolean) parameters.get("bypassProxy"); //$NON-NLS-1$
	            bypassProxy = (bypassProxy != null) ? bypassProxy : new Boolean(
	                    false);

	            Proxy proxy = (Proxy) parameters.get("proxy"); //$NON-NLS-1$

	            if (bypassProxy) { // The connection will not use proxy settings
	                sockChannel = new Socket(Proxy.NO_PROXY);
	            } else if (proxy != null) { // The connection will use this proxy
	                sockChannel = new Socket(proxy);
	            } else { // The connection will use default proxy settings, if any
	                sockChannel = new Socket();
	            }*/

	            InetSocketAddress socketAdress = new InetSocketAddress(host, port);
	            sockChannel = SocketChannel.open(socketAdress);
	            
	             // TODO: Verify how we can specify different timeout parameters to the NIO channel
	            
	/*          if (timeout < 0) {
	                sockChannel.connect(socketAdress);
	            } else {
	                sockChannel.connect(socketAdress, timeout);
	            }*/
	        }
	        sockChannel.configureBlocking(false);

	        // When the socket is opened, keep the input and output streams in
	        // the appropriate attributes
	        in = new NioDataInput(sockChannel);
	        out = new NioOutputStream(sockChannel);

	        if (initProcedure != null) {
	            // Delegate the initialization to the concrete protocol class
	            if (isServer) {
	                initProcedure.serverHandshake(handle, in, out, parameters);
	            } else {
	                initProcedure.clientHandshake(handle, in, out, parameters);
	            }
	            BasePlugin.logInfo("Handshake is finished.");//$NON-NLS-1$

	            // After all initialization is done, start the consumer thread,
	            // which will listen to the input stream to collect any byte that arrive
	            consumer = new Consumer();
	            consumerCounter++;
	            Thread consumerThread = new Thread(consumer, "Consumer-"//$NON-NLS-1$
	                    + consumerCounter);
	            consumerThread.start();

	            retries = retriesMax;
	            connectionSerialNumber++;
	        } else {
	            BasePlugin.logWarning("Handshake handler is not available. No handshake performed.");//$NON-NLS-1$
	        }
	        BasePlugin.logInfo("Protocol started.");//$NON-NLS-1$
	    }
	}

	/**
	 * Stops the communication with the current site.
	 */
	public void requestStop() {
		eventHandler.requestStop();
	}

	/**
	 * Performs the actual stop protocol operation
	 * 
	 * @throws IOException
	 *             If an error occurs while closing the streams and socket.
	 */
	private void doStopProtocol() throws IOException {
	    if (isConnected()) {
	        BasePlugin.logInfo("Stopping protocol.");//$NON-NLS-1$
	        if (consumer != null) {
	            consumer.stopConsumer();
	            consumer = null;
	        }

	        if (sockChannel != null) {
	            sockChannel.close();
	            sockChannel = null;
	        }

	        if (in != null) {
	            in.close();
	            in = null;
	        }

	        if (out != null) {
	            out.close();
	            out = null;
	        }
	        BasePlugin.logInfo("Protocol stopped."); //$NON-NLS-1$
	    }
	}

	/**
	 * Restarts the protocol by closing the current connection (if opened) and
	 * opening it again.
	 */
	public void requestRestart() {
		eventHandler.requestRestart();
	}

	/**
	 * Return the connection's state.
	 * 
	 * @return True if the connection is alive; false if not.
	 */
	public boolean isConnected() {
		boolean isConnected = false;

		if ((sockChannel != null) && (sockChannel.isConnected())) {
			isConnected = true;
		}

		return isConnected;
	}

	/**
	 * Tests if the protocol is started and at execution phase
	 * 
	 * @return True if the protocol is started; false otherwise
	 */
	public boolean isRunning() {
		return isConnected() && (consumer != null) && consumer.isRunning();
	}

	/**
	 * Sends the provided message to the communication stream, serializing it
	 * according to the current message definitions map.<br>
	 * That map contains message definitions declared by this protocol and its
	 * parents.
	 * 
	 * @param message
	 *            The message to send.
	 * 
	 */
	public void requestSendMessage(ProtocolMessage message) {
		if (message != null) {
			eventHandler.queueMessage(message);
		}
	}


	/**
	 * DESCRIPTION: This class is used to collect bytes that arrive at the input
	 * stream. <br>
	 * 
	 * RESPONSIBILITY: Monitor the input stream for available bytes and collect
	 * the bytes for message reading.<br>
	 * 
	 * COLABORATORS: None.<br>
	 * 
	 * USAGE: The class is intended to be used by the protocol engine class
	 * only.<br>
	 * For that, create a thread and set this class as its runnable. When it is
	 * no more needed, run method stopConsumer.<br>
	 * 
	 */
	private class Consumer implements Runnable {

		/**
		 * Flag that indicates if the consumer is monitoring the input stream
		 * and collecting bytes from it.
		 */
		private boolean isRunning = false;

		/**
		 * Starts monitoring and consuming bytes from the input stream.
		 * 
		 * OVERVIEW: Each time the while loop repeats, a byte is consumed from
		 * the input stream. The consumer continually tries to find a valid code
		 * in the message definitions map. When such message definition is
		 * found, then start reading the message based on it.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {

		    BasePlugin.logInfo("Starting consumer.");//$NON-NLS-1$
		    long code = 0;
			isRunning = true;
			// Executes the monitoring while the consumer is not stopped.
			while (isRunning) {
				try {
				    try {
				        if (code > Long.MAX_VALUE / 2) {
	                        // If the current code value is too big, being
	                        // impossible to find a message that fits in a long size
	                        // variable, then the consumer concludes the message
	                        // does not exist and the input stream is not
	                        // synchronized. For this reason the protocol is
	                        // stopped, because a non synchronized stream is
	                        // impossible to recover
	                        BasePlugin.logError("Message not found. Stopping protocol..."); //$NON-NLS-1$
	                        requestRestart();
	                        isRunning = false;
	                    } else {
	                        // Reads a byte from the input stream and append it to
	                        // the current code variable
	                        int nextByte = in.readByte(false);
	                        code <<= 8;
	                        code += nextByte;

	                        // Tries to find a message definition that has the
	                        // current code
	                        // Note: the code for incoming messages is kept as
	                        // negative
	                        long auxCode = (isServer)? code : -code;
	                        ProtocolMsgDefinition messageDef = messageDefCollection
	                                .get(auxCode);
	                        if (messageDef != null) {
	                            
	                            // If it finds a message with the current code,
	                            // reads the remaining of the message fields.
	                        	if (in != null) {
	                        		synchronized (in) {
	                        			IMessageHandler handler = messageDef.getHandler();
	                        			ProtocolMessage message = MessageReader.readReceivedMessage(in, auxCode, messageDef, 
	                        					ProtocolEngine.this);

	                        			// After the message is entirely read, delegates the handling to the
	                        			// handler defined at the ProtocolMessage extension. The fields
	                        			// description and how to use then can be found at
	                        			// the ProtocolMessage extension point documentation.

	                        			ProtocolMessage returnedMessage = handler.handleMessage(handle, message);
	                        			if (returnedMessage != null) {
	                        				requestSendMessage(returnedMessage);
	                        			}
	                        		}
	                        	}
	                            code = 0;
	                        }
	                    }
				    } catch (IOException e) {
	                    // Handle exception only if the IOException was not caused
	                    // by protocol disconnection                    
	                    if (isRunning) {
	                        BasePlugin.logError("Socket disconnection was detected. Stopping consumer.");//$NON-NLS-1$
	                        isRunning = false;
	                        if (exceptionHandler != null) {
	                            BasePlugin.logInfo("An user exception handler is available. Delegating exception to the handler.");//$NON-NLS-1$
	                            exceptionHandler.handleIOException(handle, e);
	                        }
	                    }
	                } catch (Exception e) {
	                    BasePlugin.logError("A protocol related error happened. Stopping consumer. Cause: " //$NON-NLS-1$
	                    		+ e.getMessage());
	                    isRunning = false;

	                    if (exceptionHandler != null) {
	                        BasePlugin.logInfo("An user exception handler is available. Delegating exception to the handler.");//$NON-NLS-1$
	                        
	                        // Delegate the exception to user
	                        if (e instanceof ProtocolHandshakeException) {
	                            exceptionHandler.handleProtocolHandshakeException(
	                                    handle, (ProtocolHandshakeException) e);
	                        } else if (e instanceof MessageHandleException) {
	                            exceptionHandler.handleMessageHandleException(
	                                    handle, (MessageHandleException) e);
	                        } else if (e instanceof InvalidMessageException) {
	                            exceptionHandler.handleInvalidMessageException(
	                                    handle, (InvalidMessageException) e);
	                        } else if (e instanceof InvalidInputStreamDataException) {
	                            exceptionHandler
	                                    .handleInvalidInputStreamDataException(
	                                            handle,
	                                            (InvalidInputStreamDataException) e);
	                        } else if (e instanceof InvalidDefinitionException) {
	                            exceptionHandler.handleInvalidDefinitionException(
	                                    handle, (InvalidDefinitionException) e);
	                        } else if (e instanceof ProtocolRawHandlingException) {
	                            exceptionHandler
	                                    .handleProtocolRawHandlingException(handle,
	                                            (ProtocolRawHandlingException) e);
	                        }
	                    }
	                }
				} catch (Throwable t) {
                    BasePlugin.logError("One unhandled error occurred in consumer thread. Restarting the protocol..."); //$NON-NLS-1$
                    requestRestart();
                    isRunning = false;
				} 
			}
			
			BasePlugin.logInfo("Consumer stopped.");//$NON-NLS-1$
		}

		/**
		 * Stops the consumer. After this operation is performed, the thread
		 * executing the consumer code will finish.
		 */
		public void stopConsumer() {
		    BasePlugin.logDebugMessage("Consumer","Stopping consumer");//$NON-NLS-1$ //$NON-NLS-2$
			isRunning = false;
		}

		/**
		 * Tests if the consumer is running
		 * 
		 * @return True if the consumer is running; false otherwise
		 */
		public boolean isRunning() {
			return isRunning;
		}
	}

	/**
	 * DESCRIPTION: This class handles all the requests that imposes IO
	 * operations, centralizing the handling activities and enhancing
	 * robustness. <br>
	 * 
	 * RESPONSIBILITY: Handle start, stop, restart and send message requests
	 * from all threads.<br>
	 * 
	 * COLABORATORS: None.<br>
	 * 
	 * USAGE: The class is intended to be used by the protocol engine class
	 * only.<br>
	 * For that, create a thread and set this class as its runnable. When it is
	 * no more needed, run method stopEventHandler.<br>
	 * 
	 */
	private class EngineEventHandler implements Runnable {
		/**
		 * Controls the running state of the thread
		 */
		private boolean isRunning = true;

		/**
		 * Request flags used for flow control in the scheduling
		 */
		private boolean restartRequested = false;
		private boolean startRequested = false;
		private boolean stopRequested = false;

		/**
		 * A queue of messages that were requested to be sent through the
		 * communication channel
		 */
		private Queue<ProtocolMessage> messagesToSend = new ConcurrentLinkedQueue<ProtocolMessage>();

		/**
		 * Variables to be used in the next start process. They must be provided
		 * by the start requester
		 */
		private SocketChannel nextChannel = null;
		private String nextHost = null;
		private int nextPort = -1;
		private int nextTimeout = -1;
		private Map<?, ?> nextParameters = null;

		/**
		 * @see Runnable#run()
		 * 
		 *      The scheduling process While the process is running, the
		 *      scheduler checks if there are requests for start, stop or
		 *      restart. While such requests are not placed, it picks a message
		 *      from the message queue and send it.
		 */
		public void run() {
		    BasePlugin.logInfo("Starting engine event handler.");//$NON-NLS-1$
		    ProtocolMessage messageToSend = null;
			while (isRunning) {
			    
			    try {
			        synchronized (messagesToSend) {
	                    if (!startRequested && !stopRequested && 
	                            !restartRequested && messagesToSend.isEmpty()) {                     
	                        try {
	                            messagesToSend.wait();
	                        } catch (InterruptedException e) {
	                            // Do nothing
	                        }
	                    }

	                    if (!messagesToSend.isEmpty()) {
	                        messageToSend = messagesToSend.poll();
	                    }
	                }
	                
	                try {
	                    if (startRequested) {
	                        // Handles a start request, if the socket is not connected and 
	                        // the consumer is not available (indicating that the protocol
	                        // is not running). When a request like this is placed, the
	                        // previous message queue is cleared and the start parameters are
	                        // set to the protocol engine
	                        startRequested = false;
	                        messagesToSend.clear();

	                        sockChannel = nextChannel;
	                        host = nextHost;
	                        port = nextPort;
	                        timeout = nextTimeout;
	                        parameters = nextParameters;

	                        doStartProtocol();

	                    } else if (stopRequested) {
	                        // Handles a stop request, if the socket is connected
	                        stopRequested = false;
	                        messagesToSend.clear();
	                        doStopProtocol();
	                        stopEventHandler();

	                    } else if (restartRequested) {
	                        // Handles a restart request
	                        restartRequested = false;
	                        messagesToSend.clear();

	                        synchronized (this) {
	                            int initialSerialNumber = connectionSerialNumber;
	                            while ((connectionSerialNumber == initialSerialNumber)&&(retries >= 0)) {
	                                try {
	                                    if (isConnected() || isRunning()) {
	                                        doStopProtocol();                                       
	                                    }
	                                    if (!isConnected()) {
	                                        doStartProtocol();    
	                                    }
	                                } catch (Exception e) {
	                                    retries--;
	                                    if (retries < 0) {
	                                        BasePlugin.logError("Number of connection retries exceeded the limit.");//$NON-NLS-1$
	                                        retries = retriesMax;
	                                        throw e; //$NON-NLS-1$ //$NON-NLS-2$
	                                    } 
	                                }      
	                            }
	                        }
	                    }
	                } catch (Exception e) {
	                    try {
	                        doStopProtocol();
	                    } catch (IOException e1) {
	                        // Do nothing
	                    }

	                    if (exceptionHandler != null) {
	                        // Delegate the exception to user
	                        if (e instanceof ProtocolHandshakeException) {
	                            exceptionHandler.handleProtocolHandshakeException(
	                                    handle, (ProtocolHandshakeException) e);
	                        } else if (e instanceof IOException) {
	                            exceptionHandler.handleIOException(handle,
	                                    (IOException) e);
	                        } else {
	                            throw e;
	                        }
	                    } else {
	                        throw e;
	                    }
	                }

	                // Send the next message of the queue
	                if (isConnected() && (messageToSend != null)) {
	                    try {
	                        MessageWriter.doSendMessage(out, messageToSend, ProtocolEngine.this);
	                        messageToSend = null;
	                    } catch (Exception e) {
	                        if (exceptionHandler != null) {
	                            // Delegate the exception to user
	                            if (e instanceof ProtocolRawHandlingException) {
	                                exceptionHandler
	                                        .handleProtocolRawHandlingException(
	                                                handle,
	                                                (ProtocolRawHandlingException) e);
	                            } else if (e instanceof InvalidMessageException) {
	                                exceptionHandler
	                                        .handleInvalidMessageException(
	                                                handle,
	                                                (InvalidMessageException) e);
	                            } else if (e instanceof InvalidDefinitionException) {
	                                exceptionHandler
	                                        .handleInvalidDefinitionException(
	                                                handle,
	                                                (InvalidDefinitionException) e);
	                            } else if (e instanceof IOException) {
	                                exceptionHandler.handleIOException(handle,
	                                        (IOException) e);
	                            } else {
	                                throw e;
	                            }
	                        } else {
	                            throw e;
	                        }
	                    }
	                }
			    } catch (Throwable t) {
			        // This catch is here to prevent the thread to crash if an unexpected error occurs
			        // For now it is just ignoring the error and moving on.
			        // TODO: Analyze each possible error situation to decide if specific error handling is needed.
			        BasePlugin.logWarning("One unhandled error occurred in event handler thread."); //$NON-NLS-1$
			    }
			}
			
			BasePlugin.logInfo("Engine event handler stopped.");//$NON-NLS-1$
		}

		/**
		 * Adds a message to the scheduler queue
		 * 
		 * @param message
		 *            The message to add to the queue
		 */
		public void queueMessage(ProtocolMessage message) {
			synchronized (messagesToSend) {
			    messagesToSend.offer(message);
			    messagesToSend.notify();
			}
		}

		/**
		 * Requests a restart to the event handler The request will be performed
		 * when there are no messages being sent through the socket. The request
		 * may not be accepted if placed right after another threads request
		 */
		public void requestRestart() {
			//if ((lastRestartGrantedTime == -1)
			//		|| (System.currentTimeMillis() - lastRestartGrantedTime > RESTART_REQUESTS_DELAY)) {
			    synchronized (messagesToSend) {
			        BasePlugin.logDebugMessage("EngineEventHandler","A restart was requested.");//$NON-NLS-1$ //$NON-NLS-2$
			        restartRequested = true;
			        //lastRestartGrantedTime = System.currentTimeMillis();
			        messagesToSend.notify();
			    }
			//} else {
			//    BasePlugin.logDebugMessage("EngineEventHandler","A restart was requested but will NOT be scheduled.");
			//}
		}

		/**
		 * Requests a protocol start to the event handler
		 * 
		 * @param channel
		 *            A connected socket channel, if available, or <code>null</code>
		 *            otherwise
		 * @param host
		 *            The host where to connect, or <code>null</code> if the
		 *            current host shall be used. This will be ignored if a
		 *            connected socket is provided
		 * @param port
		 *            The port where to connect, or <code>null</code> if the
		 *            current port shall be used. This will be ignored if a
		 *            connected socket is provided
		 * @param timeout
		 *            The desired connection timeout, or -1 if the default
		 *            timeout shall be used
		 * @param parameters
		 *            The connection parameters to use, or <code>null</code> if
		 *            the current parameters shall be used.
		 */
		public void requestStart(SocketChannel channel, String host, int port,
				int timeout, Map<?, ?> parameters) {
		    synchronized (messagesToSend) {
		        BasePlugin.logDebugMessage("EngineEventHandler","A start was requested. Host=" + //$NON-NLS-1$ //$NON-NLS-2$
		                host + "; Port=" + port + "; Channel=" + (channel != null ? "available" : "none"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		        nextChannel = channel;
		        nextHost = host;
		        nextPort = port;
		        nextTimeout = timeout;
		        nextParameters = parameters;		
		        startRequested = true;
		        messagesToSend.notify();
		    }
		}

		/**
		 * Requests a protocol stop to the event handler
		 */
		public void requestStop() {
		    synchronized (messagesToSend) {
		        BasePlugin.logDebugMessage("EngineEventHandler","A stop was requested.");//$NON-NLS-1$ //$NON-NLS-2$
		        stopRequested = true;
		        messagesToSend.notify();
		    }
		}

		/**
		 * Stops the event handler. Its thread ends after this action. This
		 * method shall only be called when the protocol engine will be disposed
		 */
		public void stopEventHandler() {
		    BasePlugin.logDebugMessage("EngineEventHandler","Stopping engine event handler.");//$NON-NLS-1$ //$NON-NLS-2$
			isRunning = false;
	         synchronized (messagesToSend) {
	             messagesToSend.notify();
	         }
		}
	}
	
	/*
	 * Getters section (for auxiliary classes use)
	 */
	
	Collection<String> getIncomingMessages() {
	    return incomingMessages;
	}
	
	Collection<String> getOutgoingMessages() {
	        return outgoingMessages;
	}
	
	ProtocolHandle getHandle() {
	    return handle;
	}
	
	boolean isBigEndianProtocol() {
	    return isBigEndianProtocol;
	}
	
	ProtocolMsgDefinition getDefinitionByCode(long code) {
	    return messageDefCollection.get(code);
	}
}
