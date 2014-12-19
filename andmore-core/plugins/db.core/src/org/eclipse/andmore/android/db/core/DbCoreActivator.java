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
package org.eclipse.andmore.android.db.core;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.ui.view.MOTODEVDatabaseExplorerView;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DbCoreActivator extends AbstractUIPlugin {

	private static final String DB_PROPERTY_TESTER_ATT_PROPERTIES = "properties";

	private static final String DB_PROPERTY_TESTER_ATT_NAMESPACE = "namespace";

	private static final String DB_PROPERTY_TESTER_EXTENSION_ID = "org.eclipse.andmore.android.db.core.propertyTesters";

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.android.db.core"; //$NON-NLS-1$

	private static final String DB_TEMPLATE_PATH = "res/template.db"; //$NON-NLS-1$

	private static List<String> pluginProperties = null;

	// The shared instance
	private static DbCoreActivator plugin;

	public static final String DATATOOLS_UI_PLUGIN_ID = "org.eclipse.datatools.connectivity.sqm.core.ui"; //$NON-NLS-1$

	public static final String TABLE_ICON = "icons/table.gif"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(DbCoreActivator.class, "Starting MOTODEV Studio for Android Database Core Plugin...");

		super.start(context);
		plugin = this;

		DbModel.assertDriverExistsAtModel();
		DbModel.cleanPreviousProfiles();

		StudioLogger.debug(DbCoreActivator.class, "MOTODEV Studio for Android Database Core Plugin started.");
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
		DbModel.deleteDriverFromModel();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DbCoreActivator getDefault() {
		return plugin;
	}

	/*
	 * Returns a File object representing a path inside this plug-in
	 */
	private File getResourceFile(String pathAtPlugin) {
		URL location = this.getBundle().getEntry(pathAtPlugin);

		File file = null;
		try {
			IPath p = new Path(FileLocator.toFileURL(location).getFile());
			file = p.toFile();
		} catch (IOException e) {
			error(NLS.bind("Could not get resource file {0}", pathAtPlugin)); //$NON-NLS-1$
		}

		return file;

	}

	/**
	 * Retrieves the location of the SQLITE3 jdbc driver
	 * 
	 * @return
	 */
	public String getDriverPath() {
		return CommonPlugin.getDefault().getDriverPath();
	}

	/*
	 * Retrieves the templateDbFile
	 */
	private File getTemplateDbFile() {
		return getResourceFile(DB_TEMPLATE_PATH);
	}

	/**
	 * Copy the template db file to a given target file
	 * 
	 * @param target
	 * @throws IOException
	 *             If file path already exists or if the copy operation failed
	 *             for some reason.
	 */
	public void copyTemplateDbFile(File target, boolean overwrite) throws IOException {
		debug(NLS.bind("Attempting to copy db template file to {0}", target.getAbsolutePath())); //$NON-NLS-1$
		File templateDbFile = getTemplateDbFile();
		if (overwrite && target.exists()) {
			target.delete();
		}

		if (overwrite || !target.exists()) {
			debug(NLS.bind("Creating parent folders for file: {0}", target.getAbsolutePath())); //$NON-NLS-1$
			target.getParentFile().mkdirs(); // Create parent folder if needed.
			FileUtil.copyFile(templateDbFile, target);
		} else {
			throw new IOException("Target file already exists"); //$NON-NLS-1$
		}
		debug(NLS.bind("DB template file succesfully copyed to {0}", target.getAbsolutePath())); //$NON-NLS-1$
	}

	/**
	 * Retrieves the {@link MOTODEVDatabaseExplorerView} instance if it's
	 * active.
	 * 
	 * @return the active instance of the {@link MOTODEVDatabaseExplorerView},
	 *         or null if there's no active db view.
	 */
	public static MOTODEVDatabaseExplorerView getMOTODEVDatabaseExplorerView() {
		IViewPart view = EclipseUtils.getActiveView(MOTODEVDatabaseExplorerView.VIEW_ID);

		if (view instanceof MOTODEVDatabaseExplorerView) {
			return (MOTODEVDatabaseExplorerView) view;
		}

		return null;
	}

	/**
	 * @return all namespaced properties defined by
	 *         org.eclipse.core.expressions.propertyTesters extensions in this
	 *         plugin.xml file.
	 * 
	 * */
	public static List<String> getPluginProperties() {
		// only load properties from extension registry once
		if (pluginProperties == null) {
			pluginProperties = new ArrayList<String>();

			IExtension propertyTesters = Platform.getExtensionRegistry().getExtension(DB_PROPERTY_TESTER_EXTENSION_ID);

			IConfigurationElement[] confElements = propertyTesters.getConfigurationElements();

			for (IConfigurationElement confElement : confElements) {
				String namespace = confElement.getAttribute(DB_PROPERTY_TESTER_ATT_NAMESPACE);
				String property = confElement.getAttribute(DB_PROPERTY_TESTER_ATT_PROPERTIES);

				String[] properties = property.split(",");
				for (String prop : properties) {
					String namespacedProperty = namespace.concat(".").concat(prop);
					pluginProperties.add(namespacedProperty);
				}
			}
		}

		return pluginProperties;
	}
}
