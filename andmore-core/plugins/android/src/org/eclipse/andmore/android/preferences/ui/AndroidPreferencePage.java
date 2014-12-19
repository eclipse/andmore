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
package org.eclipse.andmore.android.preferences.ui;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * DESCRIPTION: This class represents the preference page for Android Emulator.
 * It only gives to the user the option to reset all dialog configuration
 * clearing all 'Do not show me again' settings and showing all hidden dialogs
 * again. <br>
 * RESPONSIBILITY: Create the preference page for Android Emulator. <br>
 * COLABORATORS: none <br>
 * USAGE: The plugin.xml file links this class to the corresponding preference
 * page extension point.
 * 
 * @see PreferencePage
 * @see IWorkbenchPreferencePage
 */
public class AndroidPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private final String PREFERENCE_PAGE_HELP = AndroidPlugin.PLUGIN_ID + ".preference-android-emulator"; //$NON-NLS-1$

	private boolean cleanActionRequired = false;

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, PREFERENCE_PAGE_HELP);

		Composite entryTable = new Composite(parent, SWT.NULL);
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		layout = new GridLayout(2, false);

		Group dontAskGroup = new Group(entryTable, SWT.NONE);
		dontAskGroup.setLayout(layout);
		dontAskGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		dontAskGroup.setText(AndroidNLS.UI_Preferences_Dialogs_Group_Title);

		Label label = new Label(dontAskGroup, SWT.WRAP);
		label.setText(AndroidNLS.UI_Preferences_Dialogs_Group_Message);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		label.setLayoutData(data);

		Button clearButton = new Button(dontAskGroup, SWT.PUSH);
		clearButton.setText(AndroidNLS.UI_Preferences_Dialogs_Group_Button);
		data = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		data.widthHint = 80;
		clearButton.setLayoutData(data);
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cleanActionRequired = EclipseUtils.displayPrompt(PlatformUI.getWorkbench().getDisplay(),
						AndroidNLS.UI_Preferences_Dialogs_Group_Title, AndroidNLS.UI_Preferences_Dialogs_Clean_Message);
				;
			}
		});
		clearButton.pack();

		return entryTable;
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	/**
	 * @see PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (cleanActionRequired) {
			DialogWithToggleUtils.resetAllDialogsConfiguration();
		}

		return super.performOk();
	}
}
