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

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.andmore.android.model.AndroidProject.SourceTypes;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Element for NewAndroidProject Wizard Page
 */
public class ApplicationGroup extends Composite {

	private final AndroidProject project;

	private Text packageName = null;

	private Text activityName = null;

	private Text applicationName = null;

	private Text minSdkVersionText;

	private String userChosenPackageName = null;

	private String userChosenActivityName = null;

	private String userChosenMinSdkVersion = null;

	private String userChosenApplicationName = null;

	private final String DEFAULT_ACTIVITY_NAME = "MainActivity";

	private final String DEFAULT_APP_NAME = "User Application";

	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param project
	 */
	public ApplicationGroup(Composite parent, AndroidProject project) {
		super(parent, SWT.NONE);
		this.project = project;
		createContents(this);
	}

	/**
	 * Create Controls
	 * 
	 * @param mainComposite
	 */
	protected void createContents(Composite mainComposite) {
		setLayout(new GridLayout());
		setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// package group
		Composite packageComposite = new Composite(mainComposite, SWT.NONE);
		packageComposite.setLayout(new GridLayout(2, false));
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false);
		packageComposite.setLayoutData(data);

		// application name
		final Label applicationNameLabel = new Label(packageComposite, SWT.NONE);
		applicationNameLabel.setText(AndroidNLS.UI_ApplicationGroup_ApplicationNameLabel);
		applicationNameLabel.setFont(mainComposite.getParent().getFont());

		applicationName = new Text(packageComposite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		applicationName.setLayoutData(data);
		applicationName.setFont(mainComposite.getParent().getFont());
		applicationName.setText(DEFAULT_APP_NAME);
		project.setApplicationName(DEFAULT_APP_NAME);

		// new package label
		final Label packageLabel = new Label(packageComposite, SWT.NONE);
		packageLabel.setText(AndroidNLS.UI_ApplicationGroup_PackageNameLabel);
		packageLabel.setFont(mainComposite.getParent().getFont());

		// new package name entry field
		packageName = new Text(packageComposite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		packageName.setLayoutData(data);
		packageName.setFont(mainComposite.getParent().getFont());
		packageName.setText(project.getPackageName());

		final Label activityLabel = new Label(packageComposite, SWT.NONE);
		activityLabel.setText(AndroidNLS.UI_ApplicationGroup_ActivityNameLabel);
		activityLabel.setFont(mainComposite.getParent().getFont());

		activityName = new Text(packageComposite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		activityName.setLayoutData(data);
		activityName.setFont(mainComposite.getParent().getFont());
		activityName.setText(DEFAULT_ACTIVITY_NAME);
		project.setActivityName(DEFAULT_ACTIVITY_NAME);

		Label minSDKVersionLabel = new Label(packageComposite, SWT.NONE);
		minSDKVersionLabel.setText(AndroidNLS.UI_ApplicationGroup_MinSDKVersionLabel);
		minSdkVersionText = new Text(packageComposite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		minSdkVersionText.setLayoutData(data);
		minSdkVersionText.setFont(mainComposite.getParent().getFont());
		String minSdkVersion = project.getMinSdkVersion();
		if (minSdkVersion != null) {
			minSdkVersionText.setText(minSdkVersion);
		} else {
			minSdkVersionText.setEnabled(false);
			final Runnable listener = new Runnable() {

				@Override
				public void run() {
					AndroidPlugin.getDefault().removeSDKLoaderListener(this);
					minSdkVersionText.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							minSdkVersionText.setEnabled(true);
							minSdkVersionText.setText(project.getMinSdkVersion());
						}
					});
				}
			};
			AndroidPlugin.getDefault().addSDKLoaderListener(listener);
			minSdkVersionText.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					AndroidPlugin.getDefault().removeSDKLoaderListener(listener);
				}
			});
		}

		minSdkVersionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				project.setMinSdkVersion(minSdkVersionText.getText().trim());
				if (minSdkVersionText.isFocusControl()) {
					userChosenMinSdkVersion = minSdkVersionText.getText();
				}
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}
		});

		applicationName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				// application name only will be used if the project is a new
				// project. If it is using existing source this field will be
				// ignored
				project.setApplicationName(applicationName.getText());
				if (applicationName.isFocusControl()) {
					userChosenApplicationName = applicationName.getText();
				}
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}

		});

		activityName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				// activity name only will be used if the project is a new
				// project. If it is using existing source this field will be
				// ignored
				project.setActivityName(activityName.getText());
				if (activityName.isFocusControl()) {
					userChosenActivityName = activityName.getText();
				}
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}

		});

		packageName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				// only change package name if the project isn't using default
				// package name (it is some source based project)
				if (packageName.isFocusControl()) {
					userChosenPackageName = packageName.getText();
					project.setUsingDefaultPackage(false);
				}
				if (!project.isUsingDefaultPackage()) {
					project.setPackageName(packageName.getText());
				}
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}
		});

	}

	/**
	 * Set the enable state for all fields within this widget
	 * 
	 * @param enabled
	 */
	private void setElementsEnabled(boolean enabled) {
		packageName.setEnabled(enabled);
		activityName.setEnabled(enabled);
		applicationName.setEnabled(enabled);
	}

	/**
	 * Update Default Package name.
	 */
	public void updateDefaultName() {
		if ((project.getSourceType() != SourceTypes.NEW) && (project.getSourceType() != SourceTypes.WIDGET)
				&& (packageName != null)) {
			project.setUsingDefaultPackage(true);
			project.setActivityName("");
			packageName.setText(""); //$NON-NLS-1$
			activityName.setText("");
			applicationName.setText("");
			setElementsEnabled(false);

		} else {
			project.setUsingDefaultPackage(userChosenPackageName == null);
			packageName.setText(userChosenPackageName == null ? project.getPackageName() : userChosenPackageName);
			activityName.setText(userChosenActivityName == null ? DEFAULT_ACTIVITY_NAME : userChosenActivityName);
			applicationName.setText(userChosenApplicationName == null ? DEFAULT_APP_NAME : userChosenApplicationName);
			setElementsEnabled(true);
		}

	}

	/**
	 * Updates the Min Sdk Version field
	 */
	public void updateMinSdkVersion() {
		minSdkVersionText.setEnabled((project.getSourceType() == SourceTypes.NEW)
				|| (project.getSourceType() == SourceTypes.WIDGET));
		if (project.getSdkTarget() != null) {
			if (userChosenMinSdkVersion == null) {
				minSdkVersionText.setText(project.getSdkTarget().getVersion().getApiString());
			}
		} else {
			minSdkVersionText.setText("");
		}
	}

	/**
	 * Retrieves the Activity SWT Text control
	 * 
	 * @return activityName
	 */
	public Text getActivityText() {
		return activityName;
	}

	public boolean isElementsEnabled() {
		return packageName.isEnabled() & activityName.isEnabled() & applicationName.isEnabled();
	}

}
