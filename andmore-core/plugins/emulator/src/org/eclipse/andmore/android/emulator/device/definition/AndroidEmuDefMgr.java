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
package org.eclipse.andmore.android.emulator.device.definition;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IInputLogic;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;

/**
 * Contains methods for managing Android emulator definitions
 * 
 */
public class AndroidEmuDefMgr implements IAndroidEmuDefConstants {
	// emulator definitions
	private static final Map<String, AndroidEmuDefBean> emuDefs = new LinkedHashMap<String, AndroidEmuDefBean>();

	private final static AndroidEmuDefMgr instance = new AndroidEmuDefMgr();

	/**
	 * Initialize class
	 */
	private AndroidEmuDefMgr() {
		readExtensions();
	}

	/**
	 * Return class instance
	 * 
	 * @return AndroidEmuDefMgr instance
	 */
	public static AndroidEmuDefMgr getInstance() {
		return instance;
	}

	/**
	 * Read extension points to add emulator definitions
	 */
	private static void readExtensions() {
		IExtension[] emuDefExtensions = EclipseUtils.getInstalledPlugins(EMULATOR_DEFINITION_EXTENSION_POINT);

		for (IExtension emuDefExtension : emuDefExtensions) {
			String id = emuDefExtension.getUniqueIdentifier();

			// elements
			IConfigurationElement[] elements = emuDefExtension.getConfigurationElements();

			AndroidEmuDefBean bean = null;

			boolean extensionOk = true;
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(ELEMENT_SKIN)) {
					String skinId = element.getAttribute(ATT_SKIN_ID);
					String skinSize = element.getAttribute(ATT_SKIN_SIZE);

					if (!skinId.equals("") && !skinSize.equals("")) {
						bean = new AndroidEmuDefBean(emuDefExtension.getLabel(), skinId, skinSize);
					} else {
						extensionOk = false;
					}
				}
			}

			if (extensionOk) {
				emuDefs.put(id, bean);
			}
		}
	}

	/**
	 * Get all emulator IDs
	 * 
	 * @return all emulator IDs
	 */
	public Collection<String> getAllIds() {
		return emuDefs.keySet();
	}

	/**
	 * Retrieves the default emulator definition id, which should be initially
	 * set to the emulator devices being created
	 * 
	 * @return The default emulator definition id
	 */
	public String getDefaultId() {
		String defaultId = "";

		Collection<String> ids = getAllIds();

		/*
		 * NOTE: This is not considering more than one type of device
		 */
		if (!ids.isEmpty()) {
			Object[] idsArray = ids.toArray();
			defaultId = idsArray[0].toString();
		}

		return defaultId;
	}

	/**
	 * Get all emulator names
	 * 
	 * @return all emulator names
	 */
	public String[] getAllNames() {
		String[] allNames = new String[emuDefs.size()];

		int i = 0;
		for (AndroidEmuDefBean bean : emuDefs.values()) {
			allNames[i++] = bean.getName();
		}

		return allNames;
	}

	/**
	 * Get emulator name given its ID
	 * 
	 * @param emuDefId
	 *            emulator ID
	 * @return emulator name
	 */
	public String getName(String emuDefId) {
		String name = emuDefId;
		AndroidEmuDefBean bean = emuDefs.get(emuDefId);

		if (bean != null) {
			name = bean.getName();
		}

		return name;
	}

	/**
	 * Get emulator skin ID given its ID
	 * 
	 * @param emuDefId
	 *            emulator ID
	 * @return emulator skin ID
	 */
	public String getSkinId(String emuDefId) {
		String skinId = "";
		AndroidEmuDefBean bean = emuDefs.get(emuDefId);

		if (bean != null) {
			skinId = bean.getSkinId();
		}

		return skinId;
	}

	/**
	 * Get startup emulator command arguments
	 * 
	 * @param emuDefId
	 *            emulator ID
	 * @return emulator command line arguments
	 */
	public String getCommandLineArgumentsForEmuDefinition(String emuDefId) {
		String arguments = "";
		AndroidEmuDefBean bean = emuDefs.get(emuDefId);

		if (bean != null) {
			arguments = bean.getCommandLineArguments();
		}

		return arguments;
	}

	/**
	 * Retrieve the input logic of the given emulator definition
	 * 
	 * @param emuDefId
	 *            id of the extension that declare emulator definitions.
	 * @return the IAndroidLogic associated to the given emulator definitions
	 */
	public IInputLogic getInputLogic(String emuDefId, IAndroidEmulatorInstance instance) {

		IInputLogic inputLogic = null;

		try {
			inputLogic = (IInputLogic) PluginUtils.getExecutable(emuDefId, "inputLogic");
			inputLogic.init(instance);
		} catch (Exception e) {
			error("Could not retrieve the input logic from definition " + emuDefId);
		}

		return inputLogic;
	}

	/**
	 * Get the start logic for the given emulator definition
	 * 
	 * @param emuDefId
	 *            id of the extension that declare emulator definitions.
	 * @return the IAndroidLogic associated to the given emulator definitions
	 */
	public AbstractStartAndroidEmulatorLogic getStartLogic(String emuDefId) {

		AbstractStartAndroidEmulatorLogic startLogic = null;
		AndroidEmuDefBean bean = emuDefs.get(emuDefId);

		try {
			if (bean.getStartLogic() == null) {
				bean.setStartLogic((AbstractStartAndroidEmulatorLogic) PluginUtils
						.getExecutable(emuDefId, "startLogic"));
			}
			startLogic = bean.getStartLogic();
		} catch (Exception e) {
			error("Could not retrieve the Start logic for " + emuDefId);
		}

		return startLogic;
	}
}
