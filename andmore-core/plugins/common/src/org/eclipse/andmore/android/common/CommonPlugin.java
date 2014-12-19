/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.android.common;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommonPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.common"; //$NON-NLS-1$

	private static final String JDBC_DRIVER_PATH = "res/androidjdbc.jar";

	public static final String JDBC_DRIVER_INSTANCE_NAME = "motodev_jdbc_driver";

	// The shared instance
	private static CommonPlugin plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(CommonPlugin.class, "Starting MOTODEV Android Common Plugin...");

		super.start(context);
		plugin = this;

		StudioLogger.debug(CommonPlugin.class, "MOTODEV Android Common Plugin started.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CommonPlugin getDefault() {
		return plugin;
	}

	/**
	 * Retrieves the location of the driver
	 * 
	 * @return
	 */
	public String getDriverPath() {
		String driverPath = "";
		if (getDbResourceFile(JDBC_DRIVER_PATH) != null) {
			driverPath = getDbResourceFile(JDBC_DRIVER_PATH).getAbsolutePath();
		}
		return driverPath;
	}

	private File getDbResourceFile(String pathAtPlugin) {
		URL location = getBundle().getEntry(pathAtPlugin);

		debug("JDBC Driver Location:" + location + " JDBC Driver getBundle().getLocation():"
				+ getBundle().getLocation());

		File file = null;
		try {
			IPath p = new Path(FileLocator.toFileURL(location).getFile());
			debug("JDBC Driver Path:" + p.toOSString());
			file = p.toFile();
		} catch (IOException e) {
			error("Error while trying to locate jdbc driver into db plugin:" + e.getMessage());
		}
		return file;

	}

}
