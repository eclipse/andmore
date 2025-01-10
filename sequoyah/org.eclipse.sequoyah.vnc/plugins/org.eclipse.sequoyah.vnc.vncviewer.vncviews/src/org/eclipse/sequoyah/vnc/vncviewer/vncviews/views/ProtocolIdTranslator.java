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
 * Fabio Rigo - Bug [221741] - Support to VNC Protocol Extension
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.vncviews.views;

/**
 * This class translates a string that appears at UI to the correspondent
 * protocol ID from extension
 */
public class ProtocolIdTranslator {

	/**
	 * Returns a protocol id dynamically chosen.
	 * 
	 * @param prot
	 *            the String specifying the protocol.
	 * 
	 * @return the Protocol id or null if prot can't be associated to a known
	 *         protocol.
	 */
	public static String getProtocolId(String prot) {

		if (prot.equals("VNC 3.3")) { //$NON-NLS-1$
			return "vncProtocol33"; //$NON-NLS-1$

		} else if (prot.equals("VNC 3.7")) { //$NON-NLS-1$
			return "vncProtocol37"; //$NON-NLS-1$

		} else if (prot.equals("VNC 3.8")) { //$NON-NLS-1$
			return "vncProtocol38"; //$NON-NLS-1$
		}

		return null;
	}

}
