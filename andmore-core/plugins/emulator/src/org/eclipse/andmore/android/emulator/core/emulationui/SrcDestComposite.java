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
package org.eclipse.andmore.android.emulator.core.emulationui;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * DESCRIPTION: This is a composite that is used by several emulation UI
 * elements to choose source and destination elements
 *
 * RESPONSIBILITY: Provide means for the user to choose which emulator and phone
 * number will be involved in a emulation
 *
 * COLABORATORS: None.
 *
 * USAGE: Add the composite to a UI element that needs to have a emulator and a
 * phone number chosen by the user
 */
public class SrcDestComposite extends Composite {
	/**
	 * Message that will be shown near the emulator combo
	 */
	private String emulatorLabelStr;

	/**
	 * Message that will be shown near the phone number text field
	 */
	private String phoneNumberLabelStr;

	/**
	 * Emulator currently selected
	 */
	private String selectedEmulator;

	/**
	 * Phone number currently selected
	 */
	private String selectedPhoneNumber;

	/**
	 * True if the composite is valid and can provide information to the user
	 * class False if not.
	 */
	private boolean isValid = false;

	/**
	 * Error message to be shown to the user if the composite data is not valid
	 */
	private String errorMessage = NLS.bind(EmulatorNLS.ERR_SrcDestComposite_InvalidFillingBase,
			EmulatorNLS.ERR_SrcDestComposite_InvalidFillingPhoneNumber,
			EmulatorNLS.ERR_SrcDestComposite_InvalidFillingEmulator);

	// Widgets
	private Combo runningEmulatorsCombo;

	private Text phoneNumberText;

	// attribute for calculating label sizes (for layout purposes)
	private FontMetrics fontMetrics = null;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The parent composite of this one
	 * @param style
	 *            Style of the composite. See constants at SWT class
	 * @param showSrcControls
	 *            True if this composite should show the emulation source
	 *            controls. False otherwise
	 * @param isEmulatorSrc
	 *            True if this composite will have the emulator part as source
	 *            in emulation. False if the phone number will be the source
	 */
	public SrcDestComposite(Composite parent, int style, boolean showSrcControls, boolean isEmulatorSrc) {
		super(parent, style);

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 2;
		this.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(data);

		// initialize font metrics
		GC gc = new GC(this);
		gc.setFont(this.getFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();

		if (isEmulatorSrc) {
			// When emulator is the source part, its UI is build prior to phone
			// number UI,
			// and appropriate labels are used for both
			debug("Using emulator as source");
			emulatorLabelStr = EmulatorNLS.UI_SrcDestComposite_OriginatingRunningEmulatorLabel;
			phoneNumberLabelStr = EmulatorNLS.UI_SrcDestComposite_DestinationPhoneNumberLabel;
			if (showSrcControls) {
				debug("Showing source controls");
				createEmulatorUI();
			}
			createPhoneNumberUI();
		} else {
			// When phone number is the source part, its UI is build prior to
			// emulator UI,
			// and appropriate labels are used for both
			debug("Using phone number as source");
			emulatorLabelStr = EmulatorNLS.UI_SrcDestComposite_DestinationRunningEmulatorLabel;
			phoneNumberLabelStr = EmulatorNLS.UI_SrcDestComposite_OriginatingPhoneNumberLabel;
			if (showSrcControls) {
				debug("Showing source controls");
				createPhoneNumberUI();
			}
			createEmulatorUI();
		}

		addListeners();

		// call the check method to refresh error message.
		checkData();

	}

	/**
	 * Build the emulator part controls
	 */
	private void createEmulatorUI() {

		Label runningEmulatorsLabel = new Label(this, SWT.NONE);
		runningEmulatorsLabel.setText(emulatorLabelStr);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = getLabelWidthHint(runningEmulatorsLabel);
		runningEmulatorsLabel.setLayoutData(data);

		this.runningEmulatorsCombo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		this.runningEmulatorsCombo.setLayoutData(data);
		populateEmulatorCombo();

	}

	/**
	 * Build the phone number part controls
	 */
	private void createPhoneNumberUI() {

		Label phoneNumberLabel = new Label(this, SWT.NONE);
		phoneNumberLabel.setText(phoneNumberLabelStr);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = getLabelWidthHint(phoneNumberLabel);
		phoneNumberLabel.setLayoutData(data);

		this.phoneNumberText = new Text(this, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		this.phoneNumberText.setLayoutData(data);
		this.phoneNumberText.setTextLimit(40);

	}

	/**
	 * Add listeners to the composite controls
	 */
	private void addListeners() {

		if (runningEmulatorsCombo != null) {
			runningEmulatorsCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					selectedEmulator = getCurrentlySelectedIdentifier();
					checkData();
				}
			});
		}

