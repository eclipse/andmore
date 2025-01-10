/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcel Gorri (Eldorado) - Extend to support arrays
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.extensions.implementation.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationProject;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Asks for: - the name of a new language, and - source language, and - target
 * language
 * 
 * These data will be used for translation purposes
 */
public class NewRowInputDialog extends Dialog {

	private final int DEFAULT_NUM_ENTRIES = 1;

	private final int LIMIT_NUM_ENTRIES_SIZE = 2;

	private IProject project = null;

	ILocalizationSchema localizationSchema = null;

	private String dialogTitle = null;

	private String key;

	private boolean isArray = false;

	private int numEntries = DEFAULT_NUM_ENTRIES;

	Label errorMsg = null;

	Combo addNewCombo = null;

	/*
	 * Strings area
	 */
	Label newString = null;

	Text textNewString = null;

	/*
	 * Arrays area
	 */
	Button newArray = null;

	Text textNewArray = null;

	Text unitsNewArray = null;

	Button existingArray = null;

	Combo arraysCombo = null;

	Text unitsExistingArray = null;

	Label unitsNewArrayLabel = null;

	Label unitsExistingLabel = null;

	/**
	 * The constructor
	 */
	public NewRowInputDialog(Shell parentShell, IProject project,
			String dialogTitle) {
		super(parentShell);
		this.project = project;
		this.localizationSchema = LocalizationManager.getInstance()
				.getLocalizationSchema(project);
		this.dialogTitle = dialogTitle;
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
		newShell.setText(this.dialogTitle);
	}

	/**
	 * Get the row key
	 * 
	 * @return the row key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Return whether the new key is an array (or part of an array) or not
	 * 
	 * @return true if the key is an array (or part of an array), false
	 *         otherwise
	 */
	public boolean isArray() {
		return isArray;
	}

