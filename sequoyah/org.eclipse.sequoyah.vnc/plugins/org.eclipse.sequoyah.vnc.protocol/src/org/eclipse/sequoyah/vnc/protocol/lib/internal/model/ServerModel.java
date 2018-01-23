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
 * Daniel Barboza Franco - Bug [233775] - Does not have a way to enter the session password for the vnc connection
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Rigo - Bug [244067] - The exception handling interface should forward the protocol implementer object
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233064] - Add reconnection mechanism to avoid lose connection with the protocol
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer 
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib.internal.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolExceptionHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolHandshakeException;
import org.eclipse.sequoyah.vnc.protocol.lib.internal.engine.ProtocolEngine;
import org.eclipse.sequoyah.vnc.protocol.lib.msgdef.ProtocolMsgDefinition;

/**
 * DESCRIPTION: This class implements a model for protocols running as server
 * parts.<br>
 * 
 * RESPONSIBILITY: Maintain record of all protocols running as server,
 * associating them to their listening sockets, engine factories and running
 * engines. Provide actions to be performed at protocol running as servers.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: This class is intended to be used by the protocol framework only.
 * 
 */
public class ServerModel {

	/**
	 * The only instance of this class
	 */
	private static ServerModel instance;

	/**
	 * A map containing all sockets that are currently listening to ports
	 * associated to the running server protocols
	 */
	private Map<ProtocolHandle, ServerSocketChannel> openedSocketChannels = new HashMap<ProtocolHandle, ServerSocketChannel>();

	/**
	 * A map containing all engine factories associated to each running server
	 * protocol
	 */
	private Map<ProtocolHandle, ServerProtocolEngineFactory> engineFactories = new HashMap<ProtocolHandle, ServerProtocolEngineFactory>();

	/**
	 * A map containing a collection of all engines running associated to each
	 * running server protocol
	 */
	private Map<ProtocolHandle, Collection<ProtocolEngine>> connectedClients = new HashMap<ProtocolHandle, Collection<ProtocolEngine>>();

	/**
	 * Private constructor to fit singleton pattern
	 */
	private ServerModel() {
		// Do nothing
	}

	/**
	 * Gets the single instance of this model
	 * 
	 * @return The model
	 */
	public static ServerModel getInstance() {

		if (instance == null) {
			instance = new ServerModel();
		}

		return instance;
	}

	/**
	 * Starts listening to a local port. This allows a client to connect and a
	 * protocol engine to be started upon the connection event
	 * 
	 * @param portToBind
	 *            The local port where to bind the server to
	 * @param allMessages
	 *            A map containing all messages that belong to the protocol. The
	 *            message code as key
	 * @param incomingMessages
	 *            A collection containing the message ids of all incoming
	 *            messages. The message ids much match the id field in a
	 *            ProtocolMsgDefinition object from the allMessages map
	 * @param outgoingMessages
	 *            A collection containing the message ids of all outgoing
	 *            messages. The message ids much match the id field in a
	 *            ProtocolMsgDefinition object from the allMessages map
	 * @param protocolInitializer
	 *            The sequence of steps to execute for connection initialization
	 * @param isBigEndianProtocol
	 *            True if the protocol is big endian, false if little endian
	 *
	 */
	public ProtocolHandle startListeningToPort(int portToBind,
			Map<Long, ProtocolMsgDefinition> allMessages,
			Collection<String> incomingMessages,
			Collection<String> outgoingMessages,
			IProtocolHandshake protocolInitializer,
			IProtocolExceptionHandler exceptionHandler,
			boolean isBigEndianProtocol) {

	    ProtocolHandle handle = new ProtocolHandle();
		if ((portToBind <= 0) || (allMessages == null)
				|| (incomingMessages == null) || (outgoingMessages == null)) {
		    
		    BasePlugin.logError("Invalid parameters provided to method."); //$NON-NLS-1$
		    if (exceptionHandler != null) {
		        exceptionHandler.handleProtocolHandshakeException(handle,     
		                new ProtocolHandshakeException("Invalid parameters provided to method")); //$NON-NLS-1$)
		    }
		}

		BasePlugin.logDebugMessage("ServerModel","Creating a server socket channel to listen to connections at port " +  //$NON-NLS-1$ //$NON-NLS-2$
		        portToBind + ". Generated handle: " + handle + ".");          //$NON-NLS-1$//$NON-NLS-2$
		try
        {
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.socket().bind(new InetSocketAddress(portToBind));
            BasePlugin.logDebugMessage("ServerModel","Registering needed objects at Server Model."); //$NON-NLS-1$ //$NON-NLS-2$
            ServerProtocolEngineFactory factory = new ServerProtocolEngineFactory(
            		handle, protocolInitializer, allMessages, incomingMessages, outgoingMessages,
            		exceptionHandler, isBigEndianProtocol);
            openedSocketChannels.put(handle, channel);
            engineFactories.put(handle, factory);

            Runnable deamon = new ServerDeamon(handle, channel, factory);
            Thread deamonThread = new Thread(deamon);
            deamonThread.start();
        }
        catch (IOException e)
        {
            BasePlugin.logError("Error opening server socket. Cause: " + e.getMessage()); //$NON-NLS-1$
            if (exceptionHandler != null) {
                exceptionHandler.handleIOException(handle, e);                        
            }
        }
				
		return handle;
	}

