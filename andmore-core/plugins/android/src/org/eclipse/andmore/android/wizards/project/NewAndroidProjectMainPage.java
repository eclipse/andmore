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
package org.eclipse.andmore.android.wizards.project;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.andmore.android.model.AndroidProject.SourceTypes;
import org.eclipse.andmore.android.wizards.elements.ApplicationGroup;
import org.eclipse.andmore.android.wizards.elements.LocationGroup;
import org.eclipse.andmore.android.wizards.elements.ProjectNameGroup;
import org.eclipse.andmore.android.wizards.elements.SdkTargetSelector;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Class that represents the main page in the New Project Wizard
 */
public class NewAndroidProjectMainPage extends WizardPage {
	private static final String PAGE_NAME = "Main Page";

	private final AndroidProject project;

	private final String NEW_PROJECT_HELP = AndroidPlugin.PLUGIN_ID + ".newproj";

	// private boolean isNativeChecked = false;

	private boolean hasNativePage = false;

	private Button nativeCkb;

	private Button obfuscateCkbox;

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
			String defaultMessage = AndroidNLS.UI_NewAndroidProjectMainPage_SubtitleCreateProject;
			setMessage(status.isOK() ? defaultMessage : status.getMessage(), msgType);
		}
	};

	/**
	 * Constructor
	 * 
	 * @param project
	 *            The selected project
	 */
	public NewAndroidProjectMainPage(AndroidProject project, boolean hasNativePage) {
		super(PAGE_NAME);
		this.project = project;
		this.hasNativePage = hasNativePage;
		setTitle(AndroidNLS.UI_NewAndroidProjectMainPage_TitleCreateProject);
		setDescription(AndroidNLS.UI_NewAndroidProjectMainPage_WizardProjectDescription);
		setPageComplete(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NULL);
		mainComposite.setFont(parent.getFont());
		mainComposite.setLayout(new FillLayout(SWT.VERTICAL));

		initializeDialogUnits(mainComposite);

		final ScrolledComposite scroll = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		final Composite innerScrollComposite = new Composite(scroll, SWT.NONE);

		innerScrollComposite.setLayout(new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		innerScrollComposite.setLayoutData(data);

		// only put the project and location groups: no vertical resize, no
		// vertical style
		data = new GridData(SWT.FILL, SWT.NONE, true, false);

		ProjectNameGroup projectNameGroup = new ProjectNameGroup(innerScrollComposite, project);
		projectNameGroup.setLayoutData(data);

		// Create Location Group
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		Group groupForLocation = new Group(innerScrollComposite, SWT.SHADOW_ETCHED_IN);
		// Layout has 1 column
		groupForLocation.setLayout(new GridLayout());
		groupForLocation.setLayoutData(data);
		groupForLocation.setFont(innerScrollComposite.getFont());
		groupForLocation.setText(AndroidNLS.UI_NewAndroidProjectMainPage_LabelContents);

		LocationGroup locationGroup = new LocationGroup(groupForLocation, project, this);
		// End of Location Group

		// Create SDK Group
		// create sdk group with vertical resize, grabbing excedding space
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 150;
		Group groupForTarget = new Group(innerScrollComposite, SWT.SHADOW_ETCHED_IN);
		groupForTarget.setLayout(new GridLayout());
		groupForTarget.setLayoutData(data);
		groupForTarget.setFont(innerScrollComposite.getFont());
		groupForTarget.setText(AndroidNLS.UI_NewAndroidProjectMainPage_LabelTarget);

		final SdkTargetSelector mSdkTargetSelector = new SdkTargetSelector(groupForTarget, project);
		// End of Target Creation

		// Create Package Group
		Group group = new Group(innerScrollComposite, SWT.SHADOW_ETCHED_IN);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		// Layout has 1 column
		group.setLayout(new GridLayout());
		group.setLayoutData(data);
		group.setFont(innerScrollComposite.getFont());
		group.setText(AndroidNLS.UI_NewAndroidProjectMainPage_LabelApplication);

		final ApplicationGroup applicationGroup = new ApplicationGroup(group, project);
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				applicationGroup.updateDefaultName();
				applicationGroup.updateMinSdkVersion();
				getContainer().updateButtons();

				if (hasNativePage) {
					if (project.getSourceType() == SourceTypes.SAMPLE) {
						nativeCkb.setEnabled(false);
						nativeCkb.setSelection(false);
						project.setAddNativeSupport(false);
					} else {
						nativeCkb.setEnabled(true);
						project.setAddNativeSupport(nativeCkb.getSelection());
					}
				}

				project.setNeedToObfuscate(obfuscateCkbox.getSelection());
			}
		};

		nativeCkb = new Button(innerScrollComposite, SWT.CHECK);
		nativeCkb.setText(AndroidNLS.UI_ProjectCreation_NativeSupport);
		if (!hasNativePage) {
			nativeCkb.setEnabled(false);
		}

		obfuscateCkbox = new Button(innerScrollComposite, SWT.CHECK);
		obfuscateCkbox.setText(AndroidNLS.UI_ProjectCreation_Obfuscate);
		obfuscateCkbox.setEnabled(true);

		projectNameGroup.addListener(IWizardModel.MODIFIED, modelListener);
		projectNameGroup.addListener(IWizardModel.MODIFIED, listener);
		locationGroup.addListener(IWizardModel.MODIFIED, listener);
		locationGroup.addListener(IWizardModel.MODIFIED, modelListener);
		applicationGroup.addListener(IWizardModel.MODIFIED, modelListener);
		mSdkTargetSelector.addListener(IWizardModel.MODIFIED, modelListener);
		mSdkTargetSelector.addListener(IWizardModel.MODIFIED, listener);
		nativeCkb.addListener(SWT.Selection, listener);
		nativeCkb.addListener(SWT.Selection, modelListener);
		obfuscateCkbox.addListener(SWT.Selection, listener);
		obfuscateCkbox.addListener(SWT.Selection, modelListener);

		// create application group with no vertical resize
		setPageComplete(false);

		// Show description the first time
		setErrorMessage(null);
		setMessage(null);

		projectNameGroup.forceFocus();

		setControl(innerScrollComposite);
		innerScrollComposite.layout(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, NEW_PROJECT_HELP);

		// set up scroll
		scroll.setContent(innerScrollComposite);

		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);

		scroll.setMinSize(innerScrollComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		//
		// set control
		setControl(mainComposite);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage() {

		// returns null if there is no next page
		IWizardPage page = null;
		if (project.isAddingNativeSupport()) {
			// calls native page
			page = getWizard().getPage(NewAndroidProjectWizard.NATIVE_PAGE_NAME);
		} else {
			// calls source page when selected
			if (project.getSourceType() == SourceTypes.SAMPLE) {
				page = getWizard().getPage(NewAndroidProjectWizard.SAMPLE_PAGE_NAME);
			}
		}

		return page;
	}

	@Override
	public boolean isPageComplete() {
		boolean canFinish = true;
		if ((project.isAddingNativeSupport()) || (project.getSourceType() == SourceTypes.SAMPLE)
				|| !super.isPageComplete()) {
			canFinish = false;
		}
		return canFinish;
	}

	@Override
	public boolean canFlipToNextPage() {
		boolean canFlip = false;
		if ((project.isAddingNativeSupport()) || (project.getSourceType() == SourceTypes.SAMPLE)) {
			if (super.isPageComplete()) {
				canFlip = true;
			}
		}
		return canFlip;
	}
}
