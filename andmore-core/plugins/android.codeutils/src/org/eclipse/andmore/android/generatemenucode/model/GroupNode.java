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
package org.eclipse.andmore.android.generatemenucode.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the &lt;group&gt; element from menu.xml
 */
public class GroupNode extends AbstractMenuNode {
	private String id;

	private final List<MenuItemNode> menuItems = new ArrayList<MenuItemNode>();

	/**
	 * Adds the menu item into menuItems list
	 * 
	 * @param item
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(MenuItemNode item) {
		return menuItems.add(item);
	}

	/**
	 * @return the menuItems
	 */
	protected final List<MenuItemNode> getMenuItems() {
		return menuItems;
	}

	/**
	 * @return the id
	 */
	protected String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	protected void setId(String id) {
		this.id = id;
	}
}
