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

import org.eclipse.andmore.android.model.BuildingBlockModel;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;

/**
 * Base abstract class to create the building block wizards UI. A building block
 * represents an Android abstraction, which can be an Activity, Service,
 * Broadcast Receiver, Content Provider or Wigdet Provider.
 */
public abstract class NewBuildingBlocksWizard extends Wizard implements INewWizard {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return (!getBuildingBlock().needMoreInformation())
				&& (getBuildingBlock().getStatus().getSeverity() != IStatus.ERROR);
	}

	/**
	 * @return The building block model used by the wizard.
	 */
	protected abstract BuildingBlockModel getBuildingBlock();
}
