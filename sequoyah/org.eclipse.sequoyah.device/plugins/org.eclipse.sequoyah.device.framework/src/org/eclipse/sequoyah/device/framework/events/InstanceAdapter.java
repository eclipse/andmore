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

public abstract class InstanceAdapter implements IInstanceListener
{
    public void instanceCreated(InstanceEvent e) {
    	//do nothing
    }

    public void instanceDeleted(InstanceEvent e) {
    	//do nothing
    }

    public void instanceLoaded(InstanceEvent e) {
    	//do nothing
    }

    public void instanceUnloaded(InstanceEvent e) {
    	//do nothing
    }

    public void instanceUpdated(InstanceEvent e) {
    	//do nothing
    }
    
    public void instanceTransitioned(InstanceEvent e) {
    	//do nothing
    }
    
    public void instanceAboutToTransition(InstanceEvent e){  
    	//do nothing
    }
}
