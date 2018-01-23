/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Barboza Franco (Motorola)
 *
 * Contributors:
 * Fabio Rigo - Bug [221741] - Support to VNC Protocol Extension
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Fabio Rigo(Eldorado Research Institute) - Bug [244062] - SWTRemoteDisplay do not force the first update request to be full
 * Fabio Rigo(Eldorado Research Institute) - Bug [244806] - SWTRemoteDisplay state is not consistent on errors
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233064] - Add reconnection mechanism to avoid lose connection with the protocol
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Barboza Franco (Eldorado Research Institute) -  [243167] - Zoom mechanism not working properly 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246585] - VncViewerService is not working anymore after changes made in ProtocolHandle
 * Leo Andrade (Eldorado Research Institute) - Bug [247973] - Listener to key events is not working at SWTRemoteDisplay
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248663] - Dependency between protocol and SWTRemoteDisplay
 * Leo Andrade (Eldorado Research Institute) - Bug [247973] - notifyListeners(ev.type, ev) into addMouseListener.
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [244249] - Canvas background repaint
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [265043] - Mouse problems when performing zoom
 * Daniel Barboza Franco (Eldorado Research Institute) - [221740] - Sample implementation for Linux host
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Barboza Franco (Eldorado Research Institute) - [271205] - Remove log for mouse, keyboard and screen events
 * Daniel Barboza Franco (Eldorado Research Institute) - [275650] - Canvas rotation
 * Ed Swartz (Nokia) - Bug [286280] - handle some SWT disposing issues
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.graphics.swt;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.config.IVNCProperties;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay.Rotation;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.img.Painter;
import org.eclipse.sequoyah.vnc.vncviewer.network.IVNCPainter;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;

/**
 * This class implements the GUI part of a Remote Desktop Viewer. It also uses a
 * protocol to send local events and receive server updates.
 */
public class SWTRemoteDisplay extends Composite implements IRemoteDisplay {

	private static final String REMOTE_DISPLAY_STOP_ERROR = "Remote Display Stop error: "; //$NON-NLS-1$
	private static final String REMOTE_DISPLAY_MOUSE_EVENT_ERROR = "Remote Display mouse event error : "; //$NON-NLS-1$
	private static final String REMOTE_DISPLAY_KEY_EVENT_ERROR = "Remote Display key event error : "; //$NON-NLS-1$
	// private IProtocolImplementer protoClient;
	protected Canvas canvas;
	private ProtocolHandle handle;
	private VNCProtocolData protoClient;
	private Image screen = null;
	// private ImageData imgData;

	private Properties configurationProperties;
	private SWTVNCEventTranslator eventTranslator;

	private boolean active = false;

	private long firstRefreshDelayMs; /*
										 * Time in milliseconds for the first
										 * update
										 */
	private long refreshDelayPeriodMs; /*
										 * Time in milliseconds between 2
										 * updates
										 */
	private int connectionRetries;

	private Timer refreshTimer;
	private Listener keyListener;
	private Listener mouseListener;
	protected PaintListener paintListener = null;
	
	private double zoomFactor;

	protected ISWTPainter painter;

	private IPropertiesFileHandler propertiesFileHandler;
	private Rotation rotation = Rotation.ROTATION_0DEG;
	

	/**
	 * @param parent
	 *            the Composite to be used as the GUI components parent.
	 * @param configProperties
	 *            the properties set for configuration purposes.
	 * @param propertiesFileHandler
	 *            the handler for Properties Files.
	 */
	public SWTRemoteDisplay(Composite parent, Properties configProperties,
			IPropertiesFileHandler propertiesFileHandler) {

		super(parent, SWT.BACKGROUND);
        log(SWTRemoteDisplay.class).debug("Constructing SWT Remote Display"); //$NON-NLS-1$
		
		configurationProperties = configProperties;
		this.propertiesFileHandler = propertiesFileHandler;

		this.setLayout(parent.getLayout());

		//canvas = new Canvas(this, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
		canvas = new Canvas(this, SWT.NO_BACKGROUND);
		
		eventTranslator = new SWTVNCEventTranslator(configProperties,
				propertiesFileHandler);

		initConfiguration();

	}

	protected int getCanvasStyle() {
		return SWT.BACKGROUND;
	}

