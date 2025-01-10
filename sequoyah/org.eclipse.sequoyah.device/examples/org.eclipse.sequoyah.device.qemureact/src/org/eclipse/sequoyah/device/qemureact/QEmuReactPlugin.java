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
 * Fabio Fantato (Instituto Eldorado) - [263188] Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.qemureact;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class QEmuReactPlugin extends BasePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.device.qemureact"; //$NON-NLS-1$
	public static final String WIZARD_ID = "org.eclipse.sequoyah.device.qemureact.qemureactWizard"; //$NON-NLS-1$
	public static final String DEVICE_ID = "org.eclipse.sequoyah.device.qemureact.qemureactDevice"; //$NON-NLS-1$
	
	public static final String ICON_DEVICE_QEMUREACT = "ICON_DEVICE_QEMUREACT"; //$NON-NLS-1$
	public static final String NATURE_ID = PLUGIN_ID;

	public static final String PROPERTIES_FILENAME = "instance.properties"; //$NON-NLS-1$
	public static final String PROPERTIES_FILENAME_FULL = "/org/eclipse/sequoyah/emulator/qemureact/resources/instance.properties"; //$NON-NLS-1$

	public static final String EMULATOR_NAME = "qemu"; //$NON-NLS-1$
	public static final String EMULATOR_PARAMS = "-L . -m 128 -hda ReactOS.hd -pidfile react.id -vnc "; //$NON-NLS-1$
	public static final String EMULATOR_BIN = "qemu-react-vnc.bat"; //$NON-NLS-1$
	public static final String EMULATOR_KILL = "qemu.exe"; //$NON-NLS-1$
	public static final String EMULATOR_FILE_ID = "react.id"; //$NON-NLS-1$

	// The shared instance
	private static QEmuReactPlugin plugin;
	
	/**
	 * The constructor
	 */
	public QEmuReactPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static QEmuReactPlugin getDefault() {
		return plugin;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.common.utilities.BasePlugin#initializeImageRegistry()
	 */
	@Override
	protected void initializeImageRegistry() {
		String path = getIconPath();
		putImageInRegistry(ICON_DEVICE_QEMUREACT, path+"full/obj16/qemureact.gif"); //$NON-NLS-1$	
	}


}
