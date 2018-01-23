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

package org.eclipse.sequoyah.device.qemureact.handler;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.device.qemureact.QEmuReactResources;

public class QEmuReactStartHandler extends ServiceHandler {


	public void runService(IInstance instance) {
		BasePlugin.logInfo(QEmuReactResources.SEQUOYAH_Start_Service+"->"+instance.getName()); //$NON-NLS-1$
	}

	public void updatingService(IInstance instance) {
		// no action
	}

	@Override
	public IServiceHandler newInstance() {		
		return new QEmuReactStartHandler();
	}

	@Override
	public IStatus runService(IInstance instance,
			Map<Object, Object> arguments, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}
}
