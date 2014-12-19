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
package org.eclipse.andmore.android.emulator.core.emulationui;

import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import org.eclipse.andmore.android.emulator.core.exception.InstanceNotFoundException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.utils.EmulatorCoreUtils;

/**
 * DESCRIPTION: This class represents the parent of all nodes in the tree
 * presented in a emulation view It must have reference to the emulator host, so
 * that the tree is separated in several emulator sub-trees
 *
 * RESPONSIBILITY: To be the root of the emulator tree and maintain information
 * about which emulator is owner of the sub-tree that has this node as root
 *
 * COLABORATORS: None.
 *
 * USAGE: A class should construct an instance of this class whenever an
 * emulator information is to be included at an emulation view
 */
public class EmuViewerRootNode extends EmuViewerNode {
	/**
	 * The emulator identifier (serial port number)
	 */
	private final String serial;

	/**
	 * Constructor.
	 * 
	 * @param identifier
	 *            The identifier of the emulator that owns the sub-tree starting
	 *            at this node
	 */
	public EmuViewerRootNode(String identifier) {
		super(null, "ROOT");
		this.serial = identifier;
	}

	/**
	 * Gets the host of the emulator that owns the sub-tree starting at this
	 * node
	 * 
	 * @return The emulator host
	 */
	public String getEmulatorIdentifier() {
		return serial;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String classString;

		// For emulator root nodes, the toString method should provide the
		// emulator instance
		// name. If it is not possible to retrieve the instance name, print the
		// host itself
		String serial = getEmulatorIdentifier();
		try {
			IAndroidEmulatorInstance instance = EmulatorCoreUtils.getAndroidInstanceByIdentifier(serial);
			classString = instance.getName();
		} catch (InstanceNotFoundException e) {
			warn("The instance could not be found for retrieving its name. Using serial port instead.");
			classString = serial;
		}

		return classString;
	}
}