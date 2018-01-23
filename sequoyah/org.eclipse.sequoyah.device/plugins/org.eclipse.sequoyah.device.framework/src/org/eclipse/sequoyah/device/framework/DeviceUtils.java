package org.eclipse.sequoyah.device.framework;

/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and others
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Guimaraes (Eldorado)
 * 
 * Contributors:
 * Fabio Fantato (Eldorado) - [244069] -  Need some APIs that could be common use of the framework
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

import java.util.Collection;
import java.util.List;

import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;

/**
 * Utilities class that contains some functionalities that are not yet provided
 * by the Sequoyah device framework.
 * As soon as the methods/functionalities of this class are made available 
 * on Sequoyah they should be removed from this class.
 */
public abstract class DeviceUtils
{

    /**
     * Names of all IDevice elements in an array, so it can be used on SWT widgets.
     * @return an string array with the names all IDevice available on Sequoyah
     */
    public static String[] getAllDeviceNames()
    {
        DeviceTypeRegistry deviceTypeRegistry = DeviceTypeRegistry.getInstance();
        Collection<IDeviceType> deviceTypeList = deviceTypeRegistry.getDeviceTypes();
        String[] deviceNames = new String[deviceTypeList.size()];

        int i = 0;
        for (IDeviceType device : deviceTypeList)
        {
            deviceNames[i++] = device.getBundleName();
        }
        return deviceNames;
    }

    /**
     * Names of all IInstance elements in an array, so it can be used on SWT widgets.
     * @return an string array with the names all IInstance available on Sequoyah
     */
    public static String[] getAllInstanceNames()
    {
        InstanceRegistry instanceRegistry = InstanceRegistry.getInstance();
        Collection<IInstance> instanceList = instanceRegistry.getInstances();
        String[] instanceNames = new String[instanceList.size()];

        int i = 0;
        for (IInstance instance : instanceList)
        {
            instanceNames[i++] = instance.getName();
        }
        return instanceNames;
    }

    /**
     * 
     * @param instanceName
     * @return
     */
    public static IInstance getInstance(String instanceName)
    {
        IInstance deviceInstanceToReturn = null;

        List<IInstance> deviceInstances =
                InstanceRegistry.getInstance().getInstancesByName(instanceName);
        if (!deviceInstances.isEmpty())
        {
            deviceInstanceToReturn = deviceInstances.get(0);
        }
        return deviceInstanceToReturn;
    }

    /**
     * 
     * @return
     */
    public static IService getServiceById(IDeviceType deviceType, String serviceId)
    {
        IService serviceToReturn = null;
        if ((deviceType != null) && (serviceId != null))
        {
            for (IService aService : deviceType.getServices())
            {
                if (serviceId.equals(aService.getId()))
                {
                    serviceToReturn = aService;
                    break;
                }
            }
        }

        return serviceToReturn;
    }

	public static IDeviceType getDeviceType(IInstance instance) {
		return getDeviceTypeById(instance.getDeviceTypeId());
	}
	
	public static IDeviceType getDeviceTypeById(String deviceTypeId) {
		return DeviceTypeRegistry.getInstance().getDeviceTypeById(deviceTypeId);
	}
	
}
