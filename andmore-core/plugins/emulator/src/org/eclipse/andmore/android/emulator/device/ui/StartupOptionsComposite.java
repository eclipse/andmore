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

import java.util.List;

import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.device.IAndroidDeviceConstants;
import org.eclipse.andmore.android.emulator.device.instance.options.IStartupOptionsConstants;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOption;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOptionsGroup;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOptionsMgt;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * DESCRIPTION: <br>
 * This class implements the UI for showing all Android Emulator Device Instance
 * startup options information. <br>
 * It extends the AbstractPropertiesComposite so as to use its common
 * functionalities. <br>
 * RESPONSIBILITY: <br>
 * - Show Android Emulator Device Instance main information on the UI <br>
 * COLABORATORS: <br>
 * AbstractPropertiesComposite: extends this class <br>
 * USAGE: <br>
 * This class should be added as a regular composite whenever startup options
 * information on Android Emulator Device Instance is necessary to be shown and
 * edited on the UI.
 */
public class StartupOptionsComposite extends AbstractPropertiesComposite implements IStartupOptionsConstants {
	// The widget which displays the current command line used to pass the
	// startup options
	private Text commandLine;

	private final IAndroidSkin skin;

	private final int TABFOLDER_HEIGHT_HINT = 350;

	private boolean canCalculateScale = true;

	/**
	 * Creates a StartupOptionsComposite object.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param canCalculateScale
	 */
	public StartupOptionsComposite(Composite parent, String startupOptions, IAndroidSkin skin, boolean canCalculateScale) {
		super(parent);

		this.skin = skin;
		this.canCalculateScale = canCalculateScale;
		StartupOptionsMgt.loadFromCommandLine(startupOptions);
		createUI();

		// Set context Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAndroidDeviceConstants.STARTUP_OPTIONS_HELP);
	}

	/**
	 * Create widgets for startup options
	 */
	private void createUI() {

		Composite mainComposite = this;
		Layout mainLayout = new GridLayout();
		mainComposite.setLayout(mainLayout);

		// list of startup options groups
		List<StartupOptionsGroup> startupOptionsGroupsList = StartupOptionsMgt.getStartupOptionsGroupsList();

		// list of startup options in each group
		List<StartupOption> startupOptions = null;

		// Create Tab Folder
		final TabFolder tabFolder = new TabFolder(mainComposite, SWT.NULL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = TABFOLDER_HEIGHT_HINT;
		tabFolder.setLayoutData(data);

		/*
		 * Iterate through Startup Groups
		 */
		for (StartupOptionsGroup startupOptionGroup : startupOptionsGroupsList) {

			// Create Tab Item
			TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
			tabItem.setText(startupOptionGroup.getTitle());
			Composite group = new Composite(tabFolder, SWT.NULL);
			group.setLayout(new GridLayout(3, false));
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			tabItem.setControl(group);

			// get startup options in this group
			startupOptions = startupOptionGroup.getStartupOptions();

			/*
			 * Iterate through Startup Options in this group
			 */
			for (final StartupOption startupOption : startupOptions) {

				// create a checkbox for each startup option
				Button checkbox = new Button(group, SWT.CHECK);
				checkbox.setSelection(startupOption.isChecked());
				checkbox.setText(startupOption.getUserFriendlyName());
				checkbox.setToolTipText(startupOption.getName() + ": " //$NON-NLS-1$
						+ startupOption.getDescription());
				startupOption.setCheckedWidget(checkbox);
				checkbox.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean checkedStatus = ((Button) e.widget).getSelection();
						startupOption.setChecked(checkedStatus);
						notifyCompositeChangeListeners();
					}
				});
				GridData checkboxData = new GridData(SWT.NULL, SWT.FILL, false, false);
				checkbox.setLayoutData(checkboxData);

				// Create input fields depending on the startup option type
				switch (startupOption.getType()) {
				case TYPE_NONE:
					// extend checkbox area along the line
					checkboxData.widthHint = SWT.DEFAULT;
					checkboxData.horizontalSpan = 3;
					checkbox.setLayoutData(checkboxData);
					break;

				case TYPE_TEXT:
				case TYPE_NUMBER:
					createWidgetsForTextOrNumberType(group, startupOption);
					break;

				case TYPE_PATH:
					createWidgetsForPathType(group, startupOption);
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
		commandLineLabel.setText(""); //$NON-NLS-1$
		data = new GridData(SWT.FILL, SWT.FILL, false, true);
		commandLineLabel.setLayoutData(data);

		commandLine = new Text(commandLineArea, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL); // text
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		commandLineArea.pack();
		data.widthHint = commandLineArea.getBounds().width - commandLineLabel.getBounds().width;
		data.heightHint = commandLineArea.getBounds().height;
		commandLine.setLayoutData(data);
		commandLine.setText(StartupOptionsMgt.getParamList());
		commandLine.setEditable(false);
	}

	/**
	 * Create widgets to enable user to input data for fields of text or number
	 * type
	 * 
	 * @param parent
	 *            composite where the widgets will be attached to
	 * @param startupOption
	 *            the corresponding startup option
	 */
	private void createWidgetsForTextOrNumberType(final Composite parent, final StartupOption startupOption) {
		// create input text with calc button
		if (startupOption.getName().equals(SCALE)) {
			final Text inputText = new Text(parent, SWT.SINGLE | SWT.BORDER);
			inputText.setText(startupOption.getValue());
			startupOption.setValueWidget(inputText);
			inputText.setLayoutData(new GridData(SWT.FILL, SWT.NULL, true, false));
			inputText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					startupOption.setValue(inputText.getText());
					notifyCompositeChangeListeners();
				}
			});

