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
 * DESCRIPTION: Interface to be implemented by every entity that have serial
 * numbers
 *
 * RESPONSIBILITY: Allows the serial number of the entity to be retrieved
 *
 * COLABORATORS: None.
 *
 * USAGE: Call getSerialNumber to retrieve the entity serial number
 */
public interface ISerialNumbered {
	/**
	 * Retrieves the serial number
	 * 
	 * @return serial number
	 */
	String getSerialNumber();

	/**
	 * Retrieves the device name that is displayed in the UI
	 * 
	 * @return
	 */
	String getDeviceName();

	String getFullName();

}
