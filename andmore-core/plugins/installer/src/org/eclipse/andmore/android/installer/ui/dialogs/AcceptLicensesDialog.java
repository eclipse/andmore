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
package org.eclipse.andmore.android.installer.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.andmore.android.installer.InstallerPlugin;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.andmore.android.installer.utilities.InstallableItem;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Accept Licenses Dialog Show all unaccepted licenses and a single radio to
 * allow accept the licenses
 */
public class AcceptLicensesDialog extends TitleAreaDialog {

	private Table table;

	private Text licenseArea;

	private Text detailsArea;

	private final InstallableItem[] unitsToInstall;

	private final List<InstallableItem> unitsWithUnacceptedLicenses;

	private String license;

	private boolean isDetailsDisplayed = false;

	public AcceptLicensesDialog(Shell parentShell, InstallableItem[] iInstallableUnits, boolean isDetailsDisplayed,
			boolean forceShowLicenseDialog) {
		super(parentShell);
		this.unitsToInstall = iInstallableUnits;
		this.isDetailsDisplayed = isDetailsDisplayed;
		if (forceShowLicenseDialog) {
			unitsWithUnacceptedLicenses = Arrays.asList(iInstallableUnits);
		} else {
			unitsWithUnacceptedLicenses = new ArrayList<InstallableItem>();
			findUnitsWithUnacceptedLicenses();
		}
	}

	private void findUnitsWithUnacceptedLicenses() {
		for (InstallableItem iu : unitsToInstall) {
			if (iu.hasLicenseNotAccepted()) {
				unitsWithUnacceptedLicenses.add(iu);
			}

		}
		Collections.sort(unitsWithUnacceptedLicenses, new IUComparator());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// create main area
		super.createDialogArea(parent).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout());

		// create sash
		SashForm sash = new SashForm(mainComposite, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		// lay out form for displaying details
		if (isDetailsDisplayed) {
			table = new Table(sash, SWT.BORDER | SWT.SINGLE);
			SashForm sashLeft = new SashForm(sash, SWT.VERTICAL);

			licenseArea = new Text(sashLeft, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
			detailsArea = new Text(sashLeft, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);

			sash.setWeights(new int[] { 35, 65 });

			sashLeft.setWeights(new int[] { 80, 20 });
		}
		// lay out form NOT to display details
		else {
			table = new Table(sash, SWT.BORDER | SWT.SINGLE);
			licenseArea = new Text(sash, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);

			sash.setWeights(new int[] { 35, 65 });
		}

		Button acceptLicense = new Button(mainComposite, SWT.RADIO);
		acceptLicense.setText(InstallerNLS.AcceptLicensesDialog_AcceptLicenseButton);
		acceptLicense.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		acceptLicense.setSelection(false);
		acceptLicense.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getButton(OK).setEnabled(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		Button doNotAcceptLicense = new Button(mainComposite, SWT.RADIO);
		doNotAcceptLicense.setText(InstallerNLS.AcceptLicensesDialog_RejectLicenseButton);
		doNotAcceptLicense.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		doNotAcceptLicense.setSelection(true);
		doNotAcceptLicense.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getButton(OK).setEnabled(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		populateTable();

		setTitle(InstallerNLS.AcceptLicensesDialog_Title);
		setMessage(InstallerNLS.AcceptLicensesDialog_Description);
		setTitleImage(AbstractUIPlugin.imageDescriptorFromPlugin(InstallerPlugin.PLUGIN_ID,
				"icons/wizban/installer_image_top.png").createImage()); //$NON-NLS-1$
		return mainComposite;
	}

	/**
	 * Populate the list of available ius
	 */
	private void populateTable() {

		for (InstallableItem iu : unitsWithUnacceptedLicenses) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setData(iu);
			item.setText(iu.getDisplayName());
		}

		if (table.getItemCount() > 0) {
			table.select(0);
			if (license != null) {
				showLicense(null);
			} else {
				showLicense((InstallableItem) table.getItem(0).getData());
			}
			// only add "show details" listener, in case details are to be
			// displayed.
			if (isDetailsDisplayed) {
				showDetails((InstallableItem) table.getItem(0).getData());
			}
		}

		table.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = ((Table) e.widget).getSelection();
				TableItem item = selection[0];
				if (item != null) {
					if (license != null) {
						showLicense(null);
					} else {
						showLicense((InstallableItem) item.getData());
					}
					// only add "show details" listener, in case details are to
					// be displayed.
					if (isDetailsDisplayed) {
						showDetails((InstallableItem) item.getData());
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});
	}

	/**
	 * Show the license in the text area
	 * 
	 * @param iu
	 */
	private void showLicense(InstallableItem iu) {
		StringBuffer buffer = new StringBuffer();
		if (iu != null) {
			String licenseText = iu.getLicense();
			buffer.append(licenseText);
		} else {
			buffer.append(license);
		}

		licenseArea.setText(buffer.toString());
		Color originalColor = licenseArea.getBackground();
		licenseArea.setEditable(false);
		licenseArea.setBackground(originalColor);
	}

	/**
	 * Set details.
	 * 
	 * @param iu
	 *            Installable item.
	 */
	private void showDetails(InstallableItem iu) {
		StringBuffer buffer = new StringBuffer();
		if (iu != null) {
			String detailsText = iu.getDescription();
			buffer.append(InstallerNLS.AcceptLicensesDialog_IUDescriptionLabel + ": " + detailsText);
		}

		detailsArea.setText(buffer.toString());
	}

	/**
	 * Save the licenses accepted
	 */
	private void acceptLicenses() {
		for (TableItem item : table.getItems()) {
			InstallableItem ii = (InstallableItem) item.getData();
			ii.acceptLicenses();
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600, 600);
		newShell.setText(InstallerNLS.ConfigurationDialog_DialogTitle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		acceptLicenses();
		super.okPressed();
	}

	@Override
	public int open() {
		int returnCode = OK;
		if (unitsWithUnacceptedLicenses.size() > 0) {
			returnCode = super.open();
		}
		return returnCode;
	}

	private class IUComparator implements Comparator<InstallableItem> {

		@Override
		public int compare(InstallableItem arg0, InstallableItem arg1) {
			return arg0.getDisplayName().compareTo(arg1.getDisplayName());
		}
	}

}
