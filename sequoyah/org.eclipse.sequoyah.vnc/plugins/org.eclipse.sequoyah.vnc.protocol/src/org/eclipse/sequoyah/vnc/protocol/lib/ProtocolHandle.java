/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo - [246212] Enhance encapsulation of protocol implementer
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.protocol.lib;

/**
 * DESCRIPTION: This class is a special type of object used to represent protocols
 * being run in any of the models.<br>
 * 
 * RESPONSIBILITY: Identify a running protocol at one of the internal models 
 * outside the plugin.<br>
 * 
 * COLABORATORS: None.<br>
 * 
 * USAGE: Keep the object in a user module and:
 * - Provide it as parameter when requested by an API method which interacts 
 * with a certain connection. 
 * - Use it to identify connection related objects when it is provided by the 
 * protocol framework 
 */
public final class ProtocolHandle {
}
