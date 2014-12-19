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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.model.ContentProvider;
import org.eclipse.andmore.android.wizards.elements.InputRemoveButtons;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

/**
 * Class that implements the Content Provider Wizard Main Page.
 */
public class NewProviderMainPage extends NewBuildingBlocksWizardPage {
	private List authoritiesList;

	private Button useDefault;

	private static final String NEW_PROVIDER_HELP = CodeUtilsActivator.PLUGIN_ID + ".newcontprov";

	protected int defaultIndex = -1;

	/*
	 * Private class that implements an Input Validator for the Input Button.
	 */
	private class InputButtonValidator implements IInputValidator {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.IInputValidator#isValid(java.lang.String)
		 */
		@Override
		public String isValid(String newText) {
			IStatus status = JavaConventions.validatePackageName(newText, JavaCore.VERSION_1_7, JavaCore.VERSION_1_7);
			if (status.isOK() && getBuildBlock().getAuthoritiesList().contains(newText)) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewProviderMainPage_ErrorMessageAlreadyExists);
			}

			if (status.isOK()) {
				Pattern pattern = Pattern.compile("[a-z0-9\\._]+");
				Matcher matcher = pattern.matcher(newText);

				if (!matcher.matches()) {
					status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
							CodeUtilsNLS.ERR_NewProviderMainPage_InvalidCharactersInAuthority);
				}
			}

			if (status.isOK() && newText.equalsIgnoreCase(getBuildBlock().getDefaultAuthority())) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewProviderMainPage_ErrorMessageDefaultName);
			}
			return status.isOK() ? null : status.getMessage();
		}
	}

	/*
	 * Private class that implements a Listener for the Add Button.
	 */
	private class InputButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event arg0) {
			InputDialog dialog = new InputDialog(getShell(), CodeUtilsNLS.UI_NewProviderMainPage_TitleNewAuthority,
					CodeUtilsNLS.UI_NewProviderMainPage_MessageAvoidConflicts, "", new InputButtonValidator()); //$NON-NLS-1$
			if (dialog.open() == Window.OK) {
				String authority = dialog.getValue();
				authoritiesList.add(authority);
				getBuildBlock().addAuthority(authority);
			}
			updateStatus(getBuildBlock().getStatus());
		}
	}

	/*
	 * Private class that implements a Listener for the Remove Button.
	 */
	private class RemoveButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event arg0) {
			int selectionIndex = authoritiesList.getSelectionIndex();
			if (selectionIndex != defaultIndex) {
				String authoritySelected = authoritiesList.getItem(selectionIndex);
				authoritiesList.remove(selectionIndex);
				getBuildBlock().removeAuthority(authoritySelected);
				updateStatus(getBuildBlock().getStatus());
			}
		}
	}

	/*
	 * Private class that implements a Listener for the Use Default Button.
	 */
	private class UseDefaultButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event arg0) {
			java.util.List<String> asList = Arrays.asList(authoritiesList.getItems());
			if (useDefault.getSelection() && !asList.contains(getBuildBlock().getDefaultAuthority())) {
				getBuildBlock().addAuthority(getBuildBlock().getDefaultAuthority());
				authoritiesList.add(getBuildBlock().getDefaultAuthority(), 0);
				defaultIndex = authoritiesList.indexOf(getBuildBlock().getDefaultAuthority());
			} else if (!useDefault.getSelection()) {
				getBuildBlock().removeAuthority(getBuildBlock().getDefaultAuthority());
				authoritiesList.remove(getBuildBlock().getDefaultAuthority());
				defaultIndex = -1;
			}
			updateStatus(getBuildBlock().getStatus());
		}
	}

	/*
	 * Private class that implements a Listener for the Authorities Listener
	 */
	private class AuthoritiesListListener implements Listener {
		InputRemoveButtons inputRemoveButtons;

		/**
		 * Default constructor
		 * 
		 * @param addRemoveButtons
		 *            The AddRemoveButtons composite
		 */
		public AuthoritiesListListener(InputRemoveButtons addRemoveButtons) {
			this.inputRemoveButtons = addRemoveButtons;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event arg0) {
			int selectionCount = authoritiesList.getSelectionCount();
			boolean enabled = selectionCount > 0;
			if (selectionCount == 1) {
				enabled = !getBuildBlock().getDefaultAuthority().equals(
						authoritiesList.getItem(authoritiesList.getSelectionIndex()));
			}
			inputRemoveButtons.getRemoveButton().setEnabled(enabled);
			updateStatus(getBuildBlock().getStatus());
		}
	}

	/**
	 * Default constructor.
	 * 
	 * @param buildBlock
	 *            The content provider building block model.
	 */
	protected NewProviderMainPage(ContentProvider buildBlock) {
		super(buildBlock, CodeUtilsNLS.UI_NewProviderMainPage_WizardTitle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getBuildBlock()
	 */
	@Override
	public ContentProvider getBuildBlock() {
		return (ContentProvider) super.getBuildBlock();
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

		Label authoritiesLabel = new Label(parent, SWT.NONE);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.BEGINNING;
		authoritiesLabel.setLayoutData(gridData);
		authoritiesLabel.setText(CodeUtilsNLS.UI_NewProviderMainPage_LabelAuthorities);
		authoritiesList = new List(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		gridData = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gridData.heightHint = convertHeightInCharsToPixels(3);
		authoritiesList.setLayoutData(gridData);

		final InputRemoveButtons inputRemoveButtons = new InputRemoveButtons(parent);
		setButtonLayoutData(inputRemoveButtons.getInputButton());
		setButtonLayoutData(inputRemoveButtons.getRemoveButton());
		inputRemoveButtons.getRemoveButton().setEnabled(false);
		inputRemoveButtons.getInputButton().addListener(SWT.Selection, new InputButtonListener());
		inputRemoveButtons.getRemoveButton().addListener(SWT.Selection, new RemoveButtonListener());
		new Label(parent, SWT.NONE);
		useDefault = new Button(parent, SWT.CHECK);
		useDefault.setText(CodeUtilsNLS.UI_NewProviderMainPage_OptionUseDefault);
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		useDefault.setLayoutData(gridData);
		useDefault.setSelection(true);

		String defaultAuthority = getBuildBlock().getDefaultAuthority();
		authoritiesList.add(defaultAuthority);
		defaultIndex = 0;
		getBuildBlock().addAuthority(defaultAuthority);

		useDefault.addListener(SWT.Selection, new UseDefaultButtonListener());
		authoritiesList.addListener(SWT.Selection, new AuthoritiesListListener(inputRemoveButtons));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#typeNameChanged()
	 */
	@Override
	protected IStatus typeNameChanged() {
		if (authoritiesList != null) {
			String defaultAuthority = getBuildBlock().getDefaultAuthority();
			if (defaultIndex != -1) {
				getBuildBlock().removeAuthority(authoritiesList.getItem(defaultIndex));
				if ((defaultAuthority != null) && (defaultAuthority.length() > 0)) {

					if (!getBuildBlock().containsAuthority(defaultAuthority)) {
						authoritiesList.setItem(defaultIndex, defaultAuthority);
						getBuildBlock().addAuthority(defaultAuthority);
					} else {
						authoritiesList.remove(defaultIndex);
						defaultIndex = -1;
					}
				} else {
					authoritiesList.remove(defaultIndex);
					defaultIndex = -1;
				}
			}
		}
		return super.typeNameChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		return new Method[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_NewProviderMainPage_SubtitleCreateContentProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_NewProviderMainPage_TitleContentProvider;
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their on help should
	 * override this method
	 */
	@Override
	protected String getHelpId() {
		return NEW_PROVIDER_HELP;
	}
}
