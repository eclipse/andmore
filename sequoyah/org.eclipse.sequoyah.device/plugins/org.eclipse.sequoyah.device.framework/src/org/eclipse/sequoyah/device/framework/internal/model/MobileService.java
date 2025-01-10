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
 * Yu-Fen Kuo (MontaVista) - Bug [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Added IService2 support
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IParallelService;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.status.IStatusTransition;

public class MobileService implements IParallelService {
	private String id;
	private String name;
	private ImageDescriptor image;
	private String copyright;
	private String description;
	private String provider;
	private String version;
	private IServiceHandler handler;
	private List<DeviceServicesTransitions> devicesTransitions;
	private boolean visible;
	private boolean parallelized;
    private int interval;
	
	public MobileService(String id){
		this.id = id;
		this.devicesTransitions = new ArrayList<DeviceServicesTransitions>();
	}
	
	public String getId() {
		return id;
	}
	public ImageDescriptor getImage() {
		return image;
	}
	public String getName() {
		return name;
	}
	public String getCopyright() {
		return copyright;
	}
	public String getDescription() {
		return description;
	}
	public String getProvider() {
		return provider;
	}
	public String getVersion() {
		return version;
	}
	public IServiceHandler getHandler() {
		return handler;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setImage(ImageDescriptor image) {
		this.image = image;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public void setHandler(IServiceHandler handler) {
		this.handler = (IServiceHandler)handler.clone();
		this.handler.setService(this);
	}

	public Object clone(){
		IParallelService newService = new MobileService(id);
		newService.setName(this.name);
		newService.setImage(this.image);
		newService.setCopyright(this.copyright);
		newService.setDescription(this.description);
		newService.setProvider(this.provider);
		newService.setVersion(this.version);
		newService.setHandler(this.handler);
		newService.setVisible(this.visible);
		newService.setParallelized(this.parallelized);
		newService.setInterval(this.interval);
		return newService;
	}
	
	
	
	public String toString(){
		return "[Service: " + //$NON-NLS-1$
				"id=" + (id==null?"":id) + //$NON-NLS-1$ //$NON-NLS-2$
				",name=" + (name==null?"":name) + //$NON-NLS-1$ //$NON-NLS-2$
				",description=" + (description==null?"":description) +  //$NON-NLS-1$ //$NON-NLS-2$
				",version=" + (version==null?"":version) + //$NON-NLS-1$ //$NON-NLS-2$
				",provider=" + (provider==null?"":provider) + //$NON-NLS-1$ //$NON-NLS-2$
				",copyright=" + (copyright==null?"":copyright) + //$NON-NLS-1$ //$NON-NLS-2$
				",handler=" + (handler==null?"":handler.getClass().getName()) + //$NON-NLS-1$ //$NON-NLS-2$
				",parallelized=" + (parallelized) +
				",interval=" + (interval) +
				"]"; //$NON-NLS-1$
	}


	public boolean isVisible() {
		return this.visible;
	}

	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isParallelized() {
		return this.parallelized;
	}

	public void setParallelized(boolean parallelized) {
		this.parallelized = parallelized;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean isEqual = false;
		if(obj instanceof IService)
		{
			IService otherService = (IService) obj;
			isEqual = otherService.getId().equals(this.getId());
		}
		return isEqual;
	}
	
    public void setInterval(int interval)
    {
        this.interval = interval;
    }
    
    public int getInterval()
    {
        return interval;
    }
    
    /**
     * @return a specific device transtion
     */
    public Collection<IStatusTransition> getStatusTransitions(IDeviceType deviceType)
    {
        Map<String, IStatusTransition> transitionsMap = getTransitionsMap(deviceType);
        if(transitionsMap == null)
        {
            transitionsMap = Collections.emptyMap();
        }
        
        return transitionsMap.values();
    }
    
    
    public IStatusTransition getStatusTransitions(IDeviceType deviceType, String startId)
    {
        Map<String, IStatusTransition> transitionsMap = getTransitionsMap(deviceType);
        return transitionsMap != null ? transitionsMap.get(startId) : null;
    }

    private Map<String, IStatusTransition> getTransitionsMap(IDeviceType deviceType)
    {
        Map<String, IStatusTransition> transitionsMap = null;
        
        DeviceServicesTransitions temp = new DeviceServicesTransitions();
        temp.setDeviceTypeId(deviceType.getId());
        
        int deviceTypeIndex = devicesTransitions.indexOf(temp);
        if(deviceTypeIndex >= 0)
        {
            transitionsMap = devicesTransitions.get(deviceTypeIndex).getTransitions();
        }
        
        return transitionsMap;
    }
    
    public Collection<IStatusTransition> getStatusTransitions(String deviceTypeId)
    {
        MobileDeviceType deviceType = new MobileDeviceType(deviceTypeId, "");
        
        return getStatusTransitions(deviceType);
    }

    public IStatusTransition getStatusTransitions(String deviceTypeId, String startId)
    {
        MobileDeviceType deviceType = new MobileDeviceType(deviceTypeId, "");
        
        return getStatusTransitions(deviceType, startId);
    }

    
    /**
     * @return the devicesTransitions
     */
    public List<DeviceServicesTransitions> getDevicesTransitions()
    {
        return devicesTransitions;
    }

    /**
     * @param devicesTransitions the devicesTransitions to set
     */
    public void setDevicesTransitions(List<DeviceServicesTransitions> devicesTransitions)
    {
        this.devicesTransitions = devicesTransitions;
    }

    public void addDeviceTransitions(DeviceServicesTransitions devicesTransitions)
    {
        if(this.devicesTransitions == null)
        {
            this.devicesTransitions = new ArrayList<DeviceServicesTransitions>();
		}
		this.devicesTransitions.add(devicesTransitions);

    }
}