	/**
	 * Stops listening to the port. This causes all the running server protocol
	 * engines to be stopped as well as a model cleanup.
	 * 
 	 * @param handle
	 *            The object that identifies the server socket that is to
	 *            stop listening to.
	 * 
	 */
	public void stopListeningToPort(ProtocolHandle handle) {

	    ServerSocketChannel channel = openedSocketChannels.get(handle);
	    if (channel != null) {
	        
	        BasePlugin.logDebugMessage("ServerModel","Closing server socket channel related to provided handle."); //$NON-NLS-1$ //$NON-NLS-2$
	        try {
                channel.close();
            } catch (IOException e) {
                BasePlugin.logError("Error closing server socket. Cause: " + e.getMessage()); //$NON-NLS-1$
                ServerProtocolEngineFactory factory = engineFactories.get(handle);
                if (factory != null) {
                    IProtocolExceptionHandler excHandler = factory.getExceptionHandler();
                    if (excHandler != null) {
                        excHandler.handleIOException(handle, e);
                    }                    
                }
            }

	        BasePlugin.logDebugMessage("ServerModel","Unregistering all objects related to provided handle from Server Model."); //$NON-NLS-1$ //$NON-NLS-2$
	        Collection<ProtocolEngine> aImplCollection = connectedClients.get(handle);
	        for (ProtocolEngine aImpl : aImplCollection) {
	            aImpl.dispose();        
	            aImplCollection.remove(aImpl);
	        }
	        connectedClients.remove(handle);
	        openedSocketChannels.remove(handle);
	        engineFactories.remove(handle);
	    }
	}

	/**
	 * Restarts the provided protocol instance
	 * 
	 * @param handle
	 *            The object that identifies the server socket that is to
	 *            be reconnected.
	 * 
	 * @return True if the restart was performed; false otherwise
	 * 
	 */
	public void requestRestartProtocol(ProtocolHandle handle) {
		ServerSocketChannel channel = openedSocketChannels.get(handle);
		
		if (channel != null) {
			int portToBind = channel.socket().getLocalPort();
			ServerProtocolEngineFactory factory = engineFactories
					.get(handle);
	
			stopListeningToPort(handle);
			startListeningToPort(portToBind, factory.getAllMessages(), factory
					.getIncomingMessages(), factory.getOutgoingMessages(),
					null, factory.getExceptionHandler(), factory
							.isBigEndianProtocol());
		}
	}

	/**
	 * Finalizes and unregisters all stopped protocols from the model
	 */
	public void cleanStoppedProtocols() {

	    BasePlugin.logDebugMessage("ServerModel","Removing all stopped protocol engines from Server Model."); //$NON-NLS-1$ //$NON-NLS-2$
		Set<ProtocolHandle> keys = connectedClients.keySet();
		for (ProtocolHandle key : keys) {
			Collection<ProtocolEngine> aImplCollection = connectedClients
					.get(key);
			for (ProtocolEngine aImpl : aImplCollection) {
				if (!aImpl.isConnected()) {
				    aImpl.dispose();		
				    aImplCollection.remove(aImpl);
				}
			}
		}
	}
	
	/**
     * Tests if the protocol identified by handle is listening to some port
     * 
     * @param handle The handle that identify the protocol instance
     * 
     * @return True if the protocol is running; false otherwise
     */
    public boolean isListeningToPort(ProtocolHandle handle)
    {   
        boolean isListeningToPort = false;
        
        ServerSocketChannel channel = openedSocketChannels.get(handle);
        if (channel != null)
        {
            isListeningToPort = channel.socket().isBound() && !channel.isOpen();
        }
        return isListeningToPort;
    }

