/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and Others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Franco (Eldorado Research Institute)
 *
 * Contributors:
 * Fabio Rigo (Eldorado Research Institute) - [221741] - Support to VNC Protocol Extension
 * Eugene Melekhov (Montavista) - [227793] - Implementation of the several encodings, performance enhancement etc
 * Daniel Barboza Franco (Eldorado Research Institute) - [233775] - Does not have a way to enter the session password for the vnc connection
 * Daniel Barboza Franco (Eldorado Research Institute) - [233062] - Protocol connection port is static.
 * Fabio Rigo (Eldorado Research Institute) -  [238191] - Enhance exception handling
 * Daniel Barboza Franco (Eldorado Research Institute) -  [233064] - Add reconnection mechanism to avoid lose connection with the protocol
 * Fabio Rigo (Eldorado Research Institute) -  [246212] - Enhance encapsulation of protocol implementer
 * Daniel Barboza Franco (Eldorado Research Institute) -  [243167] - Zoom mechanism not working properly 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233121] - There is no support for proxies when connecting the protocol 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246585] - VncViewerService is not working anymore after changes made in ProtocolHandle
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248663] - Dependency between protocol and SWTRemoteDisplay
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [244249] - Canvas background repaint
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Petr Baranov (Nokia) - Bug [262371] (reopened) - New Connection Dialog improvement
 * Ed Swartz (Nokia) - Bug [286280] - handle some SWT disposing issues
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.vncviews.views;

import static org.eclipse.sequoyah.vnc.vncviewer.VNCViewerPlugin.log;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.RemoteDisplayFactory;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolExceptionHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * The VNCViewerView class implements the Eclipse View that contains a VNC
 * viewer.
 */
public class VNCViewerView extends ViewPart {

	private static final String VIEWER_COULD_NOT_BE_STARTED = "Viewer could not be started: "; //$NON-NLS-1$

	private static final String SWTDISPLAY = "SWTDisplay"; //$NON-NLS-1$

	private static SWTRemoteDisplay swtDisplay;

	private static boolean running = false;

	public static ProtocolHandle protocolHandle;
	
	private static int zoomFactor = 1;

	private static String[] currentConnection = {"", ""}; //$NON-NLS-1$ //$NON-NLS-2$

	private static String currentHost;

	private static String currentPort;

	public void createPartControl(Composite parent) {

		final ScrolledComposite scomposite = new ScrolledComposite(parent, SWT.H_SCROLL |   
				  SWT.V_SCROLL | SWT.BORDER);
		
		swtDisplay = (SWTRemoteDisplay) RemoteDisplayFactory.getDisplay(
				SWTDISPLAY, scomposite);
		running = true;
		
		scomposite.setContent(swtDisplay);
		scomposite.setExpandHorizontal(true);
		scomposite.setExpandVertical(true);
		
		swtDisplay.getCanvas().addControlListener(new ControlListener(){
			public void controlMoved(ControlEvent e) {
				
			}

			public void controlResized(ControlEvent e) {
				Point size = swtDisplay.getCanvas().getSize();
				scomposite.setMinSize(swtDisplay.getParent().computeSize(size.x, size.y));
			}
		});

		swtDisplay.getCanvas().setSize(0, 0);
		
		if (VNCViewerView.protocolHandle != null) {
			try {
				swtDisplay.start(protocolHandle);
			} catch (Exception e) {
				// TODO handle properly
				e.printStackTrace();
			}
		}

	}

	public void setFocus() {
		if (swtDisplay != null) {
			swtDisplay.setFocus();
		}
	}

	public void dispose() {

		running = false;
		if (swtDisplay != null)
			swtDisplay.dispose();
		super.dispose();
	}

	/**
	 * Performs the start action into the VNC Component.
	 */
	synchronized public static void start(String host, int port,
			String protoVersion, String password, boolean bypassProxy) {

		if ((running) && (swtDisplay != null)) {

			if (swtDisplay.isActive()) {
				swtDisplay.stop();
			}

			try {

				HashMap<String, Object> parameters = new HashMap<String, Object>();
				String protocolId = ProtocolIdTranslator
						.getProtocolId(protoVersion);

				parameters.put("password", password); //$NON-NLS-1$
				parameters.put("connectionRetries", swtDisplay //$NON-NLS-1$
						.getConnectionRetries());
				
				parameters.put("bypassProxy", new Boolean(bypassProxy)); //$NON-NLS-1$
				
				VNCViewerView.protocolHandle = PluginProtocolActionDelegate
						.requestStartProtocolAsClient(protocolId,
								new VNCProtocolExceptionHandler(), host, port,
								parameters);

                while(!PluginProtocolActionDelegate.isProtocolRunning(VNCViewerView.protocolHandle)) {
                    Thread.sleep(500);
                }
                
				swtDisplay.start(protocolHandle);
				currentHost = host;
				currentPort = Integer.toString(port);

			} catch (Exception e) {

				log(VNCViewerView.class).error(
						VIEWER_COULD_NOT_BE_STARTED + e.getMessage());
				
				Canvas canvas = swtDisplay.getCanvas();
				if ((canvas != null) && (!canvas.isDisposed())) {
					GC gc = new GC(swtDisplay.getCanvas());
					gc.fillRectangle(0, 0, swtDisplay.getScreenWidth(), swtDisplay
							.getScreenHeight());
					gc.dispose(); 
				}

			}
		}
	}

	/**
	 * Performs the stop action into the VNC Component.
	 */
	synchronized public static void stop() {

		if ((running) && (swtDisplay != null)) {

			if (swtDisplay.isActive()) {
				swtDisplay.getDisplay().syncExec(new Runnable(){
					public void run() {
						swtDisplay.stop();	
						zoomFactor = 1;
						swtDisplay.setZoomFactor(zoomFactor);
						swtDisplay.getCanvas().setSize(0, 0);
					}
	
				});
				
			}

		}

	}
	
	public static void zoomIn(){
		//double zoom = swtDisplay.getZoomFactor();
		
		double newzoom = 1;
		
		if (!swtDisplay.isActive()) return;
		
		if (zoomFactor == -2) {
			zoomFactor = 1;
		} else {
			zoomFactor++;
		}
			
		if (zoomFactor >= 1) {
			newzoom = zoomFactor;
		}
		else {
			newzoom = ((double)1) / -zoomFactor;
		}
		
		swtDisplay.setZoomFactor(newzoom);
		try {
			swtDisplay.updateRequest(false); //full request
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void zoomOut(){

		double newzoom = 1;
		
		if (!swtDisplay.isActive()) return;
		
		if (zoomFactor == 1) {
			zoomFactor = -2;
		} else {
			zoomFactor--;
		}
			
		if (zoomFactor >= 1) {
			newzoom = zoomFactor;
		}
		else {
			newzoom = ((double)1) / -zoomFactor;
		}
		
		swtDisplay.setZoomFactor(newzoom);
		try {
			swtDisplay.updateRequest(false); //full request
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void stopProtocol() throws IOException {

		if (protocolHandle != null){
			PluginProtocolActionDelegate.requestStopProtocol(protocolHandle);
		}
	}

	public static SWTRemoteDisplay getSWTRemoteDisplay() {
		return swtDisplay;
	}

	public static String getCurrentHost() {
		return currentHost;
	}

	public static String getCurrentPort() {
		return currentPort;
	}


}
