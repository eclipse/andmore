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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.andmore.wizards.buildingblocks.Method;
import org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page to create the columns of databases under create activities based
 * on database samples wizard page context.
 * */
public class CreateSampleDatabaseActivityColumnsPage extends NewLauncherWizardPage {
	/**
	 * Help id of the page.
	 * */
	public static final String PAGE_HELP_ID = CodeUtilsActivator.PLUGIN_ID + ".selectcolumnspage";

	private boolean firstLoad = true;

	private String previousSelectedTableName = "";

	private CheckboxTableViewer checkboxTableViewer;

	/**
	 * Default constructor. </br></br> Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@code null} and
	 * {@link CodeUtilsNLS#UI_CreateSampleDatabaseActivityColumnsPageName Page
	 * name} as arguments.
	 */
	public CreateSampleDatabaseActivityColumnsPage() {
		super(null, CodeUtilsNLS.UI_CreateSampleDatabaseActivityColumnsPageName);
	}

	/**
	 * Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@code activity} and
	 * {@link CodeUtilsNLS#UI_CreateSampleDatabaseActivityColumnsPageName Page
	 * name} as arguments.
	 * 
	 * @param activity
	 *            an
	 *            {@code com.motorola.studio.android.model.ActivityBasedOnTemplate}
	 *            to be used as the building block model.
	 */
	public CreateSampleDatabaseActivityColumnsPage(ActivityBasedOnTemplate activity) {
		super(activity, CodeUtilsNLS.UI_CreateSampleDatabaseActivityColumnsPageName);
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

		Composite composite = new Composite(mainComposite, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Create a checkbox table viewer
		checkboxTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		checkboxTableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Set a content and label provider
		checkboxTableViewer.setLabelProvider(new SampleDatabaseActivityColumnsPageLabelProvider());
		checkboxTableViewer.setContentProvider(new ArrayContentProvider());

		// Add a listener to the table viewer
		checkboxTableViewer.addCheckStateListener(new CheckboxTableViewerListener());

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}

		Composite buttonsComposite = new Composite(mainComposite, SWT.NONE);
		buttonsComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

		// Create buttons to select and deselect all items
		Button selectAllButton = new Button(buttonsComposite, SWT.PUSH | SWT.CENTER);
		selectAllButton.setText(CodeUtilsNLS.UI_CreateSampleDatabaseActivityColumnsPage_SelectAllButton);
		selectAllButton.addSelectionListener(new SelectAllButtonListener());

		Button unselectAllButton = new Button(buttonsComposite, SWT.PUSH | SWT.CENTER);
		unselectAllButton.setText(CodeUtilsNLS.UI_CreateSampleDatabaseActivityColumnsPage_DeselectAllButton);
		unselectAllButton.addSelectionListener(new DeselectAllButtonLister());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, PAGE_HELP_ID);

		setControl(mainComposite);
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
		return CodeUtilsNLS.UI_CreateSampleDatabaseActivityColumnsPage_Default_Message;
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
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#canFlipToNextPage()
	 */
	@Override
	public boolean canFlipToNextPage() {
		return (getErrorMessage() == null) && (checkboxTableViewer.getCheckedElements().length > 0);
	}

	/**
	 * @return True if page has header. Otherwise, returns false.
	 */
	@Override
	public boolean hasHeader() {
		return false;
	}

	private class CheckboxTableViewerListener implements ICheckStateListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org
		 * .eclipse.jface.viewers.CheckStateChangedEvent)
		 */
		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			// Check if there are checked elements or not
			if (checkboxTableViewer.getCheckedElements().length > 0) {
				// Update list of columns in the Activity according to event

				// Get the element changed
				if (event.getElement() instanceof Column) {
					Column changedElement = (Column) event.getElement();

					if (event.getChecked()) {
						// Add element to the list
						((ActivityBasedOnTemplate) getBuildBlock()).getCollectorColumnList().add(changedElement);
					} else {
						// Remove element from the list
						((ActivityBasedOnTemplate) getBuildBlock()).getCollectorColumnList().remove(changedElement);
					}

					// Wizard can finish
					((ActivityBasedOnTemplate) getBuildBlock()).setUseSampleDatabaseTableSelected(true);

				}

			} else {
				// Wizard cannot finish
				((ActivityBasedOnTemplate) getBuildBlock()).setUseSampleDatabaseTableSelected(false);

				// Remove all columns from the collection
				((ActivityBasedOnTemplate) getBuildBlock()).getCollectorColumnList().clear();
			}

			getWizard().getContainer().updateButtons();
		}
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
		@SuppressWarnings("unchecked")
		public void pageChanged(PageChangedEvent event) {
			if ((event.getSelectedPage() == CreateSampleDatabaseActivityColumnsPage.this)) {
				// Retrieve the collection of columns from the selected table
				EList<Column> columnList = new BasicEList<Column>();
				String currentTableName = "";

				if (((ActivityBasedOnTemplate) getBuildBlock()).getCollectorTable() != null) {
					currentTableName = ((ActivityBasedOnTemplate) getBuildBlock()).getCollectorTable().getName();
					columnList = ((ActivityBasedOnTemplate) getBuildBlock()).getCollectorTable().getColumns();
				}

				checkboxTableViewer.setInput(columnList.toArray(new Column[0]));

				if (firstLoad || (!currentTableName.equals(previousSelectedTableName))) {
					selectAllItems();

				}
				previousSelectedTableName = currentTableName;
				firstLoad = false;
			}

		}

	}

	/**
	 * Listener for the select all button.
	 */
	private class SelectAllButtonListener implements SelectionListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org
		 * .eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
		 * .swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			selectAllItems();
		}
	}

	/**
	 * Select all items in
	 * {@link CreateSampleDatabaseActivityColumnsPage#checkboxTableViewer
	 * checkboxTableViewer}.
	 */
	@SuppressWarnings("unchecked")
	private void selectAllItems() {
		// Make all the items in the list selected
		checkboxTableViewer.setAllChecked(true);
		// Add elements to the list
		((ActivityBasedOnTemplate) getBuildBlock()).getCollectorColumnList().addAll(
				(Collection<? extends Column>) Arrays.asList(checkboxTableViewer.getCheckedElements()));

		// Wizard can finish
		((ActivityBasedOnTemplate) getBuildBlock()).setUseSampleDatabaseTableSelected(true);
		getWizard().getContainer().updateButtons();
	}

	/**
	 * Listener for the deselect all button.
	 */
	private class DeselectAllButtonLister implements SelectionListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org
		 * .eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
		 * .swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			// Make all the items in the list deselected
			checkboxTableViewer.setAllChecked(false);
			// Clear collection of columns
			((ActivityBasedOnTemplate) getBuildBlock()).getCollectorColumnList().clear();
			// Wizard can finish
			((ActivityBasedOnTemplate) getBuildBlock()).setUseSampleDatabaseTableSelected(false);
			getWizard().getContainer().updateButtons();

		}

	}
}
