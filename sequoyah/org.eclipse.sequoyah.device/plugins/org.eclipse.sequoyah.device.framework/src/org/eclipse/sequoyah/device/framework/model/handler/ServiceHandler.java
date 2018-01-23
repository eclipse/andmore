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
 * Fabio Rigo (Eldorado) - Bug [244066] - The services are being run at one of the UI threads
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [221739] - Improvements to State machine implementation
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [252261] - Internal class MobileInstance providing functionalities
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Changed job name
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.model.handler;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.status.IStatusTransition;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public abstract class ServiceHandler implements IServiceHandler
{
    private IServiceHandler parent;

    private IService service;

    public IServiceHandler getParent()
    {
        return parent;
    }

    public void run(IInstance instance) throws SequoyahException
    {
     
    	/*
    	if (!verifyStatus(instance))
        {
            throw new SequoyahException();
        }

        createJob(instance, null);*/
    	
    	run(instance, null);
    }

    public void run(IInstance instance, Map<Object, Object> arguments) throws SequoyahException
    {
        if (!verifyStatus(instance))
        {
            throw new SequoyahException();
        }

        createJob(instance, arguments);
    }

    public IStatus run(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor)
            throws SequoyahException
    {
        if (!verifyStatus(instance))
        {
            throw new SequoyahException();
        }

        String jobName = (service != null ? service.getName() : ""); //$NON-NLS-1$
        return doRun(instance, arguments, jobName, monitor);
    }

    private void createJob(final IInstance instance, final Map<Object, Object> arguments)
    {
        final String jobName = (service != null ? service.getName() + " : " + instance.getName() : ""); //$NON-NLS-1$
        final Job serviceJob = new Job(jobName)
        {
            @Override
            public IStatus run(IProgressMonitor monitor)
            {	
            	IStatus status = null;
            	try {
            		status = doRun(instance, arguments, jobName, monitor);
				} catch (SequoyahException e) {
					e.printStackTrace();
				}
				
				return status;
            }
        };

        Display.getDefault().asyncExec(new Runnable()
        {
            public void run()
            {
                PlatformUI.getWorkbench().getProgressService().showInDialog(null, serviceJob);
            }
        });
        serviceJob.schedule();
    }

    private IStatus doRun(final IInstance instance, final Map<Object, Object> arguments,
            String jobName, IProgressMonitor monitor) throws SequoyahException
    {
    	
    	return instance.getStateMachineHandler().runService(this, instance, arguments, jobName, monitor);
    	
    }

    public void setService(IService service)
    {
        this.service = service;
    };

    public IService getService()
    {
        return service;
    }

    public abstract IStatus runService(IInstance instance, Map<Object, Object> arguments,
            IProgressMonitor monitor);

    public abstract IStatus updatingService(IInstance instance, IProgressMonitor monitor);

    public abstract IServiceHandler newInstance();

    public void updatingService(IInstance instance)
    {
        // empty default implementation
    }

    public boolean verifyStatus(IInstance instance)
    {
        IStatusTransition transition = getService().getStatusTransitions(instance.getDeviceTypeId(), instance.getStatus());
        return (transition != null);
    }

    public void setParent(IServiceHandler handler)
    {
        this.parent = handler;
    }

    @Override
    public Object clone()
    {
        IServiceHandler newHandler = newInstance();
        newHandler.setParent(parent);
        newHandler.setService(service);
        return newHandler;
    }
    
    public IStatus singleInit(List<IInstance> instances)
    {
        return Status.OK_STATUS;
    }


}
