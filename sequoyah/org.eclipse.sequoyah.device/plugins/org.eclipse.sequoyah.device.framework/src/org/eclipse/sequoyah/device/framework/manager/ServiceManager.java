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
 * Pablo Leite (Eldorado) - [329548] Added getCommonService method
 * Daniel Barboza Franco (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding method to return all services to help on addition of tooltip support for double click behavior 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.ServiceFactory;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IParallelService;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;

public class ServiceManager {
	private static ServiceManager _instance;
	private List<IService> services;
	
	private ServiceManager(){
		services = new ArrayList<IService>();
	}
	
	public static ServiceManager getInstance() {
		if (_instance==null) {
			_instance = new ServiceManager();
		}
		return _instance;
	}
		
	public void loadServices(){
		services.clear();
		Collection<String> servicesIds = PluginUtils.getInstalledPlugins(DevicePlugin.SERVICE_ID);
		for (String serviceId:servicesIds){
			try {
				services.add(ServiceFactory.createService(serviceId));
			} catch (SequoyahException t){
				// ignore missing services
				// TODO log
			}
		}
	}
	
	public void listServices(){
		for(IService service:services){
			BasePlugin.logInfo(service.toString());
		}
	}
	
	/**
	 * Retrieve all services that are available on every instance on the given list.
	 * Services not available on any service are filtered out.
	 * @param instances
	 * @return 
	 */
	public static List<IService> getCommonServices(List<IInstance> instances, boolean filterParallelized)
	{
		List<IService> commonServices = getServices(instances,
				filterParallelized, false);
		
		return (commonServices ==  null ? new ArrayList<IService>(0) : commonServices);
		
	}
	
	/**
	 * Retrieve all services that are available on every instance on the given list.
	 * @param instances
	 * @return 
	 */
	public static List<IService> getAllServices(List<IInstance> instances, boolean filterParallelized)
	{
		List<IService> commonServices = getServices(instances,
				filterParallelized, true);
		
		return (commonServices ==  null ? new ArrayList<IService>(0) : commonServices);
		
	}

	private static List<IService> getServices(List<IInstance> instances,
			boolean filterParallelized, boolean all) {
		List<IService> commonServices = null;
		
		Map<IInstance, List<IService>> availableServicesMap = new HashMap<IInstance, List<IService>>(instances.size());		
		DeviceTypeRegistry deviceTypeRegistry = DeviceTypeRegistry.getInstance();
		List<IService> allAvailableServices = new ArrayList<IService>(deviceTypeRegistry.getDeviceTypes().size() * 5);
		
		for(IInstance instance : instances)
		{
			List<IService> availableServices = getAvailableServices(instance, filterParallelized);
			availableServicesMap.put(instance, availableServices);
			for(IService service : availableServices)
			{
			    if(!allAvailableServices.contains(service))
			    {
			        allAvailableServices.add(service);
			    }
			}
		}
		
		commonServices = all ? allAvailableServices : filterCommonServices(availableServicesMap,
				allAvailableServices);
		
		return commonServices;
	}

	/*
	 * Return all services available for a given Instance.
	 */
	private static List<IService> getAvailableServices(IInstance instance, boolean filterParallelized) {
		String deviceTypeId = instance.getDeviceTypeId();
		IDeviceType deviceType = DeviceTypeRegistry.getInstance().getDeviceTypeById(deviceTypeId);
		List<IService> instanceServices = deviceType.getServices();
		
		List<IService> availableServices = new ArrayList<IService>(instanceServices.size());
		for(IService instanceService : instanceServices)
		{
			boolean isAvailable = isServiceAvailable(instance, instanceService); 
			if(isAvailable)
			{
				if(filterParallelized) {
					if(instanceService instanceof IParallelService)
					{
						IParallelService service2 = (IParallelService) instanceService;
						isAvailable = service2.isParallelized();
					}
				}
				
				if(isAvailable)
				{
					availableServices.add(instanceService);
				}
			}
		}
		return availableServices;
	}
	
	/**
	 * Returns the complete list of services installed.
	 * 
	 * @return The list of services
	 */
	public List<IService> getInstalledServices(){
		return new ArrayList<IService>(services);
	}

	/*
	 * Retrieve the intersection of services available on all instances on the given map.
	 */
	private static List<IService> filterCommonServices(
			Map<IInstance, List<IService>> availableServicesMap,
			List<IService> allAvailableServices) {
		List<IService> commonServices;
		commonServices = new ArrayList<IService>(allAvailableServices.size());
		for(IService service : allAvailableServices)
		{
			boolean validService = true;
			Iterator<IInstance> it = availableServicesMap.keySet().iterator();
			while(it.hasNext() && validService)
			{
				IInstance instance = it.next();
				List<IService> instanceServices = availableServicesMap.get(instance);
				if(!instanceServices.contains(service))
				{
					validService = false;
				}
			}
			
			if(validService && !commonServices.contains(service))
			{
				commonServices.add(service);
			}
		}
		return commonServices;
	}
	
	public static boolean isServiceAvailable(IInstance instance, IService service)
	{
		boolean isServiceAvailable = false;
			String deviceTypeId = instance.getDeviceTypeId();
			IDeviceType deviceType = DeviceTypeRegistry.getInstance().getDeviceTypeById(deviceTypeId);
			List<IService> instanceServices = deviceType.getServices();
			isServiceAvailable = instanceServices.contains(service);
			if(isServiceAvailable)
			{
				boolean inTransition = instance.getStateMachineHandler().isTransitioning();
				isServiceAvailable = (service.getStatusTransitions(instance.getDeviceTypeId(), instance.getStatus()) != null);
				isServiceAvailable = isServiceAvailable && !inTransition;
			}
			
    	return isServiceAvailable;
	}
	
	public static void runServices (final List<IInstance> instances, final String serviceId) throws SequoyahException {
		if(instances != null)
		{
			Thread t = new Thread(new Runnable()
			{

				public void run()
				{
					int interval = 0;
					List<IService> allServices = getAllServices(instances, false);
					boolean found = false;
                    Iterator<IService> it = allServices.iterator();
                    IService service = null;
                    
                    while(!found && it.hasNext())
                    {
                        service = it.next();
                        found = service.getId().equals(serviceId);
                    }

                    if (found)
                    { 
                        if(service instanceof IParallelService)
                        {
                            IParallelService parallelService = (IParallelService) service;
                            interval = parallelService.getInterval();
                        }
                        IServiceHandler hnd = service.getHandler();
                        hnd.singleInit(instances);


                        for (IInstance instance : instances) {
                            try
                            {
                                hnd.run(instance); 
                                if(interval > 0)
                                {
                                    Thread.sleep(interval);
                                }
                            }
                            catch (InterruptedException e)
                            {
                                BasePlugin.logError(e.getMessage());
                            }
                            catch (SequoyahException e)
                            {
                                BasePlugin.logError(e.getMessage());
                            }
                        }
                    }
				}
			});
			t.start();
		}
	}
	
}
