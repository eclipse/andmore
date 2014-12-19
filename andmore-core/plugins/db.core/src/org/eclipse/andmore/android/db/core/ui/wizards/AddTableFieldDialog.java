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
package org.eclipse.andmore.android.db.core.ui.wizards;

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.Field;
import org.eclipse.andmore.android.db.core.model.Field.AutoIncrementType;
import org.eclipse.andmore.android.db.core.model.Field.DataType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddTableFieldDialog extends Dialog {

	private Field field;

	private Text nameText;

	private Combo typeCombo;

	private Text defaultText;

	private Button isPrimaryButton;

	private Composite primaryKeyOptions;

	private Button noneButton;

	private Button incrementalButton;

	private Button decrementalButton;

	private Label primaryKeyBehavior;

	public AddTableFieldDialog(Shell parentShell) {
		super(parentShell);
	}

	public AddTableFieldDialog(Shell parentShell, Field newField) {
		super(parentShell);
		field = newField;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		this.getShell().setText(DbCoreNLS.CreateTableWizardPage_AddEditField_DialogTitle);
		Composite composite = new Composite(parent, SWT.FILL);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(layoutData);
		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(DbCoreNLS.AddTableFieldDialog_FieldNameLabel);
		nameLabel.setLayoutData(layoutData);

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(layoutData);
		if (field != null) {
			nameText.setText(field.getName());
		}

		nameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				getButton(OK).setEnabled(
						(nameText.getText().trim().length() > 0) && !(nameText.getText().trim().contains(" "))); //$NON-NLS-1$

			}
		});

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		isPrimaryButton = new Button(composite, SWT.CHECK);
		isPrimaryButton.setLayoutData(layoutData);
		isPrimaryButton.setText(DbCoreNLS.AddTableFieldDialog_PrimaryKeyLabel);
		if (field != null) {
			isPrimaryButton.setSelection(field.isPrimaryKey());

		}

		isPrimaryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				defaultText.setEnabled(!(isPrimaryButton.getSelection()));
				primaryKeyOptions.setEnabled((isPrimaryButton.getSelection()));
			}
		});
		primaryKeyOptions = new Composite(composite, SWT.BORDER) {
			@Override
			public void setEnabled(boolean enabled) {
				noneButton.setEnabled(enabled);
				incrementalButton.setEnabled(enabled);
				decrementalButton.setEnabled(enabled);
				primaryKeyBehavior.setEnabled(enabled);

				super.setEnabled(enabled);
			}
		};
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		primaryKeyOptions.setLayout(new GridLayout(1, false));
		primaryKeyOptions.setLayoutData(layoutData);

		primaryKeyBehavior = new Label(primaryKeyOptions, SWT.NONE);
		primaryKeyBehavior.setLayoutData(layoutData);
		primaryKeyBehavior.setText(DbCoreNLS.AddTableFieldDialog_PrimaryKeyAutomaticBehaviourLabel);

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		noneButton = new Button(primaryKeyOptions, SWT.RADIO);
		noneButton.setLayoutData(layoutData);
		noneButton.setText(DbCoreNLS.AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_NoneLabel);

		incrementalButton = new Button(primaryKeyOptions, SWT.RADIO);
		incrementalButton.setLayoutData(layoutData);
		incrementalButton.setText(DbCoreNLS.AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_IncrementalLabel);

		decrementalButton = new Button(primaryKeyOptions, SWT.RADIO);
		decrementalButton.setLayoutData(layoutData);
		decrementalButton.setText(DbCoreNLS.AddTableFieldDialog_PrimaryKeyAutomaticBehaviour_DecrementalLabel);

		Boolean isNew = (field != null) && (field.isPrimaryKey());
		if (isNew) {
			noneButton.setSelection((field.getAutoIncrementType() == AutoIncrementType.NONE));
			incrementalButton.setSelection((field.getAutoIncrementType() == AutoIncrementType.ASCENDING));
			decrementalButton.setSelection((field.getAutoIncrementType() == AutoIncrementType.DESCENDING));

		} else {
			noneButton.setSelection(true);
		}

		primaryKeyOptions.setEnabled(isNew);

		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		Label typeLabel = new Label(composite, SWT.NONE);
		typeLabel.setText(DbCoreNLS.AddTableFieldDialog_FieldTypeLabel);
		typeLabel.setLayoutData(layoutData);

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		typeCombo = new Combo(composite, SWT.READ_ONLY);
		typeCombo.setLayoutData(layoutData);

		int integerTypeIndex = 0;

		for (DataType type : DataType.values()) {
			typeCombo.add(type.toString());

			if (type.equals(DataType.INTEGER)) {
				integerTypeIndex = typeCombo.getItemCount() - 1;
			}

		}

		typeCombo.select(integerTypeIndex);

		if (field != null) {
			typeCombo.select(typeCombo.indexOf(field.getType().toString()));
		}

		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		Label defaultLabel = new Label(composite, SWT.NONE);
		defaultLabel.setText(DbCoreNLS.AddTableFieldDialog_FieldDefaultValueLabel);
		defaultLabel.setLayoutData(layoutData);

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		defaultText = new Text(composite, SWT.BORDER);
		defaultText.setLayoutData(layoutData);
		if (field != null) {
			defaultText.setText(field.getDefaultValue());
		}

		if (field != null) {
			defaultText.setEnabled(!field.isPrimaryKey());

			primaryKeyOptions.setEnabled(field.isPrimaryKey());

		}

		composite.layout();

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		if (field == null) {
			field = new Field();
		}

		field.setName(nameText.getText());
		field.setPrimaryKey(isPrimaryButton.getSelection());
		field.setDefaultValue(defaultText.getText().trim());
		field.setType(DataType.valueOf(typeCombo.getItem(typeCombo.getSelectionIndex())));

		if (noneButton.getSelection()) {

			field.setAutoIncrementType(Field.AutoIncrementType.NONE);
		} else if (incrementalButton.getSelection()) {
			field.setAutoIncrementType(Field.AutoIncrementType.ASCENDING);
		} else if (decrementalButton.getSelection()) {
			field.setAutoIncrementType(Field.AutoIncrementType.DESCENDING);
		}

		super.okPressed();

	}

	public Field getField() {
		return field;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets
	 * .Composite, int, java.lang.String, boolean)
	 */
	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		button.setEnabled((id != OK) || (field != null));
		return button;

	}

}
