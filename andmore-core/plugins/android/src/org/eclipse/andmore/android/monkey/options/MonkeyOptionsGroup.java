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
package org.eclipse.andmore.android.monkey.options;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean that represents an monkey options group
 */
public class MonkeyOptionsGroup {
	// Group ID
	private String id;

	// Group Title (user-friendly title)
	private String title;

	// Monkey options (list of the monkey options in this group)
	private List<MonkeyOption> monkeyOptions = new ArrayList<MonkeyOption>();

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public MonkeyOptionsGroup(String id) {
		this.id = id;
	}

	/**
	 * Get monkey option group ID
	 * 
	 * @return monkey option group ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set monkey option group ID
	 * 
	 * @param id
	 *            monkey option group ID
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the monkey options in this group
	 * 
	 * @return monkey options in this group
	 */
	public List<MonkeyOption> getMonkeyOptions() {
		return monkeyOptions;
	}

	/**
	 * Set the monkey options in this group
	 * 
	 * @param monkeyOptions
	 *            monkey options in this group
	 */
	public void setMonkeyOptions(List<MonkeyOption> monkeyOptions) {
		this.monkeyOptions = monkeyOptions;
	}

	/**
	 * Get monkey option group title
	 * 
	 * @return monkey option group title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set monkey option group title
	 * 
	 * @param title
	 *            monkey option group title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

}
