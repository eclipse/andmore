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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.common.utilities;

import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UtilitiesPlugin extends BasePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.device.common.utilities"; //$NON-NLS-1$
	public static final String ICON_ID_EMULATOR = "ICON_ID_EMULATOR"; //$NON-NLS-1$
	
	// The shared instance
	private static UtilitiesPlugin plugin;
	
	/**
	 * The constructor
	 */
	public UtilitiesPlugin() {
		plugin = this;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.device.common.utilities.BasePlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.device.common.utilities.BasePlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UtilitiesPlugin getDefault() {
		return plugin;
	}


	protected void initializeImageRegistry() {
		String path = getIconPath();
		putImageInRegistry(ICON_ID_EMULATOR, path+"full/obj16/emulator.gif"); //$NON-NLS-1$
	}
	
}




