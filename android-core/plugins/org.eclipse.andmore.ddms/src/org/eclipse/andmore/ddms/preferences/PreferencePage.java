/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms.preferences;

import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.Log.LogLevel;

import org.eclipse.andmore.base.InstallDetails;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.ddms.i18n.Messages;
import org.eclipse.andmore.ddms.views.DeviceView.HProfHandler;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor mUseAdbHost;
	private StringFieldEditor mAdbHostValue;
	private IntegerFieldEditor mProfilerBufsize;

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(DdmsPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		IntegerFieldEditor ife;

		ife = new IntegerFieldEditor(PreferenceInitializer.ATTR_DEBUG_PORT_BASE,
				Messages.PreferencePage_Base_Local_Debugger_Port, getFieldEditorParent());
		ife.setValidRange(1024, 32767);
		addField(ife);

		BooleanFieldEditor bfe;

		bfe = new BooleanFieldEditor(PreferenceInitializer.ATTR_DEFAULT_THREAD_UPDATE,
				Messages.PreferencePage_Thread_Updates_Enabled_By_Default, getFieldEditorParent());
		addField(bfe);

		bfe = new BooleanFieldEditor(PreferenceInitializer.ATTR_DEFAULT_HEAP_UPDATE,
				Messages.PreferencePage_Heap_Updates_Enabled_Default, getFieldEditorParent());
		addField(bfe);

		ife = new IntegerFieldEditor(PreferenceInitializer.ATTR_THREAD_INTERVAL,
				Messages.PreferencePage_Thread_Status_Refresh_Interval, getFieldEditorParent());
		ife.setValidRange(1, 60);
		addField(ife);

		if (InstallDetails.isAdtInstalled()) {
			ComboFieldEditor cfe = new ComboFieldEditor(PreferenceInitializer.ATTR_HPROF_ACTION,
					Messages.PreferencePage_HPROF_Action, new String[][] {
							{ Messages.PreferencePage_Save_Disk, HProfHandler.ACTION_SAVE },
							{ Messages.PreferencePage_Open_Eclipse, HProfHandler.ACTION_OPEN }, },
					getFieldEditorParent());
			addField(cfe);
		}

		mProfilerBufsize = new IntegerFieldEditor(PreferenceInitializer.ATTR_PROFILER_BUFSIZE_MB,
				"Method Profiler buffer size (MB):", getFieldEditorParent());
		addField(mProfilerBufsize);

		ife = new IntegerFieldEditor(PreferenceInitializer.ATTR_TIME_OUT,
				Messages.PreferencePage_ADB_Connection_Time_Out, getFieldEditorParent());
		addField(ife);

		RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(PreferenceInitializer.ATTR_LOG_LEVEL,
				Messages.PreferencePage_Logging_Level, 1, new String[][] {
						{ Messages.PreferencePage_Verbose, LogLevel.VERBOSE.getStringValue() },
						{ Messages.PreferencePage_Debug, LogLevel.DEBUG.getStringValue() },
						{ Messages.PreferencePage_Info, LogLevel.INFO.getStringValue() },
						{ Messages.PreferencePage_Warning, LogLevel.WARN.getStringValue() },
						{ Messages.PreferencePage_Error, LogLevel.ERROR.getStringValue() },
						{ Messages.PreferencePage_Assert, LogLevel.ASSERT.getStringValue() } }, getFieldEditorParent(),
				true);
		addField(rgfe);
		mUseAdbHost = new BooleanFieldEditor(PreferenceInitializer.ATTR_USE_ADBHOST,
				Messages.PreferencePage_Use_Adbhost, getFieldEditorParent());
		addField(mUseAdbHost);
		mAdbHostValue = new StringFieldEditor(PreferenceInitializer.ATTR_ADBHOST_VALUE,
				Messages.PreferencePage_Adbhost_value, getFieldEditorParent());
		mAdbHostValue.setEnabled(getPreferenceStore().getBoolean(PreferenceInitializer.ATTR_USE_ADBHOST),
				getFieldEditorParent());
		addField(mAdbHostValue);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource().equals(mUseAdbHost)) {
			mAdbHostValue.setEnabled(mUseAdbHost.getBooleanValue(), getFieldEditorParent());
		} else if (event.getSource().equals(mProfilerBufsize)) {
			DdmPreferences.setProfilerBufferSizeMb(mProfilerBufsize.getIntValue());
		}
		super.propertyChange(event);
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		mAdbHostValue.setEnabled(mUseAdbHost.getBooleanValue(), getFieldEditorParent());
	}
}
