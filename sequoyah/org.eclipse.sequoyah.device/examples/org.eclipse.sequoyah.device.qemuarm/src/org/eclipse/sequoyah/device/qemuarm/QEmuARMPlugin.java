/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Eldorado Research Institute)
 * 
 * Contributors:
 * Fabio Fantato (Eldorado Research Institute) - [221733] Persistence and New wizard for manage Device Instances
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type 
 * Daniel Barboza Franco (Eldorado Research Institute) - [221740] - Sample implementation for Linux host
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemuarm;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class QEmuARMPlugin extends BasePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.device.qemuarm"; //$NON-NLS-1$
	public static final String DEVICE_ID = "org.eclipse.sequoyah.device.qemuarm.qemuarmDevice"; //$NON-NLS-1$
	public static final String WIZARD_ID = "org.eclipse.sequoyah.device.qemuarm.qemuarmWizard"; //$NON-NLS-1$
	
	public static final String ICON_DEVICE_QEMUARM = "ICON_DEVICE_QEMUARM"; //$NON-NLS-1$
	
	public static final String EMULATOR_NAME = "qemu"; //$NON-NLS-1$
	public static final String EMULATOR_PARAMS = "-kernel integratorcp.zImage -pidfile qemuarm.id -initrd arm_root.img -vnc "; //$NON-NLS-1$
	public static final String EMULATOR_FILE_ID = "qemuarm.id"; //$NON-NLS-1$
	
	public static final String EMULATOR_WIN32_BIN = "qemu-arm-vnc.bat"; //$NON-NLS-1$
	public static final String EMULATOR_WIN32_KILL = "qemu-system-arm.exe"; //$NON-NLS-1$
	
	public static final String EMULATOR_LINUX_BIN = "qemu-arm-vnc.sh"; //$NON-NLS-1$
	public static final String EMULATOR_LINUX_KILL = "qemu-system-arm"; //$NON-NLS-1$
	
	// The shared instance
	private static QEmuARMPlugin plugin;
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor
	 */
	public QEmuARMPlugin() {
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
	public static QEmuARMPlugin getDefault() {
		return plugin;
	}
	
	/** Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = QEmuARMPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	
	/** Returns a string formatted from the specified resource string and the
	 * associated arguments.
	 * 
	 * @param key
	 *            the key associated with the format string in the resource
	 *            bundle
	 * @param args
	 *            items to be expanded into the format string
	 * @return formatted string
	 */
	public static String getResourceString(String key, Object[] args) {
		return MessageFormat.format(getResourceString(key), args);
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle(PLUGIN_ID
						+ ".QemuResources"); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			//logErrorMessage(e.getMessage());
			resourceBundle = null;
		}
		return resourceBundle;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.common.utilities.BasePlugin#initializeImageRegistry()
	 */
	@Override
	protected void initializeImageRegistry() {
		String path = getIconPath();
		putImageInRegistry(ICON_DEVICE_QEMUARM, path+"full/obj16/qemuarm.gif"); //$NON-NLS-1$	
	}


}
