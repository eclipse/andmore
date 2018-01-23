/********************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 *
 * Contributors:
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Barboza Franco (Eldorado Research Institute) - [271205] - Remove log for mouse, keyboard and screen events
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.graphics;

import java.io.DataInput;

import org.eclipse.sequoyah.vnc.vncviewer.network.RectHeader;

public class RREPaintStrategy extends AbstractPaintStrategy {

	public RREPaintStrategy(IPainterContext context) {
		super(context);
	}
	
	public void processRectangle(RectHeader rh, DataInput in) throws Exception {
		int x = rh.getX();
		int y = rh.getY();
		int width = rh.getWidth();
		int height = rh.getHeight();
		int subrectsCount = in.readInt();

/*		log(RREPaintStrategy.class).debug("Processing rectangle defined by: x=" + x + "; y=" + y + 
		        "; w=" + width + "; h=" + height + "subrects=" + subrectsCount + ".");*/
		
	    int backgroundPixel = getContext().readPixel(in);
        getContext().fillRect(backgroundPixel, x, y, width, height);

	    for (int i = 0; i < subrectsCount; i++) {
		    int subRectBackgroundPixel = getContext().readPixel(in); 
		    int subRectX = x + in.readUnsignedShort();
		    int subRectY = y + in.readUnsignedShort();
		    int subRectWidth = in.readUnsignedShort();
		    int subRectHeight = in.readUnsignedShort();
		    getContext().fillRect(subRectBackgroundPixel, subRectX, subRectY, subRectWidth, subRectHeight);
	    }
	}

}
