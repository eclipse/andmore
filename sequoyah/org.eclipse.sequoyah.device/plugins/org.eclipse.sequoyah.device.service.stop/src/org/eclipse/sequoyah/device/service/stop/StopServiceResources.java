/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.service.stop;

import org.eclipse.osgi.util.NLS;

/**
 * Resources for externalized Strings of the Sequoyah Emulator Core.
 */
public class StopServiceResources extends NLS {
	

	private static String BUNDLE_NAME = "org.eclipse.sequoyah.device.service.stop.StopServiceResources";//$NON-NLS-1$

	public static String SEQUOYAH_Stop_Service_Plugin_Name;
	public static String SEQUOYAH_Error;
	public static String SEQUOYAH_Resource_Not_Available;
	public static String SEQUOYAH_Stop_Service;
	public static String SEQUOYAH_Refresh_Service;
	public static String SEQUOYAH_Stop_Service_Update;
	public static String SEQUOYAH_Refresh_Service_Update;
	
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, StopServiceResources.class);
	}

}
