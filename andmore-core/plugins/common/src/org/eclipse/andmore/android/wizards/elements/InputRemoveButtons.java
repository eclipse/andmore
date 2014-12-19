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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Input and Remove buttons for wizards.
 */
public class InputRemoveButtons extends Composite {
	private Button inputButton;

	private Button removeButton;

	/**
	 * Create Input and Remove buttons for wizards.
	 * 
	 * @param parent
	 */
	public InputRemoveButtons(Composite parent) {
		super(parent, SWT.NONE);
		createContents();
	}

	/**
	 * Create Contents
	 */
	protected void createContents() {
		GridData gridData;
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);
		gridData = new GridData();
		gridData.verticalAlignment = GridData.BEGINNING;

		setLayoutData(gridData);

		createInputButton();

		// Separator
		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setFont(getFont());
		separator.setVisible(false);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.verticalIndent = 4;
		separator.setLayoutData(gd);
		// Separator

		createRemoveButton();
	}

	private void createInputButton() {
		inputButton = new Button(this, SWT.PUSH);
		inputButton.setText(UtilitiesNLS.AddInputRemoveButtons_InputButtonLabel);
	}

	protected void createRemoveButton() {
		removeButton = new Button(this, SWT.PUSH);
		removeButton.setText(UtilitiesNLS.UI_AddRemoveButtons_RemoveButtonLabel);
	}

	/**
	 * Return the Input button instance.
	 * 
	 * @return the Input button instance
	 */
	public Button getInputButton() {
		return inputButton;
	}

	/**
	 * Return the Remove button instance.
	 * 
	 * @return the Remove button instance
	 */
	public Button getRemoveButton() {
		return removeButton;
	}

}
