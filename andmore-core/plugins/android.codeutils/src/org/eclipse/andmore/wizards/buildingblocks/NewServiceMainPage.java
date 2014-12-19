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
import org.eclipse.andmore.android.model.Service;

/**
 * Class that implements the Service Wizard Main Page.
 */
public class NewServiceMainPage extends NewBuildingBlocksWizardPage {

	private static final String NEW_SERVICE_HELP = CodeUtilsActivator.PLUGIN_ID + ".newservice";

	/**
	 * Default constructor
	 * 
	 * @param service
	 *            The service wizard model
	 */
	public NewServiceMainPage(Service service) {
		super(service, CodeUtilsNLS.UI_NewServiceMainPage_WizardTitle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getBuildBlock()
	 */
	@Override
	public Service getBuildBlock() {
		return (Service) super.getBuildBlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		Method onCreateMethod = new Method(getBuildBlock().getOnCreateMessage()) {
			@Override
			public void handle(boolean selection) {
				getBuildBlock().setOnCreateMethod(selection);
			}
		};

		Method onStartMethod = new Method(getBuildBlock().getOnStartMessage()) {
			@Override
			public void handle(boolean selection) {
				getBuildBlock().setOnStartMethod(selection);
			}
		};

		Method[] methods = new Method[] { onCreateMethod, onStartMethod };
		return methods;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_NewServiceMainPage_SubtitleCreateService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_NewServiceMainPage_TitleService;
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their on help should
	 * override this method
	 */
	@Override
	protected String getHelpId() {
		return NEW_SERVICE_HELP;
	}
}
