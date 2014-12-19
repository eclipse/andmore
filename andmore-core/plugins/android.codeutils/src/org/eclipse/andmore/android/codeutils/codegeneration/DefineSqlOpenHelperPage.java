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
package org.eclipse.andmore.android.codeutils.codegeneration;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.andmore.android.model.Launcher;
import org.eclipse.andmore.wizards.buildingblocks.Method;
import org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard to define (select existing or create new) SQL Open Helper file to
 * enable connection and copy of database for the sample
 **/
public class DefineSqlOpenHelperPage extends NewLauncherWizardPage {
	private static final String SQL_OPEN_HELPER = "SqlOpenHelper";

	private Button ckbGenerateSQLOpenHelper;

	private Group sqlOpenHelperGroup;

	private boolean firstLoad = true;

	public static final String PAGE_HELP_ID = CodeUtilsActivator.PLUGIN_ID + ".defineconnectiondatabasepage";

	/**
	 * Default constructor. </br></br> Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@link org.eclipse.andmore.android.model.ActivityBasedOnTemplate#ActivityBasedOnTemplate()}
	 * and {@link CodeUtilsNLS#UI_DefineSqlOpenHelperPage_Title Page name} as
	 * arguments.
	 */
	public DefineSqlOpenHelperPage() {
		// need to instantiate new activity because it will define a new sql
		// open helper class to create.
		super(new ActivityBasedOnTemplate(), CodeUtilsNLS.UI_DefineSqlOpenHelperPage_Title);
	}

