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

import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.logcat.LogCatMessageList;
import com.android.ddmuilib.logcat.LogCatPanel;

import org.eclipse.andmore.base.InstallDetails;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.ddms.LogCatMonitor;
import org.eclipse.andmore.ddms.i18n.Messages;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Preference Pane for LogCat.
 */
public class LogCatPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private BooleanFieldEditor mSwitchPerspective;
	private ComboFieldEditor mWhichPerspective;
	private IntegerFieldEditor mMaxMessages;
	private BooleanFieldEditor mAutoMonitorLogcat;
	private ComboFieldEditor mAutoMonitorLogcatLevel;
	private BooleanFieldEditor mAutoScrollLock;

	public LogCatPreferencePage() {
		super(GRID);
		setPreferenceStore(DdmsPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		FontFieldEditor ffe = new FontFieldEditor(LogCatPanel.LOGCAT_VIEW_FONT_PREFKEY,
				Messages.LogCatPreferencePage_Display_Font, getFieldEditorParent());
		addField(ffe);

		mMaxMessages = new IntegerFieldEditor(LogCatMessageList.MAX_MESSAGES_PREFKEY,
				Messages.LogCatPreferencePage_MaxMessages, getFieldEditorParent());
		addField(mMaxMessages);

		mAutoScrollLock = new BooleanFieldEditor(LogCatPanel.AUTO_SCROLL_LOCK_PREFKEY,
				"Automatically enable/disable scroll lock based on the scrollbar position", getFieldEditorParent());
		addField(mAutoScrollLock);

		createHorizontalSeparator();

		if (InstallDetails.isAdtInstalled()) {
			createAdtSpecificFieldEditors();
		}
	}

	private void createHorizontalSeparator() {
		Label l = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		l.setLayoutData(gd);
	}

	private void createAdtSpecificFieldEditors() {
		mSwitchPerspective = new BooleanFieldEditor(PreferenceInitializer.ATTR_SWITCH_PERSPECTIVE,
				Messages.LogCatPreferencePage_Switch_Perspective, getFieldEditorParent());
		addField(mSwitchPerspective);
		IPerspectiveDescriptor[] perspectiveDescriptors = PlatformUI.getWorkbench().getPerspectiveRegistry()
				.getPerspectives();
		String[][] perspectives = new String[0][0];
		if (perspectiveDescriptors.length > 0) {
			perspectives = new String[perspectiveDescriptors.length][2];
			for (int i = 0; i < perspectiveDescriptors.length; i++) {
				IPerspectiveDescriptor perspective = perspectiveDescriptors[i];
				perspectives[i][0] = perspective.getLabel();
				perspectives[i][1] = perspective.getId();
			}
		}
		mWhichPerspective = new ComboFieldEditor(PreferenceInitializer.ATTR_PERSPECTIVE_ID,
				Messages.LogCatPreferencePage_Switch_To, perspectives, getFieldEditorParent());
		mWhichPerspective.setEnabled(getPreferenceStore().getBoolean(PreferenceInitializer.ATTR_SWITCH_PERSPECTIVE),
				getFieldEditorParent());
		addField(mWhichPerspective);

		createHorizontalSeparator();

		mAutoMonitorLogcat = new BooleanFieldEditor(LogCatMonitor.AUTO_MONITOR_PREFKEY,
				Messages.LogCatPreferencePage_AutoMonitorLogcat, getFieldEditorParent());
		addField(mAutoMonitorLogcat);

		mAutoMonitorLogcatLevel = new ComboFieldEditor(LogCatMonitor.AUTO_MONITOR_LOGLEVEL,
				Messages.LogCatPreferencePage_SessionFilterLogLevel, new String[][] {
						{ LogLevel.VERBOSE.toString(), LogLevel.VERBOSE.getStringValue() },
						{ LogLevel.DEBUG.toString(), LogLevel.DEBUG.getStringValue() },
						{ LogLevel.INFO.toString(), LogLevel.INFO.getStringValue() },
						{ LogLevel.WARN.toString(), LogLevel.WARN.getStringValue() },
						{ LogLevel.ERROR.toString(), LogLevel.ERROR.getStringValue() },
						{ LogLevel.ASSERT.toString(), LogLevel.ASSERT.getStringValue() }, }, getFieldEditorParent());
		mAutoMonitorLogcatLevel.setEnabled(getPreferenceStore().getBoolean(LogCatMonitor.AUTO_MONITOR_PREFKEY),
				getFieldEditorParent());
		addField(mAutoMonitorLogcatLevel);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource().equals(mSwitchPerspective)) {
			mWhichPerspective.setEnabled(mSwitchPerspective.getBooleanValue(), getFieldEditorParent());
		} else if (event.getSource().equals(mAutoMonitorLogcat)) {
			mAutoMonitorLogcatLevel.setEnabled(mAutoMonitorLogcat.getBooleanValue(), getFieldEditorParent());
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		mWhichPerspective.setEnabled(mSwitchPerspective.getBooleanValue(), getFieldEditorParent());

		mMaxMessages.setStringValue(Integer.toString(LogCatMessageList.MAX_MESSAGES_DEFAULT));

		mAutoMonitorLogcatLevel.setEnabled(mAutoMonitorLogcat.getBooleanValue(), getFieldEditorParent());
	}
}
