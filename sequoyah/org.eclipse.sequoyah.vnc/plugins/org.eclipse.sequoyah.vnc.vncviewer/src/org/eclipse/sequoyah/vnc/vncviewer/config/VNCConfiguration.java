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

import java.util.Properties;



public class VNCConfiguration {
	

	
	private Properties configurationProperties;
	private IPropertiesFileHandler propertiesFileHandler;

	/*
	public VNCConfiguration(String filename) {
	
		//String file = "C:/temp/vnc_viewer.conf";
		configurationProperties = loadPropertiesFile(filename);
	}
	*/
	
	/*
	public Properties getDefaultConfigurationProperties()
	{
		Properties properties = new Properties(); 
		Bundle pluginBundle = VNCViewerPlugin.getDefault().getBundle();
		URL vncViewerConf = pluginBundle.getResource("resources/vnc_viewer.conf");
		
		try {
			InputStream vncViewerConfStream = vncViewerConf.openStream();
			properties.load(vncViewerConfStream);
		} catch (IOException e) {
			// TODO handle properly
			e.printStackTrace();
		}
		
		return properties;
	}
	*/

	
	public VNCConfiguration(String filename, IPropertiesFileHandler propertiesFileHandler) {
		
		//String file = "C:/temp/vnc_viewer.conf";
		//configurationProperties = loadPropertiesFile(filename);
		
		this.propertiesFileHandler = propertiesFileHandler;
		configurationProperties = propertiesFileHandler.loadPropertiesFile(filename);
	}

	
	
	public Properties getConfigurationProperties() {
		
		return configurationProperties;
	}
	
	
	
	/*
	public Properties getConfigurationProperties(String filename){
		
		
		return loadPropertiesFile(filename);
	}
	
	
	private static Properties loadPropertiesFile(String filename) {

		Properties properties = new Properties(); 
		File propFile = new File (filename);
		
		try {
			properties.load( new FileInputStream(propFile));
			
		} catch (IOException e) {
			//TODO handle properly
			e.printStackTrace();
		}

		return properties;
		
	}
	*/
	
	private static void checkProperties(){
		
	}



/*
	public void addVncProperty(String key, String value) {
		this.vncProperties.put(key, value);
	}




	public String getVncProperty(String key) {
		return this.vncProperties.getProperty(key);
	}
	*/
	

}


