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

package org.eclipse.sequoyah.device.common.utilities;

import org.eclipse.osgi.util.NLS;

/**
 * Resources for externalized Strings of the Sequoyah Emulator Core.
 */
public class UtilitiesResources extends NLS {
	

	private static String BUNDLE_NAME = "org.eclipse.sequoyah.device.common.utilities.UtilitiesResources";//$NON-NLS-1$

	public static String Sequoyah_Utilities_Plugin_Name;
	public static String Sequoyah_Error;
	public static String Sequoyah_Resource_Not_Available;
	
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, UtilitiesResources.class);
	}

}
