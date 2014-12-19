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
 * Represents the &lt;menu&gt; element from menu.xml
 */
public class MenuNode extends AbstractMenuNode {
	private final List<MenuItemNode> menuItems = new ArrayList<MenuItemNode>();

	private final List<GroupNode> groups = new ArrayList<GroupNode>();

	/**
	 * Adds {@link MenuItemNode} into menuItems list or adds {@link GroupNode}
	 * into groups list.
	 * 
	 * @param node
	 */
	public void add(AbstractMenuNode node) {
		if (node instanceof MenuItemNode) {
			MenuItemNode menuItemNode = (MenuItemNode) node;
			menuItems.add(menuItemNode);
		} else if (node instanceof GroupNode) {
			GroupNode groupNode = (GroupNode) node;
			groups.add(groupNode);
		}
	}

	/**
	 * Navigates into menu items and groups to deeply collect all menu items
	 * available in this root menu node
	 * 
	 * @return list of {@link MenuItemNode}
	 */
	public List<MenuItemNode> getAllMenuItems() {
		// adding direct menu items
		List<MenuItemNode> menuItemNodes = new ArrayList<MenuItemNode>();
		menuItemNodes.addAll(menuItems);

		// adding inner menu items from internal menu items
		for (MenuItemNode node : menuItems) {
			if (node.getSubMenu() != null) {
				// has submenu => add all items inside submenu
				menuItemNodes.addAll(node.getSubMenu().getAllMenuItems());
			}
		}
		// adding inner menu items from groups
		for (GroupNode group : groups) {
			for (MenuItemNode node : group.getMenuItems()) {
				menuItemNodes.add(node);
				if (node.getSubMenu() != null) {
					// has submenu => add all items inside submenu
					menuItemNodes.addAll(node.getSubMenu().getAllMenuItems());
				}
			}
		}
		return menuItemNodes;
	}
}
