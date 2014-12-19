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
package org.eclipse.andmore.android.wizards.mat;

import java.util.Collection;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.wizards.elements.TableWithLoadingInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

/**
 * This table is used for Dump HPROF.
 */
public class DumpHPROFTable extends TableWithLoadingInfo<DumpHPROFWizardPage, Collection<String>, String> {

	/**
	 * @see DumpHPROFTable#DumpHPROFTable(Composite, int, String, Object,
	 *      boolean, Object)
	 */
	public DumpHPROFTable(Composite parent, int style, String animatedTextLabel, DumpHPROFWizardPage callingPage) {
		super(parent, style, animatedTextLabel, callingPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.elements.tablewithloadinginfo.
	 * TableWithLoadingInfo#addTableData(java.util.Collection)
	 */
	@Override
	protected void addTableData(Collection<String> elementList) {
		Collection<String> runningApps = getElementList();

		// Populate table with the info
		if (runningApps != null) {
			for (String appName : runningApps) {
				TableItem item = new TableItem(getTable(), SWT.NONE);
				if (appName != null) {
					item.setText(0, appName);
				}
			}
		}
	}

	/**
	 * Set the wizard page completion status after data is added to the page.
	 */
	@Override
	protected void executeOperationsAfterTableIsPopulated() {
		// set the page to completed
		getCallingPage().setPageComplete(getTable().getSelection().length > 0);
	}

	/**
	 * Retrieve all running applications given a serial number.
	 */
	@Override
	protected Collection<String> callServiceForRetrievingDataToPopulateTable(String serialNumber) {
		// get the running applications
		return DDMSFacade.getRunningApplications(serialNumber);
	}
}
