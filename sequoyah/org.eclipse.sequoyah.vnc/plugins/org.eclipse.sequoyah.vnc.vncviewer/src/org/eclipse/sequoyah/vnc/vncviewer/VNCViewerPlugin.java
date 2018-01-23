/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Franco (Motorola)
 *
 * Contributors:
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah 
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.vnc.utilities.logger.ILogger;
import org.eclipse.sequoyah.vnc.utilities.logger.Logger;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class VNCViewerPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.vnc.vncviewer"; //$NON-NLS-1$

	// The shared instance
	private static VNCViewerPlugin plugin;
	

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
	public static VNCViewerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	
	/**
	 * Gets the log container responsible to logging messages according plugin
	 * definitions.
	 * <p>
	 * <b>How to use log</b>
	 * <p>
	 * Put logging in sample class TestLog
	 * <ul>
	 * <li>add import static to this method.
	 * <p>
	 * <code>
	 *    import static org.eclipse.sequoyah.emulator.EmulatorFrameworkPlugin.log;
	 *   </code>
	 * <p>
	 * <li>send message using the appropriate method
	 * <p>
	 * <code>
	 * log(TestLog.class).info("info");<BR>
	 * log(TestLog.class).warn("warn");<BR>
	 * log(TestLog.class).debug("debug");<BR>
	 * log(TestLog.class).error("error",new Exception());<BR> 
	 * </code>
	 * 
	 * @param logClass
	 *            is the class object where the log was called.
	 * @return a log container
	 */
	@SuppressWarnings("unchecked")
	public static ILogger log(Class logClass) {
		return Logger.log(logClass);
	}
	
}
