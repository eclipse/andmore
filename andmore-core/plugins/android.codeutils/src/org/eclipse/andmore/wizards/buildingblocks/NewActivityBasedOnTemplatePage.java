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
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Class that implements the Activity Wizard Main Page
 */
public class NewActivityBasedOnTemplatePage extends NewLauncherWizardPage {

	private IPackageFragmentRoot currentPackageFragmentRoot;

	private IPackageFragmentRoot previousPackageFragmentRoot;

	private boolean firstLoad = true;

	/**
	 * Listener to verify when this page is visible
	 */
	private class PageChangeListener implements IPageChangedListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.IPageChangedListener#pageChanged(org.eclipse
		 * .jface.dialogs.PageChangedEvent)
		 */
		@Override
		public void pageChanged(PageChangedEvent event) {
			if ((event.getSelectedPage() == NewActivityBasedOnTemplatePage.this)) {
				if (!firstLoad) {
					handleFieldChanged(NewTypeWizardPage.TYPENAME);
					handleFieldChanged(NewTypeWizardPage.PACKAGE);
				}
				firstLoad = false;
			}
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		if (getBuildBlock().isDatabaseTemplateSelected()) {
			return !getBuildBlock().isBasicInformationFilledIn();
		} else {
			return false;
		}

	}

	private static final String NEW_ACTIVITY_BASED_ON_TEMPLATE_HELP = CodeUtilsActivator.PLUGIN_ID
			+ ".new-activity-based-on-template"; //$NON-NLS-1$

	/**
	 * Default constructor
	 * 
	 * @param activity
	 *            The activity model
	 */
	public NewActivityBasedOnTemplatePage(ActivityBasedOnTemplate activity) {
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
	public ActivityBasedOnTemplate getBuildBlock() {
		return (ActivityBasedOnTemplate) super.getBuildBlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		return null;
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

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}

		currentPackageFragmentRoot = getBuildBlock().getPackageFragmentRoot();
		previousPackageFragmentRoot = getBuildBlock().getPackageFragmentRoot();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_NewActivityMainPage_DescriptionCreateActivityBasedOnTemplate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_NewActivityMainPage_TitleActivityBasedOnTemplate;
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
		return NEW_ACTIVITY_BASED_ON_TEMPLATE_HELP;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.wizards.NewTypeWizardPage#handleFieldChanged(java.
	 * lang.String)
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		if (NewContainerWizardPage.CONTAINER.equals(fieldName)) {
			currentPackageFragmentRoot = getBuildBlock().getPackageFragmentRoot();

			if ((currentPackageFragmentRoot != null)
					&& (!currentPackageFragmentRoot.equals(previousPackageFragmentRoot))) {
				previousPackageFragmentRoot = currentPackageFragmentRoot;
				// Set the collector info to null
				getBuildBlock().setCollectorTable(null);
				getBuildBlock().setCollectorDatabaseName(null);
				getBuildBlock().setDatabaseTableSelected(false);
				getBuildBlock().setSqlOpenHelperDefined(false);
				getBuildBlock().setUseSampleDatabaseTableSelected(false);

				IWizardPage page = getWizard().getPage(CodeUtilsNLS.UI_DefineSqlOpenHelperPage_Title);

				if (!firstLoad) {
					if (page instanceof NewTypeWizardPage) {
						((NewTypeWizardPage) page).setPackageFragment(getPackageFragment(), true);
					}
				}
			}
		}
		if (getWizard().getContainer().getCurrentPage() != null) {
			getWizard().getContainer().updateButtons();
		}

	}
}