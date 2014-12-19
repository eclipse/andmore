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
package org.eclipse.andmore.android.common.utilities;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * DESCRIPTION: This class serves as an utility class containing only static
 * methods such as getters for plugin attributes, plugin resources, installation
 * path etc
 *
 * USAGE: Import this class then use the static methods.
 */
public class PluginUtils {
	public static final int OS_WINDOWS = 0;

	public static final int OS_LINUX = 1;

	/**
	 * Returns a plugin attribute using the extension as parameter.
	 *
	 * @param fromExtension
	 *            the extension from which the attribute should be collected
	 * @param element
	 *            the extension element
	 * @param attribute
	 *            the extension attribute
	 *
	 * @return the value of the extension attribute
	 *
	 * @throws MotodevExtensionException
	 *             if the executable cannot be created for any reason
	 */
	public static Object getExecutable(String extensionId, String elementName, String executableName) throws Exception

	{
		Object executable = null;

		IExtension fromExtension = getExtension(extensionId);

		if ((fromExtension != null) && (elementName != null)) {
			IConfigurationElement[] elements = fromExtension.getConfigurationElements();

			for (IConfigurationElement element : elements) {
				if (elementName.equals(element.getName())) {
					try {
						executable = element.createExecutableExtension(executableName);
					} catch (Exception e) {
						String errMsg = NLS.bind(
								UtilitiesNLS.EXC_PluginUtils_ErrorGettingTheExecutableFromExtensionPoint, new Object[] {
										executableName, elementName, extensionId });
						StudioLogger.error(PluginUtils.class, errMsg, e);

						throw new Exception(errMsg, e);
					}
				}
			}
		}

		return executable;
	}

	/**
	 * Returns a plugin attribute using the extension as parameter.
	 *
	 * @param fromExtension
	 *            the extension from which the attribute should be collected
	 * @param element
	 *            the extension element
	 * @param attribute
	 *            the extension attribute
	 *
	 * @return the value of the extension attribute
	 *
	 * @throws MotodevExtensionException
	 *             if the executable cannot be created for any reason
	 */
	public static Object getExecutable(String extensionId, String elementName) throws Exception {
		return getExecutable(extensionId, elementName, "class");
	}

	/**
	 * Returns the extension using as parameters the id of the extension and the
	 * id of its extension point.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return the extension
	 */
	public static IExtension getExtension(String extensionPointId, String extensionId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtension extension = registry.getExtension(extensionPointId, extensionId);

		return extension;
	}

	/**
	 * Returns the extension using as parameter only the id of the extension.
	 *
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return the extension
	 */
	public static IExtension getExtension(String extensionId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtension extension = registry.getExtension(extensionId);

		return extension;
	}

	/**
	 * Returns the label for the extension (extension name) using as parameters
	 * the id of the extension and the id of its extension point.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return the extension label
	 */
	public static String getExtensionLabel(String extensionPointId, String extensionId) {
		IExtension extension = getExtension(extensionPointId, extensionId);
		String extensionLabel;

		if (extension != null) {
			extensionLabel = extension.getLabel();
		} else {
			extensionLabel = extensionId;
		}

		return extensionLabel;
	}

	/**
	 * Returns the label for the extension (extension name) using as parameter
	 * only the id of the extension.
	 *
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return the extension label
	 */
	public static String getExtensionLabel(String extensionId) {
		IExtension extension = getExtension(extensionId);
		String extensionLabel;

		if (extension != null) {
			extensionLabel = extension.getLabel();
		} else {
			extensionLabel = extensionId;
		}

		return extensionLabel;
	}

	/**
	 * Returns a collection of strings containing the ids of installed plugins.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 *
	 * @return a collection object containing the ids of the installed plugins
	 */
	public static Collection<String> getInstalledPlugins(String extensionPointId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(extensionPointId);
		Collection<String> pluginIds = new LinkedHashSet<String>();

		if (extensionPoint != null) {
			for (IExtension extension : extensionPoint.getExtensions()) {
				pluginIds.add(extension.getUniqueIdentifier());
			}
		}

		return pluginIds;
	}

	/**
	 * Fills an array object with the ids contained in the collection object
	 * returned by {@link #getInstalledPlugins(String)}.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 *
	 * @return an array object containing the ids of the installed plugins
	 */
	public static String[] getInstalledPluginsAsArray(String extensionPointId) {
		Collection<String> sampleAppPluginIds = getInstalledPlugins(extensionPointId);
		String[] sampleAppPluginIdsArray = new String[sampleAppPluginIds.size()];

		return sampleAppPluginIds.toArray(sampleAppPluginIdsArray);
	}

