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
package org.eclipse.sequoyah.vnc.vncviewer.config;

import java.net.URL;
import java.util.Properties;



public interface IPropertiesFileHandler {


	


	
	/**
	 * Load the Properties file given by fileAddress.
	 * 
	 *  @param fileAdress the Properties file address.
	 *  @return the Properties read from the given file.
	 */	
	public Properties loadPropertiesFile(String fileAddress);
	
	
	//
	public boolean savePropertiesFile(URL fileURL, Properties properties) throws Exception;
	
	
	
	
	
}
