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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.BuildingBlockModel;
import org.eclipse.andmore.android.model.Launcher;
import org.eclipse.andmore.android.wizards.elements.AddInputRemoveButtons;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

/**
 * Abstract class to contribute with the Building Blocks Wizards. This class
 * creates the controls to add Intent Filters and Categories to the Building
 * Blocks
 */
public abstract class NewLauncherWizardPage extends NewBuildingBlocksWizardPage {
	private List activityActions;

	private AddInputRemoveButtons addRemoveActionButtons;

	private AddInputRemoveButtons addRemoveCategoryButtons;

	private List activityCategories;

	private class InputDialogValidator implements IInputValidator {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.IInputValidator#isValid(java.lang.String)
		 */
		@Override
		public String isValid(String newText) {
			String message = null;
			if (((newText == null) || (newText.length() == 0) || (newText.contains(" ")))) //$NON-NLS-1$
			{
				message = CodeUtilsNLS.NewLauncherWizardPage_InputDialogValidationMessage;
			}
			return message;
		}
	}

	/**
	 * Default constructor
	 * 
	 * @param buildBlock
	 *            The building block model
	 * @param pageName
	 *            The page name
	 */
	protected NewLauncherWizardPage(BuildingBlockModel buildBlock, String pageName) {
		super(buildBlock, pageName);
	}

