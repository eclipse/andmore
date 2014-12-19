/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.net.URL;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.ndk"; //$NON-NLS-1$

	// The shared instance
	private static Activator mPlugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		mPlugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		mPlugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return mPlugin;
	}

	public static <T> T getService(Class<T> clazz) {
		BundleContext context = mPlugin.getBundle().getBundleContext();
		ServiceReference ref = context.getServiceReference(clazz.getName());
		return (ref != null) ? (T) context.getService(ref) : null;
	}

	public static Bundle getBundle(String id) {
		for (Bundle bundle : mPlugin.getBundle().getBundleContext().getBundles()) {
			if (bundle.getSymbolicName().equals(id)) {
				return bundle;
			}
		}
		return null;
	}

	public static IStatus newStatus(Exception e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
	}

	public static void log(Exception e) {
		mPlugin.getLog().log(newStatus(e));
	}

	public static URL findFile(IPath path) {
		return FileLocator.find(mPlugin.getBundle(), path, null);
	}

}
