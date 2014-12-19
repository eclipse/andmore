/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.ide.eclipse.pdt.internal.preferences;

import com.android.ide.eclipse.pdt.PdtPlugin;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public final static String PREFS_DEVTREE_DIR = PdtPlugin.PLUGIN_ID + ".devtree"; //$NON-NLS-1$

    private DirectoryFieldEditor mDirectoryField;

    public PrefPage() {
        super(GRID);
        IPreferenceStore store = PdtPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(store);
        setDescription("Android Preferences");
    }

    @Override
    protected void createFieldEditors() {
        mDirectoryField = new DirectoryFieldEditor(PREFS_DEVTREE_DIR,
                "Dev Tree Location:", getFieldEditorParent());

        addField(mDirectoryField);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void dispose() {
        super.dispose();

        if (mDirectoryField != null) {
            mDirectoryField.dispose();
            mDirectoryField = null;
        }
    }

}
