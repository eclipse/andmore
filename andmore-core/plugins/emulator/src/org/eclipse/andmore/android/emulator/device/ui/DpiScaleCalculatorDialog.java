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
package org.eclipse.andmore.android.emulator.device.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.ISkinKeyXmlTags;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DpiScaleCalculatorDialog extends Dialog {
	private Text screenSizeValue;

	private Button monitorDpiValueButton;

	private Text monitorDpiValue;

	private Text monitorSizeText;

	private Text resultDpivalueText;

	private Integer resultDpiValue;

	private Label resultScaleText;

	private Double resultScaleValue;

	private Text resultScaleValueText;

	private Label errorLabel;

	private final IAndroidSkin skin;

	private final Collection<String> errors = new ArrayList<String>();

	int size1 = -1;

	int size2 = -1;

	/**
	 * The Dialog constructor
	 * 
	 * @param parentShell
	 *            The shell
	 * @param skin
	 *            The selected skin of the AVD being created/edited
	 */
	protected DpiScaleCalculatorDialog(Shell parentShell, IAndroidSkin skin) {
		super(parentShell);
		this.skin = skin;
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
		newShell.setText(EmulatorNLS.DPISCALECALCULATOR_Title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// Main Composite
		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout(2, false));
		GridData data;

		// Error Area
		errorLabel = new Label(mainComposite, SWT.READ_ONLY);
		errorLabel.setForeground(new Color(null, 255, 0, 0));
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		errorLabel.setLayoutData(data);

		// Screen Size
		Label screenSizeLabel = new Label(mainComposite, SWT.READ_ONLY);
		screenSizeLabel.setText(EmulatorNLS.DPISCALECALCULATOR_ScreenSize_Label);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		screenSizeLabel.setLayoutData(data);

		screenSizeValue = new Text(mainComposite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NULL, true, false);
		screenSizeValue.setLayoutData(data);
		screenSizeValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

		// Monitor DPI Group
		Group monitorDpiGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		monitorDpiGroup.setText(EmulatorNLS.DPISCALECALCULATOR_MonitorDpi_Label);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		monitorDpiGroup.setLayoutData(data);
		monitorDpiGroup.setLayout(new GridLayout(3, false));

		// Insert Monitor DPI value Option
		monitorDpiValueButton = new Button(monitorDpiGroup, SWT.RADIO);
		monitorDpiValueButton.setText(EmulatorNLS.DPISCALECALCULATOR_MonitorDpivalue_Label);
		monitorDpiValueButton.setSelection(true);
		monitorDpiValueButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				monitorDpiValue.setEnabled(monitorDpiValueButton.getSelection());
				validate();
			}
		});
		data = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		monitorDpiValueButton.setLayoutData(data);

		monitorDpiValue = new Text(monitorDpiGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		data.widthHint = 100;
		monitorDpiValue.setLayoutData(data);
		monitorDpiValue.setEnabled(monitorDpiValueButton.getSelection());
		monitorDpiValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

		// Calculate Monitor DPI Option
		final Button calculateMonitorDpiButton = new Button(monitorDpiGroup, SWT.RADIO);
		calculateMonitorDpiButton.setText(EmulatorNLS.DPISCALECALCULATOR_MonitorDpiSize_Label);
		calculateMonitorDpiButton.setSelection(false);
		calculateMonitorDpiButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				monitorSizeText.setEnabled(calculateMonitorDpiButton.getSelection());
				validate();
			}
		});

		monitorSizeText = new Text(monitorDpiGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		monitorSizeText.setLayoutData(data);
		monitorSizeText.setEnabled(calculateMonitorDpiButton.getSelection());
		monitorSizeText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

		// Result Group
		Group resultGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		resultGroup.setText(EmulatorNLS.DPISCALECALCULATOR_ResultGroup_Title);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		resultGroup.setLayoutData(data);
		resultGroup.setLayout(new GridLayout(4, false));

		// Moitor DPI
		Label resultDpi = new Label(resultGroup, SWT.READ_ONLY);
		resultDpi.setText(EmulatorNLS.DPISCALECALCULATOR_ResultMonitorDpi_Label);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		resultDpi.setLayoutData(data);

		resultDpivalueText = new Text(resultGroup, SWT.WRAP | SWT.READ_ONLY);
		data = new GridData(SWT.FILL, SWT.NULL, true, false);
		resultDpivalueText.setLayoutData(data);

		// Scale
		resultScaleText = new Label(resultGroup, SWT.READ_ONLY);
		resultScaleText.setText(EmulatorNLS.DPISCALECALCULATOR_ResultScale_Label);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		resultScaleText.setLayoutData(data);

		resultScaleValueText = new Text(resultGroup, SWT.WRAP | SWT.READ_ONLY);
		data = new GridData(SWT.FILL, SWT.NULL, true, false);
		resultScaleValueText.setLayoutData(data);

		mainComposite.layout();
		mainComposite.pack();

		return null;
	}

	/**
	 * Updates the Monitor DPI and Scale results
	 * 
	 */
	private void updateResult() {

		if (monitorDpiValueButton.getSelection()) {
			resultDpiValue = Integer.parseInt(monitorDpiValue.getText());
		} else {
			resultDpiValue = calculateMonitorDpi();
		}
		resultDpivalueText.setText(resultDpiValue.toString());

		resultScaleValue = calculateScale();
		resultScaleValueText.setText(resultScaleValue.toString());
	}

	/**
	 * Calculates the Monitor DPI using the user monitor size and the resolution
	 * 
	 * @return int The calculated Monitor DPI
	 */
	private int calculateMonitorDpi() {
		float monitorSize = Float.parseFloat(monitorSizeText.getText());
		Dimension b = Toolkit.getDefaultToolkit().getScreenSize();
		float width = b.width;
		float height = b.height;
		float ratio = width / height;

		double dpi = Math
				.round((width / (ratio * (Math.sqrt((Math.pow(monitorSize, 2)) / (1f + Math.pow(ratio, 2)))))));

		return (int) dpi;
	}

	/**
	 * Calculates the scale to be used using the monitor dpi, the device screen
	 * size and the skin main display dimensions
	 * 
	 * @return
	 */
	private double calculateScale() {
		double dpi = resultDpiValue;

		if ((skin != null) && (size1 == -1) && (size2 == -1)) {
			try {
				Collection<String> layouts = skin.getAvailableLayouts();
				String defLayout = layouts.toArray()[0].toString();

				size1 = skin.getSkinBean(defLayout).getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH);

				size2 = skin.getSkinBean(defLayout).getSkinPropertyValue(ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT);
			} catch (SkinException e) {
				StudioLogger.error(DpiScaleCalculatorDialog.class, "Error while calculating scale", //$NON-NLS-1$
						e);
			}

		}
		if ((size1 > 0) && (size2 > 0)) {
			double diagonalPx = Math.sqrt(Math.pow(size1, 2) + Math.pow(size2, 2));
			double screenSize = Double.parseDouble(screenSizeValue.getText());

			double scale = (screenSize * dpi) / diagonalPx;
			return (Math.round((scale * 100.0))) / 100.0;
		} else {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
		return 1;
	}

	/**
	 * Validates all the calculator fields
	 */
	public void validate() {
		final String REGEX_1 = EmulatorNLS.DPISCALECALCULATOR_Regex_TwoDigits;
		final String REGEX_2 = "\\d+"; //$NON-NLS-1$

		final String ERROR_SCREEN_SIZE = EmulatorNLS.DPISCALECALCULATOR_Error_ScreenSize;
		final String ERROR_DPI_VALUE = EmulatorNLS.DPISCALECALCULATOR_Error_MonitorDpi;
		final String ERROR_MONITOR_SIZE = EmulatorNLS.DPISCALECALCULATOR_Error_MonitorSize;

		errors.clear();
		errorLabel.setText(""); //$NON-NLS-1$

		if (!screenSizeValue.getText().matches(REGEX_1)) {
			errors.add(ERROR_SCREEN_SIZE);
		}

		if (monitorDpiValueButton.getSelection() && !monitorDpiValue.getText().matches(REGEX_2)) {
			errors.add(ERROR_DPI_VALUE);
		}

		if (!monitorDpiValueButton.getSelection() && !monitorSizeText.getText().matches(REGEX_1)) {
			errors.add(ERROR_MONITOR_SIZE);
		}

		if (errors.size() > 0) {
			getButton(OK).setEnabled(false);
			errorLabel.setText((String) errors.toArray()[0]);
			errorLabel.pack();

			resultDpivalueText.setText(""); //$NON-NLS-1$
			resultScaleValueText.setText(""); //$NON-NLS-1$
		} else {
			getButton(OK).setEnabled(true);
			updateResult();
		}

	}

	/**
	 * Gets the calculated device dpi
	 * 
	 * @return the device dpi to be used
	 */
	public String getResultDpivalue() {
		return resultDpiValue.toString();
	}

	/**
	 * Gets the calculated scale
	 * 
	 * @return the scale to be used
	 */
	public String getResultScaleValue() {
		return resultScaleValue.toString();
	}
}
