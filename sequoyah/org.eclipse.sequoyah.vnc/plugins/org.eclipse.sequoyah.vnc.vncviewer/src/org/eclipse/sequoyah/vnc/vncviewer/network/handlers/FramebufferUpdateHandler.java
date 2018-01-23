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
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.network.handlers;

import org.eclipse.sequoyah.vnc.protocol.lib.IMessageHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;

/**
 * DESCRIPTION: This class consists of the Framebuffer Update message handler.<br>
 * 
 * 
 * RESPONSIBILITY: Handle the Framebuffer Update message after it is completely
 * read from the socket
 * 
 * COLABORATORS: None<br>
 * 
 * USAGE: This class is intended to be used by Eclipse.<br>
 * 
 */
public class FramebufferUpdateHandler implements IMessageHandler {

	public ProtocolMessage handleMessage(ProtocolHandle handle,
			ProtocolMessage message) {

	    VNCProtocolData protocolData = VNCProtocolRegistry.getInstance().get(
	                handle);
	    if (protocolData != null) {
	        IVNCPainter painter = protocolData.getVncPainter();
	        
	        // Determine which area of the screen shall be redrawn. It comprises the minimum 
	        // rectangle that contains all the rectangles that were sent by this message  
	        int minX = painter.getWidth();
	        int minY = painter.getHeight();
	        int maxX = 0;
	        int maxY = 0;
	        int numRect = (Integer) message.getFieldValue("numberOfRectangles"); //$NON-NLS-1$
	        
	        for (int rect = 0; rect < numRect; rect++) {
	            int rectX1 = (Integer) message.getFieldValue("x-position", "rectangle", rect); //$NON-NLS-1$ //$NON-NLS-2$
	            int rectY1 = (Integer) message.getFieldValue("y-position", "rectangle", rect); //$NON-NLS-1$ //$NON-NLS-2$
	            int rectX2 = rectX1 + ((Integer) message.getFieldValue("width", "rectangle", rect)); //$NON-NLS-1$ //$NON-NLS-2$
	            int rectY2 = rectY1 + ((Integer) message.getFieldValue("height", "rectangle", rect)); //$NON-NLS-1$ //$NON-NLS-2$

	            minX = Math.min(minX, rectX1);
	            minY = Math.min(minY, rectY1);
	            maxX = Math.max(maxX, rectX2);
	            maxY = Math.max(maxY, rectY2);
	        }
	        
	        painter.updateRectangle(minX, minY, maxX, maxY);
	    }
	    
		return null;
	}
}
