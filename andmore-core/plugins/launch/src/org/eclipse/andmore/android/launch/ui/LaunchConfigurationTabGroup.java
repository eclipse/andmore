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

package org.eclipse.andmore.android.launch.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * DESCRIPTION: This class builds the configuration tabs that are displayed when
 * the user is editing the configuration to run MOTODEV Studio for Android
 * applications.
 *
 * RESPONSIBILITY: Build the configuration tab of the "Run As" features.
 *
 * COLABORATORS: << class relationship>
 *
 * USAGE: Used only by the extension definition.
 */
public class LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * Creates the tabs
	 *
	 * @param dialog
	 *            dialog
	 * @param mode
	 *            the launch mode
	 */
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab mainLaunchTab = new LaunchConfigurationTab();
		setTabs(new ILaunchConfigurationTab[] { mainLaunchTab, new CommonTab() });
	}

}
