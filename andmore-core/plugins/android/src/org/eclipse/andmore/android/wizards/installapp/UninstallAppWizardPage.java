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
package org.eclipse.andmore.android.wizards.installapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

public class UninstallAppWizardPage extends WizardPage {

	private Table packageTable;

	private Map<String, String> availablePackages;

	private static final String contextId = AndroidPlugin.PLUGIN_ID + ".uninstall_app"; //$NON-NLS-1$

	private boolean filterSystem;

	private Thread animatedText = null;

	/**
	 * Create a new instance of this page, given the list of items to be
	 * displayed.
	 * 
	 * @param applicationList
	 *            : the list of app
	 * @param filterSystem
	 *            : show only user applications if true
	 */
	public UninstallAppWizardPage(Map<String, String> applicationList, boolean filterSystem) {
		super("uninstall app page"); //$NON-NLS-1$
		this.filterSystem = filterSystem;
		availablePackages = applicationList;
		setTitle(AndroidNLS.UninstallAppWizardPage_PageTitle);
		setDescription(AndroidNLS.UninstallAppWizardPage_PageDescription);
	}

	public UninstallAppWizardPage(Map<String, String> applicationList) {
		this(applicationList, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout());
		packageTable = new Table(mainComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		packageTable.setHeaderVisible(true);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		packageTable.setLayoutData(layoutData);
		TableColumn packageNameColumn = new TableColumn(packageTable, SWT.CENTER);
		TableColumn isSystemColumn = new TableColumn(packageTable, SWT.CENTER);
		packageNameColumn.setText(AndroidNLS.UninstallAppWizardPage_ColumnPackageName);
		isSystemColumn.setText(AndroidNLS.UninstallAppWizardPage_ColumnPackageKiind);
		packageNameColumn.setWidth(200);
		isSystemColumn.setWidth(100);
		packageNameColumn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				sortItems(0);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});
		isSystemColumn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				sortItems(1);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		populate();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, contextId);
		setControl(mainComposite);
	}

	private void populate() {
		if (availablePackages != null) {
			Iterator<String> it = availablePackages.keySet().iterator();

			while (it.hasNext()) {
				String packageName = it.next();
				String packagePath = availablePackages.get(packageName);
				if (!filterSystem || !packagePath.toLowerCase().contains("system")) //$NON-NLS-1$
				{
					TableItem item = new TableItem(packageTable, SWT.NONE);
					item.setText(0, packageName);
					item.setText(1, packagePath.contains("system") //$NON-NLS-1$
					? AndroidNLS.UninstallAppWizardPage_SystemLabel
							: AndroidNLS.UninstallAppWizardPage_UserLabel);

				}
			}
			packageTable.setLinesVisible(false);
			packageTable.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					validatePage();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});
		} else {
			final TableItem item = new TableItem(packageTable, SWT.NONE);
			animatedText = new Thread("TextAnimator") //$NON-NLS-1$
			{
				@Override
				public void run() {
					int i = 0;
					while (!isInterrupted()) {
						String text = AndroidNLS.UninstallAppWizardPage_Loading_Applications;
						for (int j = 0; j < (i % 4); j++) {
							text += "."; //$NON-NLS-1$
						}
						final String finalText = text;
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								if (!item.isDisposed()) {
									item.setText(0, finalText);
								}
							}
						});
						i++;
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			};
			animatedText.start();
		}
		packageTable.layout(true);
		validatePage();
	}

	private void validatePage() {
		setPageComplete(availablePackages != null ? packageTable.getSelection().length > 0 : false);
	}

	/**
	 * get the list of selected packages to the uninstallation
	 * 
	 * @return
	 */
	public List<String> getPackageList() {
		List<String> selectedPackages = new ArrayList<String>();
		for (TableItem item : packageTable.getSelection()) {
			selectedPackages.add(item.getText(0));
		}

		return selectedPackages;
	}

	/**
	 * Sort items based on the given column
	 * 
	 * @param column
	 */
	private void sortItems(int column) {
		// Improvement. Nothing to do here for now.
	}

	public void setAvailablePackages(Map<String, String> applicationList) {
		availablePackages = applicationList;
		if (animatedText != null) {
			animatedText.interrupt();
		}
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				packageTable.removeAll();
				populate();
			}
		});

	}
}
