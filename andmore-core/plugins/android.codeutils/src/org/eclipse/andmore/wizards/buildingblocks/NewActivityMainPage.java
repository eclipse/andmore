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
package org.eclipse.andmore.wizards.buildingblocks;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.Activity;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Class that implements the Activity Wizard Main Page
 */
public class NewActivityMainPage extends NewLauncherWizardPage {
	@Override
	public boolean canFlipToNextPage() {

		return false;
	}

	private static final String NEW_ACTIVITY_HELP = CodeUtilsActivator.PLUGIN_ID + ".newactivity";

	/**
	 * Default constructor
	 * 
	 * @param activity
	 *            The activity model
	 */
	public NewActivityMainPage(Activity activity) {
		super(activity, CodeUtilsNLS.UI_NewActivityMainPage_PageTitle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewBuildingBlocksWizard
	 * #getBuildBlock()
	 */
	@Override
	public Activity getBuildBlock() {
		return (Activity) super.getBuildBlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		Method onCreateMethod = new Method(getBuildBlock().getOnStartMessage()) {
			@Override
			public void handle(boolean selection) {
				getBuildBlock().setOnStart(selection);
			}
		};
		return new Method[] { onCreateMethod };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage
	 * #createIntermediateControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createIntermediateControls(Composite parent) {
		createIntentFilterControls(parent);
		createMainActivityControl(parent, 4);
		createSeparator(parent, 4);
	}

	/**
	 * add samples control.
	 * 
	 * @param composite
	 *            The wizard page composite
	 */
	@Override
	protected void createSampleControls(Composite composite, int nColumns) {
		GridData data = null;

		Composite linkCompositecomposite = new Composite(composite, SWT.FILL);
		linkCompositecomposite.setFont(composite.getFont());
		GridLayout layout = new GridLayout(nColumns, false);
		linkCompositecomposite.setLayout(layout);
		data = new GridData(SWT.FILL, SWT.FILL, true, true, nColumns, 2);
		linkCompositecomposite.setLayoutData(data);

		Image image = null;
		try {
			ImageDescriptor imageDesc = ImageDescriptor.createFromURL(CodeUtilsActivator.getDefault().getBundle()
					.getEntry("icons/obj16/new_activity_template_wiz.png")); //$NON-NLS-1$
			image = imageDesc.createImage();
		} catch (Exception ex) {
			// do nothing;
		}

		CLabel imgLabelLeft = new CLabel(linkCompositecomposite, SWT.CENTER);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		imgLabelLeft.setLayoutData(data);
		imgLabelLeft.setImage(image);

		final Link templateLink = new Link(linkCompositecomposite, SWT.NONE);
		data = new GridData(SWT.LEFT, SWT.CENTER, true, true);
		templateLink.setLayoutData(data);
		templateLink.setText("<a>" + CodeUtilsNLS.UI_CreateNewActivityBasedOnTemplateLink //$NON-NLS-1$
				+ "</a>"); //$NON-NLS-1$

		templateLink.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent event) {

				NewActivityWizard newActivityWizard = (NewActivityWizard) getWizard();
				IWizardContainer container = newActivityWizard.getContainer();

				container.getShell().setVisible(false);

				if (container instanceof WizardDialog) {

					IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry()
							.findWizard("org.eclipse.andmore.android.wizards.newActivityBasedOnTemplateWizard");
					if (descriptor != null) {
						NewActivityBasedOnTemplateWizard wizard = null;
						try {
							wizard = (NewActivityBasedOnTemplateWizard) descriptor.createWizard();
							NewActivityWizard activityWizard = (NewActivityWizard) getWizard();

							wizard.init(PlatformUI.getWorkbench(), activityWizard.getSelection());
							WizardDialog nextWd = new WizardDialog(getShell(), wizard);
							nextWd.setTitle(wizard.getWindowTitle());
							nextWd.open();
						} catch (CoreException e) {
							StudioLogger
									.error(NewActivityMainPage.class,
											"could not open new activity based on template wizard from inside new activity wizard");
						}
					}

					WizardDialog nextWd = (WizardDialog) container;
					nextWd.close();
				}

			}

		});

		templateLink.setEnabled(true);

	}

	private void createMainActivityControl(Composite parent, int nColumns) {
		// Create a checkbox to allow the user to set the created activity as
		// the MAIN activity
		Button checkButtonMainActivity = new Button(parent, SWT.CHECK | SWT.LEFT);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, nColumns, 1);
		checkButtonMainActivity.setLayoutData(gridData);
		checkButtonMainActivity.setText(CodeUtilsNLS.UI_NewActivityMainPage_CheckMainButton);
		checkButtonMainActivity.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Button) {
					// Set Activity flag accordingly
					getBuildBlock().setMainActivity(((Button) e.widget).getSelection());

				}
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_NewActivityMainPage_DescriptionCreateActivity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_NewActivityMainPage_TitleActivity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewLauncherWizardPage
	 * #getIntentFiltersActions()
	 */
	@Override
	protected String[] getIntentFiltersActions() {
		String[] intentFiltersActions = new String[0];
		try {
			intentFiltersActions = AndroidUtils.getActivityActions(getBuildBlock().getProject());
		} catch (AndroidException e) {
			setErrorMessage(e.getMessage());
		}
		return intentFiltersActions;
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their on help should
	 * override this method
	 */
	@Override
	protected String getHelpId() {
		return NEW_ACTIVITY_HELP;
	}
}