	/**
	 * DESCRIPTION: This class is the deamon that runs after a port starts to be
	 * listened to. <br>
	 * 
	 * RESPONSIBILITY: It keeps waiting for client connections, and when it
	 * receives one it starts a new protocol engine to communicate with that
	 * client.<br>
	 * 
	 * COLABORATORS: None.<br>
	 * 
	 * USAGE: This class is intended to be used by the server model only.<br>
	 * 
	 */
	private class ServerDeamon implements Runnable {

		/**
		 * The object used to identify the server at the model
		 */
		private ProtocolHandle handle;

		/**
		 * The server socket channel that is used for listening to incoming connections
		 */
		private ServerSocketChannel channel;

		/**
		 * A factory that is able to create new engines to handle client
		 * connections
		 */
		private ServerProtocolEngineFactory factory;

		/**
		 * Constructor. It sets all necessary parameters for running the deamon
         *
         * @param handle
	     *            The object used to map the server at the model
		 * @param channel
		 *            The socket channel to listen to
		 * @param factory
		 *            The factory that builds new protocol engines
		 */
		public ServerDeamon(ProtocolHandle handle, ServerSocketChannel channel,
				ServerProtocolEngineFactory factory) {
		    BasePlugin.logDebugMessage("ServerDeamon","Creating a server deamon to listen to connections to port " +  //$NON-NLS-1$ //$NON-NLS-2$
		            channel.socket().getLocalPort());
		    this.handle = handle;
			this.channel = channel;
			this.factory = factory;
		}

		/**
		 * Runs the deamon
		 * 
		 * @see Runnable#run()
		 */
		public void run() {
		    BasePlugin.logDebugMessage("ServerDeamon","Starting the server deamon."); //$NON-NLS-1$ //$NON-NLS-2$
		    try {
				if ((channel != null) && (factory != null)) {				    
					while (true) {
						final SocketChannel sc;
						try {
						    BasePlugin.logDebugMessage("ServerDeamon","Listening to incoming connections."); //$NON-NLS-1$ //$NON-NLS-2$
							sc = channel.accept();
						} catch (IOException e) {
							// stopListeningToPort closes the server socket and
							// causes this exception to happen. The treatment
							// for this is to exit the while(true) block to
							// allow the cleanup to be performed (in finally
							// block) and then finish the thread.
							break;
						}

						BasePlugin.logInfo("A client has connected to port " + channel.socket().getLocalPort()); //$NON-NLS-1$
						BasePlugin.logDebugMessage("ServerDeamon","Creating a protocol engine to handle the protocol connection."); //$NON-NLS-1$ //$NON-NLS-2$
						final ProtocolEngine eng = factory.getServerProtocolEngine();
						eng.requestStart(sc, null);

						BasePlugin.logDebugMessage("ServerDeamon","Registering the protocol engine at Server Model."); //$NON-NLS-1$ //$NON-NLS-2$
						Collection<ProtocolEngine> allClients = connectedClients
								.get(handle);
						if (allClients == null) {
							allClients = new HashSet<ProtocolEngine>();
							connectedClients.put(handle,
									allClients);
						}
						allClients.add(eng);
					}					
				}
			} finally {
			    BasePlugin.logDebugMessage("ServerDeamon","Stopping the server deamon."); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					// Perform the cleanup before finishing the thread
					// execution. This includes stopping all connections
					// from server side and removing the created objects
					// from model
					if (channel.isOpen()) {
						channel.close();
					}

					BasePlugin.logDebugMessage("ServerDeamon","Unregistering all objects related to provided handle from Server Model."); //$NON-NLS-1$ //$NON-NLS-2$
					Collection<ProtocolEngine> allClients = connectedClients.get(handle);
					for (ProtocolEngine aClient : allClients) {
						aClient.requestStop();
						allClients.remove(aClient);
						aClient.dispose();
					}

					openedSocketChannels.remove(handle);
					engineFactories.remove(handle);
					connectedClients.remove(handle);

					BasePlugin.logInfo("Server deamon stopped."); //$NON-NLS-1$
				} catch (IOException e) {
					IProtocolExceptionHandler exceptionHandler = factory
							.getExceptionHandler();

					if (exceptionHandler != null) {
						exceptionHandler.handleIOException(handle, e);
					}
				}
			}
		}
	}
}
