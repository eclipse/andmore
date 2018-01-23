/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Daniel Barboza Franco (Eldorado) - [329548] - Allow multiple instances selection on Device Manager View
 * Pablo Leite (Eldorado) - [329548] - Allow multiple instances selection on Device Manager View
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.model.handler;

import java.util.List;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.manager.ServiceManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;


public class ServiceHandlerAction implements Listener {
	private List <IInstance> instances;
	private String serviceId;
	private String text;
	
	public ServiceHandlerAction(List <IInstance> instances, String serviceId){
		this.instances = instances;
		this.serviceId = serviceId;
	}
	
	public ServiceHandlerAction(String text){
		serviceId = null;
		this.text = text;
	}
	
	
     public void handleEvent(Event event) {
    	 if (serviceId != null) {  
    		 try {
    			 ServiceManager.runServices(instances, serviceId);
    		 } catch (SequoyahException te){
    			 BasePlugin.logError(text+"-"+te.getMessage()); //$NON-NLS-1$
    		 }
    	 }
     }
	
}
