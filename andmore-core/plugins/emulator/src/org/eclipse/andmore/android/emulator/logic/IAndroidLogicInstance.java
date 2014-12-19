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
package org.eclipse.andmore.android.emulator.logic;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;

/**
 * DESCRIPTION: This class adds to the Android Emulator instance contract some
 * specific methods related to services logic.
 *
 * RESPONSIBILITY: Define which additional information is required from a
 * services to plug to the Android Emulator viewer.
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the methods to retrieve information from a Android Emulator device
 * instance
 */
public interface IAndroidLogicInstance extends IAndroidEmulatorInstance {
	/**
	 * Get the command lines arguments for the instance
	 * 
	 * @return list of command line arguments
	 */
	String getCommandLineArguments();

	/**
	 * Get the command lines arguments for the instance, returned as a Property
	 * object
	 * 
	 * @return list of command line arguments
	 */
	Properties getCommandLineArgumentsAsProperties();

	/**
	 * Get the Start logic for this instance, which is the commands used to
	 * start the instance
	 * 
	 * @return Start logic class
	 */
	AbstractStartAndroidEmulatorLogic getStartLogic();

	/**
	 * Check if there is a device connected to this instance
	 * 
	 * @return true if there is a device connected, false otherwise
	 */
	boolean hasDevice();

	/**
	 * Get the reference to the File that point to the filesystem location where
	 * the user data of the VM is.
	 * 
	 * @return the File object that references the filesystem location where the
	 *         userdata of the given VM should be. Returns a null reference if
	 *         SDK is not configured or if there is no VM with the given name.
	 */
	File getUserdata();

	/**
	 * Get the reference to the files in the VM folder that contain state data.
	 * 
	 * @return File objects that reference files in the VM folder that contain
	 *         state data.
	 */
	List<File> getStateData();

	/**
	 * Tests if there is a userdata file for this android device instance or if
	 * it is clean, i.e, there is no user data file for this Android Device
	 * Instance.
	 * 
	 * @return True if there is no working copy at that location; false
	 *         otherwise
	 */
	boolean isClean();

	/**
	 * Get the timeout value to start the instance
	 * 
	 * @return timeout value
	 */
	int getTimeout();

	File getSnapshotOriginalFilePath();

}