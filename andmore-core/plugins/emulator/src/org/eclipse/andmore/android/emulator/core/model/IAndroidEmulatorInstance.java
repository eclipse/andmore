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
package org.eclipse.andmore.android.emulator.core.model;

import java.io.File;
import java.util.Properties;

import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.swt.widgets.Composite;

/**
 * DESCRIPTION: This class represents the Android Emulator instance contract.
 *
 * RESPONSIBILITY: Define which information is required from a device that
 * wishes to use the Android Emulator viewer.
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the methods to retrieve information from an Android Emulator
 * device instance
 */
public interface IAndroidEmulatorInstance {
	/**
	 * Gets the instance name in simplified format
	 * 
	 * @return The instance name
	 */
	String getName();

	/**
	 * Gets the instance name in full format
	 * 
	 * @return The instance name
	 */
	String getFullName();

	/**
	 * Gets the identifier for this instance, that is available when the
	 * emulator is started.
	 * 
	 * @return the instance identifier
	 */
	String getInstanceIdentifier();

	/**
	 * Sets the current layout being used by this instance
	 * 
	 * @param layoutName
	 *            The new layout
	 */
	void setCurrentLayout(String layoutName);

	/**
	 * Gets the current known status of the flip or slide
	 * 
	 * @return True if the flip/slide is closed; false otherwise
	 */
	String getCurrentLayout();

	/**
	 * Sets the parameter used to determine if this instance has CLI display or
	 * not
	 * 
	 * @param hasCli
	 *            True if the instance has CLI; false otherwise
	 */
	void setHasCli(boolean hasCli);

	/**
	 * Gets the parameter used to determine if this instance has CLI display or
	 * not
	 * 
	 * @return True if the instance has CLI; false otherwise
	 */
	boolean getHasCli();

	/**
	 * Sets the Android protocol object created when connecting to the VM
	 * 
	 * @param handle
	 *            The Android protocol object provided when connecting the
	 *            protocol
	 */
	void setProtocolHandle(ProtocolHandle handle);

	/**
	 * Gets the Android protocol object object that identifies the protocol
	 * connection
	 * 
	 * @returns The Android protocol object representing the connection of this
	 *          instance
	 */
	ProtocolHandle getProtocolHandle();

	/**
	 * Gets the id of the skin logic being used for this instance
	 * 
	 * @return The skin id
	 */
	String getSkinId();

	/**
	 * Gets the path of the files being used to draw the skin for this instance
	 * 
	 * @return A pointer to the folder that contains the files to be used to
	 *         draw the skin for this instance
	 */
	File getSkinPath();

	/**
	 * Tests if the instance is started
	 * 
	 * @return True if it is started; false otherwise
	 */
	boolean isStarted();

	/**
	 * Test if the instance is connected, i.e. The communication protocol is
	 * running
	 * 
	 * @return True if it is connected; false otherwise
	 */
	boolean isConnected();

	/**
	 * Test if the instance is available, i.e. The instance type is available
	 * for the current SDK setup.
	 * 
	 * @return True if it's available; false otherwise
	 */
	boolean isAvailable();

	/**
	 * Stops the Android Emulator instance
	 * 
	 * @param force
	 *            whether the stop should be forced or not
	 * 
	 * @throws InstanceStopException
	 *             If the instance fails to stop
	 */
	void stop(boolean force) throws InstanceStopException;

	/**
	 * Retrieves the collection of all instance properties
	 * 
	 * @return The collection of instance properties
	 */
	public Properties getProperties();

	/**
	 * Retrieves the input logic used by this instance to send data to the
	 * emulator
	 * 
	 * @return The input logic used by this instance
	 */
	public IInputLogic getInputLogic();

	/**
	 * Gets the emulator process associated to this instance when it's running
	 * 
	 * @return the Process representing the emulator process
	 */
	public Process getProcess();

	/**
	 * Sets the emulator process associated to this emulator instance while it's
	 * running
	 * 
	 * @param process
	 */
	public void setProcess(Process process);

	/**
	 * Performs any needed operations to change the instance
	 * orientation/rotation
	 * 
	 * @param args
	 *            Additional data provided by the skin to perform the operation
	 */
	public void changeOrientation(String args);

	/**
	 * Get the Android target that the instance is compliant to
	 * 
	 * @return Android target that the instance is compliant to
	 */
	public String getTarget();

	/**
	 * Get the Android API level that the instance is compliant to
	 * 
	 * @return Android API level that the instance is compliant to
	 */
	public int getAPILevel();

	/**
	 * Get the Android Emulator window handle
	 * 
	 * @return Android target that the instance is compliant to
	 */
	public long getWindowHandle();

	/**
	 * Sets the handle of the emulator window associated with the instance
	 * 
	 * @param handle
	 */
	public void setWindowHandle(long handle);

	/**
	 * 
	 * @param contentComposite
	 */
	public void setComposite(Composite composite);

	/**
	 * 
	 * @return
	 */
	public Composite getComposite();

}