	/**
	 * Creates the "Action" section on the wizard
	 * 
	 * @param composite
	 *            the wizard composite
	 */
	protected void createActionsControls(Composite composite) {
		Label activityActionsLabel = new Label(composite, SWT.NONE);
		activityActionsLabel.setText(CodeUtilsNLS.UI_NewLauncherWizardPage_ActionLabel);
		GridData gridData = new GridData();
		gridData.widthHint = convertWidthInCharsToPixels(12);
		gridData.verticalAlignment = GridData.BEGINNING;
		activityActionsLabel.setLayoutData(gridData);

		activityActions = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		activityActions.setItems(getBuildBlock().getIntentFilterActionsAsArray());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gridData.heightHint = convertHeightInCharsToPixels(3);
		activityActions.setLayoutData(gridData);
		addRemoveActionButtons = new AddInputRemoveButtons(composite);
		setButtonLayoutData(addRemoveActionButtons.getAddButton());
		setButtonLayoutData(addRemoveActionButtons.getInputButton());
		setButtonLayoutData(addRemoveActionButtons.getRemoveButton());

		addRemoveActionButtons.getAddButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Set<String> categorySet = new HashSet<String>(Arrays.asList(getIntentFiltersActions()));

				try {
					categorySet.removeAll(getBuildBlock().getIntentFilterActions());
				} catch (Exception e) {
					StudioLogger.error(NewLauncherWizardPage.class, e.getLocalizedMessage(), e);
				}
				FilteredActionsSelectionDialog dialog = new FilteredActionsSelectionDialog(getShell(), categorySet);
				dialog.setInitialPattern("**");
				dialog.setTitle(CodeUtilsNLS.UI_NewLauncherWizardPage_ActionSelectionDialogTitle);
				dialog.setMessage(CodeUtilsNLS.UI_NewLauncherWizardPage_ActionSelectionDialogMessage);

				if (Window.OK == dialog.open()) {
					for (Object result : dialog.getResult()) {
						getBuildBlock().addIntentFilterAction((String) result);
					}
					activityActions.setItems(getBuildBlock().getIntentFilterActionsAsArray());
					addRemoveActionButtons.getRemoveButton().setEnabled(activityActions.getSelectionCount() > 0);
					updateStatus(getBuildBlock().getStatus());
				}
			}

		});
		addRemoveActionButtons.getInputButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				InputDialog dialog = new InputDialog(getShell(),
						CodeUtilsNLS.NewLauncherWizardPage_ActionTypeDialogTitle,
						CodeUtilsNLS.NewLauncherWizardPage_ActionTypeDialogMessage, null, new InputDialogValidator());
				int result = dialog.open();
				if (result == Window.OK) {
					String action = dialog.getValue();
					if (action != null) {
						getBuildBlock().addIntentFilterAction(action.trim());
						activityActions.setItems(getBuildBlock().getIntentFilterActionsAsArray());
						addRemoveActionButtons.getRemoveButton().setEnabled(activityActions.getSelectionCount() > 0);
						updateStatus(getBuildBlock().getStatus());
					}
				}
			}
		});
		addRemoveActionButtons.getRemoveButton().setEnabled(activityActions.getSelectionCount() > 0);
		addRemoveActionButtons.getRemoveButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				for (int selection : activityActions.getSelectionIndices()) {
					getBuildBlock().removeIntentFilterAction(activityActions.getItem(selection));
				}
				activityActions.setItems(getBuildBlock().getIntentFilterActionsAsArray());
				addRemoveActionButtons.getRemoveButton().setEnabled(activityActions.getSelectionCount() > 0);
				updateStatus(getBuildBlock().getStatus());
			}

		});
		activityActions.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				addRemoveActionButtons.getRemoveButton().setEnabled(activityActions.getSelectionCount() > 0);
			}
		});
	}

	/**
	 * Creates the "Categories" section on the wizard
	 * 
	 * @param composite
	 *            the wizard composite
	 */
	protected void createCategoriesControls(Composite composite) {
		GridData gridData;
		Label activitycategoriesLabel = new Label(composite, SWT.NONE);
		activitycategoriesLabel.setText(CodeUtilsNLS.UI_NewLauncherWizardPage_CategoryLabel);
		gridData = new GridData();
		gridData.verticalAlignment = GridData.BEGINNING;
		activitycategoriesLabel.setLayoutData(gridData);

		activityCategories = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		activityCategories.setItems(getBuildBlock().getIntentFilterCategoriesAsArray());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gridData.heightHint = convertHeightInCharsToPixels(3);
		activityCategories.setLayoutData(gridData);

		addRemoveCategoryButtons = new AddInputRemoveButtons(composite);
		setButtonLayoutData(addRemoveCategoryButtons.getAddButton());
		setButtonLayoutData(addRemoveCategoryButtons.getInputButton());
		setButtonLayoutData(addRemoveCategoryButtons.getRemoveButton());

		addRemoveCategoryButtons.getAddButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Set<String> categorySet = new HashSet<String>(0);
				try {
					categorySet = new HashSet<String>(Arrays.asList(AndroidUtils
							.getIntentFilterCategories(getBuildBlock().getProject())));
				} catch (AndroidException e) {
					setErrorMessage(e.getMessage());
				}

				categorySet.removeAll(getBuildBlock().getIntentFilterCategories());

				FilteredActionsSelectionDialog dialog = new FilteredActionsSelectionDialog(getShell(), categorySet);
				dialog.setInitialPattern("**");
				dialog.setTitle(CodeUtilsNLS.UI_NewLauncherWizardPage_CategorySelectionDialogTitle);
				dialog.setMessage(CodeUtilsNLS.UI_NewLauncherWizardPage_CategorySelectionDialogMessage);

				if (Window.OK == dialog.open()) {
					for (Object result : dialog.getResult()) {
						getBuildBlock().addIntentFilterCategories((String) result);
					}
					activityCategories.setItems(getBuildBlock().getIntentFilterCategoriesAsArray());
					addRemoveCategoryButtons.getRemoveButton().setEnabled(activityCategories.getSelectionCount() > 0);
					updateStatus(getBuildBlock().getStatus());
				}
			}
		});
		addRemoveCategoryButtons.getInputButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				InputDialog dialog = new InputDialog(getShell(),
						CodeUtilsNLS.NewLauncherWizardPage_CategoryTypeDialogTitle,
						CodeUtilsNLS.NewLauncherWizardPage_CategoryTypeDialogMessage, null, new InputDialogValidator());
				int result = dialog.open();
				if (result == Window.OK) {
					String action = dialog.getValue();
					if (action != null) {
						getBuildBlock().addIntentFilterCategories(action.trim());
						activityCategories.setItems(getBuildBlock().getIntentFilterCategoriesAsArray());
						addRemoveCategoryButtons.getRemoveButton().setEnabled(
								activityCategories.getSelectionCount() > 0);
						updateStatus(getBuildBlock().getStatus());
					}
				}
			}
		});
		addRemoveCategoryButtons.getRemoveButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				for (int selection : activityCategories.getSelectionIndices()) {
					getBuildBlock().removeIntentFilterCategories(activityCategories.getItem(selection));
				}
				activityCategories.setItems(getBuildBlock().getIntentFilterCategoriesAsArray());
				addRemoveCategoryButtons.getRemoveButton().setEnabled(activityCategories.getSelectionCount() > 0);
				updateStatus(getBuildBlock().getStatus());
			}
		});
		addRemoveCategoryButtons.getRemoveButton().setEnabled(activityCategories.getSelectionCount() > 0);
		activityCategories.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				addRemoveCategoryButtons.getRemoveButton().setEnabled(activityCategories.getSelectionCount() > 0);
			}
		});
	}

	/**
	 * Creates the Intent Filter group on the wizard page
	 * 
	 * @param parent
	 *            The wizard composite
	 */
	protected void createIntentFilterControls(Composite parent) {
		Group intentFilterGroup = new Group(parent, SWT.NONE);
		intentFilterGroup.setText(CodeUtilsNLS.UI_NewLauncherWizardPage_IntentFilterGroupName);
		intentFilterGroup.setLayout(new GridLayout(4, false));
		intentFilterGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));

		createActionsControls(intentFilterGroup);
		createCategoriesControls(intentFilterGroup);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getBuildBlock()
	 */
	@Override
	public Launcher getBuildBlock() {
		return (Launcher) super.getBuildBlock();
	}

	/**
	 * Return all intent filters actions according with the Building Block
	 * 
	 * @return
	 */
	protected abstract String[] getIntentFiltersActions();

}
