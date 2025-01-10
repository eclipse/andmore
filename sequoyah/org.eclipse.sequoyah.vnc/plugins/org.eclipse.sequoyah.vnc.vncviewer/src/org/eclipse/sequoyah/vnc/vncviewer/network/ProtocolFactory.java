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
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah 
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.network;

import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;

/**
 * This class implements the "abstract factory method" pattern.
 * It instantiates objects for Protocols chosen at runtime.
 */
public class ProtocolFactory {


	/**
	 * Returns a protocol dynamically chosen.
	 * 
	 * @param prot the String specifying the protocol.
	 * 
	 * @return the Protocol instance or null if prot can't be associated to a known protocol.
	 */
	public static IProtocolHandshake getProtocol (String prot) {
		
		if (prot.equals("VNC 3.3")) { //$NON-NLS-1$
			return (VNCProtocol) (new VNCProtocol33());
		}
		else if (prot.equals("VNC 3.7")){ //$NON-NLS-1$
			return (VNCProtocol) (new VNCProtocol37());
		}
		else if (prot.equals("VNC 3.8")){ //$NON-NLS-1$
			return (VNCProtocol) (new VNCProtocol38());
		}

		return null;
	}

}
