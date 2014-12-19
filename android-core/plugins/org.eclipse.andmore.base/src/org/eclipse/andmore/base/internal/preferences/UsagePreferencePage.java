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

package org.eclipse.andmore.base.internal.preferences;

import com.android.sdkstats.DdmsPreferenceStore;
import com.android.sdkstats.SdkStatsPermissionDialog;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class UsagePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private static final int WRAP_WIDTH_PX = 200;

	private BooleanFieldEditor mOptInCheckBox;
	private DdmsPreferenceStore mStore = new DdmsPreferenceStore();

	public UsagePreferencePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
		// pass
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(1, false));
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label l = new Label(top, SWT.WRAP);
		l.setText(SdkStatsPermissionDialog.BODY_TEXT);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = WRAP_WIDTH_PX;
		l.setLayoutData(gd);

		Link privacyPolicyLink = new Link(top, SWT.WRAP);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = WRAP_WIDTH_PX;
		privacyPolicyLink.setLayoutData(gd);
		privacyPolicyLink.setText(SdkStatsPermissionDialog.PRIVACY_POLICY_LINK_TEXT);

		privacyPolicyLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				SdkStatsPermissionDialog.openUrl(event.text);
			}
		});

		mOptInCheckBox = new BooleanFieldEditor(DdmsPreferenceStore.PING_OPT_IN,
				SdkStatsPermissionDialog.CHECKBOX_TEXT, top);
		mOptInCheckBox.setPage(this);
		mOptInCheckBox.setPreferenceStore(mStore.getPreferenceStore());
		mOptInCheckBox.load();

		return top;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	@Override
	public boolean performCancel() {
		mOptInCheckBox.load();
		return super.performCancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		mOptInCheckBox.loadDefault();
		super.performDefaults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		save();
		return super.performOk();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() {
		save();
		super.performApply();
	}

	private void save() {
		mStore.setPingOptIn(mOptInCheckBox.getBooleanValue());
	}
}
