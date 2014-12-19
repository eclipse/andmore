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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Project Name SWT Element for Wizards
 */
public class ProjectNameGroup extends Composite {
	private final AndroidProject project;

	private Text projectNameField = null;

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param project
	 */
	public ProjectNameGroup(Composite parent, AndroidProject project) {
		super(parent, SWT.NONE);
		this.project = project;
		createControl(parent);
	}

	/**
	 * Create Controls
	 * 
	 * @param parent
	 */
	private void createControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project Label
		Label label = new Label(this, SWT.NONE);
		label.setText(AndroidNLS.UI_ProjectNameGroup_ProjectNameLabel);
		label.setFont(parent.getFont());

		// new project Name Field
		final Text projectName = new Text(this, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		projectName.setLayoutData(data);
		projectName.setFont(parent.getFont());
		projectName.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				project.setName(projectName.getText().trim());
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}
		});

		projectNameField = projectName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Control#forceFocus()
	 */
	@Override
	public boolean forceFocus() {
		boolean hasFocus = false;

		if (projectNameField != null) {
			hasFocus = projectNameField.setFocus();
		}

		return hasFocus;
	}
}
