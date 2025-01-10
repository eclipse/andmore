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
 * Fabio Rigo (Eldorado) - [284998] Modify addInstanceListener() method to add a listener before thread is started.
 * Fabio Rigo (Eldorado) - [287995] - Provide an instance is about to transition event
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.events;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;

public class InstanceEventManager extends EventManager
{   
    private Collection<InstanceEvent> eventsToFire = Collections.synchronizedSet(new LinkedHashSet<InstanceEvent>());
          
    private Runnable eventNotifierLoop = new Runnable() {
        @SuppressWarnings("incomplete-switch")
		public void run() {
            while (InstanceEventManager.this.isListenerAttached()) {
                if (eventsToFire != null) {
                    if (eventsToFire.isEmpty()) {                    
                        synchronized(eventsToFire)
                        {
                            try {
                                eventsToFire.wait();
                            } catch (InterruptedException e) {
                            	//do nothing
                            }
                        }
                    }
                    
                    if (!eventsToFire.isEmpty()){
                    	InstanceEvent[] array = eventsToFire.toArray(new InstanceEvent[eventsToFire.size()]);
                    	InstanceEvent e = array[0];
                        switch(e.getType()) {
                            case INSTANCE_CREATED:
                                fireInstanceCreated(e);
                                break;
                            case INSTANCE_DELETED:
                                fireInstanceDeleted(e);
                                break;
                            case INSTANCE_LOADED:
                                fireInstanceLoaded(e);
                                break;
                            case INSTANCE_UNLOADED:
                                fireInstanceUnloaded(e);
                                break;
                            case INSTANCE_UPDATED:
                                fireInstanceUpdated(e);
                                break;
                            case INSTANCE_TRANSITIONED:
                                fireInstanceTransitioned(e);
                                break;
                        }
                        eventsToFire.remove(e);
                    }                    
                }
            }
        }
    };
   
    private static InstanceEventManager _instance;
    
    private InstanceEventManager()
    {
    }
    
    public static InstanceEventManager getInstance()
    {
        if (_instance == null)
        {
            _instance = new InstanceEventManager();
        }
        
        return _instance;
    }
    
    public void addInstanceListener(IInstanceListener listener)
    {
    	boolean startThread = false;
    	if (!isListenerAttached()) {
    		startThread = true;       
        }
        
        addListenerObject(listener);
        if (startThread)
            new Thread(eventNotifierLoop, "Instance Event Manager").start();////$NON-NLS-1$    
    }

    public void removeInstanceListener(IInstanceListener listener)
    {
        removeListenerObject(listener);
        synchronized(eventsToFire) {
            eventsToFire.notify();            
        }
    }

    public void notifyListeners(InstanceEvent event) {  	
        if (event.getType() == InstanceEventType.INSTANCE_ABOUT_TO_TRANSITION) {
        	fireInstanceAboutToTransition(event);
        }
        else {
            eventsToFire.add(event);
            synchronized(eventsToFire) {
                eventsToFire.notify();            
            }
        }
    }
    
    protected void fireInstanceCreated(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceCreated(event);
                }
            });
        }  
    }   
    
    protected void fireInstanceDeleted(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceDeleted(event);
                }
            });
        }  
    }   
    
    protected void fireInstanceLoaded(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceLoaded(event);
                }
            });
        }  
    }   
    
    protected void fireInstanceUnloaded(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceUnloaded(event);
                }
            });
        }  
    }   
    
    protected void fireInstanceUpdated(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceUpdated(event);
                }
            });
        }  
    }  
    
    protected void fireInstanceTransitioned(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceTransitioned(event);
                }
            });
        }  
    }  
    
    protected void fireInstanceAboutToTransition(final InstanceEvent event)
    {
        Object list[] = getListeners();
        for (int i = 0; i < list.length; i++) {
            final IInstanceListener l = (IInstanceListener) list[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.instanceAboutToTransition(event);
                }
            });
        }
    }  
}
