/********************************************************************************
 * Copyright (c) 2010 Motorola Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Barboza Franco (Eldorado)
 * 
 * Contributors:
 * Contributor (Company) - Bug [NUMBER] - Bug Description
 * 
 * For more information and instructions of how to run this test, please refer to http://wiki.eclipse.org/Sequoyah/unit_test 
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.junits;

import java.util.HashMap;

import junit.framework.TestCase;

import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolExceptionHandler;

public class PluginProtocolActionDelegateTest extends TestCase {

	public void testStart() {
		
		String host = "localhost";
		int port = 5900;
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		String protocolId = "vncProtocol38";

		//parameters.put("password", null); //$NON-NLS-1$
		parameters.put("connectionRetries", 5);
		parameters.put("bypassProxy", true); //$NON-NLS-1$
		
		ProtocolHandle protocolHandle = null;
		
		try {
			protocolHandle = PluginProtocolActionDelegate
					.requestStartProtocolAsClient(protocolId,
							new VNCProtocolExceptionHandler(), host, port,
							parameters);
		
		} catch (Exception e) {
			
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(protocolHandle);
		int counter = 0;
        while(!PluginProtocolActionDelegate.isProtocolRunning(protocolHandle)) {
            
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
            if (++counter == 10) {
            	fail(counter * 2 + "seconds have passed without a sucessful connection");
            }
        }

	}

}
