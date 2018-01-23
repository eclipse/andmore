/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.service.start.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.sequoyah.device.service.start.StartServicePlugin;



/**
 * This delegate controls the Emulator Instance Launch Configuration. This class
 * has no initial implementation because the launch use the same behavior of
 * {@link ProgramLaunchDelegate}.
 * 
 * @author Fabio Fantato
 * 
 */

public class DeviceInstanceLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	public static final String ATTR_LOCATION = StartServicePlugin.PLUGIN_ID + ".ATTR_LOCATION"; //$NON-NLS-1$
	public static final String ATTR_TOOL_ARGUMENTS = StartServicePlugin.PLUGIN_ID + ".ATTR_TOOL_ARGUMENTS"; //$NON-NLS-1$
	public static final String ATTR_WORKING_DIRECTORY = StartServicePlugin.PLUGIN_ID + ".ATTR_WORKING_DIRECTORY"; //$NON-NLS-1$
	
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		String location = configuration.getAttribute(ATTR_LOCATION, ""); //$NON-NLS-1$
		List<String> args = new ArrayList<String>();
		args.add(location);
		String toolArgs = configuration.getAttribute(ATTR_TOOL_ARGUMENTS, ""); //$NON-NLS-1$
		if (toolArgs.trim().length()>0) {
			String[] splitedArgs = toolArgs.trim().split(" "); //$NON-NLS-1$
			for (String arg : splitedArgs) {
				args.add(arg);
			}
		}
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File(configuration.getAttribute(ATTR_WORKING_DIRECTORY, ""))); //$NON-NLS-1$
		try {
			Process p = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}