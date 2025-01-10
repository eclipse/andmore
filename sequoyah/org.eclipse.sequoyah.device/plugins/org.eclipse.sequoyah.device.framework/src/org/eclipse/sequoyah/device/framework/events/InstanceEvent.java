/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo (Eldorado) - [244951] Implement listener/event mechanism at device framework
 *
 * Contributors:
 * Fabio Rigo (Eldorado) - [287995] Provide an instance is about to transition event
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.events;

import org.eclipse.sequoyah.device.framework.model.IInstance;

public class InstanceEvent
{
    public enum InstanceEventType {
        INSTANCE_CREATED, INSTANCE_DELETED, INSTANCE_LOADED, INSTANCE_UNLOADED, INSTANCE_UPDATED, INSTANCE_TRANSITIONED, INSTANCE_ABOUT_TO_TRANSITION
    }
    
    private IInstance instance;
    private InstanceEventType type;
    private String transitionId;
    
    public InstanceEvent(InstanceEventType type, IInstance instance)
    {
        this.type = type;
        this.instance = instance;
    }
    
    public InstanceEvent(InstanceEventType type, IInstance instance, String transitionId)
    {
    	this(type, instance);
        this.transitionId = transitionId;
    }
    
    public IInstance getInstance()
    {
        return instance;
    }
    
    public InstanceEventType getType() {
        return type;
    }
    
    public String getTransitionId()
    {
    	return transitionId;
    }
}