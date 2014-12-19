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

package org.eclipse.andmore.android.packaging.ui;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class PackagingUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.packaging.ui";

	public static final String EXPORT_WIZARD_ICON = "icons/wizban/export_android_package.png";

	public static final String PACKAGING_WIZARD_CONTEXT_HELP_ID = PLUGIN_ID + ".packaging_help";

	public static final String SIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID = PackagingUIPlugin.PLUGIN_ID
			+ ".sign_external_pkg_wiz";

	public static final String UNSIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID = PackagingUIPlugin.PLUGIN_ID
			+ ".unsign_external_pkg_wiz";

	public static final int PROGRESS_MONITOR_MULTIPLIER = 100;

	// The shared instance
	private static PackagingUIPlugin plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(PackagingUIPlugin.class, "Starting MOTODEV Android Packaging UI Plugin...");

		super.start(context);
		plugin = this;

		StudioLogger.debug(PackagingUIPlugin.class, "MOTODEV Android Packaging UI Plugin started.");
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
	public static PackagingUIPlugin getDefault() {
		return plugin;
	}

}
