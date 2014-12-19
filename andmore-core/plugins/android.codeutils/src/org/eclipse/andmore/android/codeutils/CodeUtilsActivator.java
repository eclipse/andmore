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
package org.eclipse.andmore.android.codeutils;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CodeUtilsActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.codeutils"; //$NON-NLS-1$

	// Studio for Android Perspective ID
	public static final String PERSPECTIVE_ID = "org.eclipse.andmore.android.perspective";

	// The shared instance
	private static CodeUtilsActivator plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(CodeUtilsActivator.class, "Starting MOTODEV Android Code Utils Plugin...");

		super.start(context);
		plugin = this;

		StudioLogger.debug(CodeUtilsActivator.class, "MOTODEV Android Code Utils Plugin started.");
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
	public static CodeUtilsActivator getDefault() {
		return plugin;
	}

	/**
	 * Creates and returns a new image descriptor for an image file in this
	 * plug-in.
	 * 
	 * @param path
	 *            the relative path of the image file, relative to the root of
	 *            the plug-in; the path must be legal
	 * @return an image descriptor, or null if no image could be found
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
