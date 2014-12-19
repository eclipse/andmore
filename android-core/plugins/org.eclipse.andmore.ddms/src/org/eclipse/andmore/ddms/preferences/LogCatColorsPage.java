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

package org.eclipse.andmore.ddms.preferences;

import com.android.ddmuilib.logcat.LogCatPanel;

import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class LogCatColorsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public LogCatColorsPage() {
		super(GRID);
		setPreferenceStore(DdmsPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		// colors preference for different log levels
		ColorFieldEditor cfe = new ColorFieldEditor(LogCatPanel.VERBOSE_COLOR_PREFKEY, "Verbose Log Message Color",
				getFieldEditorParent());
		addField(cfe);

		cfe = new ColorFieldEditor(LogCatPanel.DEBUG_COLOR_PREFKEY, "Debug Log Message Color", getFieldEditorParent());
		addField(cfe);

		cfe = new ColorFieldEditor(LogCatPanel.INFO_COLOR_PREFKEY, "Info Log Message Color", getFieldEditorParent());
		addField(cfe);

		cfe = new ColorFieldEditor(LogCatPanel.WARN_COLOR_PREFKEY, "Warning Log Message Color", getFieldEditorParent());
		addField(cfe);

		cfe = new ColorFieldEditor(LogCatPanel.ERROR_COLOR_PREFKEY, "Error Log Message Color", getFieldEditorParent());
		addField(cfe);

		cfe = new ColorFieldEditor(LogCatPanel.ASSERT_COLOR_PREFKEY, "Assert Log Message Color", getFieldEditorParent());
		addField(cfe);
	}
}
