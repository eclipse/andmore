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

import java.util.List;

import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.monkey.options.IMonkeyOptionsConstants;
import org.eclipse.andmore.android.monkey.options.MonkeyOption;
import org.eclipse.andmore.android.monkey.options.MonkeyOptionsGroup;
import org.eclipse.andmore.android.monkey.options.MonkeyOptionsMgt;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * DESCRIPTION: <br>
 * This class implements the UI for showing all monkey options information. <br>
 * It extends the AbstractPropertiesComposite so as to use its common
 * functionalities. <br>
 * RESPONSIBILITY: <br>
 * - Show monkey information on the UI <br>
 * COLABORATORS: <br>
 * AbstractPropertiesComposite: extends this class <br>
 * USAGE: <br>
 * This class should be added as a regular composite whenever monkey options
 * information is necessary to be shown and edited on the UI.
 */
public class MonkeyOptionsComposite extends AbstractPropertiesComposite implements IMonkeyOptionsConstants {
	// The widget which displays the current command line used to pass the
	// monkey options
	private Text commandLine;

	private final int TABFOLDER_HEIGHT_HINT = 290;

	/**
	 * Creates a MonkeyOptionsComposite object.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	public MonkeyOptionsComposite(Composite parent, String monkeyOptions) {
		super(parent);

		MonkeyOptionsMgt.loadFromCommandLine(monkeyOptions);
		createUI();

		// Set context Help
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
		// IAndroidDeviceConstants.STARTUP_OPTIONS_HELP);
	}

	/**
	 * Create widgets for monkey options
	 */
	private void createUI() {

		Composite mainComposite = this;
		Layout mainLayout = new GridLayout();
		mainComposite.setLayout(mainLayout);

		// list of monkey options groups
		List<MonkeyOptionsGroup> monkeyOptionsGroupsList = MonkeyOptionsMgt.getMonkeyOptionsGroupsList();

		// list of monkey options in each group
		List<MonkeyOption> monkeyOptions = null;

		// Create Tab Folder
		final TabFolder tabFolder = new TabFolder(mainComposite, SWT.NULL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = TABFOLDER_HEIGHT_HINT;
		tabFolder.setLayoutData(data);

		/*
		 * Iterate through Monkey Groups
		 */
		for (MonkeyOptionsGroup monkeyOptionGroup : monkeyOptionsGroupsList) {

			// Create Tab Item
			TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
			tabItem.setText(monkeyOptionGroup.getTitle());
			Composite group = new Composite(tabFolder, SWT.NULL);
			group.setLayout(new GridLayout(3, false));
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			tabItem.setControl(group);

			// get monkey options in this group
			monkeyOptions = monkeyOptionGroup.getMonkeyOptions();

			/*
			 * Iterate through Monkey Options in this group
			 */
			for (final MonkeyOption monkeyOption : monkeyOptions) {

				// create a checkbox for each monkey option
				Button checkbox = new Button(group, SWT.CHECK);
				checkbox.setSelection(monkeyOption.isChecked());
				checkbox.setText(monkeyOption.getUserFriendlyName());
				checkbox.setToolTipText(monkeyOption.getName() + ": " + monkeyOption.getDescription());

				monkeyOption.setCheckedWidget(checkbox);
				checkbox.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean checkedStatus = ((Button) e.widget).getSelection();
						monkeyOption.setChecked(checkedStatus);
						notifyCompositeChangeListeners();
					}
				});
				GridData checkboxData = new GridData(SWT.NULL, SWT.FILL, false, false);
				checkbox.setLayoutData(checkboxData);

				// Create input fields depending on the monkey option type
				switch (monkeyOption.getType()) {
				case TYPE_NONE:
					// extend checkbox area along the line
					checkboxData.widthHint = SWT.DEFAULT;
					checkboxData.horizontalSpan = 3;
					checkbox.setLayoutData(checkboxData);
					break;

				case TYPE_TEXT:
				case TYPE_NUMBER:
					createWidgetsForTextOrNumberType(group, monkeyOption);
					break;

				case TYPE_PATH:
					createWidgetsForPathType(group, monkeyOption);
					break;

				default:
					// none

				}
			}
		}

		/*
		 * Command Line area
		 */
		Composite commandLineArea = new Composite(mainComposite, SWT.NONE); // composite
		commandLineArea.setLayout(new GridLayout(2, false));
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		commandLineArea.setLayoutData(data);

		Label commandLineLabel = new Label(commandLineArea, SWT.NONE); // label
		commandLineLabel.setText(AndroidNLS.UI_MonkeyOptions_CommandLine);
		data = new GridData(SWT.FILL, SWT.FILL, false, true);
		commandLineLabel.setLayoutData(data);

		commandLine = new Text(commandLineArea, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL); // text
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		commandLineArea.pack();
		data.widthHint = commandLineArea.getBounds().width - commandLineLabel.getBounds().width;
		data.heightHint = commandLineArea.getBounds().height;
		commandLine.setLayoutData(data);
		commandLine.setText(MonkeyOptionsMgt.getParamList());
		commandLine.setEditable(false);
	}

	/**
	 * Create widgets to enable user to input data for fields of text or number
	 * type
	 * 
	 * @param parent
	 *            composite where the widgets will be attached to
	 * @param monkeyOption
	 *            the corresponding monkey option
	 */
	private void createWidgetsForTextOrNumberType(final Composite parent, final MonkeyOption monkeyOption) {
		// create input text
		if ((monkeyOption.getPreDefinedValues() == null) || (monkeyOption.getPreDefinedValues().size() == 0)) {
			final Text inputText = new Text(parent, SWT.SINGLE | SWT.BORDER);
			inputText.setText(monkeyOption.getValue());
			monkeyOption.setValueWidget(inputText);
			inputText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			inputText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					monkeyOption.setValue(inputText.getText());
					notifyCompositeChangeListeners();
				}
			});
		}
		// create combobox
		else {
			final Combo combo = new Combo(parent, SWT.READ_ONLY);
			monkeyOption.setValueWidget(combo);
			int selectedIndex = 0;
			for (String preDefinedValue : monkeyOption.getPreDefinedValues()) {
				combo.add(preDefinedValue);
				if (monkeyOption.getValue().equals(preDefinedValue)) {
					combo.select(selectedIndex);
				} else {
					selectedIndex++;
				}
			}
			combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			combo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					monkeyOption.setValue(combo.getText());
					notifyCompositeChangeListeners();
				}
			});
		}
	}

	/**
	 * Create widgets to enable user to input data for fields of path type
	 * 
	 * @param parent
	 *            composite where the widgets will be attached to
	 * @param monkeyOption
	 *            the corresponding monkey option
	 */
	private void createWidgetsForPathType(final Composite parent, final MonkeyOption monkeyOption) {
		// create input text
		final Text pathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		pathText.setText(monkeyOption.getValue());
		monkeyOption.setValueWidget(pathText);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.NULL, true, false));
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				monkeyOption.setValue(pathText.getText());
				notifyCompositeChangeListeners();
			}
		});
		// create browse button
		Button pathBrowseButton = new Button(parent, SWT.PUSH);
		pathBrowseButton.setText(AndroidNLS.UI_General_BrowseButtonLabel);
		GridData data = new GridData(SWT.NULL, SWT.NULL, false, false);
		pathBrowseButton.setLayoutData(data);
		pathBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedPath = null;

				if (monkeyOption.getTypeDetails().equals(TYPE_PATH_DIR)) {
					DirectoryDialog directoryDialog = new DirectoryDialog(getShell(), SWT.OPEN);
					selectedPath = directoryDialog.open();
				} else {
					FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					String[] filterExtensions = { "*" + monkeyOption.getTypeDetails() };
					fileDialog.setFilterExtensions(filterExtensions);
					selectedPath = fileDialog.open();
				}

				if (selectedPath != null) {
					pathText.setText(selectedPath);
				}
			}
		});
	}

	/**
	 * Update command line value
	 * 
	 * @see com.motorola.studio.android.emulator.device.ui.AbstractPropertiesComposite#notifyCompositeChangeListeners()
	 */
	@Override
	protected void notifyCompositeChangeListeners() {
		commandLine.setText(MonkeyOptionsMgt.getParamList());
		super.notifyCompositeChangeListeners();
	}

	/**
	 * Retrieves the error message associated to this composites current state.
	 * The order of precedence of error is the same as the fields displayed on
	 * the UI, which means errors on fields drawn first are shown with a higher
	 * precedence than the errors of fields drawn last. The instance description
	 * field is the only non required field.
	 * 
	 * @return the error message, or <code>null</code> if there are no errors
	 */
	@Override
	public String getErrorMessage() {
		String errMsg = null;
		Status status = MonkeyOptionsMgt.validate();
		if (status.getSeverity() == IStatus.ERROR) {
			errMsg = status.getMessage();
		}
		return errMsg;
	}

	/**
	 * Reload the values being displayed in the UI as well as the ones in the
	 * model.
	 * 
	 * @param monkeyOptions
	 *            commandLine the command line used to start the emulator
	 */
	public void reloadValues(String commandLine) {
		MonkeyOptionsMgt.loadFromCommandLine(commandLine);
	}

}
