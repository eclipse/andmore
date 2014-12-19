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
package org.eclipse.andmore.android;

/**
 * DESCRIPTION: Interface to be implemented when some serial number dependent
 * piece of code needs to be run
 *
 * RESPONSIBILITY: Behaves as Runnable, but receives a serial number in the
 * parameters list
 *
 * COLABORATORS: None.
 *
 * USAGE: Provide a runnable like this when DDMSFacade needs one
 */
public interface DdmsRunnable {
	/**
	 * @see Runnable#run()
	 * 
	 * @param serialNumber
	 *            The serial number of the device that triggered this operation
	 */
	void run(String serialNumber);
}