	/**
	 * Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@code activity} and
	 * {@link CodeUtilsNLS#UI_CreateSampleDatabaseActivityPageName Page name} as
	 * arguments.
	 * 
	 * @param activity
	 *            an
	 *            {@code com.motorola.studio.android.model.ActivityBasedOnTemplate}
	 *            to be used as the building block model.
	 */
	public DefineSqlOpenHelperPage(ActivityBasedOnTemplate activity) {
		super(activity, CodeUtilsNLS.UI_DefineSqlOpenHelperPage_Title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage
	 * #createIntermediateControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createExtendedControls(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, PAGE_HELP_ID);

		createOpenHelperSection(mainComposite);

		setControl(mainComposite);
	}

	/**
	 * Listener to verify when this page is visible.
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
			if ((event.getSelectedPage() == DefineSqlOpenHelperPage.this)) {
				Launcher launcher = getBuildBlock();
				if (launcher instanceof ActivityBasedOnTemplate) {
					ActivityBasedOnTemplate activity = (ActivityBasedOnTemplate) getBuildBlock();
					if (activity != null) {

						setPackageFragmentRoot(activity.getPackageFragmentRoot(), false);

						if (firstLoad) {
							setPackageFragment(activity.getPackageFragment(), true);
							setTypeName(activity.getName() + SQL_OPEN_HELPER, true);
						}
						firstLoad = false;
					}
				}
				handleFieldChanged(NewTypeWizardPage.TYPENAME);
				handleFieldChanged(NewTypeWizardPage.PACKAGE);
			}
		}
	}

	/**
	 * Create composite group to display SQL Open Helper parameters.
	 * 
	 * @param mainComposite
	 *            parent composite.
	 */
	private void createOpenHelperSection(Composite mainComposite) {
		// check box for generating SQL Open Helper
		ckbGenerateSQLOpenHelper = new Button(mainComposite, SWT.CHECK);
		ckbGenerateSQLOpenHelper.setText(CodeUtilsNLS.UI_PersistenceWizardPageCreateNewSQLOpenHelper);
		ckbGenerateSQLOpenHelper.setSelection(true);

		sqlOpenHelperGroup = new Group(mainComposite, SWT.NONE);
		sqlOpenHelperGroup.setText(CodeUtilsNLS.UI_PersistenceWizardPageSQLOpenHelperGroupTitle);
		int numColumns = 5;
		sqlOpenHelperGroup.setLayout(new GridLayout(numColumns, false));
		sqlOpenHelperGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		/* Class selection for SQLOpenHelper */
		createTypeNameControls(sqlOpenHelperGroup, numColumns);

		/* Package selection for SQLOpenHelper */
		createContainerControls(sqlOpenHelperGroup, numColumns);

		createPackageControls(sqlOpenHelperGroup, numColumns);

		ckbGenerateSQLOpenHelper.setEnabled(true);
		((ActivityBasedOnTemplate) getBuildBlock()).setCreateOpenHelper(true);

		// add Listener for the check box of the open helper enablement
		ckbGenerateSQLOpenHelper.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				boolean selected = ckbGenerateSQLOpenHelper.getSelection();
				((ActivityBasedOnTemplate) getBuildBlock()).setCreateOpenHelper(selected);

				if (!selected) {
					setMessage(CodeUtilsNLS.UI_DefineSqlOpenHelperPage_WarningNoOpenHelperSelected,
							IMessageProvider.WARNING);
				} else {
					setMessage(null);
				}

				// get the check box which dispatched the event
				Button checkBox = event.widget != null ? (Button) event.widget : null;
				// proceed in case there is a check box
				if (checkBox != null) {
					// flag indicating whether to enable/disable the controls
					boolean enabled = checkBox.getSelection();
					// enable/disable the children of panelEnablementGroup field
					setCompositeChildremEnabled(sqlOpenHelperGroup, enabled);
				}
				setOpenHelperDefined();
				getWizard().getContainer().updateButtons();
			}
		});
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_DefineSqlOpenHelperPage_Default_Message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getHelpId()
	 */
	@Override
	protected String getHelpId() {
		return PAGE_HELP_ID;
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
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_ActivityWizard_Title;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	/**
	 * @return Returns true if page has header. Otherwise, returns false.
	 */
	@Override
	public boolean hasHeader() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		if (NewTypeWizardPage.TYPENAME.equals(fieldName)) {
			String typeNameWithParameters = getTypeName();
			((ActivityBasedOnTemplate) getBuildBlock()).setSqlOpenHelperClassName(typeNameWithParameters);
			getBuildBlock().setNameStatus(typeNameChanged());
			getBuildBlock().setPackageStatus(packageChanged());
		} else if (NewTypeWizardPage.PACKAGE.equals(fieldName)) {
			String packName = getPackageText();
			((ActivityBasedOnTemplate) getBuildBlock()).setSqlOpenHelperPackageName(packName);
			getBuildBlock().setPackageStatus(packageChanged());
		}
		updateStatus(getBuildBlock().getStatus());
		setOpenHelperDefined();
		getWizard().getContainer().updateButtons();
	}

	/**
	 * True if user selects to create open helper class and there is no error
	 * message, false otherwise.
	 */
	private void setOpenHelperDefined() {
		// update if no error detected on name and package
		((ActivityBasedOnTemplate) getBuildBlock()).setSqlOpenHelperDefined(!isCreateSQLOpenHelperClass()
				|| (getErrorMessage() == null));
	}

	/**
	 * Returns <code>true</code> in case it is necessary to create the Open SQL
	 * Helper classes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> in case it is necessary to create SQL Open
	 *         Helper class, <code>false</code> otherwise.
	 */
	public boolean isCreateSQLOpenHelperClass() {
		return ckbGenerateSQLOpenHelper != null ? ckbGenerateSQLOpenHelper.getSelection() : false;
	}

	/**
	 * Enable/disable children of the entered {@link Composite}.
	 * 
	 * @param composite
	 *            Composite to have its children enabled/disabled
	 * @param enabled
	 *            <code>true</code> for enabling the elements,
	 *            <code>false</code> for disabling the elements.
	 */
	private void setCompositeChildremEnabled(Composite composite, boolean enabled) {
		Control[] controls = composite.getChildren();
		if ((controls != null) && (controls.length > 0)) {
			for (Control control : controls) {
				control.setEnabled(enabled);
			}
		}
	}

}
