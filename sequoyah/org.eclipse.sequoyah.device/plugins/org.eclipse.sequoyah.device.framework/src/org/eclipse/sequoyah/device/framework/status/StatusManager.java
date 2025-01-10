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
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding logic to store the tooltip on double click support for dev mgt view
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.DeviceResources;
import org.eclipse.sequoyah.device.framework.manager.ServiceManager;
import org.eclipse.sequoyah.device.framework.model.IService;

public class StatusManager {
	private static StatusManager _instance;

	private StatusManager(){
		loadStatus();
	}
	
	public static StatusManager getInstance() {
		if (_instance==null) {
			_instance = new StatusManager();
		}
		return _instance;
	}
		
	public void loadStatus(){
		Collection<String> statusIds = PluginUtils.getInstalledPlugins(DevicePlugin.STATUS_ID);
		StatusRegistry.getInstance().clear();
		
		// prepare list of services for populating tooltips (as a map for faster access later)
		ServiceManager serviceManager = ServiceManager.getInstance();
		serviceManager.loadServices();
		List<IService> services = serviceManager.getInstalledServices();
		Map<String, String> serviceNamesMap = new HashMap<String, String>();
		for (IService service:services){
			if (service != null){
				serviceNamesMap.put(service.getId(), service.getName());
			}
		}
		
		StatusRegistry statusRegistry = StatusRegistry.getInstance();		
		for (String statusId:statusIds){
			// add status to mapping
			IStatus status = StatusFactory.createStatus(statusId);
			statusRegistry.addStatus(status);
			
			// add tooltip for status in the device management view
			String serviceName = serviceNamesMap.get(status.getDefaultServiceId());
			String tooltipText = null;
			if (serviceName != null) {
				tooltipText = DeviceResources.bind(DeviceResources.StatusManager_TooltipTextMessage, serviceName);
			}
			statusRegistry.addStatusTooltip(status.getId(), tooltipText);
		}
	};
	
	public void listStatus(){
		for (IStatus status:StatusRegistry.getInstance().getStatus()) {
			BasePlugin.logInfo(status.toString());
		}
	}
	
}
