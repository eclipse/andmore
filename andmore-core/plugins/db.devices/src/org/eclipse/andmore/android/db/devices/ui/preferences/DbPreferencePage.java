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
package org.eclipse.andmore.android.db.devices.ui.preferences;

import java.io.File;

import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class DbPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String LEGACY_PLUGIN_ID = "org.eclipse.andmore.android.db"; //$NON-NLS-1$

	public static final String LEGACY_DB_PATH_PREFERENCE = LEGACY_PLUGIN_ID + ".dbstudiopath"; //$NON-NLS-1$

	private DirectoryFieldEditor directoryEditor;

	private static final String PREFERENCE_PAGE_HELP = DbDevicesPlugin.PLUGIN_ID + ".preference-database"; //$NON-NLS-1$

	public DbPreferencePage() {
		setPreferenceStore(DbDevicesPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.FILL);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));

		main.setLayout(new GridLayout(1, false));
		directoryEditor = new DirectoryFieldEditor(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE,
				DbDevicesNLS.UI_PreferencePage_PathLabel, main);
		directoryEditor.getTextControl(main).addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateDirectory();
			}
		});

		directoryEditor.setStringValue(getPreferenceStore().getString(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(main, PREFERENCE_PAGE_HELP);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, PREFERENCE_PAGE_HELP);

		return main;
	}

	public boolean validateDirectory() {
		boolean valid = false;
		if (directoryEditor.getStringValue().trim().length() == 0) {
			valid = true;
		} else {
			try {
				File f = new File(directoryEditor.getStringValue());
				if (f.isDirectory()) {
					File f2 = new File(directoryEditor.getStringValue() + IPath.SEPARATOR + "test"); //$NON-NLS-1$
					f2.createNewFile();
					f2.delete();
					valid = true;
				}
			} catch (Exception e) {
				valid = false;
			}
		}
		if (!valid) {
			setErrorMessage(DbDevicesNLS.ERR_DbPrefPage_InvalidDir);
			setValid(false);
			return false;
		} else {
			setErrorMessage(null);
			setValid(true);
			return true;
		}
	}

	@Override
	protected void performDefaults() {
		getPreferenceStore().setToDefault(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE);
		directoryEditor.setStringValue(getPreferenceStore().getString(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE));
	}

	@Override
	public boolean performOk() {
		boolean canReturn = true;
		if (directoryEditor.getStringValue().trim().length() == 0) {
			performDefaults();
		} else {
			if (!validateDirectory()) {
				canReturn = false;
			} else {
				getPreferenceStore()
						.setValue(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE, directoryEditor.getStringValue());
				canReturn = super.performOk();
			}
		}
		return canReturn;
	}

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	public static void restoreBackWardPref(IPreferenceStore currentPrefStore) {
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, LEGACY_PLUGIN_ID);
		String backwardDbTempPath = preferenceStore.getString(LEGACY_DB_PATH_PREFERENCE);
		if (!backwardDbTempPath.isEmpty()) {
			currentPrefStore.setValue(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE, backwardDbTempPath);

			preferenceStore.setValue(LEGACY_DB_PATH_PREFERENCE, ""); //$NON-NLS-1$
		}
	}

}
