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

/**
 * Represents the &lt;item&gt; element from menu.xml
 */
public class MenuItemNode extends AbstractMenuNode {
	private MenuNode subMenu = null;

	private String id;

	private String onClickMethod;

	/**
	 * @return the subMenu
	 */
	protected MenuNode getSubMenu() {
		return subMenu;
	}

	/**
	 * @param subMenu
	 *            the subMenu to set
	 */
	protected void setSubMenu(MenuNode subMenu) {
		this.subMenu = subMenu;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	protected void setId(String id) {
		this.id = id;
	}

	public String getOnClickMethod() {
		return onClickMethod;
	}

	public void setOnClickMethod(String onClick) {
		this.onClickMethod = onClick;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MenuItemNode [id=" + id + ", onClickMethod=" + onClickMethod + "]";
	}
}