	/**
	 * Retrieves the namespaces used by the platform.
	 *
	 *
	 * @return a collection with the namespaces used by the platform
	 */
	public static Collection<String> getPlatformNamespaces() {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		String[] namespaces = extensionRegistry.getNamespaces();

		return Arrays.asList(namespaces);
	}

	/**
	 * Returns a plugin attribute using the extension id and the extension point
	 * id as parameters.
	 *
	 * @param extensionPointId
	 *            the id of the extension point of the exten sion
	 * @param extensionId
	 *            the id of the extension
	 * @param element
	 *            the extension element
	 * @param attribute
	 *            the extension attribute
	 *
	 * @return the value of the extension attribute
	 */
	public static String getPluginAttribute(String extensionPointId, String extensionId, String element,
			String attribute) {
		IExtension fromPlugin = getExtension(extensionPointId, extensionId);

		return getPluginAttribute(fromPlugin, element, attribute);
	}

	/**
	 * Returns a plugin attribute using the extension id and the extension point
	 * id as parameters.
	 *
	 * @param extensionId
	 *            the id of the extension
	 * @param element
	 *            the extension element
	 * @param attribute
	 *            the extension attribute
	 *
	 * @return the value of the extension attribute
	 */
	public static String getPluginAttribute(String extensionId, String element, String attribute) {
		IExtension fromPlugin = getExtension(extensionId);

		return getPluginAttribute(fromPlugin, element, attribute);
	}

	/**
	 * Returns a plugin attribute using the extension as parameter.
	 *
	 * @param fromExtension
	 *            the extension from which the attribute should be collected
	 * @param element
	 *            the extension element
	 * @param attribute
	 *            the extension attribute
	 *
	 * @return the value of the extension attribute
	 */
	public static String getPluginAttribute(IExtension fromExtension, String element, String attribute) {
		String attributeValue = null;

		if (fromExtension != null) {
			IConfigurationElement[] ceArray = fromExtension.getConfigurationElements();

			for (IConfigurationElement ce : ceArray) {
				if ((ce != null) && ce.getName().equals(element)) {
					attributeValue = ce.getAttribute(attribute);
				}
			}
		}

		return attributeValue;
	}

	/**
	 * DOCUMENT ME
	 *
	 * @param extensionId
	 *            DOCUMENT ME!
	 * @param element
	 *            DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public static Collection<Map<String, String>> getPluginAttributes(String extensionId, String element) {
		IExtension fromExtension = getExtension(extensionId);

		return getPluginAttributes(fromExtension, element);
	}

	/**
	 * DOCUMENT ME
	 *
	 * @param fromExtension
	 *            DOCUMENT ME!
	 * @param element
	 *            DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public static Collection<Map<String, String>> getPluginAttributes(IExtension fromExtension, String element) {
		Collection<Map<String, String>> elementValues = new LinkedHashSet<Map<String, String>>();

		if (fromExtension != null) {
			IConfigurationElement[] ceArray = fromExtension.getConfigurationElements();

			for (IConfigurationElement ce : ceArray) {
				if ((ce != null) && ce.getName().equals(element)) {
					String[] attributes = ce.getAttributeNames();

					if ((attributes != null) && (attributes.length > 0)) {
						int attributesLenght = attributes.length;
						Map<String, String> attributesMap = new LinkedHashMap<String, String>(attributesLenght,
								attributesLenght);

						for (String attribute : attributes) {
							String attributeValue = ce.getAttribute(attribute);

							if ((attributeValue != null) && (!attributeValue.equals(""))) {
								attributesMap.put(attribute, attributeValue);
							}
						}

						// only add to elementValues if all values were read
						// correctly
						if (attributesMap.size() == attributesLenght) {
							elementValues.add(attributesMap);
						}
					}
				}
			}
		}

		return elementValues;
	}

	/**
	 * Returns the absolute path of installation as a file object using the
	 * plugin as parameter.
	 *
	 * @param plugin
	 *            the plugin installed
	 *
	 * @return a file object pointing to the installation path of the plugin
	 */
	public static File getPluginInstallationPath(Plugin plugin) {
		Bundle pluginBundle = plugin.getBundle();

		return getPluginInstallationPath(pluginBundle);
	}

