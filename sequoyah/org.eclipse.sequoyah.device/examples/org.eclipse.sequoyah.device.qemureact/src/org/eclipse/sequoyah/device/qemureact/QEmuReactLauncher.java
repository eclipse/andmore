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
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemureact;

import org.eclipse.sequoyah.device.common.utilities.IPropertyConstants;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.model.IConnection;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.service.start.launcher.DefaultConnection;

public class QEmuReactLauncher implements IDeviceLauncher {
	public static final String SLASH = "\\"; //$NON-NLS-1$
	public IConnection connection;
	public int pid;
	
	public QEmuReactLauncher(IInstance instance){
		// get instance and generates connection;
		pid=0;
		connection = new DefaultConnection();
		connection.setHost(instance.getProperties().getProperty(IPropertyConstants.HOST));
		connection.setDisplay(instance.getProperties().getProperty(IPropertyConstants.DISPLAY));
		connection.setPort(Integer.parseInt(instance.getProperties().getProperty(IPropertyConstants.PORT)));

		}
	
	public int getPID(){
		return pid;
	}
	
	public void setPID(int pid){
		this.pid=pid;
	}
	
	
	public String getFileId() {
		return PluginUtils.getPluginInstallationPath(QEmuReactPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuReactPlugin.EMULATOR_NAME).concat(SLASH).concat(QEmuReactPlugin.EMULATOR_FILE_ID);
	}
	
	public IConnection getConnection() {
		return connection;
	}

	public String getLocation() {
		return PluginUtils.getPluginInstallationPath(QEmuReactPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuReactPlugin.EMULATOR_NAME).concat(SLASH).concat(QEmuReactPlugin.EMULATOR_BIN);

	}

	public String getToolArguments() {
		return QEmuReactPlugin.EMULATOR_PARAMS+connection.getStringHost();
	}

	public String getWorkingDirectory() {
		return PluginUtils
		.getPluginInstallationPath(QEmuReactPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuReactPlugin.EMULATOR_NAME).concat(SLASH);
	}

}
