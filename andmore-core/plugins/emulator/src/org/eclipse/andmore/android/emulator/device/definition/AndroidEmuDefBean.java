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

import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;

/**
 * This class holds an emulator definition
 * 
 */
class AndroidEmuDefBean {

	// emulator name
	private String name;

	// emulator skin ID
	private String skinId;

	// emulator start logic
	private AbstractStartAndroidEmulatorLogic startLogic = null;

	// startup emulator command arguments
	private String arguments = NativeUIUtils.getDefaultCommandLine();

	/**
	 * Create an emulator definition
	 * 
	 * @param name
	 *            emulator name
	 * @param skinId
	 *            emulator skin ID
	 * @param skinSize
	 *            emulator skin size
	 */
	AndroidEmuDefBean(String name, String skinId, String skinSize) {
		this.name = name;
		this.skinId = skinId;
	}

	/**
	 * Get emulator name
	 * 
	 * @return emulator name
	 */
	String getName() {
		return name;
	}

	/**
	 * Get emulator skin ID
	 * 
	 * @return emulator skin ID
	 */
	String getSkinId() {
		return skinId;
	}

	/**
	 * Get startup emulator command arguments
	 * 
	 * @return emulator command line arguments
	 */
	String getCommandLineArguments() {
		return arguments;
	}

	/**
	 * Get emulator start logic
	 * 
	 * @return emulator start logic
	 */
	public AbstractStartAndroidEmulatorLogic getStartLogic() {
		return startLogic;
	}

	/**
	 * Set emulator start logic
	 * 
	 * @param startLogic
	 *            emulator start logic class
	 */
	public void setStartLogic(AbstractStartAndroidEmulatorLogic startLogic) {
		this.startLogic = startLogic;
	}

}
