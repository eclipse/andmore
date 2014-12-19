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
package org.eclipse.andmore.android.wizards.widget;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.andmore.android.wizards.elements.ApplicationGroup;
import org.eclipse.andmore.android.wizards.elements.ProjectNameGroup;
import org.eclipse.andmore.android.wizards.elements.SdkTargetSelector;
import org.eclipse.andmore.android.wizards.elements.WidgetLocationGroup;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Class that represents the main page in the New Widget Project Wizard
 */
public class NewAndroidWidgetProjectMainPage extends WizardPage {

	private static final String PAGE_NAME = "Main Page";

	private final AndroidProject project;

	private final String NEW_WIDGET_PROJECT_HELP = AndroidPlugin.PLUGIN_ID + ".newwdgproj";

	/**
	 * Listener for the wizard changes
	 */
	private final Listener modelListener = new Listener() {
		@Override
		public void handleEvent(Event arg0) {
			IStatus status = project.getStatus();
			int severity = status.getSeverity();
			setPageComplete(severity != IStatus.ERROR);

			int msgType;
			switch (severity) {
			case IStatus.OK:
				msgType = IMessageProvider.NONE;
				break;
			case IStatus.ERROR:
				msgType = IMessageProvider.ERROR;
				break;
			case IStatus.WARNING:
				msgType = IMessageProvider.WARNING;
				break;
			default:
				msgType = IMessageProvider.NONE;
				break;
			}
			String defaultMessage = AndroidNLS.UI_NewAndroidWidgetProjectMainPage_SubtitleCreateProject;
			setMessage(status.isOK() ? defaultMessage : status.getMessage(), msgType);
		}
	};

	/**
	 * Constructor
	 * 
	 * @param project
	 *            The selected project
	 */
	public NewAndroidWidgetProjectMainPage(AndroidProject project) {
		super(PAGE_NAME);
		this.project = project;
		setTitle(AndroidNLS.UI_NewAndroidWidgetProjectMainPage_TitleCreateProject);
		setDescription(AndroidNLS.UI_NewAndroidWidgetProjectMainPage_WizardProjectDescription);
		setPageComplete(false);

	}

	/**
	 * Create the page SWT controls.
	 * 
	 * @param parent
	 *            The parent composite
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());

		initializeDialogUnits(parent);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// only put the project and location groups: no vertical resize, no
		// vertical style

		ProjectNameGroup projectNameGroup = new ProjectNameGroup(composite, project);
		projectNameGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		// Create Location Group
		Group groupForLocation = new Group(composite, SWT.SHADOW_ETCHED_IN);
		// Layout has 1 column
		groupForLocation.setLayout(new GridLayout());
		groupForLocation.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		groupForLocation.setFont(composite.getFont());
		groupForLocation.setText(AndroidNLS.UI_NewAndroidWidgetProjectMainPage_LabelContents);

		WidgetLocationGroup locationGroup = new WidgetLocationGroup(groupForLocation, project, this);
		// End of Location Group

		// Create SDK Group
		// create sdk group with vertical resize, grabbing excedding space
		Group groupForTarget = new Group(composite, SWT.SHADOW_ETCHED_IN);
		groupForTarget.setLayout(new GridLayout());
		groupForTarget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		groupForTarget.setFont(composite.getFont());
		groupForTarget.setText(AndroidNLS.UI_NewAndroidProjectMainPage_LabelTarget);

		final SdkTargetSelector mSdkTargetSelector = new SdkTargetSelector(groupForTarget, project);
		// End of Target Creation

		// Create Package Group
		Group group = new Group(composite, SWT.SHADOW_ETCHED_IN);

		// Layout has 1 column
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		group.setFont(composite.getFont());
		group.setText(AndroidNLS.UI_NewAndroidProjectMainPage_LabelApplication);

		final ApplicationGroup applicationGroup = new ApplicationGroup(group, project);

		Listener listener = new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				applicationGroup.updateDefaultName();
				applicationGroup.updateMinSdkVersion();
				getContainer().updateButtons();
			}
		};

		projectNameGroup.addListener(IWizardModel.MODIFIED, modelListener);
		projectNameGroup.addListener(IWizardModel.MODIFIED, listener);
		locationGroup.addListener(IWizardModel.MODIFIED, listener);
		locationGroup.addListener(IWizardModel.MODIFIED, modelListener);
		applicationGroup.addListener(IWizardModel.MODIFIED, modelListener);
		mSdkTargetSelector.addListener(IWizardModel.MODIFIED, modelListener);
		mSdkTargetSelector.addListener(IWizardModel.MODIFIED, listener);

		// create application group with no vertical resize
		setPageComplete(false);

		// Show description the first time
		setErrorMessage(null);
		setMessage(null);

		projectNameGroup.forceFocus();
		setControl(composite);
		composite.layout(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, NEW_WIDGET_PROJECT_HELP);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.DialogPage#setButtonLayoutData(org.eclipse.
	 * swt.widgets.Button)
	 */
	@Override
	public GridData setButtonLayoutData(Button button) {
		return super.setButtonLayoutData(button);
	}

}
