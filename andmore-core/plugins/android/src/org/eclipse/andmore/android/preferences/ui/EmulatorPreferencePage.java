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
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.jface.preference.IPreferenceStore;
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
 * This class draws the preference page that allow user to chose between display
 * the native emulator outside the view after it's closed or not.
 */
public class EmulatorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final String PREFERENCE_PAGE_HELP = AndroidPlugin.PLUGIN_ID + ".preference-emulator-view"; //$NON-NLS-1$

	protected boolean shallUnembedEmulators;

	private Button unembedCheckBox;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		super.getPreferenceStore().setDefault(AndroidPlugin.SHALL_UNEMBED_EMULATORS_PREF_KEY, true);
		shallUnembedEmulators = super.getPreferenceStore().getBoolean(AndroidPlugin.SHALL_UNEMBED_EMULATORS_PREF_KEY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, PREFERENCE_PAGE_HELP);

		Composite mainComposite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		mainComposite.setLayout(layout);

		Group emulatorViewGroup = new Group(mainComposite, SWT.NONE);
		GridLayout emulatorViewGroupLayout = new GridLayout();
		GridData emulatorViewGroupLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		emulatorViewGroup.setLayoutData(emulatorViewGroupLayoutData);
		emulatorViewGroup.setLayout(emulatorViewGroupLayout);
		emulatorViewGroup.setText(AndroidNLS.EmulatorPreferencePage_EmulatorViewGroup);

		unembedCheckBox = new Button(emulatorViewGroup, SWT.CHECK);
		GridData unembedCheckBoxData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		unembedCheckBox.setLayoutData(unembedCheckBoxData);
		unembedCheckBox.setText(AndroidNLS.EmulatorPreferencePage_UnembedCheckBox);
		unembedCheckBox.setSelection(shallUnembedEmulators);
		unembedCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button source = (Button) e.getSource();
				shallUnembedEmulators = source.getSelection();
				super.widgetSelected(e);
			}
		});

		Label noteLabel = new Label(emulatorViewGroup, SWT.WRAP);
		noteLabel.setText(AndroidNLS.EmulatorPreferencePage_UnembedNote);
		GridData noteLabelLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		noteLabelLayoutData.widthHint = 100;
		noteLabel.setLayoutData(noteLabelLayoutData);

		return mainComposite;
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(AndroidPlugin.SHALL_UNEMBED_EMULATORS_PREF_KEY, shallUnembedEmulators);
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		shallUnembedEmulators = true;
		unembedCheckBox.setSelection(shallUnembedEmulators);
		super.performDefaults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return AndroidPlugin.getDefault().getPreferenceStore();
	}

}
