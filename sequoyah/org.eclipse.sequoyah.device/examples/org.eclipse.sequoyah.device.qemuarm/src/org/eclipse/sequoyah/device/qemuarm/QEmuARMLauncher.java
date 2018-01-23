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
 * Fabio Fantato (Motorola) - bug#221733 - Package was changed to make able to any
 * 							  other plugin access these constants values
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Daniel Barboza Franco (Eldorado Research Institute) - [221740] - Sample implementation for Linux host
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemuarm;

import org.eclipse.core.runtime.Platform;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.model.IConnection;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.qemu.wizard.custom.IPropertyConstants;
import org.eclipse.sequoyah.device.service.start.launcher.DefaultConnection;

public class QEmuARMLauncher implements IDeviceLauncher {
	public static final String SLASH = "/"; //$NON-NLS-1$
	public IConnection connection;
	private int pid;
	
	public QEmuARMLauncher(IInstance instance){
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
		return PluginUtils.getPluginInstallationPath(QEmuARMPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuARMPlugin.EMULATOR_NAME).concat(SLASH).concat(QEmuARMPlugin.EMULATOR_FILE_ID);
	}
	
	public IConnection getConnection() {
		return connection;
	}

	public String getLocation() {
		
		String executable = null;
		
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	executable = QEmuARMPlugin.EMULATOR_WIN32_BIN;	
        }
        else {
        	executable = QEmuARMPlugin.EMULATOR_LINUX_BIN;
        }
		
		return PluginUtils.getPluginInstallationPath(QEmuARMPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuARMPlugin.EMULATOR_NAME).concat(SLASH).concat(executable);

	}

	public String getToolArguments() {
		return QEmuARMPlugin.EMULATOR_PARAMS+connection.getStringHost();
	}

	public String getWorkingDirectory() {
		return PluginUtils
		.getPluginInstallationPath(QEmuARMPlugin.getDefault())
		.getAbsolutePath().concat(SLASH).concat(QEmuARMPlugin.EMULATOR_NAME).concat(SLASH);
	}

}
