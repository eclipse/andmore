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
package org.eclipse.andmore.android.wizards.monkey;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.elements.TableWithLoadingInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Table with loading info for monkey.
 */
public class MonkeyConfigurationTabTable extends
		TableWithLoadingInfo<MonkeyConfigurationTab, Map<String, String>, String> {

	/**
	 * @see TableWithLoadingInfo#TableWithLoadingInfo(Composite, int, String,
	 *      Object)
	 */
	public MonkeyConfigurationTabTable(Composite parent, int style, String animatedTextLabel,
			MonkeyConfigurationTab callingPage) {
		super(parent, style, animatedTextLabel, callingPage);
	}

	/**
	 * Populates the table with available packages.
	 * 
	 * @see org.eclipse.andmore.android.wizards.elements.TableWithLoadingInfo#addTableData(Object)
	 */
	@Override
	protected void addTableData(Map<String, String> elementList) {
		// retrieve the available packages
		Map<String, String> availablePackages = elementList;
		// retrieve the monkey wizard page
		MonkeyConfigurationTab monkeyWizardPage = getCallingPage();
		// get the list of selected packages preference
		List<?> selectedPackagesPreference = monkeyWizardPage.getSelectedPackagesPreference();
		// get the table to be populated
		Table table = getTable();
		// iterate through the available packages
		String packageName = null;
		String packagePath = null;
		Iterator<String> it = availablePackages.keySet().iterator();
		while (it.hasNext()) {
			// the the package name and path
			packageName = it.next();
			packagePath = availablePackages.get(packageName);
			if (!monkeyWizardPage.isFilterSystem() || !packagePath.toLowerCase().contains("system")) {
				// add data
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, packageName);
				item.setText(1, packagePath.contains("system") ? AndroidNLS.UninstallAppWizardPage_SystemLabel
						: AndroidNLS.UninstallAppWizardPage_UserLabel);
				if (selectedPackagesPreference != null) {
					if (selectedPackagesPreference.contains(packageName)) {
						item.setChecked(true);
						getTable().select(table.getItemCount() - 1);
					}
				}

			}
		}
	}

	/**
	 * Get the List of installed packages from the device based on its serial
	 * number.
	 * 
	 * @see org.eclipse.andmore.android.wizards.elements.TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable(java.lang.Object)
	 */
	@Override
	protected Map<String, String> callServiceForRetrievingDataToPopulateTable(String serialNumber) {
		// installed packages to be returned
		Map<String, String> installedPackages = null;
		// based on the serial number, the the installed packages
		if (serialNumber != null) {
			try {
				installedPackages = DDMSUtils.listInstalledPackages(serialNumber);
			} catch (IOException e) {
				installedPackages = new HashMap<String, String>(0);
			}
		}
		return installedPackages;
	}

	/**
	 * Update the Launching configuration dialog.
	 * 
	 * @see org.eclipse.andmore.android.wizards.elements.TableWithLoadingInfo#executeOperationsAfterTableIsPopulated()
	 */
	@Override
	protected void executeOperationsAfterTableIsPopulated() {
		// update the launching configuration dialog
		MonkeyConfigurationTab monkeyWizardPage = getCallingPage();
		monkeyWizardPage.callUpdateLaunchConfigurationDialog();
	}

}