	private void initConfiguration() {

		connectionRetries = Integer.valueOf(
				(configurationProperties
						.getProperty(IVNCProperties.CONNECTION_RETRIES)))
				.intValue();

		zoomFactor = Double
				.valueOf(
						configurationProperties
								.getProperty(IVNCProperties.ZOOM_FACTOR))
				.doubleValue();

		firstRefreshDelayMs = Long.valueOf(
				configurationProperties
						.getProperty(IVNCProperties.FIRST_REFRESH_DELAY_MS))
				.longValue();
		refreshDelayPeriodMs = Long.valueOf(
				configurationProperties
						.getProperty(IVNCProperties.REFRESH_DELAY_PERIOD_MS))
				.longValue();

		log(SWTRemoteDisplay.class).info("Using screen parameters: retries=" + connectionRetries +  //$NON-NLS-1$
		        "; zoomFactor=" + zoomFactor +  //$NON-NLS-1$
		        "; firstRefreshDelay(ms)=" + firstRefreshDelayMs +  //$NON-NLS-1$
		        "; refreshDelay(ms)=" + refreshDelayPeriodMs + "."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Adds a KeyListener.
	 */
	private void addKeyListener() {
		keyListener = new Listener() {
			public void handleEvent(Event event) {
				final Event ev = event;

				if (isActive()) {

					try {
						keyEvent(ev);
						updateRequest(true);
						notifyListeners(ev.type, ev);
					} catch (Exception e) {
						log(SWTRemoteDisplay.class).error(
								"Remote Display error on key event."); //$NON-NLS-1$
					}
				}
			}

		};

		getCanvas().getDisplay().syncExec(new Runnable() {
			public void run() {
				canvas.addListener(SWT.KeyDown, keyListener);
				canvas.addListener(SWT.KeyUp, keyListener);
			}
		});
	}

	/**
	 * Adds a MouseListener.
	 */
	private void addMouseListener() {
		mouseListener = new Listener() {
			public void handleEvent(Event event) {
				final Event ev = event;

				if (isActive()) {

					try {
						mouseEvent(ev);
						notifyListeners(ev.type, ev);
					} catch (Exception e) {
						log(SWTRemoteDisplay.class).error(
								"Remote Display error on key event."); //$NON-NLS-1$
					}
				}
			}
		};

		getCanvas().getDisplay().syncExec(new Runnable() {
			public void run() {
				canvas.addListener(SWT.MouseMove, mouseListener);
				canvas.addListener(SWT.MouseUp, mouseListener);
				canvas.addListener(SWT.MouseDown, mouseListener);

			}
		});
	}

	/**
	 * Adds a timer that schedules the screen's update in a fixed period.
	 */
	private void addRefreshTimer() {

		refreshTimer = new Timer();
		final Display display = this.getDisplay();
		final SWTRemoteDisplay swtDisplay = this;

		display.syncExec(new Runnable() {
			public void run() {
				try {
					// The first request must not be incremental
					updateRequest(false);
				} catch (Exception e) {
					// If the first request fails, ignore it.
                    log(SWTRemoteDisplay.class).warn(
                            "The first full screen request has failed. Cause: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		});

		final Runnable updateRefresh = new Runnable() {
			public void run() {
				try {
					updateRequest(true);
				} catch (Exception e) {
					stop();
					log(SWTRemoteDisplay.class).error(
							"Update screen error: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		};
		
		refreshTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (!display.isDisposed()) {
					display.syncExec(updateRefresh);
				}
			}
		}, firstRefreshDelayMs, refreshDelayPeriodMs);

	}

	public void restart() throws Exception {
	    log(SWTRemoteDisplay.class).info("Restarting SWT remote display."); //$NON-NLS-1$
		stop();
		PluginProtocolActionDelegate.requestRestartProtocol(handle);
		start(handle);
		log(SWTRemoteDisplay.class).info("SWT remote display restarted."); //$NON-NLS-1$
	}

	synchronized public void start(ProtocolHandle handle) throws Exception {

	    log(SWTRemoteDisplay.class).info("Starting SWT remote display."); //$NON-NLS-1$
		this.handle = handle;
		protoClient = VNCProtocolRegistry.getInstance().get(handle);

		if (protoClient != null) {
			try {

				protoClient.setPaintEnabled(true);
				protoClient.setVncPainter((IVNCPainter) painter);

				((Painter) painter)
						.setPixelFormat(protoClient.getPixelFormat());
				((IVNCPainter) painter).setSize(protoClient.getFbWidth(),
						protoClient.getFbHeight());

				canvas.getDisplay().asyncExec(new Runnable(){
					public void run() {

						if (canvas.isDisposed()){
							return;
						}

						switch (SWTRemoteDisplay.this.getRotation()) {
						
							case ROTATION_0DEG: 
							case ROTATION_180DEG:
								canvas.setSize(protoClient.getFbWidth(), protoClient.getFbHeight());
								break;
							
							case ROTATION_90DEG_CLOCKWISE: 
							case ROTATION_90DEG_COUNTERCLOCKWISE:
								canvas.setSize(protoClient.getFbHeight(), protoClient.getFbWidth());								
								break;
							
						}
						//canvas.setSize(protoClient.getFbWidth(), protoClient.getFbHeight());

					}
				});
				
				addRefreshTimer();
				addKeyListener();
				addMouseListener();
				
				// This code solves synchronization issues concerning mouse devices
				Event event = new Event();
				event.x = 0;
				event.y = 0;
				this.mouseEvent(event);
				

			} catch (Exception e) {
				log(SWTRemoteDisplay.class).error(
						"Remote Display start error: " + e.getMessage()); //$NON-NLS-1$

			}

			setRunning(true);
			log(SWTRemoteDisplay.class).info("SWT remote display started."); //$NON-NLS-1$
		}		
	}

	public Rotation getRotation() {
		return rotation ;
	}

	synchronized public void stop() {
	    log(SWTRemoteDisplay.class).info("Stopping SWT remote display."); //$NON-NLS-1$
		setRunning(false);

		refreshTimer.cancel();

		canvas.getDisplay().syncExec(new Runnable(){
			public void run() {
				canvas.removeListener(SWT.KeyUp, keyListener);
				canvas.removeListener(SWT.KeyDown, keyListener);
				canvas.removeListener(SWT.MouseMove, mouseListener);
				canvas.removeListener(SWT.MouseUp, mouseListener);
				canvas.removeListener(SWT.MouseDown, mouseListener);
				canvas.removePaintListener(paintListener);
			}
			
		});

		// repaints the background to the default color
		if (canvas != null) {
			Point p = canvas.getSize();
			GC gc = new GC(canvas);
			gc.fillRectangle(0, 0, p.x, p.y);
			gc.dispose();
		}

		canvas.getDisplay().asyncExec(new Runnable(){
			public void run() {
				if ((canvas != null) && (!canvas.isDisposed())) {
					canvas.setSize(0, 0);
				}
			}
		});
		
		log(SWTRemoteDisplay.class).info("SWT remote display stopped."); //$NON-NLS-1$

	}

	/**
	 * Request a frame-buffer update to the RFB Server.
	 * 
	 * @param incremental True if the request should be based on a previous frame-buffer.
	 * See the RFB Protocol Specification for more information. 
	 * 
	 */
	public void updateRequest(boolean incremental) throws Exception {

		try {
			// Creates a update request message, setting the request
			// area to be the whole screen, and defining the incremental
			// field equal to the provided parameter
			ProtocolMessage message = new ProtocolMessage(3);
			message.setFieldValue("x-position", 0); //$NON-NLS-1$
			message.setFieldValue("y-position", 0); //$NON-NLS-1$
			message.setFieldValue("width", painter.getWidth()); //$NON-NLS-1$
			message.setFieldValue("height", painter.getHeight()); //$NON-NLS-1$
			
			message.setFieldValue("incremental", incremental ? 1 : 0); //$NON-NLS-1$

			PluginProtocolActionDelegate.sendMessageToServer(handle, message);
		} catch (Exception e) {
			log(SWTRemoteDisplay.class).error(
					"Remote Display update screen error : " + e.getMessage()); //$NON-NLS-1$
			stop();

		}

		if (screen != null) {
			screen.dispose();
		}

	}

	public void updateScreen() throws Exception {
		updateRequest(true);
	}

	public void keyEvent(Event event) throws Exception {

		try {

			ProtocolMessage message = eventTranslator.getKeyEventMessage(event);
			PluginProtocolActionDelegate.sendMessageToServer(handle, message);
			//log(SWTRemoteDisplay.class).debug("Sent key event"); //$NON-NLS-1$

		} catch (Exception e) {
			log(SWTRemoteDisplay.class).error(
					REMOTE_DISPLAY_KEY_EVENT_ERROR + e.getMessage());
			stop();

		}
	}

	public void mouseEvent(Event event) throws Exception {

		try {
			ProtocolMessage message = eventTranslator
					.getMouseEventMessage(event);
			

			Integer x = (Integer) message.getFieldValue("x-position"); //$NON-NLS-1$
			Integer y = (Integer) message.getFieldValue("y-position"); //$NON-NLS-1$

			int mouseX = (int) ((double)x / zoomFactor);
			int mouseY = (int) ((double)y / zoomFactor); 
			switch (getRotation()) {
			
				case ROTATION_0DEG:
					mouseX = (int) ((double)x / zoomFactor);
					mouseY = (int) ((double)y / zoomFactor);
					break;
					
				case ROTATION_180DEG:
					mouseX = painter.getImageData().width - (int) ((double)x / zoomFactor);
					mouseY = painter.getImageData().height - (int) ((double)y / zoomFactor);
					break;
	
				case ROTATION_90DEG_CLOCKWISE:
					mouseX = (int) ((double)y / zoomFactor);
					mouseY = painter.getImageData().height - ((int) ((double)x / zoomFactor));;
					break;

				case ROTATION_90DEG_COUNTERCLOCKWISE:
					mouseX = painter.getImageData().width - ((int) ((double)y / zoomFactor));
					mouseY = (int) ((double)x / zoomFactor);
					break;
			
			}

			message.setFieldValue("x-position", mouseX); //$NON-NLS-1$
			message.setFieldValue("y-position", mouseY); //$NON-NLS-1$
			
			PluginProtocolActionDelegate.sendMessageToServer(handle, message);
			//log(SWTRemoteDisplay.class).debug("Sent mouse event"); //$NON-NLS-1$

		} catch (Exception e) {
			log(SWTRemoteDisplay.class).error(
					REMOTE_DISPLAY_MOUSE_EVENT_ERROR + e.getMessage());
			stop();

		}
	}

	/**
	 * Gets the Canvas used to show the screen.
	 * 
	 * @return the Canvas object.
	 */
	public Canvas getCanvas() {
		return canvas;
	}

	/**
	 * Sets the Canvas object.
	 * 
	 * @param canvas
	 *            the Canvas object.
	 */
	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	/**
	 * Gets the Image object that represents the screen.
	 */
	public Image getScreen() {
		return screen;
	}

	/**
	 * Sets the Image screen.
	 * 
	 * @param screen
	 *            the Image object that represents the screen.
	 */
	public void setScreen(Image screen) {
		this.screen = screen;
	}

	/**
	 * Returns true if the component is running.
	 */
	synchronized public boolean isActive() {
		return active;
	}

	synchronized private void setRunning(boolean running) {
		this.active = running;
	}

	public void dispose() {

	    log(SWTRemoteDisplay.class).debug("Disposing SWT Remote Display"); //$NON-NLS-1$
		try {
			if (protoClient != null) {
				// protoClient.stopProtocol();
				protoClient.setPaintEnabled(false); // There are no UI
													// components
			}
			// paint the images for now on.
		} catch (Exception e) {
			log(SWTRemoteDisplay.class).error(
					REMOTE_DISPLAY_STOP_ERROR + e.getMessage());
		}

		if (screen != null) {
			screen.dispose();
		}
		if (canvas != null) {
			canvas.dispose();
		}

		super.dispose();

	}

	public int getScreenWidth() {
		return painter.getWidth();
	}

	public int getScreenHeight() {
		return painter.getHeight();
	}

	/**
	 * Returns the RefreshTimer.
	 */
	public Timer getRefreshTimer() {
		return refreshTimer;
	}

	public double getZoomFactor() {
		return zoomFactor;
	}

	public void setZoomFactor(double zoomFactor) {	    
		this.zoomFactor = zoomFactor;
		
		VNCProtocolData data = this.getProtocolData();
		if (data != null) {
		       Point canvasSize = new Point(0, 0);
		        canvasSize.x = (int) (this.getProtocolData().getFbWidth() * zoomFactor);
		        canvasSize.y = (int) (this.getProtocolData().getFbHeight() * zoomFactor);
		        
		        // if rotation is 90 or -90, swap width and height
		        if (Math.abs(getRotation().value()) == Rotation.ROTATION_90DEG_CLOCKWISE.value()) {
		        	int temp = canvasSize.x;
		        	canvasSize.x = canvasSize.y;
		        	canvasSize.y = temp;
		        }
		        
		        this.getCanvas().setSize(canvasSize); 
		}
	}

	public void setBackground(Color color) {
		super.setBackground(color);
		if (canvas != null) {
			canvas.setBackground(color);
		}
	}

	public VNCProtocolData getProtocolData() {

		return protoClient;
	}

	public void setPropertiesFileHandler(
			IPropertiesFileHandler propertiesFileHandler) {
		this.propertiesFileHandler = propertiesFileHandler;
	}

	public int getConnectionRetries() {
		return connectionRetries;
	}

	public void setRotation(final Rotation degrees) {
		
		rotation = degrees;
		
		this.getDisplay().asyncExec(new Runnable(){

			public void run() {
				Point p = canvas.getSize();		
				
				int w = 0, h = 0;
				
				switch (degrees) {
				
					case ROTATION_0DEG:
					case ROTATION_180DEG:
						if (painter.getImageData() != null) {
							w = painter.getImageData().width;
							h = painter.getImageData().height;
						}
						else {
							w = p.x;
							h = p.y;
						}
						break;
						
					case ROTATION_90DEG_CLOCKWISE:
					case ROTATION_90DEG_COUNTERCLOCKWISE:
						if (painter.getImageData() != null) {
							w = painter.getImageData().height;
							h = painter.getImageData().width;
						}
						else {
							w = p.y;
							h = p.x;
						}
						break;
					
				}

				canvas.setSize((int) (w * zoomFactor), (int) (h * zoomFactor));
			}
		});
	}

}
