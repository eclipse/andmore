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
 * Fabio Fantato (Motorola) - bug#221733 - Package was changed to make able to any
 * 							  other plugin access these constants values.
 * Yu-Fen Kuo (MontaVista)  - bug#236476 - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.qemu.wizard.custom;

/*
 * Defines constants used by new device instance wizard's properties
 */
public interface IPropertyConstants {
	public static final String HOST = "host"; //$NON-NLS-1$
	public static final String PORT = "port"; //$NON-NLS-1$
	public static final String DISPLAY = "display"; //$NON-NLS-1$

	public static final String QEMU_BINARY_INSTALLED_DIR = "InstalledDirectory"; //$NON-NLS-1$
	public static final String QEMU_BINARY_NAME = "QEmuBinaryName"; //$NON-NLS-1$
	public static final String KERNEL_IMAGE = "KernelImage"; //$NON-NLS-1$
	public static final String INITIAL_RAM_DISK = "InitialRAMDisk"; //$NON-NLS-1$
	public static final String EMULATED_MACHINE = "EmulatedMachine"; //$NON-NLS-1$
	public static final String ENABLE_VNC = "EnableVNC"; //$NON-NLS-1$
	public static final String ADDITIONAL_OPTIONS = "AdditionalOptions"; //$NON-NLS-1$
	
	public static final String DEFAULT_HOST = "127.0.0.1"; //$NON-NLS-1$
	public static final String DEFAULT_PORT = "5900"; //$NON-NLS-1$
	public static final String DEFAULT_DISPLAY = ":0.0"; //$NON-NLS-1$
}
