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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.imgdata;

import java.util.Properties;

import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class SWTRemoteDisplayImgData extends SWTRemoteDisplay {

	private Painter painter; 
	private Image screen = null;
	
	public SWTRemoteDisplayImgData(Composite parent, Properties configProperties, IPropertiesFileHandler propertiesFileHandler){
		super(parent, configProperties, propertiesFileHandler);
		painter = new Painter(this);
	}

	public IVNCPainter getPainter() {
		return painter;
	}

	protected void redrawScreen() {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (screen != null) {
					screen.dispose();
				}
				Canvas c = getCanvas();
				if (c != null && !c.isDisposed()) {
					screen = new Image(
							getCanvas().getDisplay(),
							painter
									.getImageData()
									.scaledTo(
											(int) (painter.getWidth() * getZoomFactor()),
											(int) (painter.getHeight() * getZoomFactor())));
					GC gc = new GC(getCanvas());
					gc.drawImage(screen, 0, 0);
					gc.dispose();
				}
			}
		});
	}

	public void dispose() {
		super.dispose();
		if (screen != null) {
			screen.dispose();
		}
	}
	
}
