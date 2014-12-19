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

import java.io.File;

import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.andmore.android.wizards.widget.NewAndroidWidgetProjectMainPage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Class that implements the Location group to be used in the New Widget Project
 * Wizard
 */
public class WidgetLocationGroup extends Composite {
	final private AndroidProject project;

	final private NewAndroidWidgetProjectMainPage page;

	private String lastSourcePathValue = null;

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param project
	 * @param page
	 */
	public WidgetLocationGroup(Composite parent, AndroidProject project, NewAndroidWidgetProjectMainPage page) {
		super(parent, SWT.SHADOW_ETCHED_IN);
		this.project = project;
		this.page = page;
		setLayout(new GridLayout());
		setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createControl(this);
	}

	/**
	 * Create Controls
	 * 
	 * @param parent
	 */
	private void createControl(Composite parent) {
		final Button useDefaultLocation = new Button(this, SWT.CHECK);
		useDefaultLocation.setText(AndroidNLS.UI_LocationGroup_UseDefaultLocationCheckLabel);
		useDefaultLocation.setSelection(true);

		Composite location_group = new Composite(this, SWT.NONE);
		location_group.setLayout(new GridLayout(4, false));
		location_group.setLayoutData(new GridData(GridData.FILL_BOTH));
		location_group.setFont(parent.getFont());

		Label locationLabel = new Label(location_group, SWT.NONE);
		locationLabel.setText(AndroidNLS.UI_LocationGroup_LocationLabel);

		final Text projectPath = new Text(location_group, SWT.BORDER);
		GridData data = new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1);
		projectPath.setLayoutData(data);
		projectPath.setFont(parent.getFont());
		projectPath.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
		projectPath.setEnabled(false);
		projectPath.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (projectPath.isEnabled()) {
					project.setLocation(projectPath.getText());
					notifyListeners(IWizardModel.MODIFIED, new Event());
				}
			}
		});

		final Button browseButton = new Button(location_group, SWT.PUSH);
		browseButton.setText(AndroidNLS.UI_General_BrowseButtonLabel);
		browseButton.setEnabled(false);
		page.setButtonLayoutData(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				String existing_dir = project.getLocation();

				// Disable the path if it doesn't exist
				if (existing_dir != null && (existing_dir.length() == 0 || !new File(existing_dir).exists())) {
					existing_dir = null;
				}

				DirectoryDialog dd = new DirectoryDialog(projectPath.getShell());
				dd.setMessage(AndroidNLS.UI_LocationGroup_BrowseDialogMessage);
				dd.setFilterPath(existing_dir);
				String customLocation = dd.open();
				if (customLocation != null) {
					projectPath.setText(customLocation);
				}
			}
		});

		// Listen to the default location checkbox.

		SelectionListener location_listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				boolean useDefault = useDefaultLocation.getSelection();

				String path;
				if (useDefault) {
					path = project.getDefaultLocation();
				} else {
					path = project.getLocation();
				}
				boolean enablePath = !useDefault;
				projectPath.setEnabled(enablePath);
				browseButton.setEnabled(enablePath);

				project.setUseDefaultLocation(useDefault);

				if (enablePath && (path == null || path.length() == 0)) {
					projectPath.setText(""); //$NON-NLS-1$
					projectPath.setFocus();
				} else {
					projectPath.setText(path);
				}

				notifyListeners(IWizardModel.MODIFIED, new Event());
			}

		};

		useDefaultLocation.addSelectionListener(location_listener);
	}

	/**
	 * Get last path value defined by the user for the
	 * "Create from existing source" case
	 * 
	 * @return The last path value defined by the user.
	 */
	public String getLastSourcePath() {
		return this.lastSourcePathValue;
	}

	/**
	 * Set the last path value defined by the user for the
	 * "Create from existing source" case
	 * 
	 * @param path
	 *            - The last path defined by the user
	 */
	public void setLastSourcePath(String path) {
		this.lastSourcePathValue = path;
	}
}
