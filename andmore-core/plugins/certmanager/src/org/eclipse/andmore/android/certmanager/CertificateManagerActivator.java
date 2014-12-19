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

package org.eclipse.andmore.android.certmanager;

import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CertificateManagerActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.andmore.android.certmanager"; //$NON-NLS-1$

	public static final String UNSIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID = PLUGIN_ID + ".unsign_external_pkg_wiz";

	public static final String REMOVE_SIGNATURE_WIZ_BAN = "icons/wizban/unsign_package_wiz.png";

	public static final String SIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID = PLUGIN_ID + ".sign_external_pkg_wiz";

	public static final String SIGNATURE_WIZ_BAN = "icons/wizban/sign_package_wiz.png";

	/**
	 * The manifest version
	 */
	public static final String MANIFEST_VERSION = "1.0";

	/**
	 * Manifest attribute created by
	 */
	public static final String CREATED_BY_FIELD = "Created-By";

	/**
	 * Value of Created by attribute
	 */
	public static final String CREATED_BY_FIELD_VALUE = "MOTODEV Studio for Android";

	/**
	 * Package metainf directory name
	 */
	public static final String METAFILES_DIR = "META-INF";

	/**
	 * The package manifest file name
	 */
	public static final String MANIFEST_FILE_NAME = "MANIFEST.MF";

	/**
	 * Jar separator
	 */
	public static final String JAR_SEPARATOR = "/";

	/**
	 * Prefix to be used in temp files.
	 * */
	public static final String TEMP_FILE_PREFIX = "tmppkg_";

	/**
	 * The package extension.
	 */
	public static final String PACKAGE_EXTENSION = "apk";

	/**
	 * The default package destination extension
	 */
	public static final String PACKAGE_PROJECT_DESTINATION = "dist";

	// The shared instance
	private static CertificateManagerActivator plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(CertificateManagerActivator.class,
				"Starting MOTODEV Studio for Android Key Manager Plugin...");

		super.start(context);
		plugin = this;

		StudioLogger.debug(CertificateManagerActivator.class, "MOTODEV Studio for Android Key Manager Plugin started.");
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
	public static CertificateManagerActivator getDefault() {
		return plugin;
	}

	/**
	 * The certificate manager plugin declares one view that is used to show the
	 * keystores.
	 * */
	public static KeystoreManagerView getKeyStoremManagerView() {
		IViewPart view = EclipseUtils.getActiveView(KeystoreManagerView.ID);

		if (view instanceof KeystoreManagerView) {
			return (KeystoreManagerView) view;
		}

		return null;
	}

}