		if (phoneNumberText != null) {
			phoneNumberText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					selectedPhoneNumber = phoneNumberText.getText();
					checkData();
				}
			});
		}
	}

	/**
	 * Defines the width hint to be used for the given label on a GridData
	 * object.
	 * 
	 * @param label
	 *            the label to calculate the width hint for
	 * 
	 * @return the width hint
	 */
	private int getLabelWidthHint(Label label) {
		int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, label.getText().length());
		return Math.max(widthHint, label.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	/**
	 * Populates the emulator combo box with the currently running emulators
	 */
	private void populateEmulatorCombo() {

		// Populating emulator combo with all running emulator names
		// Besides, keeping an array of the identifiers as combo data.
		Map<String, String> identifiersAndNames = new HashMap<String, String>();
		Collection<IAndroidEmulatorInstance> startedInstances = DeviceFrameworkManager.getInstance()
				.getAllStartedInstances();
		for (IAndroidEmulatorInstance instance : startedInstances) {
			identifiersAndNames.put(instance.getInstanceIdentifier(), instance.getName());
		}

		String[] instanceNamesArray = new String[identifiersAndNames.size()];
		String[] identifiersArray = new String[identifiersAndNames.size()];
		int i = 0;

		Set<String> identifiers = identifiersAndNames.keySet();
		for (String identifier : identifiers) {

			String viewerName = identifiersAndNames.get(identifier);

			// It is VERY important that the index used at the data array is
			// equal to the
			// index used at the items array. According to the selected item in
			// the combo, the
			// corresponding identifier is retrieved from the data array in the
			// future
			instanceNamesArray[i] = viewerName;
			identifiersArray[i] = identifier;
			i++;
		}

		runningEmulatorsCombo.setItems(instanceNamesArray);
		runningEmulatorsCombo.setData(identifiersArray);

		// if there is just one emulator in the combo list,
		// it will be chose by default.
		if (runningEmulatorsCombo.getItemCount() == 1) {
			runningEmulatorsCombo.select(0);
			selectedEmulator = getCurrentlySelectedIdentifier();
			checkData();
		}

	}

	/**
	 * Retrieve the identifier of the selected instance at Android Emulator
	 * combo box
	 * 
	 * @return The identifier, or an empty string if no emulator is selected
	 */
	private String getCurrentlySelectedIdentifier() {

		String currentlySelectedSerial = "";
		int index = runningEmulatorsCombo.getSelectionIndex();

		if (index >= 0) {
			String[] serials = (String[]) runningEmulatorsCombo.getData();
			currentlySelectedSerial = serials[index];

		}

		return currentlySelectedSerial;
	}

	/**
	 * Get the emulator identifier that was selected by the user
	 * 
	 * @return The selected emulator identifier
	 */
	public String getSelectedEmulator() {
		return selectedEmulator;
	}

	/**
	 * Get the phone number that was typed by the user
	 * 
	 * @return The phone number typed by the user
	 */
	public String getSelectedPhoneNumber() {
		return selectedPhoneNumber;
	}

	/**
	 * Tests if the values chosen/typed by the user are valid By invoking this
	 * method, the user class is able to know if it can proceed
	 * 
	 * @return True if the user has chosen valid values. False otherwise
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * Retrieves the error message to be shown to the user if the composite is
	 * not valid
	 * 
	 * @return The error message if the composite is not valid, or
	 *         <code>null</code> if the composite is valid and no error message
	 *         should be displayed.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Check if the data entered by the user is correct and set instance
	 * variables to store the test results
	 */
	private void checkData() {

		isValid = false;

		boolean isEmulatorValid = false;
		boolean isPhoneNumberValid = false;

		boolean isUsingPhoneNumber = (phoneNumberText != null);
		boolean isUsingEmulator = (runningEmulatorsCombo != null);

		// Tests if emulator selection is valid.
		//
		// If the emulator combo is null, that means that the user decided not
		// to use it. In
		// this case, it will always be valid. Otherwise, the combo selection
		// needs to be
		// not null and not blank
		if ((!isUsingEmulator) || ((selectedEmulator != null) && (!selectedEmulator.equals("")))) {
			isEmulatorValid = true;
		}

		// Tests if phone number selection is valid.
		//
		// If the phone number text is null, that means that the user decided
		// not to use it. In
		// this case, it will always be valid. Otherwise, the text field
		// selection needs to be
		// not null, not blank and can be parsed to double (that means that the
		// contents are
		// composed by numerals only)
		if (!isUsingPhoneNumber) {
			isPhoneNumberValid = true;
		} else if ((selectedPhoneNumber != null) && (!selectedPhoneNumber.equals(""))) {
			Pattern p = Pattern.compile("(\\d)+");
			Matcher m = p.matcher(selectedPhoneNumber);
			isPhoneNumberValid = m.matches();
		}

		// Based on previous checks, determine if the composite state is valid
		if (isEmulatorValid && isPhoneNumberValid) {
			isValid = true;
			errorMessage = null;
		} else {
			// If not valid, an error message will be shown. The following
			// calculations
			// are for determining which error has happened to build the message
			String phoneNumberError = "";
			String emulatorError = "";

			if (isUsingPhoneNumber && (!isPhoneNumberValid)) {
				phoneNumberError = EmulatorNLS.ERR_SrcDestComposite_InvalidFillingPhoneNumber;
			}
			if (isUsingEmulator && (!isEmulatorValid)) {
				emulatorError = EmulatorNLS.ERR_SrcDestComposite_InvalidFillingEmulator;
			}

			errorMessage = NLS.bind(EmulatorNLS.ERR_SrcDestComposite_InvalidFillingBase, phoneNumberError,
					emulatorError);
		}

	}
}
