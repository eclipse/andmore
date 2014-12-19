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
package org.eclipse.andmore.android.emulator.core.skin;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.osgi.util.NLS;

/**
 * DESCRIPTION: This class is the entrance point to the Android Emulator Skin
 * Contribution Framework module. Through this, other modules are able to
 * retrieve information about all plugins plugged at
 * com.motorola.studio.android.emulator.skin extension point.
 *
 * RESPONSIBILITY: - Provide information about plugged skins
 *
 * COLABORATORS: None.
 * 
 * USAGE: The user gets the instance of SkinFramework and calls one of its
 * public methods to get information regarding installed plugins
 */
public class SkinFramework implements ISkinFrameworkConstants {
	/**
	 * A map containing the ids of all plugged skins
	 */
	private final Map<String, String> skinIdMap = new HashMap<String, String>();

	/**
	 * Creates a new SkinFramework object.
	 */
	public SkinFramework() {
		populateSkinIdMap();
	}

	/**
	 * Retrieves the IDs of every skin that is plugged to this framework
	 *
	 * USAGE: Any client plugin that wishes to have a collection of available
	 * skins should call this method
	 *
	 * @return A collection of installed skin names
	 */
	public Collection<String> getAllInstalledSkinIds() {
		return skinIdMap.keySet();
	}

	/**
	 * Retrieves a skin object identified by the provided ID
	 *
	 * @param skinId
	 *            The ID of the skin to be retrieved
	 * 
	 * @return The skin object that have the ID provided
	 * 
	 * @throws SkinException
	 *             If the skin cannot be loaded by the skin framework
	 */
	public IAndroidSkin getSkinById(String skinId) throws SkinException {
		return getSkinById(skinId, null);
	}

	/**
	 * Retrieves a skin object identified by the provided ID
	 *
	 * @param skinId
	 *            The ID of the skin to be retrieved
	 * 
	 * @param emulatorInstallDir
	 *            Root of emulator installation
	 * 
	 * @return The skin object that have the ID provided
	 * 
	 * @throws SkinException
	 *             If the skin cannot be loaded by the skin framework
	 */
	public IAndroidSkin getSkinById(String skinId, File emulatorInstallDir) throws SkinException {
		IAndroidSkin selectedSkin = null;

		if (skinId != null) {
			try {
				// If a skin is not found at the already loaded skins
				// collection,
				// one must be created
				String extensionId = skinIdMap.get(skinId);
				if (extensionId != null) {
					selectedSkin = (IAndroidSkin) EclipseUtils.getExecutable(extensionId, SKIN_INFO_ATTR);
					if (emulatorInstallDir != null) {
						selectedSkin.setSkinFilesPath(emulatorInstallDir.getAbsolutePath());
					}
				} else {
					warn("The skin " + skinId + " was requested but not retrieved. It is not installed.");
					throw new SkinException(NLS.bind(EmulatorNLS.WARN_SkinFramework_SkinNotInstalled, skinId));
				}
			} catch (CoreException e) {
				error("It was not possible to load the IAndroidSkin object associated to " + skinId + " skin. Cause: "
						+ e.getMessage());

				throw new SkinException(NLS.bind(EmulatorNLS.EXC_SkinFramework_CreateIAndroidSkin, skinId));
			}
		} else {
			error("A null parameter as skin name was provided for retrieving a skin object");
			throw new SkinException(NLS.bind(EmulatorNLS.WARN_SkinFramework_SkinNotInstalled, "\"\""));
		}

		if (selectedSkin == null) {
			// If no exception is thrown until this moment and the skin object
			// is still null,
			// then it is assumed that the plugin has problems. Those are the
			// reasons that explain the assumption:
			// 1. The only situation in which the EclipseUtils.getExecutable
			// method returns null is
			// when the provided name is non existent;
			// 2. SKIN_INFO_ATTR is a constant that matches the constant from
			// the skin extension
			// specification. If the plugin was loaded even with a different
			// name, Eclipse has failed on detecting
			// if the declaring plugin was correctly built

			error("The skin plugin is not accordant with the skin extension point specification");
			throw new SkinException(EmulatorNLS.EXC_SkinFramework_CreateIAndroidSkin);
		}

		return selectedSkin;
	}

	/**
	 * Populates the skinIdMap map with the association of each skin name and
	 * its declaring extension identifier
	 */
	private void populateSkinIdMap() {
		String skinId;
		String extensionId;

		IExtension[] skinExtensions = EclipseUtils.getInstalledPlugins(SKIN_EXTENSION_POINT_ID);
		String currentId = "";

		try {
			for (IExtension skinExtension : skinExtensions) {
				currentId = skinExtension.getUniqueIdentifier();
				IConfigurationElement[] elements = skinExtension.getConfigurationElements();

				for (IConfigurationElement element : elements) {
					if (element.getName().equals(SKIN_INFO_ATTR)) {
						extensionId = skinExtension.getUniqueIdentifier();
						skinId = element.getAttribute(SKIN_ID_ATTR);
						if (skinId != null) {
							skinIdMap.put(skinId, extensionId);
						} else {
							warn("A invalid skin extension was not loaded because it did not declare its ID");
							String title = EmulatorNLS.GEN_Warning;
							String message = NLS.bind(EmulatorNLS.WARN_SkinFramework_InvalidInstalledSkinsNotLoaded,
									currentId);
							EclipseUtils.showErrorDialog(title, message);
						}
					}
				}
			}
		} catch (InvalidRegistryObjectException e) {
			warn("There are invalid skin extensions that were not loaded due to an exception. Cause: " + e.getMessage());
			String title = EmulatorNLS.GEN_Warning;
			EclipseUtils.showErrorDialog(title,
					NLS.bind(EmulatorNLS.WARN_SkinFramework_InvalidInstalledSkinsNotLoaded, currentId));
		}
	}
}
