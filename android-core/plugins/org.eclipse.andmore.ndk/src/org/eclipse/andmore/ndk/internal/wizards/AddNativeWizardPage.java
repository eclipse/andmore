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

package org.eclipse.andmore.ndk.internal.wizards;

import org.eclipse.andmore.ndk.internal.Messages;
import org.eclipse.andmore.ndk.internal.NdkManager;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.Map;

public class AddNativeWizardPage extends WizardPage {

	private final String defaultLibraryName;

	private Text libraryNameText;

	public AddNativeWizardPage(Map<String, String> templateArgs) {
		super("addNativeWizardPage"); //$NON-NLS-1$
		setDescription(Messages.AddNativeWizardPage_Description);
		setTitle(Messages.AddNativeWizardPage_Title);

		defaultLibraryName = templateArgs.get(NdkManager.LIBRARY_NAME);
		if (!NdkManager.isNdkLocationValid()) {
			setErrorMessage(Messages.AddNativeWizardPage_Location_not_valid);
		}
	}

	@Override
	public boolean isPageComplete() {
		return NdkManager.isNdkLocationValid();
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(2, false));

		Label lblLibraryName = new Label(container, SWT.NONE);
		lblLibraryName.setText(Messages.AddNativeWizardPage_LibraryName);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		composite.setLayout(new GridLayout(3, false));

		Label lblLib = new Label(composite, SWT.NONE);
		lblLib.setText("lib"); //$NON-NLS-1$

		libraryNameText = new Text(composite, SWT.BORDER);
		libraryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		libraryNameText.setText(defaultLibraryName);

		Label lblso = new Label(composite, SWT.NONE);
		lblso.setText(".so"); //$NON-NLS-1$
	}

	public void updateArgs(Map<String, String> templateArgs) {
		templateArgs.put(NdkManager.LIBRARY_NAME, libraryNameText.getText());
	}

}
