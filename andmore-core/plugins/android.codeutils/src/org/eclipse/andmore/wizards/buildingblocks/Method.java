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
package org.eclipse.andmore.wizards.buildingblocks;

/**
 * Abstract bean to define method descriptions to be used on the building block
 * wizards
 */
public abstract class Method {
	private final String message;

	/**
	 * Default constructor
	 * 
	 * @param message
	 *            The method description
	 */
	public Method(String message) {
		this.message = message;
	}

	/**
	 * Retrieves the method description
	 * 
	 * @return the method description
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Handles the method selection, i.e., when the user selects the method on
	 * the wizard
	 * 
	 * @param selection
	 *            if the method has been selected or not
	 */
	public abstract void handle(boolean selection);
}
