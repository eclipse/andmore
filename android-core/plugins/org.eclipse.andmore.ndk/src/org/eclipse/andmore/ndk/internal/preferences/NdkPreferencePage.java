/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal.preferences;

import org.eclipse.andmore.ndk.internal.Activator;
import org.eclipse.andmore.ndk.internal.Messages;
import org.eclipse.andmore.ndk.internal.NdkManager;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class NdkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private NdkDirectoryFieldEditor mNdkDirectoryEditor;

	public NdkPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.NDKPreferencePage_Preferences);
	}

	@Override
	protected void createFieldEditors() {
		mNdkDirectoryEditor = new NdkDirectoryFieldEditor(NdkManager.NDK_LOCATION, Messages.NDKPreferencePage_Location,
				getFieldEditorParent());
		addField(mNdkDirectoryEditor);
	}

	private static class NdkDirectoryFieldEditor extends DirectoryFieldEditor {
		public NdkDirectoryFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			setEmptyStringAllowed(true);
		}

		@Override
		protected boolean doCheckState() {
			if (!super.doCheckState()) {
				setErrorMessage(Messages.NDKPreferencePage_not_a_valid_directory);
				return false;
			}

			String dirname = getTextControl().getText().trim();
			if (!dirname.isEmpty() && !NdkManager.isValidNdkLocation(dirname)) {
				setErrorMessage(Messages.NDKPreferencePage_not_a_valid_NDK_directory);
				return false;
			}

			return true;
		}

		@Override
		public Text getTextControl(Composite parent) {
			setValidateStrategy(VALIDATE_ON_KEY_STROKE);
			return super.getTextControl(parent);
		}

	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do herea
	}

	@Override
	public void dispose() {
		super.dispose();

		if (mNdkDirectoryEditor != null) {
			mNdkDirectoryEditor.dispose();
			mNdkDirectoryEditor = null;
		}
	}

}
