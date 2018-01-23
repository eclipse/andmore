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
 * Daniel Barboza Franco (Eldorado Research Institute) -  [243167] - Zoom mechanism not working properly 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248663] - Dependency between protocol and SWTRemoteDisplay
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [244249] - Canvas background repaint
 * Daniel Barboza Franco (Eldorado Research Institute) - [275650] - Canvas rotation
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.img;

import java.util.Properties;

import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay.Rotation;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.ISWTPainter;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class SWTRemoteDisplayImg extends SWTRemoteDisplay {


	public SWTRemoteDisplayImg(Composite parent, Properties configProperties, IPropertiesFileHandler propertiesFileHandler) {
		this(parent, configProperties, propertiesFileHandler, null);
	}
	
	public SWTRemoteDisplayImg(Composite parent, Properties configProperties, IPropertiesFileHandler propertiesFileHandler, ISWTPainter painter_){
		super(parent, configProperties, propertiesFileHandler);
		
		painter = painter_;
		
		if (painter == null) {
			painter = new Painter(this);
		}
		
		paintListener = new PaintListener() {
            public void paintControl(PaintEvent e)
            {
            	SWTRemoteDisplayImg.this.paintControl(e);
            }
        };
        

	}

	public synchronized void start(ProtocolHandle handle) throws Exception {
		super.start(handle);

		canvas.getDisplay().asyncExec(new Runnable(){
			public void run() {
				canvas.addPaintListener(paintListener);
			}

		});
		((Painter)painter).addSWTRemoteDisplayImg(this);
	}
	
	
	public synchronized void stop() {
		super.stop();
		((Painter)painter).removeSWTRemoteDisplayImg((SWTRemoteDisplayImg)this);
	}
	
	public IVNCPainter getPainter() {
		return (Painter) painter;
	}

	protected void redrawScreen() {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				Canvas c = getCanvas();
				if (c != null && !c.isDisposed()) {
					c.redraw();
				}
			}
		});
	}

	protected void redrawScreen(final int x, final int y, final int width,
			final int height) {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				Canvas c = getCanvas();
				if (c != null && !c.isDisposed()) {
					switch (getRotation()){

						case ROTATION_0DEG:
							c.redraw(x, y, width, height, false);
							break;
							
						//TODO: handle the other cases to improve performance
						/*
						case(ROTATION_90DEG_COUNTERCLOCKWISE):
							c.redraw(y, screenW - width, height, screenW - x, false);
							break;
						*/

						default:
							c.redraw();
							break;
					}

				}
			}
		});
	}

	public void dispose() {
		super.dispose();
		if (painter != null) {
			((Painter)painter).dispose();
			
			
			//TODO: REMOVESWTRD
			
		}
	}

	protected void paintControl(PaintEvent event) {
		
		ImageData id = painter != null? ((Painter)painter).getImageData():null;
		Image image = null;
		
		if (id != null) {
			id = id.scaledTo((int)(id.width * getZoomFactor()), (int)(id.height * getZoomFactor()));
			image = new Image(event.gc.getDevice(),id);
		}
		
		
		event.gc.setBackground(canvas.getBackground());
		event.gc.setForeground(canvas.getForeground());
		
		if (image == null) {
			event.gc.fillRectangle(event.x, event.y, event.width, event.height);
		} else {
			Rectangle r = image.getBounds();
			int w = Math.min(event.width, r.width);
			int h = Math.min(event.height, r.height);
		
			int drawX, drawY, drawW, drawH;
			drawX = drawY = drawW = drawH = 0;

			Transform rotation = new Transform(event.gc.getDevice());
			
			switch (getRotation()){
			
				case ROTATION_0DEG: 
					drawX = event.x;
					drawY = event.y;
					drawW = w;
					drawH = h;
					break;
				
				//TODO: transform the coordinates so that the drawn area is optimized according to the parameters in redrawScreen()
				case ROTATION_180DEG:
					drawX = 0;
					drawY = 0; 
					drawW = id.width;
					drawH = id.height;
					
					rotation.translate(id.width, id.height);
					rotation.rotate(Rotation.ROTATION_180DEG.value());
					event.gc.setTransform(rotation);
					break;

				case ROTATION_90DEG_CLOCKWISE:
					drawX = 0;
					drawY = 0; 
					drawW = id.width;
					drawH = id.height;
					
					rotation.translate(id.height, 0);
					rotation.rotate(Rotation.ROTATION_90DEG_CLOCKWISE.value());
					event.gc.setTransform(rotation);
					break;
				
				case ROTATION_90DEG_COUNTERCLOCKWISE:
					drawX = 0;
					drawY = 0; 
					drawW = id.width;
					drawH = id.height;
					
					rotation.translate(0, id.width);
					rotation.rotate(Rotation.ROTATION_90DEG_COUNTERCLOCKWISE.value());
					event.gc.setTransform(rotation);
					break;
			}
			

			event.gc.drawImage(image, drawX, drawY, drawW, drawH, drawX, drawY, drawW, drawH);			
			
			//event.gc.drawImage(image, event.x, event.y, w, h, event.x, event.y, w, h);

			//event.gc.drawImage(image, id.width - event.height, event.x, id.width - event.y, event.width, id.width - event.height, event.x, id.width - event.y, event.width);

			//event.gc.drawImage(image, 0,0, id.width,id.height, 0,0, id.width,id.height);
			
			
			
			/*
			event.gc.setForeground(new Color(event.gc.getDevice(), SWT.MAX,0,0));
			event.gc.drawLine(id.width - event.height, event.x, id.width - event.y, event.width);
			*/
			//event.gc.drawLine(event.x, event.y, w, h);
			//event.gc.drawImage(image, 0,0, id.width,id.height, 0,0, id.width,id.height);
			//event.gc.drawImage(image, 0,0, 500, 500, 0,0, 1000 , 1000);
			
			
			/* TODO : Bug 244249 - Canvas background repaint
			if (w < event.width) {
				event.gc.fillRectangle(event.x + w, event.y, event.width - w,
						event.height);
			}
			if (h < event.height) {
				event.gc.fillRectangle(event.x, event.y + h, event.width,
						event.height - h);
			}
			*/
			
			
			image.dispose();
		}
	}
	
}
