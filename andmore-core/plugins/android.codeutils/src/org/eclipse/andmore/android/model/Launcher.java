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
package org.eclipse.andmore.android.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.IWizardContainer;

/**
 * Launcher Controller Model. As part of a MVC architecture, this class should
 * communicate with the Wizard UI to provide all needed information to create a
 * functional Launcher Building Block (Activity, Service or Broadcast Receiver).
 */
public abstract class Launcher extends BuildingBlockModel {
	private final Set<String> intentFilterCategories = new HashSet<String>();

	private final Set<String> intentFilterActions = new HashSet<String>();

	private final Set<String> intentFilterPermissions = new HashSet<String>();

	/**
	 * Constructor for the Launcher.
	 * 
	 * @param superClass
	 *            The super class for this Launcher
	 */
	public Launcher(String superClass) {
		super(superClass);
	}

	/**
	 * Creates a Launcher with a Default Intent Filter
	 * 
	 * @param superClass
	 *            the launcher superclass
	 * @param category
	 *            the intent filter category
	 * @param action
	 *            the intent filter action
	 */
	public Launcher(String superClass, String category, String action) {
		super(superClass);
		intentFilterCategories.add(category);
		intentFilterActions.add(action);
	}

	/**
	 * Adds an Intent Filter Action to this Launcher.
	 * 
	 * @param action
	 */
	public void addIntentFilterAction(String action) {
		this.intentFilterActions.add(action);
	}

	/**
	 * Adds an intent filter category to this launcher.
	 * 
	 * @param category
	 */
	public void addIntentFilterCategories(String category) {
		intentFilterCategories.add(category);
	}

	/**
	 * Adds an intent filter category to this launcher.
	 * 
	 * @param category
	 */
	@Override
	public void addIntentFilterPermissions(String permission) {
		intentFilterPermissions.add(permission);
	}

	/**
	 * Return all intent filter actions as Array.
	 * 
	 * @return
	 */
	public String[] getIntentFilterActionsAsArray() {
		return intentFilterActions.toArray(new String[intentFilterActions.size()]);
	}

	/**
	 * Returns all intent filter actions.
	 * 
	 * @return
	 */
	public Set<String> getIntentFilterActions() {
		return intentFilterActions;
	}

	/**
	 * Returns all intent filter categories.
	 * 
	 * @return
	 */
	public Set<String> getIntentFilterCategories() {
		return intentFilterCategories;
	}

	/**
	 * Returns all intent filter categories.
	 * 
	 * @return
	 */
	@Override
	public Set<String> getIntentFilterPermissions() {
		return intentFilterPermissions;
	}

	/**
	 * Return all Filter Categories as an Array.
	 * 
	 * @return
	 */
	public String[] getIntentFilterCategoriesAsArray() {
		return intentFilterCategories.toArray(new String[intentFilterCategories.size()]);
	}

	/**
	 * Return all Filter Permissions as an Array.
	 * 
	 * @return
	 */
	@Override
	public String[] getIntentFilterPermissionsAsArray() {
		return intentFilterPermissions.toArray(new String[intentFilterPermissions.size()]);
	}

	/**
	 * Remove the Intent filter action.
	 * 
	 * @param action
	 */
	public void removeIntentFilterAction(String action) {
		intentFilterActions.remove(action);
	}

	/**
	 * Remove intent filter category.
	 * 
	 * @param category
	 */
	public void removeIntentFilterCategories(String category) {
		intentFilterCategories.remove(category);
	}

	/**
	 * Remove intent filter category.
	 * 
	 * @param category
	 */
	@Override
	public void removeIntentFilterPermissions(String permission) {
		intentFilterPermissions.remove(permission);
	}

	/**
	 * Remove all Intent Filters action.
	 */
	public void removeAllIntentFilterActions() {
		intentFilterActions.clear();
	}

	/**
	 * Remove all intent filters categories.
	 */
	public void removeAllIntentFilterCategories() {
		intentFilterCategories.clear();
	}

	/**
	 * Logs to UDC all permissions used
	 */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {

		StringBuffer permissionList = new StringBuffer("Added building block permissions: ");
		int selectedPermissionsSize = getIntentFilterPermissionsAsArray().length;

		for (int i = 0; i < selectedPermissionsSize; i++) {

			String permission = getIntentFilterPermissionsAsArray()[i];
			permissionList.append(permission);

			if (i < (selectedPermissionsSize - 1)) {
				permissionList.append(", ");
			}
		}

		if (selectedPermissionsSize > 0) {

			// Logs to UDC the permissions selected
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_BUILDINGBLOCK_PERMISSION,
					UsageDataConstants.KIND_BUILDINGBLOCK_PERMISSION, "permissions: " + permissionList.toString(), //$NON-NLS-1$
					CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());
		}
		return true;
	}

}