			Button calc = new Button(parent, SWT.PUSH);
			calc.setText(EmulatorNLS.UI_DpiScale_Calculator);
			GridData calcData = new GridData(SWT.NULL, SWT.NULL, false, false);
			calc.setLayoutData(calcData);
			calc.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DpiScaleCalculatorDialog dialog = new DpiScaleCalculatorDialog(new Shell(parent.getShell()), skin);
					if (dialog.open() == Window.OK) {
						for (StartupOptionsGroup group : StartupOptionsMgt.getStartupOptionsGroupsList()) {
							for (StartupOption startupOption : group.getStartupOptions()) {
								if (startupOption.getName().equals(SCALE)) {
									startupOption.setChecked(true);
									startupOption.setValue(dialog.getResultScaleValue());
									startupOption.updateUI();
								}
							}
						}
					}
				}
			});
			calc.setEnabled(canCalculateScale);
			if (!canCalculateScale) {
				ControlDecoration controlDecoration = new ControlDecoration(inputText, SWT.LEFT | SWT.TOP);
				controlDecoration
						.setDescriptionText(EmulatorNLS.StartupOptionsComposite_Error_Loading_Skin_Cant_Calculate_Scale);
				FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
						FieldDecorationRegistry.DEC_WARNING);
				controlDecoration.setImage(fieldDecoration.getImage());
			}
		}
		// create input text
		else if ((startupOption.getPreDefinedValues() == null) || (startupOption.getPreDefinedValues().size() == 0)) {
			final Text inputText = new Text(parent, SWT.SINGLE | SWT.BORDER);
			inputText.setText(startupOption.getValue());
			startupOption.setValueWidget(inputText);
			inputText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			inputText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					startupOption.setValue(inputText.getText());
					notifyCompositeChangeListeners();
				}
			});
		}
		// create combobox
		else {
			final Combo combo = new Combo(parent, SWT.READ_ONLY);
			startupOption.setValueWidget(combo);
			int selectedIndex = 0;
			for (String preDefinedValue : startupOption.getPreDefinedValues()) {
				combo.add(preDefinedValue);
				if (startupOption.getValue().equals(preDefinedValue)) {
					combo.select(selectedIndex);
				} else {
					selectedIndex++;
				}
			}
			combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			combo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					startupOption.setValue(combo.getText());
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
	 * @param startupOption
	 *            the corresponding startup option
	 */
	private void createWidgetsForPathType(final Composite parent, final StartupOption startupOption) {
		// create input text
		final Text pathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		pathText.setText(startupOption.getValue());
		startupOption.setValueWidget(pathText);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.NULL, true, false));
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				startupOption.setValue(pathText.getText());
				notifyCompositeChangeListeners();
			}
		});
		// create browse button
		Button pathBrowseButton = new Button(parent, SWT.PUSH);
		pathBrowseButton.setText(EmulatorNLS.UI_General_BrowseButtonLabel);
		GridData data = new GridData(SWT.NULL, SWT.NULL, false, false);
		pathBrowseButton.setLayoutData(data);
		pathBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedPath = null;

				if (startupOption.getTypeDetails().equalsIgnoreCase(TYPE_PATH_DIR)) {
					DirectoryDialog directoryDialog = new DirectoryDialog(getShell(), SWT.OPEN);
					selectedPath = directoryDialog.open();
				} else {
					FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					String[] filterExtensions = { "*" + startupOption.getTypeDetails() //$NON-NLS-1$
					};
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
	 * @see org.eclipse.andmore.android.emulator.device.ui.AbstractPropertiesComposite#notifyCompositeChangeListeners()
	 */
	@Override
	protected void notifyCompositeChangeListeners() {
		commandLine.setText(StartupOptionsMgt.getParamList());
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
		Status status = StartupOptionsMgt.validate();
		if (status.getSeverity() == IStatus.ERROR) {
			errMsg = status.getMessage();
		}
		return errMsg;
	}

	/**
	 * Reload the values being displayed in the UI as well as the ones in the
	 * model.
	 * 
	 * @param startupOptions
	 *            commandLine the command line used to start the emulator
	 */
	public void reloadValues(String commandLine) {
		StartupOptionsMgt.loadFromCommandLine(commandLine);
	}
}
