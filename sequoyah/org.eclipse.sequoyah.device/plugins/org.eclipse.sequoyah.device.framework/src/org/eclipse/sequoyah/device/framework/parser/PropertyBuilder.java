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
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.parser;

import java.io.File;

import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.DevicePlugin;

public class PropertyBuilder {

	public static void write(File file){
//		Element config = new Element(XMLConstants.TAG_CONFIG);
//		Property property = new Property("TEST");
//		property.setName("test name");
//		property.setDescription("test description");
//		property.setRule(XMLConstants.RULE_READ_ONLY);
//		property.setScope(XMLConstants.SCOPE_SERVICE);
//		Element prop = Property.writeIntoElement(property);
//		config.addContent(prop);
//		Document doc = new Document();		
//		doc.setRootElement(config);   
//		XMLOutputter xout = new XMLOutputter();   
//		try {
//			xout.output(doc, new FileOutputStream(file));
//		} catch (IOException e) {
//			DevicePlugin.logError(e.getMessage(),e);
//		} 
	}
	
	
	public static void read(File file){
//		SAXBuilder sb = new SAXBuilder();
//	    Document d;
//		try {
//			d = sb.build(file);   
//			Element config = d.getRootElement();   
//			List elements = config.getChildren();   
//			Iterator i = elements.iterator();   
//			while (i.hasNext()) {   
//				Element element = (Element) i.next();
//				Property property = Property.readFromElement(element);  
//				DevicePlugin.logInfo(property.toString());
//			} 
//		}
//	    catch (JDOMException e) {
//	    	DevicePlugin.logError(e.getMessage(),e);
//		} catch (IOException e) {
//			DevicePlugin.logError(e.getMessage(),e);
//		}
	}
	
	public static void read(){
		String path = PluginUtils.getPluginInstallationPath(DevicePlugin.getDefault()).getAbsolutePath();
		File file = new File(path,"/resource/config.xml"); //$NON-NLS-1$
		read(file);
	}
	
	public static void write(){
		String path = PluginUtils.getPluginInstallationPath(DevicePlugin.getDefault()).getAbsolutePath();
		File file = new File(path,"/resource/config_out.xml"); //$NON-NLS-1$
		write(file);
	}
	
}
