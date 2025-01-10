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
 * Daniel Barboza Franco - Bug [239970] - Invisible Services
 * Fabio Rigo (Eldorado) - Bug [244066] - The services are being run at one of the UI threads
 * Mauren Brenner (Eldorado) - Bug [289577] - Replaced deprecated methods to get image
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Added parallelized param support
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.factory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionHandler;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionHandler;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionStatus;
import org.eclipse.sequoyah.device.framework.internal.model.DeviceServicesTransitions;
import org.eclipse.sequoyah.device.framework.internal.model.MobileService;
import org.eclipse.sequoyah.device.framework.model.IParallelService;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.status.IStatusTransition;
import org.eclipse.sequoyah.device.framework.status.MobileStatusTransition;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ServiceFactory {
	private static final String ELEMENT_SERVICE = "service"; //$NON-NLS-1$
	private static final String ELEMENT_STATUS = "status"; //$NON-NLS-1$
	private static final String ATR_START_ID = "startId"; //$NON-NLS-1$
	private static final String ATR_END_ID = "endId"; //$NON-NLS-1$
	private static final String ATR_HALT_ID = "haltId"; //$NON-NLS-1$
	private static final String ATR_ID = "id"; //$NON-NLS-1$
	private static final String ATR_NAME = "name"; //$NON-NLS-1$
	private static final String ATR_ICON = "icon"; //$NON-NLS-1$
	private static final String ATR_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ATR_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String ATR_COPYRIGHT = "copyright"; //$NON-NLS-1$
	private static final String ATR_VERSION = "version"; //$NON-NLS-1$
	private static final String ATR_HANDLER = "handler"; //$NON-NLS-1$
	private static final String ATR_VISIBLE = "visible"; //$NON-NLS-1$
	private static final String ATR_PARALLELIZED = "parallelized"; //$NON-NLS-1$
	private static final String ATR_INTERVAL = "interval"; //$NON-NLS-1$
	
	private static Map<String, IService> servicesMap = null;
	
	private static IService createService(IExtension originalPlugin,String serviceId,IServiceHandler handler) throws SequoyahException {
	    
	    if(servicesMap == null)
	    {
            initServicesMap();
	    }
	    
		return servicesMap.get(serviceId);
	}

    private static void initServicesMap() throws SequoyahException
    {
        servicesMap = new HashMap<String, IService>();
        loadServicesExtensions();
        loadServicesDefinitions();
    }

    private static void loadServicesDefinitions()
    {
        Collection<IExtension> defExtensions = PluginUtils
        .getInstalledExtensions(DevicePlugin.SERVICE_DEF_ID);


        for(IExtension serviceDef : defExtensions)
        {

            String serviceDefId = serviceDef.getUniqueIdentifier(); //The same as the DeviceTypeId
            String defServiceId = PluginUtils.getPluginAttribute(serviceDef, ELEMENT_SERVICE, ATR_ID);
            DeviceServicesTransitions devicesTransitions = new DeviceServicesTransitions();
            devicesTransitions.setDeviceTypeId(serviceDefId);

            List<IConfigurationElement> statusElementList = PluginUtils.getPluginElementList(serviceDef, ELEMENT_SERVICE, ELEMENT_STATUS);
            for (IConfigurationElement statusElement:statusElementList)
            {
                String startId = statusElement.getAttribute(ATR_START_ID);
                String endId    = statusElement.getAttribute(ATR_END_ID);
                String haltId   = statusElement.getAttribute(ATR_HALT_ID);

                IStatusTransition transition = new MobileStatusTransition(startId,endId,haltId);
                devicesTransitions.addTransitions(transition);
            }

            MobileService mobileService = (MobileService) servicesMap.get(defServiceId);
            if(mobileService != null)
            {
                mobileService.addDeviceTransitions(devicesTransitions);

                IServiceHandler handler = null;
                try
                {
                    handler = (IServiceHandler)PluginUtils.getExecutableAttribute(serviceDef, ELEMENT_SERVICE, ATR_HANDLER);
                }
                catch (CoreException e)
                {
                    //A empty value is valid here
                }

                if (handler!=null)
                {
                    handler.setParent(mobileService.getHandler());
                    mobileService.setHandler(handler);
                }
            }
        }
    }

    private static void loadServicesExtensions() throws SequoyahException
    {
        Collection<IExtension> installedExtensions = PluginUtils.getInstalledExtensions(DevicePlugin.SERVICE_ID);
        for(IExtension serviceExtrension : installedExtensions)
        {
            if (serviceExtrension==null) {
                throw new SequoyahException();
            }       
            
            String serviceId = PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_ID);
            IParallelService service = new MobileService(serviceId);
            service.setName(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_NAME));
            String iconName = PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_ICON);        
            ImageDescriptor image = null;
                try {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(serviceExtrension.getContributor().getName(), iconName);
            } catch (Throwable t) {
                ExceptionHandler.showException(DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED));
            }
            service.setImage(image);
            service.setDescription(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_DESCRIPTION));
            service.setProvider(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_PROVIDER));
            service.setCopyright(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE,ATR_COPYRIGHT));
            service.setVersion(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_VERSION));
            service.setVisible(new Boolean(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_VISIBLE)));
            service.setParallelized(new Boolean(PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_PARALLELIZED)));
            String interval = PluginUtils.getPluginAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_INTERVAL);
            service.setInterval(interval != null ? Integer.parseInt(interval) : 0);
            try {
                IServiceHandler originalHandler = (IServiceHandler)PluginUtils.getExecutableAttribute(serviceExtrension, ELEMENT_SERVICE, ATR_HANDLER);
                service.setHandler(originalHandler);    
            } catch (CoreException e) {
                ExceptionHandler.showException(DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED));
            }
            
            servicesMap.put(serviceId, service);
        }
    }
	
	public static IService createService(String serviceId) throws SequoyahException {
		return createService(null,serviceId,null);
	}
	
	public static IService createService(IExtension fromPlugin) throws SequoyahException {
		String id = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_SERVICE, ATR_ID);
		return createService(id);		
	}
	
}
