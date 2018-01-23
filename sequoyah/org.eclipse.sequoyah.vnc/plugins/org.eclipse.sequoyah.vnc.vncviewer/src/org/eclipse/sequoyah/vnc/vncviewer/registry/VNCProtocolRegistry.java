/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and Others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo (Eldorado Research Institute) 
 * [246212] - Enhance encapsulation of protocol implementer
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/


package org.eclipse.sequoyah.vnc.vncviewer.registry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;

public class VNCProtocolRegistry {

	private static VNCProtocolRegistry instance;

	private Map<ProtocolHandle, VNCProtocolData> protocolMapping = new HashMap<ProtocolHandle, VNCProtocolData>();

	private VNCProtocolRegistry() {
	}

	public static VNCProtocolRegistry getInstance() {
		if (instance == null) {
			instance = new VNCProtocolRegistry();
		}
		return instance;
	}

	public void register(ProtocolHandle handle, VNCProtocolData data) {
		
		// There is only 1 pair handle/data
		protocolMapping.remove(handle);
		protocolMapping.put(handle, data);
	}

	public void unregister(ProtocolHandle handle) {
		protocolMapping.remove(handle);
	}

	public VNCProtocolData get(ProtocolHandle handle) {
		return protocolMapping.get(handle);
	}
}
