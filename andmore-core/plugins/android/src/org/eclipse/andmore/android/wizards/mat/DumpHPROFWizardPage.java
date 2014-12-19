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

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.elements.sorting.TableItemSortStringSetActionListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

public class DumpHPROFWizardPage extends WizardPage {

	/**
	 * Page name
	 */
	private static final String PAGE_NAME = "Dump HPROF page"; //$NON-NLS-1$

	/**
	 * SWT table control
	 */
	private DumpHPROFTable appsTable;

	/**
	 * Serial Number
	 */
	private final String serialNumber;

	/**
	 * Help Id
	 */
	private static final String helpContextId = AndroidPlugin.PLUGIN_ID + ".dump_hprof"; //$NON-NLS-1$

	/**
	 * Constructor
	 * 
	 * @param serialNumber
	 *            Serial Number.
	 */
	public DumpHPROFWizardPage(String serialNumber) {
		super(PAGE_NAME);
		this.serialNumber = serialNumber;
		setTitle(AndroidNLS.DumpHprofPage_PageTitle);
		setDescription(AndroidNLS.DumpHprofPage_PageDescription);
	}

	@Override
	public void createControl(Composite parent) {
		this.initializeDialogUnits(parent);

		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout());

		// Running apps table
		appsTable = new DumpHPROFTable(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION,
				AndroidNLS.DumpHPROFWizardPage__Message_LoadingRunningApplications, this);

		appsTable.setTableHeaderVisible(true);

		GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
		appsTable.setLayoutData(layoutData);

		TableColumn appNameColumn = appsTable.addTableColumn(SWT.CENTER);
		appNameColumn.setText(AndroidNLS.DumpHprofPage_ColumnAppName);

		appNameColumn.setWidth(this.convertWidthInCharsToPixels(70));
		appNameColumn.addSelectionListener(new TableItemSortStringSetActionListener());

		appsTable.setTableLinesVisible(false);
		appsTable.addTableSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				validatePage();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		appsTable.populateTableAsynchronously(serialNumber);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, helpContextId);
		validatePage();
		setControl(mainComposite);
	}

	/**
	 * Validates the page
	 */
	private void validatePage() {
		setPageComplete(appsTable.getTable().getSelection().length > 0);
	}

	/**
	 * Get the selected application. This will be used to generate the HPROF
	 * file
	 * 
	 * @return
	 */
	public String getSelectedApp() {
		String selectedApp = null;

		if (appsTable.getTableSelectionCont() == 1) {
			selectedApp = appsTable.getTableSelection()[0].getText(0);
		}

		return selectedApp;
	}
}
