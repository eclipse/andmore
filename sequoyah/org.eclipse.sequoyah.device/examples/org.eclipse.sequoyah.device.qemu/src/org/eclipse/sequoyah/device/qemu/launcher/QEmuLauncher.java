/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Yu-Fen Kuo (MontaVista)  - bug#236476 - Provide a generic device type
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemu.launcher;

import org.eclipse.core.runtime.IPath;
import org.eclipse.sequoyah.device.framework.model.IConnection;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.qemu.QEmuPlugin;
import org.eclipse.sequoyah.device.qemu.wizard.custom.IPropertyConstants;
import org.eclipse.sequoyah.device.service.start.launcher.DefaultConnection;

public class QEmuLauncher implements IDeviceLauncher {

	public static final String EMULATOR_PID_FILE_NAME = "qemu.pid"; //$NON-NLS-1$
	public static final String SLASH = "\\"; //$NON-NLS-1$
	public IConnection connection;
	public int pid;
	private IInstance instance;

	public QEmuLauncher(IInstance instance) {
		// get instance and generates connection;
		pid = 0;
		this.instance = instance;
		connection = new DefaultConnection();
		connection.setHost(instance.getProperties().getProperty(
				IPropertyConstants.HOST));
		connection.setDisplay(instance.getProperties().getProperty(
				IPropertyConstants.DISPLAY));
		connection.setPort(Integer.parseInt(instance.getProperties()
				.getProperty(IPropertyConstants.PORT)));

	}

	public int getPID() {
		return pid;
	}

	public void setPID(int pid) {
		this.pid = pid;
	}

	public String getFileId() {
		IPath path = QEmuPlugin.getDefault().getStateLocation();
		String filePath = path.addTrailingSeparator().toOSString()
				+ EMULATOR_PID_FILE_NAME;

		return filePath;
	}

	public IConnection getConnection() {
		return connection;
	}

	public String getLocation() {
		return getWorkingDirectory()
				+ instance.getProperties().getProperty(
						IPropertyConstants.QEMU_BINARY_NAME);

	}

	public String getToolArguments() {
		return buildEmulatorOptions();
	}

	public String getWorkingDirectory() {
		String installedDir = instance.getProperties().getProperty(
				IPropertyConstants.QEMU_BINARY_INSTALLED_DIR);
		return installedDir.concat(SLASH);
	}

	private String buildEmulatorOptions() {
		StringBuffer buffer = new StringBuffer();
		String kernelImage = instance.getProperties().getProperty(
				IPropertyConstants.KERNEL_IMAGE);
		//buffer.append(" -L . "); //$NON-NLS-1$
		if (kernelImage != null && kernelImage != "") { //$NON-NLS-1$
			buffer.append(" -kernel "); //$NON-NLS-1$
			buffer.append(kernelImage);
		}
		String initrd = instance.getProperties().getProperty(
				IPropertyConstants.INITIAL_RAM_DISK);

		if (initrd != null && initrd != "") { //$NON-NLS-1$
			buffer.append(" -initrd "); //$NON-NLS-1$
			buffer.append(initrd);
		}
		String emulatedMachine = instance.getProperties().getProperty(
				IPropertyConstants.EMULATED_MACHINE);

		if (emulatedMachine != null && emulatedMachine != "") { //$NON-NLS-1$
			buffer.append(" -M "); //$NON-NLS-1$
			buffer.append(emulatedMachine);
		}
		String additionalOptions = instance.getProperties().getProperty(
				IPropertyConstants.ADDITIONAL_OPTIONS);

		if (additionalOptions != null && additionalOptions != "") { //$NON-NLS-1$
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(additionalOptions);
			buffer.append(" "); //$NON-NLS-1$
		}
		String enableVNC = instance.getProperties().getProperty(
				IPropertyConstants.ENABLE_VNC);

		if (enableVNC != null && Boolean.TRUE.toString().equals(enableVNC)) {
			buffer.append(" -vnc " + connection.getStringHost()); //$NON-NLS-1$
		}
		buffer.append(" -pidfile " + getFileId()); //$NON-NLS-1$
		return buffer.toString();

	}
}
