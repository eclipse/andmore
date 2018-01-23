/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and Others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo (Eldorado Research Institute) 
 *
 * Contributors:
 * Daniel Barboza Franco (Motorola) - Integration with code from bug 227793 to correctly deal with the redesigned painting process.
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.network.handlers;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.sequoyah.vnc.protocol.lib.IMessageFieldsStore;
import org.eclipse.sequoyah.vnc.protocol.lib.IRawDataHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;
import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;

/**
 * DESCRIPTION: This class consists of the reader for the framebuffer
 * rectangles, which is part of the Framebuffer Update message.<br>
 * 
 * 
 * RESPONSIBILITY: Provide the protocol framework with framebuffer data.<br>
 * 
 * COLABORATORS: None<br>
 * 
 * USAGE: This class is intended to be used by Eclipse.<br>
 * 
 */
public class FramebufferRectanglesHandler implements IRawDataHandler {

	public Map<String, Object> readRawDataFromStream(ProtocolHandle handle,
			DataInput dataStream, IMessageFieldsStore currentlyReadFields,
			boolean isBigEndian) throws IOException,
			ProtocolRawHandlingException {

		Map<String, Object> fieldsMap = new HashMap<String, Object>();

		VNCProtocolData protocolData = VNCProtocolRegistry.getInstance().get(
				handle);

		if (protocolData != null) {
			// Collects the painter where the rectangles will be processed
			// from the protocol data instance
			IVNCPainter painter = protocolData.getVncPainter();

			int x = (Integer) currentlyReadFields.getFieldValue("x-position"); //$NON-NLS-1$
			int y = (Integer) currentlyReadFields.getFieldValue("y-position"); //$NON-NLS-1$
			int width = (Integer) currentlyReadFields.getFieldValue("width"); //$NON-NLS-1$
			int height = (Integer) currentlyReadFields.getFieldValue("height"); //$NON-NLS-1$
			int encoding = (Integer) currentlyReadFields
					.getFieldValue("encodingType"); //$NON-NLS-1$

			// Process the rectangle data into the painter
			try {
				painter.processRectangle(new RectHeader(x, y, width, height,
						encoding), dataStream);
			} catch (IOException e) {
			    throw e;
			} catch (Exception e) {
				throw new ProtocolRawHandlingException(e);
			}
		}

		return fieldsMap;
	}

	public void writeRawDataToStream(ProtocolHandle handle,
			ByteArrayOutputStream dataStream,
			ProtocolMessage messageToGetInformationFrom, boolean isBigEndian)
			throws ProtocolRawHandlingException {

		// No implementation. This is a client plugin only
	}
}
