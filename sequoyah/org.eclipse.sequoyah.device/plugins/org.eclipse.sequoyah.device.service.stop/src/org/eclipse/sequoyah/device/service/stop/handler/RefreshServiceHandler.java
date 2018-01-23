/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Rigo (Eldorado) - Bug [244066] - The services are being run at one of the UI threads
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.service.stop.handler;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.device.service.stop.StopServiceResources;

public class RefreshServiceHandler extends ServiceHandler  {

	public IStatus runService(IInstance instance, Map<Object , Object> arguments,  IProgressMonitor monitor) {
		BasePlugin.logInfo(StopServiceResources.SEQUOYAH_Refresh_Service+"->"+instance.getName()); //$NON-NLS-1$
		return Status.OK_STATUS;
	}
	
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		BasePlugin.logInfo(StopServiceResources.SEQUOYAH_Refresh_Service_Update+"->"+instance.getName()); //$NON-NLS-1$
		return Status.OK_STATUS;
	}
	
	@Override
	public IServiceHandler newInstance() {		
		return new RefreshServiceHandler();
	}
}
