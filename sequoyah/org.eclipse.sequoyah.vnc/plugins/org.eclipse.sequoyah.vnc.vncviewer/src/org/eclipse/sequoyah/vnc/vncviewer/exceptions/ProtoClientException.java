/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Franco (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.exceptions;



/**
 * Represents a general exception to be thrown by the client protocols.
 */
@SuppressWarnings("serial")
public class ProtoClientException extends Exception {


	
	public ProtoClientException() {

	}

	/**
	 * @param message the message used by the Exception.
	 */
	public ProtoClientException(String message) {
		super(message);
	}

	/**
	 * @param cause the associated cause.
	 */
	public ProtoClientException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message the message used by the Exception.
	 * @param cause the associated cause.
	 */
	public ProtoClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
