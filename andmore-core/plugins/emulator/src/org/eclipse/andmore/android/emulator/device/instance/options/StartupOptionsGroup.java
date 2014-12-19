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
package org.eclipse.andmore.android.emulator.device.instance.options;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean that represents an startup options group
 * 
 */
public class StartupOptionsGroup {
	// Group ID
	private String id;

	// Group Title (user-friendly title)
	private String title;

	// Startup options (list of the startup options in this group)
	private List<StartupOption> startupOptions = new ArrayList<StartupOption>();

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public StartupOptionsGroup(String id) {
		this.id = id;
	}

	/**
	 * Get startup option group ID
	 * 
	 * @return startup option group ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set startup option group ID
	 * 
	 * @param id
	 *            startup option group ID
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the startup options in this group
	 * 
	 * @return startup options in this group
	 */
	public List<StartupOption> getStartupOptions() {
		return startupOptions;
	}

	/**
	 * Set the startup options in this group
	 * 
	 * @param startupOptions
	 *            startup options in this group
	 */
	public void setStartupOptions(List<StartupOption> startupOptions) {
		this.startupOptions = startupOptions;
	}

	/**
	 * Get startup option group title
	 * 
	 * @return startup option group title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set startup option group title
	 * 
	 * @param title
	 *            startup option group title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

}