	/**
	 * Returns the absolute path of installation as a file object using the ids
	 * of the extension and extension point as parameters.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return a file object pointing to the installation path of the plugin
	 */
	public static File getPluginInstallationPath(String extensionPointId, String extensionId) {
		IExtension extension = getExtension(extensionPointId, extensionId);

		return getPluginInstallationPath(extension);
	}

	/**
	 * Returns the absolute path of installation as a file object using the
	 * extension as parameter.
	 *
	 * @param extension
	 *            the extension object
	 *
	 * @return a file object pointing to the installation path of the plugin
	 */
	public static File getPluginInstallationPath(IExtension extension) {
		String pluginId = extension.getNamespaceIdentifier();
		Bundle pluginBundle = Platform.getBundle(pluginId);

		return getPluginInstallationPath(pluginBundle);
	}

	/**
	 * Returns the absolute path of installation as a file object using the
	 * plugin bundle as parameter.
	 *
	 * @param pluginBundle
	 *            the plugin bundle
	 *
	 * @return a file object pointing to the installation path of the plugin
	 */
	public static File getPluginInstallationPath(Bundle pluginBundle) {
		// get file using FileLocator
		File relativeInstalationPath = null;
		try {
			relativeInstalationPath = new File(FileLocator.toFileURL(pluginBundle.getEntry("")).getFile());
		} catch (IOException e) {
			StudioLogger.warn("Illegal state while getting plugin installation path (" + e.getMessage() + ").");
		}

		// if failed to get the file using FileLocator
		if (relativeInstalationPath == null) {
			String platformPath = Platform.getInstallLocation().getURL().getPath();
			String pluginPath = pluginBundle.getLocation();
			int removeIndex = pluginPath.indexOf("@");
			pluginPath = pluginPath.substring(removeIndex + 1);

			relativeInstalationPath = new File(platformPath, pluginPath);
		}

		return FileUtil.getCanonicalFile(relativeInstalationPath);
	}

	/**
	 * Returns a file object from the path: $installationPath\resource
	 *
	 * @param plugin
	 *            the plugin object
	 *
	 * @param resource
	 *            the plugin resource
	 *
	 * @return a file object pointing to the path of the resource
	 *
	 * @throws MotodevResourceNotAvailable
	 *             throws an exception if it occurs an I/O exception with the
	 *             path $installationPath\resource
	 */
	public static File getPluginResource(Plugin plugin, String resource) throws Exception {
		File pluginPath = getPluginInstallationPath(plugin);
		File resourceFile = new File(pluginPath, resource);
		File canonicalFile = null;

		canonicalFile = FileUtil.getCanonicalFile(resourceFile);

		return canonicalFile;
	}

	/**
	 * Checks if an extension is installed using the extension point id and
	 * extension id as parameters.
	 *
	 * @param extensionPointId
	 *            the id of the extension point
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return true if the extension is installed or false otherwise
	 */
	public static boolean isInstalled(String extensionPointId, String extensionId) {
		return getExtension(extensionPointId, extensionId) != null;
	}

	/**
	 * Checks if an extension is installed using the extension id as parameter.
	 *
	 * @param extensionId
	 *            the id of the extension
	 *
	 * @return true if the extension is installed or false otherwise
	 */
	public static boolean isInstalled(String extensionId) {
		return getExtension(extensionId) != null;
	}

	/**
	 * Returns which is the OS.
	 * 
	 * @return a code corresponding to the proper OS
	 */
	public static int getOS() {
		int result = -1;

		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.indexOf("linux") > -1) {
			result = OS_LINUX;
		}

		else if (osName.indexOf("windows") > -1) {
			result = OS_WINDOWS;
		}

		return result;
	}

	/**
	 * Retrieves the File object representing a file stored into the preferences
	 * area of the given plugin
	 *
	 * @return the File inside the given plugin preferences area.
	 * @throws MotodevException
	 *             if it fails to determine the preferences directory of the
	 *             given plugin
	 */
	public final static File getFileOnPreferenceDirectory(Plugin plugin, String filename) throws Exception {
		File targetXmlFile = null;

		try {
			IPath path = plugin.getStateLocation();

			if (path != null) {
				path = path.append(filename);
				targetXmlFile = path.toFile().getAbsoluteFile();
			}
		} catch (IllegalStateException e) {
			StudioLogger.warn("Illegal state while getting file on preferences directory (" + e.getMessage() + ").");
		}

		if (targetXmlFile == null) {
			throw new AndroidException("Could use file " + filename + " on preferences plug-in "
					+ plugin.getBundle().getBundleId());
		}

		return targetXmlFile;
	}
}
