/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Pablo Leite (Eldorado
 * 
 * Contributors:
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.internal.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.sequoyah.device.framework.status.IStatusTransition;

public class DeviceServicesTransitions
{
    
    private String deviceTypeId;
    private Map<String, IStatusTransition> transitions;
    
    /**
     * @return the deviceTypeId
     */
    public String getDeviceTypeId()
    {
        return deviceTypeId;
    }
    /**
     * @param deviceTypeId the deviceTypeId to set
     */
    public void setDeviceTypeId(String deviceTypeId)
    {
        this.deviceTypeId = deviceTypeId;
    }
    
    /**
     * @return the transitions
     */
    public Map<String, IStatusTransition> getTransitions()
    {
        return transitions;
    }
    
    /**
     * @param transitions the transitions to set
     */
    public void setTransitions(Map<String, IStatusTransition> transitions)
    {
        this.transitions = transitions;
    }
    
    /**
     * @param transitions the transitions to set
     */
    public void addTransitions(IStatusTransition statusTransition)
    {
        if(this.transitions == null)
        {
            this.transitions = new HashMap<String, IStatusTransition>();
        }
        this.transitions.put(statusTransition.getStartId(), statusTransition);
    }
    
    
    
    @Override
    public boolean equals(Object obj)
    {
        boolean isEqual = false;
        if(obj instanceof DeviceServicesTransitions)
        {
            DeviceServicesTransitions other = (DeviceServicesTransitions) obj;
            isEqual = other.getDeviceTypeId().equals(this.getDeviceTypeId());
        }
        
        return isEqual;
    }

}
