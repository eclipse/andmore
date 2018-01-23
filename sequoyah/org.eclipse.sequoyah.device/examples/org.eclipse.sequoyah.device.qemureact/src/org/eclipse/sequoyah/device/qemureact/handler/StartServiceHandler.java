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
 * Fabio Fantato (Eldorado) - Bug [244539] - The plug-in "org.eclipse.sequoyah.service.start" depends on jdt
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/

package org.eclipse.sequoyah.device.qemureact.handler;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.device.qemureact.QEmuReactLauncher;
import org.eclipse.sequoyah.device.service.start.StartServiceResources;
import org.eclipse.sequoyah.device.service.start.launcher.DeviceLauncherManager;

public class StartServiceHandler extends ServiceHandler {

	public IStatus runService(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		BasePlugin.logInfo(StartServiceResources.SEQUOYAH_Start_Service+" Over ->"+instance.getName()); //$NON-NLS-1$
		try {
			IDeviceLauncher launcher = new QEmuReactLauncher(instance);
			ILaunch launch = DeviceLauncherManager.launch(launcher,instance.getName());
			instance.setPID(launcher.getPID());
		} catch (Throwable t) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		BasePlugin.logInfo(StartServiceResources.SEQUOYAH_Start_Service_Update+"->"+instance.getName()); //$NON-NLS-1$
		return Status.OK_STATUS;
	}
	
	@Override
	public IServiceHandler newInstance() {		
		return new StartServiceHandler();
	}
}