	/**
	 * Get how many rows shall be added
	 * 
	 * @return the number of entries to be added
	 */
	public int getNumEntries() {
		return numEntries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.InputDialog#createDialogArea(org.eclipse.swt
	 * .widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		Composite newRowComposite = new Composite(parent, SWT.NONE);
		newRowComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		newRowComposite.setLayout(new GridLayout(2, false));

		errorMsg = new Label(newRowComposite, SWT.NONE);
		errorMsg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2,
				1));
		errorMsg.setForeground(new Color(getShell().getDisplay(), new RGB(255,
				0, 0)));

		/*
		 * Selection group
		 */
		createSelectionGroup(newRowComposite);

		/*
		 * String group
		 */
		createStringGroup(newRowComposite);

		/*
		 * Array group
		 */
		createArrayGroup(newRowComposite);

		// Set initial values
		setInitialValues();

		return newRowComposite;
	}

	/**
	 * Create Selection group, where the user chooses if he wants to add a
	 * string or an array
	 * 
	 * @param parent
	 *            parent composite
	 */
	private void createSelectionGroup(Composite parent) {
		Label addNewLabel = new Label(parent, SWT.NONE);
		addNewLabel.setText(Messages.NewRowDialog_AddNew);

		this.addNewCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY
				| SWT.SINGLE);
		addNewCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		addNewCombo.setItems(new String[] { Messages.NewRowDialog_String,
				Messages.NewRowDialog_Array });
		addNewCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Combo) e.getSource()).getText().equals(
						Messages.NewRowDialog_String)) {
					isArray = false;
					enableArraysArea(false);
					enableStringsArea(true);
					validateSelection();
				} else {
					isArray = true;
					enableStringsArea(false);
					enableArraysArea(true);
					validateSelection();
				}
			}
		});
	}

	/**
	 * Create String group
	 * 
	 * @param parent
	 *            parent composite
	 */
	private void createStringGroup(Composite parent) {
		Group stringsArea = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		stringsArea.setText(Messages.NewRowDialog_String);
		stringsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				2, 1));
		stringsArea.setLayout(new GridLayout(2, false));

		// new string
		this.newString = new Label(stringsArea, SWT.RADIO);
		newString.setText(Messages.NewRowDialog_RowKey);

		this.textNewString = new Text(stringsArea, SWT.BORDER);
		textNewString
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		textNewString.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent arg0) {
				key = textNewString.getText();
				validateSelection();
			}
		});
	}

	/**
	 * Create Array group
	 * 
	 * @param parent
	 *            parent composite
	 */
	private void createArrayGroup(Composite parent) {

		Group arraysArea = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		arraysArea.setText(Messages.NewRowDialog_Array);
		arraysArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				2, 1));
		arraysArea.setLayout(new GridLayout(4, false));

		// add to array
		this.existingArray = new Button(arraysArea, SWT.RADIO);
		existingArray.setText(Messages.NewRowDialog_AddToArray);
		existingArray.setSelection(true);
		existingArray.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableArraysArea(true);
				validateSelection();
			}
		});

		this.arraysCombo = new Combo(arraysArea, SWT.DROP_DOWN | SWT.READ_ONLY
				| SWT.SINGLE);
		arraysCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		arraysCombo.setItems(getAllStringArrayNames());
		arraysCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				key = ((Combo) e.getSource()).getText();
				validateSelection();
			}
		});

		this.unitsExistingArray = new Text(arraysArea, SWT.BORDER);
		unitsExistingArray.setText(String.valueOf(DEFAULT_NUM_ENTRIES));
		setEntriesTextSize(unitsExistingArray);
		unitsExistingArray.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				numEntries = Integer.parseInt(unitsExistingArray.getText());
				validateSelection();
			}
		});

		this.unitsExistingLabel = new Label(arraysArea, SWT.NONE);
		unitsExistingLabel.setText(Messages.NewRowDialog_Entries);

		// new array
		this.newArray = new Button(arraysArea, SWT.RADIO);
		newArray.setText(Messages.NewRowDialog_NewArray);
		newArray.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableArraysArea(true);
				validateSelection();
			}
		});

		this.textNewArray = new Text(arraysArea, SWT.BORDER);
		textNewArray
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		textNewArray.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent arg0) {
				key = textNewArray.getText();
				validateSelection();
			}
		});

		this.unitsNewArray = new Text(arraysArea, SWT.BORDER);
		unitsNewArray.setText(String.valueOf(DEFAULT_NUM_ENTRIES));
		setEntriesTextSize(unitsNewArray);
		unitsNewArray.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				numEntries = Integer.parseInt(unitsNewArray.getText());
				validateSelection();
			}
		});

		this.unitsNewArrayLabel = new Label(arraysArea, SWT.NONE);
		unitsNewArrayLabel.setText(Messages.NewRowDialog_Entries);
	}

	/**
	 * Define the size for entries field
	 * 
	 * @param text
	 *            Text composite
	 */
	private void setEntriesTextSize(Text text) {
		GC gc = new GC(text);
		FontMetrics fm = gc.getFontMetrics();
		GridData data = new GridData();
		data.widthHint = (LIMIT_NUM_ENTRIES_SIZE + 1)
				* fm.getAverageCharWidth();
		text.setLayoutData(data);
		text.setTextLimit(LIMIT_NUM_ENTRIES_SIZE);
	}

	/**
	 * Retrieve the list of string arrays of all files
	 * 
	 * @return the list of string arrays of all files
	 */
	private String[] getAllStringArrayNames() {

		List<String> arrayNames = new ArrayList<String>();

		LocalizationProject localizationProject = null;
		try {
			localizationProject = LocalizationManager.getInstance()
					.getProjectLocalizationManager(project, false)
					.getLocalizationProject();
		} catch (IOException e) {

		} catch (SequoyahException e) {

		}

		Set<StringArrayNode> allStringArrays = localizationProject
				.getAllStringArrays();

		Iterator<StringArrayNode> iterator = allStringArrays.iterator();

		while (iterator.hasNext()) {
			arrayNames.add(iterator.next().getKey());
		}

		return arrayNames.toArray(new String[arrayNames.size()]);
	}

	/**
	 * Populate attributes with their initial values
	 */
	private void setInitialValues() {

		this.addNewCombo.select(0);
		this.textNewString.setFocus();
		enableArraysArea(false);
		enableStringsArea(true);
		this.key = this.textNewString.getText();

	}

	/**
	 * Enable string group
	 * 
	 * @param enabled
	 *            true if the group must be enabled, false otherwise
	 */
	private void enableStringsArea(boolean enabled) {
		this.newString.setEnabled(enabled);
		this.textNewString.setEnabled(enabled);
		this.textNewString.setFocus();
	}

	/**
	 * Enable array group
	 * 
	 * @param enabled
	 *            true if the group must be enabled, false otherwise
	 */
	private void enableArraysArea(boolean enabled) {
		this.newArray.setEnabled(enabled);
		this.existingArray.setEnabled(enabled);

		if (!enabled) {
			enableNewArrayWidgets(enabled);
			enableExistingArrayWidgets(enabled);
		} else {
			if (this.newArray.getSelection()) {
				enableNewArrayWidgets(enabled);
				enableExistingArrayWidgets(!enabled);
				this.textNewArray.forceFocus();
			} else {
				enableExistingArrayWidgets(enabled);
				enableNewArrayWidgets(!enabled);
				this.arraysCombo.forceFocus();
			}
		}
	}

	/**
	 * Enable all widgets related to new array option
	 * 
	 * @param enabled
	 *            true if the widgets must be enabled, false otherwise
	 */
	private void enableNewArrayWidgets(boolean enabled) {
		this.textNewArray.setEnabled(enabled);
		this.unitsNewArray.setEnabled(enabled);
		this.unitsNewArrayLabel.setEnabled(enabled);
	}

	/**
	 * Enable all widgets related to existing array option
	 * 
	 * @param enabled
	 *            true if the widgets must be enabled, false otherwise
	 */
	private void enableExistingArrayWidgets(boolean enabled) {
		this.arraysCombo.setEnabled(enabled);
		this.unitsExistingArray.setEnabled(enabled);
		this.unitsExistingLabel.setEnabled(enabled);
	}

	/**
	 * Validate the current selection and enable/disable OK button
	 */
	private void validateSelection() {

		boolean result = true;
		String msg = ""; //$NON-NLS-1$

		/*
		 * Check if keys can have blank spaces and validate it accordingly
		 */
		boolean keyAcceptsBlankSpaces = localizationSchema
				.keyAcceptsBlankSpaces();

		if ((!keyAcceptsBlankSpaces)
				&& (this.textNewString.getText().contains(" "))) { //$NON-NLS-1$

			msg = Messages.NewRowDialog_Error_RowKeyHasBlankSpaces;
			result = false;

		} else {

			/*
			 * Strings
			 */
			if (this.addNewCombo.getText().equals(Messages.NewRowDialog_String)) {
				if (this.textNewString.getText().trim().length() == 0) {
					result = false;
				}
			}
			/*
			 * Arrays
			 */
			else {
				// existing
				if (this.existingArray.getSelection()) {
					if (this.arraysCombo.getText().equals("")) { //$NON-NLS-1$
						result = false;
					}
					try {
						int entries = Integer.parseInt(this.unitsExistingArray
								.getText());
						if (entries <= 0) {
							result = false;
						}
					} catch (NumberFormatException e) {
						result = false;
					}
				}
				// new
				else {
					if (this.textNewArray.getText().trim().length() == 0) {
						result = false;
					}
					try {
						int entries = Integer.parseInt(this.unitsNewArray
								.getText());
						if (entries <= 0) {
							result = false;
						}
					} catch (NumberFormatException e) {
						result = false;
					}
				}
			}
		}

		getButton(IDialogConstants.OK_ID).setEnabled(result);
		errorMsg.setText(msg);
	}
}
