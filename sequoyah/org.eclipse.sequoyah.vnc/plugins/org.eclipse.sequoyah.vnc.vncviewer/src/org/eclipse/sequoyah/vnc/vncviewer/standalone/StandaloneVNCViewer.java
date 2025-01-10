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
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.standalone;


import java.util.Properties;

import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.imgdata.SWTRemoteDisplayImgData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;


/**
 * This class is a standalone implementation of a VNC Viewer.
 * It's an example of how to use the VNC Display component.
 * This component is implemented within the SWTDisplay class and it is completely independent
 * from Eclipse, thus it can be used without the need of an Eclipse plugin, or even Eclipse. 
 */
public class StandaloneVNCViewer {

	private static Canvas canvas;
	private static int WINDOW_WIDTH = 700;
	private static int WINDOW_HEIGHT = 500;
	private static String host = "127.0.0.1"; //$NON-NLS-1$
	private static int port = 5900;
	
	
	/**
	 * Adds a key event listener to the GUI and handles key events.
	 * @param parent the parent Composite to use.
	 * */
	public static void addKeyEventListener(Composite parent) {
		
		final SWTRemoteDisplay vncswt = new SWTRemoteDisplay(parent, new Properties(), null);
		final Display display = parent.getDisplay();

		try {
			//vncswt.start(host, port, null);
			vncswt.start(null);
		} catch (Exception e){
			log(StandaloneVNCViewer.class).error("The viewer can not be started."); //$NON-NLS-1$
		}
	
		canvas = vncswt.getCanvas();
		canvas.setFocus();
		
		Listener listener = new Listener(){
			public void handleEvent(Event event){
				final Event e = event;
				
				display.syncExec(new Runnable(){
					public void run(){
					
						synchronized (vncswt.getCanvas()) {

							try {
								vncswt.keyEvent(e);
								vncswt.updateScreen();
							} catch (Exception e1) {
								log(StandaloneVNCViewer.class).error("VNC View error on key event."); //$NON-NLS-1$
							}
							
							canvas.redraw();

						}
					}
				});
			}
		
		};
		
		canvas.addListener(SWT.KeyDown, listener );
	}

	
	/**
	 * Stays in a loop performing updates on the screen buffer.
	 */
	public static void updateScreenLoop(Composite parent) {
		
		final SWTRemoteDisplay vncswt = new SWTRemoteDisplayImgData(parent, new Properties(), null);
	
		try {
			//vncswt.start(host, port, null);
			vncswt.start(null);
		} catch (Exception e){
			log(StandaloneVNCViewer.class).error("The viewer can not be started."); //$NON-NLS-1$
		}
		
		while (!parent.isDisposed()) {
		
			try {
				vncswt.updateScreen();
				vncswt.wait(500);
			}
			catch (Exception e){
				log(StandaloneVNCViewer.class).error("Error on Loop test."); //$NON-NLS-1$
			}

		}
		
	}
	
	
	public static void main(String [] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);
		
		if (args.length == 2) {
			host = args[0];
			port = (Integer.valueOf(args[1])).intValue();
		}
		else{
			log(StandaloneVNCViewer.class).error("[host] [port] expected."); //$NON-NLS-1$
			return;
		}
		
		shell.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
		shell.open();
	
		updateScreenLoop(shell);
		display.dispose();
	}

}


