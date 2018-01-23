/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Thiago Faustini Junqueira (Eldorado)
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.android.cdt.build.ui"; //$NON-NLS-1$

	// The shared instance
	private static UIPlugin plugin;

	// Constant for the Android NDK preference page.
	public static final String NDK_LOCATION_PREFERENCE = PLUGIN_ID + ".ndkpath";

	/**
	 * The constructor
	 */
	public UIPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static UIPlugin getDefault() {
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> clazz) {
		BundleContext context = plugin.getBundle().getBundleContext();
		ServiceReference ref = context.getServiceReference(clazz.getName());
		try {
			return (ref != null) ? (T) context.getService(ref) : null;
		} finally {
			if (ref != null)
				context.ungetService(ref);
		}
	}
	
	public static void log(int severity, String msg) {
		plugin.getLog().log(new Status(severity, PLUGIN_ID, msg));
	}
	
	public static void log(String msg, Exception e) {
		if (e instanceof CoreException)
			plugin.getLog().log(((CoreException)e).getStatus());
		else
			plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, msg, e));
	}
	
	public static void log(Exception e) {
		if (e instanceof CoreException)
			plugin.getLog().log(((CoreException)e).getStatus());
		else
			plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getLocalizedMessage(), e));
	}
	
}