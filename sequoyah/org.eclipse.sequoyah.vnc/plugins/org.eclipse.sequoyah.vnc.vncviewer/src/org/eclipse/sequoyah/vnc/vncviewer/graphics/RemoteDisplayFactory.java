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

package org.eclipse.sequoyah.vnc.vncviewer.graphics;

import java.util.Properties;

import org.eclipse.sequoyah.vnc.vncviewer.config.EclipsePropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.config.VNCConfiguration;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.img.SWTRemoteDisplayImg;
import org.eclipse.swt.widgets.Composite;


public class RemoteDisplayFactory {

	
	private static String DEFAULT_PROPERTIES_FILE = "resources/vnc_viewer.conf"; //$NON-NLS-1$
	
	/**
	 * This factory returns an IRemoteDisplay instance.
	 * 
	 * @param display the String that represents the desired instance. 
	 * @param parent the Composite to be used as the GUI components parent.
	 * @return the instantiated IRemoteDisplay
	 */
	public static IRemoteDisplay getDisplay(String display, Composite parent){
	
		
		IPropertiesFileHandler propertiesFileHandler = new EclipsePropertiesFileHandler();
		VNCConfiguration configurator = new VNCConfiguration(DEFAULT_PROPERTIES_FILE, propertiesFileHandler);
		
		Properties config = configurator.getConfigurationProperties();
	
		if (display.equals("SWTDisplay")){ //$NON-NLS-1$
//			return new SWTRemoteDisplayImgData(parent, config, propertiesFileHandler);
			return new SWTRemoteDisplayImg(parent, config, propertiesFileHandler);
		}
	
		return null;
	}
	
	
}
