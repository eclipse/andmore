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
package org.eclipse.andmore.android.logger.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.andmore.android.logger.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * EclipseEnvironmentManager logs the Eclipse environment information such as
 * all plug-ins available on runtime. It logs the Virtual Machine information as
 * well by calling the VMEnvironmentManager class method.
 */
public class EclipseEnvironmentManager implements EnvironmentManager {

	/**
	 * The configuration element
	 */
	private static final String CONFIGURATION = "configuration"; //$NON-NLS-1$

	/**
	 * Log file name.
	 */
	public static final String PROPERTY = "logger.properties"; //$NON-NLS-1$

	/**
	 * Platform Logger appender.
	 */
	public static final String DEFLOG_FILE = "log4j.appender.default.File"; //$NON-NLS-1$

	/**
	 * Platform Environment Logger appender.
	 */
	public static final String ENVLOG_FILE = "log4j.appender.envconf.File"; //$NON-NLS-1$

	/**
	 * Logs the environment
	 */
	@Override
	public void logEnvironment() {
		IBundleGroup[] bundleGroup;
		Bundle[] bundles;
		Bundle bundle;

		/* Map with all extensions configurations. */
		Map<String, String> log4jPropertiesMap = new LinkedHashMap<String, String>();
		Properties log4jProperties = new Properties();

		try {
			/* Reads configuration extension point extensions */
			this.getExtensionsConfiguration(log4jPropertiesMap);
			this.getPlatformConfiguration(log4jPropertiesMap);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* Builds a properties with all configurations */
		Set<String> keys = log4jPropertiesMap.keySet();
		for (String key : keys) {
			log4jProperties.setProperty(key, log4jPropertiesMap.get(key));
		}
		PropertyConfigurator.configure(log4jProperties);

		/* Logs the Environment */
		new VMEnvironmentManager().logEnvironment();

		Logger envLogger = Logger.getLogger("org.eclipse.andmore.environment"); //$NON-NLS-1$
		IBundleGroupProvider[] registry = Platform.getBundleGroupProviders();
		envLogger.info("--------------------------------------"); //$NON-NLS-1$
		envLogger.info("## Eclipse Plug-ins Log Information ##"); //$NON-NLS-1$
		envLogger.info("--------------------------------------"); //$NON-NLS-1$
		for (int i = 0; i < registry.length; i++) {
			bundleGroup = registry[i].getBundleGroups();
			for (int j = 0; j < bundleGroup.length; j++) {
				bundles = bundleGroup[j].getBundles();
				for (int k = 0; k < bundles.length; k++) {
					bundle = bundles[k];
					Dictionary<String, String> values = bundle.getHeaders();
					envLogger.info(bundle.getSymbolicName() + " - " + //$NON-NLS-1$ 
							values.get("Bundle-Version")); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Getter of the workspace path
	 * 
	 * @return The workspace path
	 */
	private File getLogsFolder() {
		IPath workspacePath = Activator.getDefault().getStateLocation();
		File file = null;
		if (workspacePath != null) {
			file = workspacePath.toFile();
		}
		return file;
	}

	/**
	 * Loads the Platform configuration.
	 * 
	 * @param log4jPropertiesMap
	 *            Map to append properties keys and values.
	 * @throws IOException
	 *             if any IO error occurs.
	 */
	private void getPlatformConfiguration(Map<String, String> log4jPropertiesMap) throws IOException {
		/* Reads the default configuration. */
		Activator activator = Activator.getDefault();
		if (activator != null) {
			URL bundleUrl = activator.getBundle().getEntry(PROPERTY);
			if (bundleUrl != null) {
				Properties props = new Properties();
				props.load(bundleUrl.openStream());

				File logs = this.getLogsFolder();
				if (!logs.exists()) {
					throw new RuntimeException("State folder does not exist."); //$NON-NLS-1$
				}

				String logFileName = props.getProperty(DEFLOG_FILE);
				String envFileName = props.getProperty(ENVLOG_FILE);
				if ((logFileName != null) && (envFileName != null)) {
					File logFile = new File(logs.getAbsolutePath() + File.separator + logFileName);
					File envFile = new File(logs.getAbsolutePath() + File.separator + envFileName);
					props.setProperty(DEFLOG_FILE, logFile.getAbsolutePath());
					props.setProperty(ENVLOG_FILE, envFile.getAbsolutePath());

					Enumeration<Object> keys = props.keys();
					while (keys.hasMoreElements()) {
						String key = (String) keys.nextElement();
						String value = props.getProperty(key);
						if (value != null) {
							log4jPropertiesMap.put(key, value);
						}
					}
				} else {
					throw new RuntimeException("Logger property file is corrupted."); //$NON-NLS-1$
				}
			} else {
				throw new RuntimeException("Could not get logger.properties URL."); //$NON-NLS-1$
			}
		} else {
			throw new RuntimeException("Could not get com.motorola.studio.platform.logger activator."); //$NON-NLS-1$
		}
	}

	/**
	 * Loads all configuration extension point extensions to read properties
	 * files.
	 * 
	 * @param log4jPropertiesMap
	 *            Map to append properties keys and values.
	 */
	private void getExtensionsConfiguration(Map<String, String> log4jPropertiesMap) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = registry.getExtensionPoint(Activator.PLUGIN_ID, CONFIGURATION);
		IExtension[] exts = extPoint.getExtensions();

		for (int i = 0; i < exts.length; i++) {
			IExtension ext = exts[i];
			if (ext != null) {
				IConfigurationElement[] configuration = ext.getConfigurationElements();
				for (int j = 0; j < configuration.length; j++) {
					IConfigurationElement config = configuration[j];
					if ((config != null) && config.getName().equals(CONFIGURATION)) {
						String file = config.getAttribute("file"); //$NON-NLS-1$
						if ((file != null) && (file.length() > 0)) {
							Bundle plugin = Platform.getBundle(ext.getNamespaceIdentifier());
							if (plugin != null) {
								URL url = plugin.getEntry(file);
								if (url != null) {
									InputStream stream = null;
									Properties props = null;
									try {
										stream = FileLocator.toFileURL(url).openStream();
										props = new Properties();
										props.load(stream);
									} catch (IOException e) {
										/*
										 * In case the custom configuration file
										 * can not be read the default will be
										 * used.
										 */
										Activator
												.getDefault()
												.getLog()
												.log(new Status(IStatus.WARNING, ext.getNamespaceIdentifier(), 0, ext
														.getNamespaceIdentifier()
														+ " can not be loaded, using default configuration.", e)); //$NON-NLS-1$
									} finally {
										try {
											stream.close();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}

									if (props != null) {
										Enumeration<Object> keys = props.keys();
										while (keys.hasMoreElements()) {
											String key = (String) keys.nextElement();
											String value = props.getProperty(key);
											if (key.startsWith("log4j.appender.") && key.endsWith(".File")) { //$NON-NLS-1$ //$NON-NLS-2$
												File logs = this.getLogsFolder();
												if (logs != null) {
													value = logs.getAbsolutePath() + File.separator + value;
													File customLogFile = new File(value);
													if (customLogFile.exists()) {
														customLogFile.delete();
													}
												}
											}
											if (value != null) {
												log4jPropertiesMap.put(key, value);
											}
										}
									}

								} else {
									Activator
											.getDefault()
											.getLog()
											.log(new Status(IStatus.WARNING, ext.getNamespaceIdentifier(), 0,
													"Could not load " + //$NON-NLS-1$ 
															file + " from " + ext.getNamespaceIdentifier() + //$NON-NLS-1$ 
															" plugin.", //$NON-NLS-1$
													null));
								}
							}
						}
					}
				}
			}
		}
	}
}
